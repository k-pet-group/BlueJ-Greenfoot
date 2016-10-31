/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016  Michael Kolling and John Rosenberg 
 
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

import bluej.BlueJTheme;
import bluej.Config;
import bluej.extensions.event.PackageListener;
import bluej.groupwork.CommitFilter;
import bluej.groupwork.Repository;
import bluej.groupwork.StatusHandle;
import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.actions.CommitAction;
import bluej.groupwork.actions.PushAction;
import bluej.pkgmgr.BlueJPackageFile;
import bluej.pkgmgr.Project;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;
import bluej.utility.DialogManager;
import bluej.utility.SwingWorker;
import bluej.utility.Utility;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import javafx.application.Platform;

import bluej.utility.javafx.SwingNodeDialog;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A Swing based user interface to commit and push. Used by DCVS systems, like
 * Git.
 *
 * @author Fabio Heday
 */
public class CommitAndPushFrame extends SwingNodeDialog implements CommitAndPushInterface
{

    private final Project project;

    private Set<TeamStatusInfo> changedLayoutFiles;
    private Repository repository;

   
    private JPanel topPanel, middlePanel, bottomPanel;
    private ActivityIndicator progressBar;
    private JCheckBox includeLayout;
    private JTextArea commitText;
    private JList commitFiles, pushFiles;
    private DefaultListModel commitListModel, pushListModel;
    private JButton commitButton, pushButton;

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

    public CommitAndPushFrame(Project proj)
    {
        project = proj;
        changedLayoutFiles = new HashSet<>();
        repository = project.getTeamSettingsController().getRepository(false);

        createUI();
        DialogManager.centreDialog(this);
    }

    /**
     * Create the user-interface for the error display dialog.
     */
    protected void createUI()
    {

        setTitle(Config.getString("team.commit.dcvs.title"));
        commitListModel = new DefaultListModel();
        pushListModel = new DefaultListModel();

        rememberPosition("bluej.commitdisplay");
        
        topPanel = new JPanel();
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        JPanel windowPanel = new JPanel();
        windowPanel.setBorder(BlueJTheme.generalBorder);

        JScrollPane commitFileScrollPane = new JScrollPane();
        
        {
            topPanel.setLayout(new BorderLayout());
            windowPanel.setLayout(new BorderLayout());
            topPanel.setBorder(BorderFactory.createEmptyBorder());

            JLabel commitFilesLabel = new JLabel(Config.getString(
                    "team.commitPush.commit.files"));
            commitFilesLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
            topPanel.add(commitFilesLabel, BorderLayout.NORTH);
            commitFiles = new JList(commitListModel);
            commitFiles.setVisibleRowCount(4);
            commitFiles.setCellRenderer(new FileRenderer(project, false));
            commitFiles.setEnabled(false);
            
            commitFileScrollPane.setViewportView(commitFiles);
            commitFiles.setMaximumSize(commitFiles.getMaximumSize());
            commitFiles.setMinimumSize(commitFiles.getMaximumSize());
            commitFiles.setBackground(new Color(windowPanel.getBackground().getRGB()));

            topPanel.add(commitFileScrollPane, BorderLayout.CENTER);
        }
        windowPanel.add(topPanel, BorderLayout.NORTH);
        middlePanel = new JPanel();
        JButton closeButton = BlueJTheme.getCancelButton();
        closeButton.setText(Config.getString("close"));
        Platform.runLater(() -> setCloseIsButton(closeButton));

        {
            middlePanel.setLayout(new BorderLayout());
            middlePanel.setBorder(BorderFactory.createEmptyBorder());

            JLabel commentLabel = new JLabel(Config.getString(
                    "team.commit.comment"));
            commentLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
            middlePanel.add(commentLabel, BorderLayout.NORTH);

            commitText = new JTextArea("");
            commitText.setRows(5);
            commitText.setColumns(35);

            Dimension size = commitText.getPreferredSize();
            size.width = commitText.getMinimumSize().width;
            commitText.setMinimumSize(size);
            
            //create listeners to enable the commit button if there is a comment.
            commitText.getDocument().addDocumentListener(new DocumentListener()
            {
                @Override
                public void insertUpdate(DocumentEvent e)
                {
                    emptyCommitText = false;
                    if (commitText.isEnabled()){
                        commitAction.setEnabled(!emptyCommitText);
                    }
                }

                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    emptyCommitText = commitText.getText().isEmpty();
                    if (commitText.isEnabled()) {
                        commitAction.setEnabled(!emptyCommitText);
                    }
                }

                @Override
                public void changedUpdate(DocumentEvent e)
                {
                    emptyCommitText = commitText.getText().isEmpty();
                    if (commitText.isEnabled()) {
                        commitAction.setEnabled(!emptyCommitText);
                    }
                }
            });
            
            JScrollPane commitTextScrollPane = new JScrollPane(commitText);
            commitTextScrollPane.setMinimumSize(size);
            middlePanel.add(commitTextScrollPane, BorderLayout.CENTER);

            commitAction = new CommitAction(this);
            commitButton = BlueJTheme.getOkButton();
            commitButton.setAction(commitAction);

            pushAction = new PushAction(this);
            pushButton = BlueJTheme.getOkButton();
            pushButton.setAction(pushAction);

            closeButton.addActionListener((ActionEvent e) -> {
                commitAndPushWorker.abort();
                commitAction.cancel();
                setVisible(false);
            });

            DBox middleBox = new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.0f);
            middleBox.setBorder(BlueJTheme.generalBorder);
            DBox checkBoxPanel = new DBox(DBoxLayout.Y_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.0f);
            checkBoxPanel.setBorder(BorderFactory.createEmptyBorder(10,0,10,10));
                        
            includeLayout = new JCheckBox(Config.getString("team.commit.includelayout"));
            includeLayout.setEnabled(false);
            includeLayout.addActionListener((ActionEvent e) -> {
                JCheckBox layoutCheck = (JCheckBox) e.getSource();
                if (layoutCheck.isSelected()) {
                    addModifiedLayouts();
                    if (!commitButton.isEnabled()) {
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

            checkBoxPanel.add(includeLayout);
            JPanel commitArea = new JPanel(new BorderLayout());
            commitArea.setBorder(BorderFactory.createEmptyBorder(6, 0, 4, 6));
            commitArea.add(commitButton, BorderLayout.EAST);
            middleBox.add(checkBoxPanel);
            middleBox.add(commitArea);
            middlePanel.add(middleBox, BorderLayout.SOUTH);
        }

        topPanel.add(middlePanel, BorderLayout.SOUTH);
        bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JScrollPane pushFileScrollPane = new JScrollPane();
        
        {
            bottomPanel.setLayout(new BorderLayout());

            JLabel pushFilesLabel = new JLabel(Config.getString(
                    "team.commitPush.push.files"));
            pushFilesLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 5, 0));
            bottomPanel.add(pushFilesLabel, BorderLayout.NORTH);
            pushFiles = new JList(pushListModel);
            pushFiles.setCellRenderer(new FileRenderer(project, true));
            pushFiles.setEnabled(false);
            pushFiles.setVisibleRowCount(4);
            pushFileScrollPane.setViewportView(pushFiles);
            pushFiles.setBackground(new Color(bottomPanel.getBackground().getRGB()));

            bottomPanel.add(pushFileScrollPane, BorderLayout.CENTER);
        }

        
        DBox pushButtonPanel = new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.5f);
        pushButtonPanel.setBorder(BlueJTheme.generalBorderWithStatusBar);
        progressBar = new ActivityIndicator();
        progressBar.setRunning(false);
        pushButtonPanel.add(progressBar);
        pushButtonPanel.add(pushButton);
        pushButtonPanel.add(closeButton);
        
        bottomPanel.add(pushButtonPanel, BorderLayout.SOUTH);
        
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(Color.black);
        windowPanel.add(separator, BorderLayout.CENTER);
        windowPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        getContentPane().add(windowPanel);
        pack();
    }

    @Override
    public void setVisible(boolean show)
    {
        if (show) {
            // we want to set comments and commit action to disabled
            // until we know there is something to commit
            commitAction.setEnabled(false);
            commitText.setEnabled(false);
            commitText.setText("");
            includeLayout.setSelected(false);
            includeLayout.setEnabled(false);
            changedLayoutFiles.clear();
            commitListModel.removeAllElements();
            pushAction.setEnabled(false);
            pushListModel.removeAllElements();

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
                        Platform.runLater(() -> DialogManager.showErrorTextFX(this.asWindow(), msgFinal));
                    }
                }
                startProgress();
                commitAndPushWorker = new CommitAndPushWorker();
                commitAndPushWorker.start();

            } else {
                super.setVisible(false);
            }
        }
        super.setVisible(show);
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
        for (TeamStatusInfo info : changedLayoutFiles) {
            commitListModel.removeElement(info);
        }
        if (commitListModel.isEmpty()) {
            commitListModel.addElement(noFilesToCommit);
            commitText.setEnabled(false);
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
        Set<File> files = new HashSet<>();
        changedLayoutFiles.stream().forEach((info) -> {
            files.add(info.getFile());
        });
        return files;
    }

    /**
     * Remove a file from the list of changes layout files.
     */
    private void removeChangedLayoutFile(File file)
    {
        for (Iterator<TeamStatusInfo> it = changedLayoutFiles.iterator(); it.hasNext();) {
            TeamStatusInfo info = it.next();
            if (info.getFile().equals(file)) {
                it.remove();
                return;
            }
        }
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
            commitListModel.removeElement(noFilesToCommit);
            commitText.setEnabled(true);
        }
        // add diagram layout files to list of files to be committed
        Set<File> displayedLayouts = new HashSet<>();
        for (TeamStatusInfo info : changedLayoutFiles) {
            File parentFile = info.getFile().getParentFile();
            if (!displayedLayouts.contains(parentFile)) {
                commitListModel.addElement(info);
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
        includeLayout.setEnabled(hasChanged);
    }
    
    public void displayMessage(String msg)
    {
        progressBar.setMessage(msg);
    }

    class CommitAndPushWorker extends SwingWorker implements StatusListener
    {

        List<TeamStatusInfo> response;
        TeamworkCommand command;
        TeamworkCommandResult result;
        private boolean aborted, isPushAvailable;

        public CommitAndPushWorker()
        {
            super();
            response = new ArrayList<>();
            FileFilter filter = project.getTeamSettingsController().getFileFilter(true, false);
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
                    CommitAndPushFrame.this.dialogThenHide(() -> TeamUtils.handleServerResponseFX(result, CommitAndPushFrame.this.asWindow()));
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
                    if (!commitAction.isEnabled() && conflicts) {
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

                    if (!commitAction.isEnabled() && conflicts) {

                        handleConflicts(mergeConflictsInPush, deleteConflictsInPush,
                                otherConflictsInPush, null);
                        return;
                    }
                }

                if (commitListModel.isEmpty()) {
                    commitListModel.addElement(noFilesToCommit);
                } else {
                    commitText.setEnabled(true);
                    commitText.requestFocusInWindow();
                    commitAction.setEnabled(!emptyCommitText);
                }
                
                if (pushListModel.isEmpty()){
                    if (isPushAvailable){
                        pushListModel.addElement(pushNeeded);
                    } else {
                        pushListModel.addElement(noFilesToPush);
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
                CommitAndPushFrame.this.dialogThenHide(() -> DialogManager.showMessageFX(CommitAndPushFrame.this.asWindow(), "team-uptodate-failed"));
                return;
            }

            stopProgress();
            CommitAndPushFrame.this.dialogThenHide(() -> DialogManager.showMessageWithTextFX(CommitAndPushFrame.this.asWindow(), dlgLabel, filesList));
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
         * @param conflicts The set to store unresolved conflicts in
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
                int status;
                //select status to use.
                if (remote) {
                    status = statusInfo.getRemoteStatus();
                } else {
                    status = statusInfo.getStatus();
                }

                if (filter.accept(statusInfo, !remote)) {
                    if (!isPkgFile) {
                        filesToCommit.add(file);
                    } else if (status == TeamStatusInfo.STATUS_NEEDSADD
                            || status == TeamStatusInfo.STATUS_DELETED
                            || status == TeamStatusInfo.STATUS_CONFLICT_LDRM) {
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

            if (!remote) {
                setLayoutChanged(!changedLayoutFiles.isEmpty());
            }
        }

        /**
         * Update the list model with a file list.
         * @param fileSet
         * @param info 
         */
        private void updateListModel(DefaultListModel listModel, Set<File> fileSet, List<TeamStatusInfo> info)
        {
            fileSet.stream().map((f) -> getTeamStatusInfoFromFile(f,info)).filter((tsi) -> (!listModel.contains(tsi))).forEach((tsi) -> {
                if (tsi!= null){
                    listModel.addElement(tsi);
                }
            });
        }

        
        private TeamStatusInfo getTeamStatusInfoFromFile(File f, List<TeamStatusInfo> info)
        {
            if (f != null && !info.isEmpty()){
                for (TeamStatusInfo item : info) {
                    if (item.getFile().equals(f)){
                        return item;
                    }
                }
            }
            return null;
        }

    }
}
