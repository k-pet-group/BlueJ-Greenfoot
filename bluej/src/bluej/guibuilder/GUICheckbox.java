package javablue.GUIBuilder;

import java.awt.*;
import java.util.Vector;


/**
 * A class reperesenting a Checkbox.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUICheckbox extends Checkbox implements GUIComponentLeaf
{
    private GUIComponentNode parent = null;
    private boolean changedGroup = false;
    private static int counter = 0;
    private boolean changedState = false;
    private transient GUIBuilderApp app;
    private StructureContainer structCont = null;
    private GUIContainer container = null;
    private String checkboxGroup = new String();
    private ComponentDescriptor componentDescriptor = new ComponentDescriptor(this);


    /**
     * Constructs a GUICheckbox with no label.
     *
     * @param parent	    The component's parent in the tree structure.
     * @param structCont    The StructureContainer containing the component.
     * @param app	    The main application.
     */
    public GUICheckbox(GUIComponentNode parent,StructureContainer structCont,GUIBuilderApp app)
    {
	super();
	setName("checkbox"+counter);
	counter++;
        this.parent = parent;
        this.structCont = structCont;
        this.app =app;
    }


    /**
     * Constructs a GUICheckbox with the specified label.
     *
     * @param label	    A string label for the checkbox.
     * @param parent	    The component's parent in the tree structure.
     * @param structCont    The StructureContainer containing the component.
     * @param app	    The main application.
     */
    public GUICheckbox(String label, GUIComponentNode parent,StructureContainer structCont,GUIBuilderApp app)
    {
	super(label);
	setName("checkbox"+counter);
	counter++;
        this.parent = parent;
        this.structCont = structCont;
        this.app =app;
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
     * Sets the name of the CheckboxGroup this component belongs to.
     * Set it to an empty string to delete the group from this checkbox.
     *
     * @param	The name of the CheckboxGroup as used in CheckboxGroupContainer.
     * @see	javablue.CheckboxGroupContainer
     */
    public void setGroup(String groupName)
    {
	checkboxGroup = groupName;
    }


    /**
     * Returns the name of the accociated CheckboxGroup.
     * An empty string is returned in case no group is set for this component.
     *
     * @return	The name of the CheckboxGroup.
     */
    public String getGroup()
    {
	return checkboxGroup;
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

	StringBuffer initCode = new StringBuffer ("new Checkbox (\""+getLabel()+"\"");
        if(!checkboxGroup.equals(""))
           initCode.append(", "+checkboxGroup);
        initCode.append(", "+getStateAsText()+")");

        if (initlevel==ComponentCode.UNREFERENCEABLE)
	    code.addUnreferenceable(initCode.toString());
	else
	{
	    String variableCode = new String("Checkbox "+getName()+" = ");

	    StringBuffer stringInit = new StringBuffer();
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
	Checkbox preview = new Checkbox(getLabel(), getCheckboxGroup(),getState());
        componentDescriptor.cloneComponent(preview);
	return preview;
    }

    /**
     * Shows the property dialog of this component. This method will not return until the
     * dialog is closed.
     *
     * @see javablue.GUICheckboxPropertyDialog
     */
    public void showPropertiesDialog()
    {
      GUICheckboxPropertyDialog propertyDialog = new GUICheckboxPropertyDialog(app,this,"Checkbox",structCont);
    }


    /**
     * Converts the state (selected/not selected) of this component to a string.
     *
     * @return	The state of this component. true means on/selected, false means off/not selected.
     */
    private String getStateAsText()
    {
	return String.valueOf(getState());
    }
}
