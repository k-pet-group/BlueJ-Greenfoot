package bluej.editor.flow;

import bluej.Config;
import bluej.editor.flow.gen.GenRandom;
import bluej.editor.flow.gen.GenString;
import bluej.editor.moe.ScopeColorsBorderPane;
import bluej.parser.InitConfig;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.When;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

@RunWith(JUnitQuickcheck.class)
public class TestBasicEditorInteraction extends FXTest
{
    private Stage stage;
    private FlowEditorPane flowEditorPane;

    @Override
    public void start(Stage stage) throws Exception
    {
        super.start(stage);

        InitConfig.init();
        Config.loadFXFonts();
        PrefMgr.setScopeHighlightStrength(100);
        PrefMgr.setFlag(PrefMgr.HIGHLIGHTING, true);

        this.stage = stage;
        FlowEditor flowEditor = new FlowEditor(null, null);
        flowEditorPane = flowEditor.getSourcePane();
        flowEditorPane.setPrefWidth(800.0);
        flowEditorPane.setPrefHeight(600.0);
        stage.setScene(new Scene(flowEditor));
        stage.show();
    }
    
    interface KeyboardMover
    {
        // Moves to new pos and returns expected position.  For convenience, current position and length are given
        // Also, the mutable target column (used for up/down movements) is passed, which may be read from and/or written to.
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
                return 0;
            else
                return prevNewLine + 1;
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
