package javablue.GUIBuilder;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Enumeration;
import javablue.GUIGraphics.*;


/**
 * GUIMenuBarPropertyDialog.java
 * A Class for showing a Dialog to edit the properties of a MenuBar
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIMenuBarPropertyDialog extends Dialog
{
    private GUIMenuBar menubar;
    private GUIBuilderApp app;
    private StructureContainer structCont;
    private ToolbarButtonGroup addGroup = new ToolbarButtonGroup();
    private List theList = new List();
    private GUIMenuComponent selectedComponent = null;
    private GUIMenuBarPropertyDialog thisDialog;
    private boolean moving = false;

    private ButtonListener buttonListener = new ButtonListener();

    private ListListener listListener = new ListListener();


     /**
       * Constructs a GUIMenuBarPropertyDialog. It enables the user to edit the properties of a MenuBar.
       @param f Frame
       @param parentWindow The parent Window of the MenuBar.
       @param menubar The GUIMenuBar to be changed.
       @param structCont The StructureContainer in which the MenuBar resides.
       @param app The GUIBuilderApp application.
       */
    public GUIMenuBarPropertyDialog (Frame frame, Window parentWindow, GUIMenuBar menubar, StructureContainer structCont, GUIBuilderApp app)
    {
	super(frame, "MenuBar Properties", true);
	this.app = app;
	this.structCont = structCont;
	this.menubar = menubar;
	thisDialog = this;

	addWindowListener(new WindowAdapter()
	    { public void windowClosing(WindowEvent e) { onCancel(); } } );

	Font appFont = app.getFont();
	theList.setFont(new Font("Monospaced", appFont.getStyle(), appFont.getSize()));

	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints constraints = new GridBagConstraints();
	constraints.gridwidth = GridBagConstraints.REMAINDER;
	constraints.fill = GridBagConstraints.HORIZONTAL;
	Panel toolbar = new Panel(gridbag);

	String toolButton[] = {"Expand", "Shrink", "---", "Delete", "Move", "Properties"};
	Button tmpButton;
	for (int i=0; i<toolButton.length; i++)
	{
	    if (toolButton[i].equals("---"))
	    {
		Separator separator = new Separator();
		gridbag.setConstraints(separator, constraints);
		toolbar.add(separator);
	    }
	    else
	    {
		tmpButton = new Button(toolButton[i]);
		// REMOVE THIS IF STATEMENT LATER:
		if (i<2)
		    tmpButton.setEnabled(false);
		tmpButton.addActionListener(buttonListener);
		gridbag.setConstraints(tmpButton, constraints);
		toolbar.add(tmpButton);
	    }
	}
	Panel east = new Panel(new BorderLayout());
	east.add(toolbar, "North");

	Panel addButtons = new Panel(new GridLayout(2,2));
	String addButton[] = {"Submenu", "Normal Item", "Separator", "Checkbox Item"};
	ToolbarButton tmpToolbarButton;
	for (int i=0; i<addButton.length; i++)
	{
	    tmpToolbarButton = new ToolbarButton(addButton[i]);
	    addButtons.add(tmpToolbarButton);
	    addGroup.addButton(tmpToolbarButton);
	}

	Panel commandButtons = new Panel (new FlowLayout());
	String commandButton[] = {"OK", "Cancel"};
	for (int i=0; i<commandButton.length; i++)
	{
	    tmpButton = new Button(commandButton[i]);
	    tmpButton.addActionListener(buttonListener);
	    commandButtons.add(tmpButton);
	}

	Panel south = new Panel(new BorderLayout());
	south.add(new Label("Add:"), "North");
	south.add(addButtons, "Center");
	south.add(commandButtons, "South");

	drawList();

	theList.addItemListener(listListener);

	add(new Label("Menu items:"), "North");
	add(east, "East");
	add(theList, "Center");
	add(south, "South");

	pack();

	Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
	Dimension dim = getSize();
	Point position = parentWindow.getLocation();
	int x = position.x+20;
	int y = position.y+20;
	if (x+dim.width>screen.width)
	    x = screen.width-dim.width;
	if (x<0)
	    x = 0;
	if (y+dim.height>screen.height)
	    y = screen.height-dim.height;
	if (y<0)
	    y = 0;
	setLocation (x, y);

	show();
    }

    private void drawList()
    {
	MenuPair[] listRep = menubar.getListRepresentation();
	theList.removeAll();
	for (int i=0; i<listRep.length;i++)
	    theList.add(listRep[i].getLabel());
    }

    private void onCancel()
    {
	dispose();
    }



    private class ButtonListener implements ActionListener
    {
	public void actionPerformed (ActionEvent e)
	{
	    if (e.getActionCommand().equals("OK"))
		onCancel();
	    else if (e.getActionCommand().equals("Cancel"))
		onCancel();
	    else if (e.getActionCommand().equals("Expand"))
		;
	    else if (e.getActionCommand().equals("Shrink"))
		;
	    else if (e.getActionCommand().equals("Delete"))
	    {
		if (selectedComponent!=null)
		{
		    selectedComponent.delete();
		    selectedComponent = null;
		    drawList();
		}
	    }
	    else if (e.getActionCommand().equals("Move"))
	    {
		if (selectedComponent!=null && !(selectedComponent instanceof GUIMenuBar))
		    moving = true;
	    }
	    else if (e.getActionCommand().equals("Properties"))
	    {
		if (selectedComponent!=null && !(selectedComponent instanceof GUIMenuBar))
		{
		    selectedComponent.showPropertiesDialog(thisDialog);
		}
	    }
	}
    }



    private class ListListener implements ItemListener
    {
	public void itemStateChanged(ItemEvent e)
	{
	    int index = ((Integer)(e.getItem())).intValue();

	    ToolbarButton tmpButton = addGroup.getSelectedButton();
	    MenuPair[] listRep = menubar.getListRepresentation();

	    GUIMenuComponent tmpMenu = listRep[index].getMenuComponent();

	    if (tmpButton!=null)
	    {
		String label = tmpButton.getLabel();
		if (label.equals("Submenu"))
		{
		    if (tmpMenu instanceof GUIMenuNodeComponent)
			((GUIMenuNodeComponent)tmpMenu).addGUIMenuComponent(new GUIMenu(structCont, app));
		    else
			tmpMenu.getGUIParent().insertGUIMenuComponent(new GUIMenu(structCont, app), tmpMenu);
		}
		else if(label.equals("Normal Item") && !(tmpMenu instanceof GUIMenuBar))
		{
		    if (tmpMenu instanceof GUIMenuNodeComponent)
			((GUIMenuNodeComponent)tmpMenu).addGUIMenuComponent(new GUIMenuItem(structCont, app));
		    else
			tmpMenu.getGUIParent().insertGUIMenuComponent(new GUIMenuItem(structCont, app), tmpMenu);
		}
		else if(label.equals("Checkbox Item") && !(tmpMenu instanceof GUIMenuBar))
		{
		    if (tmpMenu instanceof GUIMenuNodeComponent)
			((GUIMenuNodeComponent)tmpMenu).addGUIMenuComponent(new GUIMenuCheckboxItem(structCont, app));
		    else
			tmpMenu.getGUIParent().insertGUIMenuComponent(new GUIMenuCheckboxItem(structCont, app), tmpMenu);
		}
		else if(label.equals("Separator") && !(tmpMenu instanceof GUIMenuBar))
		{
		    if (tmpMenu instanceof GUIMenuNodeComponent)
			((GUIMenu)tmpMenu).addGUIMenuComponent(new GUIMenuSeparator());
		    else
			((GUIMenu)tmpMenu.getGUIParent()).insertGUIMenuComponent(new GUIMenuSeparator(), tmpMenu);
		}
		if (tmpButton.getState()!=ToolbarButton.PERMANENT)
		    addGroup.unPopAll();
		drawList();
	    }
	    else
	    {
		if (moving && !(tmpMenu instanceof GUIMenuBar))
		{
		    selectedComponent.delete();
		    if (tmpMenu instanceof GUIMenuNodeComponent)
			((GUIMenuNodeComponent)tmpMenu).addGUIMenuComponent(selectedComponent);
		    else
			((GUIMenuComponent)tmpMenu).getGUIParent().insertGUIMenuComponent(selectedComponent, tmpMenu);
		    moving = false;
		    drawList();
		}
		else
		{
		    selectedComponent = tmpMenu;
		    moving = false;
		}
	    }
	}
    }
}
