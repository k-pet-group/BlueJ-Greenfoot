package javablue.GUIBuilder;

import java.awt.*;
import java.util.Vector;

/**
 * A custom component representing a menu item.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIMenuItem extends MenuItem implements GUIMenuLeafComponent
{
    private transient GUIBuilderApp app = null;
    private StructureContainer structCont = null;
    private GUIMenuNodeMenuComponent parent = null;
    private static int counter = 0;
    private int initlevel = ComponentCode.METHODLEVEL;
    private Vector listenerVector = new Vector();


    /**
     * Constructs a new GUIMenuItem with no label.
     *
     * @param structCont    The StructureContainer containing the menu component.
     * @param app	    The main application.
     */
    public GUIMenuItem(StructureContainer structCont, GUIBuilderApp app)
    {
	super("menuItem"+counter);
	setName("menuItem"+counter);
	counter++;
	this.structCont = structCont;
	this.app =app;
    }


    /**
     * Constructs a new GUIMenuItem with the specified label.
     *
     * @param label	   A string label for the button.
     * @param structCont   The StructureContainer containing the menu component.
     * @param app	   The main application.
     */
    public GUIMenuItem(String label, StructureContainer structCont, GUIBuilderApp app)
    {
	super(label);
	setName("menuItem"+counter);
	counter++;
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
	this.parent = (GUIMenuNodeMenuComponent)parent;
    }


    /**
     * Returns the parent node in the tree structure of this menu component.
     *
     * @return	The parent of this component.
     */
    public GUIMenuNodeComponent getGUIParent ()
    {
	return parent;
    }


    /**
     * Sets a reference to the main application.
     *
     * @param	The main GUIBuilderApp application.
     */
    public void setMainApp (GUIBuilderApp app)
    {
	this.app = app;
    }


    /**
     * Return a vector with the names of all listeners accociated with this
     * menu component.
     *
     * @return String vector with the names of accociated listeners.
     */
    public Vector getListenerVector()
    {
	return listenerVector;
    }


    /**
     * Generates the Java code used to make this menu component.
     *
     * @return	The Java code used to generate this component.
     */
    public ComponentCode generateCode()
    {
	ComponentCode code = new ComponentCode ();

	String initCode = "MenuItem "+getName()+" = new MenuItem(\""+getLabel()+"\");\n";
	if (initlevel==ComponentCode.CLASSLEVEL)
	    code.addGlobal (initCode);
	else
	    code.addCreation (initCode);

	if (!getFont().equals(((MenuComponent)parent).getFont()))
	{
            Font font = getFont();
            String face = new String();
            if(font.isBold() && font.isItalic())
                face = "Font.BOLD|Font.ITALIC";
            else if (font.isBold())
                face = "Font.BOLD";
            else if(font.isItalic())
                face = "Font.ITALIC";
            else
                face = "Font.PLAIN";
	    code.addCreation(getName()+".setFont(new Font(\""+font.getName()+"\", "+face+", "+font.getSize()+");\n");
	}

	MenuShortcut shortcut = getShortcut();
	if (shortcut!=null)
	{
	    int keycode = shortcut.getKey();
	    GUIMenuShortcuts sc = new GUIMenuShortcuts();
	    int i;
	    for (i=0; sc.shortcuts[i].keycode!=keycode; i++)
		;
	    code.addCreation(getName()+".setShortcut(new MenuShortcut(java.awt.event.KeyEvent.VK_"+sc.shortcuts[i].label);
	    if (shortcut.usesShiftModifier())
		code.addCreation(", true");
	    code.addCreation("));\n");
	}

	for(int i=0; i<listenerVector.size(); i++)
	    code.addCreation(getName()+"."+((ListenerPair)(listenerVector.elementAt(i))).getAddFunction()+";\n");

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
	MenuItem preview = new MenuItem(getLabel(), getShortcut());
	preview.setFont (getFont());

	return preview;
    }


    /**
     * Shows the property dialog of this component. This method will not return
     * until the dialog is closed.
     */
    public void showPropertiesDialog(Window parentWindow)
    {
	GUIMenuPropertyDialog propertyDialog = new GUIMenuPropertyDialog(app, parentWindow, "MenuItem Properties", this, structCont);
    }


    /**
     * Removes this component from the tree structure.
     */
    public void delete()
    {
	if (parent!=null)
            parent.deleteChild(this);
    }


    /**
     * Returns a list representation of this GUIMenuComponent. This is used
     * to fill the listbox in the GUIMenuBarPropertyDialog.
     *
     * @return An array of MenuPairs representing the menu structure.
     */
    public MenuPair[] getListRepresentation ()
    {
	MenuPair[] result = {new MenuPair (" "+getLabel(), this)};
	return (result);
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
	return 1;
    }
}
