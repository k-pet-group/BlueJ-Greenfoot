package javablue.GUIBuilder;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;


/**
 * A class representing a Frame.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class GUIFrame extends Frame implements GUIComponentNormalNode
{
    private GUIComponentNode parent = null;
    private GUIComponentLayoutNode layout = null;
    private static int counter = 0;
    private transient GUIBuilderApp app = null;
    private StructureContainer structCont = null;
    private ComponentDescriptor componentDescriptor = new ComponentDescriptor(this);
    private GUIMenuBar menubar = null;


    /**
     * Constructs a GUIFrame.
     *
     * @param parent	    The component's parent in the tree structure.
     * @param structCont    The StructureContainer containing the component.
     * @param app	    The main application.
     */
    public GUIFrame (GUIComponentNode parent,StructureContainer structCont,GUIBuilderApp app)
    {
	super();
	setName("Frame"+counter);
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
     * This should always be null as a GUIFrame does not have a parent in the tree structure.
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
    public void setStructureContainer(StructureContainer structCont)
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
    public void setMainApp(GUIBuilderApp app)
    {
	this.app = app;
	if (layout!=null)
	    layout.setMainApp(app);
	if (menubar!=null)
	    menubar.setMainApp(app);
    }


    /**
     * Sets a reference to the GUIContainer that contains this component.
     * This method does not actually do anything as a GUIFrame is not in a GUIContainer
     * @param container	The GUIContainer that contains this component.
     */
    public void setContainer(GUIContainer container)
    {
    }


    /**
     * Returns the GUIContainer that contains this component.
     * It always returns null as a GUIFrame is not in a GUIContainer
     * @return	The GUIContainer that contains this component.
     */
    public GUIContainer getContainer()
    {
	return null;
    }


    /*
     * This method is used to set the Layout of the GUIFrame. The initContainers method
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
     * Returns the layout currently used in the GUIFrame.
     * @return The GUIComponentLayoutNode currently used.
     */
    public GUIComponentLayoutNode getGUILayout()
    {
	return layout;
    }


    /*
     * Returns the qualifier of the container, that is the name of the container.
     * It is used when the code is generated, so that the components can be added to the
     * right container. In this case the method always return "" as a GUIFrame is
     * always the uppermost component in the tree structure.
     * @return The qualifier of the container. Always ""
     */
    public String getQualifier()
    {
	return "";
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
     * Returns the GUIMenubar of the GUIFrame. It returns null if there is none.
     *
     * @return The GUIMenuBar of the GUIFrame.
     */
    public GUIMenuBar getGUIMenuBar()
    {
	return menubar;
    }

    /**
     * Adds a GUIComponent to this container, furthermore the GUIComponent is stored as
     * a child of the current GUIComponentLayoutNode.
     *
     * @param component The GUIComponent to be added to the container.
     *
     * @see GUIComponentLayoutNode@addGUIComponent(GUIComponent component)
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
     * @see GUIComponentLayoutNode@addGUIComponent(GUIComponent component, Object constraints)
     */
    public void add(GUIComponent component, Object constraints)
    {
        super.add((Component)component, constraints);
        layout.addGUIComponent(component, constraints);
    }
            

    /**
     * Adds a menubar to the Frame. Various item can later be added to this menu.
     * via the property dialog.
     *
     */
      
    public void addMenuBar()
    {
	if (menubar==null)
	{
	    menubar = new GUIMenuBar(this, structCont, app);
	    setMenuBar(menubar);
	}
    }


    /**
     * Removes the menubar from the Frame. All the items are removed.
     *
     */
    public void removeMenuBar()
    {
	menubar = null;
	setMenuBar(null);
    }

    
    /**
     * Removes the GUIComponentLayoutNode used as layout in the Frame. Furthermore
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
     * Removes the entire tree structure.
     */
    public void delete()
    {
	structCont.delete();
    }


    /*
     * Removes the specified GUIComponent from this container. In this case the
     * GUIComponent should always be a layout as this is the only type of children
     * a GUIFrame can have.
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
	ComponentCode code = new ComponentCode ();
	ComponentCode childCode = layout.generateCode ();
	ComponentCode listenerCode = structCont.getListenerContainer().generateCode();
	ComponentCode checkboxCode = structCont.getCheckboxGroupContainer().generateCode();

	code.addGlobal ("import java.awt.*;\n");
	code.addGlobal (listenerCode.getGlobalCode()+"\n");
	code.addGlobal (checkboxCode.getGlobalCode()+"\n");
	code.addCreation ("class "+getName()+" extends Frame\n");
	code.addCreation ("{\n");
        code.addCreation (listenerCode.getCreationCode());
	code.addCreation (childCode.getGlobalCode()+"\n");
	code.addCreation ("public void createInterface ()\n");
	code.addCreation ("{\n");
	if (menubar!=null)
	    code.addCreation (menubar.generateCode().getCreationCode()+"\n");
	code.addCreation (childCode.getCreationCode());

	for(int i=0 ; i<(componentDescriptor.getListenerVector()).size() ; i++)
	    code.addCreation(((ListenerPair)(componentDescriptor.getListenerVector()).elementAt(i)).getAddFunction()+";\n");

        code.addCreation(componentDescriptor.getDescriptionCode(getName()));
        code.addCreation ("}\n}\n");

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
	Frame preview = new Frame("Preview");
	preview.addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { ((Frame)e.getSource()).dispose(); } } );
	preview.add(layout.display(), "Center");
        componentDescriptor.cloneComponent(preview);
	if (menubar!=null)
	    preview.setMenuBar((MenuBar)menubar.display());
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
        GUIFramePropertyDialog propertyDialog = new GUIFramePropertyDialog(app,this,"Frame",structCont);
        
    }

    

    
    

}
