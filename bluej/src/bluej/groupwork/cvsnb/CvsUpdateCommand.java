package bluej.groupwork.cvsnb;

import java.util.Set;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.groupwork.UpdateListener;

/**
 * Command to perform an update of a specified set of files
 * 
 * @author Davin McCall
 */
public class CvsUpdateCommand extends CvsCommand
{
    private UpdateListener listener; // may be null
    private Set theFiles;
    private Set forceFiles;
    
    public CvsUpdateCommand(CvsRepository repository, UpdateListener listener,
            Set theFiles, Set forceFiles)
    {
        super(repository);
        this.listener = listener;
        this.theFiles = theFiles;
        this.forceFiles = forceFiles;
    }

    protected BasicServerResponse doCommand()
        throws CommandAbortedException, CommandException, AuthenticationException
    {
        UpdateServerResponse response = null;
        
        if (! theFiles.isEmpty()) {
            BlueJCvsClient client = getClient();
            response = repository.doUpdateFiles(client, listener, theFiles, false);
            if (listener != null) {
                listener.handleConflicts(response);
            }
            
            if (response.isError()) {
                return response;
            }
        }
        
        if (! forceFiles.isEmpty()) {
            BlueJCvsClient client = getClient();
            response = repository.doUpdateFiles(client, listener, forceFiles, true);
        }
        
        return response;
    }
}
