/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2017  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.guifx;

import bluej.utility.javafx.FXPlatformConsumer;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * This is the JavaFX-based Greenfoot.ask GUI which is shown in the IDE
 * when Greenfoot.ask is called.  Not to be confused with AskPanel, which
 * is the Swing version shown when running as a standalone application.
 */
@OnThread(Tag.FXPlatform)
public class AskPaneFX extends BorderPane
{
    // The prompt shown above the text field.  The parameter to Greenfoot.ask()
    private final Text promptText;
    // The text field for the user to input the answer
    private final TextField textField;
    // The action to perform once the answer is received
    private FXPlatformConsumer<String> withAnswer;

    public AskPaneFX()
    {
        promptText = new Text();
        
        textField = new TextField();
        textField.setOnAction(e -> enter());
        
        Button enterButton = new Button("OK");
        enterButton.setOnAction(e -> enter());
        
        TextFlow promptTextFlow = new TextFlow(promptText);
        promptTextFlow.setPadding(new Insets(0,10,10,10));

        BorderPane borderPane = new BorderPane(textField, promptTextFlow, enterButton, null, null);
        borderPane.getStyleClass().add("ask-pane");
        setBottom(borderPane);
        
        setVisible(false);
    }

    // Called when user clicks OK or presses enter in the text field.
    private void enter()
    {
        if (withAnswer != null)
        {
            withAnswer.accept(textField.getText());
            withAnswer = null;
        }
    }

    /**
     * Focuses the text field in the pane
     */
    public void focusTextEntry()
    {
        textField.requestFocus();
    }

    /**
     * Sets the text above the text field (usually a question to the user)
     */
    public void setPrompt(String prompt)
    {
        promptText.setText(prompt);
    }

    /**
     * Sets the handler for the answer, once the user has input it and pressed enter/clicked OK.
     */
    public void setWithAnswer(FXPlatformConsumer<String> withAnswer)
    {
        this.withAnswer = withAnswer;
    }
}
