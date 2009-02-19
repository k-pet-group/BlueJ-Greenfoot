/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej;

import java.awt.EventQueue;
import java.io.File;
import java.net.URL;
import java.util.Properties;

import bluej.extensions.event.ApplicationEvent;
import bluej.extmgr.ExtensionsManager;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;

/**
 * BlueJ starts here. The Boot class, which is responsible for dealing with
 * specialised class loaders, constructs an object of this class to initiate the
 * "real" BlueJ.
 * 
 * @author Michael Kolling
 * @version $Id: Main.java 6163 2009-02-19 18:09:55Z polle $
 */
public class Main
{
    private int FIRST_X_LOCATION = 20;
    private int FIRST_Y_LOCATION = 20;

    /**
     * Entry point to starting up the system. Initialise the system and start
     * the first package manager frame.
     */
    public Main()
    {
        Boot boot = Boot.getInstance();
        final String[] args = boot.getArgs();
        Properties commandLineProps = boot.getCommandLineProperties();
        File bluejLibDir = boot.getBluejLibDir();

        Config.initialise(bluejLibDir, commandLineProps, boot.isGreenfoot());

        // workaround java's broken UNC path handling on Windows
        if (Config.getPropBoolean("bluej.windows.customUNCHandler")) {
            String osname = System.getProperty("os.name", "");
            if (osname.startsWith("Windows")) {
                URL.setURLStreamHandlerFactory(new BluejURLStreamHandlerFactory());
            }
        }

        // process command line arguments, start BlueJ!
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                processArgs(args);
            }
        });
    }

    /**
     * Start everything off. This is used to open the projects specified on the
     * command line when starting BlueJ. Any parameters starting with '-' are
     * ignored for now.
     */
    private void processArgs(String[] args)
    {
        boolean oneOpened = false;

        // Open any projects specified on the command line
        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (!args[i].startsWith("-")) {
                    Project openProj;
                    if ((openProj = Project.openProject(args[i])) != null) {
                        oneOpened = true;

                        Package pkg = openProj.getPackage(openProj.getInitialPackageName());

                        PkgMgrFrame pmf = PkgMgrFrame.createFrame(pkg);

                        pmf.setLocation(i * 30 + FIRST_X_LOCATION, i * 30 + FIRST_Y_LOCATION);
                        pmf.setVisible(true);
                    }
                }
            }
        }

        // if we have orphaned packages, these are re-opened
        if (!oneOpened) {
            // check for orphans...
            boolean openOrphans = "true".equals(Config.getPropString("bluej.autoOpenLastProject"));
            if (openOrphans && PkgMgrFrame.hadOrphanPackages()) {
                String exists = "";
                // iterate through unknown number of orphans
                for (int i = 1; exists != null; i++) {
                    exists = Config.getPropString(Config.BLUEJ_OPENPACKAGE + i, null);
                    if (exists != null) {
                        Project openProj;
                        // checking all is well (project exists)
                        if ((openProj = Project.openProject(exists)) != null) {
                            Package pkg = openProj.getPackage(openProj.getInitialPackageName());
                            PkgMgrFrame.createFrame(pkg);
                            oneOpened = true;
                        }
                    }
                }
            }
        }

        // Make sure at least one frame exists
        if (!oneOpened) {
            if (Config.isGreenfoot()) {
                // TODO: open default project
            }
            else {
                openEmptyFrame();
            }
        }

        Boot.getInstance().disposeSplashWindow();
        ExtensionsManager.getInstance().delegateEvent(new ApplicationEvent(ApplicationEvent.APP_READY_EVENT));
    }

    /**
     * Open a single empty bluej window.
     * 
     */
    private void openEmptyFrame()
    {
        PkgMgrFrame frame = PkgMgrFrame.createFrame();
        frame.setLocation(FIRST_X_LOCATION, FIRST_Y_LOCATION);
        frame.setVisible(true);
    }

    /**
     * Exit BlueJ.
     * 
     * The open frame count should be zero by this point as PkgMgrFrame is
     * responsible for cleaning itself up before getting here.
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
