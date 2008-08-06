package bluej.groupwork.cvsnb;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.groupwork.TeamStatusInfo;

/**
 * A CVS commit operation which supports "forced commit".
 * 
 * @author Davin McCall
 */
public class CvsCommitCommand extends CvsCommitAllCommand
{
    private Set<TeamStatusInfo> forceFiles;
    
    public CvsCommitCommand(CvsRepository repository, Set<File> newFiles,
            Set<File> binaryNewFiles, Set<File> deletedFiles, Set<File> files,
            Set<TeamStatusInfo> forceFiles, String commitComment)
    {
        super(repository, newFiles, binaryNewFiles, deletedFiles, files, commitComment);
        this.forceFiles = forceFiles;
    }
    
    protected BasicServerResponse doCommand() throws CommandAbortedException,
            CommandException, AuthenticationException
    {
        // First "update" all forced files to current revisions
        for (Iterator<TeamStatusInfo> i = forceFiles.iterator(); i.hasNext(); ) {
            TeamStatusInfo info = i.next();
            String reposVer = info.getRepositoryVersion();
            if (reposVer != null && reposVer.length() != 0) {
                try {
                    repository.setFileVersion(info.getFile(), reposVer);
                }
                catch (IOException ioe) {
                    throw new CommandException(ioe, "Can't set file version");
                }
            }
        }
        
        return super.doCommand();
    }
}
