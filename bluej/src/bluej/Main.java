/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.UUID;

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
 * @version $Id: Main.java 6349 2009-05-22 14:49:01Z polle $
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
        File bluejLibDir = Boot.getBluejLibDir();

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
        
        // Send usage data back to bluej.org
        new Thread() {
            public void run()
            {
                updateStats();
            }
        }.start();
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
                    if(PkgMgrFrame.doOpen(new File(args[i]), null)) {
                        oneOpened = true;                        
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
                        if ((openProj = Project.openProject(exists, null)) != null) {
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
     * Send statistics of use back to bluej.org
     */
    private void updateStats() 
    {
        // System property name for honouring web proxy settings
        // See the JDK docs/technotes/guides/net/proxies.html
        final String useProxiesProperty = "java.net.useSystemProxies";
        String oldProxySetting = "false";   // Documented default value

        // Platform details, first the ones which vary between BlueJ/Greenfoot
        String uidPropName;
        String baseURL;
        String appVersion;
        if (Config.isGreenfoot()) {
            uidPropName = "greenfoot.uid";
            baseURL = "http://stats.greenfoot.org/updateGreenfoot.php";
            appVersion = Boot.GREENFOOT_VERSION;
        } else {
            uidPropName = "bluej.uid";
            baseURL = "http://stats.bluej.org/updateBlueJ.php";
            // baseURL = "http://localhost:8080/BlueJStats/index.php";
            appVersion = Boot.BLUEJ_VERSION;
        }

        // Then the common ones.
        String language = Config.language;
        String javaVersion = System.getProperty("java.version");
        String systemID = System.getProperty("os.name") +
                "/" + System.getProperty("os.arch") +
                "/" + System.getProperty("os.version");
        
        // User uid. Use the one already stored in the Property if it exists,
        // otherwise generate one and store it for next time.
        String uid = Config.getPropString(uidPropName, null);
        if (uid == null) {
            uid = UUID.randomUUID().toString();
            Config.putPropString(uidPropName, uid);
        } else if (uid.equalsIgnoreCase("private")) {
            // Allow opt-out
            return;
        }
        
        try {
            // Attempt to use local proxy settings to avoid any firewalls, just
            // for the rest of this method.
            oldProxySetting = System.getProperty(useProxiesProperty, oldProxySetting);
            System.setProperty(useProxiesProperty,"true");

            URL url = new URL(baseURL +
                "?uid=" + URLEncoder.encode(uid, "UTF-8") +
                "&osname=" + URLEncoder.encode(systemID, "UTF-8") +
                "&appversion=" + URLEncoder.encode(appVersion, "UTF-8") +
                "&javaversion=" + URLEncoder.encode(javaVersion, "UTF-8") +
                "&language=" + URLEncoder.encode(language, "UTF-8")
            );
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            int rc = conn.getResponseCode();
            conn.disconnect();

            if(rc != 200) Debug.reportError("Update stats failed, HTTP response code: " + rc);

        } catch (Exception ex) {
            Debug.reportError("Update stats failed", ex);
        } finally {
            System.setProperty(useProxiesProperty, oldProxySetting);
        }
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
