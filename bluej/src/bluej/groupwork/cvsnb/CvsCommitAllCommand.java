/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
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


/**
 * Command to commit all local changes to the repository. 
 * 
 * @author Davin McCall
 */
public class CvsCommitAllCommand extends CvsCommand
{
    private Set<File> newFiles;
    private Set<File> binaryNewFiles;
    private Set<File> deletedFiles;
    private Set<File> files;
    private String commitComment;
    
    public CvsCommitAllCommand(CvsRepository repository, Set<File> newFiles, Set<File> binaryNewFiles,
            Set<File> deletedFiles, Set<File> files, String commitComment)
    {
        super(repository);
        this.newFiles = newFiles;
        this.binaryNewFiles = binaryNewFiles;
        this.deletedFiles = deletedFiles;
        this.files = files;
        this.commitComment = commitComment;
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
        Set<File> dirs = new LinkedHashSet<File>();
        LinkedList<File> stack = new LinkedList<File>();
        for (Iterator<File> i = newFiles.iterator(); i.hasNext(); ) {
            File file = i.next();
            
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
        for (Iterator<File> i = binaryNewFiles.iterator(); i.hasNext(); ) {
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
    private static File[] listToFileArray(Collection<? extends File> fileList)
    {
        return fileList.toArray(new File[fileList.size()]);
    }
}
