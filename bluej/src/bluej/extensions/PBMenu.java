 package bluej.extensions;

import bluej.extmgr.ExtensionWrapper;

/**
 * The BlueJ proxy Menu object. This provides a means by which to add menu items to
 * BlueJ, under certain limitations. The object can be
 * got from {@link bluej.extensions.BlueJ#getMenu() getMenu}, or create a new
 * one to create submenus.
 *
 * @author Clive Miller
 * @version $Id: PBMenu.java 1459 2002-10-23 12:13:12Z jckm $
 */
class PBMenu extends BMenu
{
    private final ExtensionWrapper ew;
    
    /**
     * Create a new PBMenu
     */
    public PBMenu (ExtensionWrapper ew)
    {
        super (null);
        this.ew = ew;
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
        ew.registerMenuItem (item);
    }
}