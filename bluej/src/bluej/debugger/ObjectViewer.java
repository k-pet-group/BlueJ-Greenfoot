package bluej.debugger;

import bluej.Config;
import bluej.utility.Debug;
import bluej.pkgmgr.Package;
import bluej.utility.Utility;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.Hashtable;
import java.util.Enumeration;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.BorderFactory;
import javax.swing.border.Border;


/**
 ** @version $Id: ObjectViewer.java 62 1999-05-03 07:46:23Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** A window that displays the fields in an object (also know as an
 ** "Inspect window") and method call results.
 **/

public final class ObjectViewer extends JFrame

	implements ActionListener, ListSelectionListener
{
    private static final Font font = new Font("SansSerif", Font.PLAIN, Config.fontsize);
    private static final Color bgColor = new Color(208, 212, 208);

    private static String inspectTitle = Config.getString("debugger.objectviewer.title");
    private static String resultTitle = Config.getString("debugger.resultviewer.title");
    private static String staticListTitle = Config.getString("debugger.objectviewer.staticListTitle");
    private static String objListTitle = Config.getString("debugger.objectviewer.objListTitle");
    private static String inspectLabel = Config.getString("debugger.objectviewer.inspect");
    private static String getLabel = Config.getString("debugger.objectviewer.get");
    private static String close = Config.getString("close");
    private static String objectClassName = Config.getString("debugger.objectviewer.objectClassName");


    private boolean isInspection;	// true if inspecting object, false if
					//  displaying result
    private JList staticFieldList = null;
    private JList objFieldList = null;
    private JButton inspectBtn;
    private JButton getBtn;
    private DebuggerObject obj;
    private DebuggerObject selectedObject;
    private String selectedObjectName;
    private Package pkg;
    private String pkgScopeId;
    private boolean getEnabled;
    private String viewerId;		// a unique ID used to enter the 
					// viewer's object into the package
					// scope

    // === static methods ===

    protected static int count = 0;
    protected static Hashtable viewers = new Hashtable();
	
    /**
     * Return a ObjectViewer for an object. The viewer is visible.
     * This is the only way to get access to viewers - they cannot be
     * directly created.
     *
     * @arg inspection  True is this is an inspection, false for result 
     *			displays
     * @arg obj		The object displayed by this viewer
     * @arg name	The name of this object or "null" if it is not on the
     *			object bench
     * @arg pkg		The package all this belongs to
     * @arg getEnabled	if false, the "get" button is permanently disabled
     * @arg parent	The parent frame of this frame
     */
    public static ObjectViewer getViewer(boolean inspection, 
					 DebuggerObject obj, String name, 
					 Package pkg, boolean getEnabled,
					 JFrame parent)
    {
	ObjectViewer viewer = (ObjectViewer)viewers.get(obj);
	
	if(viewer == null) {
	    String id;
	    if(name==null) {
		id = "#viewer" + count; // # marks viewer for internal object
		count++;		//  which is not on bench 
	    }
	    else
		id = name;
	    viewer = new ObjectViewer(inspection, obj, pkg, id, getEnabled, parent);
	    viewers.put(obj, viewer);
	}
	viewer.update();

	// temporary
	if(obj.isArray())
	    Utility.showError(viewer, "Inspection of arrays is\n" +
			              "not yet implemented (sorry).");
	return viewer;
    }
	
    /**
     * Update all open viewers to show up-to-date values.
     */
    public static void updateViewers()
    {
	for (Enumeration e = viewers.elements(); e.hasMoreElements(); ) {
	    ObjectViewer viewer = (ObjectViewer)e.nextElement();
	    viewer.update();
	}
    }
	

    // === object methods ===

    /**
     * Constructor
     * Note: protected -- Objectviewers can only be created with the static
     * "getViewer" method. 
     */
    protected ObjectViewer(boolean inspect, DebuggerObject obj, 
			   Package pkg, String id, boolean getEnabled, 
			   JFrame parent)
    {
	super();

	isInspection = inspect;
	this.obj = obj;
	this.pkg = pkg;
	viewerId = id;
	this.getEnabled = getEnabled;
	pkgScopeId = Utility.quoteSloshes(pkg.getId());

	makeFrame(parent, isInspection, obj);
    }

    /**
     * Update the field values shown in this viewer to show current
     * object values.
     */
    public void update()
    {
	objFieldList.setListData(obj.getFields(isInspection));
	if(isInspection)
	    staticFieldList.setListData(obj.getStaticFields(true));
    }
	
    /**
     * Return this viewer's ID.
     */
    public String getId()
    {
	return viewerId;
    }
	
    /**
     * actionPerformed - something was done in the viewer dialog.
     *  Find out what it was and act.
     */
    public void actionPerformed(ActionEvent evt)
    {
	String cmd = evt.getActionCommand();

	// "Inspect" button
	if(inspectLabel.equals(cmd) && (selectedObject != null)) {
	    doInspect();
	}

	// "Get" button
	else if(getLabel.equals(cmd) && (selectedObject != null)) {
	    doGet();
	}

	// "Close" button
	else if(close.equals(cmd)) {
	    doClose();
	}
    }

    // ----- ListSelectionListener interface -----

    /**
     * The value of the list selection has changed. Update the selected
     * object. This needs some synchronisation, since we have two lists,
     * and we only want one selection.
     */
    public void valueChanged(ListSelectionEvent e)
    {
	if(e.getValueIsAdjusting())  // ignore mouse down, dragging, etc.
	    return;

	if(e.getSource() == staticFieldList) {		// click in static list
	    int slot = staticFieldList.getSelectedIndex();
	    if(slot == -1)
		return;

	    if(obj.staticFieldIsObject(slot)) {
		setCurrentObj(obj.getStaticFieldObject(slot),
			      obj.getStaticFieldName(slot));
		if(obj.staticFieldIsPublic(slot))
		    setButtonsEnabled(true, true);
		else
		    setButtonsEnabled(true, false);
	    }		
	    else {
		setCurrentObj(null, null);
		setButtonsEnabled(false, false);
	    }

	    objFieldList.clearSelection();
	}
	else if(e.getSource() == objFieldList) {	// click in object list
			
	    int slot = objFieldList.getSelectedIndex();
	    if(slot == -1)
		return;
			
	    if(obj.fieldIsObject(slot)) {
		setCurrentObj(obj.getFieldObject(slot),
			      obj.getFieldName(slot));
		if(obj.fieldIsPublic(slot))
		    setButtonsEnabled(true, true);
		else
		    setButtonsEnabled(true, false);
	    }
	    else {
		setCurrentObj(null, null);
		setButtonsEnabled(false, false);
	    }

	    if(staticFieldList != null)
		staticFieldList.clearSelection();
	}
    }

    // ----- end of ListSelectionListener interface -----

    /**
     * The "Inspect" button was pressed. Inspect the
     * selected object.
     */
    private void doInspect()
    {
	boolean isPublic = getBtn.isEnabled();
	ObjectViewer viewer = getViewer(true, selectedObject, null, pkg, 
					isPublic, this);

	// If the newly opened object is public, enter it into the
	// package scope, so that we can perform "Get" operations on it.

	if(isPublic)
	    Debugger.debugger.addObjectToScope(pkgScopeId, viewerId,
					       selectedObjectName,
					       viewer.viewerId);
    }

    /**
     * The "Get" button was pressed. Get the selected object on the
     * object bench.
     */
    private void doGet()
    {
	ObjectWrapper wrapper = new ObjectWrapper(selectedObject,
						  selectedObjectName,
						  pkg);
	pkg.getFrame().getObjectBench().add(wrapper);  // might change name

	// load the object into runtime scope
	Debugger.debugger.addObjectToScope(pkgScopeId, viewerId,
					   selectedObjectName,
					   wrapper.getName());
    }

    /**
     * Close this viewer. Don't forget to remove it from the list of open 
     * viewers.
     */
    private void doClose()
    {
	setVisible(false);
	dispose();
	viewers.remove(obj);

	// if the object shown here is not on the object bench, also
	// remove it from the package scope

	if(viewerId.charAt(0) == '#')
	    Debugger.debugger.removeObjectFromScope(pkgScopeId, viewerId);
    }

    /**
     * Store the object currently selected in the list.
     */
    private void setCurrentObj(DebuggerObject object, String name)
    {
	selectedObject = object;
	selectedObjectName = name;
    }

    /**
     * Enable or disable the Inspect and Get buttons.
     */
    private void setButtonsEnabled(boolean inspect, boolean get)
    {
	inspectBtn.setEnabled(inspect);
	if(getEnabled)
	    getBtn.setEnabled(get);
    }

    /**
     * Build the GUI interface.
     */
    private void makeFrame(JFrame parent, boolean isInspection, 
			   DebuggerObject obj)
    {
	JScrollPane scrollList;
	String className = "";

	if(isInspection) {
	    className = Utility.stripPackagePrefix(obj.getClassName());
	    setTitle(inspectTitle + " " + className);
	}
	else
	    setTitle(resultTitle);
	    
	setFont(font);
	setBackground(bgColor);

	addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent E) {
		doClose();
	    }
	});
		
	JPanel mainPanel = (JPanel)getContentPane();
	mainPanel.setBorder(BorderFactory.createEmptyBorder(16,20,10,20));

	JPanel listsPanel = new JPanel();
	listsPanel.setLayout(new BoxLayout(listsPanel, BoxLayout.Y_AXIS));

	int minRows = 2;
	int maxRows = 6;
	int rows;

	if(isInspection) {
	    JPanel titlePanel = new JPanel();
	    titlePanel.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
	    JLabel classNameLabel = new JLabel(objectClassName + " " + className);
	    titlePanel.add(classNameLabel, BorderLayout.CENTER);
	    mainPanel.add(titlePanel, BorderLayout.NORTH);

	    staticFieldList = new JList(new DefaultListModel());
	    staticFieldList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    staticFieldList.addListSelectionListener(this);
	    scrollList = new JScrollPane(staticFieldList);
	    scrollList.setColumnHeaderView(new JLabel(staticListTitle));
	    staticFieldList.setMaximumSize(new Dimension(400, 200));

	    // set the list sizes according to number of fields in object

	    rows = obj.getStaticFieldCount() + 1;
	    if(rows < minRows)
		rows = minRows;
	    else if(rows > maxRows)
		rows = maxRows;
	    staticFieldList.setVisibleRowCount(rows);

	    listsPanel.add(scrollList);
	    listsPanel.add(Box.createRigidArea(new Dimension(0,5)));
	}
	objFieldList = new JList(new DefaultListModel());
	objFieldList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	objFieldList.addListSelectionListener(this);
	scrollList = new JScrollPane(objFieldList);
	if(isInspection) {
	    scrollList.setColumnHeaderView(new JLabel(objListTitle));
	    rows = obj.getFieldCount() + 1;
	    if(rows < minRows)
		rows = minRows;
	    else if(rows > maxRows)
		rows = maxRows;
	}
	else
	    rows = 3;

	objFieldList.setVisibleRowCount(rows);
	listsPanel.add(scrollList);

	mainPanel.add(listsPanel, BorderLayout.CENTER);
		
	// Create panel with "inspect" and "get" buttons

	JPanel buttonPanel = new JPanel();
	buttonPanel.setLayout(new GridLayout(0, 1));

	inspectBtn = new JButton(inspectLabel);
	inspectBtn.addActionListener(this);
	inspectBtn.setEnabled(false);
	buttonPanel.add(inspectBtn);

	getBtn = new JButton(getLabel);
	getBtn.setEnabled(false);
	getBtn.addActionListener(this);
	buttonPanel.add(getBtn);

	JPanel buttonFramePanel = new JPanel();
	buttonFramePanel.setLayout(new BorderLayout(0,0));
	buttonFramePanel.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));
	buttonFramePanel.add(buttonPanel, BorderLayout.NORTH);
	mainPanel.add(buttonFramePanel, BorderLayout.EAST);

	// create bottom button pane with "Close" button

	buttonPanel = new JPanel();
	buttonPanel.setLayout(new FlowLayout());
	buttonPanel.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
	JButton button = new JButton(close);
	buttonPanel.add(button);
	button.addActionListener(this);

	mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		
	pack();
	if(isInspection)
	    Utility.tileWindow(this, parent);
	else
	    Utility.centreWindow(this, pkg.getFrame());
	setVisible(true);
	button.requestFocus();
    }
}
