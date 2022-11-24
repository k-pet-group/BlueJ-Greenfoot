/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2011,2012,2013,2016,2018  Poul Henriksen and Michael Kolling
 
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

import bluej.utility.javafx.FXCustomizedDialog;
import bluej.Config;

import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * A dialog to set the Player name in the project properties.
 *
 * @author Amjad Altadmri
 */
public class SetPlayerDialog extends FXCustomizedDialog<String>
{
    private TextField playerNameTextField;

    /**
     * Construct a set player dialog with the current player name as the default
     * text in the name text field.
     *
     * @param parent         The parent window.
     * @param curPlayerName  The current player name.
     */
    public SetPlayerDialog(Window parent, String curPlayerName)
    {
        super(parent, Config.getString("playername.dialog.title"), null);
        setResizable(false);

        playerNameTextField = new TextField(curPlayerName);
        playerNameTextField.setPrefWidth(250);
        Label playerNameLabel = new Label(Config.getString("playername.dialog.playerName") + ":");
        HBox nameFieldRow = new HBox(5, playerNameLabel, playerNameTextField);
        nameFieldRow.setAlignment(Pos.BASELINE_LEFT);

        setContentPane(new VBox(15, new Label(Config.getString("playername.dialog.help")), nameFieldRow));
        playerNameTextField.requestFocus();

        // Ok and cancel buttons
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(playerNameTextField.textProperty().isEmpty());
        setResultConverter(bt -> bt == ButtonType.OK ? playerNameTextField.getText() : null);
    }
}
