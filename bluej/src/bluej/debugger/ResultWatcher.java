package bluej.debugger;

/**
 * Debugger interface implemented by classes interested in the result of an invocation
 *
 * @author  Michael Cahill
 * @version $Id: ResultWatcher.java 505 2000-05-24 05:44:24Z ajp $
 */
public interface ResultWatcher
{
	/**
	 * An invocation has completed - here is the result
	 */
	void putResult(DebuggerObject result, String name);
}
