package bluej;

import bluej.debugger.Debugger;
import bluej.utility.Debug;

import bluej.pkgmgr.PkgMgrFrame;

/**
 ** @version $Id: Main.java 86 1999-05-18 02:49:53Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** This is the main entry point to BlueJ. Normal invocations start
 ** in this class's main method.
 **
 **/
public class Main
{

    public static int BLUEJ_VERSION_MAJOR = 0;
    public static int BLUEJ_VERSION_MINOR = 9;
    public static int BLUEJ_VERSION_RELEASE = 3;

    public static int BLUEJ_VERSION_NUMBER = BLUEJ_VERSION_MAJOR * 1000 +
					BLUEJ_VERSION_MINOR * 100 +
					BLUEJ_VERSION_RELEASE;

    public static String BLUEJ_VERSION = "" + BLUEJ_VERSION_MAJOR + "."
					 + BLUEJ_VERSION_MINOR + "."
					 + BLUEJ_VERSION_RELEASE;

    /**
     * main - entry point to starting up the system. Initialise the
     * system and start the first package manager frame.
     */
    public static void main(String[] args) { 

	if((args.length >= 1) && "-version".equals(args[0])) {
	    System.out.println("BlueJ version " + BLUEJ_VERSION
			       + " (Java version "
			       + System.getProperty("java.version") 
			       + ")");
	    System.out.println("--");

	    System.out.println("virtual machine: "
			       + System.getProperty("java.vm.name")
			       + " "
			       + System.getProperty("java.vm.version")
			       + " ("
			       + System.getProperty("java.vm.vendor")
			       + ")");

	    System.out.println("running on: "
			       + System.getProperty("os.name")
			       + " "
			       + System.getProperty("os.version")
			       + " ("
			       + System.getProperty("os.arch")
			       + ")");
	    System.exit(-1);
	}

	String home = null;
	if((args.length >= 2) && "-home".equals(args[0])) {
	    home = args[1];
	    String[] newArgs = new String[args.length - 2];
	    System.arraycopy(args, 2, newArgs, 0, args.length - 2);
	    args = newArgs;
	} else {
	    String install = System.getProperty("install.root");
	    home = System.getProperty("bluej.home", install);
	}

	if(home == null) {
	    Debug.reportError("BlueJ should be run from a script that sets the \"bluej.home\" property");
	    System.exit(-1);
	}
		
	Config.initialise(home);
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
