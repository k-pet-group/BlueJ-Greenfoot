package bluej.guibuilder;

import java.awt.*;
import java.util.Vector;
import java.util.Enumeration;

/**
 * A custom component representing a menu.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIMenu extends Menu implements GUIMenuNodeMenuComponent
{
    private transient GUIBuilderApp app = null;
    private StructureContainer structCont = null;
    private GUIMenuNodeComponent parent = null;
    private Vector menuVector = new Vector();
    private static int counter = 0;
    private int initlevel = ComponentCode.METHODLEVEL;


    /**
     * Constructs a new GUIMenu with no label.
     *
     * @param structCont    The StructureContainer containing the menu component.
     * @param app	    The main application.
     */
    public GUIMenu (StructureContainer structCont, GUIBuilderApp app)
    {
    	super("Menu"+counter);
	setName("Menu"+counter);
	counter++;
        this.structCont = structCont;
        this.app =app;
    }


    /**
     * Constructs a new GUIMenu with the specified label.
     *
     * @param label	   A string label for the button.
     * @param structCont   The StructureContainer containing the menu component.
     * @param app	   The main application.
     */
    public GUIMenu (String label, StructureContainer structCont, GUIBuilderApp app)
    {
    	super(label);
	setName("Menu"+counter);
	counter++;
        this.structCont = structCont;
        this.app =app;
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
     * Sets the parent of this menu component.
     *
     * @param parent	The menu component's parent in the tree structure.
     */
    public void setGUIParent (GUIMenuNodeComponent parent)
    {
	this.parent = parent;
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
     * Removes this component and its children from the tree structure.
     */
    public void delete ()
    {
	for (Enumeration enum = menuVector.elements(); enum.hasMoreElements();)
	{
	    ((GUIMenuComponent)enum.nextElement()).delete();
	}
	if (parent!=null)
	    parent.deleteChild(this);
    }


    /**
     * Shows the property dialog of this component. This method will not return
     * until the dialog is closed.
     */
    public void showPropertiesDialog(Window parentWindow)
    {
	GUIMenuPropertyDialog propertyDialog = new GUIMenuPropertyDialog(app, parentWindow, "Menu Properties", this, structCont);
    }


    /**
     * Generates the Java code used to make this menu component.
     *
     * @return	The Java code used to generate this component.
     */
    public ComponentCode generateCode ()
    {
	ComponentCode code = new ComponentCode();

	String initCode = "Menu "+getName()+" = new Menu(\""+getLabel()+"\");\n";
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
	    code.addCreation(getName()+".setFont (new Font (\""+font.getName()+"\", "+face+", "+font.getSize()+");\n");
	}

	GUIMenuComponent menu;
	ComponentCode childCode;
	StringBuffer addCode = new StringBuffer();
	for (Enumeration enum = menuVector.elements(); enum.hasMoreElements();)
	{
	    menu = ((GUIMenuComponent)enum.nextElement());
	    if (menu instanceof GUIMenuSeparator)
		addCode.append(getName()+".addSeparator();\n");
	    else
	    {
		childCode = menu.generateCode();
		code.addGlobal (childCode.getGlobalCode());
		code.addCreation (childCode.getCreationCode());
		addCode.append(getName()+".add ("+((MenuComponent)menu).getName()+");\n");
	    }
	}
	code.addCreation(addCode.toString());

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
    public MenuComponent display ()
    {
	Menu preview = new Menu(getLabel());
	preview.setFont (getFont());
	GUIMenuComponent menu;
	for (Enumeration enum = menuVector.elements(); enum.hasMoreElements();)
	{
	    menu = (GUIMenuComponent)enum.nextElement();
	    if (menu instanceof GUIMenuSeparator)
		preview.addSeparator();
	    else
		preview.add((MenuItem)menu.display());
	}
	return preview;
    }


    /**
     * Adds the specified menu component to this component.
     *
     * @param menuComponent The menu component to be added.
     */
    public void addGUIMenuComponent (GUIMenuComponent menu)
    {
	menu.setGUIParent(this);
	menuVector.addElement(menu);
	if (menu instanceof GUIMenuSeparator)
	    addSeparator();
	else
	    add((MenuItem)menu);
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
	menu.setGUIParent(this);
	int index = menuVector.indexOf(beforeComponent);
	menuVector.insertElementAt(menu, index);
	if (menu instanceof GUIMenuSeparator)
	    insertSeparator(index);
	else
	    insert((MenuItem)menu, index);
    }


    /**
     * Removes the specified menu component from this component.
     *
     * @param menuComponent The menu component to be deleted.
     */
    public void deleteChild (GUIMenuComponent menu)
    {
	int index = menuVector.indexOf(menu);
	menuVector.removeElementAt(index);
	remove(index);
    }


    /**
     * Checks if this menu bar has any menus associated.
     *
     * @returns false if the menu bar is empty, true otherwise.
     */
    public boolean containItems ()
    {
	return (!menuVector.isEmpty());
    }


    /**
     * Returns a list representation of this GUIMenuComponent. This is used
     * to fill the listbox in the GUIMenuBarPropertyDialog.
     *
     * @return An array of MenuPairs representing the menu structure.
     */
    public MenuPair[] getListRepresentation ()
    {
	int index = 1;
	MenuPair[] result = new MenuPair[getGUIItemCount()];
	result[0] = new MenuPair("+"+getLabel(), this);

	MenuPair[] tmpMenu;
	for (Enumeration enum = menuVector.elements(); enum.hasMoreElements();)
	{
	    tmpMenu = ((GUIMenuComponent)enum.nextElement()).getListRepresentation();
	    for (int i=0; i<tmpMenu.length; i++)
		result[index+i] = new MenuPair("  "+tmpMenu[i].getLabel(), tmpMenu[i].getMenuComponent());
		
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

