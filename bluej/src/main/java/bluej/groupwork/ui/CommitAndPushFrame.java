/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2017,2018,2019  Michael Kolling and John Rosenberg
 
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
import bluej.groupwork.actions.CommitAction;
import bluej.groupwork.actions.PushAction;
import bluej.groupwork.Repository;
import bluej.groupwork.StatusHandle;
import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamStatusInfo.Status;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.pkgmgr.BlueJPackageFile;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FXWorker;
import bluej.utility.javafx.FXCustomizedDialog;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.NoMultipleSelectionModel;
import bluej.utility.Utility;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A user interface to commit and push. Used by DCVS systems, like Git.
 *
 * @author Fabio Heday
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class CommitAndPushFrame extends FXCustomizedDialog<Void>
{
    private final Project project;
    private Repository repository;

    private Set<TeamStatusInfo> changedLayoutFiles = new HashSet<>();
    private ObservableList<TeamStatusInfo> commitListModel = FXCollections.observableArrayList();
    private ObservableList<TeamStatusInfo> pushListModel = FXCollections.observableArrayList();

    private final CheckBox includeLayout = new CheckBox(Config.getString("team.commit.includelayout"));
    private final TextArea commitText = new TextArea();
    private final ActivityIndicator progressBar = new ActivityIndicator();
    private final ListView<TeamStatusInfo> pushFiles = new ListView<>(pushListModel);

    private CommitAction commitAction;
    private PushAction pushAction;
    private CommitAndPushWorker commitAndPushWorker;

    // Sometimes, usually after a conflict resolution, we may need to push in order
    // to update HEAD, even though no files show as changed:
    private boolean pushNeeded = false;
    private boolean updateNeeded = false;

    public CommitAndPushFrame(Project proj)
    {
        super(null, "team.commit.dcvs.title", "team-commit-push");
        project = proj;
        repository = project.getTeamSettingsController().trytoEstablishRepository(false);
        getDialogPane().setContent(makeMainPane());
        prepareButtonPane();
    }

    /**
     * Create the user-interface for the error display dialog.
     */
    private Pane makeMainPane()
    {
        ListView<TeamStatusInfo> commitFiles = new ListView<>(commitListModel);
        commitFiles.setPlaceholder(new Label(Config.getString("team.nocommitfiles")));
        commitFiles.setCellFactory(param -> new TeamStatusInfoCell(project));
        commitFiles.setSelectionModel(new NoMultipleSelectionModel<>());
        commitFiles.disableProperty().bind(Bindings.isEmpty(commitListModel));

        ScrollPane commitFileScrollPane = new ScrollPane(commitFiles);
        commitFileScrollPane.setFitToWidth(true);
        commitFileScrollPane.setFitToHeight(true);
        VBox.setMargin(commitFileScrollPane, new Insets(0, 0, 20, 0));

        commitText.setPrefRowCount(20);
        commitText.setPrefColumnCount(35);
        commitText.setPromptText(Config.getString("team.commit.message"));
        VBox.setMargin(commitText, new Insets(0, 0, 10, 0));

        commitAction = new CommitAction(this);
        Button commitButton = new Button();
        commitAction.useButton(project, commitButton);
        commitButton.requestFocus();
        //Bind commitText properties to enable the commit button if there is a comment.
        commitText.disableProperty().bind(Bindings.isEmpty(commitListModel));
        commitAction.disabledProperty().bind(Bindings.or(commitText.disabledProperty(),
                commitText.textProperty().isEmpty()));

        includeLayout.setOnAction(event -> {
            CheckBox layoutCheck = (CheckBox) event.getSource();
            if (layoutCheck.isSelected()) {
                addModifiedLayouts();
            } // unselected
            else {
                removeModifiedLayouts();
            }
        });
        includeLayout.setDisable(true);

        HBox commitButtonPane = new HBox();
        JavaFXUtil.addStyleClass(commitButtonPane, "button-hbox");
        commitButtonPane.setAlignment(Pos.CENTER_RIGHT);
        commitButtonPane.getChildren().addAll(includeLayout, commitButton);

        pushAction = new PushAction(this);
        Button pushButton = new Button();
        pushAction.useButton(project, pushButton);

        Label pushFilesLabel = new Label(Config.getString("team.commitPush.push.files"));
        pushFiles.setCellFactory(param -> new TeamStatusInfoCell(project));
        pushFiles.setSelectionModel(new NoMultipleSelectionModel<>());
        pushFiles.disableProperty().bind(Bindings.isEmpty(pushListModel));
        ScrollPane pushFileScrollPane = new ScrollPane(pushFiles);
        pushFileScrollPane.setFitToWidth(true);
        pushFileScrollPane.setFitToHeight(true);

        HBox pushButtonPane = new HBox();
        progressBar.setRunning(false);
        JavaFXUtil.addStyleClass(pushButtonPane, "button-hbox");
        pushButtonPane.setAlignment(Pos.CENTER_RIGHT);
        pushButtonPane.getChildren().addAll(progressBar, pushButton);

        VBox mainPane = new VBox();
        JavaFXUtil.addStyleClass(mainPane, "main-pane");
        mainPane.getChildren().addAll(new Label(Config.getString("team.commitPush.commit.files")),
                commitFileScrollPane,
                new Label(Config.getString("team.commit.comment")), commitText,
                commitButtonPane,
                new Separator(Orientation.HORIZONTAL),
                pushFilesLabel, pushFileScrollPane,
                pushButtonPane);
        return mainPane;
    }

    /**
     * Prepare the button panel with a close button
     */
    private void prepareButtonPane()
    {
        getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
        this.setOnCloseRequest(event -> {
            if (commitAndPushWorker != null) {
                commitAndPushWorker.abort();
            }
            if (commitAction != null) {
                commitAction.cancel();
            }
            close();
        });
    }

    @OnThread(Tag.FXPlatform)
    public void setVisible()
    {
        // we want to set comments and commit action to disabled
        // until we know there is something to commit
        commitText.setText("");//
        includeLayout.setSelected(false);
        includeLayout.setDisable(true);//
        changedLayoutFiles.clear();
        commitListModel.clear();
        pushAction.setEnabled(false);
        pushListModel.clear();

        repository = project.getTeamSettingsController().trytoEstablishRepository(false);

        if (repository != null) {
            try {
                project.saveAllEditors();
                project.saveAll();
            } catch (IOException ioe) {
                String msg = DialogManager.getMessage("team-error-saving-project");
                if (msg != null) {
                    msg = Utility.mergeStrings(msg, ioe.getLocalizedMessage());
                    String msgFinal = msg;
                    DialogManager.showErrorTextFX(this.asWindow(), msgFinal);
                }
            }
            startProgress();
            commitAndPushWorker = new CommitAndPushWorker();
            commitAndPushWorker.start();
            if (!isShowing()) {
                show();
            }
        }
        else {
            hide();
        }
    }

    public void setComment(String newComment)
    {
        commitText.setText(newComment);
    }

    public void reset()
    {
        commitListModel.clear();
        pushListModel.clear();
        setComment("");
        progressBar.setMessage("");
    }

    private void removeModifiedLayouts()
    {
        // remove modified layouts from list of files shown for commit
        commitListModel.removeAll(changedLayoutFiles);
    }

    public String getComment()
    {
        return commitText.getText();
    }

    /**
     * Get a list of the layout files to be committed
     *
     * @return
     */
    public Set<File> getChangedLayoutFiles()
    {
        return changedLayoutFiles.stream().map(info -> info.getFile()).collect(Collectors.toSet());
    }

    /**
     * Get a set of the layout files which have changed (with status info).
     *
     * @return the set of the layout files which have changed.
     */
    public Set<TeamStatusInfo> getChangedLayoutInfo()
    {
        return changedLayoutFiles;
    }

    public boolean includeLayout()
    {
        return includeLayout != null && includeLayout.isSelected();
    }

    private void addModifiedLayouts()
    {
        // add diagram layout files to list of files to be committed
        Set<File> displayedLayouts = new HashSet<>();
        for (TeamStatusInfo info : changedLayoutFiles) {
            File parentFile = info.getFile().getParentFile();
            if (!displayedLayouts.contains(parentFile)) {
                commitListModel.add(info);
                displayedLayouts.add(info.getFile().getParentFile());
            }
        }
    }

    /**
     * Start the activity indicator.
     */
    @OnThread(Tag.FXPlatform)
    public void startProgress()
    {
        progressBar.setRunning(true);
    }

    /**
     * Stop the activity indicator. Call from any thread.
     */
    @OnThread(Tag.Any)
    public void stopProgress()
    {
        JavaFXUtil.runNowOrLater(() -> progressBar.setRunning(false));
    }

    @OnThread(Tag.FXPlatform)
    public Project getProject()
    {
        return project;
    }

    @OnThread(Tag.FXPlatform)
    public void displayMessage(String msg)
    {
        progressBar.setMessage(msg);
    }

    @OnThread(Tag.FXPlatform)
    public Window asWindow()
    {
        Scene scene = getDialogPane().getScene();
        if (scene == null)
            return null;
        else
            return scene.getWindow();
    }

    /**
     * Gets the list of files that would be pushed by a push.
     */
    public List<File> getFilesToPush()
    {
        return Utility.mapList(pushListModel, s -> s.getFile());
    }

    class CommitAndPushWorker extends FXWorker implements StatusListener
    {
        List<TeamStatusInfo> response;
        TeamworkCommand command;
        TeamworkCommandResult result;
        private boolean aborted, isPushAvailable;

        public CommitAndPushWorker()
        {
            super();
            response = new ArrayList<>();
            FileFilter filter = project.getTeamSettingsController().getFileFilter(false);
            command = repository.getStatus(this, filter, false);
        }
        /*
         * @see bluej.groupwork.StatusListener#gotStatus(bluej.groupwork.TeamStatusInfo)
         */
        @OnThread(Tag.Any)
        @Override
        public void gotStatus(TeamStatusInfo info)
        {
            response.add(info);
        }

        /*
         * @see bluej.groupwork.StatusListener#statusComplete(bluej.groupwork.CommitHandle)
         */
        @OnThread(Tag.Worker)
        @Override
        public void statusComplete(StatusHandle statusHandle)
        {
            commitAction.setStatusHandle(statusHandle);
            pushNeeded = statusHandle.pushNeeded();
            updateNeeded = statusHandle.pullNeeded();
            pushAction.setStatusHandle(statusHandle);
        }

        @OnThread(Tag.Worker)
        @Override
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

        @Override
        public void finished()
        {
            stopProgress();
            if (!aborted)
            {
                if (result.isError())
                {
                    TeamUtils.handleServerResponseFX(result, CommitAndPushFrame.this.asWindow());
                    CommitAndPushFrame.this.hide();
                }
                else if (response != null)
                {
                    Set<File> filesToCommit = new HashSet<>();
                    Set<File> filesToAdd = new LinkedHashSet<>();
                    Set<File> filesToDelete = new HashSet<>();

                    List<TeamStatusInfo> info = response;
                    getCommitFileSets(info, filesToCommit, filesToAdd, filesToDelete, changedLayoutFiles);

                    includeLayout.setDisable(changedLayoutFiles.isEmpty());
                    
                    commitAction.setFiles(filesToCommit);
                    commitAction.setNewFiles(filesToAdd);
                    commitAction.setDeletedFiles(filesToDelete);
                    updateListModel(commitListModel, filesToCommit, info);
                    updateListModel(commitListModel, filesToAdd, info);
                    updateListModel(commitListModel, filesToDelete, info);

                    if (updateNeeded)
                    {
                        // It's not possible to push if the remote branch is ahead
                        pushListModel.clear();
                        isPushAvailable = false;
                    }
                    else
                    {
                        // populate files ready to push:
                        Set<File> filesToPush = new HashSet<>();
                        getPushFileSets(info, filesToPush);
                        updateListModel(pushListModel, filesToPush, info);
                        this.isPushAvailable = pushNeeded;
                    }

                    pushAction.setEnabled(this.isPushAvailable);
                }

                if (!commitListModel.isEmpty())
                {
                    commitText.requestFocus();
                }

                if (pushListModel.isEmpty())
                {
                    if (isPushAvailable)
                    {
                        pushFiles.setPlaceholder(new Label(Config.getString("team.pushNeeded")));
                    }
                    else if (updateNeeded)
                    {
                        pushFiles.setPlaceholder(new Label(Config.getString("team.pullNeeded")));
                    }
                    else
                    {
                        pushFiles.setPlaceholder(new Label(Config.getString("team.nopushfiles")));
                    }
                }
            }
        }

        /**
         * Go through the status list, and figure out which files to commit, and
         * of those which are to be added (i.e. which aren't in the repository)
         * and which are to be removed.
         *
         * @param info The list of files with status (List of TeamStatusInfo)
         * @param filesToCommit The set to store the files to commit in
         * @param filesToAdd The set to store the files to be added in
         * @param filesToRemove The set to store the files to be removed in
         * @param modifiedLayoutFiles The set to store the team status information for the files
         */
        private void getCommitFileSets(List<TeamStatusInfo> info, Set<File> filesToCommit,
                Set<File> filesToAdd, Set<File> filesToRemove,
                Set<TeamStatusInfo> modifiedLayoutFiles)
        {
            for (TeamStatusInfo statusInfo : info)
            {
                File file = statusInfo.getFile();
                boolean isPkgFile = BlueJPackageFile.isPackageFileName(file.getName());
                Status status = statusInfo.getStatus(true);
                
                if (status == Status.NEEDS_ADD || status == Status.CONFLICT_ADD)
                {
                    filesToAdd.add(file);
                    filesToCommit.add(file);
                    statusInfo.setStatus(Status.NEEDS_ADD);
                }
                else if (status == Status.DELETED)
                {
                    filesToRemove.add(file);
                    filesToCommit.add(file);
                }
                else if (status == Status.CONFLICT_LDRM)
                {
                    filesToCommit.add(file);
                }
                else if (status == Status.CONFLICT_LMRD)
                {
                    if (file.exists())
                    {
                        statusInfo.setStatus(Status.NEEDS_MERGE);
                        filesToCommit.add(file);
                    }
                    else
                    {
                        statusInfo.setStatus(Status.DELETED);
                        filesToRemove.add(file);
                        filesToCommit.add(file);
                    }
                }
                else if (status == Status.HAS_CONFLICTS)
                {
                    // don't allow commit? Is this possible?
                }
                else if (status == Status.NEEDS_MERGE)
                {
                    filesToAdd.add(file);
                    filesToCommit.add(file);
                }
                else if (status == Status.NEEDS_COMMIT)
                {
                    if (isPkgFile)
                    {
                        modifiedLayoutFiles.add(statusInfo);
                    }
                    else
                    {
                        filesToCommit.add(file);
                    }
                }
                else if (status != Status.UP_TO_DATE)
                {
                    Debug.message("Commit and push: unhandled file status: " + status + " (for " + file + ")");
                }
            }

            includeLayout.setDisable(changedLayoutFiles.isEmpty());
        }
        
        /**
         * Go through the status list, and figure out which files to commit, and
         * of those which are to be added (i.e. which aren't in the repository)
         * and which are to be removed.
         *
         * @param info The list of files with status (List of TeamStatusInfo)
         * @param filesToPush The set to store the files that will be pushed
         */
        private void getPushFileSets(List<TeamStatusInfo> info, Set<File> filesToPush)
        {
            for (TeamStatusInfo statusInfo : info)
            {
                File file = statusInfo.getFile();
                Status status = statusInfo.getStatus(false);

                if (status != Status.UP_TO_DATE && status != Status.NEEDS_CHECKOUT
                        && status != Status.NEEDS_UPDATE)
                {
                    filesToPush.add(file);
                }
            }
        }

        /**
         * Update the list model with a file list.
         * @param fileSet
         * @param info
         */
        private void updateListModel(ObservableList<TeamStatusInfo> listModel, Set<File> fileSet,
                List<TeamStatusInfo> info)
        {
            listModel.addAll(fileSet.stream().map(file -> getTeamStatusInfoFromFile(file, info))
                    .filter(Objects::nonNull)
                    .filter(statusInfo -> !listModel.contains(statusInfo))
                    .collect(Collectors.toList()));
        }

        /**
         * Returns the status info for a specific file from an info list.
         *
         * @param file     The file which its status info is needed.
         * @param infoList The list which contains files info.
         * @return         The team status info for the file, or null if the list doesn't
         *                 include information about it.
         */
        private TeamStatusInfo getTeamStatusInfoFromFile(File file, List<TeamStatusInfo> infoList)
        {
            Optional<TeamStatusInfo> statusInfo = infoList.stream().filter(
                    info -> info.getFile().equals(file)
                ).findFirst();
            return statusInfo.isPresent() ? statusInfo.get() : null;
        }
    }
}