package javablue.GUIBuilder;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.io.Serializable;


/**
 * A class reperesenting a Scrollbar.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIScrollbar extends Scrollbar implements GUIComponentLeaf
{
    private GUIComponentNode parent = null;
    private static int counter = 0;
   
    private boolean changedValues = false;
    private boolean changedOrientation = false;
    private transient GUIBuilderApp app;
    private StructureContainer structCont = null;
    private GUIContainer container = null;
    private ComponentDescriptor componentDescriptor = new ComponentDescriptor(this);
    private GUIScrollbar thisScrollbar;
    
    GUIAdjustmentListener gm = new GUIAdjustmentListener();


    /**
     * Constructs a vertical GUIScrollbar.
     *
     * @param parent	    The component's parent in the tree structure.
     * @param structCont    The StructureContainer containing the component.
     * @param app	    The main application.
     */
    public GUIScrollbar(GUIComponentNode parent, StructureContainer structCont, GUIBuilderApp app)
    {
	super();
	this.parent = parent;
	this.structCont = structCont;
	this.app = app;
	thisScrollbar = this;
	setName("scrollbar"+counter);
	counter++;
	addAdjustmentListener(gm);
    }


    /**
     * Constructs a GUIScrollbar with the specified orientation.
     *
     * @param orientation   The orientation of the scroll bar.
     * @param parent	    The component's parent in the tree structure.
     * @param structCont    The StructureContainer containing the component.
     * @param app	    The main application.
     */
    public GUIScrollbar(int orientation, GUIComponentNode parent, StructureContainer structCont, GUIBuilderApp app)
    {
	super(orientation);
	this.parent = parent;
	this.structCont = structCont;
	this.app = app;
	thisScrollbar = this;
	setName("scrollbar"+counter);
	counter++;
	changedOrientation = true;
	addAdjustmentListener(gm);
    }


    /**
     * Constructs a GUIScrollbar with the specified orientation, initial value, page size, and minimum and maximum values.
     *
     * @param orientation   The orientation of the scroll bar.
     * @param value	    The initial value of the scroll bar.
     * @param visible	    The size of the scroll bar's bubble, representing the visible portion; the scroll bar uses this value when paging up or down by a page.
     * @param minimum	    The minimum value of the scroll bar.
     * @param maximum	    The maximum value of the scroll bar.
     * @param parent	    The component's parent in the tree structure.
     * @param structCont    The StructureContainer containing the component.
     * @param app	    The main application.
     */
    public GUIScrollbar(int orientation, int value, int visible, int minimum, int maximum, GUIComponentNode parent, StructureContainer structCont, GUIBuilderApp app)
    {
	super(orientation,value,visible,minimum,maximum);
	this.parent = parent;
	this.structCont = structCont;
	this.app = app;
	thisScrollbar = this;
	setName("scrollbar"+counter);
	changedOrientation = true;
	changedValues = true;
	counter++;
	addAdjustmentListener(gm);
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
     * this component.
     *
     * @param	The container containing this component.
     */
    public void setStructureContainer (StructureContainer structCont)
    {
	this.structCont = structCont;
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
    public void setMainApp(GUIBuilderApp app)
    {
	this.app =app;
    }


    /**
     * Sets a reference to the GUIContainer that contains this component.
     *
     * @param container The GUIContainer that contains this component.
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
     * Removes this component from the tree structure.
     */
    public void delete()
    {
	if (parent!=null)
	    parent.deleteChild(this);
    }


    /**
     * Generates the Java code used to make this component.
     *
     * @return	The Java code used to generate this component.
     */
    public ComponentCode generateCode()
    {
	ComponentCode code = new ComponentCode ();
	int initlevel = componentDescriptor.getInitLevel();

	StringBuffer initCode = new StringBuffer ("new Scrollbar (");
	if(changedOrientation)
            initCode.append (getOrientationText());
	if(changedValues)
	    initCode.append(","+getValue()+","+getVisibleAmount()+","+getMinimum()+","+getMaximum());
	initCode.append (")");

	if (initlevel==ComponentCode.UNREFERENCEABLE)
	    code.addUnreferenceable(initCode.toString());
	else
	{
	    String variableCode = new String("Scrollbar "+getName()+" = ");
	    code.addUnreferenceable(getName());
	    if (initlevel==ComponentCode.CLASSLEVEL)
		code.addGlobal(variableCode+initCode.toString()+";\n");
	    else
		code.addCreation(variableCode+initCode.toString()+";\n");

	    for(int i=0 ; i<(componentDescriptor.getListenerVector()).size() ; i++)
		code.addCreation(getName()+"."+((ListenerPair)(componentDescriptor.getListenerVector()).elementAt(i)).getAddFunction()+";\n");

	    code.addCreation(componentDescriptor.getDescriptionCode(getName()));
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
	Scrollbar preview = new Scrollbar(getOrientation(), getValue(), getVisibleAmount(), getMinimum(), getMaximum());
	componentDescriptor.cloneComponent(preview);
	preview.setBlockIncrement(getBlockIncrement());
	preview.setUnitIncrement(getUnitIncrement());
            
	return preview;
    }


    /**
     * Shows the property dialog of this component. This method will not return until the
     * dialog is closed.
     *
     * @see GUIScrollbarPropertyDialog
     */
    public void showPropertiesDialog()
    {
	GUIScrollbarPropertyDialog propertyDialog = new GUIScrollbarPropertyDialog(app,this,"Scrollbar",structCont);
    }


    /**
     * Converts the orientation value to a string.
     *
     * @return	The orientation value of this component represented as a string.
     */
    private String getOrientationText()
    {
	if(getOrientation()==Scrollbar.HORIZONTAL)
	    return "Scrollbar.HORIZONTAL";
	else
	    return "Scrollbar.VERTICAL";
    }


    /**
     * A class reperesenting a scroll bar AdjustmentListener. This is a bugfix, since
     * JDK 1.1.7 and earlier, does not handle MouseEvents on scroll bars properly.
     * This class allows the user to select the GUIScrollbar by adjusting the scroll
     * bar slider, instead of just clicking somewhere on the scroll bar.
     *
     * Created: Oct 1, 1998.
     *
     * @author Morten Knudsen & Kent Hansen
     * @version 1.0
     */
    private class GUIAdjustmentListener implements AdjustmentListener, Serializable
    {
        public void adjustmentValueChanged(AdjustmentEvent e)
        {
            container.processClick(new MouseEvent(thisScrollbar,0,0,MouseEvent.BUTTON1_MASK,0,0,1,false));
	}
    }
}
