package greenfoot.gui.inspector;

import greenfoot.util.GreenfootUtil;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

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
public class GreenfootObjectInspector extends ObjectInspector
{
    public GreenfootObjectInspector(DebuggerObject obj, InspectorManager inspectorManager, String name, Package pkg,
            InvokerRecord ir, JFrame parent)
    {
        super(obj, inspectorManager, name, pkg, ir, parent);
        new InspectorUpdater(this);
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                GreenfootUtil.makeGreenfootTitle(GreenfootObjectInspector.this);
            }
        });
    }

    /**
     * Whether the Get button should be enabled.
     * 
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
