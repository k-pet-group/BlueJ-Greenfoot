package bluej.utility;

/**
 * Implement this if you want to be notified by a ToggleMessageBox
 * whether to show a dialog again.
 * 
 * @author $Author: ajp $
 * @version $Id: ToggleMessageBoxOwner.java 49 1999-04-28 03:01:02Z ajp $
 */
public interface ToggleMessageBoxOwner 
{
    /**
     * @param dialogID used to uniquely identify each dialog
     * @param showAgain true if the dialog is to be shown again
     */
    void showDialogAgain(int dialogID, boolean showAgain);
}
