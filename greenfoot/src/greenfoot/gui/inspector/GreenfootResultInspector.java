package greenfoot.gui.inspector;

import greenfoot.util.GreenfootUtil;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import bluej.debugger.DebuggerObject;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.debugmgr.inspector.ResultInspector;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;

/**
 * Result inspector for Greenfoot.
 * 
 * @author Poul Henriksen
 */
public class GreenfootResultInspector extends ResultInspector
{

    public GreenfootResultInspector(DebuggerObject obj, InspectorManager inspectorManager, String name, Package pkg,
            InvokerRecord ir, ExpressionInformation info, JFrame parent)
    {
        super(obj, inspectorManager, name, pkg, ir, info, parent);
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                GreenfootUtil.makeGreenfootTitle(GreenfootResultInspector.this);
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
        return GreenfootInspector.isGetEnabled(selectedField);
    }

    /**
     * The "Get" button was pressed. Start dragging the selected object.
     */
    @Override
    protected void doGet()
    {
        GreenfootInspector.doGet(selectedField);
    }

}
