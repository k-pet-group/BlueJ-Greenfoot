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
import bluej.pkgmgr.*;


/**
 * Manages the menues being added by extensions.
 *
 * Author: Damiano Bolla, University of Kent at Canterbury, 2003
 */

public class MenuManager implements PopupMenuListener
{
    private ExtensionsManager extMgr;
    private JPopupMenu popupMenu;
    private Object attachedObject;
    private JPopupMenu.Separator menuSeparator;


    /**
     *Constructor for the MenuManager object
     *
     * @param  aPopupMenu  Description of the Parameter
     */
    public MenuManager(JPopupMenu aPopupMenu)
    {
        extMgr = ExtensionsManager.get();
        popupMenu = aPopupMenu;
        popupMenu.addPopupMenuListener(this);
    }


    /**
     * Add all the menu currently available to the menu.
     * This may be called more than once.
     *
     * @param  onThisProject  The feature to be added to the ExtensionMenu attribute
     */
    public void addExtensionMenu(Project onThisProject)
    {
        // Get all menus that can be possibly be generated now.
        LinkedList menuItems = extMgr.getMenuItems(attachedObject, onThisProject);

        if (menuItems.isEmpty())
            return;

        // What I need to do now is to make shure that menus I am adding are only the new ones.
        MenuElement[] elements = popupMenu.getSubElements();
        for (int index = 0; index < elements.length; index++) {
            // For all menus already generated I have to skip the ones already present
            JComponent aComponent = (JComponent) elements[index].getComponent();
            if (aComponent == null)
                continue;

            if (!(aComponent instanceof JMenuItem))
                continue;

            ExtensionWrapper aWrapper = (ExtensionWrapper) aComponent.getClientProperty("bluej.extmgr.ExtensionWrapper");
            if (aWrapper == null)
                continue;

            ckeckThisWrapper(aWrapper, menuItems);
        }

        if (menuItems.isEmpty())
            return;

        if (menuSeparator == null) {
            menuSeparator = new JPopupMenu.Separator();
            popupMenu.add(menuSeparator);
        }

        for (Iterator iter = menuItems.iterator(); iter.hasNext(); )
            popupMenu.add((JComponent) iter.next());
    }


    /**
     * Returns true if this extension has already put a menuItem in the menu
     *
     * @param  forThisWrapper  Description of the Parameter
     * @param  menuItems       Description of the Parameter
     */
    private void ckeckThisWrapper(ExtensionWrapper forThisWrapper, LinkedList menuItems)
    {
        for (Iterator iter = menuItems.iterator(); iter.hasNext(); ) {
            JComponent aComponent = (JComponent) iter.next();

            ExtensionWrapper aWrapper = (ExtensionWrapper) aComponent.getClientProperty("bluej.extmgr.ExtensionWrapper");
            if (aWrapper == null)
                continue;

            // The menu I would like to add is not present in the currently availabe menus
            if (forThisWrapper != aWrapper)
                continue;

            iter.remove();
        }
    }


    /**
     * Sets the object being attached to this menu.
     *
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
        int itemsCount = 0;

        JPopupMenu aPopup = (JPopupMenu) event.getSource();

        MenuElement[] elements = aPopup.getSubElements();

        for (int index = 0; index < elements.length; index++) {

            JComponent aComponent = (JComponent) elements[index].getComponent();
            if (aComponent == null)
                continue;

            if (!(aComponent instanceof JMenuItem))
                continue;

            ExtensionWrapper aWrapper = (ExtensionWrapper) aComponent.getClientProperty("bluej.extmgr.ExtensionWrapper");
            if (aWrapper == null)
                continue;

            if (!aWrapper.isValid()) {
                popupMenu.remove(aComponent);
                continue;
            }

            aWrapper.safePostMenuItem(attachedObject, (JMenuItem) aComponent);

            itemsCount++;
        }

        if (itemsCount <= 0 && menuSeparator != null) {
            popupMenu.remove(menuSeparator);
            menuSeparator = null;
        }
    }


    /**
     *  Description of the Method
     *
     * @param  event  Description of the Parameter
     */
    public void popupMenuWillBecomeInvisible(PopupMenuEvent event) { }


    /**
     *  Description of the Method
     *
     * @param  event  Description of the Parameter
     */
    public void popupMenuCanceled(PopupMenuEvent event) { }
}
