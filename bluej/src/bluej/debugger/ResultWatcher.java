package bluej.debugger;

import bluej.testmgr.*;

/**
 * Debugger interface implemented by classes interested in the result of an invocation
 *
 * @author  Michael Cahill
 * @version $Id: ResultWatcher.java 1626 2003-02-11 01:46:35Z ajp $
 */
public interface ResultWatcher
{
	/**
	 * An invocation has completed - here is the result
	 * If the invocation has a void result (note that is a void type), name == null.
	 * It should be possible for result to be null and name to not be,
	 * though no code currently creates this situation.
	 */
	void putResult(DebuggerObject result, String name, InvokerRecord ir);
	
	/**
	 * An invocation has failed - here is the error message
	 */
	void putError(String message);
}
