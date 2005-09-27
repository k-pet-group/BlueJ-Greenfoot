package bluej.debugmgr;

import bluej.debugger.DebuggerObject;
import bluej.testmgr.record.*;

/**
 * Debugger interface implemented by classes interested in the result of an invocation.
 * All methods should be called on the GUI thread.
 *
 * @author  Michael Kolling
 * @author  Poul Henriksen
 * @version $Id: ResultWatcher.java 3590 2005-09-27 04:33:52Z davmac $
 */
public interface ResultWatcher
{
	/**
	 * An invocation has completed - here is the result.
     * 
     * @param result   The invocation result object (null for a void result).
     * @param name     The name of the result. For a constructed object, this
     *                 is the name supplied by the user. Otherwise this is  the
     *                 literal "result", or null if the result is void type.
     * @param ir       The record for the completed invocation
	 */
	void putResult(DebuggerObject result, String name, InvokerRecord ir);
	
	/**
	 * An invocation has failed (compilation error) - here is the error message.
	 */
	void putError(String message);
	
    /**
     * A runtime exception occurred - here is the exception text
     */
    void putException(String message);
}
