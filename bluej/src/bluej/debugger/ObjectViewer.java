package bluej.debugger;

import bluej.Config;
import bluej.utility.Debug;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.Utility;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.JSplitPane;

/**
 ** @version $Id: ObjectViewer.java 239 1999-08-17 07:55:00Z mik $
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

    private static final int VISIBLE_ARRAY_FIELDS = 45;
    private static final int ARRAY_QUERY_INDEX = 40;
    private static final int ARRAY_QUERY_SLOT_VALUE = -2;

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
    private boolean isInScope;
    private boolean queryArrayElementSelected;

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
     * @param inspection  True is this is an inspection, false for result 
     *			displays
     * @param obj		The object displayed by this viewer
     * @param name	The name of this object or "null" if it is not on the
     *			object bench
     * @param pkg		The package all this belongs to
     * @param getEnabled	if false, the "get" button is permanently disabled
     * @param parent	The parent frame of this frame
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
     * "getViewer" method. 'pkg' may be null if getEnabled is false.
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
	isInScope = false;
	if(pkg == null) {
	    if(getEnabled)
		Debug.reportError("cannot enable 'get' with null package");
	    pkgScopeId = "";
	}
	else
	    pkgScopeId = Utility.quoteSloshes(pkg.getId());

	makeFrame(parent, isInspection, obj);
    }

    /**
     * Update the field values shown in this viewer to show current
     * object values.
     */
    public void update()
    {
	// if is an array and needs compressing
	if(obj.isArray() && obj.getInstanceFieldCount() > VISIBLE_ARRAY_FIELDS) 
	    objFieldList.setListData(
		compressArrayList(obj.getInstanceFields(isInspection)));
	else 
	    objFieldList.setListData(obj.getInstanceFields(isInspection));

	// static fields only applicable if not an array and list not null
	if(isInspection && !obj.isArray() && staticFieldList != null)
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
	if(inspectLabel.equals(cmd)) { // null objects checked for inside doInspect
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
		
		if(obj.staticFieldIsPublic(slot) && !selectedObject.isArray())
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

	    // add index to slot method for truncated arrays
	    if(obj.isArray()) 
		slot = indexToSlot(objFieldList.getSelectedIndex());

	    // occurs if valueChanged picked up a clearSelection event from
	    // the list
	    if(slot == -1)
		return;

	    queryArrayElementSelected = (slot == ARRAY_QUERY_SLOT_VALUE);

	    // for array compression..
	    if(queryArrayElementSelected) {	  // "..." in Array inspector 
	    	setCurrentObj(null, null);	  //  selected
		// check to see if elements are objects, 
		// using the index replaced by this element
		if(obj.instanceFieldIsObject(ARRAY_QUERY_INDEX))
		    setButtonsEnabled(true, false);
		else
		    setButtonsEnabled(false, false);
	    }
	    else if(obj.instanceFieldIsObject(slot)) {
		setCurrentObj(obj.getInstanceFieldObject(slot),
			      obj.getInstanceFieldName(slot));
		if(obj.instanceFieldIsPublic(slot) && !selectedObject.isArray())
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
     * Converts list index position to that of array element position in arrays.
     * For small arrays this is a direct match to list index, in larger arrays
     * (greater than 45 elements) the first 40 , a wildcard (...) and the last
     * 4 are shown.
     *
     * @param  listIndexPosition  the position selected in the list
     * @return the translated index of field array element
     */
    private int indexToSlot(int listIndexPosition)
    {
	int fieldCount = obj.getInstanceFieldCount();
	if(fieldCount < VISIBLE_ARRAY_FIELDS)
	    return listIndexPosition;
	else {
 	    int index;
	    if(listIndexPosition < ARRAY_QUERY_INDEX)
		index = listIndexPosition;
	    else if(listIndexPosition == ARRAY_QUERY_INDEX)
		index = ARRAY_QUERY_SLOT_VALUE;
	    else
		index = listIndexPosition + (fieldCount - VISIBLE_ARRAY_FIELDS);

	    return index;
	}
    }



    /**
     * Compresses an array field name list to a maximum of 45 elements
     * for display purposes.  When a selected element is chosen 
     * indexToSlot allows the selection to be converted to the original 
     * array element position.
     *
     * @param  fullArrayFieldList the full field list for an array
     * @return the compressed array
     */
    private Vector compressArrayList(Vector fullArrayFieldList)
    {
	//** rewrite using Vector "remove" and "add"
	if(fullArrayFieldList.size() > VISIBLE_ARRAY_FIELDS) {
	    Vector newArray = new Vector(VISIBLE_ARRAY_FIELDS);
	    
	    for(int i = 0; i < VISIBLE_ARRAY_FIELDS; i++) {
		// first 40 elements are the same
		if(i < ARRAY_QUERY_INDEX)
		    newArray.add(fullArrayFieldList.elementAt(i));
		else if(i == ARRAY_QUERY_INDEX) {
		    String queryElement = (String)fullArrayFieldList.elementAt(i);
		    int bracketPos = queryElement.indexOf("[");
		     
		    newArray.add(
			queryElement.substring(0, (bracketPos-1)) + "[...]");
		}
		else 
		    newArray.add(fullArrayFieldList.elementAt(
		       i + fullArrayFieldList.size() - VISIBLE_ARRAY_FIELDS));
	    }
	    return newArray;
	}
	else
	    return fullArrayFieldList;
    }

   /**
     * Shows a dialog to select array element for inspection
     */
    private void selectArrayElement()
    {
	    String response = Utility.askString(this, 
					"Enter array element index [...]", 
					"Array Inspection", 
					null);
	    if(response != null) {
		try {
		    int slot = Integer.parseInt(response);
		    
		    // check if within bounds of array
		    if(slot >= 0 && slot < obj.getInstanceFieldCount()) {

			// if its an object set as current object
			if(obj.instanceFieldIsObject(slot) ) {
			    setCurrentObj(obj.getInstanceFieldObject(slot),
					  obj.getInstanceFieldName(slot));
			    setButtonsEnabled(true, false);
			}
			else
			    setButtonsEnabled(false, false);
		    }
		    else // not within array bounds
			Utility.showError(this, 
			   "Element specified is not within array bounds");
		    
		} 
		catch(NumberFormatException e) {
		    // input could not be parsed, eg. non integer value
		    setCurrentObj(null, null);
		    Utility.showError(this, 
			   "Unable to access the array element specified");
		}
	
	    }
	    else {  
		// set current object to null to avoid re-inspection of 
		// previously selected wildcard 
	    	setCurrentObj(null, null);
	    }
    }


    /**
     * The "Inspect" button was pressed. Inspect the
     * selected object.
     */
    private void doInspect()
    {
	// if need to query array element
	if(queryArrayElementSelected) {
	    selectArrayElement();
	}
	
	if(selectedObject != null) {

	    boolean isPublic = getBtn.isEnabled();
	    ObjectViewer viewer = getViewer(true, selectedObject, null, pkg, 
					    isPublic, this);

	    // If the newly opened object is public, enter it into the
	    // package scope, so that we can perform "Get" operations on it.
	    if(isPublic)
		viewer.addToScope(viewerId, selectedObjectName);
	}
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
     * 
     * 
     */
    private void addToScope(String parentViewerId, String objectName)
    {
	Debugger.debugger.addObjectToScope(pkgScopeId, parentViewerId,
					   objectName, viewerId);
	isInScope = true;
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

	if(isInScope && (viewerId.charAt(0) == '#'))
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
	JScrollPane staticScrollPane = null;
	JScrollPane objectScrollPane = null;
	String className = "";

	if(isInspection) {
	    className = Utility.stripPackagePrefix(obj.getClassName());
	    //className = obj.getClassName();
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

	int maxRows = 8;
	int rows;

	if(isInspection) {
	    JPanel titlePanel = new JPanel();
	    titlePanel.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
	    JLabel classNameLabel = new JLabel(objectClassName + " " + className);
	    titlePanel.add(classNameLabel, BorderLayout.CENTER);
	    mainPanel.add(titlePanel, BorderLayout.NORTH);

	    if(!obj.isArray()) {
		staticFieldList = new JList(new DefaultListModel());
		staticFieldList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		staticFieldList.addListSelectionListener(this);
		staticScrollPane = new JScrollPane(staticFieldList);
		staticScrollPane.setColumnHeaderView(new JLabel(staticListTitle));

		// set the list sizes according to number of fields in object
		rows = obj.getStaticFieldCount() + 2;
		if(rows > maxRows)
		    rows = maxRows;
		staticFieldList.setVisibleRowCount(rows);
	    }
	}

	objFieldList = new JList(new DefaultListModel());
	objFieldList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	objFieldList.addListSelectionListener(this);
	objectScrollPane = new JScrollPane(objFieldList);

	if(isInspection) {
	    objectScrollPane.setColumnHeaderView(new JLabel(objListTitle));
	    rows = obj.getInstanceFieldCount() + 2;
	    if(rows > maxRows)
		rows = maxRows;
	}
	else
	    rows = 3;

	objFieldList.setVisibleRowCount(rows);

	if(isInspection && !obj.isArray()) {
	    JSplitPane listPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	    listPane.setTopComponent(staticScrollPane);
	    listPane.setBottomComponent(objectScrollPane);
	    listPane.setDividerSize(Config.splitPaneDividerWidth);
	    mainPanel.add(listPane, BorderLayout.CENTER);
	}
	else  
	   mainPanel.add(objectScrollPane);

	// add mouse listener to monitor for double clicks to inspect list 
	// objects. assumption is made that valueChanged will have selected
	// object on first click
	MouseListener mouseListener = new MouseAdapter() {
	    public void mouseClicked(MouseEvent e) {
		// monitor for double clicks
		if (e.getClickCount() == 2) {
		    doInspect();
		}
	    }
	};
	objFieldList.addMouseListener(mouseListener);
	
	if(staticFieldList != null)
	    staticFieldList.addMouseListener(mouseListener);


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
	if(isInspection || pkg == null) {
	    Utility.tileWindow(this, parent);
	}
	else
	    Utility.centreWindow(this, pkg.getFrame());

	setVisible(true);
	button.requestFocus();
    }
}
