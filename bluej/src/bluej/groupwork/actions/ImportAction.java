package bluej.groupwork.actions;

import java.awt.EventQueue;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.Config;
import bluej.groupwork.BasicServerResponse;
import bluej.groupwork.InvalidCvsRootException;
import bluej.groupwork.Repository;
import bluej.groupwork.TeamSettingsController;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;

/**
 * An action to perform an import into a repository, i.e. to share a project.
 * 
 * @author Kasper
 * @version $Id: ImportAction.java 4838 2007-02-07 01:01:21Z davmac $
 */
public class ImportAction extends TeamAction 
{
	public ImportAction()
    {
        super("team.import");
    }
	
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(PkgMgrFrame pmf)
    {
        Project project = pmf.getProject();
	    
        if (project == null) {
            return;
        }
	    
        doImport(pmf, project);
    }

    private void doImport(final PkgMgrFrame pmf, final Project project)
    {
        // The team settings controller is not initially associated with the
        // project, so you can still modify the repository location
        final TeamSettingsController tsc = new TeamSettingsController(project.getProjectDir());
        final Repository repository = tsc.getRepository();
        
        if (repository == null) {
            // user cancelled
            return;
        }

        setStatus(Config.getString("team.sharing"));
        startProgressBar(); 
        
        Thread thread = new Thread() {
            
            BasicServerResponse basicServerResponse = null;
            
            public void run()
            {
                // boolean resetStatus = true;
                try {
                    basicServerResponse = repository.shareProject();
                    if (basicServerResponse != null && ! basicServerResponse.isError()) {
                        project.setTeamSettingsController(tsc);
                        Set files = tsc.getProjectFiles(true);
                        Set newFiles = new HashSet(files);
                        basicServerResponse = repository.commitAll(newFiles, Collections.EMPTY_SET, files, Config.getString("team.import.initialMessage"));
                    }
                    
                    stopProgressBar();
                }
                catch (CommandAbortedException e) {
                    stopProgressBar();
                }
                catch (CommandException e) {
                    stopProgressBar();
                    e.printStackTrace();
                }
                catch (AuthenticationException e) {
                    handleAuthenticationException(e);
                }
                catch (InvalidCvsRootException e) {
                    handleInvalidCvsRootException(e);
                }
                                
                EventQueue.invokeLater(new Runnable() {
                    public void run()
                    {
                        handleServerResponse(basicServerResponse);
                        if(! basicServerResponse.isError()) {
                            setStatus(Config.getString("team.shared"));
                        }
                        else {
                            clearStatus();
                        }
                    }
                });
            }
        };
        thread.start();
    }
}
