/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2017  Michael Kolling and John Rosenberg
 
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
import bluej.groupwork.CommitFilter;
import bluej.groupwork.Repository;
import bluej.groupwork.StatusHandle;
import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamStatusInfo.Status;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.pkgmgr.BlueJPackageFile;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.utility.DialogManager;
import bluej.utility.FXWorker;
import bluej.utility.javafx.FXCustomizedDialog;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.Utility;

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
import java.util.Objects;
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
public class CommitAndPushFrame extends FXCustomizedDialog<Void> implements CommitAndPushInterface
{
    private final Project project;
    private Repository repository;

    private Set<TeamStatusInfo> changedLayoutFiles = new HashSet<>();
    private ObservableList commitListModel = FXCollections.observableArrayList();
    private ObservableList pushListModel = FXCollections.observableArrayList();

    private CheckBox includeLayout;
    private TextArea commitText;
    private ActivityIndicator progressBar;

    private CommitAction commitAction;
    private PushAction pushAction;
    private CommitAndPushWorker commitAndPushWorker;
    private boolean emptyCommitText = true;

    //sometimes, usually after a conflict resolution, we need to push in order
    //to update HEAD.
    private boolean pushWithNoChanges = false;

    private static final String noFilesToCommit = Config.getString("team.nocommitfiles");
    private static final String noFilesToPush = Config.getString("team.nopushfiles");
    private static final String pushNeeded = Config.getString("team.pushNeeded");

    public CommitAndPushFrame(Project proj, Window owner)
    {
        super(owner, "team.commit.dcvs.title", "team-commit-push");
        project = proj;
        repository = project.getTeamSettingsController().getRepository(false);
        buildUI();
        prepareButtonPane();
//        DialogManager.centreDialog(this);
        rememberPosition("bluej.commitdisplay");
        //setOnShown(e -> org.scenicview.ScenicView.show(this.asWindow().getScene()));
    }

    /**
     * Create the user-interface for the error display dialog.
     */
    private void buildUI()
    {
        VBox mainPane = new VBox();
        JavaFXUtil.addStyleClass(mainPane, "main-pane");
        Label commitFilesLabel = new Label(Config.getString("team.commitPush.commit.files"));
        ListView commitFiles = new ListView(commitListModel);
//            commitFiles.setCellRenderer(new FileRenderer(project, false));
        commitFiles.setDisable(true);

        ScrollPane commitFileScrollPane = new ScrollPane(commitFiles);
        commitFileScrollPane.setFitToWidth(true);
        commitFileScrollPane.setFitToHeight(true);
        commitFiles.setBackground(mainPane.getBackground());//
        VBox.setMargin(commitFileScrollPane, new Insets(0, 0, 20, 0));

        Label commentLabel = new Label(Config.getString("team.commit.comment"));
        commitText = new TextArea();
        commitText.setPrefRowCount(20);
        commitText.setPrefColumnCount(35);
        VBox.setMargin(commitText, new Insets(0, 0, 10, 0));


        commitAction = new CommitAction(this);
        Button commitButton = new Button();
        commitAction.useButton(PkgMgrFrame.getMostRecent(), commitButton);
        //Bind commitText properties to enable the commit button if there is a comment.
        commitButton.disableProperty().bind(Bindings.or(commitText.disabledProperty(), commitText.textProperty().isEmpty()));

        includeLayout = new CheckBox(Config.getString("team.commit.includelayout"));
        includeLayout.setDisable(true);
        includeLayout.setOnAction(event -> {
            CheckBox layoutCheck = (CheckBox) event.getSource();
            if (layoutCheck.isSelected()) {
                addModifiedLayouts();
                if (commitButton.isDisable()) {
                    commitAction.setEnabled(!emptyCommitText);
                }
            } // unselected
            else {
                removeModifiedLayouts();
                if (isCommitListEmpty()) {
                    commitAction.setEnabled(false);
                }
            }
        });

        HBox commitButtonPane = new HBox();
        JavaFXUtil.addStyleClass(commitButtonPane, "button-hbox");
        commitButtonPane.setAlignment(Pos.CENTER_RIGHT);
        commitButtonPane.getChildren().addAll(includeLayout, commitButton);

        pushAction = new PushAction(this);
        Button pushButton = new Button();
        pushAction.useButton(PkgMgrFrame.getMostRecent(), pushButton);

        Label pushFilesLabel = new Label(Config.getString("team.commitPush.push.files"));
        ListView pushFiles = new ListView(pushListModel);
//            pushFiles.setCellRenderer(new FileRenderer(project, true));
        pushFiles.setDisable(true);
        ScrollPane pushFileScrollPane = new ScrollPane(pushFiles);
        pushFileScrollPane.setFitToWidth(true);
        pushFileScrollPane.setFitToHeight(true);

        HBox pushButtonPane = new HBox();
        progressBar = new ActivityIndicator();
        progressBar.setRunning(false);
        JavaFXUtil.addStyleClass(pushButtonPane, "button-hbox");
        pushButtonPane.setAlignment(Pos.CENTER_RIGHT);
        pushButtonPane.getChildren().addAll(progressBar, pushButton);

        mainPane.getChildren().addAll(commitFilesLabel, commitFileScrollPane,
                commentLabel, commitText,
                commitButtonPane,
                new Separator(Orientation.HORIZONTAL),
                pushFilesLabel, pushFileScrollPane,
                pushButtonPane);
        getDialogPane().setContent(mainPane);
    }

    /**
     * Create the button panel with a close button
     * @return Pane the buttonPanel
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

    @Override
    public void setVisible(boolean show)
    {
        if (show) {
            // we want to set comments and commit action to disabled
            // until we know there is something to commit
            commitAction.setEnabled(false);//
            commitText.setDisable(true);//
            commitText.setText("");//
            includeLayout.setSelected(false);
            includeLayout.setDisable(true);//
            changedLayoutFiles.clear();
            commitListModel.clear();
            pushAction.setEnabled(false);
            pushListModel.clear();

            repository = project.getTeamSettingsController().getRepository(false);

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
                show(); // looking at the else before, this line is maybe wrong
            }
            else {
                hide();
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

    @Override
    public void reset()
    {
        commitListModel.clear();
        pushListModel.clear();
        setComment("");
    }

    private void removeModifiedLayouts()
    {
        // remove modified layouts from list of files shown for commit
        commitListModel.removeAll(changedLayoutFiles);
        if (commitListModel.isEmpty()) {
            commitListModel.add(noFilesToCommit);
            commitText.setDisable(true);
        }
    }

    private boolean isCommitListEmpty()
    {
        return commitListModel.isEmpty() || commitListModel.contains(noFilesToCommit);
    }

    @Override
    public String getComment()
    {
        return commitText.getText();
    }

    /**
     * Get a list of the layout files to be committed
     *
     * @return
     */
    @Override
    public Set<File> getChangedLayoutFiles()
    {
        return changedLayoutFiles.stream().map(info -> info.getFile()).collect(Collectors.toSet());
    }

    /**
     * Remove a file from the list of changes layout files.
     */
    private void removeChangedLayoutFile(File file)
    {
        changedLayoutFiles.removeIf(info -> info.getFile().equals(file));
    }

    /**
     * Get a set of the layout files which have changed (with status info).
     *
     * @return the set of the layout files which have changed.
     */
    @Override
    public Set<TeamStatusInfo> getChangedLayoutInfo()
    {
        return changedLayoutFiles;
    }

    @Override
    public boolean includeLayout()
    {
        return includeLayout != null && includeLayout.isSelected();
    }

    private void addModifiedLayouts()
    {
        if (commitListModel.contains(noFilesToCommit)) {
            commitListModel.remove(noFilesToCommit);
            commitText.setDisable(false);
        }
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
    @Override
    public void startProgress()
    {
        progressBar.setRunning(true);
    }

    /**
     * Stop the activity indicator. Call from any thread.
     */
    @Override
    public void stopProgress()
    {
        progressBar.setRunning(false);
    }

    @Override
    public Project getProject()
    {
        return project;
    }

    private void setLayoutChanged(boolean hasChanged)
    {
        includeLayout.setDisable(!hasChanged);
    }

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
            FileFilter filter = project.getTeamSettingsController().getFileFilter(true);
            command = repository.getStatus(this, filter, false);
        }

        public boolean isPushAvailable()
        {
            return this.isPushAvailable;
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
            pushWithNoChanges = statusHandle.pushNeeded();
            pushAction.setStatusHandle(statusHandle);
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
//                    CommitAndPushFrame.this.dialogThenHide(() -> TeamUtils.handleServerResponseFX(result, CommitAndPushFrame.this.asWindow()));
                    TeamUtils.handleServerResponseFX(result, CommitAndPushFrame.this.asWindow());
                    CommitAndPushFrame.this.hide();
                } else if (response != null) {
                    //populate files to commit.
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
                            needsMerge, modifiedLayoutFiles, false);

                    if (!mergeConflicts.isEmpty() || !deleteConflicts.isEmpty()
                            || !otherConflicts.isEmpty() || !needsMerge.isEmpty()) {

                        handleConflicts(mergeConflicts, deleteConflicts,
                                otherConflicts, needsMerge);
                        return;
                    }

                    commitAction.setFiles(filesToCommit);
                    commitAction.setNewFiles(filesToAdd);
                    commitAction.setDeletedFiles(filesToDelete);
                    //update commitListModel
                    updateListModel(commitListModel, filesToCommit, info);
                    updateListModel(commitListModel, filesToAdd, info);
                    updateListModel(commitListModel, filesToDelete, info);

                    //populate files ready to push.
                    Set<File> filesToCommitInPush = new HashSet<>();
                    Set<File> filesToAddInPush = new HashSet<>();
                    Set<File> filesToDeleteInPush = new HashSet<>();
                    Set<File> mergeConflictsInPush = new HashSet<>();
                    Set<File> deleteConflictsInPush = new HashSet<>();
                    Set<File> otherConflictsInPush = new HashSet<>();
                    Set<File> needsMergeInPush = new HashSet<>();
                    Set<File> modifiedLayoutFilesInPush = new HashSet<>();

                    getCommitFileSets(info, filesToCommitInPush, filesToAddInPush, filesToDeleteInPush,
                            mergeConflictsInPush, deleteConflictsInPush, otherConflictsInPush,
                            needsMergeInPush, modifiedLayoutFilesInPush, true);

                    //update commitListModel
                    updateListModel(pushListModel, filesToCommitInPush, info);
                    updateListModel(pushListModel, filesToAddInPush, info);
                    updateListModel(pushListModel, filesToDeleteInPush, info);
                    updateListModel(pushListModel, modifiedLayoutFilesInPush, info);

                    this.isPushAvailable = pushWithNoChanges || !filesToCommitInPush.isEmpty() || !filesToAddInPush.isEmpty()
                            || !filesToDeleteInPush.isEmpty() || !modifiedLayoutFilesInPush.isEmpty();

                    pushAction.setEnabled(this.isPushAvailable);

                    //in the case we are commiting the resolution of a merge, we should check if the same file that is beingmarked as otherConflict 
                    //on the remote branch is being commitd to the local branch. if it is, then this is the user resolution to the conflict and we should 
                    //procceed with the commit. and then with the push as normal.
                    boolean conflicts;
                    conflicts = !mergeConflictsInPush.isEmpty() || !deleteConflictsInPush.isEmpty()
                            || !otherConflictsInPush.isEmpty() || !needsMergeInPush.isEmpty();
                    if (commitAction.isDisabled() && conflicts) {
                        //there is a file in some of the conflict list.
                        //check if this fill will commit normally. if it will, we should allow.
                        Set<File> conflictingFilesInPush = new HashSet<>();
                        conflictingFilesInPush.addAll(mergeConflictsInPush);
                        conflictingFilesInPush.addAll(deleteConflictsInPush);
                        conflictingFilesInPush.addAll(otherConflictsInPush);
                        conflictingFilesInPush.addAll(needsMergeInPush);

                        for (File conflictEntry : conflictingFilesInPush) {
                            if (filesToCommit.contains(conflictEntry)) {
                                conflictingFilesInPush.remove(conflictEntry);
                                mergeConflictsInPush.remove(conflictEntry);
                                deleteConflictsInPush.remove(conflictEntry);
                                otherConflictsInPush.remove(conflictEntry);
                                needsMergeInPush.remove(conflictEntry);

                            }
                            if (filesToAdd.contains(conflictEntry)) {
                                conflictingFilesInPush.remove(conflictEntry);
                                mergeConflictsInPush.remove(conflictEntry);
                                deleteConflictsInPush.remove(conflictEntry);
                                otherConflictsInPush.remove(conflictEntry);
                                needsMergeInPush.remove(conflictEntry);
                            }
                            if (filesToDelete.contains(conflictEntry)) {
                                conflictingFilesInPush.remove(conflictEntry);
                                mergeConflictsInPush.remove(conflictEntry);
                                deleteConflictsInPush.remove(conflictEntry);
                                otherConflictsInPush.remove(conflictEntry);
                                needsMergeInPush.remove(conflictEntry);
                            }
                        }
                        conflicts = !conflictingFilesInPush.isEmpty();
                    }

                    if (commitAction.isDisabled() && conflicts) {

                        handleConflicts(mergeConflictsInPush, deleteConflictsInPush,
                                otherConflictsInPush, null);
                        return;
                    }
                }

                if (commitListModel.isEmpty()) {
                    commitListModel.add(noFilesToCommit);
                } else {
                    commitText.setDisable(false);
                    commitText.requestFocus();
                    commitAction.setEnabled(!emptyCommitText);
                }

                if (pushListModel.isEmpty()){
                    if (isPushAvailable){
                        pushListModel.add(pushNeeded);
                    } else {
                        pushListModel.add(noFilesToPush);
                    }
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
                // CommitAndPushFrame.this.dialogThenHide(() -> DialogManager.showMessageFX(CommitAndPushFrame.this.asWindow(), "team-uptodate-failed"));
                DialogManager.showMessageFX(CommitAndPushFrame.this.asWindow(), "team-uptodate-failed");
                CommitAndPushFrame.this.hide();
                return;
            }

            stopProgress();
            // CommitAndPushFrame.this.dialogThenHide(() -> DialogManager.showMessageWithTextFX(CommitAndPushFrame.this.asWindow(), dlgLabel, filesList));
            DialogManager.showMessageWithTextFX(CommitAndPushFrame.this.asWindow(), dlgLabel, filesList);
            CommitAndPushFrame.this.hide();
        }

        /**
         * Build a list of files, max out at 10 files.
         *
         * @param conflicts
         * @return
         */
        private String buildConflictsList(Set<File> conflicts)
        {
            String filesList = "";
            Iterator<File> i = conflicts.iterator();
            for (int j = 0; j < 10 && i.hasNext(); j++) {
                File conflictFile = (File) i.next();
                filesList += "    " + conflictFile.getName() + "\n";
            }

            if (i.hasNext()) {
                filesList += "    " + Config.getString("team.commit.moreFiles");
            }

            return filesList;
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
         * @param mergeConflicts The set to store files with merge conflicts in.
         * @param deleteConflicts The set to store files with conflicts in,
         * which need to be resolved by first deleting the local file
         * @param otherConflicts The set to store files with "locally deleted"
         * conflicts (locally deleted, remotely modified).
         * @param needsMerge The set of files which are updated locally as well
         * as in the repository (required merging).
        //         * @param conflicts The set to store unresolved conflicts in
         *
         * @param remote false if this is a non-distributed repository.
         */
        private void getCommitFileSets(List<TeamStatusInfo> info, Set<File> filesToCommit, Set<File> filesToAdd,
                                       Set<File> filesToRemove, Set<File> mergeConflicts, Set<File> deleteConflicts,
                                       Set<File> otherConflicts, Set<File> needsMerge, Set<File> modifiedLayoutFiles, boolean remote)
        {

            CommitFilter filter = new CommitFilter();
            Map<File, File> modifiedLayoutDirs = new HashMap<>();

            for (TeamStatusInfo statusInfo : info) {
                File file = statusInfo.getFile();
                boolean isPkgFile = BlueJPackageFile.isPackageFileName(file.getName());
                Status status;
                //select status to use.
                if (remote) {
                    status = statusInfo.getRemoteStatus();
                } else {
                    status = statusInfo.getStatus();
                }

                if (filter.accept(statusInfo, !remote)) {
                    if (!isPkgFile) {
                        filesToCommit.add(file);
                    } else if (status == Status.NEEDSADD
                            || status == Status.DELETED
                            || status == Status.CONFLICT_LDRM) {
                        // Package file which must be committed.
                        if (filesToCommit.add(statusInfo.getFile().getParentFile())) {
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
                        if (!filesToCommit.contains(parentFile)) {
                            modifiedLayoutFiles.add(file);
                            modifiedLayoutDirs.put(parentFile, file);
                            // keep track of StatusInfo objects representing changed diagrams
                            changedLayoutFiles.add(statusInfo);
                        } else {
                            // We must commit the file unconditionally
                            filesToCommit.add(file);
                        }
                    }

                    if (status == Status.NEEDSADD) {
                        filesToAdd.add(statusInfo.getFile());
                    } else if (status == Status.DELETED
                            || status == Status.CONFLICT_LDRM) {
                        filesToRemove.add(statusInfo.getFile());
                    }
                } else if (!isPkgFile) {
                    if (status == Status.HASCONFLICTS) {
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
                    if (status == Status.NEEDSMERGE) {
                        needsMerge.add(statusInfo.getFile());
                    }
                }
            }

            if (!remote) {
                setLayoutChanged(!changedLayoutFiles.isEmpty());
            }
        }

        /**
         * Update the list model with a file list.
         * @param fileSet
         * @param info
         */
        private void updateListModel(ObservableList<TeamStatusInfo> listModel, Set<File> fileSet, List<TeamStatusInfo> info)
        {
            listModel.addAll(fileSet.stream().map(file -> getTeamStatusInfoFromFile(file, info))
                    .filter(Objects::nonNull)
                    .filter(statusInfo -> !listModel.contains(statusInfo))
                    .collect(Collectors.toList()));
        }

        private TeamStatusInfo getTeamStatusInfoFromFile(File file, List<TeamStatusInfo> infoList)
        {
            if (file != null && !infoList.isEmpty()){
               return infoList.stream().filter(info -> info.getFile().equals(file)).findFirst().get();
            }
            return null;
        }
    }
}