package bluej.guibuilder;

import java.awt.*;
import java.util.Vector;


/**
 * A class representing a Panel.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class GUIPanel extends Panel implements GUIComponentNormalNode
{
    private GUIComponentNode parent = null;
    private GUIComponentLayoutNode layout = null;
    private static int counter = 0;
    private transient GUIBuilderApp app = null;
    private StructureContainer structCont = null;
    private GUIContainer container = null;
    private ComponentDescriptor componentDescriptor = new ComponentDescriptor(this);
    

    /**
     * Constructs a GUIPanel.
     *
     * @param parent	    The component's parent in the tree structure.
     * @param structCont    The StructureContainer containing the component.
     * @param app	    The main application.
     */
    public GUIPanel (GUIComponentNode parent, StructureContainer structCont, GUIBuilderApp app)
    {
	super();
	this.parent = parent;
	this.structCont = structCont;
	this.app = app;
	setName("Panel"+counter);
	counter++;
    }


    /**
     * Sets the parent of this component.
     *
     * @param parent	The component's parent in the tree structure.
     */
    public void setParent (GUIComponentNode parent)
    {
	this.parent = parent;
    }


    /**
     * Returns the parent node in the tree structure of this component.
     * This is either a GUILayout or a GUIScrollPane.
     *
     * @return	The parent of this component.
     */
    public GUIComponentNode getTreeParent()
    {
        return parent;
    }


    /**
     * Sets a reference to the StructureContainer that contains the tree containing
     * this component. Furthermore it calls the equivalent method in its layout.
     *
     * @param	The container containing this component.
     */
    public void setStructureContainer (StructureContainer structCont)
    {
	this.structCont = structCont;
	if (layout!=null)
	    layout.setStructureContainer(structCont);
    }


    /**
     * Returns the StructureContainer that contains the tree containing this component.
     *
     * @return	The container containing this component.
     */
    public StructureContainer getStructureContainer ()
    {
	return structCont;
    }


    /**
     * Sets a reference to the main application.
     *
     * @param	The main GUIBuilderApp application.
     */
    public void setMainApp (GUIBuilderApp app)
    {
	this.app = app;
	if (layout!=null)
	    layout.setMainApp(app);
    }


    /**
     * Sets a reference to the GUIContainer that contains this component.
     *
     * @param container	The GUIContainer that contains this component.
     */
    public void setContainer(GUIContainer container)
    {
	this.container = container;
    }


    /**
     * Returns the GUIContainer that contains this component.
     *
     * @return	The GUIContainer that contains this component.
     */
    public GUIContainer getContainer()
    {
	return container;
    }
    

    /*
     * This method is used to set the Layout of the GUIPanel. The initContainers method
     * in the layout is invoked from here.
     * @param layout The GUIComponentLayoutNode to be used.
     */
    public void setGUILayout(GUIComponentLayoutNode layout)
    {
	super.setLayout((LayoutManager)layout);
	this.layout = layout;
	layout.initContainers();
    }


    /*
     * Returns the layout currently used in the GUIPanel.
     * @return The GUIComponentLayoutNode currently used.
     */
    public GUIComponentLayoutNode getGUILayout ()
    {
	return layout;
    }


    /*
     * Returns the qualifier of the container, that is the name of the container.
     * It is used when the code is generated, so that the components can be added to the
     * right container.
     *
     * @return The qualifier of the container. 
     */
    public String getQualifier()
    {
	return (getName()+".");
    }


    /**
     * Returns the ComponentDescriptor of this component.
     * This descriptor contains the elements common to all GUIComponents.
     *
     * @return	The ComponentDescriptor for this component.
     */
    public ComponentDescriptor getComponentDescriptor()
    {
        return componentDescriptor;
    }


    /**
     * Adds a GUIComponent to this container, furthermore the GUIComponent is stored as
     * a child of the current GUIComponentLayoutNode.
     *
     * @param component The GUIComponent to be added to the container.
     *
     * @see bluej.GUIComponentLayoutNode@addGUIComponent(GUIComponent component)
     */
    public void add(GUIComponent component)
    {
        super.add((Component)component);
	layout.addGUIComponent(component);
    }


    /**
     * Adds a GUIComponent to this container with the specified constraints.
     * Furthermore the GUIComponent is stored as a child
     * of the current GUIComponentLayoutNode with the specified constraints.
     *
     * @param component The GUIComponent to be added to the container.
     * @param constraints The constraints to be used.
     *
     * @see bluej.guibuilder.GUIComponentLayoutNode@addGUIComponent(GUIComponent component, Object constraints)
     */
    public void add(GUIComponent component, Object constraints)
    {
        super.add((Component)component, constraints);
        layout.addGUIComponent(component, constraints);
    }


    /**
     * Removes the GUIComponentLayoutNode used as layout in the Panel. Furthermore
     * removeAll from the super class Contaiener is called.
     *
     * @see java.awt.Container#removeAll()
     */
    public void removeLayout()
    {
	layout = null;
	removeAll();
    }


    /**
     * Removes this component from the tree structure.
     */
    public void delete()
    {
	if (parent!=null)
	    parent.deleteChild(this);
    }


    /*
     * Removes the specified GUIComponent from this container. In this case the
     * GUIComponent should always be a layout as this is the only type of children
     * a GUIPanel can have.
     * @param component The GUIComponent to be removed.
     */
    public void deleteChild(GUIComponent component)
    {
	if (component.equals(layout))
	    layout = null;
    }


    /**
     * Generates the Java code used to make this component.
     *
     * @return	The Java code used to generate this component.
     */
    public ComponentCode generateCode()
    {
        ComponentCode code = new ComponentCode();
	int initlevel = componentDescriptor.getInitLevel();

        StringBuffer initCode = new StringBuffer ("new Panel ()");
	
	if (initlevel==ComponentCode.UNREFERENCEABLE)
	    code.addUnreferenceable(initCode.toString());
	else
	{
	    String variableCode = new String("Panel "+getName()+" = ");
	    code.addUnreferenceable(getName());
	    if (initlevel==ComponentCode.CLASSLEVEL)
		code.addGlobal(variableCode+initCode.toString()+";\n");
	    else
		code.addCreation(variableCode+initCode.toString()+";\n");
            code.addCreation(componentDescriptor.getDescriptionCode(getName()));
            ComponentCode childCode = layout.generateCode ();
            
            code.addCreation (childCode.getCreationCode()+"\n");
            code.addGlobal (childCode.getGlobalCode()+"\n");
	}
        return code;
    }


    /**
     * Makes a copy of this component. This is used for the preview function, since a
     * component can only be shown in one container.
     *
     * @return	A copy of this component.
     *
     * @see StructureContainer#preview()
     */
    public Component display()
    {
	Panel preview = new Panel(new BorderLayout());
	preview.add(layout.display());
        componentDescriptor.cloneComponent(preview);
	return preview;
    }


    /**
     * Shows the property dialog of this component. This method will not return until the
     * dialog is closed.
     *
     * @see javablue.GUIButtonPropertyDialog
     */
    public void showPropertiesDialog()
    {
        GUIPanelPropertyDialog propertyDialog = new GUIPanelPropertyDialog(app,this,"Panel",structCont);
        
    }
}
