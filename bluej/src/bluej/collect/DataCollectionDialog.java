/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012,2013,2016,2019,2020  Michael Kolling and John Rosenberg
 
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
package bluej.collect;

import javax.swing.*;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;

import bluej.Config;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;

/**
 * Package-visible dialog, displayed to ask the user whether they want to opt-in or opt-out
 * of the data collection project.
 *
 */
class DataCollectionDialog extends Dialog<Boolean>
{
    public DataCollectionDialog()
    {
        initModality(Modality.APPLICATION_MODAL);
        // There is a problem on some systems where the dialog begins by appearing very small.
        // It seems like this may be some kind of JavaFX glitch.
        // So although resizing doesn't make much sense, we need to let users who have a small
        // dialog resize it to the proper size.
        setResizable(true);
        
        setTitle("BlueJ - " + Config.getString("collect.dialog.title"));
        
        Config.addDialogStylesheets(getDialogPane());
        VBox body = new VBox();
        JavaFXUtil.addStyleClass(body, "blackbox-optin-content");
        
        Pane headerPanel = makeHeaderPanel();
            
        body.getChildren().add(headerPanel);
        body.getChildren().add(makeButtonPanel());
        body.getChildren().add(new Separator(Orientation.HORIZONTAL));
        body.getChildren().add(makeExplanationText());
    
        getDialogPane().setContent(body);
    }

    /**
     * Make a component with the ethics text inside
     * @return
     */
    private Node makeExplanationText()
    {
        TextFlow text = new TextFlow();
        JavaFXUtil.addStyleClass(text, "blackbox-optin-explanation");

        Hyperlink link = new Hyperlink(Config.getString("collect.dialog.ethics.seemore") + ".");
        link.setOnAction(e -> {
            SwingUtilities.invokeLater(() -> Utility.openWebBrowser("http://www.bluej.org/blackbox.html"));
        });

        text.getChildren().addAll(
            new Text(Config.getString("collect.dialog.ethics1")),
            link,
            new Text("\n\n"),
            new Text(Config.getString("collect.dialog.ethics2")));
        
        return text;
    }

    /**
     * Makes the header panel, which contains an icon on the left, and header text on the right
     */
    private Pane makeHeaderPanel()
    {
        HBox headerPanel = new HBox();
        JavaFXUtil.addStyleClass(headerPanel, "blackbox-optin-header");
    
        headerPanel.getChildren().add(new ImageView(Config.getFixedImageAsFXImage("bluej-icon-48.png")));
        
        String header = 
            Config.getString("collect.dialog.header1") + " "
          + Config.getString("collect.dialog.header2") + "\n\n"
          + Config.getString("collect.dialog.header3");
        
        headerPanel.getChildren().add(new TextFlow(JavaFXUtil.withStyleClass(new Text(header), "blackbox-optin-header-text")));
        
        return headerPanel;
    }

    /**
     * Makes the button panel, which has Opt-In/Opt-Out buttons
     */
    private Pane makeButtonPanel()
    {
        BorderPane buttonPanel = new BorderPane();
        
        Button buttonNo = new Button();
        buttonNo.setText(Config.getString("collect.dialog.no"));
        buttonNo.setOnAction(e -> {
            setResult(false);
            close();
        });
        buttonPanel.setLeft(buttonNo);
        Button buttonYes = new Button();
        buttonYes.setText(Config.getString("collect.dialog.yes"));
        buttonYes.setOnAction(e -> {
            setResult(true);
            close();
        });
        buttonPanel.setRight(buttonYes);
        buttonYes.setDefaultButton(true);
        setOnShown(e -> buttonYes.requestFocus());
        return buttonPanel;
    }
    
}
