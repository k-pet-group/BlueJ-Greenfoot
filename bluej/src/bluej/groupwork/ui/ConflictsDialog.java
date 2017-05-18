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
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
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
        buildUI();
    }

    private void buildUI()
    {
        VBox mainPanel = new VBox();
        Pane bluejConflictsPanel = makeConflictsPanel(Config.getString("team.conflicts.classes"),
                bluejConflicts);
        Pane nonBluejConflictsPanel = makeConflictsPanel(Config.getString("team.conflicts.classes"),
                nonBluejConflicts);
        Pane buttonPanel = makeButtonPanel();

        mainPanel.getChildren().add(bluejConflictsPanel);
        if (nonBluejConflicts.size() > 0) {
            mainPanel.getChildren().add(nonBluejConflictsPanel);
        }
        mainPanel.getChildren().add(buttonPanel);
        getDialogPane().getChildren().add(mainPanel);

        rememberPosition("bluej.teamwork.conflicts");
    }

    private Pane makeConflictsPanel(String headline, List<String> conflicts)
    {
        VBox labelPanel = new VBox();
//        labelPanel.setBorder(BlueJTheme.dialogBorder);
        labelPanel.setAlignment(Pos.BASELINE_LEFT);

        //heading
        labelPanel.getChildren().addAll(new Label(headline), new Separator(Orientation.VERTICAL));//5

        VBox conflictsPanel = new VBox();
        conflictsPanel.setAlignment(Pos.BASELINE_LEFT);

        //the conflicting files labels
        //TODO make it stream
        for (Iterator<String> i = conflicts.iterator(); i.hasNext();) {
            String conflict = i.next();
            conflictsPanel.getChildren().add(new Label(conflict));
        }

        ScrollPane scrollPane = new ScrollPane(conflictsPanel);
        labelPanel.getChildren().add(scrollPane);

        return labelPanel;
    }

    /**
     * Create the button panel with a Resolve button and a close button
     * @return Pane the buttonPanel
     */
    private Pane makeButtonPanel()
    {
        Pane buttonPanel = new FlowPane();

        //TODO
//        buttonPanel.setAlignment(??);
//        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
//        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        //close button
        Button closeButton = new Button(Config.getString("close"));
        closeButton.setOnAction(event -> close());

        //resolve button
        Button resolveButton = new Button(Config.getString("team.conflicts.show"));
        resolveButton.setOnAction(event -> {
            project.openEditorsForSelectedTargets();
            // move to resolve button
            close();
        });

        resolveButton.requestFocus();

        buttonPanel.getChildren().addAll(resolveButton, closeButton);
        resolveButton.setDisable(bluejConflicts.size() <= 0);

        return buttonPanel;
    }
}
