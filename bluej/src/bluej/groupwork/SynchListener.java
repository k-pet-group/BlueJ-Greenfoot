package bluej.groupwork;

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
public class SynchListener implements CVSListener {

	boolean isTerminated = false;
	/* (non-Javadoc)
	 * @see org.netbeans.lib.cvsclient.event.CVSListener#messageSent(org.netbeans.lib.cvsclient.event.MessageEvent)
	 */
	public void messageSent(MessageEvent arg0) {
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
	public void commandTerminated(TerminationEvent arg0) {
		isTerminated = true;
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

}
