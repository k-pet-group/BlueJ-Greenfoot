package bluej.debugmgr;

import bluej.debugger.DebuggerObject;
import bluej.testmgr.record.*;

/**
 * Debugger interface implemented by classes interested in the result of an invocation
 *
 * @author  Michael Kolling
 * @author  Poul Henriksen
 * @version $Id: ResultWatcher.java 3532 2005-08-19 06:01:30Z davmac $
 */
public interface ResultWatcher
{
	/**
	 * An invocation has completed - here is the result. This is called
     * asynchronously (not on the AWT event thread).
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
     * This is called asynchronously.
	 */
	void putError(String message);
	
    /**
     * A runtime exception occurred - here is the exception text
     * This is called asynchronously.
     */
    void putException(String message);
}
