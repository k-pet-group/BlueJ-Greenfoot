package bluej.debugger;

/**
 ** Debugger interface implemented by classes interested in the result of an invocation
 ** @version $Id: ResultWatcher.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 **/

public interface ResultWatcher
{
	/**
	 ** An invocation has completed - here is the result
	 **/
	public void putResult(DebuggerObject result, String name);
}
