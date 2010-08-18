/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import bluej.BlueJTheme;
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
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;
import bluej.utility.SwingWorker;
import bluej.utility.Utility;


/**
 * A Swing based user interface to add commit comments.
 * @author Bruce Quig
 * @version $Id$
 */
public class CommitCommentsFrame extends EscapeDialog
{
    private JList commitFiles;
    private JPanel topPanel;
    private JPanel bottomPanel;
    private JTextArea commitText;
    private JButton commitButton;
    private JCheckBox includeLayout;
    private ActivityIndicator progressBar;
    private CommitAction commitAction;
    private CommitWorker commitWorker;

    private Project project;
    
    private Repository repository;
    private DefaultListModel commitListModel;
    
    private Set<TeamStatusInfo> changedLayoutFiles;
    
    /** The packages whose layout should be committed compulsorily */
    private Set<File> packagesToCommmit = new HashSet<File>();
    
    private static String noFilesToCommit = Config.getString("team.nocommitfiles"); 

    public CommitCommentsFrame(Project proj)
    {
        project = proj;
        changedLayoutFiles = new HashSet<TeamStatusInfo>();
        createUI();
        DialogManager.centreDialog(this);
    }
    
    public void setVisible(boolean show)
    {
        super.setVisible(show);
        if (show) {
            // we want to set comments and commit action to disabled
            // until we know there is something to commit
            commitAction.setEnabled(false);
            commitText.setEnabled(false);
            includeLayout.setSelected(false);
            includeLayout.setEnabled(false);
            changedLayoutFiles.clear();
            commitListModel.removeAllElements();
            
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
                        DialogManager.showErrorText(this, msg);
                    }
                }
                startProgress();
                commitWorker = new CommitWorker();
                commitWorker.start();
            }
            else {
                super.setVisible(false);
            }
        }
    }
    
    /**
     * Create the user-interface for the error display dialog.
     */
    protected void createUI()
    {
        setTitle(Config.getString("team.commit.title"));
        commitListModel = new DefaultListModel();
        
        //setIconImage(BlueJTheme.getIconImage());
        setLocation(Config.getLocation("bluej.commitdisplay"));

        // save position when window is moved
        addComponentListener(new ComponentAdapter() {
                public void componentMoved(ComponentEvent event)
                {
                    Config.putLocation("bluej.commitdisplay", getLocation());
                }
            });

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBorder(BlueJTheme.generalBorderWithStatusBar);
        splitPane.setResizeWeight(0.5);

        topPanel = new JPanel();

        JScrollPane commitFileScrollPane = new JScrollPane();

        {
            topPanel.setLayout(new BorderLayout());

            JLabel commitFilesLabel = new JLabel(Config.getString(
                        "team.commit.files"));
            commitFilesLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            topPanel.add(commitFilesLabel, BorderLayout.NORTH);

            commitFiles = new JList(commitListModel);
            commitFiles.setCellRenderer(new FileRenderer(project));
            commitFiles.setEnabled(false);
            commitFileScrollPane.setViewportView(commitFiles);
            
            topPanel.add(commitFileScrollPane, BorderLayout.CENTER);
        }

        splitPane.setTopComponent(topPanel);

        bottomPanel = new JPanel();

        {
            bottomPanel.setLayout(new BorderLayout());

            JLabel commentLabel = new JLabel(Config.getString(
                        "team.commit.comment"));
            commentLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            bottomPanel.add(commentLabel, BorderLayout.NORTH);

            commitText = new JTextArea("");
            commitText.setRows(6);
            commitText.setColumns(42);

            Dimension size = commitText.getPreferredSize();
            size.width = commitText.getMinimumSize().width;
            commitText.setMinimumSize(size);

            JScrollPane commitTextScrollPane = new JScrollPane(commitText);
            commitTextScrollPane.setMinimumSize(size);
            bottomPanel.add(commitTextScrollPane, BorderLayout.CENTER);

            commitAction = new CommitAction(this);
            commitButton = BlueJTheme.getOkButton();
            commitButton.setAction(commitAction);
            getRootPane().setDefaultButton(commitButton);

            JButton closeButton = BlueJTheme.getCancelButton();
            closeButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        commitWorker.abort();
                        commitAction.cancel();
                        setVisible(false);
                    }
                });
           
            DBox buttonPanel = new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.5f);
            buttonPanel.setBorder(BlueJTheme.generalBorder);
            
            progressBar = new ActivityIndicator();
            progressBar.setRunning(false);
            
            DBox checkBoxPanel = new DBox(DBoxLayout.Y_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.5f);
            includeLayout = new JCheckBox(Config.getString("team.commit.includelayout"));
            includeLayout.setEnabled(false);
            includeLayout.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    JCheckBox layoutCheck = (JCheckBox)e.getSource();
                    if(layoutCheck.isSelected()) {
                        addModifiedLayouts();
                        if(!commitButton.isEnabled())
                            commitAction.setEnabled(true);
                    }
                    // unselected
                    else {
                        removeModifiedLayouts();
                        if(isCommitListEmpty())
                            commitAction.setEnabled(false);
                    }
                }
            });

            checkBoxPanel.add(includeLayout);
            checkBoxPanel.add(buttonPanel);
            
            buttonPanel.add(progressBar);
            buttonPanel.add(commitButton);
            buttonPanel.add(closeButton);
            bottomPanel.add(checkBoxPanel, BorderLayout.SOUTH);
        }

        splitPane.setBottomComponent(bottomPanel);

        getContentPane().add(splitPane);
        pack();
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
        for(Iterator<TeamStatusInfo> it = changedLayoutFiles.iterator(); it.hasNext(); ) {
            TeamStatusInfo info = it.next();
            if (! packagesToCommmit.contains(info.getFile().getParentFile())) {
                commitListModel.removeElement(info);
            }
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
    
    private void addModifiedLayouts()
    {
        if(commitListModel.contains(noFilesToCommit)) {
            commitListModel.removeElement(noFilesToCommit);
            commitText.setEnabled(true);
        }
        // add diagram layout files to list of files to be committed
        Set<File> displayedLayouts = new HashSet<File>();
        for(Iterator<TeamStatusInfo> it = changedLayoutFiles.iterator(); it.hasNext(); ) {
            TeamStatusInfo info = it.next();
            File parentFile = info.getFile().getParentFile();
            if (! displayedLayouts.contains(parentFile)
                    && ! packagesToCommmit.contains(parentFile)) {
                commitListModel.addElement(info);
                displayedLayouts.add(info.getFile().getParentFile());
            }
        }
    }
    
    /**
     * Get a list of the layout files to be committed
     */
    public Set<File> getChangedLayoutFiles()
    {
        Set<File> files = new HashSet<File>();
        for(Iterator<TeamStatusInfo> it = changedLayoutFiles.iterator(); it.hasNext(); ) {
            TeamStatusInfo info = it.next();
            files.add(info.getFile());
        }
        return files;
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
        includeLayout.setEnabled(hasChanged);
    }

    /**
    * Inner class to do the actual cvs status check to populate commit dialog
    * to ensure that the UI is not blocked during remote call
    */
    class CommitWorker extends SwingWorker implements StatusListener
    {
        List<TeamStatusInfo> response;
        TeamworkCommand command;
        TeamworkCommandResult result;
        private boolean aborted;

        public CommitWorker()
        {
            super();
            response = new ArrayList<TeamStatusInfo>();
            FileFilter filter = project.getTeamSettingsController().getFileFilter(true);
            command = repository.getStatus(this, filter, false);
        }
        
        /*
         * @see bluej.groupwork.StatusListener#gotStatus(bluej.groupwork.TeamStatusInfo)
         */
        public void gotStatus(TeamStatusInfo info)
        {
            response.add(info);
        }
        
        /*
         * @see bluej.groupwork.StatusListener#statusComplete(bluej.groupwork.CommitHandle)
         */
        public void statusComplete(StatusHandle statusHandle)
        {
            commitAction.setStatusHandle(statusHandle);
        }
        
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

        public void finished()
        {
            stopProgress();
            if (! aborted) {
                if (result.isError()) {
                    TeamUtils.handleServerResponse(result, CommitCommentsFrame.this);
                    setVisible(false);
                }
                else if (response != null) {
                    Set<File> filesToCommit = new HashSet<File>();
                    Set<File> filesToAdd = new LinkedHashSet<File>();
                    Set<File> filesToDelete = new HashSet<File>();
                    Set<File> mergeConflicts = new HashSet<File>();
                    Set<File> deleteConflicts = new HashSet<File>();
                    Set<File> otherConflicts = new HashSet<File>();
                    Set<File> needsMerge = new HashSet<File>();
                    Set<File> modifiedLayoutFiles = new HashSet<File>();

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

                if(commitListModel.isEmpty()) {
                    commitListModel.addElement(noFilesToCommit);
                }
                else {
                    commitText.setEnabled(true);
                    commitText.requestFocusInWindow();
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
            if (! mergeConflicts.isEmpty()) {
                dlgLabel = "team-resolve-merge-conflicts";
                filesList = buildConflictsList(mergeConflicts);
            }
            else if (! deleteConflicts.isEmpty()) {
                dlgLabel = "team-resolve-conflicts-delete";
                filesList = buildConflictsList(deleteConflicts);
            }
            else if (! otherConflicts.isEmpty()) {
                dlgLabel = "team-update-first";
                filesList = buildConflictsList(otherConflicts);
            }
            else {
                stopProgress();
                DialogManager.showMessage(CommitCommentsFrame.this, "team-uptodate-failed");
                CommitCommentsFrame.this.setVisible(false);
                return;
            }

            stopProgress();
            DialogManager.showMessageWithText(CommitCommentsFrame.this, dlgLabel, filesList);
            CommitCommentsFrame.this.setVisible(false);
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
         * of those which are to be added (i.e. which aren't in the repository) and
         * which are to be removed.
         * 
         * @param info  The list of files with status (List of TeamStatusInfo)
         * @param filesToCommit  The set to store the files to commit in
         * @param filesToAdd     The set to store the files to be added in
         * @param filesToRemove  The set to store the files to be removed in
         * @param mergeConflicts The set to store files with merge conflicts in.
         * @param deleteConflicts The set to store files with conflicts in, which
         *                        need to be resolved by first deleting the local file
         * @param otherConflicts  The set to store files with "locally deleted" conflicts
         *                        (locally deleted, remotely modified).
         * @param needsMerge     The set of files which are updated locally as
         *                       well as in the repository (required merging).
         * @param conflicts      The set to store unresolved conflicts in
         */
        private void getCommitFileSets(List<TeamStatusInfo> info, Set<File> filesToCommit, Set<File> filesToAdd,
                Set<File> filesToRemove, Set<File> mergeConflicts, Set<File> deleteConflicts,
                Set<File> otherConflicts, Set<File> needsMerge, Set<File> modifiedLayoutFiles)
        {
            //boolean includeLayout = project.getTeamSettingsController().includeLayout();
            
            CommitFilter filter = new CommitFilter();
            Map<File,File> modifiedLayoutDirs = new HashMap<File,File>();

            for (Iterator<TeamStatusInfo> it = info.iterator(); it.hasNext();) {
                TeamStatusInfo statusInfo = it.next();
                File file = statusInfo.getFile();
                boolean isPkgFile = BlueJPackageFile.isPackageFileName(file.getName());
                int status = statusInfo.getStatus();
                if(filter.accept(statusInfo)) {
                    if (! isPkgFile) {
                        commitListModel.addElement(statusInfo);
                        filesToCommit.add(file);
                    }
                    else if (status == TeamStatusInfo.STATUS_NEEDSADD
                                || status == TeamStatusInfo.STATUS_DELETED
                                || status == TeamStatusInfo.STATUS_CONFLICT_LDRM) {
                        // Package file which must be committed.
                        if (packagesToCommmit.add(statusInfo.getFile().getParentFile())) {
                            commitListModel.addElement(statusInfo);
                            File otherPkgFile = modifiedLayoutDirs.remove(file.getParentFile());
                            if (otherPkgFile != null) {
                                removeChangedLayoutFile(otherPkgFile);
                                filesToCommit.add(otherPkgFile);
                            }
                        }
                        filesToCommit.add(statusInfo.getFile());
                    }
                    else {
                        // add file to list of files that may be added to commit
                        File parentFile = file.getParentFile();
                        if (! packagesToCommmit.contains(parentFile)) {
                            modifiedLayoutFiles.add(file);
                            modifiedLayoutDirs.put(parentFile, file);
                            // keep track of StatusInfo objects representing changed diagrams
                            changedLayoutFiles.add(statusInfo);
                        }
                        else {
                            // We must commit the file unconditionally
                            filesToCommit.add(file);
                        }
                    }
                    
                    if (status == TeamStatusInfo.STATUS_NEEDSADD) {
                        filesToAdd.add(statusInfo.getFile());
                    }
                    else if (status == TeamStatusInfo.STATUS_DELETED
                            || status == TeamStatusInfo.STATUS_CONFLICT_LDRM) {
                        filesToRemove.add(statusInfo.getFile());
                    }
                }
                else {
                    if(! isPkgFile) {
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
            }
            
            setLayoutChanged (! changedLayoutFiles.isEmpty());
        }
    }
}
