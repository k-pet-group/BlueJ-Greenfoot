package bluej.extensions;

import javax.swing.JMenuItem;

/**
 *  This interface allows an Extension Writer to provide its own menu to the
 *  BlueJ Tools Menu. The duty is extremly simple, there is only one function to
 *  implement and that should return the JMenuItem you want to display.
 *
 *  To help you I am going to provide a more complex example that you may scale
 *  down or up depending on your needs.
 *
 *  This example will provide you with TWO active menues. You can istantiate
 *  ExtensionMenu and then just do bluej.setMenuGen(myNewClass);
 *  Damiano
 *  
 *  <PRE>
 * import bluej.extensions.*;
 * import javax.swing.*;
 * import java.awt.event.*;
 *
 * public class ExtensionMenu implements MenuGen
 *   {
 *   private BlueJ bluej;
 *   private MenuAction anAction;
 *   private MenuAction moreAction;
 *
 *   public ExtensionMenu(BlueJ bluej)
 *     {
 *     this.bluej = bluej;
 *     anAction = new MenuAction ( "Hello", "Hello there" );
 *     moreAction = new MenuAction ( "Wow", "It works !" );
 *     }
 *
 *   public JMenuItem getMenuItem ( )
 *     {
 *     JMenu aMenu = new JMenu("Testing");
 *     aMenu.add (new JMenuItem (anAction));
 *     aMenu.add (new JMenuItem (moreAction));
 *    return aMenu;
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
 *       // You may want the current package to play with it, right ?
 *      bluej.getCurrentPackage();
 *      }
 *    }
 *  }
 */
public interface MenuGen
{

    /**
     *  Your implementation just needs to return a single menu item.
     *  You MUST follows the following guidelines:
     *  - The returned JMenuItem and all possible subtree MUST always be a new  one.
     *  - Do NOT store them away in your code.
     *
     * @return    The menuItem value
     */
    public JMenuItem getMenuItem();
}
