package bluej.groupwork.cvsnb;

import java.util.List;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

/**
 * Command to list modules available in a Cvs repository.
 * 
 * @author Davin McCall
 */
public class CvsModulesCommand extends CvsCommand
{
    private List modules;
    
    public CvsModulesCommand(CvsRepository repository, List modules)
    {
        super(repository);
        this.modules = modules;
    }

    protected BasicServerResponse doCommand()
        throws CommandAbortedException, CommandException, AuthenticationException
    {
        return repository.doGetModules(getClient(), modules);
    }

}
