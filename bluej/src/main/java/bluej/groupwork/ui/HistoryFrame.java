/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012,2014,2016,2017,2019  Michael Kolling and John Rosenberg
 
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.groupwork.HistoryInfo;
import bluej.groupwork.LogHistoryListener;
import bluej.groupwork.Repository;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.utility.DialogManager;
import bluej.utility.FXWorker;
import bluej.utility.javafx.FXCustomizedDialog;
import bluej.utility.javafx.JavaFXUtil;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A frame to display the commit history, including dates, users, revisions
 * and commit comments.
 *
 * @author Davin McCall
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class HistoryFrame extends FXCustomizedDialog<Void>
{
    private Project project;
    private HistoryWorker worker;

    private List<HistoryInfo> historyInfoList;
    private ObservableList<HistoryInfo> listModel = FXCollections.observableArrayList();

    private ListView<HistoryInfo> historyList = new ListView<>(listModel);
    private ComboBox<String> fileFilterCombo = new ComboBox<>();
    private ComboBox<String> userFilterCombo = new ComboBox<>();
    private ActivityIndicator activityBar = new ActivityIndicator();

    /**
     * Create a new HistoryFrame.
     */
    public HistoryFrame(PkgMgrFrame pmf)
    {
        super(pmf.getWindow(), "team.history.title", "team-history");
        project = pmf.getProject();
        prepareData();
        prepareButtonPane();
        getDialogPane().setContent(makeMainPane());
        DialogManager.centreDialog(this);
    }

    /**
     * Construct the main pane UI components.
     */
    private Pane makeMainPane()
    {
        // History list
        historyList.setCellFactory(param -> new HistoryCell());
        ScrollPane historyPane = new ScrollPane(historyList);
        historyPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        historyPane.setFitToWidth(true);
        historyPane.setFitToHeight(true);

        HBox filterBox = new HBox();
        filterBox.setAlignment(Pos.BASELINE_LEFT);
        filterBox.getChildren().addAll(new Label(Config.getString("team.history.filefilter") + " "), fileFilterCombo,
                                       new Label(Config.getString("team.history.userfilter") + " "), userFilterCombo,
                                       activityBar);
        HBox.setMargin(fileFilterCombo, new Insets(0, 40, 0, 0));
        HBox.setMargin(userFilterCombo, new Insets(0, 40, 0, 0));

        // Main content pane
        VBox mainPane = new VBox();
        JavaFXUtil.addStyleClass(mainPane, "main-pane");
        mainPane.getChildren().addAll(historyPane, filterBox);
        VBox.setVgrow(historyPane, Priority.ALWAYS);
        return mainPane;
    }

    /**
     * Create the button panel with a Close button
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

    private void prepareData()
    {
        Repository repository = project.getTeamSettingsController().trytoEstablishRepository(false);

        if (repository != null) {
            worker = new HistoryWorker(repository);
            worker.start();
            activityBar.setRunning(true);
        }
    }

    /**
     * Filter the history info list according to the selected file and user
     * filters. The filtered list is then displayed.
     */
    private void refilter()
    {
        String user = null;
        if (userFilterCombo.getSelectionModel().getSelectedIndex() > 0) {
            user = userFilterCombo.getSelectionModel().getSelectedItem();
        }

        String file = null;
        if (fileFilterCombo.getSelectionModel().getSelectedIndex() > 0) {
            file = fileFilterCombo.getSelectionModel().getSelectedItem();
        }

        List<HistoryInfo> displayList;
        if (user == null && file == null) {
            displayList = historyInfoList;
        }
        else {
            displayList = new ArrayList<>();
            for (HistoryInfo info : historyInfoList) {
                if (user != null && !info.getUser().equals(user)) {
                    continue;
                }
                if (file != null && !historyInfoHasFile(info, file)) {
                    continue;
                }

                displayList.add(info);
            }
        }

        listModel.setAll(displayList);
    }

    /**
     * Check whether a history item pertains at all to a particular file
     */
    private boolean historyInfoHasFile(HistoryInfo info, String file)
    {
        return Arrays.stream(info.getFiles()).anyMatch(f -> f.equals(file));
    }

    /**
     * Reset the filter boxes (user filter and file filter), adding a complete list
     * of all users and files.
     */
    private void resetFilterBoxes()
    {
        SortedSet<String> files = new TreeSet<>();
        SortedSet<String> users = new TreeSet<>();

        for (HistoryInfo info : historyInfoList) {
            users.add(info.getUser());
            Collections.addAll(files, info.getFiles());
        }

        fileFilterCombo.getItems().clear();
        fileFilterCombo.getItems().add(Config.getString("team.history.allFiles"));
        fileFilterCombo.getItems().addAll(files);
        fileFilterCombo.getSelectionModel().selectFirst();
        fileFilterCombo.setOnAction(e -> refilter());

        userFilterCombo.getItems().clear();
        userFilterCombo.getItems().add(Config.getString("team.history.allUsers"));
        userFilterCombo.getItems().addAll(users);
        userFilterCombo.getSelectionModel().selectFirst();
        userFilterCombo.setOnAction(e -> refilter());
    }

    /**
     * A worker class to fetch the required information from the repository
     * in the background.
     */
    private class HistoryWorker extends FXWorker implements LogHistoryListener
    {
        private List<HistoryInfo> responseList;
        private Repository repository;
        private TeamworkCommand command;
        private TeamworkCommandResult response;

        public HistoryWorker(Repository repository)
        {
            this.responseList = new ArrayList<>();
            command = repository.getLogHistory(this);
            this.repository = repository;
        }

        @OnThread(Tag.Worker)
        public Object construct()
        {
            response = command.getResult();
            return response;
        }

        @OnThread(Tag.Any)
        public void logInfoAvailable(HistoryInfo hInfo)
        {
            responseList.add(hInfo);
        }

        public void finished()
        {
            if (command != null) {
                activityBar.setRunning(false);
                command = null; // marks the command as finished
                if (response.isError()) {
                    HistoryFrame.this.dialogThenHide(() -> TeamUtils.handleServerResponseFX(response, HistoryFrame.this.asWindow()));
                }
                else {
                    responseList.sort(new DateCompare());

                    // Make the history list forget the preferred size that was forced
                    // upon it when we built the frame.
                    // TODO Maybe not needed in FX
                    historyList.setPrefSize(-1, -1);

                    listModel.setAll(responseList);
                    historyInfoList = responseList;

                    resetFilterBoxes();

                    DataCollector.teamHistoryProject(project, repository);
                }
            }
        }

        public void abort()
        {
            activityBar.setRunning(false);
            if (command != null) {
                command.cancel();
                command = null;
            }
        }
    }
}

/**
 * A comparator to sort HistoryInfo objects by date.
 *
 * @author Davin McCall
 */
class DateCompare implements Comparator<HistoryInfo>
{
    public int compare(HistoryInfo hi0, HistoryInfo hi1)
    {
        return hi1.getDate().compareTo(hi0.getDate());
    }
}
