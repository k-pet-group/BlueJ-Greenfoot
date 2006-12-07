package greenfoot.gui.inspector;

import javax.swing.JFrame;

import bluej.debugger.DebuggerClass;
import bluej.debugmgr.inspector.ClassInspector;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;

/**
 * Inspector that updates the values in the inspector with a fixed tiemr
 * interval.
 * 
 * @author Poul Henriksen
 * 
 */
public class UpdatingClassInspector extends ClassInspector
{

    public UpdatingClassInspector(DebuggerClass clss, InspectorManager inspectorManager, Package pkg, InvokerRecord ir,
            JFrame parent)
    {
        super(clss, inspectorManager, pkg, ir, parent);
        InspectorUpdater updater = new InspectorUpdater(this);
    }

}
