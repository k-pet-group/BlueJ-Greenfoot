package bluej.extmgr;

/**
 * This class will manage dynamic popup menues for extensions.
 * The basic idea is simple, add menu items only when the user wants them.
 * We do this for the following reason.
 * 1) No more need to keep track if and when an extension is gone.
 * 2) Extension writers have a finer control on how to display the menu items.
 * 3) No more problems with syncronization between user and swing thread (all is swing)
 * 4) The whole system is much simpler.
 * 
 * Damiano
 * 
 * Author: Damiano Bolla, University of Kent at Canterbury, 2003
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;


/**
 * Michael: this is work in progress, when it is done it will be formatted and commented appropriately.
 * 
 * This class will be created each time a new popup menu is created.
 * NOTA: A popup menu is not the same thing as a popup menu item. 
 * When you create this class you bind it to the object it is referred to.
 */

public class PopupManager implements PopupMenuListener
  {
  private ExtensionsManager extMgr;
  private Object attachedObject;
  private LinkedList menuItems;

  public PopupManager(Object attachedTo )
    {
    extMgr = ExtensionsManager.get();
    attachedObject  = attachedTo;
    }

  /**
   * For long lived PopupManagers it may happens that the object they are attached to 
   * changes during the lifetime.
   */
  public void setAttachedObject ( Object attachedTo )
    {
    attachedObject  = attachedTo;
    }

  /**
   * Called just before the PopupMenu becomes visible.
   * It is nice that we can change the contents of the menu...
   */
  public void popupMenuWillBecomeVisible(PopupMenuEvent event) 
    {
//    System.out.println ("Before will");

    Object source = event.getSource();
    // Let it throw an exception if it is not a JPopupMenu.
    JPopupMenu aPopup = (JPopupMenu)source;

    // Let me get all menues that should be shown
    menuItems = extMgr.getMenuItems(attachedObject);

    // None found, nothing to do.
    if ( menuItems.isEmpty() ) return;

    for ( Iterator iter = menuItems.iterator(); iter.hasNext(); )
      aPopup.add((Component)iter.next());    
    }

  public void popupMenuWillBecomeInvisible(PopupMenuEvent event) 
    {
//    System.out.println ("Become Invisible");
    
    Object source = event.getSource();
    // Let it throw an exception if it is not a JPopupMenu.
    JPopupMenu aPopup = (JPopupMenu)source;

    if ( menuItems.isEmpty() ) return;

    for ( Iterator iter = menuItems.iterator(); iter.hasNext(); )
      aPopup.remove((Component)iter.next());    
    }

  public void popupMenuCanceled(PopupMenuEvent event) 
    {
//    System.out.println ("Cancel");

    Object source = event.getSource();
    // Let it throw an exception if it is not a JPopupMenu.
    JPopupMenu aPopup = (JPopupMenu)source;
    
    if ( menuItems.isEmpty() ) return;

    for ( Iterator iter = menuItems.iterator(); iter.hasNext(); )
      aPopup.remove((Component)iter.next());    
    }
     
  }