package bluej.debugger;

/**
 * Interface implemented by classes interested in the MethodDialog
 *
 * @author  Michael Kolling
 * @version $Id: CallDialogWatcher.java 1371 2002-10-14 08:26:48Z mik $
 */
public interface CallDialogWatcher
{
	void callDialogEvent(CallDialog dlg, int event);
}
