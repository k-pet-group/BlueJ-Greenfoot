package bluej.extensions;

import bluej.pkgmgr.PkgMgrFrame;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JMenuItem;
import javax.swing.JMenu;

/**
 * The BlueJ proxy Menu object. This provides a means by which to add menu items to
 * BlueJ, under certain limitations. The object can be
 * got from {@link bluej.extensions.BlueJ#getMenu() getMenu}, or create a new
 * one to create submenus.
 *
 * @author Clive Miller
 * @version $Id: BMenu.java 1459 2002-10-23 12:13:12Z jckm $
 */
public class BMenu extends BMenuItem
{
    private final Collection items; // of BMenuItem

    /**
     * Create a new BMenu, in order to create a submenu effect
     * @param text the text to appear on the menu
     */
    public BMenu (String text)
    {
        super (text);
        this.items = new ArrayList();
    }
    
    /**
     * Request that a menu item be added to this menu.
     * @param item The menu item to be added to this menu. This could be one of: <ul>
     * <li>a <code>BMenuItem</code>,
     * <li>a <code>BMenu</code></ul>
     * @param item the item to add to the menu
     */
    public void addMenuItem (BMenuItem item)
    {
        items.add (item); // Add the new item to our database
        
        // Go through all issued JMenus and add the new child to them
        for (Iterator it=issuedFramesIterator(); it.hasNext(); ) {
            PkgMgrFrame pmf = (PkgMgrFrame)it.next();
            JMenu menu = (JMenu)getIssuedJMenuItem (pmf);
            menu.add (item.getJMenuItem (pmf));
        }
    }
    
    public void removeMenuItems()
    {
        for (Iterator it=items.iterator(); it.hasNext(); ) {
            BMenuItem item = (BMenuItem)it.next();
            item.removeMenuItems();
        }
        items.clear();
        for (Iterator it=issuedFramesIterator(); it.hasNext(); ) {
            PkgMgrFrame pmf = (PkgMgrFrame)it.next();
            JMenu menu = (JMenu)getIssuedJMenuItem (pmf);
            if (menu.getParent() != null) menu.getParent().remove (menu);
        }
    }
        
    JMenuItem createJMenuItem (PkgMgrFrame pmf)
    {
        JMenu menu = new JMenu (getText());
        for (Iterator it=items.iterator(); it.hasNext(); ) {
            BMenuItem item = (BMenuItem)it.next();
            menu.add (item.getJMenuItem (pmf));
        }
        return menu;
    }
}