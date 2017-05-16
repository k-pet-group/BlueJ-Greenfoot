/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2012,2014,2016,2017  Michael Kolling and John Rosenberg

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

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.groupwork.Repository;
import bluej.groupwork.StatusHandle;
import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamViewFilter;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.pkgmgr.Project;
import bluej.utility.FXWorker;
import bluej.utility.javafx.FXCustomizedDialog;
import bluej.utility.javafx.JavaFXUtil;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Main frame for CVS Status Dialog
 *
 * @author bquig
 * @author Amjad Altadmri
 *
 */
public class StatusFrame extends FXCustomizedDialog<Void>
{
    private Project project;
    private Repository repository;
    private StatusTableModel statusModel;

    private Button refreshButton;
    private ActivityIndicatorFX progressBar;
    private StatusWorker worker;

    private static final int MAX_ENTRIES = 20;
    private final boolean isDVCS;

    /**
     * Creates a new instance of StatusFrame. Called via factory method
     * getStatusWindow.
     */
    public StatusFrame(Project proj)
    {
        super(null, "team.status", "team-status");
        project = proj;
        isDVCS = project.getTeamSettingsController().isDVCS();
        // The layout should be Vertical, if not replace with a VBox.
        getDialogPane().setContent(makeMainPane());
        prepareButtonPane();
    }

    private Pane makeMainPane()
    {
        // try and set up a reasonable default amount of entries that avoids resizing
        // and scrolling once we get info back from repository
        statusModel = isDVCS ?
                new StatusTableModelDVCS(project, estimateInitialEntries()) :
                new StatusTableModelNonDVCS(project, estimateInitialEntries());

        //TODO check the next line
        TableView<TeamStatusInfo> statusTable = new TableView<>(statusModel.getResources());
        //TODO implements the next line
        // statusTable.getTableHeader().setReorderingAllowed(false);


        //set up custom renderer to colour code status message field
//        StatusMessageCellRenderer statusRenderer = new StatusMessageCellRenderer(project);
//        statusTable.setDefaultRenderer(java.lang.Object.class, statusRenderer);

        TableColumn<TeamStatusInfo, String> firstColumn = new TableColumn<>(statusModel.getColumnName(0));
        firstColumn.setPrefWidth(70);
        JavaFXUtil.addStyleClass(firstColumn, "team-status-firstColumn");
        firstColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper((String) getValueAt(v.getValue(), 0)));

        TableColumn<TeamStatusInfo, Object> secondColumn = new TableColumn<>(statusModel.getColumnName(1));
        secondColumn.setPrefWidth(40);
        JavaFXUtil.addStyleClass(secondColumn, "team-status-secondColumn");
        secondColumn.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(getValueAt(v.getValue(), 1)));
//      secondColumn.setCellFactory(col -> new StatusTableCell(project));

        TableColumn<TeamStatusInfo, Integer> thirdColumn = new TableColumn<>(statusModel.getColumnName(2));
        thirdColumn.setPrefWidth(60);
        JavaFXUtil.addStyleClass(thirdColumn, "team-status-thirdColumn");
        thirdColumn.setCellValueFactory(v -> new SimpleObjectProperty<>((Integer) getValueAt(v.getValue(), 2)));
//      thirdColumn.setCellFactory(col -> new StatusTableCell(project));

        statusTable.getColumns().setAll(firstColumn, secondColumn, thirdColumn);


        /*
        value.setCellValueFactory(v -> new ReadOnlyObjectWrapper(new StringOrRef(v.getValue().getValue())));
        value.setCellFactory(col -> new ValueCell());
        */

        ScrollPane statusScroller = new ScrollPane(statusTable);
//        Dimension prefSize = statusTable.getMaximumSize();
//        Dimension scrollPrefSize =  statusTable.getPreferredScrollableViewportSize();
//        Dimension best = new Dimension(scrollPrefSize.width + 50, prefSize.height + 30);
//        statusScroller.setPreferredSize(best);

        VBox mainPane = new VBox();
        mainPane.setSpacing(10);
        mainPane.getChildren().addAll(statusScroller, makeRefreshPane());
        return mainPane;
    }

    /**
     * Create the Refresh button and status progress bar
     * @return Pane the progress HBox
     */
    private Pane makeRefreshPane()
    {
        // progress bar
        progressBar = new ActivityIndicatorFX();
        progressBar.setRunning(false);

        //refresh button
        refreshButton = new Button(Config.getString("team.status.refresh"));
        refreshButton.setDisable(true);
        refreshButton.setOnAction(event -> update());
        refreshButton.requestFocus();

        HBox box = new HBox();
        box.setAlignment(Pos.BASELINE_CENTER);
        box.getChildren().addAll(progressBar, refreshButton);
        return box;
    }

    /**
     * Create the button panel with a Resolve button and a close button
     * @return Pane the buttonPanel
     */
    private void prepareButtonPane()
    {
        //close button
        getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
        this.setOnCloseRequest(event -> {
            if (worker != null) {
                worker.abort();
            }
        });
    }

    /**
     * try and estimate the number of entries in status table to avoid resizing
     * once repository has responded.
     */
    private int estimateInitialEntries()
    {
        // Use number of targets + README.TXT
        int initialEntries = project.getFilesInProject(true, false).size() + 1;
        // may need to include diagram layout
        //if(project.includeLayout())
        //    initialEntries++;
        // Limit to a reasonable maximum
        if(initialEntries > MAX_ENTRIES) {
            initialEntries = MAX_ENTRIES;
        }
        return initialEntries;
    }

    /**
     * Refresh the status window.
     */
    public void update()
    {
        repository = project.getRepository();
        if (repository != null) {
            progressBar.setRunning(true);
            refreshButton.setDisable(true);
            worker = new StatusWorker();
            worker.start();
        }
        else {
            hide();
        }
    }

    /**
     * Find the table entry at a particular column for a specific info object (row).
     *
     * @param   info    the info object which occupies a row
     * @param   col     the table column number
     * @return          the Object at that location in the table
     */
    public Object getValueAt(TeamStatusInfo info, int col)
    {
        switch (col) {
            case 0:
                return ResourceDescriptor.getResource(project, info, false);
            case 1:
                return isDVCS ? info.getStatus() : info.getLocalVersion();
            case 2:
                return isDVCS ? info.getRemoteStatus() : info.getStatus();
            default:
                break;
        }

        return null;
    }

    /**
     * Inner class to do the actual cvs status call to ensure that the UI is not
     * blocked during remote call
     */
    class StatusWorker extends FXWorker implements StatusListener
    {
        ObservableList<TeamStatusInfo> resources;
        TeamworkCommand command;
        TeamworkCommandResult result;
        boolean aborted;
        FileFilter filter = project.getTeamSettingsController().getFileFilter(true);

        public StatusWorker()
        {
            super();
            resources = FXCollections.observableArrayList();
            //Set files = project.getTeamSettingsController().getProjectFiles(true);
            command = repository.getStatus(this, filter, true);
        }

        public void abort()
        {
            command.cancel();
            aborted = true;
        }

        @OnThread(Tag.Unique)
        public Object construct()
        {
            result = command.getResult();
            return resources;
        }

        @OnThread(Tag.Any)
        public void gotStatus(TeamStatusInfo info)
        {
            resources.add(info);
        }

        @OnThread(Tag.Any)
        public void statusComplete(StatusHandle commitHandle)
        {
            // Nothing to be done here.
        }

        public void finished()
        {
            progressBar.setRunning(false);
            if (! aborted) {
                if (result.isError()) {
                    StatusFrame.this.dialogThenHide(() -> TeamUtils.handleServerResponseFX(result, StatusFrame.this.asWindow()));
                }
                else {
                    resources.sort((info0, info1) -> info1.getStatus().ordinal() - info0.getStatus().ordinal());

                    TeamViewFilter filter = new TeamViewFilter();
                    // Remove old package files from display
                    resources.removeIf(info -> !filter.accept(info));
                    statusModel.setStatusData(resources);

                    Map<File, String> statusMap = new HashMap<>();

                    for (TeamStatusInfo s : resources)
                    {
                        statusMap.put(s.getFile(), s.getStatus().getStatusString());
                    }

                    DataCollector.teamStatusProject(project, repository, statusMap);
                }
                refreshButton.setDisable(false);
            }
        }
    }
}
