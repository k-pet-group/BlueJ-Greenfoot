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
 * BlueJ starts here.
 * The Boot class, which is responsible for dealing with specialised
 * class loaders, constructs an object of this class to initiate the 
 * "real" BlueJ.
 *
 * @author  Michael Kolling
 * @version $Id: Main.java 2101 2003-07-08 14:00:26Z mik $
 */
public class Main
{
    private int FIRST_X_LOCATION = 20;
    private int FIRST_Y_LOCATION = 20;

    /**
     * Entry point to starting up the system. Initialise the
     * system and start the first package manager frame.
     */
    public Main()
    {
        Boot boot = Boot.getInstance();
        String [] args = boot.getArgs();
		File bluejLibDir = boot.getBluejLibDir();
        
        Config.initialise(bluejLibDir);

        MiscPrefPanel.register();
        ClassMgrPrefPanel.register();

        // You got to create it here since it is used by the Package manager frame
        ExtensionsManager extManager = ExtensionsManager.initialise();
        
        // It is here to have an extension to be ready whan a command line project is summoned
        extManager.loadExtensions();

        processArgs(args);
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
