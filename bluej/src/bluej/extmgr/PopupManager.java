package bluej.extmgr;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

/**
 * Michael: this is work in progress, when it is done it will be formatted and commented appropriately.
 *
 * This class will manage dynamic popup menues for extensions.
 * The basic idea is simple, add menu items only when the user wants them.
 * We do this for the following reason.
 * 1) No more need to keep track if and when an extension is gone.
 * 2) Extension writers have a finer control on how to display the menu items.
 * 3) No more problems with syncronization between user and swing thread (all is swing)
 * 4) The whole system is much simpler.
 * 
 * For an example of how attache them to a normal menu search for PopupManager into 
 * the source tree.
 * This class will be created each time a new popup menu is created.
 * NOTA: A popup menu is not the same thing as a popup menu item.
 * When you create this class you bind it to the object it is referred to.
 * Damiano
 *
 * Author: Damiano Bolla, University of Kent at Canterbury, 2003
 */

public class PopupManager implements PopupMenuListener
{
    private ExtensionsManager extMgr;
    private Object attachedObject;
    private LinkedList menuItems;


    /**
     * Constructor for the PopupManager object
     *
     * @param  attachedTo  The object this popup is attached to.
     */
    public PopupManager(Object attachedTo)
    {
//    System.out.println ("PopupManager attachedTo="+attachedTo);
        extMgr = ExtensionsManager.get();
        attachedObject = attachedTo;
    }


    /**
     * For long lived PopupManagers it may happens that the object they are attached to
     * changes during the lifetime.
     *
     * @param  attachedTo  The new attachedObject value
     */
    public void setAttachedObject(Object attachedTo)
    {
//    System.out.println ("PopupManager.setAttachedObject attachedTo="+attachedTo);
        attachedObject = attachedTo;
    }


    /**
     * Called just before the PopupMenu becomes visible.
     * This callback is most likely what swing authors thought you need to make
     * dynamic menues. It fits the framework perfectly.
     * The cast of getSource() to JPopupMenu is safe since it must be a PopupMenu
     *
     * @param  event  The event being posted by swing
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent event)
    {
//    System.out.println ("Before will");
        JPopupMenu aPopup = (JPopupMenu) event.getSource();

        // Let me get all menues that should be shown
        menuItems = extMgr.getMenuItems(attachedObject);

        for (Iterator iter = menuItems.iterator(); iter.hasNext(); )
            aPopup.add((Component) iter.next());
    }


    /**
     * Called when the menu is going to go away. Simply get rid of all the
     * items just added.
     *
     * @param  event  The event being posted by swing
     */
    public void popupMenuWillBecomeInvisible(PopupMenuEvent event)
    {
//    System.out.println ("Become Invisible");
        JPopupMenu aPopup = (JPopupMenu) event.getSource();

        for (Iterator iter = menuItems.iterator(); iter.hasNext(); )
            aPopup.remove((Component) iter.next());
    }


    /**
     * Need to do some more research on what this is for..
     *
     * @param  event  The event being posted by swing
     */
    public void popupMenuCanceled(PopupMenuEvent event)
    {
//    System.out.println ("Cancel");
        JPopupMenu aPopup = (JPopupMenu) event.getSource();

        for (Iterator iter = menuItems.iterator(); iter.hasNext(); )
            aPopup.remove((Component) iter.next());
    }

}
