package javablue.GUIBuilder;

import java.awt.*;
import java.util.Vector;
import java.util.Enumeration;


/**
 * A class representing a GridBagLayout.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class GUIGridBagLayout extends GridBagLayout implements GUIComponentLayoutNode
{
    private GUIComponentNode parent = null;
    private Vector position = new Vector(10,5);
    private transient GUIBuilderApp app = null;
    private int row = 1;
    private int column = 1;
    private String name = new String();
    private static int counter = 0;
    StructureContainer structCont;
    private GridBagConstraintsCodeGenerator guiconstraints = new GridBagConstraintsCodeGenerator();


    /**
     * Constructs a GUIGridBagLayout.
     *
     * @param parent	    The component's parent in the tree structure.
     * @param structCont    The StructureContainer containing the component.
     * @param app	    The main application.
     */
    public GUIGridBagLayout(GUIComponentNode parent,StructureContainer structCont,GUIBuilderApp app)
    {
	super();
	name = "gridbaglayout"+counter;
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
     * @param	structCont The StructureContainer containing layout.
     */
    public void setStructureContainer (StructureContainer structCont)
    {
	this.structCont = structCont;
	for (Enumeration enum = position.elements(); enum.hasMoreElements();)
	{
	    ((GUIContainer)enum.nextElement()).setStructureContainer(structCont);
	}
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
     * @param app   The main GUIBuilderApp application.
     */
    public void setMainApp (GUIBuilderApp app)
    {
	this.app = app;
	for (Enumeration enum = position.elements(); enum.hasMoreElements();)
	{
	    ((GUIContainer)enum.nextElement()).setMainApp(app);
	}
    }


     /**
     * Sets the name of the layout.
     *
     * @param name  The name of the layout.
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
	for (Enumeration enum = position.elements(); enum.hasMoreElements();)
	{
	    ((GUIContainer)enum.nextElement()).setHighlight(state);
	}
    }


     /**
     * Add a new component to the layout with the specified constraints.
     * 
     * @param component	   The component to be added.
     * @param constraints   The constraints to be used.
     */
    public void addGUIComponent (Object component, Object constraints)
    {
        ((GUIComponent)component).setParent(this);

        ((GUIContainer)position.lastElement()).setComponent((GUIComponent)component);
        setConstraints((Component)position.elementAt(position.size()-1),(GridBagConstraints)constraints);
        GUIContainer tmpCont = new GUIContainer(this, structCont, app);
	((Container)parent).add(tmpCont);
	position.addElement(tmpCont);
        
    }

    
     /**
     * Add a new component to the layout. The component will be added with a new GridBagConstraints object as constraints. 
     * 
     * @param	component The component to be added.
     */
    public void addGUIComponent (GUIComponent component)
    {
        addGUIComponent(component,new GridBagConstraints());
    }


     /**
     * Returns whether the number of components is fixed in this layout. ALways returns false.
     *
     * @return	false.
     */
    public boolean hasFixedSize()
    {
	return false;
    }


     /**
    * Returns the number of the components in this layout.
    *
    * @return number of components.
    */
    public int getGUIComponentCount()
    {
        return position.size()-1;
    }


    /**
    * Returns an array of the GUIComponents in this layout.
    *
    * @return Array of GUIComponents.
    */
    public GUIComponent[] getGUIComponents()
    {
        GUIComponent[] components = new GUIComponent[position.size()];
        int count = 0;
        GUIComponent cmp;
        
        for(int i=0 ; i < position.size() ; i++)
        {
            cmp = ((GUIContainer)position.elementAt(i)).getComponent();
            if(cmp!=null)
            {
                components[count] = cmp;
                count++;
            }
        }
        return components;
    }
    

    /**
     * Returns the constraints of the component.
     *
     * @param A GUIComponent
     * @return The constraints of the component.
     */
    public GridBagConstraints getConstraints(GUIComponent component)
    {
        return super.getConstraints(((GUIConcreteComponent)component).getContainer());
    }
    

    /**
     * Removes this component from the tree structure. Also removes its children.
     */
    public void delete()
    {
	for (int i=0; i<position.size(); i++)
	    if (position.elementAt(i)!=null)
		((GUIComponent)position.elementAt(i)).delete();
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
	position.removeElement(((GUIConcreteComponent)component).getContainer());
        ((Container)parent).remove(((GUIConcreteComponent)component).getContainer());
    }


    /**
     * This method initializes the GUIContainer on which components can be placed.
     * This method should be called when a GUIComponentNode changes layout.
     *
     */
    public void initContainers()
    {
        position.removeAllElements();
        ((Container)parent).removeAll();
        GUIContainer container = new GUIContainer(this, structCont, app);
        position.addElement (container);
        ((Container)parent).add(container);
        
        
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
        guiconstraints.setName("constraints"+((GUIComponentNormalNode)parent).getQualifier());
        // avoid duplicate names of constraints 
	code.addCreation ("GridBagLayout "+name+" = new GridBagLayout ();\n\n");
	code.addCreation (((GUIComponentNormalNode)parent).getQualifier()+"setLayout("+name+");\n\n");
        GUIContainer tmpContainer;
        GUIComponent tmpComp;
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagConstraints gbcOld = null;
        for (int i=0; i<position.size(); i++)
            {
                tmpContainer = ((GUIContainer)position.elementAt(i));
                gbc = getConstraints(tmpContainer);
                tmpComp = tmpContainer.getComponent();
                if (tmpComp!=null)
                    {
                        childCode = tmpComp.generateCode();
                        code.addGlobal (childCode.getGlobalCode());
                        initCode.append (childCode.getCreationCode());
                        addCode.append(guiconstraints.getCode(gbc,gbcOld));
                        addCode.append (((GUIComponentNormalNode)parent).getQualifier()+"add ("+childCode.getUnreferenceableCode()+","+guiconstraints.getName()+");\n");
                        gbcOld = gbc;
                        
                    }
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
	Panel preview = new Panel(new GridBagLayout());
        GUIComponent cmp;
	for (int i=0; i<position.size(); i++)
	{
            cmp = ((GUIContainer)position.elementAt(i)).getComponent();
            if(cmp!=null)
                preview.add(cmp.display(),getConstraints((GUIContainer)position.elementAt(i)));
        }
        return preview;
        
    }


     /**
     * Shows the layout property dialog. This method will not return until the
     * dialog is closed.
     *
     * @see GUILayoutPropertyDialog
     */
    public void showPropertiesDialog()
    {
        GUILayoutPropertyDialog propertyDialog = new GUILayoutPropertyDialog(app,this,structCont,(GUIComponentNormalNode)parent);
        
    }



   
    
            
  
    
   
   
    


    

    
}
