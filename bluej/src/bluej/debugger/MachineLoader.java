package bluej.debugger;

import bluej.classmgr.ClassMgr;

/**
 * This class defines a separate thread to load the second ("remote")
 * virtual machine. This is done asynchronously, since it takes a loooong
 * time, and we would like to do it in the background.
 *
 * @author  Michael Kolling
 * @version $Id: MachineLoader.java 1954 2003-05-15 06:06:01Z ajp $
 */
public class MachineLoader extends Thread
{
	private Debugger d;
	
    /**
     * Create the machine loader thread.
     */
    public MachineLoader(Debugger d)
    {
        super("MachineLoader");
        this.d = d;
    }

    /**
     * run - this method executes when the thread is started. Load
     *  the virtual machine here. (The remote virtual machine is
     *  internally referred to as the "debugger".)
     */
    public void run()
    {
        d.startDebugger();
        d.setLibraries(ClassMgr.getClassMgr().getAllClassPath().toString());
    }
}
