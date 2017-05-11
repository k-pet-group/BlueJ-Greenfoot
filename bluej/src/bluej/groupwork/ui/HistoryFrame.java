/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012,2014,2016,2017  Michael Kolling and John Rosenberg
 
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
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
import bluej.utility.FXWorker;
import bluej.utility.javafx.FXCustomizedDialog;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A frame to display the commit history, including dates, users, revisions
 * and commit comments.
 *
 * @author Davin McCall
 * @author Amjad Altadmri
 */
public class HistoryFrame extends FXCustomizedDialog<Void>
{
    private Project project;
    private ActivityIndicatorFX activityBar;
    private HistoryWorker worker;

    private ObservableList<HistoryInfo> listModel = new SimpleListProperty<>();
    private ListView<HistoryInfo> historyList = new ListView<>(listModel);
    private ScrollPane historyPane = new ScrollPane(historyList);
    private List<HistoryInfo> historyInfoList;

    private ComboBox<String> fileFilterCombo = new ComboBox<>();
    private ComboBox<String> userFilterCombo = new ComboBox<>();
    private EventHandler<ActionEvent> filterActionListener;
    private Label filterSpacer;

    /**
     * Create a new HistoryFrame.
     */
    public HistoryFrame(PkgMgrFrame pmf)
    {
        super(pmf.getFXWindow(), "team.history.title", "team-history");
        project = pmf.getProject();
        buildUI();
        prepareData();
    }

    /**
     * Construct the UI components.
     */
    private void buildUI()
    {
        // Content pane
        VBox contentPane = new VBox();
        setContentPane(contentPane);

        // History list
        historyList.setCellFactory(param -> new HistoryCell());
        historyPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        contentPane.getChildren().add(historyPane);

        // Find a suitable size for the history list
        // TODO maybe not needed for FX
        listModel.setAll(getTemporaryDumpInfoList());
        listModel.setAll(Collections.emptyList());
//        Dimension size = historyList.getPreferredSize();
//        historyList.setPreferredSize(size);

        contentPane.getChildren().add(new Separator(Orientation.VERTICAL));

        // File and user filter boxes
        HBox filterBox = new HBox();
        filterBox.getChildren().add(new Label(Config.getString("team.history.filefilter") + " "));
        fileFilterCombo.setDisable(true);
        filterBox.getChildren().add(fileFilterCombo);
        filterBox.getChildren().add(new Separator(Orientation.HORIZONTAL));
        filterBox.getChildren().add(new Label(Config.getString("team.history.userfilter") + " "));

        userFilterCombo.setDisable(true);
        filterBox.getChildren().add(userFilterCombo);
        // Add in a spacer, which helps ensure the initial size of the frame is ok.
        // When the filter combo boxes are filled, the spacer is removed.
        // TODO these should be changed to Separators and styled in the CSS
        filterSpacer = new Label("                              ");
        userFilterCombo.getItems().add("         ");
        fileFilterCombo.getItems().add("               ");
        filterBox.getChildren().add(filterSpacer);
        contentPane.getChildren().add(filterBox);

        filterActionListener = e -> refilter();

        contentPane.getChildren().add(new Separator(Orientation.VERTICAL));

        // Activity indicator and close button
        HBox buttonBox = new HBox();
        activityBar = new ActivityIndicatorFX();
        buttonBox.getChildren().add(activityBar);
//        buttonBox.getChildren().add(Box.createHorizontalGlue());
        Button closeButton = new Button(Config.getString("close"));
        closeButton.setOnAction(event -> {
            if (worker != null) {
                worker.cancel();
            }
            close();
        });
        buttonBox.getChildren().add(closeButton);
        contentPane.getChildren().add(buttonBox);
    }

    private List<HistoryInfo> getTemporaryDumpInfoList()
    {
        List<HistoryInfo> tempList = new ArrayList<>(5);
        HistoryInfo tempInfo;
        if (project.getTeamSettingsController().isDVCS()){
            tempInfo = new HistoryInfo(new String[] {"somepath/abcdefg.java"}, "", "2006/11/34 12:34:56", "John Smith J. Doe", "this is the expected comment length of comments");
        } else {
            tempInfo = new HistoryInfo(new String[] {"somepath/abcdefg.java"}, "1.1", "2006/11/34 12:34:56", "abraham", "this is the expected comment length of comments");
        }

        for (int i = 0; i < 8; i++) {
            tempList.add(tempInfo);
        }
        return tempList;
    }

    private void prepareData()
    {
        Repository repository;
        if (project.getTeamSettingsController().isDVCS()){
            //don't connect to the remote repository if git.
            repository = project.getTeamSettingsController().getRepository(false);
        } else {
            //we need to connect to the remote repository if svn.
            repository = project.getRepository();
        }

        if (repository != null) {
            worker = new HistoryWorker(repository);
            worker.start();
            activityBar.setRunning(true);
        }
    }

    public void abort()
    {
        if (worker != null) {
            worker.cancel();
        }
    }

    /**
     * Filter the history info list according to the selected file and user
     * filters. The filtered list is then displayed.
     */
    private void refilter()
    {
        String user = null;
        int userIndex = userFilterCombo.getSelectionModel().getSelectedIndex();
        if (userIndex != 0) {
            user = userFilterCombo.getItems().get(userIndex);
        }

        String file = null;
        int fileIndex = fileFilterCombo.getSelectionModel().getSelectedIndex();
        if (fileIndex != 0) {
            file = fileFilterCombo.getItems().get(fileIndex);
        }

        List<HistoryInfo> displayList;
        if (user == null && file == null) {
            displayList = historyInfoList;
        }
        else {
            // TODO change to streams
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
        Set<String> users = new HashSet<>();
        Set<String> files = new HashSet<>();

        for (HistoryInfo info : historyInfoList) {
            users.add(info.getUser());
            Collections.addAll(files, info.getFiles());
        }

        List<String> usersList = new ArrayList<>(users);
        Collections.sort(usersList);
        List<String> filesList = new ArrayList<>(files);
        Collections.sort(filesList);

        userFilterCombo.getItems().clear();
        userFilterCombo.getItems().add(Config.getString("team.history.allUsers"));
        userFilterCombo.getItems().addAll(usersList);
        userFilterCombo.setOnAction(filterActionListener);
        userFilterCombo.setDisable(false);

        fileFilterCombo.getItems().clear();
        fileFilterCombo.getItems().add(Config.getString("team.history.allFiles"));
        fileFilterCombo.getItems().addAll(filesList);
        fileFilterCombo.setOnAction(filterActionListener);
        fileFilterCombo.setDisable(false);

        filterSpacer.setVisible(false);
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

        @OnThread(Tag.Unique)
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

        public void cancel()
        {
            if (command != null) {
                activityBar.setRunning(false);
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
