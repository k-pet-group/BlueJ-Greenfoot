package bluej.extmgr;

import java.awt.*;
import java.util.*;

import javax.swing.JPopupMenu;

/**
 * Michael: this is work in progress, when it is done it will be formatted and commented appropriately.
 *
 * Author: Damiano Bolla, University of Kent at Canterbury, 2003
 */

public class MenuManager
{
    private ExtensionsManager extMgr;
    private LinkedList menuItems;


    /**
     *Constructor for the MenuManager object
     */
    public MenuManager()
    {
        extMgr = ExtensionsManager.get();
        menuItems = new LinkedList();
    }


    /**
     *  Description of the Method
     *
     * @param  aPopup  Description of the Parameter
     */
    public void revalidate(JPopupMenu aPopup)
    {

        for (Iterator iter = menuItems.iterator(); iter.hasNext(); )
            aPopup.remove((Component) iter.next());

        menuItems = extMgr.getMenuItems(null);

        for (Iterator iter = menuItems.iterator(); iter.hasNext(); )
            aPopup.add((Component) iter.next());

    }

}
