package bluej.browser;

import javax.swing.*;
import java.awt.event.*; // ActionListener, etc

import bluej.Config;
import bluej.pkgmgr.LibraryBrowserPkgMgrFrame;

/**
 * A basic JPopupMenu subclass which is attached to LibraryNode objects
 * in the library chooser tree.
 * 
 * @see LibraryNode
 * @see LibaryChooser
 * @author $Author: mik $
 * @version $Id: LibraryPopupMenu.java 36 1999-04-27 04:04:54Z mik $
 */
public class LibraryPopupMenu extends JPopupMenu {
    public static final String OPENCOMMAND = Config.getString("browser.librarychooser.popup.open");
    protected  JMenuItem openMI = new JMenuItem(OPENCOMMAND);
    public static final String USECOMMAND = Config.getString("browser.librarychooser.popup.use");
    protected  JMenuItem useMI = new JMenuItem(USECOMMAND);
    public static final String EXPANDCOMMAND = Config.getString("browser.librarychooser.popup.expand");
    protected  JMenuItem expandMI = new JMenuItem(EXPANDCOMMAND);
    public static final String CONTRACTCOMMAND = Config.getString("browser.librarychooser.popup.collapse");
    protected  JMenuItem contractMI = new JMenuItem(CONTRACTCOMMAND);
    public static final String RENAMECOMMAND = Config.getString("browser.librarychooser.popup.rename");
    protected  JMenuItem renameMI = new JMenuItem(RENAMECOMMAND);
    public static final String REMOVECOMMAND = Config.getString("browser.librarychooser.popup.delete");
    protected  JMenuItem removeMI = new JMenuItem(REMOVECOMMAND);
    public static final String PROPCOMMAND = Config.getString("browser.librarychooser.popup.properties");
    protected  JMenuItem propMI = new JMenuItem(PROPCOMMAND);

    /**
     * Create a new LibraryPopupMenu.
     */
    public LibraryPopupMenu() {}
	
    /**
     * Create a new LibraryPopupMenu with the specified listener.
     * 
     * @param listener the object responsible for handling this menu's actions
     */
    public LibraryPopupMenu(ActionListener listener) {
	add(useMI);
	useMI.addActionListener(listener);
	useMI.setActionCommand(USECOMMAND);
	add(openMI);
	openMI.addActionListener(listener);
	openMI.setActionCommand(OPENCOMMAND);
	addSeparator();
	add(expandMI);
	expandMI.addActionListener(listener);
	expandMI.setActionCommand(EXPANDCOMMAND);
	add(contractMI);
	contractMI.addActionListener(listener);
	contractMI.setActionCommand(CONTRACTCOMMAND);
	addSeparator();
	add(renameMI);
	renameMI.addActionListener(listener);
	renameMI.setActionCommand(RENAMECOMMAND);
	renameMI.setEnabled(false);
	add(removeMI);
	removeMI.addActionListener(listener);
	removeMI.setActionCommand(REMOVECOMMAND);
	removeMI.setEnabled(false);
	addSeparator();
	add(propMI);
	propMI.addActionListener(listener);
	propMI.setActionCommand(PROPCOMMAND);
	propMI.setEnabled(true);
		
	setVisible(false);
    }

    /**
     * Configure the menu according to the state of the LibraryNode it is
     * attached to.
     * 
     * @param isExpanded true if the node is expanded.
     * @param isCollapsed true if the node is collapsed.
     */
    public void configure(LibraryNode node, boolean isExpanded, 
			  boolean isCollapsed) 
    {
	if (LibraryBrowserPkgMgrFrame.isStandalone)
	    useMI.setEnabled(false);
		
	if (node.isLeaf()) {
	    expandMI.setEnabled(false);
	    contractMI.setEnabled(false);
			
	    // no other conditions are applicable
	    return;
	}
		
	if (isExpanded)
	    contractMI.setEnabled(true);
	else
	    contractMI.setEnabled(false);
		
	if (isCollapsed)
	    expandMI.setEnabled(true);
	else
	    expandMI.setEnabled(false);
    }
}
