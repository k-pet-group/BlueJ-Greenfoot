package bluej.extensions;

import javax.swing.JMenuItem;

/**
 * Extensions which wish to add a menu item to BlueJ's menus should register an
 * instance of MenuGenerator with the BlueJ proxy object.  
 *
 * A MenuGenerator provides a set of functions which can be called back
 * by BlueJ to request the actual menu items which will be displayed, and
 * to indicate that a particular menu item is about to be displayed, so
 * that an extension can (e.g.) enable or disable appropriate items.
 *
 * Note that the JMenuItem which is returned by the extension can itself
 * be a JMenu, allowing extensions to build more complex menu structures.
 *
 * Note also that only Tools menus are supported at BlueJ 1.3.0. Class
 * and Object menus, and the "notification" methods will be supported from
 * the next BlueJ maintenance release.
 *
 * Below is a more complex example which creates a menu with two active menu items. 
 * 
 * To activate the menus you instantiate an object of the ExtensionMenu class
 * and then register it with the BlueJ proxy object, e.g.:
 * <pre>
 *        ExtensionMenu myMenu = new ExtensionMenu();
 *        bluej.setMenuGenerator(myMenu);
 * </pre>
 * Note that the MenuGenerator's <code>getMenuItem()</code> method may be called more than 
 * once during a BlueJ session, it should return a new set of MenuItems for each 
 * invocation. This is a restriction required by the Swing implementation, which 
 * does not allow sharing of MenuItems between menus. You can, of course, share 
 * MenuActions between all of the appropriate MenuItems.
 *
 * The code for the ExtensionMenu class is:
 * <PRE>
 * import bluej.extensions.*;
 * import javax.swing.*;
 * import java.awt.event.*;
 *
 * public class ExtensionMenu extends MenuGenerator
 *   {
 *   private MenuAction anAction;
 *   private MenuAction moreAction;
 *
 *   public ExtensionMenu()
 *     {
 *     anAction = new MenuAction ( "Hello", "Hello World" );
 *     moreAction = new MenuAction ( "Wow", "It works!" );
 *     }
 *
 *   public JMenuItem getMenuItem ( )
 *     {
 *     JMenu aMenu = new JMenu("Testing");
 *     aMenu.add (new JMenuItem (anAction));
 *     aMenu.add (new JMenuItem (moreAction));
 *     return aMenu;
 *     }
 *
 *   class MenuAction extends AbstractAction
 *     {
 *     private String aMessage;
 *
 *     public MenuAction ( String menuName, String aMessage)
 *       {
 *       this.aMessage = aMessage;
 *       putValue (AbstractAction.NAME,menuName);
 *       }
 *
 *    public void actionPerformed ( ActionEvent anEvent )
 *      {
 *      System.out.println ("ButtonMessage="+aMessage);
 *      }
 *    }
 *  }
 *  
 * </PRE>
 *
 * @version $Id: MenuGenerator.java 2181 2003-09-25 10:56:45Z damiano $
 */

 /*
  * Author Damiano Bolla, University of Kent at Canterbury. January 2003
  */
 
public class MenuGenerator
{
  /**
   * Returns the JMenuItem to be added to the BlueJ Tools menu.
   * Extensions should not retain references to the menu items created.
   *
   * Note that this method will be superceded by getToolsMenuItem() at the
   * next maintenance release of BlueJ
   */
  public JMenuItem getMenuItem( )
  {
      return null;
  }

  /**
   * Returns the JMenuItem to be added to the BlueJ Tools menu.
   * Extensions should not retain references to the menu items created.
   *
   * Note: this method will not be called by BlueJ 1.3.0.
   *
   * @param bp the BlueJ package with which this menu item will be associated.
   */
  public JMenuItem getToolsMenuItem(BPackage bp)
  {
      return null;
  }
  
  /**
   * Returns the JMenuItem to be added to the BlueJ Class menu
   * Extensions should not retain references to the menu items created.
   *
   * Note: this method will not be called by BlueJ 1.3.0.
   *
   * @param bc the BlueJ class with which this menu item will be associated.
   */
  public JMenuItem getClassMenuItem(BClass bc)
  {
      return null;
  }
  
  /**
   * Returns the JMenuItem to be added to the BlueJ Object menu
   * Extensions should not retain references to the menu items created.
   *
   * Note: this method will not be called by BlueJ 1.3.0.
   *
   * @param bo the BlueJ object with which this menu item will be associated.
   */
  public JMenuItem getObjectMenuItem(BObject bo)
  {
      return null;
  }
  
  /**
   * Called by BlueJ when a tools menu added by an extension is about to
   * be displayed. An extension can use this notification to decide whether
   * to enable/disable menu items and so on.
   *
   * Note: this method will not be called by BlueJ 1.3.0.
   *
   * @param bp the BlueJ package for which the menu is to be displayed
   * @param jmi the menu item which will be displayed (as provided by the
   * extension in a previous call to getToolsMenuItem)
   */
  public void notifyPostToolsMenu(BPackage bp, JMenuItem jmi) 
  {
      return;
  }

  /**
   * Called by BlueJ when a class menu added by an extension is about to
   * be displayed. An extension can use this notification to decide whether
   * to enable/disable menu items and so on.
   *
   * Note: this method will not be called by BlueJ 1.3.0.
   *
   * @param bc the BlueJ class for which the menu is to be displayed
   * @param jmi the menu item which will be displayed (as provided by the
   * extension in a previous call to getToolsMenuItem)
   */
  public void notifyPostClassMenu(BClass bc, JMenuItem jmi) 
  {
      return;
  }

  /**
   * Called by BlueJ when an object menu added by an extension is about to
   * be displayed. An extension can use this notification to decide whether
   * to enable/disable menu items and so on.
   *
   * Note: this method will not be called by BlueJ 1.3.0.
   *
   * @param bo the BlueJ object for which the menu is to be displayed
   * @param jmi the menu item which will be displayed (as provided by the
   * extension in a previous call to getToolsMenuItem)
   */
  public void notifyPostObjectMenu(BObject bo, JMenuItem jmi) 
  {
      return;
  }

}
