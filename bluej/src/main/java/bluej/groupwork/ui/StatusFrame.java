/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2012,2014,2016,2017,2018,2019  Michael Kolling and John Rosenberg

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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

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
@OnThread(Tag.FXPlatform)
public class StatusFrame extends FXCustomizedDialog<Void>
{
    private Project project;
    private Repository repository;
    private StatusTableModel statusModel;

    private Button refreshButton;
    private ActivityIndicator progressBar;
    private StatusWorker worker;

    private TableView<TeamStatusInfo> statusTable;

    /**
     * Creates a new instance of StatusFrame. Called via factory method
     * getStatusWindow.
     */
    public StatusFrame(Project project)
    {
        super(null, "team.status", "team-status");
        this.project = project;
        getDialogPane().setContent(makeMainPane());
        prepareButtonPane();
    }

    @Override
    protected Node wrapButtonBar(Node original)
    {
        makeRefreshPaneComponents();
        BorderPane borderPane = new BorderPane(progressBar, null, original, null, refreshButton);
        JavaFXUtil.addStyleClass(borderPane, "replacement-button-bar");
        return borderPane;
    }

    private Node makeMainPane()
    {
        // try and set up a reasonable default amount of entries that avoids resizing
        // and scrolling once we get info back from repository
        statusModel = new StatusTableModel();
        statusTable = new TableView<>(statusModel.getResources());

        TableColumn<TeamStatusInfo, String> firstColumn = new TableColumn<>(statusModel.getColumnName(0));
        JavaFXUtil.addStyleClass(firstColumn, "team-status-firstColumn");
        firstColumn.prefWidthProperty().bind(statusTable.widthProperty().multiply(0.49));
        firstColumn.setCellValueFactory(v ->
                new ReadOnlyStringWrapper(ResourceDescriptor.getResource(project, v.getValue(), false)));

        TableColumn<TeamStatusInfo, Object> secondColumn = new TableColumn<>(statusModel.getColumnName(1));
        JavaFXUtil.addStyleClass(secondColumn, "team-status-secondColumn");
        secondColumn.prefWidthProperty().bind(statusTable.widthProperty().multiply(0.249));
        secondColumn.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(getValueAt(v.getValue(), 1)));
        secondColumn.setCellFactory(col -> new StatusTableCell(1));

        TableColumn<TeamStatusInfo, Object> thirdColumn = new TableColumn<>(statusModel.getColumnName(2));
        JavaFXUtil.addStyleClass(thirdColumn, "team-status-thirdColumn");
        thirdColumn.prefWidthProperty().bind(statusTable.widthProperty().multiply(0.249));
        thirdColumn.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(getValueAt(v.getValue(), 2)));
        thirdColumn.setCellFactory(col -> new StatusTableCell(2));

        statusTable.getColumns().setAll(firstColumn, secondColumn, thirdColumn);

        // An unconstrained policy is necessary for the automatic column width calculation bindings
        // to work. If set to constrained, column widths all come out as the same (for some reason).
        statusTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        statusTable.setBackground(null);
        JavaFXUtil.addStyleClass(statusTable, "status-table");

        // Wrap in border pane so that padding works right:
        return new BorderPane(statusTable);
    }

    /**
     * Create the Refresh button and status progress bar
     */
    private void makeRefreshPaneComponents()
    {
        // progress bar
        progressBar = new ActivityIndicator();
        progressBar.setRunning(false);
        BorderPane.setAlignment(progressBar, Pos.CENTER);

        //refresh button
        refreshButton = new Button(Config.getString("team.status.refresh"));
        refreshButton.setDisable(true);
        refreshButton.setOnAction(event -> update());
        refreshButton.requestFocus();
    }

    /**
     * Set up the buttons panel to contain a close button, and register the close action.
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
            case 1:
                return info.getStatus();
            case 2:
                return info.getStatus(false);
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

        @OnThread(Tag.Worker)
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
                if (statusTable.getItems() != null ) {
                    statusTable.getItems().clear();
                }
                statusTable.refresh();
                statusTable.setItems(resources);
                
                // Sort by status, descending. The sort above actually does this, but this makes it visible
                // in the UI by marking the second column header with an indicator:
                TableColumn<TeamStatusInfo,?> secondColumn = statusTable.getColumns().get(1);
                statusTable.getSortOrder().add(secondColumn);
                secondColumn.setSortType(SortType.DESCENDING);
            }
        }
    }
}
