package bluej.extensions;

import javax.swing.JMenuItem;

/**
 * Extensions which wish to add a menu item to BlueJ should register an
 * instance of MenuGenerator with the BlueJ proxy object.
 *
 * To activate the menus you instantiate an object of the ExtensionMenu class
 * and then register it with the BlueJ proxy object, e.g.:
 * <pre>
 *        MenuBuilder myMenu = new MenuBuilder(bluej);
 *        bluej.setMenuGenerator(myMenu);
 * </pre>
 * 
 * A fully functional example follows. Read it carefully and use it as a base.
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
