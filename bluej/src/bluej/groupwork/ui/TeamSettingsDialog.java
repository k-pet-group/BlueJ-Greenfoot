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

import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamSettingsController;

import bluej.utility.javafx.FXCustomizedDialog;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A dialog for teamwork settings.
 *
 * @author fisker
 * @author bquig
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class TeamSettingsDialog extends FXCustomizedDialog<TeamSettings>
{
    private TeamSettingsController teamSettingsController;
    private TeamSettingsPanel teamSettingsPanel;
    private Button okButton;

    /**
     * Create a team settings dialog with a reference to the team settings
     * controller that it manipulates.
     */
    public TeamSettingsDialog(Window parent, TeamSettingsController controller)
    {
        super(parent, "team.settings.title", "team-settings");
        teamSettingsController = controller;

        if(teamSettingsController.hasProject()) {
            setTitle(getTitle() + " - " + teamSettingsController.getProject().getProjectName());
        }

        setHeaderText(null);//
//        setGraphic(new ImageView(this.getClass().getResource("team.png").toString()));
//        dialogPane = new DialogPaneAnimateError(errorLabel, () -> updateOKButton());
//        setDialogPane(dialogPane);
//        getDialogPane().setPrefSize(360, 600);

        makeButtonPane();
        teamSettingsPanel = new TeamSettingsPanel(teamSettingsController, this, getDialogPane().getStyleClass());
        getDialogPane().getChildren().add(teamSettingsPanel);
    }

    /**
     * Set up the panel containing the ok and cancel buttons, with associated
     * actions.
     */
    private void makeButtonPane()
    {
        // Set the button types.
//        ButtonType testConnection = new ButtonType("Test connection", ButtonData.APPLY  OTHER);
//        getDialogPane().getButtonTypes().addAll(testConnection, ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        /*
         * Write the data from the teamSettingsPanel to the project's team.defs file.
         * If checkbox in teamSettingsPanel is checked, the data is also stored in bluej.properties
         */
        // TODO Move this to the dialog result?
        okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
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
