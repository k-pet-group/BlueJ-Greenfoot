package bluej.debugger;

/**
 ** @version $Id: MethodDialogWatcher.java 36 1999-04-27 04:04:54Z mik $
 ** @author Justin Tan
 ** Interface implemented by classes interested in the MethodDialog
 **/

public interface MethodDialogWatcher
{
    public void methodDialogEvent(MethodDialog dlg, int event);
}
