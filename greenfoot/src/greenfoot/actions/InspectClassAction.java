package greenfoot.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JFrame;

import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.debugger.DebuggerClass;
import bluej.debugmgr.inspector.InspectorManager;

/**
 * Action to inspect a class.
 * 
 * @author Poul Henriksen
 */
public class InspectClassAction extends AbstractAction
{
    private InspectorManager inspectorManager;
    private DebuggerClass cls;
    private Package pkg;
    private JFrame frame;
    
    public InspectClassAction(DebuggerClass cls, Package pkg, InspectorManager inspMan, JFrame parent) 
    {
        super(Config.getString("inspect.class"));
        inspectorManager = inspMan;
        this.pkg = pkg;
        this.cls = cls;
        this.frame = parent;
    }
    
    public void actionPerformed(ActionEvent e)
    {
        inspectorManager.getClassInspectorInstance(cls, pkg, frame);
    }
}
