package bluej.debugmgr;

/**
 * Interface implemented by classes interested in the MethodDialog
 *
 * @author  Michael Kolling
 * @version $Id: CallDialogWatcher.java 2032 2003-06-12 05:04:28Z ajp $
 */
public interface CallDialogWatcher
{
	void callDialogEvent(CallDialog dlg, int event);
}
