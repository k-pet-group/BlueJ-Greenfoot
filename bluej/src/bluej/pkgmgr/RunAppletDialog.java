package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.DialogManager;

import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

/**
 ** @version $Id: RunAppletDialog.java 267 1999-11-10 02:53:02Z mik $
 ** @author Bruce Quig
 **
 ** Dialog for generating HTML and running applets.
 **/

public class RunAppletDialog extends JDialog

	implements ActionListener, ListSelectionListener
{
    // Internationalisation
    static final String okay = Config.getString("okay");
    static final String cancel = Config.getString("cancel");
    static final String runAppletTitle = Config.getString("pkgmgr.runApplet.title");
    static final String createWebPage = Config.getString("pkgmgr.runApplet.webPageLabel");

    static final int EXEC_APPLETVIEWER = 0;
    static final int EXEC_WEBBROWSER = 1;
    static final int GENERATE_PAGE_ONLY = 2;
  
    private static final String ADD_BUTTON = "Add";
    private static final String DELETE_BUTTON = "Delete";
    private static final String PARAM_NAME = "PARAM NAME";
    
    private static final String VALUE = "VALUE";
    private String webPageName;

    private JList parameterList;
    private JTextField paramNameField;
    private JTextField paramValueField;
    private JTextField heightField;
    private JTextField widthField;
    private JButton addButton;
    private JButton deleteButton;
    private JFrame parent;
    private DefaultListModel appletParameters;
    private JRadioButton generateWebPage;
    private JRadioButton runAppletViewer;
    private JRadioButton runWebBrowser;

    private boolean ok;		// result: which button?

    public RunAppletDialog(JFrame parent, String appletClassName)
    {
	super(parent, runAppletTitle, true);
	this.parent = parent;
	addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent E) {
		ok = false;
		setVisible(false);
	    }
	});
	JPanel mainPanel = (JPanel)getContentPane();  // has BorderLayout
	mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));	

	appletParameters = new DefaultListModel();
	webPageName = appletClassName + AppletClassRole.HTML_EXTENSION;

	// button panel at bottom of dialog
	JPanel buttonPanel = new JPanel();
	buttonPanel.setLayout(new FlowLayout());
	buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	JButton button;
	buttonPanel.add(button = new JButton(okay));
	button.addActionListener(this);
	getRootPane().setDefaultButton(button);
	buttonPanel.add(button = new JButton(cancel));
	button.addActionListener(this);
	getContentPane().add("South", buttonPanel);

 	GridBagLayout gridBag = new GridBagLayout();
	GridBagConstraints gridConstraints = new GridBagConstraints();
	gridConstraints.insets = new Insets(5, 5, 5, 5);
	JPanel webPanel = new JPanel();

	// Radio Button group for execution options
	ButtonGroup bGroup = new ButtonGroup();
	JPanel radioPanel = new JPanel();
	radioPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
	radioPanel.setLayout(new GridLayout(3, 1));
	generateWebPage = new JRadioButton("Generate web page only", false);
	radioPanel.add(generateWebPage);
	bGroup.add(generateWebPage);
	runAppletViewer = new JRadioButton("Run Applet in appletviewer", true);
	radioPanel.add(runAppletViewer);
	bGroup.add(runAppletViewer);
	runWebBrowser = new JRadioButton("Run Applet in web browser", false);
	radioPanel.add(runWebBrowser);
	bGroup.add(runWebBrowser);
	getContentPane().add("North", radioPanel);
	
	webPanel.setBorder(BorderFactory.createCompoundBorder(
			       BorderFactory.createLineBorder(Color.darkGray),
			       BorderFactory.createEmptyBorder(10, 10, 10, 10)));
	
	addGridBagComponent(webPanel, gridBag, gridConstraints, new JLabel("Height = "), 
			    0, 1, 1, 1, GridBagConstraints.EAST);

	heightField = new JTextField(5);
	addGridBagComponent(webPanel, gridBag, gridConstraints, heightField, 
			    1, 1, 1, 1, GridBagConstraints.WEST);

	addGridBagComponent(webPanel, gridBag, gridConstraints, new JLabel("Width = "),
			    2, 1, 1, 1, GridBagConstraints.EAST);

	widthField = new JTextField(5);
	addGridBagComponent(webPanel, gridBag, gridConstraints, widthField, 
			    3, 1, 1, 1, GridBagConstraints.WEST);
	
	addGridBagComponent(webPanel, gridBag, gridConstraints, new JLabel("Add new parameter"), 
			    4, 2, 2, 1, GridBagConstraints.CENTER); 

	parameterList = new JList(appletParameters);
	parameterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	parameterList.setModel(appletParameters);
	parameterList.addListSelectionListener(this);
	JScrollPane parameterScroller = new JScrollPane(parameterList);
	parameterScroller.setColumnHeaderView(new JLabel("Applet parameters", JLabel.CENTER));
	addGridBagComponent(webPanel, gridBag, gridConstraints, parameterScroller, 
			    0, 2, 4, 4, GridBagConstraints.CENTER);

	addGridBagComponent(webPanel, gridBag, gridConstraints, new JLabel("Name = "), 
			    4, 3, 1, 1, GridBagConstraints.WEST);

	paramNameField = new JTextField(16);
	addGridBagComponent(webPanel, gridBag, gridConstraints, paramNameField, 
			    5, 3, 1, 1, GridBagConstraints.WEST);

	addGridBagComponent(webPanel, gridBag, gridConstraints, new JLabel("Value = "), 
			    4, 4, 1, 1, GridBagConstraints.WEST);

	paramValueField = new JTextField(16);
	addGridBagComponent(webPanel, gridBag, gridConstraints, paramValueField, 
			    5, 4, 1, 1, GridBagConstraints.WEST);

	deleteButton = new JButton(DELETE_BUTTON);
	deleteButton.addActionListener(this);
	addGridBagComponent(webPanel, gridBag, gridConstraints, deleteButton, 
			    4, 5, 1, 1, GridBagConstraints.EAST);
 	deleteButton.setEnabled(false);

	addButton = new JButton(ADD_BUTTON);
	addButton.addActionListener(this);
	addGridBagComponent(webPanel, gridBag, gridConstraints, addButton, 
			    5, 5, 1, 1, GridBagConstraints.WEST);
	addButton.setEnabled(true);

 	getContentPane().add("Center", webPanel);
		
	DialogManager.centreDialog(this);
    }
  
    
    /**
     * Method to simplify adding components to a gridbag layout and modify constraints
     * @param container  the container the component is to be added to
     * @param layout  the GridBagLayout object to be used
     * @param constraints  the GridBagConstraints object being used
     * @param component  the component to be added
     * @param gridx  x coordinate for component starting position
     * @param gridy  y coordinate for component starting position
     * @param gridWidth  number of grid cells for width of component
     * @param gridHeight  number of grid cells for height of component
     * @param anchor  the alignment of component within grid cell
     */
    private void addGridBagComponent(Container container, GridBagLayout layout, 
				     GridBagConstraints constraints, Component component, 
				     int gridx, int gridy, int gridWidth, int gridHeight, int anchor)
    {
	constraints.gridx = gridx;
	constraints.gridy = gridy;
	constraints.gridwidth = gridWidth;
	constraints.gridheight = gridHeight;
	constraints.anchor = anchor;
	layout.setConstraints(component, constraints);
	// check that this layout has not already been set
	if(!container.getLayout().equals(layout)) 
	    container.setLayout(layout);
	container.add(component);
    }


    /**
     * Show this dialog and return true if "OK" was pressed, false if
     * cancelled.
     */
    public boolean display()
    {
	ok = false;
	pack();
	setVisible(true);
	return ok;
    }


    /**
     * Show this dialog and return true if "OK" was pressed, false if
     * cancelled.
     */
    public String getWebPageName()
    {
	return webPageName;
    }

    public void actionPerformed(ActionEvent evt)
    {
	String cmd = evt.getActionCommand();

	if(okay.equals(cmd))
	    doOK();
	else if(cancel.equals(cmd))
	    doCancel();
	// adding an applet parameter
	else if(ADD_BUTTON.equals(cmd)) {
	    if(!paramNameField.getText().equals("") && !paramValueField.getText().equals("")) {
		addAppletParameter();
		paramNameField.requestFocus();
	    }
	}
	// deleting selected applet parameter
	else if(DELETE_BUTTON.equals(cmd)){
	    appletParameters.remove(parameterList.getSelectedIndex());
	    deleteButton.setEnabled(false);
	}
    }


    /**
     * Add a parameter to the applet parameter list.
     */
    public void addAppletParameter()
    {
	String appletParameter = 
	    "<" + PARAM_NAME + " = " + paramNameField.getText() 
	    + "   " + VALUE + " = " + paramValueField.getText() + ">";
	appletParameters.addElement(appletParameter);
	paramNameField.setText("");
	paramValueField.setText("");
    }


    /**
     * Close action when OK is pressed.
     */
    public void doOK()
    {
	if(!checkFieldsAreValid()) {
	    DialogManager.showError(parent, "Applet Height and Width must be specified");
	}
	else { // collect information from fields
	    ok = true;
	    setVisible(false);
	}
    }


    /**
     * Close action when Cancel is pressed.
     */
    public void doCancel()
    {
	ok = false;
	setVisible(false);
    }


    /**
     * Check that required fields have entries.  
     * There is no checking the validity of what is entered.
     * @return true if both width and height fields are not empty
     */
    public boolean checkFieldsAreValid()
    {
	return (!widthField.getText().equals("") && !heightField.getText().equals(""));
    }


    /**
     * Returns height of height text field.
     * @return height of applet as a String
     */
    public String getAppletHeight()
    {
	return heightField.getText();
    }


    /**
     * Returns value of width text field.
     * @return width of Applet as a String
     */
    public String getAppletWidth()
    {
	return widthField.getText();
    }


   /**
     * sets value of height text field.
     * @param height value to set  in field
     */
    public void setAppletHeight(int height)
    {
	heightField.setText(String.valueOf(height));
    }


    /**
     * sets value of width text field.
     * @param width value to set in field
     */
    public void setAppletWidth(int width)
    {
	widthField.setText(String.valueOf(width));
    }


    /**
     * Returns applet parameters.
     * @return applet parameters as an array of Strings or null if no parameters
     */
    public String[] getAppletParameters()
    {
	Object[] objectArray = appletParameters.toArray();
	String[] paramStringArray = new String[objectArray.length];
	for(int i = 0; i < objectArray.length; i++)
	    paramStringArray[i] = (String)objectArray[i];

	return paramStringArray;
    }

  /**
     * Returns applet parameters.
     * @return applet parameters as an array of Strings or null if no parameters
     */
    public void setAppletParameters(String[] parameters)
    {
	for(int i = 0; i < parameters.length; i++)
	    appletParameters.addElement(parameters[i]);

    }


    /**
     * Returns an int representing the radio button chosen 
     * for execution option.
     * @return int representing index of radio button selected
     */
    public int getAppletExecutionOption()
    {
	if(runAppletViewer.isSelected())
	    return EXEC_APPLETVIEWER;
	else if(runWebBrowser.isSelected())
	    return  EXEC_WEBBROWSER;
	else
	    return  GENERATE_PAGE_ONLY;
    }


    // ----- ListSelectionListener interface -----

    /**
     * The value of the list selection has changed. 
     */
    public void valueChanged(ListSelectionEvent e)
    {
	//if(e.getValueIsAdjusting())  // ignore mouse down, dragging, etc.
	//    return;
	if(!parameterList.isSelectionEmpty())
	    deleteButton.setEnabled(true);

    }

    // ----- end of ListSelectionListener interface -----



}
