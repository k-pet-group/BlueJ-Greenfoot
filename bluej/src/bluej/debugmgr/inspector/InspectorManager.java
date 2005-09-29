package bluej.debugmgr.inspector;

import javax.swing.JFrame;

import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerObject;
import bluej.debugmgr.ExpressionInformation;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;


/**
 * 
 * Interface for a manager that creates and holds references to inspectors
 * 
 * @author Poul Henriksen
 * @version $Id$
 */
public interface InspectorManager
{
    /**
     * 
     * Remove and dispose the inspector.
     * 
     * @param obj Object that the inspector inspects
     */
    public void removeInspector(DebuggerObject obj);
    
    /**
     * 
     * Remove and dispose the inspector.
     * 
     * @param cls Class that the inspector inspects
     */
    public void removeInspector(DebuggerClass cls);
    
    /**
     * Return an ObjectInspector for an object. The inspector is visible.
     *
     * @param obj
     *            The object displayed by this viewer
     * @param name
     *            The name of this object or "null" if the name is unobtainable
     * @param pkg
     *            The package all this belongs to
     * @param ir
     *            the InvokerRecord explaining how we got this result/object if
     *            null, the "get" button is permanently disabled
     * @param info
     *            The information about the the expression that gave this result
     * @param parent
     *            The parent frame of this frame
     * @return The Viewer value
     */
    public ObjectInspector getInspectorInstance(DebuggerObject obj,
            String name, Package pkg, InvokerRecord ir, JFrame parent);
    
    /**
     * Return a ClassInspector for a class. The inspector is visible.
     *
     * @param clss
     *            The class displayed by this viewer
     * @param name
     *            The name of this object or "null" if it is not on the object
     *            bench
     * @param pkg
     *            The package all this belongs to
     * @param getEnabled
     *            if false, the "get" button is permanently disabled
     * @param parent
     *            The parent frame of this frame
     * @return The Viewer value
     */
    public ClassInspector getClassInspectorInstance(DebuggerClass clss,
            Package pkg, JFrame parent);

    /**
     * Return an ObjectInspector for an object. The inspector is visible.
     * 
     * @param obj The object displayed by this viewer
     * @param name The name of this object or "null" if the name is unobtainable
     * @param pkg The package all this belongs to
     * @param ir the InvokerRecord explaining how we got this result/object if
     *            null, the "get" button is permanently disabled
     * @param info The information about the the expression that gave this
     *            result
     * @param parent The parent frame of this frame
     * @return The Viewer value
     */
    public ResultInspector getResultInspectorInstance(DebuggerObject obj,
        String name, Package pkg, InvokerRecord ir, ExpressionInformation info,
        JFrame parent);
    
    /**
     * Whether we are in testing mode. If true, the inspectors should show testing stuff.
     * 
     */
    public boolean inTestMode();
}
