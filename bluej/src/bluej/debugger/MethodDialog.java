package bluej.debugger;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.MultiLineLabel;
import bluej.utility.Utility;
import bluej.pkgmgr.Package;
import bluej.views.ConstructorView;
import bluej.views.MemberView;
import bluej.views.MethodView;
import bluej.debugger.CallHistory;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.*;


/**
 ** @version $Id: MethodDialog.java 69 1999-05-11 04:23:02Z bruce $
 **
 ** @author Michael Cahill
 ** @author Bruce Quig
 ** @author Michael Kolling
 **
 **  This dialog is used for an interactive method call. The call
 **  can be an object creation or an invocation of an object method.
 **  A new instance of this dialog is created for each method.
 */

public class MethodDialog extends JDialog 

	implements ActionListener, FocusListener
{
    static final int MD_CREATE = 0;
    static final int MD_CALL = 1;

    static final int OK = 0;
    static final int CANCEL = 1;

    int dialogType;
	
    // Window Titles
    static final String wCreateTitle = Config.getString("pkgmgr.methodCall.titleCreate");
    static final String wCallRoutineTitle = Config.getString("pkgmgr.methodCall.titleCall");
    // MD_CREATE Specific
    static final String sNameOfInstance = Config.getString("pkgmgr.methodCall.namePrompt");
    static final String emptyFieldMsg = Config.getString("error.methodCall.emptyField");
    static final String illegalNameMsg = Config.getString("error.methodCall.illegalName");
    static final String duplicateNameMsg = Config.getString("error.methodCall.duplicateName");

    static final String okayCommand = Config.getString("okay");

    static final String commentSlash = "   ";

    // Buttons
    private JButton bOk, bCancel;

    private String methodName;

    // Text Area
    private JPanel descPanel;

    private JComboBox[] params;
    private MultiLineLabel status;
    private MethodDialogWatcher watcher;
    private JTextField instanceNameText;
    private JTextField focusedTextField;
    private JLabel callLabel;

    private CallHistory history;
    private ObjectBench bench;
    private String[] paramNames;
    private Class[] paramClasses;
    private boolean emptyField = false;

    public MethodDialog(Package pkg, String className, String instanceName, MemberView method)
    {
	super(pkg.getFrame(), false);
	JPanel butPanel = new JPanel();
	JPanel centerPanel = new JPanel();
	boolean hasArgs = false;
	history = pkg.getCallHistory();
	bench = pkg.getBench();
	status = new MultiLineLabel("\n\n");
	status.setForeground(Color.red);
	JPanel statusPanel = new JPanel();
	statusPanel.setMinimumSize(new Dimension(120,40));
	
	centerPanel.setLayout(new BorderLayout());

	// Set up Cursor bug workaround to allow a WAIT_CURSOR to be shown
	Component glass = getGlassPane(); 
	glass.setCursor(Cursor.getPredefinedCursor
			(Cursor.WAIT_CURSOR));
	glass.addMouseListener(new MouseAdapter() {
	    public void mousePressed(MouseEvent e) {}
	});
	// end of Workaround


	// Find out the type of dialog and if hasArgs
	if( method instanceof MethodView ) {
	    dialogType = MD_CALL;
	    methodName = ((MethodView)method).getName();
	    hasArgs = ((MethodView)method).hasParameters();
	}		
	else if (method instanceof ConstructorView ) {
	    dialogType = MD_CREATE;
	    hasArgs = ((ConstructorView)method).hasParameters();
	}

	getContentPane().setLayout(new BorderLayout());

	descPanel = new JPanel();
	descPanel.setLayout(new FlowLayout());


	// Create the Button Panel
	butPanel.setLayout(new FlowLayout());
	butPanel.add(bOk = new JButton(okayCommand));
	bOk.addActionListener(this);

	butPanel.add(bCancel = new JButton(Config.getString("cancel")));
	bCancel.addActionListener(this);

	getContentPane().add("South", butPanel);
	getContentPane().add("North", descPanel);
	getRootPane().setDefaultButton(bOk);

	// parse method signature for param fields, there may be a better way
	String[] paramNames = null;	
	if(hasArgs) 
	    paramNames = parseParamNames(method.getLongDesc());

	//
	// Set dialog items depends on the Dialog type
	//
	switch (dialogType) {
	
	case MD_CALL:		
	    makeCallDialog(instanceName, method, paramNames, centerPanel);
	    break;

	case MD_CREATE:
	    makeCreateDialog(className, instanceName, method, paramNames, 
			     centerPanel);
	    break;
	    
	default:	// error!
	    throw new Error("Invalid MethodDialog type " + dialogType);
	}
	
	statusPanel.add(status);	
	centerPanel.add("Center", statusPanel);
	
    
	getContentPane().add("Center", centerPanel);
		
	// Set some attributes for this DialogBox
	Utility.centreDialog(this);

	// Close Action when close button is pressed
	addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent event) {
		setVisible(false);
	    }
	});
    }


    /**
     * makeCallDialog - create a dialog to make a method call
     */
    private void makeCallDialog(String instanceName, MemberView method, 
				String[] paramNames, JPanel panel)
    {
	JPanel tmpPanel;

	setTitle(wCallRoutineTitle);
		
	if (paramNames != null) {

	    MethodView methView = (MethodView)method;
	    paramClasses = methView.getParameters();

	    tmpPanel = new JPanel();
	    GridBagLayout gridBag = new GridBagLayout();
	    tmpPanel.setLayout(gridBag);
	    GridBagConstraints constraints = new GridBagConstraints();
	    constraints.insets = new Insets(2,2,2,2);

	    callLabel = new JLabel("", JLabel.RIGHT);
	    setCallLabel(instanceName, methodName);

	    gridBag.setConstraints(callLabel, constraints);
	    tmpPanel.add(callLabel);

	    params = new JComboBox[paramClasses.length];

	    for (int i = 0; i < paramClasses.length; i++) {
		constraints.gridx = 1;
		params[i] = new JComboBox(history.getHistory(paramClasses[i]));
		params[i].setEditable(true);
		params[i].getEditor().setItem("");
		// Action listener commented out for present, 
		// need to disable tab firing action event

		// add FocusListener for text insertion
		((JTextField)params[i].getEditor().getEditorComponent()).addFocusListener(this);

		constraints.gridy = i;
		gridBag.setConstraints(params[i], constraints);
		tmpPanel.add(params[i]);
		constraints.gridx = 2;
		constraints.anchor = GridBagConstraints.WEST;

		JLabel eol = new JLabel(",  " + commentSlash + paramNames[i], JLabel.LEFT);
		if (i == (paramClasses.length - 1))
		    if(paramClasses.length == 1)
			eol.setText(")");
		    else
			eol.setText(")  " + commentSlash + paramNames[i] );
				    
		gridBag.setConstraints(eol,constraints);
		tmpPanel.add(eol);
	    }
	    tmpPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
	    panel.add("North", tmpPanel);
	}
    } // makeCallDialog


    /**
     * setCallLabel - set the text of the label showing the call to be
     *  made.
     */
    private void setCallLabel(String instanceName, String methodName)
    {
	if (callLabel != null)
	    callLabel.setText(instanceName + "." + methodName + " (");
    }

    /**
     * makeCreateDialog - create a dialog to create an object (including 
     *  constructor call)
     */
    private void makeCreateDialog(String className, String instanceName,
				  MemberView method, String[] paramNames,
				  JPanel panel)
    {
	JPanel tmpPanel;

	setTitle(wCreateTitle);

	JLabel instName = new JLabel(sNameOfInstance);
	instanceNameText = new JTextField(instanceName, 14);
				
	if(paramNames != null) {
	    ConstructorView consView = (ConstructorView)method;
	    paramClasses = consView.getParameters();

	    tmpPanel = new JPanel();
	    GridBagLayout gridBag = new GridBagLayout();
	    tmpPanel.setLayout(gridBag);
	    GridBagConstraints constraints = new GridBagConstraints();
	    constraints.insets = new Insets(2,2,2,2);

	    gridBag.setConstraints(instName, constraints);
	    tmpPanel.add(instName);
		    
	    constraints.gridx = 1;
	    constraints.gridwidth = 2;
	    constraints.anchor = GridBagConstraints.WEST;
	    gridBag.setConstraints(instanceNameText, constraints);
	    tmpPanel.add(instanceNameText);

	    JLabel name = new JLabel("new " + className + "(", JLabel.RIGHT);
	    constraints.gridwidth = 1;
	    constraints.gridx = 0;
	    constraints.anchor = GridBagConstraints.EAST;
	    gridBag.setConstraints(name,constraints);
	    tmpPanel.add(name);

	    params = new JComboBox[paramClasses.length];
		    
	    for (int i = 0; i < paramClasses.length; i++) {
		constraints.gridx = 1;
		params[i] = new JComboBox(history.getHistory(paramClasses[i]));
		params[i].setEditable(true);
		params[i].getEditor().setItem("");

		// add FocusListener for text insertion
		((JTextField)params[i].getEditor().getEditorComponent()).addFocusListener(this);
	  
		constraints.gridy = (i + 1);
		gridBag.setConstraints(params[i], constraints);
		tmpPanel.add(params[i]);
		constraints.gridx = 2;
		constraints.anchor = GridBagConstraints.WEST;
		JLabel eol = new JLabel(",  " + commentSlash + paramNames[i], JLabel.LEFT);
		if (i == (paramClasses.length - 1)) {
		    if(paramClasses.length == 1)
			eol.setText(")");
		    else
			eol.setText(")  " + commentSlash + paramNames[i]);
		}
		gridBag.setConstraints(eol,constraints);
		tmpPanel.add(eol);
	    }
	}
	else {
	    tmpPanel = new JPanel();
	    tmpPanel.add(instName);
	    tmpPanel.add(instanceNameText);
	}
	tmpPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
	panel.add("North", tmpPanel);
    } // makeCreateDialog


    /**
     * parseParamNames - parses a long description of a method and returns 
     * the argument types and names as an array of Strings.
     *
     * @return an array of Strings representing method arguments. 
     */
    public String[] parseParamNames(String longMethodName)
    { 
	String[] argNames = null;
	StringTokenizer tokenizer = new StringTokenizer(longMethodName," ,()",true);
	String arg = "";
	boolean inArg = false;
	Vector args = new Vector();
	
	while( tokenizer.hasMoreTokens()){
	    String token = tokenizer.nextToken();
	    
	    if (token.equals("(")){
		inArg = true;
		arg = "";
	    }
	    else if (token.equals(",")){
		args.addElement(arg);
		inArg = true;
		arg = "";
	    }
	    else if (token.equals(")")){
		args.addElement(arg);
		inArg = false;
	    }
	    else {
		if(inArg)
		    arg += token; 
	    }	
	}
	//Debug.message("params = " + args.toString());
	argNames = new String[args.size()];
	for(int i = 0; i < args.size(); i++)
	    argNames[i] = ((String)args.elementAt(i)).trim();
	
	return argNames;
    } // parseParamNames

    /**
     * Set the visibility of the dialog, clearing parameter edit fields 
     * and setting focus.
     */
    public void setVisible(boolean show)
    {
	super.setVisible(show);
	// reset status label message
	setMessage("");

	if (show) {
	    // clear params from any JComboBoxes
	    clearParameters();

	    if(params != null) {
		//params[0].requestFocus();
		((JTextField)params[0].getEditor().getEditorComponent()).requestFocus();
	    }
	    else if(dialogType == MD_CREATE) {
		instanceNameText.selectAll();
		instanceNameText.requestFocus();
	    }
	    else
		bOk.requestFocus();
	}
    }

    /**
     * Process action events
     */	
    public void actionPerformed(ActionEvent event)
    {
	Object obj = event.getSource();

	if (obj == bOk) 
	    doOk();
	else if (obj == bCancel)
	    doCancel();
    }

    /**
     * doOk - Process an "Ok" event to invoke a Constructor or Method. 
     *  Collects arguments and calls watcher objects (Invoker).
     */		
    public void doOk()
    {
	// Debug.message("doOk()");
	paramNames = getParamNames();  // sets "emptyField"

	if(dialogType == MD_CREATE) {
	    if(! Utility.isIdentifier(getNewInstanceName())) {
		setMessage(illegalNameMsg);
		return;
	    }
	    if(bench.hasObject(getNewInstanceName())) {
		setMessage(duplicateNameMsg);
		return;
	    }
	}

	if(emptyField) {
	    setMessage(emptyFieldMsg);
	    emptyField = false;
	}
	else {
	    setWaitCursor(true);
	    callWatcher(OK);
	}
    }

    /**
     * Process a "Cancel" event to cancel a Constructor or Method call.
     * Makes dialog invisible.
     */		
    public void doCancel()
    {
	callWatcher(CANCEL);
	this.setVisible(false);
    }

    /**
     * addWatcher - Set a Watcher
     */
    public void addWatcher(MethodDialogWatcher w)
    {
	if (w != null) 
	    watcher = w;
    }	

    /**
     * callWatcher - notify watcher of dialog events.
     */	
    public void callWatcher(int event)
    {
	if (watcher != null)
	    watcher.methodDialogEvent(this, event);
    }

    /**
     * setDescription - display a new description in the dialog
     */
    public void setDescription(MultiLineLabel label)
    {
	descPanel.removeAll();
	descPanel.add(label);
	invalidate();
	validate();
    }
	
    /**
     * Get arguments from param entry fields as array of strings.
     * May be null (if there are no arguments).
     */
    public String[] getParamNames()
    {
	String[] args = null;

	if(params != null) {
	    // Debug.message("params not null");
	    args = new String[params.length];
	    for(int i = 0; i < params.length; i++) {
	        String arg = (String)params[i].getEditor().getItem();
		if(arg == null || arg.trim().equals(""))
		    emptyField = true;
		args[i] = arg;
	    }
	}
	return args;
    }

    /**
     * Inserts text into edit field (JComboBox) that has focus. 
     * Has problems at present (09/02/99) finding focused component.
     * Swing bug?
     */	
    public void insertText(String text)
    {
	if(params != null) {
	    if(focusedTextField != null) {
		focusedTextField.setText(text);
		// bring to front after insertion, doesn't seem to work.
		this.show();
	    }
	}
    }

    /**
     * setMessage - Sets a status bar style message for the dialog mainly
     *  for reporting back compiler errors upon method calls. 
     */
    public void setMessage(String message)
    {
	status.setText(message);
	pack();
	invalidate();
	validate();
    }

    /**
     * setInstanceName - set the name of the instance shown in the label
     *  for method call dialogs.
     */
    public void setInstanceName(String instanceName)
    {
	setCallLabel(instanceName, methodName);
    }

    /**
     * setNewInstanceName - set the field for the new instance name for
     *  contruction dialogs.
     */
    public void setNewInstanceName(String name)
    {
	instanceNameText.setText(name);
    }

    /**
     * getNewInstanceName - get the contents of the instance name field.
     */
    public String getNewInstanceName()
    {
	if(instanceNameText == null)
	    return "";
	else
	    return instanceNameText.getText().trim();
    }

    /**
     * getArgs - Return an array with parameter names. "null" if there
     *  are no parameters.
     */
    public String[] getArgs()
    {
	return paramNames;
    }

    /**
     * getArgTypes - Get an array with the classes of the parameters for
     *  this method. "null" if there are no parameters.
     */
    public Class[] getArgTypes()
    {
	return paramClasses;
    }


    /**
     * Workaround for udating model problems with JComboBox.
     * Updates CallHistory and resets model to updated Vectors.  Ugly and brutal
     * but corrects problems with JComboBox update problems. 
     *
     */
    public void updateParameters()
    {
	if(params != null) {
	    for(int i = 0; i < params.length; i++) {
		history.addCall(paramClasses[i], (String)params[i].getEditor().getItem());
		params[i].setModel(new DefaultComboBoxModel(history.getHistory(paramClasses[i])));
	    }	    
	}
    }

    /**
     * Clear parameters of any param entry fields 
     *
     */
    private void clearParameters()
    {
	//Debug.message("Calling clearParameters");
	if(params != null) {
	    for(int i = 0; i < params.length; i++) {
		((JTextField)params[i].getEditor().getEditorComponent()).setText("");
	    }	    
	}
    }

    /**
     * setWaitCursor - Sets the cursor to "wait" style cursor, using swing 
     *  bug workaround at present 
     */
    public void setWaitCursor(boolean state)
    {
	getGlassPane().setVisible(state);
    }

    // --- FocusListener interface methods ---

    /**
     * FocusEventListener method fired when watched component gains focus 
     * Assigns focusedTextField to work around difficulties in JComboBox
     * firing focus gained events.
     */
    public void focusGained(FocusEvent fe)
    {
	if(fe.getComponent() instanceof JTextField)
	    focusedTextField = (JTextField)fe.getComponent();
    }

    /**
     * FocusEventListener method fired when watched component loses focus 
     * Does nothing at present except for debug message.
     * 
     */   
    public void focusLost(FocusEvent fe)
    {
	// Debug.message(" Focus Lost: " + fe.paramString());
    }

}
