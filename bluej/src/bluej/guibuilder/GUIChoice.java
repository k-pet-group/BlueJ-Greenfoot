package javablue.GUIBuilder;

import java.awt.*;
import java.util.Vector;


/**
 * A class reperesenting a Choice.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIChoice extends Choice implements GUIComponentLeaf
{
    private GUIComponentNode parent = null;
    private static int counter = 0;
 
    private GUIBuilderApp app;
    private StructureContainer structCont = null;
    private GUIContainer container;
    private ComponentDescriptor componentDescriptor = new ComponentDescriptor(this);
    

    /**
     * Constructs a GUIChoice.
     *
     * @param parent	    The component's parent in the tree structure.
     * @param structCont    The StructureContainer containing the component.
     * @param app	    The main application.
     */
    public GUIChoice(GUIComponentNode parent,StructureContainer structCont,GUIBuilderApp app)
    {
	super();
	setName("choice"+counter);
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

	StringBuffer initCode = new StringBuffer ("new Choice ()");
	if (initlevel==ComponentCode.UNREFERENCEABLE)
	    code.addUnreferenceable(initCode.toString());
	else
	{
	    String variableCode = new String("Choice "+getName()+" = ");

	    StringBuffer stringInit = new StringBuffer();
	    for(int i=0; i < getItemCount(); i++)
		stringInit.append(getName()+"."+"add(\""+getItem(i)+"\");\n");

	    code.addUnreferenceable(getName());
	    if (initlevel==ComponentCode.CLASSLEVEL)
	    {
		code.addGlobal(variableCode+initCode.toString()+";\n");
		code.addCreation(stringInit.toString());
	    }
	    else
	    {
		code.addCreation(variableCode+initCode.toString()+";\n");
		code.addCreation(stringInit.toString());
	    }

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
	Choice preview = new Choice();
        componentDescriptor.cloneComponent(preview);
	// Copy items in the Choice:
        for(int i = 0 ; i < getItemCount() ; i++)
            preview.add(getItem(i));
        return preview;
    }


    /**
     * Shows the property dialog of this component. This method will not return until the
     * dialog is closed.
     *
     * @see GUIChoicePropertyDialog
     */
    public void showPropertiesDialog()
    {
        GUIChoicePropertyDialog propertyDialog = new GUIChoicePropertyDialog(app,this,"Choice",structCont);
        
    }
}
