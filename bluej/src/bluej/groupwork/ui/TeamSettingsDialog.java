/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2016,2017,2018,2019,2020  Michael Kolling and John Rosenberg

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
import bluej.utility.DialogManager;
import bluej.utility.javafx.FXCustomizedDialog;
import bluej.utility.javafx.JavaFXUtil;
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
    public TeamSettingsDialog(Window owner, TeamSettingsController controller, boolean isShareAction)
    {
        super(owner, "team.settings.title", "team-settings");
        setResizable(false);
        teamSettingsController = controller;

        if(teamSettingsController.hasProject()) {
            setTitle(getTitle() + " - " + teamSettingsController.getProject().getProjectName());
        }

        setHeaderText(null);

        prepareButtonPane();
        teamSettingsPanel = new TeamSettingsPanel(teamSettingsController, this, isShareAction);
        getDialogPane().setContent(teamSettingsPanel);
        DialogManager.centreDialog(this);
        
        setOnShown(e -> {
            // The dialog logic steals focus to one of the buttons, we need a runLater to steal it back:
            JavaFXUtil.runAfterCurrent(() -> teamSettingsPanel.doRequestFocus()); 
        });
        
        setResultConverter(bt -> bt == ButtonType.OK ? getSettings() : null);
    }

    /**
     * Set up the buttons panel to contain ok and cancel buttons, and associate their actions.
     */
    private void prepareButtonPane()
    {
        // Set the button types.
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        /*
         * Write the data from the teamSettingsPanel to the project's team.defs file.
         * If checkbox in teamSettingsPanel is checked, the data is also stored in bluej.properties
         */
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
