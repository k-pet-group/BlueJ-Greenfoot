package bluej.browser;

import javax.swing.*;
import java.awt.event.*; // ActionListener, etc.

/**
 * A basic LibraryPopupMenu subclass which is attached to UserLibraryNode objects
 * in the library chooser tree.
 * 
 * @see LibraryNode
 * @see LibaryChooser
 * @see LibraryPopupMenu
 * @author $Author: mik $
 * @version $Id: UserLibraryPopupMenu.java 36 1999-04-27 04:04:54Z mik $
 */
public class UserLibraryPopupMenu extends LibraryPopupMenu 
{
    /**
     * Create a new UserLibraryPopupMenu with the specified listener.
     * 
     * @param listener the object responsible for handling this menu's actions
     */
    public UserLibraryPopupMenu(ActionListener listener) 
    {
	super(listener);
	propMI.setEnabled(true);
	openMI.setEnabled(false);
	useMI.setEnabled(false);
	removeMI.setEnabled(true);
	renameMI.setEnabled(true);
    }
}
