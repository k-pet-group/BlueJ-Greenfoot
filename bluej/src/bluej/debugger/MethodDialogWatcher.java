package bluej.debugger;

/**
 * Interface implemented by classes interested in the MethodDialog
 *
 * @author  Justin Tan
 * @version $Id: MethodDialogWatcher.java 291 1999-11-30 06:24:36Z ajp $
 */
public interface MethodDialogWatcher
{
	void methodDialogEvent(MethodDialog dlg, int event);
}
