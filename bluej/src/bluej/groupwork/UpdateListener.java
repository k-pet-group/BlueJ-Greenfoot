package bluej.groupwork;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.netbeans.lib.cvsclient.event.CVSAdapter;
import org.netbeans.lib.cvsclient.event.MessageEvent;
import org.netbeans.lib.cvsclient.event.TerminationEvent;

/**
 * This class can be registred as a listener when doing an update. Registering
 * could look like this <br/>
 * <em> 
 * UpdateListener updateListener = new UpdateListener();
 * client.getEventManager().addCVSListener(updateListener);
 * <em/>
 * When the UpdateCommand has been executed, this listener will have build a 
 * list of UpdateResults that can be accessed using getUpdateResults()
 * 
 * @author fisker
 * 
 */
public class UpdateListener extends BasicServerResponse{
	/**
     * Stores a tagged line
     */
    private final StringBuffer taggedLine = new StringBuffer();
    
    /**
     * Stores the UpdateResults
     */
    private List updateResults = new LinkedList();
    /**
     * Called when the server wants to send a message to be displayed to
     * the user. The message is only for information purposes and clients
     * can choose to ignore these messages if they wish.
     * @param e the event
     */
    public void messageSent(MessageEvent e)
    {
    	//System.out.println("messageSent: " + e.getMessage());
        String line = e.getMessage();
        PrintStream stream = e.isError() ? System.err
                                         : System.out;

        if (e.isTagged())
        {
            String message = MessageEvent.parseTaggedMessage(taggedLine, line);
	    // if we get back a non-null line, we have something
	    // to output. Otherwise, there is more to come and we
	    // should do nothing yet.
            if (message != null)
            {
            	e.setMessage(message);
            }
        }
        try {
			UpdateResult updateResult = UpdateResult.parse(e.getMessage());
			//System.out.println("added: " + updateResult.toString());
			updateResults.add(updateResult);
		} catch (UnableToParseInputException e1) {
			//e1.printStackTrace();
		}
		
    }
    
    public void commandTerminated(TerminationEvent e){
    	isTerminated = true;
    	synchronized(this){
    		notifyAll();
    	}
    }
    
    
    
    private List getUpdateResultsOfType(char type){
    	List results = new LinkedList();
    	for (Iterator i = updateResults.iterator(); i.hasNext();) {
			UpdateResult updateResult = (UpdateResult) i.next();
			if (updateResult.getStatusCode()== type){
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
    public List getUpdateResults(){
    	waitForExecutionToFinish();
    	return updateResults;
    }
   
    /**
     * Get a list of UpdateResults that represents conflicts. 
     * This method will block until the UpdateCommand we are listening for has
     * terminated.
     * @return List of UpdateResults which represents conflicts
     */
    public List getConflicts(){
    	waitForExecutionToFinish();
    	return getUpdateResultsOfType('C');
    }
    
    /**
     * Get a list of UpdateResults that represents unknowns. 
     * This method will block until the UpdateCommand we are listening for has
     * terminated.
     * @return List of UpdateResults which represents unknowns
     */
    public List getUnknown(){
    	waitForExecutionToFinish();
    	return getUpdateResultsOfType('?');
    }
    
    /**
     * Get a list of UpdateResults that represents updates. 
     * This method will block until the UpdateCommand we are listening for has
     * terminated.
     * @return List of UpdateResults which represents updates
     */
    public List getUpdated(){
    	waitForExecutionToFinish();
    	return getUpdateResultsOfType('U');
    }

	/**
	 * @return
	 */
	public int size() {
		return updateResults.size();
	}
    
    
}
