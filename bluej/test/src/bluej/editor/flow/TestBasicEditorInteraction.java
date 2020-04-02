/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019,2020  Michael Kolling and John Rosenberg

 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 

 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 

 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 This file is subject to the Classpath exception as provided in the
 LICENSE.txt file that accompanied this code.
 */
package bluej.editor.flow;

import bluej.Config;
import bluej.editor.flow.gen.GenRandom;
import bluej.editor.flow.gen.GenString;
import bluej.parser.InitConfig;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Utility;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(JUnitQuickcheck.class)
public class TestBasicEditorInteraction extends FXTest
{
    private Stage stage;
    private FlowEditorPane flowEditorPane;
    private FlowEditor flowEditor;

    @Override
    public void start(Stage stage) throws Exception
    {
        super.start(stage);

        InitConfig.init();
        Config.loadFXFonts();
        PrefMgr.setScopeHighlightStrength(100);
        PrefMgr.setFlag(PrefMgr.HIGHLIGHTING, true);

        this.stage = stage;
        flowEditor = new FlowEditor(null, "", null, null, null, () -> {}, new ReadOnlyBooleanWrapper(true), true);
        flowEditorPane = flowEditor.getSourcePane();
        flowEditorPane.setPrefWidth(800.0);
        flowEditorPane.setPrefHeight(600.0);
        stage.setScene(new Scene(new BorderPane(flowEditor, new MenuBar(flowEditor.getFXMenu().toArray(Menu[]::new)), null, null, null)));
        stage.show();
    }
    
    interface KeyboardMover
    {
        // Moves to new pos and returns expected position.  For convenience, current position and length are given
        // Also, the mutable target column (used for up/down movements) is passed, which may be read from and/or written to.
        // The new pos is inherently determined, e.g. a keyboard mover for end-of-line will always move to the current end of line
        int move(int curPos, int curLen, AtomicInteger targetColumn);
    }
    
    class NamedKeyboardMover
    {
        private final String name;
        private final KeyboardMover mover;

        public NamedKeyboardMover(String name, KeyboardMover mover)
        {
            this.name = name;
            this.mover = mover;
        }
    }
    
    @Property(trials = 5)
    public void testKeyboardMovement(@From(GenString.class) String rawContent, @From(GenRandom.class) Random r)
    {
        String content = removeInvalid(rawContent);
        setText(content);
        clickOn(flowEditorPane);

        List<NamedKeyboardMover> movers = getMovers();
        AtomicInteger targetColumn = new AtomicInteger(-1);

        int curPos = 0;
        int curAnchor = 0;
        for (int i = 0; i < 12; i++)
        {
            // Sometimes, randomise position to stop us getting stuck near top/bottom:
            if (i == 0 || r.nextInt(3) == 1)
            {
                curPos = r.nextInt(content.length() + 1);
                curAnchor = r.nextInt(content.length() + 1);
                int curPosFinal = curPos;
                int curAnchorFinal = curAnchor;
                fx_(() -> {
                    flowEditorPane.positionCaret(curPosFinal);
                    flowEditorPane.positionAnchor(curAnchorFinal);
                });
                targetColumn.set(-1);
            }
            boolean shiftDown = r.nextBoolean();
            NamedKeyboardMover mover = movers.get(r.nextInt(movers.size()));
            if (shiftDown)
            {
                press(KeyCode.SHIFT);
            }
            int newPos = mover.mover.move(curPos, content.length(), targetColumn);
            if (shiftDown)
            {
                release(KeyCode.SHIFT);
            }
            assertEquals("Pressing " + mover.name + (shiftDown ? " holding shift" : ""), newPos, fx(() -> flowEditorPane.getCaretPosition()).intValue());
            assertEquals("Pressing " + mover.name + (shiftDown ? " holding shift" : ""), shiftDown ? curAnchor : newPos, fx(() -> flowEditorPane.getAnchorPosition()).intValue());
            curPos = newPos;
            if (!shiftDown)
            {
                curAnchor = curPos;
            }
        }
    }

    @Property(trials = 5)
    public void testKeyboardDelete(@From(GenString.class) String rawContent, @From(GenRandom.class) Random r)
    {
        String content = removeInvalid(rawContent);
        setText(content);
        clickOn(flowEditorPane);

        List<NamedKeyboardMover> movers = getMovers();

        for (int i = 0; i < 12; i++)
        {
            int curPos = r.nextInt(content.length() + 1);
            int curAnchor = r.nextInt(10) == 1 ? curPos : r.nextInt(content.length() + 1);
            int initialPos = curPos;
            int initialAnchor = curAnchor;
            fx_(() -> {
                flowEditorPane.positionCaret(initialPos);
                flowEditorPane.positionAnchor(initialAnchor);
            });
            if (r.nextBoolean())
            {
                // Move selection with keyboard
                NamedKeyboardMover mover = movers.get(r.nextInt(movers.size()));
                press(KeyCode.SHIFT);
                curPos = mover.mover.move(curPos, content.length(), new AtomicInteger(-1));
                release(KeyCode.SHIFT);
            }            
            boolean deleteForward = r.nextBoolean();
            if (curPos == curAnchor)
            {
                // Will do it without selection; this is equivalent to a one char selection in that direction:
                if (deleteForward && curAnchor < content.length())
                    curAnchor += 1;
                else if (!deleteForward && curAnchor > 0)
                    curAnchor -= 1;
            }
            push(deleteForward ? KeyCode.DELETE : KeyCode.BACK_SPACE);

            int begin = Math.min(curAnchor, curPos);
            int end = Math.max(curAnchor, curPos);
            String newContent = content.substring(0, begin) + content.substring(end);
            assertEquals(newContent, fx(() -> flowEditorPane.getDocument().getFullContent()));
            // Caret should be at beginning of old selection afterwards:
            assertEquals(begin, fx(() -> flowEditorPane.getCaretPosition()).intValue());
            assertEquals(begin, fx(() -> flowEditorPane.getAnchorPosition()).intValue());
            content = newContent;
        }
    }

    @Property(trials = 5)
    public void testCutCopyPaste(@From(GenString.class) String rawContent, @From(GenRandom.class) Random r)
    {
        String content = removeInvalid(rawContent);
        setText(content);
        clickOn(flowEditorPane);

        for (int i = 0; i < 5; i++)
        {
            int curPos = r.nextInt(content.length() + 1);
            int initialPos = curPos;
            int curAnchor = r.nextInt(10) == 1 ? curPos : r.nextInt(content.length() + 1);
            fx_(() -> {
                flowEditorPane.positionCaret(initialPos);
                flowEditorPane.positionAnchor(curAnchor);
            });
            int curLineStart = content.lastIndexOf('\n', curPos - 1) + 1;
            int curLineEnd = content.indexOf('\n', curPos);
            if (curLineEnd == -1)
                curLineEnd = content.length();
            else
                curLineEnd += 1; // Go past the newline
            
            String selected = content.substring(Math.min(curPos, curAnchor), Math.max(curPos, curAnchor));
            String beforeSelected = content.substring(0, Math.min(curPos, curAnchor));
            String afterSelected = content.substring(Math.max(curPos, curAnchor));
            String withoutSelected = beforeSelected + afterSelected;
            String beforeLine = content.substring(0, curLineStart);
            String afterLine = content.substring(curLineEnd);
            String line = content.substring(curLineStart, curLineEnd);
            // Line should have no \n (if last line) or only last char should be \n:
            MatcherAssert.assertThat(line.indexOf('\n'), Matchers.either(Matchers.equalTo(line.length() - 1)).or(Matchers.equalTo(-1)));
            
            // Pick one of cut, copy, paste, delete, cut-line, cut-end-of-line, copy-line:
            int action = r.nextInt(7);
            switch (action)
            {
                case 0:
                    // Cut:
                    clearClipboard();
                    push(KeyCode.SHORTCUT, KeyCode.X);
                    String cut = fx(() -> Clipboard.getSystemClipboard().getString());
                    assertEquals(selected, cut);
                    assertEquals(withoutSelected, fx(() -> flowEditorPane.getDocument().getFullContent()));
                    content = withoutSelected;
                    break;
                case 1:
                    // Copy:
                    clearClipboard();
                    push(KeyCode.SHORTCUT, KeyCode.C);
                    String copied = fx(() -> Clipboard.getSystemClipboard().getString());
                    assertEquals(selected, copied);
                    assertEquals(content, fx(() -> flowEditorPane.getDocument().getFullContent()));
                    break;
                case 2:
                    String pasteContent = "Pa\u2248ste" + r.nextInt();
                    setClipboard(pasteContent);
                    push(KeyCode.SHORTCUT, KeyCode.V);
                    content = beforeSelected + pasteContent + afterSelected;
                    assertEquals(content, fx(() -> flowEditorPane.getDocument().getFullContent()));
                    break;
                case 3:
                    // Delete, if there is a selection:
                    if (curAnchor != curPos)
                    {
                        push(r.nextBoolean() ? KeyCode.DELETE : KeyCode.BACK_SPACE);
                        assertEquals(withoutSelected, fx(() -> flowEditorPane.getDocument().getFullContent()));
                        content = withoutSelected;
                    }
                    break;
                case 4:
                    // Cut whole caret line (ignoring anchor)
                    int cutCount = 1 + r.nextInt(3);
                    // Also check that repeated invocations add to clipboard
                    setClipboard("OldContent");
                    String prevClipboard = "";
                    for (int c = 0; c < cutCount; c++)
                    {
                        push(KeyCode.F4);
                        assertEquals(prevClipboard + line, fx(() -> Clipboard.getSystemClipboard().getString()));
                        prevClipboard += line;
                        content = beforeLine + afterLine;
                        assertEquals(content, fx(() -> flowEditorPane.getDocument().getFullContent()));

                        curPos = curLineStart;
                        curLineStart = content.lastIndexOf('\n', curPos - 1) + 1;
                        curLineEnd = content.indexOf('\n', curPos);
                        if (curLineEnd == -1)
                            curLineEnd = content.length();
                        else
                            curLineEnd += 1; // Go past the newline
                        beforeLine = content.substring(0, curLineStart);
                        afterLine = content.substring(curLineEnd);
                        line = content.substring(curLineStart, curLineEnd);
                    }
                    break;
                case 5:
                    // Cut to end of caret line (ignoring anchor)
                    clearClipboard();
                    fx_(() -> flowEditor.getActions().getActionByName("cut-end-of-line").actionPerformed(false));
                    assertEquals(content.substring(curPos, curLineEnd), fx(() -> Clipboard.getSystemClipboard().getString()));
                    content = beforeLine + content.substring(curLineStart, curPos) + afterLine;
                    assertEquals(content, fx(() -> flowEditorPane.getDocument().getFullContent()));
                    break;
                case 6:
                    // Copy whole caret line (ignoring anchor)
                    int copyCount = 1 + r.nextInt(3);
                    // Also check that repeated invocations add to clipboard
                    setClipboard("OldContent");
                    prevClipboard = "";
                    for (int c = 0; c < copyCount; c++)
                    {
                        push(KeyCode.F2);
                        assertEquals(prevClipboard + line, fx(() -> Clipboard.getSystemClipboard().getString()));
                        prevClipboard += line;
                        assertEquals(content, fx(() -> flowEditorPane.getDocument().getFullContent()));

                        curPos = curLineEnd;
                        curLineStart = content.lastIndexOf('\n', curPos - 1) + 1;
                        curLineEnd = content.indexOf('\n', curPos);
                        if (curLineEnd == -1)
                            curLineEnd = content.length();
                        else
                            curLineEnd += 1; // Go past the newline
                        line = content.substring(curLineStart, curLineEnd);
                    }
                    break;
            }
        }
    }
    
    @Property(trials = 3)
    public void testWordActions(@From(GenRandom.class) Random r)
    {
        List<String> words = List.of("java", "fooBar", "Kölling", "a", "63", "1_000", "a_B");
        List<String> nonWords = List.of(" ", "\n", "{", ".", "+", "    ");
        
        // Make a random String with no two words directly adjacent:
        boolean lastWasWord = false;
        String content = "";
        // Each element is a pair of start, end
        List<int[]> wordBoundaries = new ArrayList<>();
        for (int i = 0; i < 20; i++)
        {
            if (!lastWasWord && r.nextBoolean())
            {
                String nextWord = words.get(r.nextInt(words.size()));
                wordBoundaries.add(new int[] {content.length(), content.length() + nextWord.length()});
                content += nextWord;
                lastWasWord = true;
            }
            else
            {
                String nonWord = nonWords.get(r.nextInt(nonWords.size()));
                if (nonWord.trim().isEmpty() && wordBoundaries.size() > 0)
                {
                    wordBoundaries.get(wordBoundaries.size() - 1)[1] += nonWord.length();
                }
                else if (!nonWord.trim().isEmpty())
                {
                    wordBoundaries.add(new int[] {content.length(), content.length() + nonWord.length()});
                }
                content += nonWord;
                lastWasWord = false;
            }
        }
        
        setText(content);
        clickOn(flowEditorPane);
        fx_(() -> flowEditorPane.positionCaret(wordBoundaries.get(0)[0]));
        int expectedAnchor = wordBoundaries.get(0)[0];
        for (int[] wordBoundary : wordBoundaries)
        {
            int prevPos = fx(() -> flowEditorPane.getCaretPosition());
            boolean holdShift = r.nextInt(3) != 1;
            if (holdShift)
                press(KeyCode.SHIFT);
            push(Config.isMacOS() ? KeyCode.ALT : KeyCode.CONTROL, KeyCode.RIGHT);
            assertEquals("Word-Right after " + prevPos, wordBoundary[1], fx(() -> flowEditorPane.getCaretPosition()).intValue());
            if (holdShift)
                release(KeyCode.SHIFT);
            else
                expectedAnchor = wordBoundary[1];
            assertEquals("Anchor word-right after " + prevPos + " shift: " + holdShift, expectedAnchor, fx(() -> flowEditorPane.getAnchorPosition()).intValue());
        }

        // Go backwards this time:
        Collections.reverse(wordBoundaries);
        fx_(() -> flowEditorPane.positionCaret(wordBoundaries.get(0)[1]));
        expectedAnchor = wordBoundaries.get(0)[1];
        for (int[] wordBoundary : wordBoundaries)
        {
            int prevPos = fx(() -> flowEditorPane.getCaretPosition());
            boolean holdShift = r.nextInt(3) != 1;
            if (holdShift)
                press(KeyCode.SHIFT);
            push(Config.isMacOS() ? KeyCode.ALT : KeyCode.CONTROL, KeyCode.LEFT);
            assertEquals("Word-Left after " + prevPos, wordBoundary[0], fx(() -> flowEditorPane.getCaretPosition()).intValue());
            if (holdShift)
                release(KeyCode.SHIFT);
            else
                expectedAnchor = wordBoundary[0];
            assertEquals("Anchor word-left after " + prevPos + " shift: " + holdShift, expectedAnchor, fx(() -> flowEditorPane.getAnchorPosition()).intValue());
        }
    }
    
    private static final String BLOCK_TEST = 
        "public class Foo\n" +
                "{\n" +
                "    // Field comment\n" +
                "    private int field = 0;\n" +
                "\n" +
                "    public Foo()\n" +
                "    {\n" +
                "        if (true)\n" +
                "        {\n" +
                "            field = 1;\n" +
                "        }\n" +
                "        else\n" +
                "        {\n" +
                "            field = 0;\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    public int method(int x, int y)\n" +
                "    {\n" +
                "        while (true)\n" +
                "        {\n" +
                "            if (true)\n" +
                "                if (false)\n" +
                "                    break;\n" +
                "                else\n" +
                "                {\n" +
                "                    while (false)\n" +
                "                        field = 1;\n" +
                "                }\n" +
                "        }\n" +
                "    }\n" +
                "}";
    
    @Test
    public void testAutoIndent()
    {
        String unindented = BLOCK_TEST.replace("  ", "");
        setText(unindented);
        clickOn(flowEditorPane);
        // Do it twice to check if it produces same result on correctly indented code:
        for (int i = 0; i < 2; i++)
        {
            fx_(() -> {
                flowEditor.enableParser(true);
                flowEditor.getSourceDocument().flushReparseQueue();
                flowEditor.getActions().getActionByName("autoindent").actionPerformed(false);
            });
            sleep(1000);
            assertEquals(BLOCK_TEST, fx(() -> flowEditorPane.getDocument().getFullContent()));
        }
        // Check that undo/redo works (and that the second run above left no undo trace):
        push(KeyCode.SHORTCUT, KeyCode.Z);
        assertEquals(unindented, fx(() -> flowEditorPane.getDocument().getFullContent()));
        push(KeyCode.SHORTCUT, KeyCode.Y);
        assertEquals(BLOCK_TEST, fx(() -> flowEditorPane.getDocument().getFullContent()));
    }

    @Test
    public void testAddComment()
    {
        setText(BLOCK_TEST);
        clickOn(flowEditorPane);
        int indexOfMethod = BLOCK_TEST.indexOf("public int method");
        
        fx_(() -> {
            flowEditor.enableParser(true);
            flowEditor.getSourceDocument().flushReparseQueue();
            flowEditor.getSourcePane().positionCaret(indexOfMethod);
            flowEditor.getActions().getActionByName("add-javadoc").actionPerformed(false);
        });
        sleep(1000);
        String expected = BLOCK_TEST.substring(0, indexOfMethod - 4) +
            "    /**\n" +
            "     * Method method\n" +
            "     *\n" +
            "     * @param x A parameter\n" +
            "     * @param y A parameter\n" +
            "     * @return The return value\n" +
            "     */\n" + 
            BLOCK_TEST.substring(indexOfMethod - 4);
        
        assertEquals(expected, fx(() -> flowEditorPane.getDocument().getFullContent()));
    }

    @Test
    public void testAddMethod()
    {
        setText(BLOCK_TEST);
        clickOn(flowEditorPane);
        int indexOfGap = BLOCK_TEST.indexOf("\n    public int method");

        fx_(() -> {
            flowEditor.enableParser(true);
            flowEditor.getSourceDocument().flushReparseQueue();
            flowEditor.getSourcePane().positionCaret(indexOfGap);
            flowEditor.getActions().getActionByName("insert-method").actionPerformed(false);
        });
        sleep(1000);
        String expected = BLOCK_TEST.substring(0, indexOfGap) +
            "    /**\n" +
            "     * An example of a method - replace this comment with your own\n" +
            "     *\n" +
            "     * @param  y  a sample parameter for a method\n" +
            "     * @return    the sum of x and y\n" +
            "     */\n" +
            "    public int sampleMethod(int y)\n" +
            "    {\n" +
            "        // put your code here\n" +
            "        return y;\n" +
            "    }\n"  +
            BLOCK_TEST.substring(indexOfGap);

        assertEquals(expected, fx(() -> flowEditorPane.getDocument().getFullContent()));
    }

    @Property(trials = 2)
    public void testCommentUncomment(@From(GenRandom.class) Random r)
    {
        setText(BLOCK_TEST);
        clickOn(flowEditorPane);
        String history = "";
        List<String> docLines = Arrays.asList(Utility.splitLines(BLOCK_TEST));

        for (int i = 0; i < 5; i++)
        {
            // Make a selection of a random bunch of lines and comment and uncomment them (in a random order), then check the results:
            int lineA = r.nextInt(docLines.size());
            int lineB = r.nextInt(docLines.size());
            int startLine = Math.min(lineA, lineB);
            int endLine = Math.max(lineA, lineB);
            boolean comment = r.nextBoolean();
            history += (comment ? "Commented" : "Uncomment") + " lines " + startLine + " to " + endLine + " inclusive\n";

            // Make the selection:
            fx_(() -> {
                flowEditorPane.positionCaret(flowEditorPane.getDocument().getLineStart(startLine) + r.nextInt(docLines.get(startLine).length() + 1));
                flowEditorPane.moveCaret(flowEditorPane.getDocument().getLineStart(endLine) + 1 + (docLines.get(endLine).length() == 0 ? 0 : r.nextInt(docLines.get(endLine).length())));
            });
            // Double-check initial content:
            assertEquals(docLines.stream().collect(Collectors.joining("\n")), fx(() -> flowEditorPane.getDocument().getFullContent()));


            
            fx_(() -> flowEditor.getActions().getActionByName(comment ? "comment-block" : "uncomment-block").actionPerformed(false));
            for (int line = startLine; line <= endLine; line++)
            {
                docLines.set(line, addRemoveComment(docLines.get(line), comment));
            }
            assertEquals(history, docLines.stream().collect(Collectors.joining("\n")), fx(() -> flowEditorPane.getDocument().getFullContent()));
        }
    }

    private String addRemoveComment(String line, boolean addComment)
    {
        int firstNonWhitespace = findFirstNonWhitespaceAfter(line, 0);
        if (addComment && firstNonWhitespace < line.length())
        {
            return line.substring(0, firstNonWhitespace) + "// " + line.substring(firstNonWhitespace);
        }
        else if (!addComment && line.startsWith("//", firstNonWhitespace))
        {
            return line.substring(0, firstNonWhitespace) + line.substring(findFirstNonWhitespaceAfter(line, firstNonWhitespace + 2));
        }
        else
        {
            return line;
        }
    }

    private static int findFirstNonWhitespaceAfter(String line, int start)
    {
        int firstNonWhitespace = start;
        while (firstNonWhitespace < line.length() && Character.isWhitespace(line.charAt(firstNonWhitespace)))
        {
            firstNonWhitespace += 1;
        }
        return firstNonWhitespace;
    }

    @Property(trials = 2)
    public void testIndentAddRemove(@From(GenRandom.class) Random r)
    {
        setText(BLOCK_TEST);
        clickOn(flowEditorPane);
        List<String> docLines = Arrays.asList(Utility.splitLines(BLOCK_TEST));

        for (int i = 0; i < 5; i++)
        {
            // Make a selection of a random bunch of lines and indent and deindent them (in a random order), then check the results:
            int lineA = r.nextInt(docLines.size());
            int lineB = r.nextInt(docLines.size());
            int startLine = Math.min(lineA, lineB);
            int endLine = Math.max(lineA, lineB);

            // Make the selection:
            fx_(() -> {
                flowEditorPane.positionCaret(flowEditorPane.getDocument().getLineStart(startLine) + r.nextInt(docLines.get(startLine).length() + 1));
                flowEditorPane.moveCaret(flowEditorPane.getDocument().getLineStart(endLine) + 1 + (docLines.get(endLine).length() == 0 ? 0 : r.nextInt(docLines.get(endLine).length())));
            });
            // Double-check initial content:
            assertEquals(docLines.stream().collect(Collectors.joining("\n")), fx(() -> flowEditorPane.getDocument().getFullContent()));


            boolean indent = r.nextBoolean();
            fx_(() -> flowEditor.getActions().getActionByName(indent ? "indent-block" : "deindent-block").actionPerformed(false));
            for (int line = startLine; line <= endLine; line++)
            {
                docLines.set(line, addRemoveIndent(docLines.get(line), indent));
            }
            assertEquals((indent ? "Indented" : "Deindented") + " lines " + startLine + " to " + endLine + " inclusive", docLines.stream().collect(Collectors.joining("\n")), fx(() -> flowEditorPane.getDocument().getFullContent()));
        }
        
        // Autoindent to restore:
        fx_(() -> {
            flowEditor.enableParser(true);
            flowEditor.getSourceDocument().flushReparseQueue();
            flowEditor.getActions().getActionByName("autoindent").actionPerformed(false);
        });
        sleep(1000);
        assertEquals(BLOCK_TEST, fx(() -> flowEditorPane.getDocument().getFullContent()));
    }

    private String addRemoveIndent(String line, boolean addIndent)
    {
        if (addIndent)
        {
            return "    " + line;
        }
        else
        {
            int newStart = 0;
            for (int i = 0; i < 4; i++)
            {
                if (i < line.length() && line.charAt(i) == ' ')
                    newStart += 1;
                else
                    break;
            }
            return line.substring(newStart);
        }
    }

    private void setClipboard(String pasteContent)
    {
        fx_(() -> Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, pasteContent)));
    }

    private void clearClipboard()
    {
        fx_(() -> Clipboard.getSystemClipboard().setContent(Map.of()));
    }

    private List<NamedKeyboardMover> getMovers()
    {
        List<NamedKeyboardMover> movers = new ArrayList<>();

        movers.add(new NamedKeyboardMover("Ctrl-Home", (pos, len, tgt) -> {
            push(KeyCode.SHORTCUT, KeyCode.HOME);
            tgt.set(-1);
            return 0;
        }));
        movers.add(new NamedKeyboardMover("Ctrl-End", (pos, len, tgt) -> {
            push(KeyCode.SHORTCUT, KeyCode.END);
            tgt.set(-1);
            return len;
        }));
        movers.add(new NamedKeyboardMover("Home", (pos, len, tgt) -> {
            push(KeyCode.HOME);
            tgt.set(-1);
            String curContent = fx(() -> flowEditorPane.getDocument().getFullContent());
            int prevNewLine = curContent.lastIndexOf('\n', pos - 1);
            if (prevNewLine == -1)
            {
                return 0;
            }
            else
            {
                // Smart-home; moves to first non-whitespace unless already there
                int firstContent = prevNewLine + 1;
                // Don't go beyond end of document or end of line:
                while (firstContent < curContent.length() && Character.isWhitespace(curContent.charAt(firstContent)) &&  curContent.charAt(firstContent) != '\n')
                    firstContent += 1;
                if (pos == firstContent)
                {
                    return prevNewLine + 1;
                }
                else
                {
                    return firstContent;
                }
            }
        }));
        movers.add(new NamedKeyboardMover("End", (pos, len, tgt) -> {
            push(KeyCode.END);
            tgt.set(-1);
            String curContent = fx(() -> flowEditorPane.getDocument().getFullContent());
            int nextNewLine = curContent.indexOf('\n', pos);
            if (nextNewLine == -1)
                return len;
            else
                return nextNewLine;
        }));
        movers.add(new NamedKeyboardMover("Left", (pos, len, tgt) -> {
            push(KeyCode.LEFT);
            tgt.set(-1);
            return Math.max(0, pos - 1);
        }));
        movers.add(new NamedKeyboardMover("Right", (pos, len, tgt) -> {
            push(KeyCode.RIGHT);
            tgt.set(-1);
            return Math.min(len, pos + 1);
        }));
        KeyboardMover singleUp = (pos, len, tgt) -> {
            int expected = fx(() -> {
                Document document = flowEditorPane.getDocument();
                int curLine = document.getLineFromPosition(flowEditorPane.getCaretPosition());
                int curLineStart = document.getLineStart(curLine);
                if (curLineStart == 0)
                    return 0;
                int prevLineStart = document.getLineStart(curLine - 1);
                // Clamp if the column would be off the end of previous line:
                tgt.compareAndExchange(-1, pos - curLineStart);
                return Math.min(tgt.get() + prevLineStart, curLineStart - 1);
            });
            push(KeyCode.UP);
            return expected;
        };
        movers.add(new NamedKeyboardMover("Up", singleUp));
        // More likely to provoke issues remembering the target column:
        movers.add(new NamedKeyboardMover("Double Up", (pos, len, tgt) -> {
            int after = singleUp.move(pos, len, tgt);
            return singleUp.move(after, len, tgt);
        }));
        KeyboardMover singleDown = (pos, len, tgt) -> {
            int expected = fx(() -> {
                Document document = flowEditorPane.getDocument();
                int curLine = document.getLineFromPosition(flowEditorPane.getCaretPosition());
                int curLineStart = document.getLineStart(curLine);
                if (curLine == document.getLineCount() - 1)
                    return document.getLength();
                int nextLineStart = document.getLineStart(curLine + 1);
                int nextLineEnd = document.getLineEnd(curLine + 1);
                // Clamp if the column would be off the end of next line:
                tgt.compareAndExchange(-1, pos - curLineStart);
                return Math.min(tgt.get() + nextLineStart, nextLineEnd);
            });
            push(KeyCode.DOWN);
            return expected;
        };
        movers.add(new NamedKeyboardMover("Down", singleDown));
        // More likely to provoke issues remembering the target column:
        movers.add(new NamedKeyboardMover("Double Down", (pos, len, tgt) -> {
            int after = singleDown.move(pos, len, tgt);
            return singleDown.move(after, len, tgt);
        }));
        movers.add(new NamedKeyboardMover("Page Up", (pos, len, tgt) -> {
            int expected = fx(() -> {
                Document document = flowEditorPane.getDocument();
                int[] visibleRange = flowEditorPane.getLineRangeVisible();
                int numLines = visibleRange[1] - visibleRange[0];
                int curLine = document.getLineFromPosition(flowEditorPane.getCaretPosition());
                
                int curLineStart = document.getLineStart(curLine);
                if (curLine <= numLines)
                    return 0;
                int prevLineStart = document.getLineStart(curLine - numLines);
                // Clamp if the column would be off the end of previous line:
                tgt.compareAndExchange(-1, pos - curLineStart);
                return Math.min(tgt.get() + prevLineStart, document.getLineEnd(curLine - numLines));
            });
            push(KeyCode.PAGE_UP);
            return expected;
        }));
        movers.add(new NamedKeyboardMover("Page Down", (pos, len, tgt) -> {
            int expected = fx(() -> {
                Document document = flowEditorPane.getDocument();
                int[] visibleRange = flowEditorPane.getLineRangeVisible();
                int numLines = visibleRange[1] - visibleRange[0];
                int curLine = document.getLineFromPosition(flowEditorPane.getCaretPosition());

                int curLineStart = document.getLineStart(curLine);
                if (curLine + numLines >= document.getLineCount())
                    return document.getLength();
                int nextLineStart = document.getLineStart(curLine + numLines);
                // Clamp if the column would be off the end of previous line:
                tgt.compareAndExchange(-1, pos - curLineStart);
                return Math.min(tgt.get() + nextLineStart, document.getLineEnd(curLine + numLines));
            });
            push(KeyCode.PAGE_DOWN);
            return expected;
        }));
        return movers;
    }

    private void setText(String content)
    {
        fx_(() -> flowEditorPane.getDocument().replaceText(0, flowEditorPane.getDocument().getLength(), content));
        sleep(1000);
    }
}
