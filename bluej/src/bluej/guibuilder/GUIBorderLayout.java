package bluej.guibuilder;

import java.awt.*;


/**
 * A class representing a BorderLayout.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class GUIBorderLayout extends BorderLayout implements GUIComponentLayoutNode
{
    private GUIComponentNode parent = null;
    private final static String[] posName = {"North","South","East","West","Center"};
    private static int counter = 0;
    private GUIContainer[] position = new GUIContainer[5];
    private transient GUIBuilderApp app = null;
    private StructureContainer structCont = null;
    private String name = new String();


     /**
     * Constructs a GUIBorderLayout.
     *
     * @param parent	    The component's parent in the tree structure.
     * @param structCont    The StructureContainer containing the component.
     * @param app	    The main application.
     */
    public GUIBorderLayout(GUIComponentNode parent, StructureContainer structCont, GUIBuilderApp app)
    {
	super();
	for (int i=0; i<position.length; i++)
	    position[i] = null;
	name = "borderlayout"+counter;
	counter++;
        this.parent = parent;
        this.structCont = structCont;
        this.app =app;
    }


    /**
     * Sets the parent of this layout.
     *
     * @param parent	The layout's parent in the tree structure.
     */
    public void setParent (GUIComponentNode parent)
    {
	this.parent = parent;
    }


     /**
     * Returns the parent node in the tree structure of this layout.
     * This is a GUIComponentNode.
     *
     * @return	The parent of this layout.
     */
    public GUIComponentNode getTreeParent()
    {
        return parent;
    }


     /**
     * Sets a reference to the StructureContainer that contains the tree containing this layout. Also updates the StrucutreContainer of its children.
     *
     * @param structCont    The StructureContainer containing layout.
     */
    public void setStructureContainer (StructureContainer structCont)
    {
	this.structCont = structCont;
	for (int i=0; i<position.length; i++)
	    position[i].setStructureContainer(structCont);
    }


    /**
     * Returns the StructureContainer that contains the tree containing this layout.
     *
     * @return	The container containing this layout.
     */
    public StructureContainer getStructureContainer ()
    {
	return structCont;
    }

    
    /**
     * This method does nothing. It is only implemented to comply with the GUIComponent interface.
     *
     */
    public void setContainer(GUIContainer container)
    {
    }


    /**
     * This method does nothing. It is only implemented to comply with the GUIComponent interface.
     *
     */
    public GUIContainer getContainer()
    {
	return null;
    }


    /**
     * Sets a reference to the main application. Also updates the main app of its children int the tree structure.
     *
     * @param app	The main GUIBuilderApp application.
     */
    public void setMainApp (GUIBuilderApp app)
    {
	this.app = app;
	for (int i=0; i<position.length; i++)
	    position[i].setMainApp(app);
    }


     /**
     * Sets the name of the layout.
     *
     * @param name	The name of the layout.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    
     /**
     * Returns the name of the layout.
     *
     * @return	The name of the layout.
     */
    public String getName()
    {
	return name;
    }


    /**
     * Sets the higlight of all its children. Used when a whole layout is selected.
     *
     * @param state The state to be set.
     */
    public void setHighlight (boolean state)
    {
	for (int i=0; i<position.length; i++)
	    position[i].setHighlight(state);
    }


    /**
     * Returns the constraints of the specified component.
     * Possible values are "Center", "South", "West", "North" and "Easst".
     *
     * @param  component The component to get the constraints from.
     * @return String describing the constraints of the component.
     */
    public String getConstraints(GUIComponent component)
    {
        for(int i = 0 ; i < position.length ; i++)
        {
            GUIComponent cmp = position[i].getComponent();
            if(cmp!=null)
                if(cmp.equals(component))
                    return posName[i];
        }
        return "";
    }

    
     /**
     * Add a new component to the layout with the specified constraints.
     * 
     * @param component   The component to be added.
     * @param component   The constraints to be used.
     */
    public void addGUIComponent (Object component, Object constraints)
    {
	((GUIComponent)component).setParent(this);
	for (int i=0; i<posName.length; i++)
	    if (constraints.equals(posName[i]))
	    {
		if (position[i]!= null)
		    ((Container)parent).remove(position[i]);
		position[i] = new GUIContainer((GUIComponent)component, this, structCont, app);
		((Container)parent).add(position[i], posName[i]);
	    }
    }


    /**
     * Add a new component to the layout. The component will be placed in the "Center" position. 
     * 
     * @param component    The component to be added.
     */
    public void addGUIComponent (GUIComponent component)
    {
	addGUIComponent (component, "Center");
    }


     /**
     * Returns whether the number of components is fixed in this layout. Always returns true.
     *
     * @return	true.
     */
    public boolean hasFixedSize()
    {
	return true;
    }


    /**
    * Returns the number of the components in this layout.
    *
    * @return number of components.
    */
    public int getGUIComponentCount()
    {
        int count=0;
        for(int i = 0 ; i < position.length ; i++)
            if(position[i].getComponent()!= null )
                count++;
        return count;
    }


    /**
    * Returns an array of the GUIComponents in this layout.
    *
    * @return Array of GUIComponents.
    */
    public GUIComponent[] getGUIComponents()
    {
        GUIComponent[] components = new GUIComponent[position.length];
        int count = 0;
        GUIComponent cmp;
        
        // safe
        for(int i = 0 ; i < position.length ; i++)
        {
            cmp = position[i].getComponent();
            if(cmp!=null)
            {
                components[count] = cmp;
                count++;
            }
        }
        return components;
    }
    
    
     /**
     * Removes this component from the tree structure. Also removes its children.
     */
    public void delete()
    {
	for (int i=0; i<position.length; i++)
	    if (position[i].getComponent()!=null)
		position[i].getComponent().delete();
	if (parent!=null)
	    parent.deleteChild(this);
    }


    /**
     * Removes the specified component from this layout.
     *
     * param component   The component to be deleted.
     */
    public void deleteChild(GUIComponent component)
    {
	for (int i=0; i<position.length; i++)
	    if ((position[i].getComponent()!=null) && position[i].getComponent().equals(component))
	    {
		((Container)parent).remove(((GUIConcreteComponent)component).getContainer());
		position[i] = new GUIContainer(this, structCont, app);
		((Container)parent).add(position[i], posName[i]);
	    }
    }


    /**
     * This method initializes the GUIContainer on which components can be placed.
     * This method should be called when a GUIComponentNode changes layout.
     *
     */
    public void initContainers()
    {
        ((Container)parent).removeAll();
        for (int i=0; i<position.length; i++)
	{
	    position[i] = new GUIContainer(this, structCont, app);
	    ((Container)parent).add(position[i], posName[i]);
	}
    }

    
     /**
     * Generates the Java code used to make this layouts and its components.
     *
     * @return	The Java code used to generate this layout and its components.
     */
    public ComponentCode generateCode()
    {
	ComponentCode code = new ComponentCode ();
	ComponentCode childCode;
	StringBuffer initCode = new StringBuffer();
	StringBuffer addCode = new StringBuffer();

	code.addCreation (((GUIComponentNormalNode)parent).getQualifier()+"setLayout (new BorderLayout (");
        if(getHgap()!= 0 || getVgap()!=0)
            code.addCreation(""+getHgap()+","+getVgap());
        code.addCreation("));\n\n");
	for (int i=0; i<position.length; i++)
	    if (position[i].getComponent()!=null)
	    {
		childCode = position[i].getComponent().generateCode();
		code.addGlobal (childCode.getGlobalCode());
		initCode.append (childCode.getCreationCode());
		addCode.append (((GUIComponentNormalNode)parent).getQualifier()+"add ("+childCode.getUnreferenceableCode()+", \""+posName[i]+"\");\n");
	    }
	code.addCreation(initCode.toString()+"\n"+addCode.toString());

	return code;
    }


     /**
     * Makes a panel with this layout containing the components in the layout. This is used for the preview function, since a
     * component can only be shown in one container.
     *
     * @return	A Panel with this layout and components of this layout.
     *
     * @see StructureContainer#preview()
     */
    public Component display()
    {
	Panel preview = new Panel(new BorderLayout(getHgap(),getVgap()));
	for (int i=0; i<posName.length; i++)
	    if (position[i].getComponent()!=null)
		preview.add(position[i].getComponent().display(), posName[i]);
	return (preview);
    }


    /**
     * Shows the layout property dialog. This method will not return until the
     * dialog is closed.
     *
     * @see javablue.GUILayoutPropertyDialog
     */
    public void showPropertiesDialog()
    {
        GUILayoutPropertyDialog propertyDialog = new GUILayoutPropertyDialog(app,this,structCont,(GUIComponentNormalNode)parent);
        
    }


    
            
}
