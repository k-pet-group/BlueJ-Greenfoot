package bluej.debugmgr.inspector;

import java.io.File;
import java.util.*;

import javax.swing.*;

import bluej.Config;
import bluej.debugger.DebuggerObject;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.*;

/**
 * A window that displays the fields in an object or a method return value.
 *
 * @author  Michael Kolling
 * @version $Id: ObjectInspector.java 2229 2003-10-28 02:09:36Z ajp $
 */
public class ObjectInspector extends Inspector
    implements InspectorListener
{
    // === static variables ===

    protected final static String inspectTitle =
        Config.getString("debugger.inspector.title");
    protected final static String resultTitle =
        Config.getString("debugger.inspector.result.title");
    protected final static String objListTitle =
        Config.getString("debugger.inspector.objListTitle");
    protected final static String objectNameLabel =
        Config.getString("debugger.inspector.objectNameLabel");

    protected static Class[] inspectorClasses = new Class[10];
    protected static int inspectorCount = 0;
    protected static Set loadedProjects = new HashSet();

   /**
     * Return an ObjectInspector for an object. The inspector is visible.
     * This is the only way to get access to viewers - they cannot be
     * directly created.
     *
     * @param  isResult    false is this is an inspection, true for result
     *                     displays
     * @param  obj         The object displayed by this viewer
     * @param  name        The name of this object or "null" if the name is unobtainable
     * @param  pkg         The package all this belongs to
     * @param  ir          the InvokerRecord explaining how we got this result/object
     *                     if null, the "get" button is permanently disabled
     * @param  parent      The parent frame of this frame
     * @return             The Viewer value
     */
    public static ObjectInspector getInstance(boolean isResult,
                                                DebuggerObject obj, String name,
                                                Package pkg, InvokerRecord ir,
                                                JFrame parent)
    {
        ObjectInspector inspector = (ObjectInspector) inspectors.get(obj);

        if (inspector == null) {
            inspector = new ObjectInspector(isResult, obj, name, pkg, ir, parent);
            inspectors.put(obj, inspector);
        }
        inspector.update();

        inspector.setVisible(true);
        inspector.bringToFront();

        return inspector;
    }


    // === instance variables ===

    protected DebuggerObject obj;
    protected String objName;           // a String for display that contains this objects
                                        // name on the object bench
                                            
    protected boolean isResult;         // true if displaying result, false if
                                        //  inspecting object
    protected boolean queryArrayElementSelected = false;

    protected TreeSet arraySet = null;  // array of Integers representing the array indexes from
                                        // a large array that have been selected for viewing
    protected List indexToSlotList = null;  // list which is built when viewing an array
                                            // that records the object slot corresponding to each
                                            // array index




    /**
     *  Constructor
     *  Note: private -- Objectviewers can only be created with the static
     *  "getViewer" method. 'pkg' may be null if 'ir' is null.
     *
     * @param  isResult    false is this is an inspection, true for result
     *                     displays
     * @param  obj         The object displayed by this viewer
     * @param  name        The name of this object or "null" if the name is unobtainable
     * @param  pkg         The package all this belongs to
     * @param  ir          the InvokerRecord explaining how we created this result/object
     *                     if null, the "get" button is permanently disabled
     * @param  parent      The parent frame of this frame
     */
    private ObjectInspector(boolean isResult, DebuggerObject obj,
                            String name, Package pkg, InvokerRecord ir,
                            JFrame parent)
    {
        super(pkg, ir);

        setTitle(isResult ? resultTitle : inspectTitle);

        this.isResult = isResult;
        this.obj = obj;
        this.objName = name;
                
        String fullTitle = objectNameLabel + " " + JavaNames.stripPrefix(obj.getClassName());
        
        if (name != null)
            fullTitle += " (" + name + ")";

        makeFrame(parent, isResult, true, fullTitle, true);
    }

    /**
     * Return the header title of the list in this inspector.
     */
    protected String getListTitle()
    {
        return objListTitle;
    }

    /**
     * True if this inspector is used to display a method call result.
     */
    protected boolean showingResult()
    {
        return isResult;
    }

    /**
     * True if this inspector is used to display a method call result.
     */
    protected Object[] getListData()
    {
        // if is an array (we potentially will compress the array if it is large)
        if (obj.isArray()) {
            return compressArrayList(
                    obj.getInstanceFields(!isResult)).toArray(new Object[0]);
        } else {
            return obj.getInstanceFields(!isResult).toArray(new Object[0]);
        }
    }

    /**
     * An element in the field list was selected.
     */
    protected void listElementSelected(int slot)
    {
        // add index to slot method for truncated arrays
        if (obj.isArray()) {
            slot = indexToSlot(slot);
            // if selection is the first field containing array length
            // we treat as special case and do nothing more
            if(slot == ARRAY_LENGTH_SLOT_VALUE) {
                setCurrentObj(null, null);
                setButtonsEnabled(false, false);
                return;
            }
        }

        queryArrayElementSelected = (slot == (ARRAY_QUERY_SLOT_VALUE));

        // for array compression..
        if (queryArrayElementSelected) {  // "..." in Array inspector
            setCurrentObj(null, null);  //  selected
            // check to see if elements are objects,
            // using the first item in the array
            if (obj.instanceFieldIsObject(0)) {
                setButtonsEnabled(true, false);
            } else {
                setButtonsEnabled(false, false);
            }
        }
        else if (obj.instanceFieldIsObject(slot))
        {
            String newInspectedName;
            
            if (objName != null && !obj.isArray()) {
                newInspectedName = objName + "." + obj.getInstanceFieldName(slot);
            }
            else if (objName != null && obj.isArray()) {
                newInspectedName = objName + obj.getInstanceFieldName(slot);
            }
            else {
                newInspectedName = obj.getInstanceFieldName(slot);
            }
            
            setCurrentObj(obj.getInstanceFieldObject(slot),
                          newInspectedName);

            if (obj.instanceFieldIsPublic(slot)) {
                setButtonsEnabled(true, true);
            } else {
                setButtonsEnabled(true, false);
            }
        }
        else {
            setCurrentObj(null, null);
            setButtonsEnabled(false, false);
        }
    }

    /**
     * Show the inspector for the class of an object.
     */
    protected void showClass()
    {
        ClassInspector insp =
   	       ClassInspector.getInstance(obj.getClassRef(), pkg, this);
    }

    /**
     * We are about to inspect an object - prepare.
     */
    protected void prepareInspection()
    {
        // if need to query array element
        if (queryArrayElementSelected) {
            selectArrayElement();
        }
    }
    
    /**
     * Remove this inspector.
     */
    protected void remove()
    {
        inspectors.remove(obj);
    }  

    /**
     * Shows a dialog to select array element for inspection
     */
    private void selectArrayElement()
    {
        String response = DialogManager.askString(this, "ask-index");

        if (response != null) {
            try {
                int slot = Integer.parseInt(response);

                // check if within bounds of array
                if (slot >= 0 && slot < obj.getInstanceFieldCount()) {
                    // if its an object set as current object
                    if (obj.instanceFieldIsObject(slot)) {
                        setCurrentObj(obj.getInstanceFieldObject(slot),
                                obj.getInstanceFieldName(slot));
                        setButtonsEnabled(true, false);
                    } else {
                        // it is not an object - a primitive, so lets
                        // just display it in the array list display
                        setButtonsEnabled(false, false);
                        //arraySet.add(new Integer(slot));
                        update();
                    }
                } else {  // not within array bounds
                    DialogManager.showError(this, "out-of-bounds");
                }
            }
            catch (NumberFormatException e) {
                // input could not be parsed, eg. non integer value
                setCurrentObj(null, null);
                DialogManager.showError(this, "cannot-access-element");
            }
        }
        else
        {
            // set current object to null to avoid re-inspection of
            // previously selected wildcard
            setCurrentObj(null, null);
        }
    }


    /**
     *  If this is the display of a method call result, return a String with
     *  the result.
     *
     * @return    The Result value
     */
    public String getResult()
    {
        if (isResult) {
            return (String) obj.getInstanceFields(false).get(0);
        } else {
            return "";
        }
    }

    private final static int VISIBLE_ARRAY_START = 40;  // show at least the first 40 elements
    private final static int VISIBLE_ARRAY_TAIL = 5;  // and the last five elements

    private final static int ARRAY_QUERY_SLOT_VALUE = -2;  // signal marker of the [...] slot in our
    private final static int ARRAY_LENGTH_SLOT_VALUE = -1;  // marker for having selected the slot containing array length
    
    /**
     * Compress a potentially large array into a more displayable
     * shortened form.
     *
     * Compresses an array field name list to a maximum of VISIBLE_ARRAY_START
     * which are guaranteed to be displayed at the start, then some [..] expansion
     * slots, followed by VISIBLE_ARRAY_TAIL elements from the end of the array.
     * When a selected element is chosen
     * indexToSlot allows the selection to be converted to the original
     * array element position.
     *
     * @param  fullArrayFieldList  the full field list for an array
     * @return                     the compressed array
     */
    private List compressArrayList(List fullArrayFieldList)
    {
        // mimic the public length field that arrays possess
        // according to the java spec...
        fullArrayFieldList.add(0, ("int length = " + fullArrayFieldList.size()));
        indexToSlotList = new LinkedList();
        indexToSlotList.add(0, new Integer(ARRAY_LENGTH_SLOT_VALUE));

        // the +1 here is due to the fact that if we do not have at least one more than
        // the sum of start elements and tail elements, then there is no point in displaying
        // the ... elements because there would be no elements for them to reveal
        if (fullArrayFieldList.size() > (VISIBLE_ARRAY_START + VISIBLE_ARRAY_TAIL + 2))
        {

            // the destination list
            List newArray = new ArrayList();
            for (int i = 0; i <= VISIBLE_ARRAY_START; i++)
            {
                // first 40 elements are displayed as per normal
                newArray.add(fullArrayFieldList.get(i));
                if(i < VISIBLE_ARRAY_START)
                    indexToSlotList.add(new Integer(i));
            }

            // now the first of our expansion slots
            newArray.add("[...]");
            indexToSlotList.add(new Integer(ARRAY_QUERY_SLOT_VALUE));

            for (int i = VISIBLE_ARRAY_TAIL; i > 0; i--) {
                // last 5 elements are displayed
                newArray.add(fullArrayFieldList.get(
                        fullArrayFieldList.size() - i));
                // slot is offset by one due to length field being included in 
                // fullArrayFieldList therefore we add 1 to compensate        
                indexToSlotList.add(new Integer(
                        fullArrayFieldList.size() - (i + 1)));
            }
            return newArray;
        }
        else
        {
            for (int i = 0; i < fullArrayFieldList.size(); i++) {
                indexToSlotList.add(new Integer(i));
            }
            return fullArrayFieldList;
        }
    }

    /**
     * Converts list index position to that of array element position in arrays.
     * Uses the List built in compressArrayList to do the mapping.
     *
     * @param   listIndexPosition   the position selected in the list
     * @return                      the translated index of field array element
     */
    private int indexToSlot(int listIndexPosition)
    {
        Integer slot = (Integer) indexToSlotList.get(listIndexPosition);

        return slot.intValue();
    }

    /**
     * Intialise additional inspector panels.
     */
    protected void initInspectors(JTabbedPane inspTabs)
    {
        if (inspectorCount == 0) {
            loadInspectors(Config.getSystemInspectorDir());
        }

        Project proj = null;
        // most common case, pkg can be null if inspection call came from debugger
        if(pkg != null)
            proj = pkg.getProject();
        // 1 project open...
        else if(Project.getOpenProjectCount() == 1)
            proj = Project.getProject();

        if (proj != null && !loadedProjects.contains(proj.getProjectDir()))
        {
            loadedProjects.add(proj.getProjectDir());
            loadInspectors(new File(proj.getProjectDir(),
                                    inspectorDirectoryName));
        }

        addInspectors(inspTabs);
    }

    private void loadInspectors(File inspectorDir)
    {
        ClassLoader loader = new InspectorClassLoader(inspectorDir);
        String[] inspName = inspectorDir.list();
        if (inspName != null) {
            for (int i=0; i < inspName.length; i++) {  // Add inspectors (if any)
                try {
                    if (inspName[i].endsWith(".class")) {
                        try {
                            Class theInspClass = loader.loadClass(inspName[i].substring(0, inspName[i].length() - 6));
                            InspectorPanel theInsp = ((InspectorPanel) theInspClass.newInstance());
                            // If control gets here, the class implements Inspector!
                            int inspIdx = inspectorCount;
                            inspectorCount++;
                            if (inspectorCount >= inspectorClasses.length) {
                                Class[] temp = new Class[inspectorClasses.length * 2];
                                System.arraycopy(inspectorClasses, 0, temp, 0, inspectorClasses.length);
                                inspectorClasses = temp;
                            }
                            inspectorClasses[inspIdx] = theInspClass;
                            //System.out.println(""+inspIdx+": "+theInspClass);
                        }
                        catch (ClassNotFoundException e) {
                        }
                        catch (InstantiationException e) {
                        }
                        catch (IllegalAccessException e) {
                        }
                        catch (ClassCastException e) {
                        }
                    }
                }
                catch (Exception catchalle) {
                    catchalle.printStackTrace();
                }
            }
        }
    }

    private void addInspectors(JTabbedPane inspTabs)
    {
        for (int i = 0; i < inspectorCount; i++) {  // Add inspectors (if any)
            try {
                InspectorPanel theInsp = ((InspectorPanel)inspectorClasses[i].newInstance());
                String[] ic = theInsp.getInspectedClassnames();

                for (int j = 0; j < ic.length; j++) {
                    if (obj.isAssignableTo(ic[j])) {
                        boolean initOK = theInsp.initialize(ObjectInspector.this.obj);
                        if (initOK) {  //Inspector makes final decision
                            theInsp.addInspectorListener(this);
                            inspTabs.add(theInsp.getInspectorTitle(), theInsp);
                        }
                        break;
                    }
                }
            }
            // we catch and report all exceptions.. if there is buggy
            // code in an inspector, it won't affect the rest of blueJ
            // (the main inspector panel will always come up)
            catch (Exception e) { 
                Debug.reportError("Error while trying to load inspector: " + e);
            }
        }
    }

    public void inspectEvent(InspectorEvent e)
    {
        getInstance(false, e.getDebuggerObject(), null, pkg, null, this);
    }
}
