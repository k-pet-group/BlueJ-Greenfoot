package bluej.groupwork;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.netbeans.lib.cvsclient.event.FileAddedEvent;
import org.netbeans.lib.cvsclient.event.FileRemovedEvent;
import org.netbeans.lib.cvsclient.event.FileUpdatedEvent;
import org.netbeans.lib.cvsclient.event.MessageEvent;


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
public class UpdateServerResponse extends BasicServerResponse
{
	/**
     * Stores a tagged line
     */
    private final StringBuffer taggedLine = new StringBuffer();
    
    /**
     * Stores the UpdateResults
     */
    private List updateResults = new LinkedList();
    
    /**
     * Stores the names of new directories which were discovered during update
     */
    private List newDirectoryNames = new LinkedList();
    
    /** Listener to receive notifications of file changes */
    private UpdateListener listener;
    
    /**
     * Files which had conflicts, and for which a decision must be made:
     * Keep the local version, or the repository version. Map File to File
     * (original name, backup name)
     */
    private Map conflictsMap;
    
    /**
     * Constructor for UpdateServerResponse.
     * 
     * @param listener  The listener to receive notification of file changes.
     *                  May be null.
     */
    public UpdateServerResponse(UpdateListener listener)
    {
        this.listener = listener;
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
        //		" isTagged: " + e.isTagged()	);

        if (e.isError()){
        	System.err.println("CVS: " + line);
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
            //if (! e.isTagged()) {
            //    System.out.println("CVS: " + line);
            //}
        }
        
        if (e.isTagged())
        {
            line = MessageEvent.parseTaggedMessage(taggedLine, line);
            // if we get back a non-null line, we have something
            // to output. Otherwise, there is more to come and we
            // should do nothing yet.
            if (line == null) {
            	return;
            }
            else {
                if (e.isError()) {
                    System.err.println("CVS: " + line);
                }
                //else {
                //    System.out.println("CVS: " + line);
                //}
            }
        }
        
        try {
			UpdateResult updateResult = UpdateResult.parse(line);
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
    
    private List getUpdateResultsOfType(char type)
    {
    	List results = new LinkedList();
    	for (Iterator i = updateResults.iterator(); i.hasNext();) {
			UpdateResult updateResult = (UpdateResult) i.next();
			if (updateResult.getStatusCode()== type) {
				results.add(updateResult);
			}
		}
    	return results;
    }
    
    /**
     * Get the list of UpdateResults. This method will block until the 
     * UpdateCommand we are listening for has terminated.
     * @return List of UpdateResults
     */
    public List getUpdateResults()
    {
    	waitForExecutionToFinish();
    	return updateResults;
    }
   
    /**
     * Get a list of UpdateResults that represents conflicts. 
     * This method will block until the UpdateCommand we are listening for has
     * terminated.
     * @return List of UpdateResults which represents conflicts
     */
    public List getConflicts()
    {
    	waitForExecutionToFinish();
    	return getUpdateResultsOfType(UpdateResult.CONFLICT);
    }
    
    /**
     * Get a list of UpdateResults that represents unknowns. 
     * This method will block until the UpdateCommand we are listening for has
     * terminated.
     * @return List of UpdateResults which represents unknowns
     */
    public List getUnknown()
    {
    	waitForExecutionToFinish();
    	return getUpdateResultsOfType(UpdateResult.UNKNOWN);
    }
    
    /**
     * Get a list of UpdateResults that represents updates. 
     * This method will block until the UpdateCommand we are listening for has
     * terminated.
     * @return List of UpdateResults which represents updates
     */
    public List getUpdated()
    {
    	waitForExecutionToFinish();
    	return getUpdateResultsOfType(UpdateResult.UPDATED);
    }
    
    /**
     * Get a list of directory names discovered during cvs -n update.
     * 
     * @return  the directory names (List of String)
     */
    public List getNewDirectoryNames()
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
    public void setConflictMap(Map m)
    {
        conflictsMap = m;
    }
    
    /**
     * Get the set of files which had binary conflicts. These are files which
     * have been modified both locally and in the repository. A decision needs to
     * be made about which version (local or repository) is to be retained; use
     * the overrideFiles() method to finalise this decision.
     */
    public Set getBinaryConflicts()
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
    public BasicServerResponse overrideFiles(Set files)
    {
        // First delete backups of files which are to be overridden
        for (Iterator i = files.iterator(); i.hasNext(); ) {
            File f = (File) i.next();
            File backupFile = (File) conflictsMap.remove(f);
            if (backupFile != null) {
                backupFile.delete();
            }
        }
        
        // Then, for the other files, rename the backup over the original
        // file
        for (Iterator i = conflictsMap.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry) i.next();
            File f = (File) entry.getKey();
            File backupFile = (File) entry.getValue();
            f.delete();
            backupFile.renameTo(f);
        }
        
        // For CVS, no communication with the server was really necessary,
        // because the files were already downloaded. Use a dummy result.
        BasicServerResponse result = new BasicServerResponse();
        result.commandTerminated(null);
        return result;
    }
}
