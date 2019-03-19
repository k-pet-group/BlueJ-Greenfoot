/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019  Michael Kolling and John Rosenberg

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
import bluej.editor.moe.ScopeColorsBorderPane;
import bluej.parser.InitConfig;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.Utility;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.When;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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

    @Override
    public void start(Stage stage) throws Exception
    {
        super.start(stage);
        
        InitConfig.init();
        Config.loadFXFonts();
        PrefMgr.setScopeHighlightStrength(100);
        PrefMgr.setFlag(PrefMgr.HIGHLIGHTING, true);
        
        this.stage = stage;
        flowEditorPane = new FlowEditorPane("");
        flowEditorPane.setPrefWidth(800.0);
        flowEditorPane.setPrefHeight(600.0);
        ScopeColorsBorderPane scopeColors = new ScopeColorsBorderPane();
        scopeColors.scopeClassOuterColorProperty().set(Color.BLACK);
        scopeColors.scopeClassInnerColorProperty().set(Color.BLACK);
        scopeColors.scopeClassColorProperty().set(Color.GREEN);
        scopeColors.scopeMethodColorProperty().set(Color.YELLOW);
        scopeColors.scopeMethodOuterColorProperty().set(Color.BLACK);
        javaSyntaxView = new JavaSyntaxView(flowEditorPane, scopeColors);
        stage.setScene(new Scene(flowEditorPane));
        stage.show();
    }

    @Property(trials=5)
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
        int linesVisible = lineRangeVisible[1] - lineRangeVisible[0];
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
            assertThat((int)caretY, between(0, 600));
        }
        
        
        // We pick a bunch of random locations in the file, position the caret there,
        // scroll to make them visible, and then record the X, Y.  Then we scroll back to those locations
        // and click at that point, which should result in the original caret position.
        
        // Each array is <line index to scroll to>, <caret position in file>, <X pixels in screen>, <Y pixels in screen>
        List<int[]> savedPositions = new ArrayList<>();
        for (int i = 0; i < 5; i++)
        {
            int lastLineWhichCanBeTop = Math.max(0, lines.size() - linesVisible);
            int topLine = r.nextInt(lastLineWhichCanBeTop + 1);
            fx_(() -> flowEditorPane.scrollTo(topLine));
            int lineOfInterest = topLine + r.nextInt(linesVisible);
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
            savedPositions.add(new int[] {topLine, caretPos, (int)Math.round(caretX), (int)Math.round(caretY)});
        }

        for (int[] savedPosition : savedPositions)
        {
            fx_(() -> {
                flowEditorPane.scrollTo(savedPosition[0]);
            });
            sleep(200);
            clickOn(savedPosition[2], savedPosition[3]);
            sleep(200);
            assertEquals("Clicked on " + savedPosition[2] + ", " + savedPosition[3], savedPosition[1], (int)fx(() -> flowEditorPane.getCaretPosition()));
        }
        
        // Turn off highlighting so that all text is black:
        PrefMgr.setFlag(PrefMgr.HIGHLIGHTING, false);

        int flowX = fx(() -> flowEditorPane.localToScreen(flowEditorPane.getBoundsInLocal()).getMinX()).intValue();
        int flowY = fx(() -> flowEditorPane.localToScreen(flowEditorPane.getBoundsInLocal()).getMinY()).intValue();
        for (int i = 0; i < 5; i++)
        {
            // We now pick two of the caret positions and make a selection between them, then check the background:
            // Either of these might be earlier in document, we scroll to the one we label "from":
            int[] fromSaved = savedPositions.get(r.nextInt(savedPositions.size()));
            int[] toSaved = savedPositions.get(r.nextInt(savedPositions.size()));
            fx_(() -> {
                flowEditorPane.scrollTo(fromSaved[0]);
                flowEditorPane.positionCaretWithoutScrolling(toSaved[1]);
                flowEditorPane.positionAnchor(fromSaved[1]);
            });
            int toPosX = fx(() -> caret.getElements().isEmpty() ? flowX + 1.0 : caret.localToScreen(caret.getBoundsInLocal()).getCenterX()).intValue();
            int toPosY = fx(() -> caret.getElements().isEmpty() ? flowY + 1.0 : caret.localToScreen(caret.getBoundsInLocal()).getCenterY()).intValue();
            sleep(200);
            WritableImage editorImage = fx(() -> flowEditorPane.snapshot(null, null));
            int firstY = fromSaved[1] <= toSaved[1] ? fromSaved[3] : toPosY;
            int lastY = Math.min((int)editorImage.getHeight() - 1, fromSaved[1] <= toSaved[1] ? toPosY : fromSaved[3]);

            for (int y = firstY; y <= lastY; y += 10)
            {
                boolean mightBeFirstLine = y - firstY <= 20;
                boolean mightBeLastLine = lastY - y <= 20;
                int startX = (mightBeFirstLine ? (fromSaved[1] <= toSaved[1] ? fromSaved[2] : toPosX) : flowX) + 10;
                int endX = (mightBeLastLine ? (fromSaved[1] <= toSaved[1] ? toPosX : fromSaved[2]) : flowX + (int)editorImage.getWidth()) - 10;
                // If selection begins at start of the line, will be very little blue, so skip it:
                // Also skip if the end is before the start (because it's a one line selection that wraps, and we may be chosing a region that overlaps both):
                if (endX <= flowX + 5 || endX < startX)
                    continue;
                // Should be blue:
                List<Stream<Color>> colorRegions = List.of(streamArea(editorImage, startX - flowX, y - flowY), streamArea(editorImage, endX - flowX, y - flowY));
                for (Stream<Color> colorRegion : colorRegions)
                {
                    Stream<Color> nonBlack = colorRegion.filter(c -> c.getRed() + c.getGreen() + c.getBlue() > 0.05);
                    // Last item is count, not alpha:
                    Color average = nonBlack.map(c -> new double[]{c.getRed(), c.getGreen(), c.getBlue(), 1})
                            .reduce((a, b) -> new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2], a[3] + b[3]})
                            .map(a -> Color.color(a[0] / a[3], a[1] / a[3], a[2] / a[3])).orElseThrow();
                    
                    assertThat("X: " + (startX - flowX) + ", " + (endX - flowX) + " Y: " + (y - flowY), average.getBlue(), Matchers.greaterThan(average.getGreen() + 0.1));
                    assertThat("X: " + (startX - flowX) + ", " + (endX - flowX) + " Y: " + (y - flowY), average.getBlue(), Matchers.greaterThan(average.getRed() + 0.1));
                }
            }
        }
        // TODO test clicking, caret and selection display (especially when one or both ends off-screen)
        
    }
    
    private static Stream<Color> streamArea(WritableImage image, int x, int y)
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
        return r.stream();
    }
    

    // We remove awkward unprintable characters that mess up the location tracking for click positions.
    // To see this again, pass seed=1L to testEditor.
    private String removeInvalid(String rawContent)
    {
        int[] valid = rawContent.codePoints().filter(n -> {
            if (n >= 32 && n != 127 && n <= 0xFFFF)
                return true;
            else if (n == '\n')
                return true;
            else
                return false;
                
        }).toArray();
        return new String(valid, 0, valid.length);
    }

    private void setTextLines(String... lines)
    {
        setText(Arrays.stream(lines).collect(Collectors.joining("\n")));
    }

    private void setText(String content)
    {
        fx_(() -> flowEditorPane.getDocument().replaceText(0, flowEditorPane.getDocument().getLength(), content));
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
        List<TextFlow> guiLines = fx(() -> {
            return flowEditorPane.lookupAll(".text-line").stream().sorted(Comparator.comparing(Node::getLayoutY)).map(t -> (TextFlow)t).collect(Collectors.toList());
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
    private String getAllText(TextFlow textFlow)
    {
        return textFlow.getChildren().stream().filter(c -> c instanceof Text).map(c -> ((Text)c).getText()).collect(Collectors.joining());
    }
    
    @Test
    public void testScope()
    {
        String beforeEnterPoint = "public class Foo\n{\n    public void method() {\n        int x = 8;\n        ";
        String afterEnterPoint = "\n    }\n}\n";
        setText(beforeEnterPoint + afterEnterPoint);
        fx_(() -> flowEditorPane.requestFocus());
        fx_(() -> flowEditorPane.positionCaret(beforeEnterPoint.length()));
        sleep(500);
        // Find the caret Y:
        Node caret = lookup(".flow-caret").query();
        double y = fx(() -> flowEditorPane.sceneToLocal(caret.localToScene(caret.getBoundsInLocal())).getCenterY());
        
        // Check initial scopes:
        checkScopes(5, scope(Color.GREEN, between(0, 2), between(780, 800)));
        checkScopes((int)y, 
            scope(Color.GREEN, between(0, 2), between(22, 28)),
            scope(Color.YELLOW, between(25, 30), between(50, 55))
        );
        for (int i = 0; i < 10; i++)
        {
            push(KeyCode.ENTER);
            sleep(150);
            assertEquals(beforeEnterPoint + "\n".repeat(i + 1) + afterEnterPoint, fx(() -> flowEditorPane.getDocument().getFullContent()));
            // Scopes should still be the same:
            checkScopes(5, scope(Color.GREEN, between(0, 2), between(780, 800)));
            y = fx(() -> flowEditorPane.sceneToLocal(caret.localToScene(caret.getBoundsInLocal())).getCenterY());
            checkScopes((int) y,
                scope(Color.GREEN, between(0, 2), between(22, 28)),
                scope(Color.YELLOW, between(25, 30), between(50, 55))
            );
        }
        for (int i = 0; i < 30; i++)
        {
            push(KeyCode.ENTER);
        }
        y = fx(() -> flowEditorPane.sceneToLocal(caret.localToScene(caret.getBoundsInLocal())).getCenterY());
        // Now, the top should have scrolled off, so should be nested scopes at top:
        checkScopes((int) 5,
            scope(Color.GREEN, between(0, 2), between(22, 28)),
            scope(Color.YELLOW, between(25, 30), between(50, 55))
        );
        checkScopes((int) y,
            scope(Color.GREEN, between(0, 2), between(22, 28)),
            scope(Color.YELLOW, between(25, 30), between(50, 55))
        );
        
        // Get back to top:
        push(KeyCode.PAGE_UP);
        push(KeyCode.PAGE_UP);
        checkScopes(5, scope(Color.GREEN, between(0, 2), between(780, 800)));
        fx_(() -> flowEditorPane.positionCaret(beforeEnterPoint.length()));
        sleep(500);

        for (int i = 0; i < 40; i++)
        {
            // Check scope position as we scroll down:
            if (i == 0)
                checkScopes(5, scope(Color.GREEN, between(0, 2), between(780, 800)));
            y = fx(() -> flowEditorPane.sceneToLocal(caret.localToScene(caret.getBoundsInLocal())).getCenterY());
            checkScopes((int)y,
                    scope(Color.GREEN, between(0, 2), between(22, 28)),
                    scope(Color.YELLOW, between(25, 30), between(50, 55))
            );
            push(KeyCode.DOWN);
        }
        
    }

    private static Matcher<Integer> between(int low, int high)
    {
        return Matchers.both(Matchers.greaterThanOrEqualTo(low)).and(Matchers.lessThanOrEqualTo(high));
    }

    private void checkScopes(int y, Scope... scopes)
    {
        // Take screenshot of background:
        WritableImage image = fx(() -> flowEditorPane.snapshotBackground());
        // Go from LHS:
        int scopeIndex = 0;
        boolean inScope = false;
        Color scopeColor = Color.TURQUOISE;
        int scopeStartX = 0;
        for (int x = 0; x < image.getWidth() && scopeIndex < scopes.length; x += 1)
        {
            Color c = image.getPixelReader().getColor(x, y);
            if (!c.equals(Color.BLACK))
            {
                if (c.equals(Color.WHITE) && inScope)
                {
                    // End of scope
                    // We don't always get exactly the same colour, so have some tolerance: 
                    try
                    {
                        assertThat("At " + y, scopeColor.getRed(), Matchers.closeTo(scopes[scopeIndex].expectedColor.getRed(), 0.03));
                        assertThat("At " + y, scopeColor.getGreen(), Matchers.closeTo(scopes[scopeIndex].expectedColor.getGreen(), 0.03));
                        assertThat("At " + y, scopeColor.getBlue(), Matchers.closeTo(scopes[scopeIndex].expectedColor.getBlue(), 0.03));
                        assertThat("At " + y, scopeStartX, scopes[scopeIndex].lhsCheck);
                        assertThat("At " + y, x - 1, scopes[scopeIndex].rhsCheck);
                    }
                    catch (AssertionError e)
                    {
                        System.out.println("Failing editor image:\n" + asBase64(image));
                        throw e;
                    }
                    inScope = false;
                    scopeIndex += 1;
                }
                else if (!c.equals(Color.WHITE) && !inScope)
                {
                    scopeStartX = x - 1;
                    scopeColor = c;
                    inScope = true;
                }
            }
        }
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
}
