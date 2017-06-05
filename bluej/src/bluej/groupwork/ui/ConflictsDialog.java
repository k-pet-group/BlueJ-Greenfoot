/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2010,2016,2017  Michael Kolling and John Rosenberg

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
import bluej.pkgmgr.Project;
import bluej.utility.javafx.FXCustomizedDialog;

import java.util.Iterator;
import java.util.List;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A dialog which presents conflicts after an update.
 *
 * @author fisker
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class ConflictsDialog extends FXCustomizedDialog<Void>
{
    //    private Label heading;
    private List<String> bluejConflicts;
    private List<String> nonBluejConflicts;
    private Project project;

    /**
     * @param project2
     * @param blueJconflicts
     * @param nonBlueJConflicts
     */
    public ConflictsDialog(Project project, List<String> bluejConflicts,
                           List<String> nonBlueJConflicts)
    {
        super(null, "team.conflicts.title", "team-conflicts");
        this.project = project;
        this.bluejConflicts = bluejConflicts;
        this.nonBluejConflicts = nonBlueJConflicts;

        getDialogPane().setContent(makeMainPane());
        //close button
        getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
//        DialogManager.centreDialog(this);
        loadAndTrackPosition("bluej.teamwork.conflicts");

    }

    private Pane makeMainPane()
    {
        VBox mainPanel = new VBox();

        Pane bluejConflictsPanel = makeConflictsPanel(Config.getString("team.conflicts.classes"), bluejConflicts);
        mainPanel.getChildren().add(bluejConflictsPanel);

        Pane nonBluejConflictsPanel = makeConflictsPanel(Config.getString("team.conflicts.classes"), nonBluejConflicts);
        if (nonBluejConflicts.size() > 0) {
            mainPanel.getChildren().add(nonBluejConflictsPanel);
        }

        //resolve button
        Button resolveButton = new Button(Config.getString("team.conflicts.show"));
        resolveButton.setOnAction(event -> {
            project.openEditorsForSelectedTargets();
            // move to resolve button
            close();
        });
        resolveButton.requestFocus();
        resolveButton.setDisable(bluejConflicts.size() <= 0);
        mainPanel.getChildren().add(resolveButton);

        return mainPanel;
    }

    private Pane makeConflictsPanel(String headline, List<String> conflicts)
    {
        VBox labelPanel = new VBox();
        labelPanel.setAlignment(Pos.BASELINE_LEFT);

        VBox conflictsPanel = new VBox();
        conflictsPanel.setAlignment(Pos.BASELINE_LEFT);

        //the conflicting files labels
        //TODO make it stream
        for (Iterator<String> i = conflicts.iterator(); i.hasNext();) {
            String conflict = i.next();
            conflictsPanel.getChildren().add(new Label(conflict));
        }

        ScrollPane scrollPane = new ScrollPane(conflictsPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        //heading
        labelPanel.getChildren().addAll(new Label(headline),
                                        new Separator(Orientation.VERTICAL), //5
                                        scrollPane);

        return labelPanel;
    }
}
