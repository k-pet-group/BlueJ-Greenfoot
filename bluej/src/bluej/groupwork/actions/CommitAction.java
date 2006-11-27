package bluej.groupwork.actions;

import java.util.Set;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.groupwork.BasicServerResponse;
import bluej.groupwork.InvalidCvsRootException;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.Config;


/**
 * An action to do an actual commit. By this stage we know what we are
 * committing and have the commit comments.
 * 
 * @author Kasper
 * @version $Id: CommitAction.java 4704 2006-11-27 00:07:19Z bquig $
 */
public class CommitAction extends TeamAction
{
    private Set newFiles; // which files are new files
    private Set deletedFiles; // which files are to be removed
    private Set files; // files to commit (includes both of above)
    
    public CommitAction()
    {
        super("team.commit");
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
    

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(PkgMgrFrame pmf)
    {
        Project project = pmf.getProject();
        
        if (project != null) {
            project.saveAllEditors();
            project.getCommitCommentsDialog(pmf).setVisible(false);
            doCommit(project, pmf);
        }
    }

    private void doCommit(final Project project, final PkgMgrFrame pmf)
    {
        Thread thread = new Thread() {
                public void run()
                {
                    BasicServerResponse basicServerResponse = null;

                    try {
                        String comment = project.getCommitCommentsDialog(pmf).getComment();
                        // Note, getRepository() cannot return null here - otherwise
                        // the commit dialog was cancelled (and we'd never get here)
                        basicServerResponse = project.getRepository().commitAll(newFiles, deletedFiles, files, comment);
                    } catch (CommandAbortedException e) {
                        e.printStackTrace();
                    } catch (CommandException e) {
                        e.printStackTrace();
                    } catch (AuthenticationException e) {
                        handleAuthenticationException(e);
                    } catch (InvalidCvsRootException e) {
                        handleInvalidCvsRootException(e);
                    }

                    stopProgressBar();
                    if (basicServerResponse != null && ! basicServerResponse.isError()) {
                        setStatus(Config.getString("team.commit.statusDone"));
                    }
                  
                    handleServerResponse(basicServerResponse);
                }
            };

        thread.start();
        startProgressBar();
        setStatus(Config.getString("team.commit.statusMessage"));
    }
}
