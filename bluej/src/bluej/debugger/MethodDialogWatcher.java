package bluej.debugger;

/**
 ** @version $Id: MethodDialogWatcher.java 49 1999-04-28 03:01:02Z ajp $
 ** @author Justin Tan
 ** Interface implemented by classes interested in the MethodDialog
 **/

public interface MethodDialogWatcher
{
	void methodDialogEvent(MethodDialog dlg, int event);
}
