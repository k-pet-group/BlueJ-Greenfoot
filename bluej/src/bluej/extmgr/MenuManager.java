package bluej.extmgr;

import bluej.pkgmgr.*;
import bluej.utility.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;

/**
 * This manages the menues for a given extension-wrapper
 * This is here to have a clean modular code
 * So, the idea is that there is a MenuManger for each extension
 * and you get to this trough the extension wrapper
 */
public class MenuManager 
  {
  // To get the JMenuItem from the extension I need to know this so I store it away
  private final ExtensionWrapper myWrapper;

  /** 
   * I need to remembar what JMenuItem is associated with PkgMgrFrame
   * NOTE: There is a HUGE syncronization problem here, the way I think to solve it
   * is by accessing this ONLY by means of Swing invokeLater....
   */
  private HashMap framesToJmenu;

  /**
   * Every extension manager will have one of this, you may ask why
   * not put it there, to have a modular approach. This just handles menues
   * it does not do other things. It has quite a few peculiarities that 
   * other code is not interested on.
   */
  public MenuManager ( ExtensionWrapper myWrapper )
    {
    this.myWrapper = myWrapper;
    framesToJmenu  = new HashMap(20);    
    }


  /**
   * This will revalidate this extension menu on all frames
   * It is called by an Extension when it wants to revalidate its menues
   */
  public void menuExtensionRevalidateReq (  )
    {
    EventQueue.invokeLater(new MenuExtensionRevalidateReq());
    }

  /**
   * Given a frame revalidate this extension against it.
   * You MUST call this one from within a SWING thread. Tipically when a frame
   * is created and you are setting it up...
   */
  public void menuFrameRevalidateReq ( PkgMgrFrame thisFrame )
    {
    JMenuItem oldMenuItem = (JMenuItem)framesToJmenu.get(thisFrame);
    JMenuItem newMenuItem = menuFrameRevalidate (thisFrame,oldMenuItem);
    framesToJmenu.put(thisFrame,newMenuItem);
    }


  /**
   * This does the actual work, it is disjoint since I need it from two places
   * You MUST call this function from WITHIN a Swing Thread
   */
  private JMenuItem menuFrameRevalidate ( PkgMgrFrame thisFrame, JMenuItem oldMenuItem )
    {
    JMenuItem newMenuItem=null;
    int insertPoint;

    // This will return -1 if the given menu is not present... 
    insertPoint = removeMenuItem (oldMenuItem);

    if ( ! myWrapper.isValid() ) return null;

    newMenuItem = myWrapper.safeMenuGenGetMenuItem();
    if ( newMenuItem == null ) return null;

    JMenu toolsMenu = thisFrame.getToolsMenu();
    if ( toolsMenu == null ) return null;

    if ( insertPoint < 0 ) 
        toolsMenu.add(newMenuItem);
    else
        toolsMenu.add(newMenuItem,insertPoint);

    return newMenuItem;
    }


  /**
   * This will remove the given item AND return its previous position.
   * returns the removed item position if all is fine
   * -1 if either the item is null or cannot be found (strange, really)
   * This MUST be called from a Swing safe thread
   */
  private int removeMenuItem ( JMenuItem anItem )
    {
    if ( anItem == null ) return -1;

    Container aContainer = anItem.getParent();

    // This should never happen, if it does you just get double menues...
    if ( ! (aContainer instanceof JPopupMenu) )  {
        Debug.message("MenuManager.removeMenuItem(): ERROR: aContainer.Class="+aContainer.getClass().getName());
        return -1;
    }

    JPopupMenu parentMenu = (JPopupMenu)aContainer;

    int index = parentMenu.getComponentIndex(anItem);
    if ( index < 0 ) return -1;

    // FInally I can remove this menu....
    parentMenu.remove(index);

    return index;
    } 


  /**
   * This is just to satisfy the swing utilities for RunLater
   */
  class MenuExtensionRevalidateReq implements Runnable
    {
    public void run ()
      {
      }
      /*
      HashMap newFramesToJmenu = new HashMap(20);
      PkgMgrFrame[] allFrames  = PkgMgrFrame.getAllFrames();

      for (int index=0; index<allFrames.length; index++ )
        {
        PkgMgrFrame thisFrame = allFrames[index];
        //Debug.message("MenuManager.MenuExtensionRevalidateReq.run(): index="+index);

        thisFrame.toolsExtensionsCheckSeparator();
        JMenuItem oldMenuItem = (JMenuItem)framesToJmenu.get(thisFrame);
        JMenuItem newMenuItem = menuFrameRevalidate (thisFrame,oldMenuItem);
        newFramesToJmenu.put(thisFrame,newMenuItem);
        }
      framesToJmenu = newFramesToJmenu;
      }
      */
    }
  }


