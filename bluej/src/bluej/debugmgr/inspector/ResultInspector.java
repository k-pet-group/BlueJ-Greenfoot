package bluej.debugmgr.inspector;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.DebuggerObject;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.JavaNames;
import bluej.utility.DialogManager;

/**
 * @author Poul Henriksen
 *
 */
public class ResultInspector extends Inspector implements InspectorListener {

    // === static variables ===

    protected final static String resultTitle =
        Config.getString("debugger.inspector.result.title");
   
    protected static Class[] inspectorClasses = new Class[10];
    protected static int inspectorCount = 0;
    protected static Set loadedProjects = new HashSet();

   /**
     * Return an ObjectInspector for an object. The inspector is visible.
     * This is the only way to get access to viewers - they cannot be
     * directly created.
     *
     * @param  obj         The object displayed by this viewer
     * @param  name        The name of this object or "null" if the name is unobtainable
     * @param  pkg         The package all this belongs to
     * @param  ir          the InvokerRecord explaining how we got this result/object
     *                     if null, the "get" button is permanently disabled
     * @param  parent      The parent frame of this frame
     * @return             The Viewer value
     */
    public static ResultInspector getInstance(DebuggerObject obj,
                                                String name, Package pkg,
                                                InvokerRecord ir, JFrame parent)
    {
        ResultInspector inspector = (ResultInspector) inspectors.get(obj);

        if (inspector == null) {
            inspector = new ResultInspector(obj, name, pkg, ir, parent);
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
    private ResultInspector(DebuggerObject obj, String name,
                            Package pkg, InvokerRecord ir, JFrame parent)
    {
        super(pkg, ir);
        String className = JavaNames.stripPrefix(obj.getClassName());
        
        setTitle(resultTitle);        
        setBorder(BlueJTheme.generalBorderWithStatusBar);
        
        // removed - mik - results dlgs don't have a header!?
        //String fullTitle = name + " : " + className;        
        //String underlinedNameLabel = "<html><u>"+fullTitle+ "</u></font>";
        //setHeader(new JLabel(underlinedNameLabel, JLabel.CENTER));
        this.obj = obj;
        this.objName = name;        
        
        makeFrame(true, true);
      	DialogManager.centreWindow(this, parent);
    }
    

    /**
     * True if this inspector is used to display a method call result.
     */
    protected Object[] getListData()
    {
        DebuggerObject realValue = obj.getFieldObject(0);
                
        String fieldString = JavaNames.stripPrefix(realValue.getClassName())
        + " " + obj.getInstanceFieldName(0)
        + " = " + DebuggerObject.OBJECT_REFERENCE;
        
        return new Object[] {fieldString};        
    }

    
    /**
     * An element in the field list was selected.
     */
    protected void listElementSelected(int slot)
    {
       
        if (obj.instanceFieldIsObject(slot)) {
            String newInspectedName;
            
            if (objName != null ) {
                newInspectedName = objName + "." + obj.getInstanceFieldName(slot);
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
    }
    
    /**
     * Remove this inspector.
     */
    protected void remove()
    {
        inspectors.remove(obj);
    }  

   
    /**
     *  return a String with the result.
     *
     * @return    The Result value
     */
    public String getResult()  {       
            return (String) obj.getInstanceFields(false).get(0);      
    }
 
    public void inspectEvent(InspectorEvent e)
    {
        getInstance(e.getDebuggerObject(), null, pkg, null, this);
    }
    
    protected int getPreferredRows() {
        return 2;
    }
    
    protected void initInspectors(JTabbedPane inspTabs) {         
    }
}
