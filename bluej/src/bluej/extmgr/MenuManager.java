package bluej.extmgr;

import bluej.pkgmgr.*;

import java.util.*;

import javax.swing.*;
import javax.swing.JPopupMenu;
import javax.swing.event.*;


/**
 * Manages the menues being added by extensions.
 * An instance of this class is attached to each popup menu that needs to be aware of extensions menu.
 *
 * Author: Damiano Bolla, University of Kent at Canterbury, 2003,2004,2005
 */
public final class MenuManager implements PopupMenuListener {
    private final ExtensionsManager extMgr;
    private final JPopupMenu.Separator menuSeparator;
    private final JPopupMenu popupMenu;
    private Object attachedObject;

    /**
     * Constructor for the MenuManager object.
     *
     * @param  aPopupMenu  The menu that extensions are attaching to.
     */
    public MenuManager(JPopupMenu aPopupMenu) {
        extMgr = ExtensionsManager.getInstance();
        popupMenu = aPopupMenu;
        popupMenu.addPopupMenuListener(this);
        menuSeparator = new JPopupMenu.Separator();
    }

    /**
     * Add all the menu currently available to the menu.
     * This may be called any time BlueJ feels that the menu needs to be updated.
     *
     * @param  onThisProject  a specific project to look for, or null for all projects.
     */
    public void addExtensionMenu(Project onThisProject) {
        // Get all menus that can be possibly be generated now.
        LinkedList menuItems = extMgr.getMenuItems(attachedObject, onThisProject);

        // Retrieve all the items from the current menu
        MenuElement[] elements = popupMenu.getSubElements();

        for (int index = 0; index < elements.length; index++) {
            JComponent aComponent = (JComponent) elements[index].getComponent();

            if (aComponent == null) {
                continue;
            }

            if (!(aComponent instanceof JMenuItem)) {
                continue;
            }

            ExtensionWrapper aWrapper = (ExtensionWrapper) aComponent.getClientProperty(
                    "bluej.extmgr.ExtensionWrapper");

            if (aWrapper == null) {
                continue;
            }

            popupMenu.remove(aComponent);
        }

        popupMenu.remove(menuSeparator);

        // If the provided menu is empty we are done here.
        if (menuItems.isEmpty()) {
            return;
        }

        popupMenu.add(menuSeparator);

        for (Iterator iter = menuItems.iterator(); iter.hasNext();)
            popupMenu.add((JComponent) iter.next());
    }

    /**
     * Sets the object being attached to this menu.
     *
     * @param  attachedTo  The new attachedObject value
     */
    public void setAttachedObject(Object attachedTo) {
        attachedObject = attachedTo;
    }

    /**
     * Notify to all valid extensions that this menu Item is about to be displayed.
     *
     * @param  event  The associated event
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
        int itemsCount = 0;

        JPopupMenu aPopup = (JPopupMenu) event.getSource();

        MenuElement[] elements = aPopup.getSubElements();

        for (int index = 0; index < elements.length; index++) {
            JComponent aComponent = (JComponent) elements[index].getComponent();

            if (aComponent == null) {
                continue;
            }

            if (!(aComponent instanceof JMenuItem)) {
                continue;
            }

            ExtensionWrapper aWrapper = (ExtensionWrapper) aComponent.getClientProperty(
                    "bluej.extmgr.ExtensionWrapper");

            if (aWrapper == null) {
                continue;
            }

            if (!aWrapper.isValid()) {
                popupMenu.remove(aComponent);

                continue;
            }

            aWrapper.safePostMenuItem(attachedObject, (JMenuItem) aComponent);

            itemsCount++;
        }

        if (itemsCount <= 0) {
            popupMenu.remove(menuSeparator);
        }
    }

    /**
     *  Dummy to saisfy the implements PopupMenuListener.
     */
    public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
    }

    /**
     *  Dummy to saisfy the implements PopupMenuListener.
     */
    public void popupMenuCanceled(PopupMenuEvent event) {
    }
}
