package bluej.utility;

/**
 * Implement this if you want to be notified by a ToggleMessageBox
 * whether to show a dialog again.
 * 
 * @author $Author: mik $
 * @version $Id: ToggleMessageBoxOwner.java 36 1999-04-27 04:04:54Z mik $
 */
public interface ToggleMessageBoxOwner 
{
    /**
     * @param dialogID used to uniquely identify each dialog
     * @param showAgain true if the dialog is to be shown again
     */
    public void showDialogAgain(int dialogID, boolean showAgain);
}
