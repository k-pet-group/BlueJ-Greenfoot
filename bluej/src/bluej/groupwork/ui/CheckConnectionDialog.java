/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016,2017,2018,2019  Michael Kolling and John Rosenberg
 
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

import bluej.groupwork.git.GitProvider;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;

import bluej.Config;
import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.TeamworkProvider;
import bluej.utility.javafx.FXCustomizedDialog;
import bluej.utility.javafx.JavaFXUtil;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A dialog which displays an activity indicator while connection settings are
 * being verified
 *
 * @author Davin McCall
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class CheckConnectionDialog extends FXCustomizedDialog<Void>
{
    private ProgressBar activityIndicator;
    private Text connLabel;

    private TeamSettings settings;
    private TeamworkProvider provider;

    public CheckConnectionDialog(Window owner, TeamSettings settings)
    {
        super(owner, "team.settings.checkConnection", "team-test-connection");
        this.settings = settings;
        this.provider = new GitProvider() ;
        buildUI();
    }

    private void buildUI()
    {
        VBox contentPane = new VBox();
        contentPane.setMinHeight(120.0);
        JavaFXUtil.addStyleClass(contentPane, "pane");
        getDialogPane().setContent(contentPane);
        getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);

        connLabel = new Text(Config.getString("team.checkconn.checking"));
        contentPane.getChildren().add(new TextFlow(connLabel));

        activityIndicator = new ProgressBar();
        contentPane.getChildren().add(activityIndicator);
        activityIndicator.setMaxWidth(9999.0);
        contentPane.setFillWidth(true);
    }

    public void showAndCheck()
    {
        // Must start the thread before calling showAndWait, because
        // we are modal - showAndWait will block.
        new Thread(new Runnable() {
            @Override
            @OnThread(Tag.Worker)
            public void run() {
                final TeamworkCommandResult res = provider.checkConnection(settings);
                Platform.runLater(() -> {
                    if (!res.isError())
                    {
                        connLabel.setText(Config.getString("team.checkconn.ok"));
                    }
                    else
                    {
                        connLabel.setText(Config.getString("team.checkconn.bad")
                                + System.getProperty("line.separator") + System.getProperty("line.separator")
                                + res.getErrorMessage());
                    }
                    activityIndicator.setProgress(1.0);
                });
            }
        }).start();
        showAndWait();
    }
}
