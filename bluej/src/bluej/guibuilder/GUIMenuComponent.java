package javablue.GUIBuilder;

import java.awt.*;
import java.util.Vector;

/**
 * Defines the common interface for all menu component.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public interface GUIMenuComponent
{
    /**
     * Sets a reference to the main application.
     *
     * @param	The main GUIBuilderApp application.
     */
    public void setMainApp (GUIBuilderApp app);

    /**
     * Sets the parent of this menu component.
     *
     * @param parent	The menu component's parent in the tree structure.
     */
    public void setGUIParent (GUIMenuNodeComponent parent);

    /**
     * Returns the parent node in the tree structure of this menu component.
     *
     * @return	The parent of this component.
     */
    public GUIMenuNodeComponent getGUIParent ();

    /**
     * Removes this component from the tree structure.
     */
    public void delete();

    /**
     * Makes a copy of this menu component. This is used for the preview
     * function, since a menu component can only be shown in one frame at a
     * time.
     *
     * @return	A copy of this menu component.
     *
     * @see StructureContainer#preview()
     */
    public MenuComponent display ();

    /**
     * Returns a list representation of this GUIMenuComponent. This is used
     * to fill the listbox in the GUIMenuBarPropertyDialog.
     *
     * @return An array of MenuPairs representing the menu structure.
     */
    public MenuPair[] getListRepresentation ();

    /**
     * Returns the number of menu components this component contains.
     * GUIMenuLeafComponents always returns 1, GUIMenuNodeComponents may
     * return any number at or above 1 depending on how many items and sub
     * menus it contains.
     *
     * @return	The number of sub components.
     */
    public int getGUIItemCount ();

    /**
     * Generates the Java code used to make this menu component.
     *
     * @return	The Java code used to generate this component.
     */
    public ComponentCode generateCode();

    /**
     * Shows the property dialog of this component. This method will not return
     * until the dialog is closed.
     */
    public void showPropertiesDialog(Window parent);
}











