package bluej.browser;

import javax.swing.*;
import java.awt.event.*; // ActionListener, etc.

/**
 * A basic LibraryPopupMenu subclass which is attached to SystemLibraryNode objects
 * in the library chooser tree.
 * 
 * @see LibraryNode
 * @see LibaryChooser
 * @see LibraryPopupMenu
 * @author $Author: mik $
 * @version $Id: SystemLibraryPopupMenu.java 36 1999-04-27 04:04:54Z mik $
 */
public class SystemLibraryPopupMenu extends LibraryPopupMenu {
    /**
     * Create a new SystemLibraryPopupMenu with the specified listener.
     * 
     * @param listener the object responsible for handling this menu's actions
     */
    public SystemLibraryPopupMenu(ActionListener listener) 
    {
	super(listener);
	useMI.setEnabled(false);
	openMI.setEnabled(false);
	renameMI.setEnabled(false);
	removeMI.setEnabled(false);
    }
	
}
