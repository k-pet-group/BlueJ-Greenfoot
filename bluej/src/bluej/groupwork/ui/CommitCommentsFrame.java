/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2010,2014,2016,2017,2018  Michael Kolling and John Rosenberg

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import bluej.Config;
import bluej.groupwork.CommitFilter;
import bluej.groupwork.Repository;
import bluej.groupwork.StatusHandle;
import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamStatusInfo.Status;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.actions.CommitAction;
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
 * A user interface to add commit comments.
 * @author Bruce Quig
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class CommitCommentsFrame extends FXCustomizedDialog<Void> implements CommitAndPushInterface
{
    private Project project;
    private Repository repository;
    private CommitAction commitAction;
    private CommitWorker commitWorker;

    private ObservableList<TeamStatusInfo> commitListModel = FXCollections.observableArrayList();
    private Set<TeamStatusInfo> changedLayoutFiles = new HashSet<>();
    /** The packages whose layout should be committed compulsorily */
    private Set<File> packagesToCommmit = new HashSet<>();

    private final TextArea commitText = new TextArea("");
    private final CheckBox includeLayout = new CheckBox(Config.getString("team.commit.includelayout"));
    private final ActivityIndicator progressBar = new ActivityIndicator();

    public CommitCommentsFrame(Project project)
    {
        super(null, "team.commit.title", "team-commit-comments");
        this.project = project;
        getDialogPane().setContent(makeMainPane());
        prepareButtonPane();
    }

    /**
     *
     */
    private Pane makeMainPane()
    {
        ListView<TeamStatusInfo> commitFiles = new ListView<>(commitListModel);
        commitFiles.setPlaceholder(new Label(Config.getString("team.nocommitfiles")));
        commitFiles.setCellFactory(param -> new TeamStatusInfoCell(project));
        commitFiles.setDisable(true);

        ScrollPane fileScrollPane = new ScrollPane(commitFiles);
        fileScrollPane.setFitToWidth(true);
        fileScrollPane.setFitToHeight(true);
        VBox.setMargin(fileScrollPane, new Insets(0, 0, 20, 0));

        commitText.setPrefRowCount(20);
        commitText.setPrefColumnCount(35);
        VBox.setMargin(commitText, new Insets(0, 0, 10, 0));

        commitAction = new CommitAction(this);
        Button commitButton = new Button();
        commitAction.useButton(project, commitButton);
        commitButton.requestFocus();
        //Bind commitText properties to enable the commit button if there is a comment.
        commitText.disableProperty().bind(Bindings.isEmpty(commitListModel));
        commitAction.disabledProperty().bind(Bindings.or(commitText.disabledProperty(), commitText.textProperty().isEmpty()));

        progressBar.setRunning(false);

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
        commitButtonPane.getChildren().addAll(includeLayout, commitButton, progressBar);

        VBox mainPane = new VBox();
        JavaFXUtil.addStyleClass(mainPane, "main-pane");
        mainPane.getChildren().addAll(new Label(Config.getString("team.commit.files")), fileScrollPane,
                                      new Label(Config.getString("team.commit.comment")), commitText,
                                      commitButtonPane);
        VBox.setVgrow(fileScrollPane, Priority.ALWAYS);
        VBox.setVgrow(commitText, Priority.ALWAYS);

        return mainPane;
    }

    /**
     * Prepare the button panel with a Resolve button and a close button
     */
    private void prepareButtonPane()
    {
        //close button
        getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
        this.setOnCloseRequest(event -> {
            if (commitWorker != null) {
                commitWorker.abort();
                commitAction.cancel();
            }
        });
    }

    public void setVisible(boolean visible)
    {
        if (visible) {
            // we want to set comments and commit action to disabled
            // until we know there is something to commit
            includeLayout.setSelected(false);
            includeLayout.setDisable(true);
            changedLayoutFiles.clear();
            commitListModel.clear();

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
                        DialogManager.showErrorTextFX(asWindow(), msgFinal);
                    }
                }
                startProgress();
                commitWorker = new CommitWorker();
                commitWorker.start();
                if (!isShowing()) {
                    show();
                }
            }
            else {
                hide();
            }
        }
        else {
            hide();
        }
    }

    public String getComment()
    {
        return commitText.getText();
    }

    public void setComment(String newComment)
    {
        commitText.setText(newComment);
    }

    public void reset()
    {
        commitListModel.clear();
        setComment("");
    }

    private void removeModifiedLayouts()
    {
        // remove modified layouts from list of files shown for commit
        for (TeamStatusInfo info : changedLayoutFiles) {
            if (!packagesToCommmit.contains(info.getFile().getParentFile())) {
                commitListModel.remove(info);
            }
        }
    }

    private void addModifiedLayouts()
    {
        // add diagram layout files to list of files to be committed
        Set<File> displayedLayouts = new HashSet<>();
        for (TeamStatusInfo info : changedLayoutFiles) {
            File parentFile = info.getFile().getParentFile();
            if (!displayedLayouts.contains(parentFile)
                    && !packagesToCommmit.contains(parentFile)) {
                commitListModel.add(info);
                displayedLayouts.add(info.getFile().getParentFile());
            }
        }
    }

    /**
     * Get a list of the layout files to be committed
     */
    public Set<File> getChangedLayoutFiles()
    {
        return changedLayoutFiles.stream().map(TeamStatusInfo::getFile).collect(Collectors.toSet());
    }

    /**
     * Remove a file from the list of changes layout files.
     */
    private void removeChangedLayoutFile(File file)
    {
        for(Iterator<TeamStatusInfo> it = changedLayoutFiles.iterator(); it.hasNext(); ) {
            TeamStatusInfo info = it.next();
            if (info.getFile().equals(file)) {
                it.remove();
                return;
            }
        }
    }

    /**
     * Get a set of the layout files which have changed (with status info).
     */
    public Set<TeamStatusInfo> getChangedLayoutInfo()
    {
        return changedLayoutFiles;
    }

    public boolean includeLayout()
    {
        return includeLayout != null && includeLayout.isSelected();
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

    public Project getProject()
    {
        return project;
    }

    private void setLayoutChanged(boolean hasChanged)
    {
        includeLayout.setDisable(!hasChanged);
    }

    /**
     * Inner class to do the actual cvs status check to populate commit dialog
     * to ensure that the UI is not blocked during remote call
     */
    class CommitWorker extends FXWorker implements StatusListener
    {
        List<TeamStatusInfo> response;
        TeamworkCommand command;
        TeamworkCommandResult result;
        private boolean aborted;

        public CommitWorker()
        {
            super();
            response = new ArrayList<>();
            FileFilter filter = project.getTeamSettingsController().getFileFilter(true, true);
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
            if (!aborted) {
                if (result.isError()) {
                    CommitCommentsFrame.this.dialogThenHide(() -> TeamUtils.handleServerResponseFX(result, CommitCommentsFrame.this.asWindow()));
                } else if (response != null) {
                    Set<File> filesToCommit = new HashSet<>();
                    Set<File> filesToAdd = new LinkedHashSet<>();
                    Set<File> filesToDelete = new HashSet<>();
                    Set<File> mergeConflicts = new HashSet<>();
                    Set<File> deleteConflicts = new HashSet<>();
                    Set<File> otherConflicts = new HashSet<>();
                    Set<File> needsMerge = new HashSet<>();
                    Set<File> modifiedLayoutFiles = new HashSet<>();

                    List<TeamStatusInfo> info = response;
                    getCommitFileSets(info, filesToCommit, filesToAdd, filesToDelete,
                            mergeConflicts, deleteConflicts, otherConflicts,
                            needsMerge, modifiedLayoutFiles);

                    if (!mergeConflicts.isEmpty() || !deleteConflicts.isEmpty()
                            || !otherConflicts.isEmpty() || !needsMerge.isEmpty()) {

                        handleConflicts(mergeConflicts, deleteConflicts,
                                otherConflicts, needsMerge);
                        return;
                    }

                    commitAction.setFiles(filesToCommit);
                    commitAction.setNewFiles(filesToAdd);
                    commitAction.setDeletedFiles(filesToDelete);
                }

                if (!commitListModel.isEmpty()) {
                    commitText.requestFocus();
                }
            }
        }

        private void handleConflicts(Set<File> mergeConflicts, Set<File> deleteConflicts,
                                     Set<File> otherConflicts, Set<File> needsMerge)
        {
            String dlgLabel;
            String filesList;

            // If there are merge conflicts, handle those first
            if (!mergeConflicts.isEmpty()) {
                dlgLabel = "team-resolve-merge-conflicts";
                filesList = buildConflictsList(mergeConflicts);
            } else if (!deleteConflicts.isEmpty()) {
                dlgLabel = "team-resolve-conflicts-delete";
                filesList = buildConflictsList(deleteConflicts);
            } else if (!otherConflicts.isEmpty()) {
                dlgLabel = "team-update-first";
                filesList = buildConflictsList(otherConflicts);
            } else {
                stopProgress();
                CommitCommentsFrame.this.dialogThenHide(() -> DialogManager.showMessageFX(CommitCommentsFrame.this.asWindow(), "team-uptodate-failed"));
                return;
            }

            stopProgress();
            CommitCommentsFrame.this.dialogThenHide(() -> DialogManager.showMessageWithTextFX(CommitCommentsFrame.this.asWindow(), dlgLabel, filesList));
        }

        /**
         * Buid a list of files, max out at 10 files.
         * @param conflicts
         * @return
         */
        private String buildConflictsList(Set<File> conflicts)
        {
            String filesList = "";
            Iterator<File> i = conflicts.iterator();
            for (int j = 0; j < 10 && i.hasNext(); j++) {
                File conflictFile = i.next();
                filesList += "    " + conflictFile.getName() + "\n";
            }

            if (i.hasNext()) {
                filesList += "    " + Config.getString("team.commit.moreFiles");
            }

            return filesList;
        }

        /**
         * Go through the status list, and figure out which files to commit, and
         * of those which are to be added (i.e. which aren't in the repository) and
         * which are to be removed.
         *
         * @param info                  The list of files with status (List of TeamStatusInfo)
         * @param filesToCommit         The set to store the files to commit in
         * @param filesToAdd            The set to store the files to be added in
         * @param filesToRemove         The set to store the files to be removed in
         * @param mergeConflicts        The set to store files with merge conflicts in.
         * @param deleteConflicts       The set to store files with conflicts in, which
         *                                  need to be resolved by first deleting the local file
         * @param otherConflicts        The set to store files with "locally deleted" conflicts
         *                                  (locally deleted, remotely modified).
         * @param needsMerge            The set of files which are updated locally as
         *                                  well as in the repository (required merging).
         * @param modifiedLayoutFiles
         *
         */
        private void getCommitFileSets(List<TeamStatusInfo> info, Set<File> filesToCommit, Set<File> filesToAdd,
                                       Set<File> filesToRemove, Set<File> mergeConflicts, Set<File> deleteConflicts,
                                       Set<File> otherConflicts, Set<File> needsMerge, Set<File> modifiedLayoutFiles)
        {

            CommitFilter filter = new CommitFilter();
            Map<File,File> modifiedLayoutDirs = new HashMap<>();

            for (TeamStatusInfo statusInfo : info) {
                File file = statusInfo.getFile();
                boolean isPkgFile = BlueJPackageFile.isPackageFileName(file.getName());
                Status status = statusInfo.getStatus();
                if (filter.accept(statusInfo, true)) {
                    if (!isPkgFile) {
                        commitListModel.add(statusInfo);
                        filesToCommit.add(file);
                    } else if (status == Status.NEEDS_ADD
                            || status == Status.DELETED
                            || status == Status.CONFLICT_LDRM) {
                        // Package file which must be committed.
                        if (packagesToCommmit.add(statusInfo.getFile().getParentFile())) {
                            commitListModel.add(statusInfo);
                            File otherPkgFile = modifiedLayoutDirs.remove(file.getParentFile());
                            if (otherPkgFile != null) {
                                removeChangedLayoutFile(otherPkgFile);
                                filesToCommit.add(otherPkgFile);
                            }
                        }
                        filesToCommit.add(statusInfo.getFile());
                    } else {
                        // add file to list of files that may be added to commit
                        File parentFile = file.getParentFile();
                        if (!packagesToCommmit.contains(parentFile)) {
                            modifiedLayoutFiles.add(file);
                            modifiedLayoutDirs.put(parentFile, file);
                            // keep track of StatusInfo objects representing changed diagrams
                            changedLayoutFiles.add(statusInfo);
                        } else {
                            // We must commit the file unconditionally
                            filesToCommit.add(file);
                        }
                    }

                    if (status == Status.NEEDS_ADD) {
                        filesToAdd.add(statusInfo.getFile());
                    } else if (status == Status.DELETED
                            || status == Status.CONFLICT_LDRM) {
                        filesToRemove.add(statusInfo.getFile());
                    }
                } else if (!isPkgFile) {
                    if (status == Status.HAS_CONFLICTS) {
                        mergeConflicts.add(statusInfo.getFile());
                    }
                    if (status == Status.UNRESOLVED
                            || status == Status.CONFLICT_ADD
                            || status == Status.CONFLICT_LMRD) {
                        deleteConflicts.add(statusInfo.getFile());
                    }
                    if (status == Status.CONFLICT_LDRM) {
                        otherConflicts.add(statusInfo.getFile());
                    }
                    if (status == Status.NEEDS_MERGE) {
                        needsMerge.add(statusInfo.getFile());
                    }
                }
            }

            setLayoutChanged(!changedLayoutFiles.isEmpty());
        }
    }
}
