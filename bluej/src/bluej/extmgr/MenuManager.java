package bluej.extmgr;

import java.awt.*;
import java.util.*;

import javax.swing.JPopupMenu;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;



/**
 * Manages the menues being added by extensions.
 *
 * Author: Damiano Bolla, University of Kent at Canterbury, 2003
 */

public class MenuManager implements PopupMenuListener

{
    public static int TOOLS_MENU=1;
    public static int CLASS_MENU=2;
    public static int OBJECT_MENU=3;

    private ExtensionsManager extMgr;
    private LinkedList menuItems;
    private JPopupMenu popupMenu;
    private Object attachedObject;
    private int menuType;
    

    /**
     *Constructor for the MenuManager object
     */
    public MenuManager(JPopupMenu aPopupMenu )
    {
        extMgr = ExtensionsManager.get();
        menuItems = new LinkedList();
        popupMenu = aPopupMenu;
        popupMenu.addPopupMenuListener(this);
    }


    /**
     *  Add all the menu currently available to the menu.
     *  Called only once.
     *
     * @param  aPopup  Description of the Parameter
     */
    public void addExtensionMenu ()
    {
        menuItems = extMgr.getMenuItems(null);

        for (Iterator iter = menuItems.iterator(); iter.hasNext(); )
            popupMenu.add((JComponent) iter.next());
    }


    /**
     * Sets the object being attached to this menu.
     * 
     * @param  attachedTo  The new attachedObject value
     */
    public void setAttachedObject(Object attachedTo)
    {
        attachedObject = attachedTo;
    }

    /**
     * Notify to all valid extensions that this menu Item is about to be displayed.
     *
     * @param  event  The associated event
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent event)
    {
        JPopupMenu aPopup = (JPopupMenu) event.getSource();

        for (Iterator iter = menuItems.iterator(); iter.hasNext(); ) 
            {
            JComponent aComponent = (JComponent)iter.next();

            ExtensionWrapper aWrapper = (ExtensionWrapper)aComponent.getClientProperty("bluej.extmgr.ExtensionWrapper");
            if ( aWrapper == null ) 
                continue;  

            if ( ! aWrapper.isValid() ) {
                popupMenu.remove(aComponent);
                iter.remove();
            }
              
            aWrapper.safePostMenuItem(attachedObject,(JMenuItem)aComponent);
        }
    }


    public void popupMenuWillBecomeInvisible(PopupMenuEvent event)
    {
    }

   public void popupMenuCanceled(PopupMenuEvent event)
    {
    }
}
