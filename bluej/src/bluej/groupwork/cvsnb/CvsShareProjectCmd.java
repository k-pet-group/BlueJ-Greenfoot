package bluej.groupwork.cvsnb;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;


/**
 * Command to share a project to a CVS repository.
 * 
 * @author Davin McCall
 */
public class CvsShareProjectCmd extends CvsCommand
{
    public CvsShareProjectCmd(CvsRepository repository)
    {
        super(repository);
    }
    
    protected BasicServerResponse doCommand()
        throws CommandAbortedException, CommandException, AuthenticationException
    {
        BasicServerResponse importResponse;
        BasicServerResponse checkoutResponse;
     
        BlueJCvsClient client = getClient();
        importResponse = repository.importInRepository(client);

        if (importResponse.isError()) {
            return importResponse;
        } else {
            // We need a fresh client
            client = getClient();
            
            if (client != null) {
                checkoutResponse = repository.doCheckout(client, repository.getProjectPath());
            }
            else {
                throw new CommandAbortedException("","");
            }
        }

        return checkoutResponse;
    }

}
