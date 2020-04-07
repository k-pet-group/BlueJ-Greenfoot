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
import bluej.collect.DiagnosticWithShown;
import bluej.collect.StrideEditReason;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.editor.Editor;
import bluej.editor.EditorWatcher;
import bluej.editor.flow.gen.GenRandom;
import bluej.editor.flow.gen.GenString;
import bluej.editor.stride.FrameCatalogue.ShowReason;
import bluej.parser.InitConfig;
import bluej.pkgmgr.Package;
import bluej.prefmgr.PrefMgr;
import bluej.stride.generic.Frame.View;
import bluej.stride.generic.Frame.ViewChangeReason;
import bluej.utility.Utility;
import com.google.common.io.Files;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnitQuickcheck.class)
public class TestBasicEditorDisplay extends FXTest
{
    private Stage stage;
    private FlowEditorPane flowEditorPane;
    private JavaSyntaxView javaSyntaxView;
    private FlowEditor flowEditor;

    @Override
    public void start(Stage stage) throws Exception
    {
        super.start(stage);
        
        InitConfig.init();
        Config.loadFXFonts();
        PrefMgr.setScopeHighlightStrength(100);
        PrefMgr.setFlag(PrefMgr.HIGHLIGHTING, true);
        PrefMgr.setEditorFontSize(12);
        
        this.stage = stage;
        flowEditor = new FlowEditor(w -> null, "", new EditorWatcher()
        {
            @Override
            public void modificationEvent(Editor editor)
            {
                
            }

            @Override
            public void saveEvent(Editor editor)
            {

            }

            @Override
            public void closeEvent(Editor editor)
            {

            }

            @Override
            public String breakpointToggleEvent(int lineNo, boolean set)
            {
                return null;
            }

            @Override
            public void generateDoc()
            {

            }

            @Override
            public void setProperty(String key, String value)
            {

            }

            @Override
            public String getProperty(String key)
            {
                return null;
            }

            @Override
            public Package getPackage() { return null; }

            @Override
            public void scheduleCompilation(boolean immediate, CompileReason reason, CompileType type)
            {

            }

            @Override
            public void recordJavaEdit(String javaSource, boolean includeOneLineEdits)
            {

            }

            @Override
            public void recordStrideEdit(String javaSource, String strideSource, StrideEditReason reason)
            {

            }

            @Override
            public void clearAllBreakpoints()
            {

            }

            @Override
            public void recordOpen()
            {

            }

            @Override
            public void recordSelected()
            {

            }

            @Override
            public void recordClose()
            {

            }

            @Override
            public void recordShowErrorIndicators(Collection<Integer> identifiers)
            {

            }

            @Override
            public void recordShowErrorMessage(int identifier, List<String> quickFixes)
            {

            }

            @Override
            public void recordEarlyErrors(List<DiagnosticWithShown> diagnostics, int compilationIdentifier)
            {

            }

            @Override
            public void recordLateErrors(List<DiagnosticWithShown> diagnostics, int compilationIdentifier)
            {

            }

            @Override
            public void recordFix(int errorIdentifier, int fixIndex)
            {

            }

            @Override
            public void recordCodeCompletionStarted(Integer lineNumber, Integer columnNumber, String xpath, Integer elementOffset, String stem, int codeCompletionId)
            {

            }

            @Override
            public void recordCodeCompletionEnded(Integer lineNumber, Integer columnNumber, String xpath, Integer elementOffset, String stem, String replacement, int codeCompletionId)
            {

            }

            @Override
            public void recordUnknownCommandKey(String enclosingFrameXpath, int cursorIndex, char key)
            {

            }

            @Override
            public void recordShowHideFrameCatalogue(String enclosingFrameXpath, int cursorIndex, boolean show, ShowReason reason)
            {

            }

            @Override
            public void recordViewModeChange(String enclosingFrameXpath, int cursorIndex, View oldView, View newView, ViewChangeReason reason)
            {

            }

            @Override
            public void showingInterface(boolean showingInterface)
            {

            }

            @Override
            public void showPreferences(int paneIndex)
            {

            }
        }, null, null, null, new ReadOnlyBooleanWrapper(true), true);
        flowEditorPane = flowEditor.getSourcePane();
        flowEditorPane.setPrefWidth(800.0 + MarginAndTextLine.TEXT_LEFT_EDGE);
        flowEditorPane.setMaxWidth(Region.USE_PREF_SIZE);
        flowEditorPane.setPrefHeight(600.0);
        flowEditorPane.setAllowScrollBars(false);
        ScopeColorsBorderPane scopeColors = flowEditor;
        scopeColors.scopeClassOuterColorProperty().set(Color.BLACK);
        scopeColors.scopeClassInnerColorProperty().set(Color.BLACK);
        scopeColors.scopeClassColorProperty().set(Color.GREEN);
        scopeColors.scopeMethodColorProperty().set(Color.YELLOW);
        scopeColors.scopeMethodOuterColorProperty().set(Color.BLACK);
        stage.setScene(new Scene(flowEditor));
        // We have to hide the text and caret to prevent them interfering with our examination
        // of the selection and scope backgrounds after taking a screenshot:
        File tmpStyleSheet = File.createTempFile("hide-foreground", "css");
        tmpStyleSheet.deleteOnExit();
        Files.write(".editor-text, .flow-caret { -fx-opacity: 0.01; } .text-line .flow-selection {-fx-fill: blue;-fx-stroke: null;}", tmpStyleSheet, StandardCharsets.UTF_8);
        stage.getScene().getStylesheets().add(tmpStyleSheet.toURI().toURL().toString());
        stage.show();
    }

    @Ignore("Flaky test turned off for CI")
    @Property(trials=3)
    public void testEditor(@From(GenString.class) String rawContent, @From(GenRandom.class) Random r)
    {
        String content = removeInvalid(rawContent);
        setText(content);

        List<String> lines = flowEditorPane.getDocument().getLines().stream().map(s -> s.toString()).collect(Collectors.toList());

        fx_(() -> flowEditorPane.positionCaret(0));
        checkVisibleLinesAgainst(lines);
        for (int i = 0; i < 3; i++)
        {
            int newTop = r.nextInt(lines.size());
            fx_(() -> flowEditorPane.scrollTo(newTop));
            // Wait for layout:
            sleep(200);
            checkVisibleLinesAgainst(lines.subList(newTop, lines.size()));
        }
        
        fx_(() -> {
            flowEditorPane.positionCaret(0);
            flowEditorPane.requestFocus();
        });

        Path caret = lookup(".flow-caret").query();
        int[] lineRangeVisible = flowEditorPane.getLineRangeVisible();
        int linesVisible = lineRangeVisible[0] == lineRangeVisible[1] ? 1 : lineRangeVisible[1] - lineRangeVisible[0];
        // Move the caret to line index "i" in each iteration of the loop, and check it is visible:
        for (int i = 0; i < Math.min(80, lines.size()); i++)
        {
            int iFinal = i;
            assertTrue("Line " + i + " should be visible, last range: " + lineRangeVisible[0] + " to " + lineRangeVisible[1], fx(() -> flowEditorPane.isLineVisible(iFinal)));
            push(KeyCode.DOWN);
            // Allow time for relayout:
            sleep(100);
            lineRangeVisible = fx(() -> flowEditorPane.getLineRangeVisible());
            // Check there's always the same number of lines visible, give or take a couple:
            assertThat(lineRangeVisible[1] - lineRangeVisible[0], between(linesVisible - 1, linesVisible + 1));
            double caretY = fx(() -> flowEditorPane.sceneToLocal(caret.localToScene(caret.getBoundsInLocal())).getCenterY());
            if (caretY < editorSnapshot(false).getHeight())
                assertThat((int)caretY, between(0, 600));
        }
        
        
        // We pick a bunch of random locations in the file, position the caret there,
        // scroll to make them visible, and then record the X, Y.  Then we scroll back to those locations
        // and click at that point, which should result in the original caret position.
        
        // Each array is <line index to scroll to>, <caret position in file>, <X pixels in screen>, <Y pixels in screen>
        List<SavedPosition> savedPositions = new ArrayList<>();
        for (int i = 0; i < 5; i++)
        {
            int lastLineWhichCanBeTop = Math.max(0, lines.size() - linesVisible);
            int topLine = r.nextInt(lastLineWhichCanBeTop + 1);
            fx_(() -> flowEditorPane.scrollTo(topLine));
            // Don't try to click on very last visible line as it may be only partially visible:
            int lineOfInterest = topLine + r.nextInt(linesVisible - 1);
            int columnOfInterest = r.nextInt(lines.get(lineOfInterest).length() + 1);
            int caretPos = fx(() -> {
                int p = flowEditorPane.getDocument().getLineStart(lineOfInterest) + columnOfInterest;
                flowEditorPane.positionCaretWithoutScrolling(p);
                return p;
            });
            sleep(200);
            // TODO what if the position requires a horizontal scroll?
            double caretX = fx(() -> caret.localToScreen(caret.getBoundsInLocal()).getCenterX());
            double caretY = fx(() -> caret.localToScreen(caret.getBoundsInLocal()).getCenterY());
            savedPositions.add(new SavedPosition(topLine, caretPos, (int)Math.round(caretX), (int)Math.round(caretY)));
        }

        for (SavedPosition savedPosition : savedPositions)
        {
            fx_(() -> {
                flowEditorPane.scrollTo(savedPosition.topLine);
            });
            sleep(200);
            clickOn(savedPosition.screenX, savedPosition.screenY);
            sleep(200);
            assertEquals("Clicked on " + savedPosition.screenX + ", " + savedPosition.screenY, savedPosition.caretPos, (int)fx(() -> flowEditorPane.getCaretPosition()));
        }
        
        // Turn off highlighting so that all text is black:
        PrefMgr.setFlag(PrefMgr.HIGHLIGHTING, false);

        int flowX = fx(() -> flowEditorPane.localToScreen(flowEditorPane.getBoundsInLocal()).getMinX()).intValue();
        int flowY = fx(() -> flowEditorPane.localToScreen(flowEditorPane.getBoundsInLocal()).getMinY()).intValue();
        for (int i = 0; i < 5; i++)
        {
            // We now pick two of the caret positions and make a selection between them, then check the background:
            // Either of these might be earlier in document, we scroll to the one we label "from":
            SavedPosition fromSaved = savedPositions.get(r.nextInt(savedPositions.size()));
            SavedPosition toSaved = savedPositions.get(r.nextInt(savedPositions.size()));
            fx_(() -> {
                flowEditorPane.scrollTo(fromSaved.topLine);
                flowEditorPane.positionCaretWithoutScrolling(toSaved.caretPos);
                flowEditorPane.positionAnchor(fromSaved.caretPos);
            });
            sleep(200);
            int toPosX = fx(() -> caret.getElements().isEmpty() ? flowX + MarginAndTextLine.TEXT_LEFT_EDGE + 1.0 : caret.localToScreen(caret.getBoundsInLocal()).getCenterX()).intValue();
            int toPosY = fx(() -> caret.getElements().isEmpty() ? flowY + 1.0 : caret.localToScreen(caret.getBoundsInLocal()).getCenterY()).intValue();
            sleep(200);
            WritableImage editorImage = editorSnapshot(true);
            boolean fromFirst = fromSaved.caretPos <= toSaved.caretPos;
            int firstY = fromFirst ? fromSaved.screenY : toPosY;
            int lastY = Math.min((int)editorImage.getHeight() - 1, fromFirst ? toPosY : fromSaved.screenY);

            int firstLine = fx(() -> flowEditorPane.getDocument().getLineFromPosition(fromFirst ? fromSaved.caretPos : toSaved.caretPos));
            int lastLine = fx(() -> flowEditorPane.getDocument().getLineFromPosition(fromFirst ? toSaved.caretPos : fromSaved.caretPos));

            int[] visibleLines = fx(() -> flowEditorPane.getLineRangeVisible());
            for (int line = visibleLines[0]; line <= visibleLines[1]; line++)
            {
                int lineFinal = line;
                Optional<double[]> lineBounds = fx(() -> flowEditorPane.getTopAndBottom(lineFinal));
                // Skip lines which are not visible:
                if (lineBounds.isEmpty())
                    continue;
                List<ColorTestArea> colorRegions = new ArrayList<>();
                // Within image, not screen:
                int y = (int)((lineBounds.get()[0] + lineBounds.get()[1]) / 2.0);
                if (y <= 0 || y >= editorImage.getHeight())
                    continue;
                
                
                if (line <= firstLine)
                {
                    // Look for white region before the selection (if any):
                    int x = line == firstLine ? ((fromFirst ? fromSaved.screenX : toPosX) - flowX - 10) : 10;
                    if (x >= 0)
                    {
                        colorRegions.add(new ColorTestArea(line == firstLine ? "White just before selection" : "White above selection", false, editorImage,
                                x, y));
                    }
                }
                if (line >= firstLine && line <= lastLine)
                {
                    // Look for start and end of a blue region:
                    int startX = (line == firstLine ? (fromFirst ? fromSaved.screenX : toPosX) : flowX + MarginAndTextLine.TEXT_LEFT_EDGE) + 10;
                    int endX = (line == lastLine ? (fromFirst ? toPosX : fromSaved.screenX) : flowX + (int)editorImage.getWidth()) - 10;
                    // If selection ends at the start of the line, will be very little blue, so skip it:
                    // Similarly, skip if selection is so small we won't pick up the blue:
                    if (endX <= flowX + MarginAndTextLine.TEXT_LEFT_EDGE + 5 || startX > endX - 10)
                    {
                        
                    }
                    else
                    {
                        colorRegions.add(new ColorTestArea("Blue left in selection", true, editorImage, startX - flowX, y));
                        colorRegions.add(new ColorTestArea("Blue right in selection", true, editorImage, endX - flowX, y));
                    }
                }
                if (line >= lastLine)
                {
                    if (line > lastLine || (fromFirst ? toPosX : fromSaved.screenX) < editorImage.getWidth() - 10)
                    {
                        colorRegions.add(new ColorTestArea("White after selection", false, editorImage, (int) editorImage.getWidth() - 5, y));
                    }
                }

                int selectionFrom = Math.min(fromSaved.caretPos, toSaved.caretPos);
                String selSnippet = content.substring(selectionFrom, Math.min(selectionFrom + 10, content.length()));
                for (ColorTestArea colorRegion : colorRegions)
                {
                    colorRegion.check(selSnippet);
                }
            }
        }
        // TODO test clicking, caret and selection display (especially when one or both ends off-screen)
        
    }
    
    private static List<Color> getColoredArea(WritableImage image, int x, int y)
    {
        List<Color> r = new ArrayList<>();
        for (int dx = -4; dx < 4; dx++)
        {
            for (int dy = -4; dy < 4; dy++)
            {
                int tx = x + dx;
                int ty = y + dy;
                if (tx >= 0 && ty >= 0 && tx < image.getWidth() && ty < image.getHeight())
                {
                    r.add(image.getPixelReader().getColor(tx, ty));
                }
            }
        }
        return r;
    }
    
    private static class ColorTestArea
    {
        private final String description;
        private final boolean expectBlue;
        private final WritableImage image;
        private final int x;
        private final int y;

        public ColorTestArea(String description, boolean expectBlue, WritableImage image, int x, int y)
        {
            this.description = description;
            this.expectBlue = expectBlue;
            this.image = image;
            this.x = x;
            this.y = y;
        }
        
        public void check(String selectionSnippet)
        {
            Stream<Color> nonBlack = getColoredArea(image, x, y).stream().filter(c -> c.getRed() + c.getGreen() + c.getBlue() > 0.05);
            // Last item is count, not alpha.  We can't accumulate in Color as it is clamped to 0-1
            // valid range, so we use an array:
            Color average = nonBlack.map(c -> new double[]{c.getRed(), c.getGreen(), c.getBlue(), 1})
                    .reduce((a, b) -> new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2], a[3] + b[3]})
                    .map(a -> Color.color(a[0] / a[3], a[1] / a[3], a[2] / a[3])).orElseThrow(() -> new RuntimeException("No pixels available for " + x + ", " + y));

            String fullDescription = description + " x : " + x + " y : " + y + "{{" + selectionSnippet + "}}";
            if (expectBlue)
            {
                assertThat(fullDescription, average.getBlue(), Matchers.greaterThan(average.getGreen() + 0.1));
                assertThat(fullDescription, average.getBlue(), Matchers.greaterThan(average.getRed() + 0.1));
            }
            else
            {
                // Otherwise, should be white:
                assertThat(fullDescription, average.getBlue(), Matchers.closeTo(average.getGreen(), 0.1));
                assertThat(fullDescription, average.getBlue(), Matchers.closeTo(average.getRed(), 0.1));
            }
        }
    }


    private void setTextLines(String... lines)
    {
        setText(Arrays.stream(lines).collect(Collectors.joining("\n")));
    }

    private void setText(String content)
    {
        fx_(() -> {
            flowEditorPane.getDocument().replaceText(0, flowEditorPane.getDocument().getLength(), content);
            flowEditor.enableParser(true);
        });
        sleep(1000);
    }

    /**
     * Checks that the lines in the visible editor window match with the start
     * of the given list.  (The list may be longer than what is shown in the GUI
     * window, and the test will still pass.)
     * @param lines
     */
    @OnThread(Tag.Any)
    private void checkVisibleLinesAgainst(List<String> lines)
    {
        List<MarginAndTextLine> guiLines = fx(() -> {
            return flowEditorPane.lookupAll(".margin-and-text-line").stream().sorted(Comparator.comparing(Node::getLayoutY)).map(t -> (MarginAndTextLine)t).collect(Collectors.toList());
        });

        // Check that text lines are there in order
        List<String> guiLineContent = fx(() -> Utility.mapList(guiLines, this::getAllText));

        // May not show all lines if document is truncated:
        for (int i = 0; i < guiLineContent.size(); i++)
        {
            assertEquals(lines.get(i), guiLineContent.get(i));
        }
        // Check lines cover full height of window, unless document is too short:
        assertThat(guiLines.get(0).getLayoutY(), Matchers.lessThanOrEqualTo(0.0));
        if (lines.size() > guiLines.size())
        {
            assertThat(guiLines.get(guiLines.size() - 1).getLayoutY() 
                            + guiLines.get(guiLines.size() - 1).getHeight(),
                Matchers.greaterThanOrEqualTo(flowEditorPane.getHeight()));
        }
        else
        {
            assertEquals(lines.size(), guiLines.size());
        }
        // Check lines have less than one pixel gap:
        for (int i = 1; i < guiLines.size(); i++)
        {
            double bottomPrev = guiLines.get(i - 1).getLayoutBounds().getMaxY();
            double topCur = guiLines.get(i).getLayoutBounds().getMinY();
            assertThat(topCur, Matchers.lessThanOrEqualTo(bottomPrev + 1.0));
        }
    }

    @OnThread(Tag.FXPlatform)
    private String getAllText(MarginAndTextLine node)
    {
        return ((TextFlow)node.lookup(".text-line")).getChildren().stream().filter(c -> c instanceof Text).map(c -> ((Text)c).getText()).collect(Collectors.joining());
    }
    
    @Test
    public void testScope()
    {
        String beforeEnterPoint = "public class Foo\n{\n    public void method() {\n        int x = 8;\n        ";
        String afterEnterPoint = "\n    }\n}\n";
        setText(beforeEnterPoint + afterEnterPoint);
        fx_(() -> flowEditorPane.requestFocus());
        fx_(() -> flowEditorPane.positionCaret(beforeEnterPoint.length()));
        sleep(300);
        // Find the caret Y:
        Node caret = lookup(".flow-caret").query();
        double y = fx(() -> flowEditorPane.sceneToLocal(caret.localToScene(caret.getBoundsInLocal())).getCenterY());
        
        // Check initial scopes:
        checkScopes(6, scope(Color.GREEN, between(0, 2), between(780, 800)));
        checkScopes((int)y, 
            scope(Color.GREEN, between(0, 2), between(36, 40)),
            scope(Color.YELLOW, between(36, 40), between(75, 80))
        );
        for (int i = 0; i < 10; i++)
        {
            push(KeyCode.ENTER);
            sleep(150);
            assertEquals(beforeEnterPoint + "\n        ".repeat(i + 1) + afterEnterPoint, fx(() -> flowEditorPane.getDocument().getFullContent()));
            // Scopes should still be the same:
            checkScopes(6, scope(Color.GREEN, between(0, 2), between(780, 800)));
            y = fx(() -> flowEditorPane.sceneToLocal(caret.localToScene(caret.getBoundsInLocal())).getCenterY());
            checkScopes((int) y,
                scope(Color.GREEN, between(0, 2), between(36, 40)),
                scope(Color.YELLOW, between(36, 40), between(75, 80))
            );
        }
        for (int i = 0; i < 30; i++)
        {
            push(KeyCode.ENTER);
        }
        y = fx(() -> flowEditorPane.sceneToLocal(caret.localToScene(caret.getBoundsInLocal())).getCenterY());
        // Now, the top should have scrolled off, so should be nested scopes at top:
        checkScopes((int) 6,
            scope(Color.GREEN, between(0, 2), between(36, 40)),
            scope(Color.YELLOW, between(36, 40), between(75, 80))
        );
        checkScopes((int) y,
            scope(Color.GREEN, between(0, 2), between(36, 40)),
            scope(Color.YELLOW, between(36, 40), between(75, 80))
        );
        
        // Get back to top:
        push(KeyCode.PAGE_UP);
        push(KeyCode.PAGE_UP);
        checkScopes(6, scope(Color.GREEN, between(0, 2), between(780, 800)));
        fx_(() -> flowEditorPane.positionCaret(beforeEnterPoint.length()));
        sleep(500);

        for (int i = 0; i < 40; i++)
        {
            // Check scope position as we scroll down:
            if (i == 0)
                checkScopes(6, scope(Color.GREEN, between(0, 2), between(780, 800)));
            y = fx(() -> flowEditorPane.sceneToLocal(caret.localToScene(caret.getBoundsInLocal())).getCenterY());
            if (y < editorSnapshot(false).getHeight())
            {
                checkScopes((int) y,
                        scope(Color.GREEN, between(0, 2), between(36, 40)),
                        scope(Color.YELLOW, between(36, 40), between(75, 80))
                );
            }
            push(KeyCode.DOWN);
            sleep(100);
        }
        
    }

    @Test
    public void testScope2()
    {
        String beforeEnterPoint = "/**\n" +
                " * Write a description of class Main here.\n" +
                " *\n" +
                " * @author (your name)\n" +
                " * @version (a version number or a date)\n" +
                " */\n" +
                "public class Main\n" +
                "{\n" +
                "    // instance variables - replace the example below with your own\n" +
                "    private int x;\n" +
                "\n" +
                "    /**\n" +
                "     * Constructor for objects of class Main\n" +
                "     */\n" +
                "    public Main()\n" +
                "    {\n" +
                "        // initialise instance variables\n" +
                "        x = 0;\n";
        String afterEnterPoint = "\n" +
                "    }\n" +
                "\n" +
                "    /**\n" +
                "     * An example of a method - replace this comment with your own\n" +
                "     *\n" +
                "     * @param  y  a sample parameter for a method\n" +
                "     * @return    the sum of x and y\n" +
                "     */\n" +
                "    public int sampleMethod(int y)\n" +
                "    {\n" +
                "        // put your code here\n" +
                "        return x + y;\n" +
                "    }\n" +
                "}";
        
        setText(beforeEnterPoint + afterEnterPoint);
        fx_(() -> flowEditorPane.requestFocus());
        fx_(() -> flowEditorPane.positionCaret(beforeEnterPoint.length()));
        sleep(300);
        // Find the caret Y:
        Node caret = lookup(".flow-caret").query();
        double y = fx(() -> flowEditorPane.sceneToLocal(caret.localToScene(caret.getBoundsInLocal())).getCenterY());

        double methodHeaderY = getYPosForRelLine(caret, -4);

        // Check initial scopes:
        checkScopes(6, scope(Color.GREEN, between(0, 2), between(780, 800)));
        checkScopes((int) methodHeaderY,
                scope(Color.GREEN, between(0, 2), between(35, 40)),
                scope(Color.YELLOW, between(35, 40), between(780, 800))
        );
        checkScopes((int) y,
            scope(Color.GREEN, between(0, 2), between(35, 40)),
            scope(Color.YELLOW, between(35, 40), between(75, 80))
        );
        push(KeyCode.ENTER);
        push(KeyCode.HOME);
        assertEquals(19, fx(() -> flowEditorPane.getDocument().getLineFromPosition(flowEditorPane.getCaretPosition())).intValue());
        assertEquals(0, fx(() -> flowEditorPane.getDocument().getColumnFromPosition(flowEditorPane.getCaretPosition())).intValue());
        // There may be auto-indent, but the new line should be there at least:
        assertThat(fx(() -> flowEditorPane.getDocument().getFullContent()), Matchers.startsWith(beforeEnterPoint + "\n"));
        write(" y");
        sleep(500);
        // Check scopes got pushed to delete:
        checkScopes(6, scope(Color.GREEN, between(0, 2), between(780, 800)));
        checkScopes((int) methodHeaderY,
            scope(Color.GREEN, between(0, 2), between(35, 40)),
            scope(Color.YELLOW, between(35, 40), between(780, 800))
        );
        checkScopes((int)y,
            scope(Color.GREEN, between(0, 2), between(5, 10)),
            // The yellow will only be visible on RHS:
            scope(Color.YELLOW, between(770, 800), between(780, 800))
        );
        push(KeyCode.BACK_SPACE);
        sleep(500);
        // Check scopes back to same as initial:
        checkScopes(6, scope(Color.GREEN, between(0, 2), between(780, 800)));
        checkScopes((int) y,
                scope(Color.GREEN, between(0, 2), between(35, 40)),
                scope(Color.YELLOW, between(35, 40), between(75, 80))
        );
    }

    @Test
    public void testScope3()
    {
        String beforeEnterPoint =
                "public class Main\n" +
                "{\n" +
                "    /**\n" +
                "     * Compile the currently selected class targets.\n" +
                "     */\n" +
                "    public void compileSelected()\n" +
                "    {\n" +
                "        Package thePkg = getPackage();\n" +
                "        List<Target> targets = thePkg.getSelectedTargets();\n" +
                "        if (targets.size() > 0) {\n" +
                "            for (Target target : targets) {\n" +
                "                if (target instanceof ClassTarget) {\n" +
                "                    ClassTarget t = (ClassTarget) target;\n" +
                "                    if (t.hasSourceCode())\n" +
                "                        thePkg.compile(t, CompileReason.USER, CompileType.EXPLICIT_USER_COMPILE);\n" +
                "                    ";
        String afterEnterPoint="\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        else {\n" +
                "            DialogManager.showErrorFX(getFXWindow(), \"no-class-selected-compile\");\n" +
                "        }\n" +
                "    }\n" +
                "}\n";

        setText(beforeEnterPoint + afterEnterPoint);
        fx_(() -> flowEditorPane.requestFocus());
        fx_(() -> flowEditorPane.positionCaret(beforeEnterPoint.length()));
        sleep(300);
        // Find the caret Y:
        Node caret = lookup(".flow-caret").query();
        double y = fx(() -> flowEditorPane.sceneToLocal(caret.localToScene(caret.getBoundsInLocal())).getCenterY());

        final int linesUpToHeader = -10;
        double methodHeaderY = getYPosForRelLine(caret, linesUpToHeader);
        final int linesUpToMethodInner = -7;
        double methodInnerY = getYPosForRelLine(caret, linesUpToMethodInner);

        // Check initial scopes:
        checkScopes(6, scope(Color.GREEN, between(0, 2), between(780, 800)));
        checkScopes((int) methodHeaderY,
                scope(Color.GREEN, between(0, 2), between(35, 40)),
                scope(Color.YELLOW, between(35, 40), between(780, 800))
        );
        checkScopes((int) methodInnerY,
                scope(Color.GREEN, between(0, 2), between(35, 40)),
                scope(Color.YELLOW, between(35, 40), between(75, 80))
        );
        checkScopes((int) y,
                scope(Color.GREEN, between(0, 2), between(35, 40)),
                scope(Color.YELLOW, between(35, 40), between(75, 80))
        );
        push(KeyCode.ENTER);
        push(KeyCode.HOME);
        assertEquals(beforeEnterPoint.lines().count(), fx(() -> flowEditorPane.getDocument().getLineFromPosition(flowEditorPane.getCaretPosition())).intValue());
        assertEquals(0, fx(() -> flowEditorPane.getDocument().getColumnFromPosition(flowEditorPane.getCaretPosition())).intValue());
        // There may be auto-indent, but the new line should be there at least:
        assertThat(fx(() -> flowEditorPane.getDocument().getFullContent()), Matchers.startsWith(beforeEnterPoint + "\n"));
        write(" y");
        sleep(500);
        // Check scopes got pushed left:
        checkScopes(6, scope(Color.GREEN, between(0, 2), between(780, 800)));
        checkScopes((int) methodHeaderY,
                scope(Color.GREEN, between(0, 2), between(35, 40)),
                scope(Color.YELLOW, between(35, 40), between(780, 800))
        );
        checkScopes((int) methodInnerY,
                scope(Color.GREEN, between(0, 2), between(35, 40)),
                scope(Color.YELLOW, between(35, 40), between(75, 80))
        );
        checkScopes((int)y,
                scope(Color.GREEN, between(0, 2), between(5, 10)),
                // The yellow will only be visible on RHS:
                scope(Color.YELLOW, between(770, 800), between(780, 800))
        );
        push(KeyCode.BACK_SPACE);
        sleep(500);
        // Check scopes back to same as initial:
        checkScopes(6, scope(Color.GREEN, between(0, 2), between(780, 800)));
        checkScopes((int) methodInnerY,
                scope(Color.GREEN, between(0, 2), between(35, 40)),
                scope(Color.YELLOW, between(35, 40), between(75, 80))
        );
        checkScopes((int) y,
                scope(Color.GREEN, between(0, 2), between(35, 40)),
                scope(Color.YELLOW, between(35, 40), between(75, 80))
        );
    }

    @Test
    public void testScope4()
    {
        // Check that adding newlines inside a method does not ruin the scopes later on:
        
        String beforeEnterPoint =
                "/**\n" +
                " * Write a description of class Basic here.\n" +
                " *\n" +
                " * @author (your name)\n" +
                " * @version (a version number or a date)\n" +
                " */\n" +
                "public class Basic\n" +
                "{\n" +
                "    // instance variables - replace the example below with your own\n" +
                "    private int x;\n" +
                "\n" +
                "    /**\n" +
                "     * Constructor for objects of class Basic\n" +
                "     */\n" +
                "    public Basic()\n" +
                "    {\n" +
                "        // initialise instance variables\n" +
                "        x = 0;";
        String afterEnterPoint="\n" +
            "    }\n" +
            "    \n" +
            "    public static void m()\n" +
            "    {\n" +
            "        int x = 1;\n" +
            "        x = 2;\n" +
            "        x = 3;\n" +
            "        System.out.println(\"a\");\n" +
            "        System.out.println(\"b\");\n" +
            "        System.out.println(\"c\");\n" +
            "    }\n" +
            "}\n";

        setText(beforeEnterPoint + afterEnterPoint);
        fx_(() -> flowEditorPane.requestFocus());
        fx_(() -> flowEditorPane.positionCaret(beforeEnterPoint.length()));
        sleep(300);
        // Find the caret Y:
        Node caret = lookup(".flow-caret").query();
        double mainY = fx(() -> flowEditorPane.sceneToLocal(caret.localToScene(caret.getBoundsInLocal())).getCenterY());
        double gapY = getYPosForRelLine(caret, 2);
        double methodAfterY = getYPosForRelLine(caret, 5);

        for (int i = 0; i < 3; i++)
        {
            // Check scopes:
            checkScopes(6, scope(Color.GREEN, between(0, 2), between(780, 800)));
            checkScopes((int) mainY,
                scope(Color.GREEN, between(0, 2), between(35, 40)),
                scope(Color.YELLOW, between(35, 40), between(75, 80))
            );
            checkScopes((int) gapY,
                scope(Color.GREEN, between(0, 2), between(35, 40)),
                scope(Color.GREEN, between(780, 800), between(780, 800))
            );
            checkScopes((int) methodAfterY,
                scope(Color.GREEN, between(0, 2), between(35, 40)),
                scope(Color.YELLOW, between(35, 40), between(75, 80))
            );
            // Add and remove lines:
            push(KeyCode.ENTER);
            push(KeyCode.ENTER);
            sleep(500);
            push(KeyCode.SHIFT, KeyCode.UP);
            push(KeyCode.SHIFT, KeyCode.END);
            push(KeyCode.DELETE);
            push(KeyCode.SHIFT, KeyCode.UP);
            push(KeyCode.SHIFT, KeyCode.END);
            push(KeyCode.DELETE);
            sleep(500);
        }
    }

    // Gets y position for a line above/below this one
    private double getYPosForRelLine(Node caret, int lineDistance)
    {
        for (int i = 0; i < Math.abs(lineDistance); i++)
        {
            push(lineDistance < 0 ? KeyCode.UP : KeyCode.DOWN);
        }
        sleep(200);
        double y = fx(() -> flowEditorPane.sceneToLocal(caret.localToScene(caret.getBoundsInLocal())).getCenterY());
        for (int i = 0; i < Math.abs(lineDistance); i++)
        {
            push(lineDistance < 0 ? KeyCode.DOWN : KeyCode.UP);
        }
        return y;
    }


    private static Matcher<Integer> between(int low, int high)
    {
        return Matchers.both(Matchers.greaterThanOrEqualTo(low)).and(Matchers.lessThanOrEqualTo(high));
    }

    private boolean isGrey(Color c)
    {
        return Math.abs(c.getRed() - c.getBlue()) < 0.01 && Math.abs(c.getGreen() - c.getBlue()) < 0.01;
    }

    private void checkScopes(int y, Scope... scopes)
    {
        // Take screenshot of background:
        WritableImage image = editorSnapshot(false);
        assertThat((Integer)y, Matchers.lessThan((int)image.getHeight()));
        // Go from LHS:
        int scopeIndex = 0;
        boolean inScope = false;
        Color scopeColor = Color.TURQUOISE;
        int scopeStartX = 0;
        for (int x = 0; x < image.getWidth() && scopeIndex < scopes.length; x += 1)
        {
            Color c = image.getPixelReader().getColor(x, y);
            if (isGrey(c) && inScope)
            {
                // End of scope
                // We don't always get exactly the same colour, so have some tolerance: 
                try
                {
                    String coords = "At " + x + ", " + y;
                    assertThat(coords, scopeColor.getRed(), Matchers.closeTo(scopes[scopeIndex].expectedColor.getRed(), 0.03));
                    assertThat(coords, scopeColor.getGreen(), Matchers.closeTo(scopes[scopeIndex].expectedColor.getGreen(), 0.03));
                    assertThat(coords, scopeColor.getBlue(), Matchers.closeTo(scopes[scopeIndex].expectedColor.getBlue(), 0.03));
                    assertThat(coords, scopeStartX, scopes[scopeIndex].lhsCheck);
                    assertThat(coords, x - 1, scopes[scopeIndex].rhsCheck);
                }
                catch (AssertionError e)
                {
                    System.out.println("Failing editor image:\n" + asBase64(image));
                    throw e;
                }
                inScope = false;
                scopeIndex += 1;
            }
            else if (!isGrey(c) && !inScope)
            {
                scopeStartX = x - 1;
                scopeColor = c;
                inScope = true;
            }
        }
        assertEquals("All scopes found at " + y, scopes.length, scopeIndex);
    }

    @OnThread(Tag.Any)
    private WritableImage editorSnapshot(boolean includeMargin)
    {
        return fx(() -> {
            SnapshotParameters params = new SnapshotParameters();
            // If we don't want the margin in the snapshot:
            if (!includeMargin)
            {
                params.setViewport(new Rectangle2D(MarginAndTextLine.TEXT_LEFT_EDGE, 0, 800, 600));
            }
            else
            {
                params.setViewport(new Rectangle2D(0, 0, 800 + MarginAndTextLine.TEXT_LEFT_EDGE, 600));
            }
            return flowEditorPane.snapshot(params, null);
        });
    }

    private Scope scope(Color expectedColor, Matcher<Integer> lhsCheck, Matcher<Integer> rhsCheck)
    {
        return new Scope(expectedColor, lhsCheck, rhsCheck);
    }
    
    private static class Scope
    {
        private final Color expectedColor;
        private final Matcher<Integer> lhsCheck;
        private final Matcher<Integer> rhsCheck;

        public Scope(Color expectedColor, Matcher<Integer> lhsCheck, Matcher<Integer> rhsCheck)
        {
            this.expectedColor = expectedColor;
            this.lhsCheck = lhsCheck;
            this.rhsCheck = rhsCheck;
        }
    }
    
    @Test
    public void testSyntax()
    {
        setText("public class Bar {}");
        checkTokens("$keyword1#public$ $keyword2#class$ Bar {}");

        setText("// public class Commented {}");
        checkTokens("$comment-normal#// public class Commented {}");
        
        setTextLines(
            "class MyClass",
            "{",
            "    /** A Javadoc comment",
            "    split over two lines like this.*/",
            "    public static int var() { return 0; }",
            "}");
        checkTokensLines(
            "$keyword2#class$ MyClass",
            "{",
            "    $comment-javadoc#/** A Javadoc comment",
            "$comment-javadoc#    split over two lines like this.*/$",
            "    $keyword1#public$ $keyword1#static$ $primitive#int$ var() { $keyword1#return$ 0; }",
            "}"
        );

        setTextLines(
                "class A {",
                "    /** this field */",
                "    int x = 8;}");
        checkTokensLines(
                "$keyword2#class$ A {",
                "    $comment-javadoc#/** this field */$",
                "    $primitive#int$ x = 8;}"
        );    
    }


    private void checkTokensLines(String... expectedLines)
    {
        checkTokens(Arrays.stream(expectedLines).collect(Collectors.joining("\n")));
    }
    
    private void checkTokens(String expected)
    {
        // Each outer list is a line, each inner list is a list of expected Text items
        List<List<Consumer<Text>>> contentCheckers = Arrays.stream(expected.split("\n")).map(line -> Arrays.stream(line.split("\\$")).filter(s -> !s.isEmpty()).<Consumer<Text>>map(seg -> {
            if (seg.contains("#"))
            {
                // First part is CSS classes, last part is actual text.
                String[] subsegs = seg.split("#");
                return t -> {
                    assertEquals(subsegs[subsegs.length - 1], t.getText());
                    for (int i = 0; i < subsegs.length - 1; i++)
                    {
                        assertThat(t.getStyleClass(), Matchers.hasItem("token-" + subsegs[i]));
                    }
                };
            }
            else
            {
                return t -> assertEquals(seg, t.getText());
            }
        }).collect(Collectors.toList())).collect(Collectors.toList());
        
        List<TextLine> lines = flowEditorPane.lookupAll(".text-line").stream().map(l -> (TextLine)l).sorted(Comparator.comparing(n -> n.getLayoutY())).collect(Collectors.toList());

        assertEquals(contentCheckers.size(), lines.size());
        for (int i = 0; i < lines.size(); i++)
        {
            List<Consumer<Text>> segmentCheckers = contentCheckers.get(i);
            List<Text> actualSegments = lines.get(i).getChildren().stream().filter(t -> t instanceof Text).map(t -> (Text)t).collect(Collectors.toList());
            assertEquals(actualSegments.stream().map(Text::getText).collect(Collectors.joining()), segmentCheckers.size(), actualSegments.size());
            for (int j = 0; j < actualSegments.size(); j++)
            {
                segmentCheckers.get(j).accept(actualSegments.get(j));
            }
        }
    }

    private static class SavedPosition
    {
        private final int topLine;
        private final int caretPos;
        private final int screenX;
        private final int screenY;

        public SavedPosition(int topLine, int caretPos, int screenX, int screenY)
        {
            this.topLine = topLine;
            this.caretPos = caretPos;
            this.screenX = screenX;
            this.screenY = screenY;
        }
    }
}
