package bluej.groupwork;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
     * Constructor for UpdateServerResponse.
     * 
     * @param listener  The listener to receive notification of file changes.
     *                  May be null.
     */
    public UpdateServerResponse(UpdateListener listener)
    {
        this.listener = listener;
    }
    
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
        	System.err.println(line);
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
        
        if (e.isTagged())
        {
            line = MessageEvent.parseTaggedMessage(taggedLine, line);
            // if we get back a non-null line, we have something
            // to output. Otherwise, there is more to come and we
            // should do nothing yet.
            if (line == null) {
            	return;
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
	 * @return
	 */
	public int size() {
		return updateResults.size();
	}
    
//    public void fileInfoGenerated(FileInfoEvent arg0)
//    {
        // This doesn't seem to work very well.
//        FileInfoContainer container = arg0.getInfoContainer();
//        
//        if (container instanceof DefaultFileInfoContainer) {
//            DefaultFileInfoContainer dfic = (DefaultFileInfoContainer) container;
//            String type = dfic.getType();
//            
//            if (! type.equals("?")) {
//                System.out.println("UpdateServerResponse, fileInfoEvent: ");
//                System.out.println("   File = " + dfic.getFile());
//                System.out.println("   Type = " + dfic.getType());
//            }
//        }
        
        // arg0.getInfoContainer().
//    }
}
