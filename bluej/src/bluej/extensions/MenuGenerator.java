package bluej.extensions;

import javax.swing.JMenuItem;

/**
 * Extensions which wish to add a menu item to the BlueJ Tools menu should register an
 * instance of MenuGenerator with the BlueJ proxy object.  
 *
 * A MenuGenerator is only required to implement one function, which should return the 
 * JMenuItem which the extension wishes to display. The JMenuItem returned can itself be a JMenu, 
 * allowing extensions to build more complex menu structures.
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
 * @version $Id: MenuGenerator.java 2098 2003-07-07 18:52:58Z damiano $
 */

 /*
  * Author Damiano Bolla, University of Kent at Canterbury. January 2003
  */
 
public class MenuGenerator
{
  /**
   * Returns the JMenuItem to be added to the BlueJ Tools menu.
   * Extensions should not retain references to the menu items created.
   */
  public JMenuItem getMenuItem()
  {
      return null;
  }
}
