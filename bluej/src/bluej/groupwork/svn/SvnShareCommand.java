package bluej.groupwork.svn;

import java.io.File;

import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.javahl.SVNClientInterface;

import bluej.Config;
import bluej.groupwork.TeamworkCommandAborted;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;

/**
 * Subversion command to share a project
 * 
 * @author Davin McCall
 */
public class SvnShareCommand extends SvnCommand
{
    public SvnShareCommand(SvnRepository repository)
    {
        super(repository);
    }
    
    protected TeamworkCommandResult doCommand()
    {
        SVNClientInterface client = getClient();
        File projectPath = getRepository().getProjectPath();
        String projUrl = getRepository().getReposUrl() + "/" + projectPath.getName();
        
        try {
            client.mkdir(new String[] {projUrl},
                    Config.getString("team.import.initialMessage"));
            
            client.checkout(projUrl, projectPath.getAbsolutePath(),
                    Revision.HEAD, false);
            
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
