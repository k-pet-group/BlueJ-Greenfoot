/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2014,2016,2017,2018,2019  Michael Kolling and John Rosenberg

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import bluej.Config;
import bluej.groupwork.actions.UpdateAction;
import bluej.groupwork.Repository;
import bluej.groupwork.StatusHandle;
import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamStatusInfo.Status;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamViewFilter;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.UpdateFilter;
import bluej.pkgmgr.BlueJPackageFile;
import bluej.pkgmgr.Project;
import bluej.utility.DialogManager;
import bluej.utility.FXWorker;
import bluej.utility.javafx.FXCustomizedDialog;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.Utility;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A user interface for showing files to be updated
 *
 * @author Bruce Quig
 * @author Davin McCall
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class UpdateFilesFrame extends FXCustomizedDialog<Void>
{
    private CheckBox includeLayoutCheckbox;
    private ActivityIndicator progressBar;
    private UpdateAction updateAction;
    private Button updateButton;
    private UpdateWorker updateWorker;

    private Project project;
    private Repository repository;
    private ObservableList<UpdateStatus> updateListModel;

    private Set<TeamStatusInfo> changedLayoutFiles = new HashSet<>(); // set of TeamStatusInfo
    private Set<File> forcedLayoutFiles = new HashSet<>(); // set of File

    private static UpdateStatus noFilesToUpdate = new UpdateStatus(Config.getString("team.noupdatefiles"));
    private static UpdateStatus needUpdate = new UpdateStatus(Config.getString("team.pullNeeded"));

    private boolean includeLayout = true;
    private boolean pullWithNoChanges = false;

    /**
     * Constructor for UpdateFilesFrame.
     */
    public UpdateFilesFrame(Project project)
    {
        super(null, "team.update.title", "team-update-files");
        this.project = project;
        updateAction.useButton(project, updateButton);
        buildUI();
    }

    @Override
    protected Node wrapButtonBar(Node original)
    {
        updateAction = new UpdateAction(this);
        updateButton = new Button();
        // Note that we can't connect the button and action yet as we are called by the
        // superclass constructor, and project is not set yet.
        updateButton.requestFocus();
        
        progressBar = new ActivityIndicator();
        progressBar.setRunning(false);
        
        HBox updateButtonPane = new HBox();
        JavaFXUtil.addStyleClass(updateButtonPane, "button-hbox");
        updateButtonPane.getChildren().addAll(progressBar, updateButton, original);
        
        return updateButtonPane;
    }

    /**
     * Create the user-interface for the error display dialog.
     */
    private void buildUI()
    {
        VBox mainPane = new VBox();
        JavaFXUtil.addStyleClass(mainPane, "main-pane");

        updateListModel = FXCollections.observableArrayList();
        Label updateFilesLabel = new Label(Config.getString("team.update.files"));
        ListView<UpdateStatus> updateFiles = new ListView<>(updateListModel);
        updateFiles.setCellFactory(param -> new FileRendererCell(project));
        updateFiles.setEditable(false);

        ScrollPane updateFileScrollPane = new ScrollPane(updateFiles);
        updateFileScrollPane.setFitToWidth(true);
        updateFileScrollPane.setFitToHeight(true);

        includeLayoutCheckbox = new CheckBox(Config.getString("team.update.includelayout"));
        includeLayoutCheckbox.setDisable(true);
        includeLayoutCheckbox.setOnAction(event -> {
            CheckBox layoutCheck = (CheckBox)event.getSource();
            includeLayout = layoutCheck.isSelected();
            resetForcedFiles();
            if (includeLayout)
            {
                addModifiedLayouts();
            }
            else
            {
                removeModifiedLayouts();
            }
        });

        mainPane.getChildren().addAll(updateFilesLabel, updateFileScrollPane, includeLayoutCheckbox);
        getDialogPane().setContent(mainPane);
        
        prepareButtonPane();
    }

    /**
     * Create the button panel with a close button
     * @return Pane the buttonPanel
     */
    private void prepareButtonPane()
    {
        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        this.setOnCloseRequest(event -> {
            if (updateWorker != null) {
                updateWorker.abort();
            }
            if (updateAction != null) {
                updateAction.cancel();
            }
            close();
        });
    }

    public void setVisible(boolean visible)
    {
        if (visible) {
            show();
            // we want to set update action disabled until we know that
            // there's something to update
            updateAction.setEnabled(false);
            includeLayoutCheckbox.setSelected(false);
            includeLayoutCheckbox.setDisable(true);
            changedLayoutFiles.clear();
            forcedLayoutFiles.clear();
            updateListModel.clear();

            repository = project.getRepository();

            if (repository != null) {
                try {
                    project.saveAllEditors();
                    project.saveAll();
                }
                catch (IOException ioe) {
                    String msg = DialogManager.getMessage("team-error-saving-project");
                    if (msg != null) {
                        msg = Utility.mergeStrings(msg, ioe.getLocalizedMessage());
                        String msgFinal = msg;
                        DialogManager.showErrorTextFX(this.asWindow(), msgFinal);
                    }
                }
                startProgress();
                updateWorker = new UpdateWorker();
                updateWorker.start();
            }
            else {
                hide();
            }
        }
        else {
            hide();
        }
    }

    private void removeModifiedLayouts()
    {
        // remove modified layouts from list of files shown for commit:
        updateListModel.removeIf(updateStatus ->
            changedLayoutFiles.stream().anyMatch(statusInfo ->
                updateStatus.infoStatus != null &&
                        statusInfo.getFile().equals(updateStatus.infoStatus.getFile())
            )
        );

        if(updateListModel.isEmpty())
        {
            if (pullWithNoChanges)
            {
                updateListModel.add(needUpdate);
            }
            else
            {
                updateListModel.add(noFilesToUpdate);
                updateAction.setEnabled(false);
            }
        }
    }

    /**
     * Add the modified layouts to the displayed list of files to be updated. Should only be
     * called if there is at least one modified layout file.
     */
    private void addModifiedLayouts()
    {
        updateListModel.remove(noFilesToUpdate);
        updateListModel.remove(needUpdate);
        updateAction.setEnabled(true);
        
        for (TeamStatusInfo statusInfo : changedLayoutFiles)
        {
            updateListModel.add(new UpdateStatus(statusInfo));
        }
    }

    /**
     * Start the activity indicator.
     */
    public void startProgress()
    {
        progressBar.setRunning(true);
    }

    /**
     * Stop the activity indicator. Call from any thread.
     */
    public void stopProgress()
    {
        progressBar.setRunning(false);
    }

    public void disableLayoutCheck()
    {
        includeLayoutCheckbox.setDisable(true);
    }

    /**
     * Re-set the forced files in the update action. This needs to be
     * done when the "include layout" option is toggled.
     */
    private void resetForcedFiles()
    {
        Set<File> forcedFiles = new HashSet<>(forcedLayoutFiles);
        if (includeLayout) {
            forcedFiles.addAll(changedLayoutFiles.stream().map(TeamStatusInfo::getFile).collect(Collectors.toSet()));
        }
        updateAction.setFilesToForceUpdate(forcedFiles);
    }

    /**
     * Worker to do the actual status check (to populate commit dialog) in the background, to
     * avoid blocking the UI.
     */
    class UpdateWorker extends FXWorker implements StatusListener
    {
        List<TeamStatusInfo> response;
        TeamworkCommand command;
        TeamworkCommandResult result;
        private boolean aborted;
        private StatusHandle statusHandle;

        @OnThread(Tag.FXPlatform)
        public UpdateWorker()
        {
            super();
            response = new ArrayList<>();
            FileFilter filter = project.getTeamSettingsController().getFileFilter(false);
            command = repository.getStatus(this, filter, true);
        }

        /* (non-Javadoc)
         * @see bluej.groupwork.StatusListener#gotStatus(bluej.groupwork.TeamStatusInfo)
         */
        @OnThread(Tag.Any)
        public void gotStatus(TeamStatusInfo info)
        {
            response.add(info);
        }

        /* (non-Javadoc)
         * @see bluej.groupwork.StatusListener#statusComplete(bluej.groupwork.CommitHandle)
         */
        @OnThread(Tag.Worker)
        public void statusComplete(StatusHandle statusHandle)
        {
            pullWithNoChanges = statusHandle.pullNeeded();
            this.statusHandle = statusHandle;
        }

        @OnThread(Tag.Worker)
        public Object construct()
        {
            result = command.getResult();
            return response;
        }

        public void abort()
        {
            command.cancel();
            aborted = true;
        }

        @OnThread(Tag.FXPlatform)
        public void finished()
        {
            stopProgress();
            if (! aborted) {
                if (result.isError()) {
                    UpdateFilesFrame.this.dialogThenHide(() -> TeamUtils.handleServerResponseFX(result, UpdateFilesFrame.this.asWindow()));
                }
                else {
                    Set<File> filesToUpdate = new HashSet<>();
                    Set<File> modifiedLayoutFiles = new HashSet<>();

                    List<TeamStatusInfo> info = response;
                    getUpdateFileSet(info, filesToUpdate, modifiedLayoutFiles);

                    // Build the actual set of files to update. If there are new or removed
                    // directories, don't include files within.
                    Set<File> updateFiles = new HashSet<>();
                    for (File file : filesToUpdate) {
                        if (!filesToUpdate.contains(file.getParentFile())) {
                            updateFiles.add(file);
                        }
                    }
                    forcedLayoutFiles.removeIf(file -> filesToUpdate.contains(file.getParentFile()));

                    updateAction.setStatusHandle(statusHandle);
                    updateAction.setFilesToUpdate(updateFiles);
                    resetForcedFiles();

                    if (includeLayout && ! changedLayoutFiles.isEmpty())
                    {
                        for (TeamStatusInfo statusInfo : changedLayoutFiles)
                        {
                            updateListModel.add(new UpdateStatus(statusInfo));
                        }
                    }

                    if(updateListModel.isEmpty() && !pullWithNoChanges)
                    {
                        updateListModel.add(noFilesToUpdate);
                    }
                    else
                    {
                        if (pullWithNoChanges && updateListModel.isEmpty())
                        {
                            updateListModel.add(needUpdate);
                        }
                        updateAction.setEnabled(true);
                    }
                }
            }
        }
        
        /**
         * Go through the status list, and figure out which files to update, and
         * which to force update.
         *
         * @param info  The list of files with status (List of TeamStatusInfo)
         * @param filesToUpdate  The set to store the files to update in
         * @param modifiedLayoutFiles  The set to store the files to be force updated in
         *                       (any files in this set prevent update from occurring)
         */
        private void getUpdateFileSet(List<TeamStatusInfo> info, Set<File> filesToUpdate, Set<File> modifiedLayoutFiles)
        {
            getUpdateFileSetDist(info, filesToUpdate, modifiedLayoutFiles);

            if (! changedLayoutFiles.isEmpty()) {
                includeLayoutCheckbox.setDisable(false);
                includeLayoutCheckbox.setSelected(includeLayout);
            }
        }
        
        /**
        * Go through file statuses and determine which files will be updated (distributed version control).
        */
        private void getUpdateFileSetDist(List<TeamStatusInfo> info, Set<File> filesToUpdate, Set<File> modifiedLayoutFiles)
        {
            UpdateFilter filter = new UpdateFilter();
            TeamViewFilter viewFilter = new TeamViewFilter();
            for (TeamStatusInfo statusInfo : info) {
                Status status = statusInfo.getStatus(false);
                if (filter.acceptDist(status)) {
                    if (! BlueJPackageFile.isPackageFileName(statusInfo.getFile().getName()))
                    {
                        updateListModel.add(new UpdateStatus(statusInfo));
                        filesToUpdate.add(statusInfo.getFile());
                    }
                    else {
                        if (! viewFilter.accept(statusInfo)) {
                            // If the file should not be viewed, just ignore it.
                        }
                        else if (status != Status.NEEDS_UPDATE && status != Status.NEEDS_MERGE)
                        {
                            // The package file is new or removed. There is no
                            // option not to include it in the update.
                            updateListModel.add(new UpdateStatus(statusInfo));
                            forcedLayoutFiles.add(statusInfo.getFile());
                        }
                        else
                        {
                            // add file to list of layout files that may optionally be updated:
                            modifiedLayoutFiles.add(statusInfo.getFile());
                            // keep track of StatusInfo objects representing changed diagrams
                            changedLayoutFiles.add(statusInfo);
                        }
                    }
                }
            }
        }
    }
}
