/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016,2017  Michael Kolling and John Rosenberg
 
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
package bluej.groupwork.ui;

import bluej.Config;
import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamSettingsController;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Window;

/**
 * A dialog for teamwork settings.
 *
 * @author fisker
 * @author bquig
 */
public class TeamSettingsDialog extends Dialog<TeamSettings>
{
    private String title = Config.getString("team.settings.title");
    private TeamSettingsController teamSettingsController;
    private TeamSettingsPanel teamSettingsPanel;
    private Button okButton;

    /**
     * Create a team settings dialog with a reference to the team settings
     * controller that it manipulates.
     */
    public TeamSettingsDialog(Window parent, TeamSettingsController controller)
    {
        initOwner(parent);
        teamSettingsController = controller;
        initModality(Modality.WINDOW_MODAL);

        if(teamSettingsController.hasProject()) {
            title += " - " + teamSettingsController.getProject().getProjectName();
        }
        setTitle(title);

//        dialogPane = new DialogPaneAnimateError(errorLabel, () -> updateOKButton());
//        setDialogPane(dialogPane);
//        Config.addDialogStylesheets(getDialogPane());

        makeButtonPane();
        teamSettingsPanel = new TeamSettingsPanel(teamSettingsController, this);
        getDialogPane().getChildren().add(teamSettingsPanel);
    }

    /**
     * Set up the panel containing the ok and cancel buttons, with associated
     * actions.
     */
    private void makeButtonPane()
    {
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);

        /*
         * Write the data from the teamSettingsPanel to the project's team.defs file
         * If checkbox in teamSettingsPanel is checked, the data is also stored in
         * bluej.properties
         */
        okButton.setOnAction(event -> {
            TeamSettings settings = teamSettingsPanel.getSettings();
            teamSettingsController.updateSettings(settings, teamSettingsPanel.getUseAsDefault());
            if (teamSettingsController.hasProject()) {
                teamSettingsController.writeToProject();
            }
        });
    }

    /**
     * Disable the fields used to specify the repository:
     * group, prefix, server and protocol. Called when the team settings
     * dialog is connected to a project already.
     */
    public void disableRepositorySettings()
    {
        teamSettingsPanel.disableRepositorySettings();
    }

    /**
     * Enabled or disable to "Ok" button of the dialog.
     */
    public void setOkButtonEnabled(boolean enabled)
    {
        okButton.setDisable(!enabled);
    }
    
    /**
     * Get the settings specified by the user
     */
    public TeamSettings getSettings()
    {
        return teamSettingsPanel.getSettings();
    }

    public Button getOkButton()
    {
        return okButton;
    }
}
