package greenfoot.gui.inspector;

import greenfoot.util.GreenfootUtil;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import bluej.debugger.DebuggerClass;
import bluej.debugmgr.inspector.ClassInspector;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;

/**
 * Inspector that updates the values in the inspector with a fixed timer
 * interval.
 * 
 * @author Poul Henriksen
 * 
 */
public class GreenfootClassInspector extends ClassInspector
{

    public GreenfootClassInspector(DebuggerClass clss, InspectorManager inspectorManager, Package pkg, InvokerRecord ir,
            JFrame parent)
    {
        super(clss, inspectorManager, pkg, ir, parent);
        new InspectorUpdater(this);
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                GreenfootUtil.makeGreenfootTitle(GreenfootClassInspector.this);
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
