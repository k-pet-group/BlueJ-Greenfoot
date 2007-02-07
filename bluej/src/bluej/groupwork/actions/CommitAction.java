package bluej.groupwork.actions;

import java.awt.event.ActionEvent;
import java.util.Set;
import javax.swing.AbstractAction;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.groupwork.BasicServerResponse;
import bluej.groupwork.InvalidCvsRootException;
import bluej.groupwork.ui.CommitCommentsFrame;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.Config;
import bluej.groupwork.TeamUtils;
import java.awt.EventQueue;


/**
 * An action to do an actual commit. By this stage we know what we are
 * committing and have the commit comments.
 * 
 * @author Kasper
 * @version $Id: CommitAction.java 4838 2007-02-07 01:01:21Z davmac $
 */
public class CommitAction extends AbstractAction
{
    private Set newFiles; // which files are new files
    private Set deletedFiles; // which files are to be removed
    private Set files; // files to commit (includes both of above)
    private Set modifiedLayouts;
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
            
                BasicServerResponse basicServerResponse = null;
            
                public void run()
                {
                    try {
                        String comment = commitCommentsFrame.getComment();
                        
                        //last step before committing is to add in modified diagram 
                        //layouts if selected in commit comments dialog
                        if(commitCommentsFrame.includeLayout()) {
                            files.addAll(commitCommentsFrame.getChangedLayoutFiles());
                        }
                        
                        // Note, getRepository() cannot return null here - otherwise
                        // the commit dialog was cancelled (and we'd never get here)
                        basicServerResponse = project.getRepository().commitAll(newFiles, deletedFiles, files, comment);
                    } catch (CommandAbortedException e) {
                        e.printStackTrace();
                    } catch (CommandException e) {
                        e.printStackTrace();
                    } catch (AuthenticationException e) {
                        TeamUtils.handleAuthenticationException(commitCommentsFrame);
                    } catch (InvalidCvsRootException e) {
                        TeamUtils.handleInvalidCvsRootException(commitCommentsFrame);
                    }

                    commitCommentsFrame.stopProgress();
                    if (basicServerResponse != null && ! basicServerResponse.isError()) {
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                PkgMgrFrame.displayMessage(project, Config.getString("team.commit.statusDone"));
                            }
                        });
                    }
                  
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            TeamUtils.handleServerResponse(basicServerResponse, commitCommentsFrame);
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
