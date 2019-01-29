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
package bluej.flow;

import bluej.editor.flow.FlowEditorPane;
import bluej.flow.gen.GenString;
import bluej.utility.Utility;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.When;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testfx.framework.junit.ApplicationTest;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(JUnitQuickcheck.class)
public class TestBasicEditorDisplay extends FXTest
{
    private Stage stage;
    private FlowEditorPane flowEditorPane;

    @Override
    public void start(Stage stage) throws Exception
    {
        super.start(stage);
        this.stage = stage;
        flowEditorPane = new FlowEditorPane("");
        stage.setScene(new Scene(flowEditorPane));
        stage.show();
    }

    @Property(trials=5)
    public void testEditor(@From(GenString.class) String content)
    {
        Platform.runLater(() -> flowEditorPane.getDocument().replaceText(0, 0, content));
        
        sleep(1000);
        
        List<String> lines = flowEditorPane.getDocument().getLines().map(s -> s.toString()).collect(Collectors.toList());

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
                
        // TODO check that lines have no gap
        // TODO test scrolling, clicking, caret and selection display (especially when one or both ends off-screen)
        
    }
    
    @OnThread(Tag.FXPlatform)
    private String getAllText(TextFlow textFlow)
    {
        return textFlow.getChildren().stream().filter(c -> c instanceof Text).map(c -> ((Text)c).getText()).collect(Collectors.joining());
    }
}
