package bluej.groupwork.cvsnb;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.groupwork.UpdateListener;

/**
 * Command to perform an update.
 * 
 * @author Davin McCall
 */
public class CvsUpdateCommand extends CvsCommand
{
    private UpdateListener listener;
    
    public CvsUpdateCommand(CvsRepository repository, UpdateListener listener)
    {
        super(repository);
        this.listener = listener;
    }

    protected BasicServerResponse doCommand()
        throws CommandAbortedException, CommandException, AuthenticationException
    {
        UpdateServerResponse response = repository.doUpdateAll(getClient(), listener);
        listener.handleConflicts(response);
        return response;
    }
}
