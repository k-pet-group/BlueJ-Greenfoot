/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2010,2014,2016,2017  Michael Kolling and John Rosenberg

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

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import bluej.Config;
import bluej.groupwork.CommitFilter;
import bluej.groupwork.Repository;
import bluej.groupwork.StatusHandle;
import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.actions.CommitAction;
import bluej.pkgmgr.BlueJPackageFile;
import bluej.pkgmgr.Project;
import bluej.utility.DialogManager;
import bluej.utility.FXWorker;
import bluej.utility.javafx.FXCustomizedDialog;
import bluej.utility.Utility;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A Swing based user interface to add commit comments.
 * @author Bruce Quig
 * @author Amjad Altadmri
 */
public class CommitCommentsFrame extends FXCustomizedDialog<Void> implements CommitAndPushInterface
{
    private Project project;
    private Repository repository;

    private CommitAction commitAction;
    private CommitWorker commitWorker;

    private ObservableList commitListModel = FXCollections.emptyObservableList();
    private Set<TeamStatusInfo> changedLayoutFiles = new HashSet<>();
    /** The packages whose layout should be committed compulsorily */
    private Set<File> packagesToCommmit = new HashSet<>();

    private static String noFilesToCommit = Config.getString("team.nocommitfiles");

    private TextArea commitText;
    private Button commitButton;
    private CheckBox includeLayout;
    private ActivityIndicatorFX progressBar;

    public CommitCommentsFrame(Project proj, Window owner)
    {
        super(owner, "team.commit.title", "team-commit-comments");
        project = proj;
        buildUI();
//        DialogManager.centreDialog(this);
    }

    /**
     * Create the user-interface for the error display dialog.
     */
    protected void buildUI()
    {
        //setIconImage(BlueJTheme.getIconImage());
        rememberPosition("bluej.commitdisplay");

        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.5);

        VBox topPanel = new VBox();

        ScrollPane commitFileScrollPane = new ScrollPane();

        Label commitFilesLabel = new Label(Config.getString("team.commit.files"));
        topPanel.getChildren().add(commitFilesLabel);

        ListView commitFiles = new ListView(commitListModel);
        commitFiles.setCellFactory(param -> new TeamStatusInfoCell(project));
        commitFiles.setDisable(true);
        commitFileScrollPane.setContent(commitFiles);

        topPanel.getChildren().add(commitFileScrollPane);

        splitPane.getItems().add(topPanel);

        VBox bottomPanel = new VBox();
        Label commentLabel = new Label(Config.getString("team.commit.comment"));
        bottomPanel.getChildren().add(commentLabel);

        commitText = new TextArea("");
        commitText.setPrefRowCount(6);
        commitText.setPrefColumnCount(42);

        commitText.setMinSize(commitText.getMinWidth(), commitText.getPrefHeight());

        ScrollPane commitTextScrollPane = new ScrollPane(commitText);
        commitTextScrollPane.setMinSize(commitText.getMinWidth(), commitText.getPrefHeight());
        bottomPanel.getChildren().add(commitTextScrollPane);

        commitAction = new CommitAction(this);
        commitButton = new Button();// BlueJTheme.getOkButton();
        commitButton.setOnAction(event -> commitAction.actionPerformed(null));
        commitButton.requestFocus();

        Button closeButton = new Button();// BlueJTheme.getCancelButton();
        closeButton.setOnAction(event -> {
            commitWorker.abort();
            commitAction.cancel();
            setVisible(false);
        });

        HBox buttonPanel = new HBox();

        progressBar = new ActivityIndicatorFX();
        progressBar.setRunning(false);

        VBox checkBoxPanel = new VBox();
        includeLayout = new CheckBox(Config.getString("team.commit.includelayout"));
        includeLayout.setDisable(true);
        includeLayout.setOnAction(event -> {
            CheckBox layoutCheck = (CheckBox)event.getSource();
            if(layoutCheck.isSelected()) {
                addModifiedLayouts();
                if(commitButton.isDisabled())
                    commitAction.setEnabled(true);
            }
            // unselected
            else {
                removeModifiedLayouts();
                if (isCommitListEmpty())
                    commitAction.setEnabled(false);
            }
        });

        checkBoxPanel.getChildren().addAll(includeLayout, buttonPanel);
        buttonPanel.getChildren().addAll(progressBar, commitButton, closeButton);
        bottomPanel.getChildren().add(checkBoxPanel);

        splitPane.getItems().add(bottomPanel);
        getDialogPane().getChildren().add(splitPane);
    }

    public void setVisible(boolean visible)
    {
        if (visible) {
            show();
            // we want to set comments and commit action to disabled
            // until we know there is something to commit
            commitAction.setEnabled(false);
            commitText.setDisable(true);
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
                        Platform.runLater(() -> DialogManager.showErrorTextFX(asWindow(), msgFinal));
                    }
                }
                startProgress();
                commitWorker = new CommitWorker();
                commitWorker.start();
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
        if (commitListModel.isEmpty()) {
            commitListModel.add(noFilesToCommit);
            commitText.setDisable(true);
        }
    }

    private boolean isCommitListEmpty()
    {
        return commitListModel.isEmpty() || commitListModel.contains(noFilesToCommit);
    }

    private void addModifiedLayouts()
    {
        if(commitListModel.contains(noFilesToCommit)) {
            commitListModel.remove(noFilesToCommit);
            commitText.setDisable(false);
        }
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
            FileFilter filter = project.getTeamSettingsController().getFileFilter(true);
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
        @OnThread(Tag.Any)
        @Override
        public void statusComplete(StatusHandle statusHandle)
        {
            commitAction.setStatusHandle(statusHandle);
        }

        @OnThread(Tag.Unique)
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

                if (commitListModel.isEmpty()) {
                    commitListModel.add(noFilesToCommit);
                } else {
                    commitText.setDisable(false);
                    commitText.requestFocus();
                    commitAction.setEnabled(true);
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
                int status = statusInfo.getStatus();
                if (filter.accept(statusInfo, true)) {
                    if (!isPkgFile) {
                        commitListModel.add(statusInfo);
                        filesToCommit.add(file);
                    } else if (status == TeamStatusInfo.STATUS_NEEDSADD
                            || status == TeamStatusInfo.STATUS_DELETED
                            || status == TeamStatusInfo.STATUS_CONFLICT_LDRM) {
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

                    if (status == TeamStatusInfo.STATUS_NEEDSADD) {
                        filesToAdd.add(statusInfo.getFile());
                    } else if (status == TeamStatusInfo.STATUS_DELETED
                            || status == TeamStatusInfo.STATUS_CONFLICT_LDRM) {
                        filesToRemove.add(statusInfo.getFile());
                    }
                } else if (!isPkgFile) {
                    if (status == TeamStatusInfo.STATUS_HASCONFLICTS) {
                        mergeConflicts.add(statusInfo.getFile());
                    }
                    if (status == TeamStatusInfo.STATUS_UNRESOLVED
                            || status == TeamStatusInfo.STATUS_CONFLICT_ADD
                            || status == TeamStatusInfo.STATUS_CONFLICT_LMRD) {
                        deleteConflicts.add(statusInfo.getFile());
                    }
                    if (status == TeamStatusInfo.STATUS_CONFLICT_LDRM) {
                        otherConflicts.add(statusInfo.getFile());
                    }
                    if (status == TeamStatusInfo.STATUS_NEEDSMERGE) {
                        needsMerge.add(statusInfo.getFile());
                    }
                }
            }

            setLayoutChanged(!changedLayoutFiles.isEmpty());
        }
    }
}
