package bluej.debugger;

/**
 * Interface implemented by classes interested in the MethodDialog
 *
 * @author  Michael Kolling
 * @version $Id: CallDialogWatcher.java 1459 2002-10-23 12:13:12Z jckm $
 */
public interface CallDialogWatcher
{
	void callDialogEvent(CallDialog dlg, int event);
}
