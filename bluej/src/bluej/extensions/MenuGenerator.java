package bluej.extensions;

import javax.swing.JMenuItem;

/**
 * Extensions which wish to add a menu item to BlueJ should register an
 * instance of MenuGenerator with the BlueJ proxy object.
 *
 * To activate the menus you instantiate an object of the ExtensionMenu class
 * and then register it with the BlueJ proxy object, e.g.:
 * <pre>
 * bluej.setMenuGenerator(new MenuBuilder(bluej));
 * </pre>
 * The fully functional example below will provide to you not only a working code
 * but will also state what are the guidelines that you must follow while adding menu to BlueJ.
 * Read the example carefully and use it as a base.
 * <pre>
 * 
 * import bluej.extensions.*;
 * import bluej.extensions.event.*;
 * 
 * import java.net.URL;
 * import javax.swing.*;
 * import java.awt.event.*;
 * 
 * 
 * // This example shows how you can bind different menu to different part of BlueJ
 * // It is important to remembar the rules you have to follow.
 * // - getToolsMenuItem, getClassMenuItem, getObjectMenuItem can be called at any time.
 * // - They must generate a new JMenuItem every time.
 * // - No reference to the JMenuItem should be stored in the extension.
 * // - You must be quick in generating your menu.
 * // Having said all the bove we strongly suggest that you just copy this example and then adapt it
 * // to suit your needs. This example is fairly flexible and should cover most cases.
 * 
 * public class MenuBuilder extends MenuGenerator
 * {
 *     private ToolsAction aToolsAction;
 *     private ClassAction aClassAction;
 *     private ObjectAction aObjectAction;
 *     private BlueJ bluej;
 * 
 * 
 *     MenuBuilder(BlueJ aBluej)
 *     {
 *         bluej = aBluej;
 * 
 *         aToolsAction = new ToolsAction("Click Tools", "From Tools Menu");
 *         aClassAction = new ClassAction("Click Class", "From Class Menu");
 *         aObjectAction = new ObjectAction("Click Object", "From Object Menu");
 *     }
 * 
 * 
 *     public JMenuItem getToolsMenuItem(BPackage aPackage)
 *     {
 *         return new JMenuItem(aToolsAction);
 *     }
 * 
 * 
 *     public JMenuItem getClassMenuItem(BClass aClass)
 *     {
 *         return new JMenuItem(aClassAction);
 *     }
 * 
 * 
 *     public JMenuItem getObjectMenuItem(BObject anObject)
 *     {
 *         return new JMenuItem(aObjectAction);
 *     }
 * 
 * 
 *     // This utility will print the objects currently selected in BlueJ
 *     private void printCurrentStatus()
 *     {
 *         try {
 *             BPackage aPkg = bluej.getCurrentPackage();
 *             System.out.println("Current Package=" + aPkg);
 *             BClass aClass = aPkg.getCurrentBClass();
 *             System.out.println("Current Class=" + aClass);
 *             BObject anObj = aPkg.getCurrentObject();
 *             System.out.println("Current Object=" + anObj);
 *         } catch (Exception exc) {
 *         }
 *     }
 * 
 * 
 * 
 *     // Now there are a few classes that differentiate between the various menu
 *     // and provide a uniforma way to manage all menu created.
 *     class ToolsAction extends AbstractAction
 *     {
 *         private String aMessage;
 * 
 * 
 *         public ToolsAction(String menuName, String aMessage)
 *         {
 *             this.aMessage = aMessage;
 *             putValue(AbstractAction.NAME, menuName);
 *         }
 * 
 *         public void actionPerformed(ActionEvent anEvent)
 *         {
 *             System.out.println("---------- Tools Menu ----------");
 *             System.out.println("Message=" + aMessage);
 *             printCurrentStatus();
 *         }
 *     }
 * 
 * 
 *     class ClassAction extends AbstractAction
 *     {
 *         private String aMessage;
 * 
 * 
 *         public ClassAction(String menuName, String aMessage)
 *         {
 *             this.aMessage = aMessage;
 *             putValue(AbstractAction.NAME, menuName);
 *         }
 * 
 * 
 *         public void actionPerformed(ActionEvent anEvent)
 *         {
 *             System.out.println("---------- Class Menu ----------");
 *             System.out.println("Message=" + aMessage);
 *             printCurrentStatus();
 *         }
 *     }
 * 
 * 
 *     class ObjectAction extends AbstractAction
 *     {
 *         private String aMessage;
 * 
 * 
 *         public ObjectAction(String menuName, String aMessage)
 *         {
 *             this.aMessage = aMessage;
 *             putValue(AbstractAction.NAME, menuName);
 *         }
 * 
 * 
 *         public void actionPerformed(ActionEvent anEvent)
 *         {
 *             System.out.println("---------- Object Menu ----------");
 *             System.out.println("Message=" + aMessage);
 *             printCurrentStatus();
 *         }
 *     }
 * 
 * }
 * 
 * 
 * </pre> 
 */
  
public class MenuGenerator
{
    /**
     * Returns the JMenuItem that should be inserted in the Tools Menu.
     *
     * @param  aPackage  The Package this menu is bound to. Can be null.
     * @return     The menu item to display.
     */
    public JMenuItem getToolsMenuItem(BPackage aPackage)
    {
        return null;
    }


    /**
     * Returns the JMenuItem that should be inserted in the Class Menu.
     *
     * @param  aClass  The BClass this menu is bound to.
     * @return     The menu item to display.
     */
    public JMenuItem getClassMenuItem(BClass aClass)
    {
        return null;
    }


    /**
     * Returns the JMenuItem that should be inserted in the Object Menu.
     *
     * @param  anObject  The BObject this menu is bound to.
     * @return     The menu item to display.
     */
    public JMenuItem getObjectMenuItem(BObject anObject)
    {
        return null;
    }

}
