package bluej.guibuilder;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.Box;
import javax.swing.table.*;
import java.beans.*;

import bluej.guibuilder.graphics.*;
import bluej.pkgmgr.Package;
import bluej.Config;

// JavaBeans Archiving classes.
import archiver.XMLInputStream;
import archiver.XMLOutputStream;
import archiver.BeanScriptInputStream;
import archiver.BeanScriptOutputStream;
import archiver.JavaOutputStream;

/**
 *
javax/swing/JAppletBeanInfo.class
javax/swing/JButtonBeanInfo.class
javax/swing/JCheckBoxBeanInfo.class
javax/swing/JCheckBoxMenuItemBeanInfo.class
javax/swing/JColorChooserBeanInfo.class
javax/swing/JComboBoxBeanInfo.class
javax/swing/JComponentBeanInfo.class
javax/swing/JDialogBeanInfo.class
javax/swing/JEditorPaneBeanInfo.class
javax/swing/JFileChooserBeanInfo.class
javax/swing/JFrameBeanInfo.class
javax/swing/JInternalFrameBeanInfo.class
javax/swing/JLabelBeanInfo.class
javax/swing/JListBeanInfo.class
javax/swing/JMenuBarBeanInfo.class
javax/swing/JMenuBeanInfo.class
javax/swing/JMenuItemBeanInfo.class
javax/swing/JOptionPaneBeanInfo.class
javax/swing/JPanelBeanInfo.class
javax/swing/JPasswordFieldBeanInfo.class
javax/swing/JPopupMenuBeanInfo.class
javax/swing/JProgressBarBeanInfo.class
javax/swing/JRadioButtonBeanInfo.class
javax/swing/JRadioButtonMenuItemBeanInfo.class
javax/swing/JScrollBarBeanInfo.class
javax/swing/JScrollPaneBeanInfo.class
javax/swing/JSeparatorBeanInfo.class
javax/swing/JSliderBeanInfo.class
javax/swing/JSplitPaneBeanInfo.class
javax/swing/JTabbedPaneBeanInfo.class
javax/swing/JTableBeanInfo.class
javax/swing/JTextAreaBeanInfo.class
javax/swing/JTextFieldBeanInfo.class
javax/swing/JTextPaneBeanInfo.class
javax/swing/JToggleButtonBeanInfo.class
javax/swing/JToolBarBeanInfo.class
javax/swing/JTreeBeanInfo.class
javax/swing/JWindowBeanInfo.class
javax/swing/text/
javax/swing/text/JTextComponentBeanInfo.class
*/

/**
 * This is the main class for the GUI-Builder application. Instantiate this
 * class to start the application.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUIBuilderApp extends JFrame
{
    /**
     * Ease-of-use constant for setMode() and getMode().
     * Specifies the application to be in add-mode.
     *
     * @see blue.guibuilder.GUIBuilderApp#setMode
     * @see blue.guibuilder.GUIBuilderApp#getMode
     */
    public static final int ADDMODE = 0;

    /**
     * Ease-of-use constant for setMode() and getMode().
     * Specifies the application to be in move-mode.
     *
     * @see bluej.guibuilder.GUIBuilderApp#setMode
     * @see bluej.guibuilder.GUIBuilderApp#getMode
     */
    public static final int MOVEMODE = 1;

    /**
     * Ease-of-use constant for setMode() and getMode().
     * Specifies the application to be in select-mode.
     *
     * @see bluej.guibuilder.GUIBuilderApp#setMode
     * @see bluej.guibuilder.GUIBuilderApp#getMode
     */
    public static final int SELECTMODE = 2;

    private JTabbedPane center;
    private JPanel workarea;
    private JToggleButton selectButton;

    private Label status = new Label("Add");

    private GUIBuilderApp app;
    private ButtonGroup buttonGroup = new ButtonGroup();
    private NewComponentAction buttonAction = null;

    private int mode = SELECTMODE;
    private GUIComponent selectedComponent = null;
    private StructureContainer selectedStruct = null;
    private PropertyTableModel selectedModel;

    private Vector structureVector = new Vector();
    private StructureContainer structCont = null;
    private Package pkg = null;

    private String defaultdir = new String();
    private String structdir = new String();

    private JMenuItem file_new_frameMenu = new JMenuItem("Frame");
    private JMenuItem file_new_dialogMenu = new JMenuItem("Dialog");
    private JMenuItem file_openMenu = new JMenuItem("Open");
    private JMenuItem file_saveMenu = new JMenuItem("Save");
    private JMenuItem file_previewMenu = new JMenuItem("Preview");
    private JMenuItem file_generateMenu = new JMenuItem("Generate");
    private JMenuItem file_quitMenu = new JMenuItem("Quit");
    private JMenuItem help_aboutMenu = new JMenuItem("About");

    private MenuListener menuListener = new MenuListener();
    private ButtonListener buttonListener = new ButtonListener();


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
//        setResizable(false);
        registerPropertyEditors();
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
     * @see bluej.guibuilder.GUIBuilderApp#ADDMODE
     * @see bluej.guibuilder.GUIBuilderApp#MOVEMODE
     * @see bluej.guibuilder.GUIBuilderApp#SELECTMODE
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
            selectButton.setSelected(true);
            break;
            case MOVEMODE:
            mode = MOVEMODE;
            selectButton.setSelected(true);
            break;
        }
    }


    /**
     * Gets the mode the application currently is in.
     *
     * @return The mode of the application.
     *
     * @see bluej.guibuilder.GUIBuilderApp#ADDMODE
     * @see bluej.guibuilder.GUIBuilderApp#MOVEMODE
     * @see bluej.guibuilder.GUIBuilderApp#SELECTMODE
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
    public ButtonGroup getButtonGroup ()
    {
        return buttonGroup;
    }

    public NewComponentAction getButtonAction ()
    {
        return buttonAction;
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

        if(selectedModel != null) {
            selectedModel.setObject(component);
            selectedModel.filterTable(1);
            selectedModel.fireTableDataChanged();
        }
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
        JMenuBar menubar = new JMenuBar();
        JMenu filemenu = new JMenu("File");
        JMenu helpmenu = new JMenu("Help");
        menubar.add(filemenu);
        menubar.add(Box.createHorizontalGlue());
        menubar.add(helpmenu);

        JMenu file_newMenu = new JMenu("New");

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
        file_new_frameMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.CTRL_MASK));
        file_new_frameMenu.addActionListener(menuListener);
        file_new_dialogMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Event.CTRL_MASK));
        file_new_dialogMenu.addActionListener(menuListener);
        file_openMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK));
        file_openMenu.addActionListener(menuListener);
        file_saveMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK));
        file_saveMenu.addActionListener(menuListener);
        file_previewMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK));
        file_previewMenu.addActionListener(menuListener);
        file_generateMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, Event.CTRL_MASK));
        file_generateMenu.addActionListener(menuListener);
        file_quitMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK));
        file_quitMenu.addActionListener(menuListener);
        help_aboutMenu.addActionListener(menuListener);

        setJMenuBar(menubar);

        /* Main toolbar */

        JPanel windowPanel = new JPanel();
        {
            JPanel tools = new JPanel();
            {
                tools.setLayout(new BoxLayout(tools, BoxLayout.Y_AXIS));
                tools.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));

                selectButton = new JToggleButton("Select", Config.getImage("guibuilder.image.selectbutton"));
                {
                    selectButton.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                                                    selectButton.getPreferredSize().height));
                    buttonGroup.add(selectButton);
                    tools.add(selectButton);
                }

                tools.add(Box.createVerticalStrut(6));

                NewComponentListener newComponentListener = new NewComponentListener();

                NewComponentAction actions[] = createNewComponentActions();

                for(int i=0; i<actions.length; i++)
                {
                    JToggleButton newbutton = new JToggleButton(actions[i]);
                    {
                        newbutton.addActionListener(newComponentListener);
                        newbutton.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                                                    newbutton.getPreferredSize().height));
                        buttonGroup.add(newbutton);
                        tools.add(newbutton);
                    }
                }

                tools.add(Box.createVerticalStrut(6));

                // Initialize action-buttons and status bar:
                String[] modeButton = {"Delete", "Move", "Properties"};
                JButton tmpButton;
                for(int i=0; i<3 ; i++)
                {
                    tmpButton = new JButton(modeButton[i]);
                    tmpButton.addActionListener (buttonListener);

                    tmpButton.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                                                tmpButton.getPreferredSize().height));

                    tools.add(tmpButton);
                }
            }

            workarea = new JPanel();
            {
 //            workarea.setBackground(Color.lightGray);
                workarea.setBorder(BorderFactory.createEtchedBorder());
                workarea.setPreferredSize(new Dimension(400,400));
            }

            selectedModel = new PropertyTableModel();
            PropertyColumnModel columnModel = new PropertyColumnModel();

            JTable table = new JTable(selectedModel, columnModel);
            table.setAutoResizeMode(table.AUTO_RESIZE_LAST_COLUMN);
            table.setRowHeight(20);

            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(workarea), new JScrollPane(table));

            windowPanel.setLayout(new BorderLayout());
            windowPanel.setBorder(Config.generalBorder);
            windowPanel.add(tools, BorderLayout.WEST);
            windowPanel.add(splitPane, BorderLayout.CENTER);
            windowPanel.add(status, BorderLayout.SOUTH);
        }

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(windowPanel, BorderLayout.CENTER);

        pack();
    }


    /**
     * Shows a Dialog with some information about the application.
     */
    private void showAboutDialog()
    {
        AboutDialog about = new AboutDialog (app);
    }

    /**
     * Method which registers property editors for types.
     */
    private void registerPropertyEditors()  {
        PropertyEditorManager.registerEditor(Color.class, javax.swing.beaninfo.SwingColorEditor.class);
        PropertyEditorManager.registerEditor(Font.class, javax.swing.beaninfo.SwingFontEditor.class);
        PropertyEditorManager.registerEditor(Border.class, javax.swing.beaninfo.SwingBorderEditor.class);
        PropertyEditorManager.registerEditor(Boolean.class, javax.swing.beaninfo.SwingBooleanEditor.class);
        PropertyEditorManager.registerEditor(boolean.class, javax.swing.beaninfo.SwingBooleanEditor.class);

        PropertyEditorManager.registerEditor(Integer.class, javax.swing.beaninfo.SwingIntegerEditor.class);
        PropertyEditorManager.registerEditor(int.class, javax.swing.beaninfo.SwingIntegerEditor.class);

        PropertyEditorManager.registerEditor(Float.class, javax.swing.beaninfo.SwingNumberEditor.class);
        PropertyEditorManager.registerEditor(float.class, javax.swing.beaninfo.SwingNumberEditor.class);

        PropertyEditorManager.registerEditor(java.awt.Dimension.class, javax.swing.beaninfo.SwingDimensionEditor.class);
        PropertyEditorManager.registerEditor(java.awt.Rectangle.class, javax.swing.beaninfo.SwingRectangleEditor.class);
        PropertyEditorManager.registerEditor(java.awt.Insets.class, javax.swing.beaninfo.SwingInsetsEditor.class);

        PropertyEditorManager.registerEditor(String.class, javax.swing.beaninfo.SwingStringEditor.class);
        PropertyEditorManager.registerEditor(Object.class, javax.swing.beaninfo.SwingObjectEditor.class);
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
        else if (source.equals(file_new_dialogMenu)) {
            structureVector.addElement(
                new StructureContainer(app, workarea));   //, new GUIDialog(app, null, null, app)));
        }
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
	    else if (source.equals(help_aboutMenu)) {
 //		showAboutDialog();
            handleSave("out.xml");
        }
	}
    }


    public void handleSave(String filename)  {
        try {
            FileOutputStream out = new FileOutputStream(filename);

//            String ext = BuilderFileFilter.getExtension(filename);

            ObjectOutput s = null;

//            if (ext.equals(XMLFileFilter.EXT))  {
//                s = new XMLOutputStream(out);
//            } else if (ext.equals(BSFileFilter.EXT))  {
//                s = new BeanScriptOutputStream(out);
//            } else if (ext.equals(JavaFileFilter.EXT)) {
                  s = new JavaOutputStream(out);
//            } else {
//                s = new ObjectOutputStream(out);
//            }

//            s.writeObject(panel.getRoot());
            s.writeObject(workarea); //selectedStruct.getTree());
            s.close();

//            this.filename = filename;
//            setTitle();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

//                                                Config.getImage("guibuilder.image.jbutton")

    private NewComponentAction[] createNewComponentActions()
    {
        BeanInfo jbuttonBeanInfo = null, jcheckboxBeanInfo = null, jradiobuttonBeanInfo = null, jtogglebuttonBeanInfo = null;

        try {
            jbuttonBeanInfo = Introspector.getBeanInfo(javax.swing.JButton.class);
            jcheckboxBeanInfo = Introspector.getBeanInfo(javax.swing.JCheckBox.class);
            jradiobuttonBeanInfo = Introspector.getBeanInfo(javax.swing.JRadioButton.class);
            jtogglebuttonBeanInfo = Introspector.getBeanInfo(javax.swing.JToggleButton.class);
        }
        catch(IntrospectionException iex)
        {

        }

        NewComponentAction jbuttonAction = new NewComponentAction(
                                                "JButton",
                                                new ImageIcon(jbuttonBeanInfo.getIcon(BeanInfo.ICON_COLOR_16x16))
                                               )
        {
            public GUIComponent createNewComponent(GUIComponentNode parent,
                                                StructureContainer structCont,
                                                GUIBuilderApp app)
            {
                return new GUIButton("Button",parent,structCont,app);
            }
        };

        NewComponentAction jcheckboxAction = new NewComponentAction(
                                                "JCheckBox",
                                                new ImageIcon(jcheckboxBeanInfo.getIcon(BeanInfo.ICON_COLOR_16x16))
                                               )
        {
            public GUIComponent createNewComponent(GUIComponentNode parent,
                                                StructureContainer structCont,
                                                GUIBuilderApp app)
            {
                return new GUICheckbox("Checkbox",parent,structCont,app);
            }
        };

        NewComponentAction jradiobuttonAction = new NewComponentAction(
                                                "JRadioButton",
                                                new ImageIcon(jradiobuttonBeanInfo.getIcon(BeanInfo.ICON_COLOR_16x16))
                                               )
        {
            public GUIComponent createNewComponent(GUIComponentNode parent,
                                                StructureContainer structCont,
                                                GUIBuilderApp app)
            {
                return new GUICheckbox("Checkbox",parent,structCont,app);
            }
        };

        NewComponentAction jtogglebuttonAction = new NewComponentAction(
                                                "JToggleButton",
                                                new ImageIcon(jtogglebuttonBeanInfo.getIcon(BeanInfo.ICON_COLOR_16x16))
                                               )
        {
            public GUIComponent createNewComponent(GUIComponentNode parent,
                                                StructureContainer structCont,
                                                GUIBuilderApp app)
            {
                return new GUICheckbox("Checkbox",parent,structCont,app);
            }
        };

        NewComponentAction jlabelAction = new NewComponentAction("JLabel")
        {
            public GUIComponent createNewComponent(GUIComponentNode parent,
                                                StructureContainer structCont,
                                                GUIBuilderApp app)
            {
                return new GUILabel("Label",parent,structCont,app);
            }
        };

        NewComponentAction jtextfieldAction = new NewComponentAction("JTextField")
        {
            public GUIComponent createNewComponent(GUIComponentNode parent,
                                                StructureContainer structCont,
                                                GUIBuilderApp app)
            {
                return new GUITextField("TextField",parent,structCont,app);
            }
        };

        NewComponentAction jpanelAction = new NewComponentAction("JPanel")
        {
            public GUIComponent createNewComponent(GUIComponentNode parent,
                                                StructureContainer structCont,
                                                GUIBuilderApp app)
            {
                GUIComponent tmpComponent = new GUIPanel(parent,structCont,app);

                ((GUIPanel)tmpComponent).setGUILayout((GUIComponentLayoutNode)(new GUIFlowLayout ((GUIComponentNode)tmpComponent,structCont,app)));

                return tmpComponent;
            }
        };

        return new NewComponentAction[] { jbuttonAction, jcheckboxAction, jradiobuttonAction,
                                         jtogglebuttonAction, jlabelAction, jtextfieldAction,
                                         jpanelAction };
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
     * This class handles clicks on the new component buttons (JButton etc.).
     *
     * Created: Oct 1, 1998.
     *
     * @author Morten Knudsen & Kent Hansen
     * @version 1.0
     */
    private class NewComponentListener implements ActionListener
    {
        public void actionPerformed (ActionEvent e)
        {
            if (buttonGroup.getSelection()!=null)
                setMode(ADDMODE);
            else
                setMode(SELECTMODE);

            if (selectedComponent != null)
            {
                if (selectedComponent instanceof GUIComponentLeaf)
                    ((GUIConcreteComponent)selectedComponent).getContainer().setHighlight(false);
                else if (selectedComponent instanceof GUIComponentNode)
                    ((GUIComponentNormalNode)selectedComponent).getGUILayout().setHighlight(false);

                selectedComponent = null;
            }

            buttonAction = (NewComponentAction) (((JToggleButton)e.getSource()).getAction());
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

/*                 {
                    public int getColumnCount() { return 2; }
                    public int getRowCount() { return 10;}
                    public Object getValueAt(int row, int col) {
                        if (selectedComponent != null)
                        {
                            try {
                                BeanInfo binfo = Introspector.getBeanInfo(selectedComponent.getClass());

                                PropertyDescriptor bdesc[] = binfo.getPropertyDescriptors();

                                if(row < bdesc.length) {

                                    if(col == 0)
                                        return bdesc[row].getDisplayName();
                                    else
                                    {
                                        PropertyEditor ed = PropertyEditorManager.findEditor(bdesc[row].getPropertyType());

                                        if(ed == null)
                                            return "no editor";

                                        if(ed.isPaintable() || ed.supportsCustomEditor())
                                            return "paint";
                                       else {
                                            try {
                                                String ret = ed.getAsText();
                                                if(ret != null)
                                                    return ret;
                                            }
                                            catch (Exception e)
                                            {

                                            }
                                       }
                                    }

                                }
                            }
                            catch(IntrospectionException iex)
                            {
                            }
                        }

                        return "";
                    }
                };
  */