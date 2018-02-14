/*
 This file is part of the Greenfoot program.
 Copyright (C) 2014,2016,2017,2018  Poul Henriksen and Michael Kolling

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

import bluej.Config;
import bluej.utility.javafx.FXCustomizedDialog;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * A dialog to ask the user for a pasted image name.
 *
 * @Author Amjad Altadmri
 */
public class PastedImageNameDialog extends FXCustomizedDialog<String>
{

    public PastedImageNameDialog(Window parent, Image image, String style)
    {
        super(parent, Config.getString("editor.paste.image.title"), style);

        ImageView imageView = new ImageView(image);

        TextField fileNameField = new TextField();
        fileNameField.setAlignment(Pos.BASELINE_LEFT);
        fileNameField.setPromptText(Config.getString("editor.paste.image.name.prompt"));
        fileNameField.requestFocus();

        HBox fileNameRow = new HBox(new Label(Config.getString("editor.paste.image.prompt")), fileNameField, new Label(".png"));
        fileNameRow.setAlignment(Pos.BASELINE_LEFT);
        HBox.setHgrow(fileNameField, Priority.ALWAYS);

        VBox bodyPanel = new VBox(20, imageView, fileNameRow);
        bodyPanel.setAlignment(Pos.CENTER);
        setContentPane(bodyPanel);

        // add buttons
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(fileNameField.textProperty().isEmpty());

        setResultConverter(bt -> bt == ButtonType.OK ? fileNameField.getText() : null);
    }
}
