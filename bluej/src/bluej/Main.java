/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019  Michael Kolling and John Rosenberg
 
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

import bluej.collect.DataCollector;
import bluej.extensions2.event.ApplicationEvent;
import bluej.extmgr.ExtensionWrapper;
import bluej.extmgr.ExtensionsManager;
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.JavaFXUtil;
import de.codecentric.centerdevice.MenuToolkit;
import de.codecentric.centerdevice.dialogs.about.AboutStageBuilder;
import javafx.application.Platform;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.swing.*;
import java.awt.*;
import java.awt.desktop.QuitResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * BlueJ starts here. The Boot class, which is responsible for dealing with
 * specialised class loaders, constructs an object of this class to initiate the
 * "real" BlueJ.
 * 
 * @author Michael Kolling
 */
public class Main
{
    private static final String MESSAGE_ROOT = "https://www.bluej.org/message/";
    private static final String TESTING_MESSAGE_ROOT = "https://www.bluej.org/message_test/";
    /** 
     * Whether we've officially launched yet. While false "open file" requests only
     * set initialProject.
     */
    private static boolean launched = false;
    
    /** On MacOS X, this will be set to the project we should open (if any) */ 
    private static List<File> initialProjects;

    private static QuitResponse macEventResponse = null;  // used to respond to external quit events on MacOS

    /**
     * Only used on Mac.  For some reason, executing the AppleJavaExtensions open
     * file handler (that is set from Boot.main) on initial load (e.g. because
     * the user double-clicked a project.greenfoot file) means that later on,
     * the context class loader is null on the JavaFX thread.
     * Honestly, I [NB] have no idea what the hell is going on there.
     * But the work-around is apparent: store the context class loader early on,
     * then if it is null later, restore it.  (There's no problem if the file
     * handler is not executed; the context class loader is the same early on as
     * later on the FX thread).
     */
    private static ClassLoader storedContextClassLoader;
    
    /** The mechanism to show the initial GUI */
    private static GuiHandler guiHandler = null;

    /**
     * Entry point to starting up the system. Initialise the system and start
     * the first package manager frame.
     */
    @OnThread(Tag.Any)
    public Main()
    {
        Boot boot = Boot.getInstance();
        final String[] args = Boot.cmdLineArgs;
        Properties commandLineProps = boot.getCommandLineProperties();
        File bluejLibDir = Boot.getBluejLibDir();

        Config.initialise(bluejLibDir, commandLineProps, boot.isGreenfoot());

        CompletableFuture<Stage> futureMainWindow = new CompletableFuture<>();
        // Must do this after Config initialisation:
        if (!Config.isGreenfoot())
            new Thread(() -> fetchAndShowCentralMsg(PrefMgr.getFlag(PrefMgr.NEWS_TESTING) ?  TESTING_MESSAGE_ROOT : MESSAGE_ROOT, futureMainWindow)).start();

        if (guiHandler == null) {
            guiHandler = new BlueJGuiHandler();
        }
        
        // Note we must do this OFF the AWT dispatch thread. On MacOS X, if the
        // application was started by double-clicking a project file, an "open file"
        // event will be generated once we add a listener and will be delivered on
        // the dispatch thread. It will then be processed before the call to
        // processArgs() (just below) is called.
        if (Config.isMacOS()) {
            prepareMacOSApp();
        }
        
        // process command line arguments, start BlueJ!
        Platform.runLater(() -> {
            List<ExtensionWrapper> loadedExtensions = ExtensionsManager.getInstance().getLoadedExtensions(null);
            DataCollector.bluejOpened(getOperatingSystem(), getJavaVersion(), getBlueJVersion(), getInterfaceLanguage(), loadedExtensions);
            Stage stage = processArgs(args);
            futureMainWindow.complete(stage);
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
     * 
     * @return A handle to the main window which was opened, or null if there was no window opened.
     */
    @OnThread(Tag.FXPlatform)
    private static Stage processArgs(String[] args)
    {
        launched = true;
        
        boolean oneOpened = false;

        // Open any projects specified on the command line
        if (args.length > 0) {
            for (String arg : args) {
                if (!arg.startsWith("-")) {
                    oneOpened |= guiHandler.tryOpen(new File(arg), true);
                }
            }
        }

        // Open a project if requested by the OS (Mac OS)
        if (initialProjects != null) {
            for (File initialProject : initialProjects) {
                oneOpened |= guiHandler.tryOpen(initialProject, true);
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
                        oneOpened |= guiHandler.tryOpen(new File(exists), false);
                    }
                }
            }
        }

        Stage window = guiHandler.initialOpenComplete(oneOpened);
        
        Boot.getInstance().disposeSplashWindow();
        ExtensionsManager.getInstance().delegateEvent(new ApplicationEvent(ApplicationEvent.EventType.APP_READY_EVENT));
        
        return window;
    }

    /**
     * Prepare MacOS specific behaviour (About menu, Preferences menu, Quit
     * menu)
     */
    private static void prepareMacOSApp()
    {
        storedContextClassLoader = Thread.currentThread().getContextClassLoader();
        initialProjects = Boot.getMacInitialProjects();

        // Even though BlueJ is JavaFX, the open-files handling still goes
        // through the java.awt.Desktop handling, once it is loaded
        // So we must do the handler set up for AWT/Swing even though we're running
        // in JavaFX.
        prepareMacOSMenuSwing();

        // We are using the NSMenuFX library to fix Mac Application menu only when it is a FX
        // menu. When the JDK APIs (i.e. handleAbout() etc) are fixed, both should go back to
        // the way as in prepareMacOSMenuSwing().
        prepareMacOSMenuFX();

        // This is not included in the above condition to avoid future bugs,
        // as this is not related to the application menu and will not be affected
        // when the above condition will.
        if (Config.isGreenfoot())
        {
            Debug.message("Disabling App Nap");
            try
            {
                Runtime.getRuntime().exec("defaults write org.greenfoot NSAppSleepDisabled -bool YES");
            }
            catch (IOException e)
            {
                Debug.reportError("Error disabling App Nap", e);
            }
        }
    }

    /**
     * Prepare Mac Application FX menu using the NSMenuFX library.
     * This is needed for due to a bug in the JDK APIs, not responding to
     * handleAbout() etc, when the menu is on FX.
     */
    private static void prepareMacOSMenuFX()
    {
        Platform.runLater(() -> {
            // Sets the JavaFX fxml Default Class Loader to avoid a fxml LoadException.
            // This used to be fired only in the release not while running from the repository.
            FXMLLoader.setDefaultClassLoader(AboutStageBuilder.class.getClassLoader());
            // Get the toolkit
            MenuToolkit menuToolkit = MenuToolkit.toolkit();
            // Create the default Application menu
            Menu defaultApplicationMenu = menuToolkit.createDefaultApplicationMenu(Config.getApplicationName());
            // Update the existing Application menu
            menuToolkit.setApplicationMenu(defaultApplicationMenu);

            // About
            defaultApplicationMenu.getItems().get(0).setOnAction(event -> guiHandler.handleAbout());

            // Preferences
            // It has been added without a separator due to a bug in the library used
            MenuItem preferences = new MenuItem(Config.getString("menu.tools.preferences"));
            if (Config.hasAcceleratorKey("menu.tools.preferences")) {
                preferences.setAccelerator(Config.getAcceleratorKeyFX("menu.tools.preferences"));
            }
            preferences.setOnAction(event -> guiHandler.handlePreferences());
            defaultApplicationMenu.getItems().add(1, preferences);

            // Quit
            defaultApplicationMenu.getItems().get(defaultApplicationMenu.getItems().size()-1).
                    setOnAction(event -> guiHandler.handleQuit());
        });
    }

    /**
     * Prepare Mac Application Swing menu using the java.awt.Desktop APIs.
     */
    @SuppressWarnings("threadchecker")
    private static void prepareMacOSMenuSwing()
    {
        Desktop.getDesktop().setAboutHandler(e -> {
            Platform.runLater(() -> guiHandler.handleAbout());
        });

        Desktop.getDesktop().setPreferencesHandler(e -> {
            Platform.runLater(() -> guiHandler.handlePreferences());
        });

        Desktop.getDesktop().setQuitHandler((e, response) -> {
            macEventResponse = response;
            Platform.runLater(() -> guiHandler.handleQuit());
            // response.confirmQuit() does not need to be called, since System.exit(0) is called explcitly
            // response.cancelQuit() is called to cancel (in wantToQuit())
        });

        Desktop.getDesktop().setOpenFileHandler(e ->  {
            if (launched)
            {
                List<File> files = e.getFiles();
                Platform.runLater(() ->
                {
                    for (File file : files)
                    {
                        guiHandler.tryOpen(file, true);
                    }
                });
            }
            else
                {
                initialProjects = e.getFiles();
            }
        });

        Boot.getInstance().setQuitHandler(() -> Platform.runLater(() -> guiHandler.handleQuit()));
    }

    /**
     * Handle the "quit" command: if any projects are open, prompt user to make sure, and quit if
     * confirmed.
     */
    @OnThread(Tag.FXPlatform)
    public static void wantToQuit()
    {
        int projectCount = Project.getOpenProjectCount();
        // We set a null owner here to make the dialog come to the front of all windows;
        // the user may have triggered the quit shortcut from any window, not just a PkgMgrFrame:
        int answer = projectCount <= 1 ? 0 : DialogManager.askQuestionFX(null, "quit-all");
        if (answer == 0)
        {
            doQuit();
        }
        else
        {
            SwingUtilities.invokeLater(() ->
            {
                if (macEventResponse != null)
                {
                    macEventResponse.cancelQuit();
                    macEventResponse = null;
                }
            });
        }
    }

    /**
     * Perform the closing down and quitting of BlueJ, including unloading
     * extensions.
     */
    @OnThread(Tag.FXPlatform)
    public static void doQuit() {
        guiHandler.doExitCleanup();
        ExtensionsManager extMgr = ExtensionsManager.getInstance();
        extMgr.unloadExtensions();
        bluej.Main.exit();
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

        String editorStats = "";
        int javaEditors = Config.getEditorCount(Config.SourceType.Java);
        int strideEditors = Config.getEditorCount(Config.SourceType.Stride);
        try
        {
            if (javaEditors != -1 && strideEditors != -1)
            {
                editorStats = "&javaeditors=" + URLEncoder.encode(Integer.toString(javaEditors), "UTF-8")
                    + "&strideeditors=" + URLEncoder.encode(Integer.toString(strideEditors), "UTF-8");
            }
        }
        catch (UnsupportedEncodingException ex)
        {
            Debug.reportError(ex);
        }

        Config.resetEditorsCount();
        
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
                "&language=" + URLEncoder.encode(language, "UTF-8") +
                editorStats // May be blank string, e.g. in BlueJ
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
        String osArch = System.getProperty("os.arch");
        // On Windows, the 32-bit JDK will see x86 for the processor, even if the processor is
        // 64-bit and the OS itself is 64-bit!  So we collect some extra info on Windows:
        if (Config.isWinOS())
        {
            // Taken from https://stackoverflow.com/a/5940770/412908
            String arch = System.getenv("PROCESSOR_ARCHITECTURE");
            String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");

            osArch += (arch != null && arch.endsWith("64"))
                    || (wow64Arch != null && wow64Arch.endsWith("64"))
                    ? "(64)" : "";
        }
        return System.getProperty("os.name") +
                "/" + osArch +
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
    @OnThread(Tag.FXPlatform)
    private static void exit()
    {
        DataCollector.bluejClosed();
        
        // save configuration properties
        Config.handleExit();
        // exit with success status

        // We wrap this in a Platform.runLater/Swing.invokeLater to make sure it
        // runs after any pending FX actions or Swing actions:
        JavaFXUtil.runAfterCurrent(() -> SwingUtilities.invokeLater(() -> System.exit(0)));
    }

    // See comment on the field.
    public static ClassLoader getStoredContextClassLoader()
    {
        return storedContextClassLoader;
    }
    
    /**
     * Set the inital GUI, created after the initial project is opened.
     * @param initialGUI  A consume which displays the GUI for the initial project.
     */
    public static void setGuiHandler(GuiHandler initialGUI)
    {
        Main.guiHandler = initialGUI;
    }


    /**
     * Fetch and show a message from bluej.org, by looking at the central index then fetching
     * the current message (if any, and if unseen).
     * 
     * @param withStage A future which will complete with a parent window (or null if none).
     *                  The message should be shown as the modal child of this window.
     */
    private static void fetchAndShowCentralMsg(String messageRoot, CompletableFuture<Stage> withStage)
    {
        try
        {
            // latest.txt should have two dates, one on each line.  Top one is
            // start date of the message (and serves as its identifier), bottom one
            // is the expiry date of the message.
            Scanner scanner = new Scanner(new URL(messageRoot + "latest.txt").openStream(), "UTF-8").useDelimiter("\n");

            LocalDate startDate = LocalDate.parse(scanner.nextLine());
            LocalDate endDate = LocalDate.parse(scanner.nextLine());

            LocalDate lastSeen = null;
            try
            {
                lastSeen = LocalDate.parse(Config.getPropString(Config.MESSAGE_LATEST_SEEN));
            }
            catch (Exception e)
            {
                // Can't read saved value; just leave lastSeen as null
            }

            boolean seenMessage = lastSeen != null && (startDate.isBefore(lastSeen) || startDate.isEqual(lastSeen));
            boolean expired = LocalDate.now().isAfter(endDate);

            if (!seenMessage && !expired)
            {
                Platform.runLater(() -> {
                    // Display the message in a web view component:
                    WebView webView = new WebView();
                    
                    // In 5 seconds time, cancel the loading attempt - probably a connection or server issue:
                    FXPlatformRunnable preventTimeout = JavaFXUtil.runAfter(Duration.seconds(5), () -> {
                        webView.getEngine().getLoadWorker().cancel();
                    });

                    AtomicBoolean shownWindow = new AtomicBoolean(false);

                    // Only bother showing the window once (if!) the page load has succeeded:
                    JavaFXUtil.addChangeListener(webView.getEngine().getLoadWorker().stateProperty(), state -> {
                        if (state == State.SUCCEEDED && !shownWindow.get())
                        {
                            // Loaded!  Show it to the user.
                            shownWindow.set(true);
                            
                            // Make sure we don't cancel now we've been successful: 
                            JavaFXUtil.runNowOrLater(() -> {
                                preventTimeout.run();

                                makeLinksOpenExternally(webView.getEngine().getDocument());
                            });
                                
                            withStage.handle((parent, error) -> {
                                if (parent != null)
                                {
                                    JavaFXUtil.runNowOrLater(() -> showMessageWindow(startDate, webView, parent));
                                }
                                return null;
                            });
                        }
                    });
                    
                    // Now set off the loading attempt:
                    webView.getEngine().load(messageRoot + startDate.toString() + ".html");
                });
            }
        }
        catch (MalformedURLException e)
        {
            // Shouldn't happen:
            Debug.reportError(e);
        }
        catch (IOException e)
        {
            // Might not have any Internet connection.
            // Silently abandon attempt to show message
        }
    }

    /**
     * Overrides the click behaviour of all &lt;a&gt; tags in the document
     * so that they open in an external browser window.
     * 
     * @param document The document in which to override the links
     */
    private static void makeLinksOpenExternally(Document document)
    {
        // Adapted from https://stackoverflow.com/questions/15555510/javafx-stop-opening-url-in-webview-open-in-browser-instead/18536564#18536564
        NodeList nodeList = document.getElementsByTagName("a");
        for (int i = 0; i < nodeList.getLength(); i++)
        {
            Node node= nodeList.item(i);
            EventTarget eventTarget = (EventTarget) node;
            eventTarget.addEventListener("click", new org.w3c.dom.events.EventListener()
            {
                @Override
                public void handleEvent(org.w3c.dom.events.Event evt)
                {
                    EventTarget target = evt.getCurrentTarget();
                    HTMLAnchorElement anchorElement = (HTMLAnchorElement) target;
                    String href = anchorElement.getHref();
                    SwingUtilities.invokeLater(() -> Utility.openWebBrowser(href));
                    evt.preventDefault();
                }
            }, false);
        }
    }

    /**
     * Shows the message fetched from the server in a new modal window.  When the window
     * is closed, record that the user has seen the message.
     * 
     * @param startDate The start date (identifier) of the message being shown.
     * @param webView The WebView component in which the message has been loaded
     * @param parent The parent window.  Must be non-null
     */
    @OnThread(Tag.FXPlatform)
    private static void showMessageWindow(LocalDate startDate, WebView webView, Stage parent)
    {
        Stage window = new Stage();
        window.initModality(Modality.WINDOW_MODAL);
        window.initOwner(parent);
        window.setTitle(Config.getString("bluej.central.msg.title"));
        Button button = new Button(Config.getString("okay"));
        button.setDefaultButton(true);
        button.setOnAction(e -> {
            window.hide();
        });
        window.setOnHidden(e -> {
            // Record that we have seen the new message:
            Config.recordLatestSeen(startDate);
        });

        BorderPane.setAlignment(button, Pos.CENTER);
        BorderPane.setMargin(button, new Insets(15));
        BorderPane.setMargin(webView, new Insets(10));
        webView.setPrefWidth(650);
        webView.setPrefHeight(400);
        window.setScene(new Scene(new BorderPane(webView, null, null, button, null)));
        window.show();
        window.toFront();
    }
}
