package bluej.guibuilder;

/**
 * This class is used to group a GUIMenuComponent and a string representation
 * of that menu together. The string representation is used in the listbox in
 * the GUIMenuBarPropertyDialog as a return type from the
 * getListRepresentation() method.
 *
 * Created: Oct 2, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class MenuPair
{
    private String label = null;
    private GUIMenuComponent menu = null;


    /**
     * Constructs a new MenuPair with the specified label and the specified
     * GUIMenuComponent.
     *
     * @param label The string representation of the menu.
     * @param menu  The menu component.
     */
    public MenuPair (String label, GUIMenuComponent menu)
    {
	this.label = label;
	this.menu = menu;
    }


    /**
     * Sets the label of the MenuPair.
     *
     * @param label The string representation of the menu.
     */
    public void setLabel (String label)
    {
	this.label = label;
    }


    /**
     * Returns the label of the MenuPair.
     *
     * @return The string representation of the menu.
     */
    public String getLabel ()
    {
	return label;
    }


    /**
     * Sets the menu component of the MenuPair.
     *
     * @param menu  The menu component.
     */
    public void setMenuComponent (GUIMenuComponent menu)
    {
	this.menu = menu;
    }


    /**
     * Returns the menu component of the MenuPair.
     *
     * @return The menu component.
     */
    public GUIMenuComponent getMenuComponent ()
    {
	return menu;
    }
}
