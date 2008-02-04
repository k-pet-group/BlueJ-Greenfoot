package bluej.groupwork.svn;

import java.util.List;

import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.DirEntry;
import org.tigris.subversion.javahl.NodeKind;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.javahl.SVNClientInterface;

import bluej.groupwork.TeamworkCommandAborted;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;

/**
 * A command to retrieve a list of modules from a subversion repository.
 * 
 * @author Davin McCall
 */
public class SvnModulesCommand extends SvnCommand
{
    private List modulesList;
    
    public SvnModulesCommand(SvnRepository repository, List modulesList)
    {
        super(repository);
        this.modulesList = modulesList;
    }
    
    protected TeamworkCommandResult doCommand()
    {
        SVNClientInterface client = getClient();
        
        try {
            DirEntry [] entries = client.list(getRepository().getReposUrl(), Revision.HEAD, false);
            if (! isCancelled()) {
                for (int i = 0; i < entries.length; i++) {
                    if (entries[i].getNodeKind() == NodeKind.dir) {
                        modulesList.add(entries[i].getPath());
                    }
                }
                
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
