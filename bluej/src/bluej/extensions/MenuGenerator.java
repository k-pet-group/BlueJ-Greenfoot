package bluej.extensions;

import javax.swing.JMenuItem;

/**
 * Extensions which wish to add a menu item to BlueJ should register an
 * instance of MenuGenerator with the BlueJ proxy object.
 *
 * To activate the menus you instantiate an object of the ExtensionMenu class
 * and then register it with the BlueJ proxy object, e.g.:
 * <pre>
 *        ExtensionMenu myMenu = new ExtensionMenu();
 *        bluej.setMenuGenerator(myMenu);
 * </pre>
 * 
 * When implementing a class that extends MenuGenerator you need to be awayre of.
 * - getXXXXMenuItem( ... ) can be called at any time before being displayed.
 * - the returned JMenuItem must not be stored anywhere in the extension code.
 *
 * @version    $Id: MenuGenerator.java 2087 2003-06-30 12:55:15Z damiano $
 */

/*
  * Author Damiano Bolla, University of Kent at Canterbury 2003
  */
  
/**
 * Returns the JMenuItem to be added to the BlueJ Tools menu.
 * Extensions must not retain references to the menu items created.
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
