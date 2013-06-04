/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013  Michael Kolling and John Rosenberg 
 
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

import com.apple.eawt.Application;
import com.apple.eawt.AppEvent;
import com.apple.eawt.QuitResponse;

import bluej.collect.DataCollector;
import bluej.extensions.event.ApplicationEvent;
import bluej.extmgr.ExtensionsManager;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.actions.HelpAboutAction;
import bluej.pkgmgr.actions.PreferencesAction;
import bluej.pkgmgr.actions.QuitAction;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import java.util.List;

/**
 * BlueJ starts here. The Boot class, which is responsible for dealing with
 * specialised class loaders, constructs an object of this class to initiate the
 * "real" BlueJ.
 * 
 * @author Michael Kolling
 */
public class Main
{
    private static final int FIRST_X_LOCATION = 20;
    private static final int FIRST_Y_LOCATION = 20;
    
    /** 
     * Whether we've officially launched yet. While false "open file" requests only
     * set initialProject.
     */
    private static boolean launched = false;
    
    /** On MacOS X, this will be set to the project we should open (if any) */ 
    private static List<File> initialProjects;

    private static QuitResponse macEventResponse = null;  // used to respond to external quit events on MacOS

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
        
        // Note we must do this OFF the AWT dispatch thread. On MacOS X, if the
        // application was started by double-clicking a project file, an "open file"
        // event will be generated once we add a listener and will be delivered on
        // the dispatch thread. It will then be processed before the call to
        // processArgs() (just below) is called.
        if (Config.isMacOS()) {
            prepareMacOSApp();
        }

        if (Config.isGreenfoot()) {
            // Avoid having to put the Greenfoot classes on the system classpath:
            // (only an issue with JDK 7u21, 6u45, and later).
            System.setProperty("java.rmi.server.useCodebaseOnly", "false");
        }
        
        DataCollector.bluejOpened(getOperatingSystem(), getJavaVersion(), getBlueJVersion(), getInterfaceLanguage(), ExtensionsManager.getInstance().getLoadedExtensions(null));

        // process command line arguments, start BlueJ!
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run()
            {
                processArgs(args);
            }
        });
        
        // Send usage data back to bluej.org
        new Thread() {
            @Override
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
    private static void processArgs(String[] args)
    {
        launched = true;
        
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
        
        // Open a project if requested by the OS (Mac OS)
        if (initialProjects != null) {
            for (File initialProject : initialProjects) {
                oneOpened |= (PkgMgrFrame.doOpen(initialProject, null));
            }
        }

        // if we have orphaned packages, these are re-opened
        if (!oneOpened) {
            // check for orphans...
            boolean openOrphans = "true".equals(Config.getPropString("bluej.autoOpenLastProject"));
            if (openOrphans && hadOrphanPackages()) {
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
                // Handled by Greenfoot
            }
            else {
                openEmptyFrame();
            }
        }

        Boot.getInstance().disposeSplashWindow();
        ExtensionsManager.getInstance().delegateEvent(new ApplicationEvent(ApplicationEvent.APP_READY_EVENT));
    }

    /**
     * Prepare MacOS specific behaviour (About menu, Preferences menu, Quit
     * menu)
     */ 
    private static void prepareMacOSApp()
    {
        Application macApp = Application.getApplication();

        if (macApp != null) {

            macApp.setAboutHandler(new com.apple.eawt.AboutHandler() {
                @Override
                public void handleAbout(AppEvent.AboutEvent e)
                {
                    HelpAboutAction.getInstance().actionPerformed(PkgMgrFrame.getMostRecent());
                }
            });

            macApp.setPreferencesHandler(new com.apple.eawt.PreferencesHandler() {
                @Override
                public void handlePreferences(AppEvent.PreferencesEvent e)
                {
                    PreferencesAction.getInstance().actionPerformed(PkgMgrFrame.getMostRecent());
                }
            });

            macApp.setQuitHandler(new com.apple.eawt.QuitHandler() {
                @Override
                public void handleQuitRequestWith(AppEvent.QuitEvent e, QuitResponse response)
                {
                    macEventResponse = response;
                    QuitAction.getInstance().actionPerformed(PkgMgrFrame.getMostRecent());
                    // response.confirmQuit() does not need to be called, since System.exit(0) is called explcitly
                    // response.cancelQuit() is called to cancel (in wantToQuit())
                }
            });

            macApp.setOpenFileHandler(new com.apple.eawt.OpenFilesHandler() {
                @Override
                public void openFiles(AppEvent.OpenFilesEvent e)
                {
                    if (launched) {
                        List<File> files = e.getFiles();
                        for(File file : files) {
                            PkgMgrFrame.doOpen(file, null);
                        }
                    }
                    else {
                        initialProjects = e.getFiles();
                    }
                }
            });
        }
    }

    /**
     * Quit menu item was chosen.
     */
    public static void wantToQuit()
    {
        int answer = 0;
        if (Project.getOpenProjectCount() > 1)
            answer = DialogManager.askQuestion(PkgMgrFrame.getMostRecent(), "quit-all");
        if (answer == 0) {
            doQuit();
        }
        else {
            if(macEventResponse != null) {
                macEventResponse.cancelQuit();
                macEventResponse = null;
            }
        }
    }


    /**
     * perform the closing down and quitting of BlueJ. Note that the order of
     * the events is relevant - Extensions should be unloaded after package
     * close
     */
    public static void doQuit()
    {
        PkgMgrFrame[] pkgFrames = PkgMgrFrame.getAllFrames();

        // handle open packages so they are re-opened on startup
        handleOrphanPackages(pkgFrames);

        // We replicate some of the behaviour of doClose() here
        // rather than call it to avoid a nasty recursion
        for (int i = pkgFrames.length - 1; i >= 0; i--) {
            PkgMgrFrame aFrame = pkgFrames[i];
            aFrame.doSave();
            aFrame.closePackage();
            PkgMgrFrame.closeFrame(aFrame);
        }

        ExtensionsManager extMgr = ExtensionsManager.getInstance();
        extMgr.unloadExtensions();
        bluej.Main.exit();
    }

    /**
     * When bluej is exited with open packages we want it to open these the next
     * time that is started (this is default action, can be changed by setting
     *
     * @param openFrames
     */
    private static void handleOrphanPackages(PkgMgrFrame[] openFrames)
    {
        // if there was a previous list, delete it
        if (hadOrphanPackages())
            removeOrphanPackageList();
        // add an entry for each open package
        for (int i = 0; i < openFrames.length; i++) {
            PkgMgrFrame aFrame = openFrames[i];
            if (!aFrame.isEmptyFrame()) {
                Config.putPropString(Config.BLUEJ_OPENPACKAGE + (i + 1), aFrame.getPackage().getPath().toString());
            }
        }
    }

    /**
     * Checks if there were orphan packages on last exit by looking for
     * existence of a valid BlueJ project among the saved values for the
     * orphaned packages.
     *
     * @return whether a valid orphaned package exist.
     */
    public static boolean hadOrphanPackages()
    {
        String dir = "";
        // iterate through unknown number of orphans
        for (int i = 1; dir != null; i++) {
            dir = Config.getPropString(Config.BLUEJ_OPENPACKAGE + i, null);
            if (dir != null) {
                if(Project.isProject(dir)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * removes previously listed orphan packages from bluej properties
     */
    private static void removeOrphanPackageList()
    {
        String exists = "";
        for (int i = 1; exists != null; i++) {
            exists = Config.removeProperty(Config.BLUEJ_OPENPACKAGE + i);
        }
    }

    /**
     * Open a single empty bluej window.
     */
    private static void openEmptyFrame()
    {
        PkgMgrFrame frame = PkgMgrFrame.createFrame();
        frame.setLocation(FIRST_X_LOCATION, FIRST_Y_LOCATION);
        frame.setVisible(true);
    }
    
    /**
     * Send statistics of use back to bluej.org
     */
    private static void updateStats() 
    {
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
            appVersion = getBlueJVersion();
        }

        // Then the common ones.
        String language = getInterfaceLanguage();
        String javaVersion = getJavaVersion();
        String systemID = getOperatingSystem();
        
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
            Debug.reportError("Update stats failed: " + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    private static String getBlueJVersion()
    {
        return Boot.BLUEJ_VERSION;
    }

    private static String getOperatingSystem()
    {
        return System.getProperty("os.name") +
                "/" + System.getProperty("os.arch") +
                "/" + System.getProperty("os.version");
    }

    private static String getJavaVersion()
    {
        return System.getProperty("java.version");
    }

    private static String getInterfaceLanguage()
    {
        return Config.language;
    }

    /**
     * Exit BlueJ.
     * <p>
     * The open frame count should be zero by this point as PkgMgrFrame is
     * responsible for cleaning itself up before getting here.
     */
    private static void exit()
    {
        if (PkgMgrFrame.frameCount() > 0) {
            Debug.reportError("Frame count was not zero when exiting. Work may not have been saved");
        }

        DataCollector.bluejClosed();
        
        // save configuration properties
        Config.handleExit();
        // exit with success status
        System.exit(0);
    }
}
