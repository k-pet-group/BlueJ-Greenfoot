package bluej.debugger;

/**
 ** Debugger interface implemented by classes interested in the result of an invocation
 ** @version $Id: ResultWatcher.java 49 1999-04-28 03:01:02Z ajp $
 ** @author Michael Cahill
 **/

public interface ResultWatcher
{
	/**
	 ** An invocation has completed - here is the result
	 **/
	void putResult(DebuggerObject result, String name);
}
