package bluej.debugmgr;

import bluej.debugger.DebuggerObject;
import bluej.testmgr.record.*;

/**
 * Debugger interface implemented by classes interested in the result of an invocation
 *
 * @author  Michael Kšlling
 * @author  Poul Henriksen
 * @version $Id: ResultWatcher.java 2612 2004-06-14 20:36:28Z mik $
 */
public interface ResultWatcher
{
	/**
	 * An invocation has completed - here is the result.
	 * If the invocation has a void result (note that is a void type), name == null.
	 * It should be possible for result to be null and name to not be,
	 * though no code currently creates this situation.
	 */
	void putResult(DebuggerObject result, String name, InvokerRecord ir);
	
	/**
	 * An invocation has failed - here is the error message
	 */
	void putError(String message);
	
	/**
	 * A watcher shuold be able to return information about the result that it
	 * is watching. T is used to display extra information (about the expression
	 * that gave the result) when the result is shown.
	 * 
	 * @return An object with information on the expression
	 */
	public ExpressionInformation getExpressionInformation();
}
