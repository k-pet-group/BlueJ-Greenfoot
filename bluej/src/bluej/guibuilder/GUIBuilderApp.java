package bluej.guibuilder;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Vector;
import bluej.guibuilder.graphics.*;
import bluej.pkgmgr.Package;

/**
 * This is the main class for the GUI-Builder application. Instantiate this
 * class to start the application.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIBuilderApp extends Frame
{
    /**
     * Ease-of-use constant for setMode() and getMode().
     * Specifies the application to be in add-mode.
     *
     * @see javablue.GUIBuilder.GUIBuilderApp#setMode
     * @see javablue.GUIBuilder.GUIBuilderApp#getMode
     */
    public static final int ADDMODE = 0;

    /**
     * Ease-of-use constant for setMode() and getMode().
     * Specifies the application to be in move-mode.
     *
     * @see javablue.GUIBuilder.GUIBuilderApp#setMode
     * @see javablue.GUIBuilder.GUIBuilderApp#getMode
     */
    public static final int MOVEMODE = 1;

    /**
     * Ease-of-use constant for setMode() and getMode().
     * Specifies the application to be in select-mode.
     *
     * @see javablue.GUIBuilder.GUIBuilderApp#setMode
     * @see javablue.GUIBuilder.GUIBuilderApp#getMode
     */
    public static final int SELECTMODE = 2;

    private Panel north = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private Panel center;
    private Panel south = new Panel(new GridLayout(6,1));
    private Label status = new Label("Add");

    private GUIBuilderApp app;
    private ToolbarButtonGroup buttonGroup = new ToolbarButtonGroup();

    private int mode = SELECTMODE;
    private GUIComponent selectedComponent = null;
    private StructureContainer selectedStruct = null;
    
    private Vector structureVector = new Vector();
    private StructureContainer structCont = null;
    private Package pkg = null;

    private String defaultdir = new String();
    private String structdir = new String();
    
    private MenuItem file_new_frameMenu = new MenuItem("Frame");
    private MenuItem file_new_dialogMenu = new MenuItem("Dialog");
    private MenuItem file_openMenu = new MenuItem("Open");
    private MenuItem file_saveMenu = new MenuItem("Save");
    private MenuItem file_previewMenu = new MenuItem("Preview");
    private MenuItem file_generateMenu = new MenuItem("Generate");
    private MenuItem file_quitMenu = new MenuItem("Quit");
    private MenuItem help_aboutMenu = new MenuItem("About");

    private MenuListener menuListener = new MenuListener();
    private ButtonListener buttonListener = new ButtonListener();
    ToolbarButtonListener toolbarButtonListener = new ToolbarButtonListener();


    /**
     * Constructs a new GUIBuilderApp. This starts up the application and shows
     * the toolbar on the screen.
     */
    public GUIBuilderApp()
    {
	super("GUIBuilder");
	app = this;
	addWindowListener(new WindowAdapter() { public void 
		    windowClosing(WindowEvent e) { shutdown(); } } );
	setResizable(false);
	setBackground(Color.lightGray);
	createInterface();

	show();
    }


    public GUIBuilderApp(Package pkg)
    {
	super("GUIBuilder");
	app = this;
	this.pkg = pkg;
	addWindowListener(new WindowAdapter() { public void 
		    windowClosing(WindowEvent e) { shutdown(); } } );
	setResizable(false);
	setBackground(Color.lightGray);
	createInterface();

	show();
    }


    /**
     * Shuts down the application and closes all open windows.
     */
    public void shutdown()
    {
	while (structureVector.size()>0)
	    ((StructureContainer)structureVector.elementAt(0)).delete();
	dispose();
    }


    /**
     * Sets the mode of the application to be in.
     *
     * @param newmode The mode of the application.
     *
     * @see javablue.GUIBuilder.GUIBuilderApp#ADDMODE
     * @see javablue.GUIBuilder.GUIBuilderApp#MOVEMODE
     * @see javablue.GUIBuilder.GUIBuilderApp#SELECTMODE
     */
    public void setMode(int newmode)
    {
	switch (newmode)
	{
	    case ADDMODE:
		mode = ADDMODE;
		break;
	    case SELECTMODE:
		mode = SELECTMODE;
		buttonGroup.unPopAll();
		break;
	    case MOVEMODE:
		mode = MOVEMODE;
		buttonGroup.unPopAll();
		break;
	}
    }


    /**
     * Gets the mode the application currently is in.
     *
     * @return The mode of the application.
     *
     * @see javablue.GUIBuilder.GUIBuilderApp#ADDMODE
     * @see javablue.GUIBuilder.GUIBuilderApp#MOVEMODE
     * @see javablue.GUIBuilder.GUIBuilderApp#SELECTMODE
     */
    public int getMode()
    {
	return mode;
    }


    /**
     * Sets the status text. The status text is located below the toolbar in the
     * applications frame.
     *
     * @param text The text to show in the status bar.
     */
    public void setStatusText (String text)
    {
	status.setText(text);
    }


    /**
     * Returns the button group that contain the buttons representing the
     * components that can be added.
     *
     * @return The toolbar button group.
     */
    public ToolbarButtonGroup getButtonGroup ()
    {
	return buttonGroup;
    }


    /**
     * Sets the selected component. The delete, move and properties buttons
     * work on the selected component.
     *
     * @param component The component the user have selected.
     */
    public void setSelectedComponent (GUIComponent component)
    {
	if (selectedComponent!=null && (selectedComponent instanceof GUIComponentLeaf))
	    ((GUIComponentLeaf)selectedComponent).getContainer().setHighlight(false);
	selectedComponent = component;
    }


    /**
     * Returns the currently selected component. In case no component is
     * selected, null is returned.
     *
     * @return The selected component.
     */
    public GUIComponent getSelectedComponent ()
    {
	return selectedComponent;
    }


    public Package getPackage()
    {
	return pkg;
    }


    /**
     * Sets the selected structure. The preview, save and generate menu items
     * operates on the selected structure
     *
     * @param structCont The structure the user are currently working on.
     */
    public void setSelectedStructure (StructureContainer structCont)
    {
	selectedStruct = structCont;
    }


    /**
     * Removes the specified structure container from this application.
     *
     * @param structCont The structure to remove
     */
    public void removeStructure(StructureContainer structCont)
    {
	if (structCont.equals(selectedStruct))
	    selectedStruct = null;
	if (selectedComponent !=null && selectedComponent.getStructureContainer().equals(structCont))
	    selectedComponent = null;
	structureVector.removeElement(structCont);
    }


    /**
     * Creates the user interface (the toolbar frame) for this application.
     */
    private void createInterface()
    {
	// Initialize and add menus:
	MenuBar menubar = new MenuBar();
	Menu filemenu = new Menu("File");
	Menu helpmenu = new Menu("Help");
	menubar.add(filemenu);
	menubar.setHelpMenu(helpmenu);

	Menu file_newMenu = new Menu("New");

	file_newMenu.add(file_new_frameMenu);
	file_newMenu.add(file_new_dialogMenu);
	filemenu.add(file_newMenu);
	filemenu.add(file_openMenu);
	filemenu.add(file_saveMenu);
	filemenu.addSeparator();
	filemenu.add(file_previewMenu);
	filemenu.addSeparator();
	filemenu.add(file_generateMenu);
	filemenu.addSeparator();
	filemenu.add(file_quitMenu);
	helpmenu.add(help_aboutMenu);
	file_new_frameMenu.setShortcut(new MenuShortcut(KeyEvent.VK_F));
	file_new_frameMenu.addActionListener(menuListener);
	file_new_dialogMenu.setShortcut(new MenuShortcut(KeyEvent.VK_D));
	file_new_dialogMenu.addActionListener(menuListener);
	file_openMenu.setShortcut(new MenuShortcut(KeyEvent.VK_O));
	file_openMenu.addActionListener(menuListener);
	file_saveMenu.setShortcut(new MenuShortcut(KeyEvent.VK_S));
	file_saveMenu.addActionListener(menuListener);
	file_previewMenu.setShortcut(new MenuShortcut(KeyEvent.VK_P));
	file_previewMenu.addActionListener(menuListener);
	file_generateMenu.setShortcut(new MenuShortcut(KeyEvent.VK_G));
	file_generateMenu.addActionListener(menuListener);
	file_quitMenu.setShortcut(new MenuShortcut(KeyEvent.VK_Q));
	file_quitMenu.addActionListener(menuListener);
	help_aboutMenu.addActionListener(menuListener);

	setMenuBar(menubar);

	setLayout(new BorderLayout());

	// Initialize and add "Add"-buttons:
	Panel[] panel = new Panel[2];
	String[] guiButton = {"Button","Canvas","Checkbox","Choice","Label",
			    "List","Scrollbar","TextArea","TextField"};
	panel[0] = makeCardPanel(guiButton);

	String[] containerButton = {"Panel","ScrollPane"};
	panel[1] = makeCardPanel(containerButton);

	String[] label = {"Components", "Containers"};
	center = new TabControl(label, panel);

	// Initialize action-buttons and status bar:
	south.add(new Separator());
	String[] modeButton = {"Delete", "Move", "Properties"};
	Button tmpButton;
	for(int i=0; i<3 ; i++)
	{
	    tmpButton = new Button(modeButton[i]);
	    tmpButton.addActionListener (buttonListener);
	    south.add(tmpButton);
	}
	south.add(new Separator());
	south.add(status);

	// Add everything:
	add("South",south);
	add("North",north);
	add("Center",center);

	// Position the frame somewhere nice and show it:
	pack();
	Dimension dim = getPreferredSize();
	setSize((int)(dim.width*1.1), dim.height);

	Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
	setLocation(screen.width/20,(screen.height-dim.height)/2);
    }


    /**
     * Shows a Dialog with some information about the application.
     */
    private void showAboutDialog()
    {
	AboutDialog about = new AboutDialog (app);
    }

    /**
     * Creates a Panel containing ToolbarButtons with labels with the texts
     * specified in the string array.
     *
     * @param text The button labels.
     * @return A panel containing the buttons.
     */
    private Panel makeCardPanel(String[] text)
    {
	// Setup the GridBagLayout:
	GridBagLayout gridbag = new GridBagLayout ();
	GridBagConstraints constraints = new GridBagConstraints();

	constraints.gridwidth = GridBagConstraints.REMAINDER;
	constraints.fill      = GridBagConstraints.HORIZONTAL;
	constraints.weightx   = 1.0;
	constraints.anchor    = GridBagConstraints.NORTH;
	ToolbarButton[] button = new ToolbarButton[text.length];
	Panel tmpPanel = new Panel(gridbag);
	Separator sep = new Separator();

	// Initialize the buttons:
	for(int i=0; i<text.length; i++)
	{
	    button[i] = new ToolbarButton(text[i]);
	    button[i].setName(text[i]);
	    button[i].addMouseListener(toolbarButtonListener);
	    buttonGroup.addButton(button[i]);
	}

	// Add the buttons to the panel:
	int i;
	for(i=0; i<button.length-1 ; i++)
	{
	    if (text[i].equals("---"))
	    {
		gridbag.setConstraints(sep, constraints);
		tmpPanel.add(sep);
	    }
	    else
	    {
		gridbag.setConstraints(button[i], constraints);
		tmpPanel.add(button[i]);
	    }
	}
	constraints.weighty = 1;
	gridbag.setConstraints(button[i], constraints);
	tmpPanel.add(button[i]);

	return tmpPanel;
    }


    /**
     * This class handles the menu items.
     *
     * Created: Oct 1, 1998.
     *
     * @author Morten Knudsen & Kent Hansen
     * @version 1.0
     */
    private class MenuListener implements ActionListener
    {
	public void actionPerformed (ActionEvent e)
	{
	    Object source = e.getSource();
	    // Make a new Frame:
	    if (source.equals(file_new_frameMenu))
		structureVector.addElement(new StructureContainer(app, new GUIFrame(null, null, app)));
	    // Make a new Dialog:
	    else if (source.equals(file_new_dialogMenu))
		structureVector.addElement(new StructureContainer(app, new GUIDialog(app, null, null, app)));
	    // Load a structure from disk:
	    else if (source.equals(file_openMenu))
	    {
		try
		{
		    FileDialog f = new FileDialog(app,"Open structure",FileDialog.LOAD);
                    f.setDirectory(structdir);
                    f.setModal(true);
                    f.show();
                
                    String strFile = f.getDirectory()+f.getFile();
                    File file = null;
                    if(strFile!=null)
                	file = new File(strFile);
                    if(file!=null)
        	    {
                	ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
                	StructureContainer tmp = (StructureContainer)in.readObject();
                	tmp.setMainApp(app);
                	structureVector.addElement(tmp);
                	structdir = f.getDirectory();

                	tmp.show();
                	status.setText(" Done!");
                    }
		}
		catch (Exception exception)
		{
		    System.out.println("Exception: "+exception.getMessage());
		}
	    }
	    // Save a structure to disk:
	    else if (source.equals(file_saveMenu))
	    {
		try
		{
		    status.setText("Saving...");
                    FileDialog f = new FileDialog(app,"Save structure",FileDialog.SAVE);
                    f.setDirectory(structdir);
                    f.setModal(true);
                    f.show();
                    String strFile = f.getFile();
                    File file = null;
                    if(strFile!=null)
                	file = new File(strFile);
                    if(file!=null)
                    {
                        structdir = f.getDirectory();
                        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(structdir+file.getName()));
                        out.writeObject(selectedStruct);
                        out.close();
                        
                        status.setText(" Done!");
                    }
		}
		catch (IOException exception)
		{
		    System.out.println("Exception: "+exception.getMessage());
		}
	    }
	    // Preview the selected structure:
	    else if (source.equals(file_previewMenu))
		selectedStruct.preview();
	    // Generate the code of the selected structure, and save it to disk:
	    else if (source.equals(file_generateMenu))
	    {
        	try
		{
                    FileDialog f = new FileDialog(app,"Save generated code",FileDialog.SAVE);
            
                    f.setDirectory(defaultdir);
                    f.setModal(true);
                    f.show();
                    String strFile = f.getFile();
                    File file = null;
                    if(strFile!=null)
                	file = new File(strFile);
         
                    if(file!=null)
                    {
                	defaultdir = f.getDirectory();
			PrintWriter pw = new PrintWriter(new FileOutputStream(defaultdir+file.getName()));
                	
                	pw.print(selectedStruct.generateCode());
                	pw.flush();
                	pw.close();
                	status.setText("Code saved");
                    }
        	}
        	catch(java.io.IOException ex){
                    status.setText("File could not be generated");
        	}
            }
	    // Exit the application:
	    else if (source.equals(file_quitMenu))
		shutdown();
	    else if (source.equals(help_aboutMenu))
		showAboutDialog();
	}
    }



    /**
     * This class handles clicks on the action-buttons (delete, move, etc).
     *
     * Created: Oct 1, 1998.
     *
     * @author Morten Knudsen & Kent Hansen
     * @version 1.0
     */
    private class ButtonListener implements ActionListener
    {
	public void actionPerformed (ActionEvent e)
	{
	    setMode(SELECTMODE);
	    // Only do something if a component is selected:
	    if (selectedComponent!=null)
	    {
    		String activated = e.getActionCommand();
		// Delete it:
		if (activated.equals("Delete"))
		{
		    selectedComponent.delete();
		    selectedComponent=null;
		    if (selectedStruct!=null)
			selectedStruct.redraw();
		}
		// Move it:
		else if (activated.equals("Move"))
		{
		    if (selectedComponent!=null && selectedComponent.getTreeParent() != null )
		    {
			setMode(MOVEMODE);
			status.setText("Click on new position");
		    }
		}
		// Show the property dialog of the component:
		else if (activated.equals("Properties"))
		{
		    if (selectedComponent!=null)
			selectedComponent.showPropertiesDialog();
		}
	    }
	}
    }



    /**
     * This class handles clicks on the add-buttons (Button, Canvas, etc.).
     *
     * Created: Oct 1, 1998.
     *
     * @author Morten Knudsen & Kent Hansen
     * @version 1.0
     */
    private class ToolbarButtonListener extends MouseAdapter
    {
	public void mouseClicked (MouseEvent e)
	{
	    // If the user pressed the left-button:
	    if ((e.getModifiers()==MouseEvent.BUTTON1_MASK))
	    {
		if (buttonGroup.getSelectedButton()!=null)
		    setMode(ADDMODE);
		else
		    setMode(SELECTMODE);

		if (selectedComponent!=null)
		{
		    if (selectedComponent instanceof GUIComponentLeaf)
			((GUIConcreteComponent)selectedComponent).getContainer().setHighlight(false);
		    else if (selectedComponent instanceof GUIComponentNode)
			((GUIComponentNormalNode)selectedComponent).getGUILayout().setHighlight(false);
		    selectedComponent = null;
		}
	    }
	}
    }



    class AboutDialog extends Dialog
    {
	public AboutDialog(Frame frame)
	{
	    super (frame, "About GUIBuilder", true);
	    setLocation(frame.getLocation());

	    addWindowListener(new WindowAdapter() { public void 
		    windowClosing(WindowEvent e) { closeDialog(); } } );

	    setLayout(new GridBagLayout());
	    GridBagConstraints gbc = new GridBagConstraints();
	    gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
	    gbc.weightx = 1;

	    add(new Label("This GUI Builder is developed by"), gbc);
	    add(new Label("Morten R. Knudsen (mrkmrk@hotmail.com) and"), gbc);
	    add(new Label("Kent B. Hansen (kent.hansen@bigfoot.com)."), gbc);

	    Panel okPanel = new Panel();
	    Button okButton = new Button("OK");
	    okButton.addActionListener(new ButtonListener());
	    okPanel.add(okButton);
	    add(okPanel, gbc);

	    pack();
	    show();
	}

	private void closeDialog()
	{
	    dispose();
	}

	private class ButtonListener implements ActionListener
	{
	    public void actionPerformed (ActionEvent e)
	    {
		closeDialog();
	    }
	}
    }
}
