package bluej;

import bluej.debugger.Debugger;
import bluej.utility.Utility;
import bluej.utility.Debug;

import bluej.pkgmgr.PkgMgrFrame;

/**
 ** @version $Id: Main.java 53 1999-04-29 23:43:58Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** This is the main entry point to BlueJ. Normal invocations start
 ** in this class's main method.
 **
 **/
public class Main
{

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
     * Close all frames and exit.
     */
    public static void exit()
    {
	PkgMgrFrame.handleExit();
	Config.handleExit();
	Debugger.handleExit();
	System.exit(0);
    }

}
