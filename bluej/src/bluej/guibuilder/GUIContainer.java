package bluej.guibuilder;

import java.awt.*;
import java.awt.event.*;
import bluej.guibuilder.graphics.*;
import java.io.Serializable;

/**
 * A class representing a graphical container for GUIComponents. This container
 * shows a visible border around the components. Also it is used as a
 * placeholder for empty locations in a layout.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIContainer extends Panel
{
    private transient GUIBuilderApp app;
    private StructureContainer structCont;
    private GUIComponentNode parent;
    private GUIComponent component = null;
    private PopupMenu borderMenu = new PopupMenu("Select action");
    private PopupMenu componentMenu = new PopupMenu("Select action");

    protected boolean highlighted = false;
    private boolean validAddPosition = true;

    private MouseHandler mouseHandler = new MouseHandler();
    private PopupHandler popupHandler = new PopupHandler();


    /**
     * Constructs an empty GUIContainer.
     *
     * @param parent	    The container's parent in the tree structure.
     * @param structCont    The StructureContainer containing the component.
     * @param app	    The main application.
     */
    public GUIContainer (GUIComponentNode parent, StructureContainer structCont,GUIBuilderApp app )
    {
	super();
	this.app = app;
	this.structCont = structCont;
	this.parent = parent;
	initPopupMenu();
	setBorders();
    }

    /**
     * Constructs a GUIContainer with the specified component inside.
     *
     * @param component	    The component to be placed inside this container.
     * @param parent	    The container's parent in the tree structure.
     * @param structCont    The StructureContainer containing the component.
     * @param app	    The main application.
     */
    public GUIContainer (GUIComponent component, GUIComponentNode parent, StructureContainer structCont, GUIBuilderApp app)
    {
	super();
	this.app = app;
	this.structCont = structCont;
	this.parent = parent;
	initPopupMenu();
	setBorders();
	setComponent(component);
    }


    /**
     * Sets a reference to the main application.
     *
     * @param	The main GUIBuilderApp application.
     */
    public void setMainApp (GUIBuilderApp app)
    {
	this.app = app;
	if (component!=null)
	    component.setMainApp(app);
    }


    /**
     * Inserts a component into this container. If there is already a component
     * present, that component is deleted first.
     *
     * @param component	    The component to be placed inside this container.
     */
    public void setComponent (GUIComponent component)
    {
	// Delete existing component:
        if(this.component != null)
            remove((Component)(this.component));

	// If the component is in another container, remove it from that
	// container before insertion:
	if (((GUIConcreteComponent)component).getContainer()!=null)
	    ((GUIConcreteComponent)component).getContainer().removeComponent();

	// Insert component:
	((Component)component).addMouseListener(mouseHandler);
	((Component)component).add(componentMenu);
        add ((Component)component, "Center");
	this.component = component;

	// Set references:
	component.setStructureContainer(structCont);
	((GUIConcreteComponent)component).setContainer(this);
	component.setParent(parent);
	component.setMainApp(app);

	if (parent instanceof GUIGridLayout)
	    ((GUIGridLayout)parent).setValidAddPosition();
    }


    /**
     * Returns the component in this container. Null is returned in case no
     * component is present.
     *
     * @return	The component placed inside this container.
     */
    public GUIComponent getComponent ()
    {
	return component;
    }


    /**
     * Remove the set component from this container.
     */
    public void removeComponent ()
    {
        if(component != null)
	{
	    ((Component)component).removeMouseListener(mouseHandler);
            remove((Component)(component));
	    component = null;
	}
    }


    /**
     * Sets the validAddPosition flag. Only if this flag is set, the container
     * allows components to be inserted. This feature is used in the GridLayout,
     * where several empty containers may be present, and it is only allowed
     * to insert component in the first free container.
     *
     * @param state The new state of this container. true means insertion is allowed.
     * @see javablue.GUIGridLayout
     */
    public void setValidAddPosition(boolean state)
    {
	validAddPosition = state;
    }


    /**
     * Sets a reference to the StructureContainer that contains the tree containing
     * this component.
     *
     * @param structCont    The container containing this component.
     */
    public void setStructureContainer (StructureContainer structCont)
    {
	this.structCont = structCont;
	if (component!=null)
	    component.setStructureContainer(structCont);
    }


    /**
     * Sets the highlight state of this container. If a container is highlighted
     * the colors of the borders are changed, so that it appears to be pressed
     * down.
     *
     * @param choice	The highlight state if this container. true means the container should be highlighted.
     */
    public void setHighlight (boolean choice)
    {
	highlighted = choice;
        paintComponents(getGraphics());
    }


    /**
     * Shows the popup menu of this container.
     *
     * @param e	The mouse event.
     */
    private void showPopup (MouseEvent e)
    {
	// The user pressed on the component:
	if (component!=null && component.equals(e.getSource()))
	    componentMenu.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
	// The user pressed on the borders:
	else
	    borderMenu.show(e.getComponent(), e.getPoint().x, e.getPoint().y);

    }


    /**
     * Initialize the popup menus.
     */
    private void initPopupMenu ()
    {
	MenuItem[] borderMenuItem, componentMenuItem;

	String[] label = {"Delete", "Move", "Properties"};
	borderMenuItem = new MenuItem[label.length+1];
	componentMenuItem = new MenuItem[label.length];

	for (int i=0; i<label.length; i++)
	{
	    borderMenuItem[i] = new MenuItem(label[i]);
	    borderMenuItem[i].addActionListener(popupHandler);
	    borderMenu.add(borderMenuItem[i]);
	    componentMenuItem[i] = new MenuItem(label[i]);
	    componentMenuItem[i].addActionListener(popupHandler);
	    componentMenu.add(componentMenuItem[i]);
	}
	borderMenuItem[label.length] = new MenuItem("Layout");
	borderMenuItem[label.length].addActionListener(popupHandler);
	borderMenu.add(borderMenuItem[label.length]);
    }


    /**
     * Initialize the borders of this container.
     */
    private void setBorders()
    {
	setLayout(new BorderLayout());
	setName("Center");
	addMouseListener(mouseHandler);
	
	GUIBorder[] border = {new NorthGUIBorder(), new SouthGUIBorder(),
	    new EastGUIBorder(), new WestGUIBorder()}; //, new CenterField()};
	String[] posName = {"North", "South", "East", "West"}; //, "Center"};

	for (int i=0; i<border.length; i++)
	{
	    border[i].addMouseListener(mouseHandler);
	    border[i].setName(posName[i]);
	    border[i].add(borderMenu);
	    add (border[i], posName[i]);
	}
    }


    /**
     * Processes mouse events on this container.
     *
     * @param e	The mouse event.
     */
    protected void processClick(MouseEvent e)
    {
	// Is the application in select-mode?
	if (app.getMode()==GUIBuilderApp.SELECTMODE)
	{
	    // De-select any previous selected component:
	    GUIComponent tmp = app.getSelectedComponent();
	    if (tmp!=null)
	    {
		if (tmp instanceof GUIComponentLeaf)
		    ((GUIConcreteComponent)tmp).getContainer().setHighlight(false);
		else if (tmp instanceof GUIComponentNormalNode)
		    ((GUIComponentNormalNode)tmp).getGUILayout().setHighlight(false);
	    }

	    // If user pressed on the border, select the GUIComponentNode
	    // containing the component
	    if (e.getComponent().getName().equals("North") ||
		e.getComponent().getName().equals("South") ||
		e.getComponent().getName().equals("East") ||
		e.getComponent().getName().equals("West") ||
		e.getComponent().getName().equals("Center"))
	    {
		app.setSelectedComponent(parent.getTreeParent());
                if(parent instanceof GUIComponentNode)
                    ((GUIComponentLayoutNode)parent).setHighlight(true);
	    }
	    // otherwise the user pressed on the component, which then will
	    // be selected:
	    else
	    {
		app.setSelectedComponent(component);
		setHighlight(true);
	    }
	}
	// If the app. is in add-mode (an add-button on the toolbar is pressed),
	// no component is present in this container and it is allowed to add
	// component to this container:
	else if (app.getMode()==GUIBuilderApp.ADDMODE && component==null && validAddPosition)
	{
	    setHighlight(false);
	    ToolbarButton tmpButton = app.getButtonGroup().getSelectedButton();
	    
	    // If the button isn't permanently selected, go back to select-mode:
	    if (tmpButton!=null && tmpButton.getState()!=ToolbarButton.PERMANENT)
		app.setMode(GUIBuilderApp.SELECTMODE);

	    // Make a new component:
	    GUIComponent tmpComponent = null;
	    if (tmpButton.getName().equals("Button"))
		tmpComponent = new GUIButton("Button",parent,structCont,app);
	    else if (tmpButton.getName().equals("Canvas"))
	    {
		tmpComponent = new GUICanvas(parent,structCont,app);
		((Canvas)tmpComponent).setSize(10,10);
	    }
	    else if (tmpButton.getName().equals("Checkbox"))
		tmpComponent = new GUICheckbox("Checkbox",parent,structCont,app);
	    else if (tmpButton.getName().equals("Choice"))
		tmpComponent = new GUIChoice(parent,structCont,app);
	    else if (tmpButton.getName().equals("Label"))
                tmpComponent = new GUILabel("Label",parent,structCont,app);
            else if (tmpButton.getName().equals("List"))
		tmpComponent = new GUIList(parent,structCont,app);
	    else if (tmpButton.getName().equals("Scrollbar"))
		tmpComponent = new GUIScrollbar(Scrollbar.HORIZONTAL,parent,structCont,app);
	    else if (tmpButton.getName().equals("TextArea"))
		tmpComponent = new GUITextArea("TextArea",parent,structCont,app);
	    else if (tmpButton.getName().equals("TextField"))
		tmpComponent = new GUITextField("TextField",parent,structCont,app);

	    else if (tmpButton.getName().equals("Panel"))
	    {
		tmpComponent = new GUIPanel(parent,structCont,app);
		if (parent instanceof GUIComponentLayoutNode)
		{
		    if (((GUIComponentLayoutNode)parent).hasFixedSize())
			setComponent(tmpComponent);
		    else
			((GUIComponentLayoutNode)parent).addGUIComponent(tmpComponent);
		}
		else
		    setComponent(tmpComponent);

		((GUIPanel)tmpComponent).setGUILayout((GUIComponentLayoutNode)(new GUIFlowLayout ((GUIComponentNode)tmpComponent,structCont,app)));
		tmpComponent = null;
	    }
	    else if (tmpButton.getName().equals("ScrollPane"))
		tmpComponent = new GUIScrollPane(parent,structCont,app);

	    // Insert the new component:
	    if (tmpComponent!=null)
	    {
		if (parent instanceof GUIComponentLayoutNode)
		{
		    if (((GUIComponentLayoutNode)parent).hasFixedSize())
			setComponent(tmpComponent);
		    else
			((GUIComponentLayoutNode)parent).addGUIComponent(tmpComponent);
		}
		else
		    setComponent(tmpComponent);

		// Uncomment the following two lines, if you want to show the
		// property dialog each time you add a component:

                //if(tmpComponent instanceof GUIComponentLeaf) 
                //    tmpComponent.showPropertiesDialog();
                
	    }

            structCont.redraw();
	}
	// Do this if the app is in move-mode, and there's room for a component:
	else if (app.getMode()==GUIBuilderApp.MOVEMODE /*&& component==null*/ && app.getSelectedComponent()!=null && validAddPosition)
	{
	    // Get the component to be moved:
	    GUIComponent comp2bmoved = app.getSelectedComponent();
            GUIComponent comp2bswapped = getComponent();
            
	    // Deselect it:
	    if (comp2bmoved instanceof GUIComponentLeaf)
		((GUIComponentLeaf)comp2bmoved).getContainer().setHighlight(false);
	    else if (comp2bmoved instanceof GUIComponentLayoutNode)
		((GUIComponentLayoutNode)comp2bmoved).setHighlight(false);

            boolean recursive = false; //for disallowing containers moving into one of its subcomponents
            GUIComponent tmp;
            if(comp2bswapped != null)
                tmp = comp2bswapped;
            else
                tmp = parent;
            
            while(tmp.getTreeParent() != null)
            {
                if(comp2bmoved == tmp)
                {
                    // comp2bswapped is one of comp2bmoved's subcomponents
                    recursive = true; 
                    break;
                }
                tmp = tmp.getTreeParent();
            }
	    // Remove it from it's previous position:
            if(comp2bswapped == null && !recursive)
                comp2bmoved.getTreeParent().deleteChild(comp2bmoved);
            else 
            {
                if(!recursive)
                {
                    GUIContainer tmpCont = ((GUIConcreteComponent)comp2bmoved).getContainer();
                    ((GUIConcreteComponent)comp2bmoved).setContainer(this);
                    ((GUIConcreteComponent)comp2bswapped).setContainer(tmpCont);
                    ((GUIConcreteComponent)comp2bmoved).getContainer().setComponent(comp2bmoved);
                    tmpCont.setComponent(comp2bswapped);
                }
                
            }
            
            
	    // and insert it and redraw if nessesary:
	    if (!comp2bmoved.getStructureContainer().equals(structCont))
		comp2bmoved.getStructureContainer().redraw();
            if(comp2bswapped == null)
            {
                if (((GUIComponentLayoutNode)parent).hasFixedSize())
                    setComponent(comp2bmoved);
                else
                {
                    if(!recursive)
                        ((GUIComponentLayoutNode)parent).addGUIComponent(comp2bmoved);
                }
            }
            
	    // Cleanup, go back to select-mode and redraw:
	    if(!recursive)
            {
                setHighlight(false);
                app.setMode(GUIBuilderApp.SELECTMODE);
                app.setStatusText("");
            }
            structCont.redraw();
            
	}
    }


    /**
     * An inner class used to handle mouse-clicks on this container.
     *
     * Created: Oct 1, 1998.
     *
     * @author Morten Knudsen & Kent Hansen
     * @version 1.0
     */
    private class MouseHandler extends MouseAdapter implements Serializable
    {
	public void mouseEntered (MouseEvent e)
	{
	    if (app.getMode()!=GUIBuilderApp.SELECTMODE && component==null && validAddPosition)
		setHighlight(true);
            if(app.getMode() == GUIBuilderApp.MOVEMODE && validAddPosition)
                setHighlight(true);
            
	}


	public void mouseExited (MouseEvent e)
	{
	    if (app.getMode()!=GUIBuilderApp.SELECTMODE && component==null && validAddPosition)
		setHighlight(false);
            if(app.getMode() == GUIBuilderApp.MOVEMODE && validAddPosition)
                setHighlight(false);
            
	}


	public void mouseClicked (MouseEvent e)
	{
            if ((e.getModifiers()==MouseEvent.BUTTON1_MASK))
                processClick(e);
	}


	public void mousePressed (MouseEvent e)
	{
	    if ((e.getModifiers()==MouseEvent.BUTTON3_MASK))
		showPopup(e);
	}
    }


    /**
     * An inner class used to handle the popup-menus.
     *
     * Created: Oct 1, 1998.
     *
     * @author Morten Knudsen & Kent Hansen
     * @version 1.0
     */
    private class PopupHandler implements ActionListener, Serializable
    {
	public void actionPerformed (ActionEvent e)
	{
	    if (e.getActionCommand().equals("Delete"))
	    {
		if (((MenuComponent)e.getSource()).getParent()==componentMenu)
                    component.delete();
		else
                    (((GUIComponentLayoutNode)parent).getTreeParent()).delete();
	    }
	    else if (e.getActionCommand().equals("Move"))
	    {
                
                
                if (((MenuComponent)e.getSource()).getParent()==componentMenu)
                {
                    app.setMode(GUIBuilderApp.MOVEMODE);
                    app.setStatusText("Click on new position");
                    app.setSelectedComponent(component);
                }
                else if(parent.getTreeParent().getTreeParent()!=null)
                {
                   app.setSelectedComponent(((GUIComponentLayoutNode)parent).getTreeParent());
                   app.setMode(GUIBuilderApp.MOVEMODE);
                   app.setStatusText("Click on new position");
                }
                
	    }
	    else if (e.getActionCommand().equals("Properties"))
	    {
		if (((MenuComponent)e.getSource()).getParent()==componentMenu)
        	    component.showPropertiesDialog();
		else
                    (((GUIComponentLayoutNode)parent).getTreeParent()).showPropertiesDialog();
                    
            }
	    else if (e.getActionCommand().equals("Layout"))
	    {
		parent.showPropertiesDialog();
	    }
	    structCont.redraw();
	}
    }
}



/**
 * A super class for all GUIContainer border classes. It handles the colors of
 * the borders.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
abstract class GUIBorder extends Canvas
{
    protected final static int SPACING = 8;
    protected final static Color bgLight = new Color(220, 220, 220);
    protected final static Color bgDark = new Color(215, 215, 215);
    protected Color nwColor, seColor, cColor, bgColor;


    public GUIBorder ()
    {
	super();
    }


    public void paint (Graphics g)
    {
	if (((GUIContainer)getParent()).highlighted)
	{
	    nwColor = SystemColor.controlShadow;
	    seColor = Color.white;
	    cColor = SystemColor.control;
	    setBackground(bgDark);
	}
	else
	{
	    nwColor = Color.white;
	    seColor = SystemColor.controlShadow;
	    cColor = SystemColor.control;
	    setBackground(bgLight);
	}
    }


    public abstract Dimension getMinimumSize ();


    public Dimension getPreferredSize ()
    {
	return (getMinimumSize());
    }
}



/**
 * A custom component representing a border.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class SouthGUIBorder extends GUIBorder
{
    public void paint (Graphics g)
    {
	super.paint(g);
	Dimension dim = getSize();
        g.setColor (nwColor);
	g.drawLine (0, 0, 0, dim.height-2);
	g.setColor (cColor);
	g.drawLine (0, dim.height-1, 0, dim.height-1);
	g.drawLine (dim.width-1, dim.height-1, dim.width-1, dim.height-1);
	g.setColor (seColor);
	g.drawLine (1, dim.height-1, dim.width-2, dim.height-1);
	g.drawLine (dim.width-1, dim.height-2, dim.width-1, 0);
    }


    public Dimension getMinimumSize ()
    {
	return (new Dimension(0, SPACING));
    }
}



/**
 * A custom component representing a border.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class NorthGUIBorder extends GUIBorder
{
    public void paint (Graphics g)
    {
	super.paint(g);
	Dimension dim = getSize();
	g.setColor (nwColor);
	g.drawLine (0, dim.height-1, 0, 1);
	g.drawLine (1, 0, dim.width-2, 0);
	g.setColor (cColor);
	g.drawLine (0, 0, 0, 0);
	g.drawLine (dim.width-1, 0, dim.width-1, 0);
	g.setColor (seColor);
	g.drawLine (dim.width-1, 1, dim.width-1, dim.height-1);
    }


    public Dimension getMinimumSize ()
    {
	return (new Dimension(0, SPACING));
    }
}



/**
 * A custom component representing a border.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class WestGUIBorder extends GUIBorder
{
    public void paint (Graphics g)
    {
	super.paint(g);
	Dimension dim = getSize();
        g.setColor (nwColor);
	g.drawLine (0, 0, 0, dim.height);
    }


    public Dimension getMinimumSize ()
    {
	return (new Dimension(SPACING, 0));
    }
}



/**
 * A custom component representing a border.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class EastGUIBorder extends GUIBorder
{
    public void paint (Graphics g)
    {
	super.paint(g);
	Dimension dim = getSize();
        g.setColor (seColor);
	g.fillRect (dim.width-1, 0, dim.width-1, dim.height);
    }


    public Dimension getMinimumSize ()
    {
	return (new Dimension(SPACING, 0));
    }
}
