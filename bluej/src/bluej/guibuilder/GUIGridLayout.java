package javablue.GUIBuilder;

import java.awt.*;
import java.util.Vector;
import java.util.Enumeration;


/**
 * A class representing a GridLayout.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class GUIGridLayout extends GridLayout implements GUIComponentLayoutNode
{
    private GUIComponentNode parent = null;
    private Vector position = new Vector(10,5);
    private static int counter = 0;
    private transient GUIBuilderApp app = null;
    private StructureContainer structCont = null;
    private String name = new String();


     /**
     * Constructs a GUIGridLayout.
     *
     * @param parent	    The component's parent in the tree structure.
     * @param structCont    The StructureContainer containing the component.
     * @param app	    The main application.
     */
    public GUIGridLayout(GUIComponentNode parent,StructureContainer structCont,GUIBuilderApp app)
    {
	super();
	name = "gridlayout"+counter;
	counter++;
        this.parent = parent;
        this.structCont = structCont;
        this.app =app;
    }


     /**
     * Constructs a GUIBorderLayout.
     *
     * @param row           The number of rows in the grid.
     * @param column        The number of columns in the grid.
     * @param parent	    The component's parent in the tree structure.
     * @param structCont    The StructureContainer containing the component.
     * @param app	    The main application.
     */
    public GUIGridLayout(int row,int column,GUIComponentNode parent,StructureContainer structCont,GUIBuilderApp app)
    {
	super(row,column);
	name = "gridlayout"+counter;
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
        return (GUIComponentNode)parent;
    }


     /**
     * Sets a reference to the StructureContainer that contains the tree containing this layout. Also updates the StrucutreContainer of its children.
     *
     * @param structCont  The StructureContainer containing layout.
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
     * @param app  The main GUIBuilderApp application.
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
    public void setName(String  v)
    {
        this.name = v;
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
     * Add a new component to the layout. The specified constraints will be ignored.
     * 
     * @param component   The component to be added.
     * @param constraints   The constraints.
     */
    public void addGUIComponent (Object component, Object constraints)
    {
        addGUIComponent((GUIComponent)component);
	    
    }


    /**
     * Add a new component to the layout. The component will be placed in the first avaible position. 
     * 
     * @param component   The component to be added.
     */
    public void addGUIComponent (GUIComponent component)
    {
        ((GUIComponent)component).setParent(this);
        boolean bFound = false;
        int index = 0;
        
        for(int i = 0 ; i < position.size() ; i++)
            if(((GUIContainer)position.elementAt(i)).getComponent() == null)
            {
                bFound = true;
                ((GUIContainer)position.elementAt(i)).setComponent(component);
                index = i;
                break;
            }
        if(bFound == false)
        {
            position.addElement(new GUIContainer((GUIComponent)component,this,structCont,app));
            index = position.size() - 1;
            ((Container)parent).add((GUIContainer)position.elementAt(index));
        }
	
        
    }


    /**
     * Returns whether the number of components is fixed in this layout. ALways returns true.
     *
     * @return	true.
     */
    public boolean hasFixedSize ()
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
        int count = 0;
        for(int i = 0 ; i < position.size() ; i++)
            if(((GUIContainer)position.elementAt(i)).getComponent() != null)
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
     * Adjusts the number of GUIContainers int the layout, so that it complies.
     * with the number of rows and columns.
     * Should be called after the number of rows or columns have changed.
     *
     */
    public void adjustContainers()
    {
        int sum = getRows() * getColumns();
        
        GUIContainer container;
        if(sum > position.size())
            for(int i = position.size() ; i < sum ; i++)
            {
                container = new GUIContainer(this,structCont,app);
                position.addElement(container);
                ((Container)parent).add(container);
            }
        else if(sum < position.size())
        {
            int i = position.size()-1;
            while(((GUIContainer)position.elementAt(i)).getComponent()==null && i >= sum)
            {
                container = (GUIContainer)position.elementAt(i);
                ((Container)parent).remove(container);
                position.removeElementAt(i);
                i--;
                    
            }
                
        }
        
    }


    /**
     * This methods makes sure that only one empty GUIContainer int the layout can be added into.
     */
    public void setValidAddPosition()
    {
        GUIContainer container;
        boolean first = true;
        for(int i = 0 ; i < position.size(); i++)
            {
                container = (GUIContainer)position.elementAt(i);
                if(container.getComponent() != null)
                    container.setValidAddPosition(false);
                else
                {
                    if(first)
                    {
                        container.setValidAddPosition(true);
                        first = false ;
                    }
                    else
                        container.setValidAddPosition(false);
                }
            }
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
        GUIContainer container = new GUIContainer(this,structCont,app);
        position.addElement(container);
        ((Container)parent).add(container);
        
    }


    /**
     * This method initializes the GUIContainer on which components can be placed.
     * This method should be called when a GUIComponentNode changes layout.
     *
     */
    public void initContainers()
    {
	GUIContainer container;
        position.removeAllElements();
        ((Container)parent).removeAll();
        for (int i=0; i<(getRows()*getColumns()); i++)
        {
            container = new GUIContainer(this, structCont, app);
            position.addElement(container);
            ((Container)parent).add(container);
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

	code.addCreation (((GUIComponentNormalNode)parent).getQualifier()+"setLayout (new GridLayout ("+getRows()+","+getColumns());
        if(getHgap()!= 0 || getVgap()!=0)
            code.addCreation(""+getHgap()+","+getVgap());
        code.addCreation("));\n\n");

       
	for (int i=0; i<position.size(); i++)
	    if (position.elementAt(i)!=null && ((GUIContainer)position.elementAt(i)).getComponent()!=null)
	    {
		childCode = ((GUIComponent)((GUIContainer)(position.elementAt(i))).getComponent()).generateCode();
		code.addGlobal (childCode.getGlobalCode());
		initCode.append (childCode.getCreationCode());
		addCode.append (((GUIComponentNormalNode)parent).getQualifier()+"add ("+childCode.getUnreferenceableCode()+");\n");
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
        Panel preview = new Panel(new GridLayout(getRows(), getColumns(),getHgap(),getVgap()));
	GUIComponent tmpComp;
	for (Enumeration enum = position.elements(); enum.hasMoreElements();)
	{
	    tmpComp = ((GUIContainer)enum.nextElement()).getComponent();
	    if (tmpComp!=null)
		preview.add(tmpComp.display());
	}
	return preview;
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
