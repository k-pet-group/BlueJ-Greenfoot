package bluej.groupwork;

import java.io.PrintStream;

import org.netbeans.lib.cvsclient.event.CVSListener;
import org.netbeans.lib.cvsclient.event.FileAddedEvent;
import org.netbeans.lib.cvsclient.event.FileInfoEvent;
import org.netbeans.lib.cvsclient.event.FileRemovedEvent;
import org.netbeans.lib.cvsclient.event.FileUpdatedEvent;
import org.netbeans.lib.cvsclient.event.MessageEvent;
import org.netbeans.lib.cvsclient.event.ModuleExpansionEvent;
import org.netbeans.lib.cvsclient.event.TerminationEvent;

/**
 * @author fisker
 *
 */
public class BasicServerResponse implements CVSListener {

	/**
     * Stores a tagged line
     */
    private final StringBuffer taggedLine = new StringBuffer();
    private StringBuffer message = new StringBuffer();
	boolean isTerminated = false;
	private TerminationEvent terminationEvent;
	private String newline = System.getProperty("line.separator");
	
	/* (non-Javadoc)
	 * @see org.netbeans.lib.cvsclient.event.CVSListener#messageSent(org.netbeans.lib.cvsclient.event.MessageEvent)
	 */
	public void messageSent(MessageEvent e) {
		String line = e.getMessage();
        PrintStream stream = e.isError() ? System.err : System.out;

        if (e.isTagged())
        {
            String message = MessageEvent.parseTaggedMessage(taggedLine, line);
	    // if we get back a non-null line, we have something
	    // to output. Otherwise, there is more to come and we
	    // should do nothing yet.
            if (message != null)
            {
                //stream.println("tagged:" + message);
            }
        }
        else
        {
        	message.append(line + newline);
            //stream.println("nontagged: " + line);
        }
	}

	/* (non-Javadoc)
	 * @see org.netbeans.lib.cvsclient.event.CVSListener#fileAdded(org.netbeans.lib.cvsclient.event.FileAddedEvent)
	 */
	public void fileAdded(FileAddedEvent arg0) {
	}

	/* (non-Javadoc)
	 * @see org.netbeans.lib.cvsclient.event.CVSListener#fileRemoved(org.netbeans.lib.cvsclient.event.FileRemovedEvent)
	 */
	public void fileRemoved(FileRemovedEvent arg0) {
	}

	/* (non-Javadoc)
	 * @see org.netbeans.lib.cvsclient.event.CVSListener#fileUpdated(org.netbeans.lib.cvsclient.event.FileUpdatedEvent)
	 */
	public void fileUpdated(FileUpdatedEvent arg0) {
	}

	/* (non-Javadoc)
	 * @see org.netbeans.lib.cvsclient.event.CVSListener#fileInfoGenerated(org.netbeans.lib.cvsclient.event.FileInfoEvent)
	 */
	public void fileInfoGenerated(FileInfoEvent arg0) {
	}

	/* (non-Javadoc)
	 * @see org.netbeans.lib.cvsclient.event.CVSListener#commandTerminated(org.netbeans.lib.cvsclient.event.TerminationEvent)
	 */
	public void commandTerminated(TerminationEvent terminationEvent) {
		isTerminated = true;
		this.terminationEvent = terminationEvent;
		synchronized(this){
    		notifyAll();
    	}
	}

	
	public void waitForExecutionToFinish(){
		while(!isTerminated){
    		try {
				wait();
			} catch (InterruptedException e) {}
    	}
	}
	/* (non-Javadoc)
	 * @see org.netbeans.lib.cvsclient.event.CVSListener#moduleExpanded(org.netbeans.lib.cvsclient.event.ModuleExpansionEvent)
	 */
	public void moduleExpanded(ModuleExpansionEvent arg0) {
	}

	/**
	 * @return Returns the terminationEvent.
	 */
	public boolean isError() {
		waitForExecutionToFinish();
		return terminationEvent.isError();
	}
	
	public String getMessage(){
		return message.toString();
	}
}
