package bluej.debugmgr;

import bluej.debugger.DebuggerObject;
import bluej.testmgr.record.*;

/**
 * Debugger interface implemented by classes interested in the result of an invocation
 *
 * @author  Michael Kolling
 * @author  Poul Henriksen
 * @version $Id: ResultWatcher.java 3348 2005-04-15 02:36:36Z davmac $
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
	 * An invocation has failed (compilation error) - here is the error message
	 */
	void putError(String message);
	
    /**
     * A runtime exception occurred - here is the exception text
     */
    void putException(String message);
    
	/**
	 * A watcher shuold be able to return information about the result that it
	 * is watching. T is used to display extra information (about the expression
	 * that gave the result) when the result is shown.
	 * 
	 * @return An object with information on the expression
	 */
	public ExpressionInformation getExpressionInformation();
}
