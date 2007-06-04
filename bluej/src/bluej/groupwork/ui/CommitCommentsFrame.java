package bluej.groupwork.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.*;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.groupwork.*;
import bluej.groupwork.actions.CommitAction;
import bluej.pkgmgr.Project;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;
import bluej.utility.SwingWorker;


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
    
    private Set changedLayoutFiles;
    
    private static String noFilesToCommit = Config.getString("team.nocommitfiles"); 

    public CommitCommentsFrame(Project proj)
    {
        project = proj;
        changedLayoutFiles = new HashSet();
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
                project.saveAllEditors();
                project.saveAllGraphLayout();
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
        for(Iterator it = changedLayoutFiles.iterator();it.hasNext();) {
            commitListModel.removeElement(it.next());
        }
        if(commitListModel.isEmpty()) {
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
        for(Iterator it = changedLayoutFiles.iterator(); it.hasNext(); ) {
            commitListModel.addElement(it.next());
        }
    }
    
    /**
     * Get a list of the layout files to be committed
     */
    public Set getChangedLayoutFiles()
    {
        Set files = new HashSet();
        for(Iterator it = changedLayoutFiles.iterator(); it.hasNext(); ) {
            TeamStatusInfo info = (TeamStatusInfo)it.next();
            files.add(info.getFile());
        }
        return files;
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
        List response;
        TeamworkCommand command;
        TeamworkCommandResult result;
        private boolean aborted;

        public CommitWorker()
        {
            super();
            response = new ArrayList();
            Set files = project.getTeamSettingsController().getProjectFiles(true);
            command = repository.getStatus(this, files, false);
        }
        
        /* (non-Javadoc)
         * @see bluej.groupwork.StatusListener#gotStatus(bluej.groupwork.TeamStatusInfo)
         */
        public void gotStatus(TeamStatusInfo info)
        {
            response.add(info);
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
                    Set filesToCommit = new HashSet();
                    Set filesToAdd = new HashSet();
                    Set filesToDelete = new HashSet();
                    Set conflicts = new HashSet();
                    Set modifiedLayoutFiles = new HashSet();

                    List info = response;
                    getCommitFileSets(info, filesToCommit, filesToAdd, filesToDelete, conflicts, modifiedLayoutFiles);

                    if (conflicts.size() != 0) {
                        String filesList = "";
                        Iterator i = conflicts.iterator();
                        for (int j = 0; j < 10 && i.hasNext(); j++) {
                            File conflictFile = (File) i.next();
                            filesList += "    " + conflictFile.getName() + "\n";
                        }

                        if (i.hasNext()) {
                            filesList += "    (and more - check status)";
                        }

                        stopProgress();
                        DialogManager.showMessageWithText(CommitCommentsFrame.this, "team-update-first", filesList);
                        CommitCommentsFrame.this.setVisible(false);
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
                    //this should be conditional upon a need to commit
                    // this should be re-enabled when we fully handle diagram layout change detection
                    commitText.setEnabled(true);
                    commitAction.setEnabled(true);

                }
            }
            
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
         * @param conflicts      The set to store unresolved conflicts in
         */
        private void getCommitFileSets(List info, Set filesToCommit, Set filesToAdd,
                Set filesToRemove, Set conflicts, Set modifiedLayoutFiles)
        {
            //boolean includeLayout = project.getTeamSettingsController().includeLayout();
            
            CommitFilter filter = new CommitFilter();

            for (Iterator it = info.iterator(); it.hasNext();) {
                TeamStatusInfo statusInfo = (TeamStatusInfo) it.next();
                int status = statusInfo.getStatus();
                if(filter.accept(statusInfo)) {
                    if (!statusInfo.getFile().getName().equals("bluej.pkg") 
                            || status == TeamStatusInfo.STATUS_NEEDSADD 
                            || status == TeamStatusInfo.STATUS_DELETED ) {
                        
                        commitListModel.addElement(statusInfo);
                        filesToCommit.add(statusInfo.getFile());
                    }
                    
                    if (status == TeamStatusInfo.STATUS_NEEDSADD) {
                        filesToAdd.add(statusInfo.getFile());
                    }
                    else if (status == TeamStatusInfo.STATUS_DELETED) {
                        filesToRemove.add(statusInfo.getFile());
                    }
                    else if(statusInfo.getFile().getName().equals("bluej.pkg")){
                        // add file to list of files that may be added to commit
                        modifiedLayoutFiles.add(statusInfo.getFile());
                        // keep track of StatusInfo objects representing changed diagrams
                        changedLayoutFiles.add(statusInfo);
                       
                        setLayoutChanged(true);
                    }
                }
                else {
                    if (status == TeamStatusInfo.STATUS_HASCONFLICTS
                                || status == TeamStatusInfo.STATUS_NEEDSMERGE
                                || status == TeamStatusInfo.STATUS_UNRESOLVED) {
                        if(!statusInfo.getFile().getName().equals("bluej.pkg") || includeLayout())
                            conflicts.add(statusInfo.getFile());
                    }
                }
            }
        }
        
    }
   
}
