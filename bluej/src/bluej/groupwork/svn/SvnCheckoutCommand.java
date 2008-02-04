package bluej.groupwork.svn;

import java.io.File;

import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.javahl.SVNClientInterface;

import bluej.groupwork.TeamworkCommandAborted;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;

/**
 * Subversion "checkout" command.
 * 
 * @author Davin McCall
 */
public class SvnCheckoutCommand extends SvnCommand
{
    private File checkoutPath;
    private String moduleName;
    
    public SvnCheckoutCommand(SvnRepository repository, File projectPath)
    {
        super(repository);
        this.checkoutPath = projectPath.getAbsoluteFile();
        moduleName = projectPath.getName();
    }
    
    protected TeamworkCommandResult doCommand()
    {
        SVNClientInterface client = getClient();
        String reposUrl = getRepository().getReposUrl();
        reposUrl += "/" + moduleName;
        
        try {
            client.checkout(reposUrl, checkoutPath.getAbsolutePath(),
                Revision.HEAD, true);
            if (! isCancelled()) {
                return new TeamworkCommandResult();
            }
        }
        catch (ClientException ce) {
            if (! isCancelled()) {
                return new TeamworkCommandError(ce.getLocalizedMessage());
            }
        }

        return new TeamworkCommandAborted();
    }
}
