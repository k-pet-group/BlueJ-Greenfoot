package bluej.groupwork.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.Set;

import javax.swing.AbstractAction;

import bluej.Config;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.ui.CommitCommentsFrame;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;


/**
 * An action to do an actual commit. By this stage we know what we are
 * committing and have the commit comments.
 * 
 * @author Kasper
 * @version $Id: CommitAction.java 4916 2007-04-12 03:57:23Z davmac $
 */
public class CommitAction extends AbstractAction
{
    private Set newFiles; // which files are new files
    private Set deletedFiles; // which files are to be removed
    private Set files; // files to commit (includes both of above)
    private CommitCommentsFrame commitCommentsFrame;
    
    public CommitAction(CommitCommentsFrame frame)
    {
        super(Config.getString("team.commit"));
        commitCommentsFrame = frame; 
    }
    
    /**
     * Set the files which are new, that is, which aren't presently under
     * version management and which need to be added.
     */
    public void setNewFiles(Set newFiles)
    {
        this.newFiles = newFiles;
    }
    
    /**
     * Set the files which have been deleted locally, and the deletion
     * needs to be propagated to the repository.
     */
    public void setDeletedFiles(Set deletedFiles)
    {
        this.deletedFiles = deletedFiles;
    }
    
    /**
     * Set all files which are to be committed. This should include both
     * the new files and the deleted files, as well as any other files
     * which have been locally modified and need to be committed.
     */
    public void setFiles(Set files)
    {
        this.files = files;
    }
    
    /**
     * accessor for combined list of new, deleted and modified files
     */
    public Set getFiles()
    {
        return files;
    }
    
    /**
     * 
     */
    public void actionPerformed(ActionEvent event) 
    {
        Project project = commitCommentsFrame.getProject();
        
        if (project != null) {
            project.saveAllEditors();
            //project.getCommitCommentsDialog().setVisible(false);
            setEnabled(false);
            doCommit(project);
            
        }
        
    }

    private void doCommit(final Project project)
    {
        Thread thread = new Thread() {
            
                TeamworkCommandResult result = null;
            
                public void run()
                {
                    String comment = commitCommentsFrame.getComment();

                    //last step before committing is to add in modified diagram 
                    //layouts if selected in commit comments dialog
                    if(commitCommentsFrame.includeLayout()) {
                        files.addAll(commitCommentsFrame.getChangedLayoutFiles());
                    }

                    Set binFiles = TeamUtils.extractBinaryFilesFromSet(newFiles);

                    // Note, getRepository() cannot return null here - otherwise
                    // the commit dialog was cancelled (and we'd never get here)
                    TeamworkCommand command = project.getRepository().commitAll(newFiles, binFiles, 
                            deletedFiles, files, comment);

                    result = command.getResult();
                    commitCommentsFrame.stopProgress();

                    if (! result.isError() && ! result.wasAborted()) {
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                PkgMgrFrame.displayMessage(project, Config.getString("team.commit.statusDone"));
                            }
                        });
                    }
                  
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            TeamUtils.handleServerResponse(result, commitCommentsFrame);
                            setEnabled(true);
                            commitCommentsFrame.setVisible(false);
                        }
                    });
                }
            };

        commitCommentsFrame.startProgress();
        thread.start();
        PkgMgrFrame.displayMessage(project, Config.getString("team.commit.statusMessage"));
    }

}
