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
import java.util.*;

import org.netbeans.lib.cvsclient.event.FileAddedEvent;
import org.netbeans.lib.cvsclient.event.FileRemovedEvent;
import org.netbeans.lib.cvsclient.event.FileUpdatedEvent;
import org.netbeans.lib.cvsclient.event.MessageEvent;

import bluej.groupwork.UnableToParseInputException;
import bluej.groupwork.UpdateListener;
import bluej.groupwork.UpdateResults;


/**
 * This class can be registred as a listener when doing an update. Registering
 * could look like this:<p>
 * 
 * <code> 
 * UpdateServerResponse updateServerResponse = new UpdateServerResponse();<br>
 * client.getEventManager().addCVSListener(updateServerResponse);<br>
 * </code>
 * 
 * <p>When the UpdateCommand has been executed, this listener will have build a 
 * list of UpdateResults that can be accessed using getUpdateResults()
 * 
 * @author fisker
 * 
 */
public class UpdateServerResponse extends BasicServerResponse implements UpdateResults
{
    /**
     * Stores a tagged line
     */
    private final StringBuffer taggedLine = new StringBuffer();
    
    /**
     * Stores the UpdateResults
     */
    private List<CvsUpdateResult> updateResults = new LinkedList<CvsUpdateResult>();
    
    /**
     * Stores the names of new directories which were discovered during update
     */
    private List<String> newDirectoryNames = new LinkedList<String>();
    
    /** Listener to receive notifications of file changes */
    private UpdateListener listener;
    
    /**
     * Files which had conflicts, and for which a decision must be made:
     * Keep the local version, or the repository version. Map File to File
     * (original name, backup name)
     */
    private Map<File,File> conflictsMap;
    
    private BlueJCvsClient client;
    
    /**
     * Constructor for UpdateServerResponse.
     * 
     * @param listener  The listener to receive notification of file changes.
     *                  May be null.
     */
    public UpdateServerResponse(UpdateListener listener, BlueJCvsClient client)
    {
        this.listener = listener;
        this.client = client;
    }
    
    /*
     * Example output from server:
     * 
     * cvs update: Updating .
     * cvs update: New directory 'abcdef' -- ignored 
     * cvs update: nonmergeable file needs merge
     * cvs update: revision 1.7 from repository is now in bluej.pkg
     * cvs update: file from working directory is now in .#bluej.pkg.1.6
     * cvs update: Updating +libs
     * cvs update: Updating Examples
     * 
     * If a file is deleted in repository as well as locally:
     * cvs update: warning: Examples/bluej.pkg is not (any longer) pertinent
     */
    
    /* CVS: RCS file: /home/cvsroot/simple/Alakazam.java,v
       CVS: retrieving revision 1.6
       CVS: retrieving revision 1.7
       CVS: Merging differences between 1.6 and 1.7 into Alakazam.java
       renameLocalFile, /home/davmac/bluej/examples/simple/Alakazam.java, .#Alakazam.java.1.6
       CVS: cvs update: Updating .
       CVS: rcsmerge: warning: conflicts during merge
       CVS: CVS: cvs update: conflicts found in Alakazam.java
       CVS: cvs update: Updating +libs
       CVS tagged: text C 
       CVS tagged: fname Alakazam.java
       CVS tagged: newline
       CVS: C Alakazam.java
       CVS tagged: text M 
       CVS tagged: fname bluej.pkg
       CVS tagged: newline
       CVS: M bluej.pkg
       CVS: cvs update: Updating Examples */

    
    
    /**
     * Called when the server wants to send a message to be displayed to
     * the user. The message is only for information purposes and clients
     * can choose to ignore these messages if they wish.
     * @param e the event
     */
    public void messageSent(MessageEvent e)
    {
        String line = e.getMessage();
        //System.out.println("UpdateServerResponse parsed: " + e.getMessage() + 
        //      " isTagged: " + e.isTagged()    );

        if (e.isTagged())
        {
            // System.out.println("CVS tagged: " + e.getMessage());
            line = MessageEvent.parseTaggedMessage(taggedLine, line);
            // if we get back a non-null line, we have something
            // to output. Otherwise, there is more to come and we
            // should do nothing yet.
            if (line == null) {
                return;
            }
        }
        
        if (e.isError()) {
            // System.out.println("CVS err: " + line);
            int offset = 27;
            if (line.startsWith("cvs update: New directory")
                    || line.startsWith("cvs server: New directory")
                    || (offset = 29) != 0 && line.startsWith("cvs checkout: New directory")) {
                // Sheesh, CVS is really stoopid. When doing "cvs -n update",
                // it won't recurse into new directories (i.e. directories
                // which weren't previously known to the client) no matter
                // what. At least it gives us this message, so we can record
                // the directory name and re-stat it later.
                int n = line.lastIndexOf("-- ignored");
                if (n != -1) {
                    String dirName = line.substring(offset, n - 2);
                    newDirectoryNames.add(dirName);
                }
            }
        }
        else {
            // System.out.println("CVS: " + line);
            if (line.startsWith("Merging differences between ")) {
                // We get this when a textual merge is performed.
                if (client != null) {
                    client.nextConflictNonBinary();
                }
            }
        }

        try {
            CvsUpdateResult updateResult = CvsUpdateResult.parse(line);
            updateResults.add(updateResult);
        } catch (UnableToParseInputException e1) {
            //e1.printStackTrace();
        }
    }
    
    public void fileUpdated(FileUpdatedEvent arg0)
    {
        if (listener != null) {
            String filePath = arg0.getFilePath();
            listener.fileUpdated(new File(filePath));
        }
    }
    
    public void fileAdded(FileAddedEvent arg0)
    {
        if (listener != null) {
            String filePath = arg0.getFilePath();
            listener.fileAdded(new File(filePath));
        }
    }
    
    public void fileRemoved(FileRemovedEvent arg0)
    {
        if (listener != null) {
            String filePath = arg0.getFilePath();
            listener.fileRemoved(new File(filePath));
        }
    }
    
    private List<CvsUpdateResult> getUpdateResultsOfType(char type)
    {
        List<CvsUpdateResult> results = new LinkedList<CvsUpdateResult>();
        for (Iterator<CvsUpdateResult> i = updateResults.iterator(); i.hasNext();) {
            CvsUpdateResult updateResult = i.next();
            if (updateResult.getStatusCode() == type) {
                results.add(updateResult);
            }
        }
        return results;
    }
    
    /* (non-Javadoc)
     * @see bluej.groupwork.UpdateResults#getConflicts()
     */
    public List<File> getConflicts()
    {
        waitForExecutionToFinish();
        List<CvsUpdateResult> curList = getUpdateResultsOfType(CvsUpdateResult.CONFLICT);
        List<File> fileList = new ArrayList<File>(curList.size());
        for (Iterator<CvsUpdateResult> i = curList.iterator(); i.hasNext(); ) {
            CvsUpdateResult result = i.next();
            fileList.add(new File(client.getLocalPath(), result.getFilename()));
        }
        return fileList;
    }
    
    /**
     * Get a list of UpdateResults that represents updates. 
     * This method will block until the UpdateCommand we are listening for has
     * terminated.
     * @return List of UpdateResults which represents updates
     */
    public List<CvsUpdateResult> getUpdated()
    {
        waitForExecutionToFinish();
        return getUpdateResultsOfType(CvsUpdateResult.UPDATED);
    }
    
    /**
     * Get a list of directory names discovered during cvs -n update.
     * 
     * @return  the directory names (List of String)
     */
    public List<String> getNewDirectoryNames()
    {
        return newDirectoryNames;
    }
    
    /**
     * Set the conflict map, which maps the files which had binary conflicts
     * to the backup name assigned by CVS.
     * 
     * @param m The map (File to File). The key is the original file name
     *          (the repository version of the file) and the value is the
     *          backup name (the local version of the file).
     */
    public void setConflictMap(Map<File,File> m)
    {
        conflictsMap = m;
    }
    
    /**
     * Get the set of files which had binary conflicts. These are files which
     * have been modified both locally and in the repository. A decision needs to
     * be made about which version (local or repository) is to be retained; use
     * the overrideFiles() method to finalise this decision.
     */
    public Set<File> getBinaryConflicts()
    {
        return conflictsMap.keySet();
    }
    
    /**
     * Once the initial update has finished and the binary conflicts are known,
     * this method must be called to select whether to keep the local or use the
     * remove version of the conflicting files.
     *  
     * @param files  A set of files to fetch from the repository, overwriting the
     *               local version. (For any file not in the set, the local version
     *               is retained). 
     */
    public void overrideFiles(Set<File> files)
    {
        // First delete backups of files which are to be overridden
        for (Iterator<File> i = files.iterator(); i.hasNext(); ) {
            File f = (File) i.next();
            File backupFile = (File) conflictsMap.remove(f);
            if (backupFile != null) {
                backupFile.delete();
            }
        }
        
        // Then, for the other files, rename the backup over the original
        // file
        for (Iterator<Map.Entry<File,File>> i = conflictsMap.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<File,File> entry = i.next();
            File f = entry.getKey();
            File backupFile = entry.getValue();
            f.delete();
            backupFile.renameTo(f);
        }
    }
}
