package bluej;

import bluej.utility.Debug;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.debugger.Debugger;
import bluej.debugger.MachineLoader;
import bluej.classmgr.ClassMgrPrefPanel;
import bluej.prefmgr.MiscPrefPanel;
import bluej.prefmgr.PrefMgr;

/**
 * This is the main entry point to BlueJ. Normal invocations start
 * in this class's main method.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @version $Id: Main.java 518 2000-05-30 05:15:03Z ajp $
 */
public class Main
{
    public static int BLUEJ_VERSION_MAJOR = 1;
    public static int BLUEJ_VERSION_MINOR = 1;
    public static int BLUEJ_VERSION_RELEASE = 0;
    public static String BLUEJ_VERSION_SUFFIX = " alpha";


    public static int BLUEJ_VERSION_NUMBER = BLUEJ_VERSION_MAJOR * 1000 +
                                             BLUEJ_VERSION_MINOR * 100 +
                                             BLUEJ_VERSION_RELEASE;

    public static String BLUEJ_VERSION = "" + BLUEJ_VERSION_MAJOR + "." +
                                         BLUEJ_VERSION_MINOR + "." +
                                         BLUEJ_VERSION_RELEASE +
                                         BLUEJ_VERSION_SUFFIX;


    public static String BLUEJ_VERSION_TITLE = "BlueJ " + BLUEJ_VERSION;

    /**
     * Entry point to starting up the system. Initialise the
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
        PrefMgr.initialise();

        MiscPrefPanel.register();
        ClassMgrPrefPanel.register();

        // start the MachineLoader (a separate thread) to load the
        // remote virtual machine in the background

        MachineLoader machineLoader = new MachineLoader();
        // lower priority to improve GUI response time
        machineLoader.setPriority(Thread.currentThread().getPriority() - 1);
        machineLoader.start();

        processArgs(args);
    }

    /**
     * Start everything off. This is used to open the projects
     * specified on the command line when starting BlueJ.
     */
    public static void processArgs(String[] args)
    {
        boolean oneOpened = false;
        if(args.length > 0) {
            for(int i = 0; i < args.length; i++) {
                Project openProj;
                if((openProj = Project.openProject(args[i])) != null) {
                    oneOpened = true;

                    Package pkg = openProj.getPackage(
                                    openProj.getInitialPackageName());

                    PkgMgrFrame pmf = PkgMgrFrame.createFrame(pkg);

                    pmf.show();
                }
            }
        }

        if(args.length == 0 || !oneOpened) {
            // no arguments, so start an empty package manager window
            PkgMgrFrame frame = PkgMgrFrame.createFrame();
            frame.setVisible(true);
        }
    }

    /**
     * Exit BlueJ.
     *
     * The open frame count should be zero by this point as PkgMgrFrame
     * is responsible for cleaning itself up before getting here.
     */
    public static void exit()
    {
        if (PkgMgrFrame.frameCount() > 0)
            Debug.reportError("Frame count was not zero when exiting. Work may not have been saved");

        // save configuration properties
        Config.handleExit();
        // free resources for debugger
        Debugger.handleExit();
        // exit with success status
        System.exit(0);
    }
}
