package bluej.groupwork.svn;

import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.SVNClientInterface;

import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandAborted;
import bluej.groupwork.TeamworkCommandResult;
import bluej.utility.Debug;

/**
 * Base class for subversion command implementations.
 * 
 * @author Davin McCall
 */
abstract public class SvnCommand
    implements TeamworkCommand
{
    private SvnRepository repository;
    private SVNClientInterface client;
    private boolean cancelled = false;
    
    protected SvnCommand(SvnRepository repository)
    {
        this.repository = repository;
    }
    
    public synchronized void cancel()
    {
        cancelled = true;
        if (client != null) {
            try {
                client.cancelOperation();
            }
            catch (ClientException ce) {
                // Why would we get an exception on a cancel?
                Debug.message("Exception during subversion cancel:");
                ce.printStackTrace(System.out);
            }
        }
    }
    
    /**
     * Check whether this command has been cancelled.
     */
    protected synchronized boolean isCancelled()
    {
        return cancelled;
    }
    
    /**
     * Get a handle to the SVN client interface.
     */
    protected SVNClientInterface getClient()
    {
        return client;
    }
    
    /**
     * Get a handle to the repository.
     */
    protected SvnRepository getRepository()
    {
        return repository;
    }

    public TeamworkCommandResult getResult()
    {
        return repository.execCommand(this);
    }
    
    public TeamworkCommandResult doCommand(SVNClientInterface client)
    {
        synchronized (this) {
            if (cancelled) {
                return new TeamworkCommandAborted();
            }
            this.client = client;
        }
        
        TeamworkCommandResult result = doCommand();
        this.client = null; // so that cancellation after completion doesn't
                       // cause the next command to be cancelled
        return result;
    }
    
    abstract protected TeamworkCommandResult doCommand();
}
