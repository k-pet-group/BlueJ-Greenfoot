package bluej.extensions;

import javax.swing.JMenuItem;

/**
 * Extensions wich wish to add a menu item to the BlueJ tools menu must register an
 * instance of MenuGenerator with BlueJ. 
 * MenuGenerator duty is extremly simple, there is only one function to
 * implement and that should return the JMenuItem you want to display.
 *
 * Adding a single menu is therefore very simple.
 * What follows is a more complex example that you may scale down or up depending on your needs, 
 * it provides you with two active menues. 
 * 
 * To activate the menues you can istantiate ExtensionMenu and then just do bluej.setMenuGenerator(myNewClass);
 *
 * 
 * <PRE>
 * import bluej.extensions.*;
 * import javax.swing.*;
 * import java.awt.event.*;
 *
 * public class ExtensionMenu implements MenuGenerator
 *   {
 *   private BlueJ bluej;
 *   private MenuAction anAction;
 *   private MenuAction moreAction;
 *
 *   public ExtensionMenu(BlueJ bluej)
 *     {
 *     this.bluej = bluej;
 *     anAction = new MenuAction ( "Hello", "Hello Damiano" );
 *     moreAction = new MenuAction ( "Wow", "It works !" );
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
 *    public MenuAction ( String menuName, String aMessage)
 *       {
 *      this.aMessage = aMessage;
 *       putValue (AbstractAction.NAME,menuName);
 *       }
 *
 *    public void actionPerformed ( ActionEvent anEvent )
 *      {
 *       System.out.println ("ButtonMessage="+aMessage);
 *       // You may want the current package to play with it, if so use the following.
 *      bluej.getCurrentPackage();
 *      }
 *    }
 *  }
 *  
 * </PRE>
 *
 * @version $Id: MenuGenerator.java 1780 2003-04-10 08:10:42Z damiano $
 */

 /*
  * Author Damiano Bolla, University of Kent at Canterbury. January 2003
  */
 
public interface MenuGenerator
{
  /**
   * Returns the JMenuItem you want to add to the BlueJ Tools menu.
   * <PRE>
   * You must follows the following guidelines:
   * - The returned JMenuItem and all possible subtree must always be a new one.
   * - Do not store them away in your code.
   *</PRE>
   */
  public JMenuItem getMenuItem();
}
