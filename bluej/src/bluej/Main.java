package bluej;

import bluej.debugger.Debugger;
import bluej.utility.Utility;
import bluej.utility.Debug;

import java.util.Vector;
import java.util.Enumeration;

import bluej.pkgmgr.MainPkgMgrFrame;

/**
 ** @version $Id: Main.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** This is the main entry point to BlueJ. Normal invocations start
 ** in this class's main method.
 **
 ** This class does two things: it starts up the system (by doing some
 ** initialisation and then calling the package manager Main class) and
 ** it maintains a set of open top level frames.
 **/
public class Main
{
    private static Vector frames = new Vector();

    /**
     * main - entry point to starting up the system. Initialise the
     * system and start the first package manager frame.
     */
    public static void main(String[] args) { 
	String home = null;
	if ((args.length >= 2) && "-home".equals(args[0])) {
	    home = args[1];
	    String[] newArgs = new String[args.length - 2];
	    System.arraycopy(args, 2, newArgs, 0, args.length - 2);
	    args = newArgs;
	} else {
	    String install = System.getProperty("install.root");
	    home = System.getProperty("bluej.home", install);
	}

	if(home == null) {
	    Utility.reportError("BlueJ should be run from a script that sets the \"bluej.home\" property");
	    System.exit(-1);
	}
		
	Config.initialise(home);
	//BlueJEvent.initialise();
	bluej.pkgmgr.Main.main(args);
    }
		
    /**
     * addFrame - add a frame to the set of currently open top level
     *  frames
     */
    public static void addFrame(BlueJFrame frame) {
	frames.addElement(frame);
    }
	
    /**
     * removeFrame - remove a frame from the set of currently open 
     *  top level frames
     */
    public static void removeFrame(BlueJFrame frame) {
	if(frames.size() == 1) {
	    // If only one frame, close should close existing package rather
	    // than remove frame 
	    bluej.pkgmgr.PkgMgrFrame pkgFrame = (bluej.pkgmgr.PkgMgrFrame)frame;
	    pkgFrame.removePackage();
	} else 
	    frames.removeElement(frame);
    }

    /**
     * frameCount - return the number of currently open top level frames
     */
    public static int frameCount()
    {
	return frames.size();
    }

    /**
     * displayMessage - display a short text message to the user. In the
     *  current implementation, this is done by showing the message in
     *  the status bars of all open package windows.
     */
    public static void displayMessage(String message)
    {
	for(Enumeration e = frames.elements(); e.hasMoreElements(); )
	    {
		BlueJFrame frame = (BlueJFrame)e.nextElement();
		frame.setStatus(message);
	    }
    }

    /**
     * 
     */
    public static void exit()
    {
	for(int i = frames.size() - 1; i >= 0; i--) {
	    BlueJFrame frame = (BlueJFrame)frames.elementAt(i);
	    frame.doClose();
	}
		
	Config.handleExit();
	Debugger.handleExit();
	System.exit(0);
    }

}
