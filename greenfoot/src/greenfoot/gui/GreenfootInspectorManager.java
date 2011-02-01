package greenfoot.gui;

import greenfoot.gui.inspector.GreenfootClassInspector;
import greenfoot.gui.inspector.GreenfootObjectInspector;
import greenfoot.gui.inspector.GreenfootResultInspector;

import java.awt.EventQueue;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JFrame;

import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerObject;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.inspector.ClassInspector;
import bluej.debugmgr.inspector.Inspector;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.debugmgr.inspector.ObjectInspector;
import bluej.debugmgr.inspector.ResultInspector;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.ClassInspectInvokerRecord;
import bluej.testmgr.record.InvokerRecord;

/**
 * An inspector manager for Greenfoot projects.
 * 
 * @author Davin McCall
 */
public class GreenfootInspectorManager implements InspectorManager
{
    /** This holds all object inspectors for a world. */
    private Map<DebuggerObject, Inspector> objectInspectors = new HashMap<DebuggerObject, Inspector> ();
    /** This holds all class inspectors for a world. */
    private Map<String, Inspector> classInspectors = new HashMap<String, Inspector> ();

    @Override
    public void removeInspector(DebuggerObject obj)
    {
        objectInspectors.remove(obj);
    }

    @Override
    public void removeInspector(DebuggerClass cls)
    {
        classInspectors.remove(cls.getName());
    }

    @Override
    public ObjectInspector getInspectorInstance(DebuggerObject obj,
            String name, Package pkg, InvokerRecord ir, JFrame parent)
    {
        ObjectInspector inspector = (ObjectInspector) objectInspectors.get(obj);
        
        if (inspector == null) {
            inspector = new GreenfootObjectInspector(obj, this, name, pkg, ir, parent);
            objectInspectors.put(obj, inspector);
        }
        
        final ObjectInspector insp = inspector;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                insp.update();
                insp.updateLayout();
                insp.setVisible(true);
                insp.bringToFront();
            }
        });
        
        return inspector;
    }

    @Override
    public ClassInspector getClassInspectorInstance(DebuggerClass clss,
            Package pkg, JFrame parent)
    {
        ClassInspector inspector = (ClassInspector) classInspectors.get(clss.getName());

        if (inspector == null) {
            ClassInspectInvokerRecord ir = new ClassInspectInvokerRecord(clss.getName());
            inspector = new GreenfootClassInspector(clss, this, pkg, ir, parent);
            classInspectors.put(clss.getName(), inspector);
        }

        final Inspector insp = inspector;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                insp.update();
                insp.updateLayout();
                insp.setVisible(true);
                insp.bringToFront();
            }
        });

        return inspector;
    }

    @Override
    public ResultInspector getResultInspectorInstance(DebuggerObject obj,
            String name, Package pkg, InvokerRecord ir,
            ExpressionInformation info, JFrame parent)
    {
        ResultInspector inspector = (ResultInspector) objectInspectors.get(obj);
        
        if (inspector == null) {
            inspector = new GreenfootResultInspector(obj, this, name, pkg, ir, info, parent);
            objectInspectors.put(obj, inspector);
        }

        final ResultInspector insp = inspector;
        insp.update();
        insp.updateLayout();
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                insp.setVisible(true);
                insp.bringToFront();
            }
        });

        return inspector;
    }

    @Override
    public boolean inTestMode()
    {
        // Greenfoot does not support testing:
        return false;
    }

    /**
     * Removes all inspector instances for this project.
     * This is used when VM is reset or the project is recompiled.
     */
    public void removeAllInspectors()
    {
        for (Iterator<Inspector> it = objectInspectors.values().iterator(); it.hasNext();) {
            Inspector inspector = it.next();
            inspector.setVisible(false);
            inspector.dispose();
        }
        objectInspectors.clear();
        
        for (Iterator<Inspector> it = classInspectors.values().iterator(); it.hasNext();) {
            Inspector inspector = it.next();
            inspector.setVisible(false);
            inspector.dispose();
        }
        classInspectors.clear();
    }
}
