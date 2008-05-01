package greenfoot.gui.inspector;

import javax.swing.JFrame;

import bluej.debugger.DebuggerObject;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.debugmgr.inspector.ObjectInspector;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;

/**
 * Inspector that updates the values in the inspector with a fixed timer
 * interval.
 * 
 * @author Poul Henriksen
 * 
 */
public class UpdatingObjectInspector extends ObjectInspector
{
    public UpdatingObjectInspector(DebuggerObject obj, InspectorManager inspectorManager, String name, Package pkg,
            InvokerRecord ir, JFrame parent)
    {
        super(obj, inspectorManager, name, pkg, ir, parent);
        new InspectorUpdater(this);
    }

    /**
     * Whether the Get button should be enabled.
     * @return True if the selected object is an actor
     */
    @Override
    protected boolean isGetEnabled()
    {
        return GreenfootInspector.isGetEnabled(selectedObject);
    }

    /**
     * The "Get" button was pressed. Start dragging the selected object.
     */
    @Override
    protected void doGet()
    {
        GreenfootInspector.doGet(selectedObject);
    }
}
