package bluej;

import bluej.classmgr.ClassMgrPrefPanel;
import bluej.extmgr.ExtensionsManager;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.prefmgr.MiscPrefPanel;
import bluej.utility.Debug;
import java.io.File;

/**
 * This is the main entry point to BlueJ. Normal invocations start
 * in this class's main method.  
 *
 * @author  Michael Kolling
 * @version $Id: Main.java 1997 2003-05-30 12:10:30Z damiano $
 */
public class Main
{
    public static int BLUEJ_VERSION_MAJOR = 1;
    public static int BLUEJ_VERSION_MINOR = 3;
    public static int BLUEJ_VERSION_RELEASE = 0;
    public static String BLUEJ_VERSION_SUFFIX = " beta 2";

    public static int BLUEJ_VERSION_NUMBER = BLUEJ_VERSION_MAJOR * 1000 +
                                             BLUEJ_VERSION_MINOR * 100 +
                                             BLUEJ_VERSION_RELEASE;

    public static String BLUEJ_VERSION = BLUEJ_VERSION_MAJOR
                                         + "." + BLUEJ_VERSION_MINOR
                                         + "." + BLUEJ_VERSION_RELEASE
                                         + BLUEJ_VERSION_SUFFIX;


    public static String BLUEJ_VERSION_TITLE = "BlueJ " + BLUEJ_VERSION;

    public static String BLUEJ_JAR = "bluej.jar";


    private int FIRST_X_LOCATION = 20;
    private int FIRST_Y_LOCATION = 20;


    /**
     * Entry point to starting up the system. Initialise the
     * system and start the first package manager frame.
     */
    public Main()
    {
        Boot boot = Boot.get();
        String [] args = boot.getArgs();
        File bluejLibDir = boot.getBluejLibDir();
        
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

        SplashWindow splash = new SplashWindow(bluejLibDir);
        Config.initialise(bluejLibDir);

        MiscPrefPanel.register();
        ClassMgrPrefPanel.register();

        // You got to create it here since it is used by the Package manager frame
        ExtensionsManager extManager = ExtensionsManager.initialise();
        
        // It is here to have an extension to be ready whan a command line project is summoned
        extManager.loadExtensions();

        processArgs(args);
        splash.remove();
        }

    /**
     * Start everything off. This is used to open the projects
     * specified on the command line when starting BlueJ.
     * Any parameters starting with '-' are ignored for now.
     */
    private  void processArgs(String[] args)
    {
        boolean oneOpened = false;
        if(args.length > 0) {
            for(int i = 0; i < args.length; i++) {
                if (!args[i].startsWith ("-")) {
                    Project openProj;
                    if((openProj = Project.openProject(args[i])) != null) {
                        oneOpened = true;

                        Package pkg = openProj.getPackage(
                                        openProj.getInitialPackageName());

                        PkgMgrFrame pmf = PkgMgrFrame.createFrame(pkg);

                        pmf.setLocation(i*30 + FIRST_X_LOCATION,
                                         i*30 + FIRST_Y_LOCATION);
                        pmf.show();
                    }
                }
            }
        }

        if(args.length == 0 || !oneOpened) {
            // no arguments, so start an empty package manager window
            PkgMgrFrame frame = PkgMgrFrame.createFrame();
            frame.setLocation(FIRST_X_LOCATION, FIRST_Y_LOCATION);
            frame.show();
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
        // exit with success status
        System.exit(0);
    }

}
