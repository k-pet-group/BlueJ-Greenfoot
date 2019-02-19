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
import bluej.editor.moe.ScopeColorsBorderPane;
import bluej.editor.flow.gen.GenRandom;
import bluej.editor.flow.gen.GenString;
import bluej.parser.InitConfig;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Utility;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.When;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

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
    public void testEditor(@When(seed=1L) @From(GenString.class) String content, @When(seed=1L) @From (GenRandom.class) Random r)
    {
        setText(content);

        List<String> lines = flowEditorPane.getDocument().getLines().map(s -> s.toString()).collect(Collectors.toList());

        checkVisibleLinesAgainst(lines);
        for (int i = 0; i < 3; i++)
        {
            int newTop = r.nextInt(lines.size());
            fx_(() -> flowEditorPane.scrollTo(newTop));
            // Wait for layout:
            sleep(200);
            checkVisibleLinesAgainst(lines.subList(newTop, lines.size()));
        }

        // TODO test clicking, caret and selection display (especially when one or both ends off-screen)
        
    }

    private void setText(@When(seed = 1L) @From(GenString.class) String content)
    {
        fx_(() -> flowEditorPane.getDocument().replaceText(0, 0, content));
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
            sleep(300);
            assertEquals(beforeEnterPoint + "\n".repeat(i + 1) + afterEnterPoint, fx(() -> flowEditorPane.getDocument().getFullContent()));
            // Scopes should still be the same:
            checkScopes(5, scope(Color.GREEN, between(0, 2), between(780, 800)));
            checkScopes((int) y,
                scope(Color.GREEN, between(0, 2), between(22, 28)),
                scope(Color.YELLOW, between(25, 30), between(50, 55))
            );
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
}
