package bluej.debugger;

import bluej.Config;
import bluej.classmgr.ClassMgr;

/**
 * This class defines a separate thread to load the second ("remote")
 * virtual machine. This is done asynchronously, since it takes a loooong
 * time, and we would like to do it in the background.
 *
 * @author  Michael Kolling
 * @version $Id: MachineLoader.java 291 1999-11-30 06:24:36Z ajp $
 */
public class MachineLoader extends Thread
{
    /**
     * Create the machine loader thread.
     */
    public MachineLoader()
    {
        super("MachineLoader");
    }

    /**
     * run - this method executes when the thread is started. Load
     *  the virtual machine here. (The remote virtual machine is
     *  internally referred to as the "debugger".)
     */
    public void run()
    {
        Debugger.debugger.startDebugger();

        Debugger.debugger.setLibraries(ClassMgr.getClassMgr().getAllClassPath());
    }
}
