package bluej.groupwork.cvsnb;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.groupwork.BlueJCvsClient;

/**
 * Command to commit all local changes to the repository. 
 * 
 * @author Davin McCall
 */
public class CvsCommitAllCommand extends CvsCommand
{
    private Set newFiles;
    private Set binaryNewFiles;
    private Set deletedFiles;
    private Set files;
    private String commitComment;
    
    public CvsCommitAllCommand(CvsRepository repository, Set newFiles, Set binaryNewFiles,
            Set deletedFiles, Set files, String commitComment)
    {
        super(repository);
    }
    
    protected BasicServerResponse doCommand()
        throws CommandAbortedException, CommandException, AuthenticationException
    {
        BlueJCvsClient client = getClient();
        File projectPath = repository.getProjectPath();
        
        // First we need to do "cvs add" to put files and directories
        // under version control if they are not already. Start by building
        // a list of directories to add.
        
        // Note, we need to use a LinkedHashSet to preserve order.
        Set dirs = new LinkedHashSet();
        LinkedList stack = new LinkedList();
        for (Iterator i = newFiles.iterator(); i.hasNext(); ) {
            File file = (File) i.next();
            
            File parent = file.getParentFile();
            while (! repository.isDirectoryUnderCVS(parent) && ! dirs.contains(parent)) {
                stack.addLast(parent);
                if (parent.equals(projectPath)) {
                    break;
                }
                parent = parent.getParentFile();
            }
            while (! stack.isEmpty()) {
                dirs.add(stack.removeLast());
            }
        }
        
        // The list of directories must include those containing binary files
        for (Iterator i = binaryNewFiles.iterator(); i.hasNext(); ) {
            File file = (File) i.next();
            
            File parent = file.getParentFile();
            while (! repository.isDirectoryUnderCVS(parent) && ! dirs.contains(parent)) {
                stack.addLast(parent);
                if (parent.equals(projectPath)) {
                    break;
                }
                parent = parent.getParentFile();
            }
            while (! stack.isEmpty()) {
                dirs.add(stack.removeLast());
            }
        }
        
        // we also add the files which need to be added
        dirs.addAll(newFiles);
        
        // "cvs remove" files which need to be removed
        BasicServerResponse basicServerResponse =
            repository.removeFromRepository(client, deletedFiles);
        if (basicServerResponse.isError()) {
            return basicServerResponse;
        }
        
        client = getClient();
        
        // "cvs add" new directories and text files
        basicServerResponse = repository.addToRepository(client, listToFileArray(dirs), false);
        if (basicServerResponse.isError()) {
            return basicServerResponse;
        }
        
        client = getClient();
        
        // add the binary files
        basicServerResponse = repository.addToRepository(client, listToFileArray(binaryNewFiles), true);
        if (basicServerResponse.isError()) {
            return basicServerResponse;
        }

        client = getClient();
        
        // Now perform the commit.
        basicServerResponse = repository.commitToRepository(client, files, commitComment);
        return basicServerResponse;
    }

    /**
     * Convert a List of Files to an array of Files.
     *
     * @param fileList
     */
    private static File[] listToFileArray(Collection fileList)
    {
        File[] files = new File[fileList.size()];
        int j = 0;

        for (Iterator i = fileList.iterator(); i.hasNext();) {
            File file = (File) i.next();
            files[j++] = file;
        }

        return files;
    }
}
