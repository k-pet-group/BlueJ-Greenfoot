package javablue.GUIBuilder;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Enumeration;
import java.io.Serializable;
import javablue.GUIGraphics.*;

/**
 * A custom component representing a menu bar.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIMenuBar extends MenuBar implements GUIMenuNodeBarComponent
{
    private transient GUIBuilderApp app = null;
    private StructureContainer structCont = null;
    private GUIFrame parent = null;
    private Vector menuVector = new Vector();
    private int initlevel = ComponentCode.METHODLEVEL;

    /**
     * Constructs a new GUIMenuBar.
     *
     * @param parent	The frame window which this menu bar is attached to.
     * @param structCont   The StructureContainer containing the menu component.
     * @param app	   The main application.
     */
    public GUIMenuBar (GUIFrame parent, StructureContainer structCont, GUIBuilderApp app)
    {
    	super();
	setName("menuBar");
        this.parent = parent;
        this.structCont = structCont;
        this.app =app;
    }


    /**
     * Sets the parent of this menu component.
     *
     * @param parent	The menu component's parent in the tree structure.
     */
    public void setGUIParent (GUIMenuNodeComponent parent)
    {
    }


    /**
     * Returns the parent node in the tree structure of this menu component.
     *
     * @return	The parent of this component.
     */
    public GUIMenuNodeComponent getGUIParent ()
    {
	return null;
    }


    /**
     * Sets a reference to the main application.
     *
     * @param	The main GUIBuilderApp application.
     */
    public void setMainApp (GUIBuilderApp app)
    {
	this.app = app;
	for (Enumeration enum = menuVector.elements(); enum.hasMoreElements();)
	{
	    ((GUIMenuComponent)enum.nextElement()).setMainApp(app);
	}
    }


    /**
     * Adds the specified menu component to this component.
     *
     * @param menuComponent The menu component to be added.
     */
    public void addGUIMenuComponent (GUIMenuComponent menu)
    {
	add((Menu)menu);
	menu.setGUIParent(this);
	menuVector.addElement(menu);
    }


    /**
     * Inserts the specified menu component at the specified position in this
     * component.
     *
     * @param menuComponent	The menu component to be inserted.
     * @param beforeComponent	Insert the component just before this component.
     */
    public void insertGUIMenuComponent (GUIMenuComponent menu, GUIMenuComponent beforeComponent)
    {
	addGUIMenuComponent(menu);
    }


    /**
     * Removes the specified menu component from this component.
     *
     * @param menuComponent The menu component to be deleted.
     */
    public void deleteChild (GUIMenuComponent menu)
    {
	remove((MenuComponent)menu);
	menuVector.removeElement(menu);
    }


    /**
     * Removes this component from the tree structure.
     */
    public void delete ()
    {
    }


    /**
     * Generates the Java code used to make this menu component.
     *
     * @return	The Java code used to generate this component.
     */
    public ComponentCode generateCode()
    {
	ComponentCode code = new ComponentCode();

	String initCode = "MenuBar "+getName()+" = new MenuBar();\n";
	if (initlevel==ComponentCode.CLASSLEVEL)
	    code.addGlobal (initCode);
	else
	    code.addCreation (initCode);

	GUIMenuNodeMenuComponent menu;
	ComponentCode childCode;
	StringBuffer addCode = new StringBuffer();
	for (Enumeration enum = menuVector.elements(); enum.hasMoreElements();)
	{
	    menu = ((GUIMenuNodeMenuComponent)enum.nextElement());
	    childCode = menu.generateCode();
	    code.addGlobal (childCode.getGlobalCode());
	    code.addCreation (childCode.getCreationCode());
	    addCode.append(getName()+".add ("+((MenuComponent)menu).getName()+");\n");
	}
	code.addCreation(addCode.toString());
	code.addCreation("setMenuBar ("+getName()+");\n");

	return code;
    }


    /**
     * Makes a copy of this menu component. This is used for the preview
     * function, since a menu component can only be shown in one frame at a
     * time.
     *
     * @return	A copy of this menu component.
     *
     * @see StructureContainer#preview()
     */
    public MenuComponent display()
    {
	MenuBar preview = new MenuBar();
	preview.setFont (getFont());
	for (Enumeration enum = menuVector.elements(); enum.hasMoreElements();)
	{
	    preview.add((Menu)((GUIMenuComponent)enum.nextElement()).display());
	}
	return preview;
    }


    /**
     * Shows the property dialog of this component. This method will not return
     * until the dialog is closed.
     */
    public void showPropertiesDialog(Window parentWindow)
    {
	GUIMenuBarPropertyDialog dialog = new GUIMenuBarPropertyDialog ((Frame)app, parentWindow, this, structCont, app);
    }


    /**
     * Checks if this menu bar has any menus associated.
     *
     * @returns false if the menu bar is empty, true otherwise.
     */
    public boolean containItems()
    {
	return (!menuVector.isEmpty());
    }


    /**
     * Returns a list representation of this GUIMenuComponent. This is used
     * to fill the listbox in the GUIMenuBarPropertyDialog.
     *
     * @return An array of MenuPairs representing the menu structure.
     */
    public MenuPair[] getListRepresentation()
    {
	int index = 1;
	MenuPair[] result = new MenuPair[getGUIItemCount()];
	result[0] = new MenuPair("+MenuBar", this);

	MenuPair[] tmpMenu;
	for (Enumeration enum = menuVector.elements(); enum.hasMoreElements();)
	{
	    tmpMenu = ((GUIMenuComponent)enum.nextElement()).getListRepresentation();
	    for (int i=0; i<tmpMenu.length; i++)
		result[index+i] = new MenuPair("   "+tmpMenu[i].getLabel(), tmpMenu[i].getMenuComponent());

	    index += tmpMenu.length;
	}
	return result;
    }


    /**
     * Returns the number of menu components this component contains.
     * GUIMenuLeafComponents always returns 1, GUIMenuNodeComponents may
     * return any number at or above 1 depending on how many items and sub
     * menus it contains.
     *
     * @return	The number of sub components.
     */
    public int getGUIItemCount()
    {
	int count = 0;
	for (Enumeration enum = menuVector.elements(); enum.hasMoreElements();)
	{
	    count += ((GUIMenuComponent)enum.nextElement()).getGUIItemCount();
	}
	return count+1;
    }
}
