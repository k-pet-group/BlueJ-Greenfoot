package bluej.groupwork.cvsnb;

import java.io.File;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.groupwork.BlueJCvsClient;

/**
 * Command to check out a project from a CVS repository.
 * 
 * @author Davin McCall
 */
public class CvsCheckoutCommand extends CvsCommand
{
    private File projectPath;
    
    public CvsCheckoutCommand(CvsRepository repository, File projectPath)
    {
        super(repository);
        this.projectPath = projectPath;
    }
    
    protected BasicServerResponse doCommand()
        throws CommandAbortedException, CommandException, AuthenticationException
    {
        BlueJCvsClient client = getClient();
        return repository.doCheckout(client, projectPath);
    }
}
