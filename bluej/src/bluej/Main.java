package bluej;

import java.io.File;
import java.util.Properties;

import bluej.classmgr.ClassMgrPrefPanel;
import bluej.editor.moe.EditorPrefPanel;
import bluej.extmgr.ExtensionsManager;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.prefmgr.MiscPrefPanel;
import bluej.utility.Debug;

/**
 * BlueJ starts here.
 * The Boot class, which is responsible for dealing with specialised
 * class loaders, constructs an object of this class to initiate the 
 * "real" BlueJ.
 *
 * @author  Michael Kolling
 * @version $Id: Main.java 2374 2003-11-19 03:44:33Z ajp $
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

        Config.initialise(bluejLibDir, processCommandLineProperties(args));

        EditorPrefPanel.register();
        MiscPrefPanel.register();
        ClassMgrPrefPanel.register();

        // You got to create it here since it is used by the Package manager frame
        ExtensionsManager extManager = ExtensionsManager.initialise();
        
        // It is here to have an extension to be ready whan a command line project is summoned
        extManager.loadExtensions();

        processArgs(args);
    }

    private Properties processCommandLineProperties(String[] args)
    {
        Properties props = new Properties();

        for(int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("-D"))
                continue;
            
            String definition = args[i].substring(2);
            int definitionEquals = definition.indexOf('=');
            
            if (definitionEquals < 0)
                continue;
            
            String propName = definition.substring(0, definitionEquals); 
            String propValue = definition.substring(definitionEquals+1);
            
            if (!propName.equals("") && !propValue.equals(""))
                props.put(propName, propValue);
        }

        return props;
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

                        Package pkg = openProj.getOrCreatePackageTree(
                                        openProj.getInitialPackageName());

                        PkgMgrFrame pmf = PkgMgrFrame.createFrame(pkg);

                        pmf.setLocation(i*30 + FIRST_X_LOCATION,
                                         i*30 + FIRST_Y_LOCATION);
                        pmf.show();
                    }
                }
            }
        }

        // if we have orphaned packages, these are re-opened
        if(args.length == 0 || !oneOpened) {
            // check for orphans...
            boolean openOrphans = "true".equals(Config.getPropString("bluej.autoOpenLastProject"));
            if(openOrphans && PkgMgrFrame.hadOrphanPackages()) {
                String exists = "";
                // iterate through unknown number of orphans
                for(int i = 1; exists != null; i++) {
                    exists = Config.getPropString(Config.BLUEJ_OPENPACKAGE + i, null);
                    if(exists != null){
                        Project openProj;
                        // checking all is well (project exists)
                        if((openProj = Project.openProject(exists)) != null) {
                            Package pkg = openProj.getOrCreatePackageTree(openProj.getInitialPackageName());
                            PkgMgrFrame pmf = PkgMgrFrame.createFrame(pkg);
                            oneOpened = true;
                        }
                
                    }
                    // this should only happen if there was one old project to auto-open
                    // and it cannot be found
                    else if (!oneOpened) {
                        openEmptyFrame();
                    }
                }
            }
        
            else {
                // no arguments, so start an empty package manager window
                openEmptyFrame();
            }
        }
    }
    
    /**
     * Open a single empty bluej window.
     *
     */
    private void openEmptyFrame()
    {
        PkgMgrFrame frame = PkgMgrFrame.createFrame();
        frame.setLocation(FIRST_X_LOCATION, FIRST_Y_LOCATION);
        frame.show(); 
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
