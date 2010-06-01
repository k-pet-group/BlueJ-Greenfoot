package rmiextension;

import greenfoot.core.GreenfootMain;
import rmiextension.wrappers.WrapperPool;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerEvent;
import bluej.debugger.DebuggerListener;
import bluej.debugger.jdi.JdiDebugger;
import bluej.extensions.BPackage;
import bluej.extensions.BProject;
import bluej.extensions.ExtensionBridge;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.Debug;

/**
 * A class that listens for the debugger terminating the Greenfoot VM, and relaunches Greenfoot
 */
public class GreenfootRelauncher implements DebuggerListener
{  
    // ------------- DebuggerListener interface ------------
    
    private static GreenfootRelauncher singleton;

    public void debuggerEvent(DebuggerEvent e)
    {
        if (e.getNewState() == Debugger.NOTREADY && e.getOldState() == Debugger.IDLE) {
            final JdiDebugger debugger = (JdiDebugger)e.getSource();
            
            //It is important to have this code run at a later time.
            //If it runs from this thread, it tries to notify us and we get some
            //sort of RMI deadlock between the two VMs (I think).
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run()
                {
                    try { 
                        BProject bProject = bluej.pkgmgr.Project.getProject(debugger.getStartingDirectory()).getBProject();
                        WrapperPool.instance().remove(bProject);
                        BPackage bPackage = bProject.getPackages()[0];
                        WrapperPool.instance().remove(bPackage);
                        ProjectManager.instance().openGreenfoot(new Project(bPackage), GreenfootMain.VERSION_OK);
                    } catch (Exception ex) {
                        Debug.reportError("Exception while trying to relaunch Greenfoot", ex);
                    }
                }
            });
            
        }
    }
    
    public static void addDebuggerListener(BProject project)
    {
        if (singleton == null)
            singleton = new GreenfootRelauncher();
        try {
            ExtensionBridge.addDebuggerListener(project, singleton);
        } catch (ProjectNotOpenException ex) {
            Debug.reportError("Project not open when adding debugger listener in Greenfoot", ex);
        }
    }

}
