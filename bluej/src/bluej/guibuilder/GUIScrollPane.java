package javablue.GUIBuilder;

import java.awt.*;
import java.util.Vector;


/**
 * A class representing a ScrollPane.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class GUIScrollPane extends ScrollPane implements GUIComponentNormalNode
{
    private GUIComponentNode parent = null;
    private GUIContainer child = null;
    private static int counter = 0;
    private transient GUIBuilderApp app = null;
    private StructureContainer structCont = null;
    private GUIContainer container = null;
    private ComponentDescriptor componentDescriptor = new ComponentDescriptor(this);
    

    /**
     * Constructs a GUIScrollPane.
     *
     * @param parent	    The component's parent in the tree structure.
     * @param structCont    The StructureContainer containing the component.
     * @param app	    The main application.
     */
    public GUIScrollPane (GUIComponentNode parent, StructureContainer structCont, GUIBuilderApp app)
    {
	super(ScrollPane.SCROLLBARS_ALWAYS);
	this.parent = parent;
	this.structCont = structCont;
	this.app = app;
	setName("ScrollPane"+counter);
	counter++;
    }


    /**
     * Constructs a GUIScrollPane.
     *
     * @param scrollbarPolicy  The visibility of the scrollbars. Possible values are:
     * ScrollPane.SCROLLBARS_WHEN_NEEDED, ScrollPane.SCROLLBARS_ALWAYS, ScrollPane.SCROLLBARS_NEVER.
     * @param parent	    The component's parent in the tree structure.
     * @param structCont    The StructureContainer containing the component.
     * @param app	    The main application.
     */
    public GUIScrollPane (int scrollbarPolicy, GUIComponentNode parent, StructureContainer structCont, GUIBuilderApp app)
    {
	super(scrollbarPolicy);
	this.parent = parent;
	this.structCont = structCont;
	this.app = app;
	setName("ScrollPane"+counter);
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
	child = new GUIContainer(this, structCont, app);
	super.add(child);
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
     * this component. Furthermore it calls the equivalent method in its child.
     *
     * @param	The container containing this component.
     */
    public void setStructureContainer (StructureContainer structCont)
    {
	this.structCont = structCont;
	if (child!=null)
	    child.setStructureContainer(structCont);
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
	if (child!=null)
	    child.setMainApp(app);
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
     * This method is used to set the Layout of the GUIScrollPane.
     * This method does not actually do anything as a ScrollPane does not have a layout.
     * @param layout The GUIComponentLayoutNode to be used. It is ignored.
     */
    public void setGUILayout (GUIComponentLayoutNode layout)
    {
    }


    /**
     * Returns the layout currently used in the GUIScrollPane.
     * It always returns null as a ScrollPane does not have a layout.
     * @return The GUIComponentLayoutNode currently used. Always null.
     */
    public GUIComponentLayoutNode getGUILayout()
    {
	return null;
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
     * Returns the single child of this container.
     *
     * @return The GUIComponent child of this container.
     */
    public GUIComponent getChild()
    {
        return child.getComponent();
    }

    
    /**
     * Adds a GUIComponent to this container. 
     *
     * @param component The GUIComponent to be added to the container.
     *
     */
    public void add(GUIComponent component)
    {
        child.setComponent(component);
    }


    /**
     * Adds a GUIComponent to this container ignoring the specified constraints.
     * This method calls the add(GUIComponent component) function in this class.
     *
     * @param component The GUIComponent to be added to the container.
     * @param constraints The constraints to be ignored.
     *
     * @see GUIScrollPane@add(GUIComponent component)
     */
    public void add(GUIComponent component, Object constraints)
    {
        add(component);
    }


    /**
     * Removes the GUIComponentLayoutNode used as layout in the ScrollPane.
     * This method does not actually do anything as a ScrollPane does not have a layout.
     *
     */
    public void removeLayout()
    {
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
     * GUIComponent should always be a GUIConcreteComponent as this is the only type of child
     * a GUIScrollPane can have.
     * @param component The GUIComponent to be removed.
     */
    public void deleteChild(GUIComponent component)
    {
	if (child.getComponent()!=null && child.getComponent().equals(component))
	{
	    remove(((GUIConcreteComponent)component).getContainer());
	    child = new GUIContainer(this, structCont, app);
	    add(child);
	}
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

        StringBuffer initCode = new StringBuffer ("new ScrollPane(");
	if(getScrollbarDisplayPolicy()!=ScrollPane.SCROLLBARS_AS_NEEDED)
        {
            if(getScrollbarDisplayPolicy() == ScrollPane.SCROLLBARS_ALWAYS)
                initCode.append("ScrollPane.SCROLLBARS_ALWAYS");
            else
                initCode.append("ScrollPane.SCROLLBARS_NEVER");
        }
        initCode.append(")");
        if (initlevel==ComponentCode.UNREFERENCEABLE)
	    code.addUnreferenceable(initCode.toString());
	else
	{
	    String variableCode = new String("ScrollPane "+getName()+" = ");
	    code.addUnreferenceable(getName());

	    if (initlevel==ComponentCode.CLASSLEVEL)
		code.addGlobal(variableCode+initCode.toString()+";\n");
	    else
		code.addCreation(variableCode+initCode.toString()+";\n");
            code.addCreation(componentDescriptor.getDescriptionCode(getName()));
	    if (child.getComponent()!=null)
	    {
        	ComponentCode childCode = child.getComponent().generateCode ();
		code.addGlobal (childCode.getGlobalCode());
        	code.addCreation (childCode.getCreationCode()+"\n");
		code.addCreation (getQualifier()+"add ("+childCode.getUnreferenceableCode()+");\n");
	    }
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
	ScrollPane preview = new ScrollPane(getScrollbarDisplayPolicy());
	if (child.getComponent()!=null)
	    preview.add(child.getComponent().display());
        componentDescriptor.cloneComponent(preview);
        return preview;
    }


    /**
     * Shows the property dialog of this component. This method will not return until the
     * dialog is closed.
     *
     * @see GUIButtonPropertyDialog
     */
    public void showPropertiesDialog()
    {
        GUIScrollPanePropertyDialog propertyDialog = new GUIScrollPanePropertyDialog(app,this,"ScrollPane",structCont);
        
    }
}
