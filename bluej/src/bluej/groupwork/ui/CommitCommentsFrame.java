package bluej.groupwork.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.*;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.groupwork.InvalidCvsRootException;
import bluej.groupwork.Repository;
import bluej.groupwork.StatusFilter;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.actions.CommitAction;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.DialogManager;
import bluej.utility.SwingWorker;
import bluej.utility.EscapeDialog;


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
    private ActivityIndicator progressBar;
    private CommitAction commitAction;

    private PkgMgrFrame parent;
    
    private Repository repository;
    private DefaultListModel commitListModel;

    public CommitCommentsFrame(PkgMgrFrame pmf)
    {
        super(pmf);
        parent = pmf;
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
            repository = parent.getProject().getRepository();
            
            if (repository != null) {
                parent.getProject().saveAllEditors();
                parent.getProject().saveAllGraphLayout();
                startProgress();
                new CommitWorker().start();
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
            commitFiles.setCellRenderer(new CommitFileRenderer());
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

            commitAction = new CommitAction();
            commitButton = BlueJTheme.getOkButton();
            commitButton.setAction(commitAction);
            getRootPane().setDefaultButton(commitButton);

            JButton closeButton = BlueJTheme.getCancelButton();
            closeButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        setVisible(false);
                    }
                });

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBorder(BlueJTheme.generalBorder);
            
            progressBar = new ActivityIndicator();
            progressBar.setRunning(false);
            buttonPanel.add(progressBar);
            buttonPanel.add(commitButton);
            buttonPanel.add(closeButton);
            bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
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

    private List initCommitFiles()
    {
        List statusServerResponse = null;

        try {
            // Always include the bluej.pkg files - they will be filtered later.
            // We don't want to filter them here because we need to always commit
            // new bluej.pkg files to the repository.
            Set files = parent.getProject().getTeamSettingsController().getProjectFiles(true);
            Set remoteDirs = repository.getRemoteDirs();
            statusServerResponse = repository.getStatus(files, remoteDirs);
        } catch (CommandAbortedException e) {
            e.printStackTrace();
        } catch (CommandException e) {
            e.printStackTrace();
        } catch (AuthenticationException e) {
            TeamUtils.handleAuthenticationException(parent);
        } catch (InvalidCvsRootException e) {
            TeamUtils.handleInvalidCvsRootException(parent);
        }

        return statusServerResponse;
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

    /**
    * Inner class to do the actual cvs status check to populate commit dialog
    * to ensure that the UI is not blocked during remote call
    */
    class CommitWorker extends SwingWorker
    {
        List response;

        public Object construct()
        {
            response = initCommitFiles();
            return response;
        }

        public void finished()
        {
            if (response != null) {
                Set filesToCommit = new HashSet();
                Set filesToAdd = new HashSet();
                Set filesToDelete = new HashSet();
                Set conflicts = new HashSet();
                
                List info = response;
                getCommitFileSets(info, filesToCommit, filesToAdd, filesToDelete, conflicts);
                
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
                commitListModel.addElement(Config.getString("team.nocommitfiles"));
               
            }
            else {
                //this should be conditional upon a need to commit
                // this should be re-enabled when we fully handle diagram layout change detection
                commitText.setEnabled(true);
                commitAction.setEnabled(true);
                
            }
            
            stopProgress();
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
                Set filesToRemove, Set conflicts)
        {
            boolean includeLayout = parent.includeLayout();
            StatusFilter filter = new StatusFilter(parent.getProject().getTeamSettingsController());

            for (Iterator it = info.iterator(); it.hasNext();) {
                TeamStatusInfo statusInfo = (TeamStatusInfo) it.next();
                int status = statusInfo.getStatus();
                if(filter.accept(statusInfo)) {
                    if (!statusInfo.getFile().getName().equals("bluej.pkg") 
                            || includeLayout 
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
                }
                else {
                    if (status == TeamStatusInfo.STATUS_HASCONFLICTS
                                || status == TeamStatusInfo.STATUS_NEEDSMERGE
                                || status == TeamStatusInfo.STATUS_UNRESOLVED) {
                        if(!statusInfo.getFile().getName().equals("bluej.pkg") || includeLayout)
                            conflicts.add(statusInfo.getFile());
                    }
                }
            }
        }
        
    }
   
}
