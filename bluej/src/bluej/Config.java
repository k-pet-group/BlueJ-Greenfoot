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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.*;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polygon;
import javafx.stage.Screen;
import javafx.stage.Window;

import javax.swing.ImageIcon;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.JavaFXUtil;
import javafx.stage.WindowEvent;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.utility.Debug;
import bluej.utility.Utility;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;

/**
 * Class to handle application configuration for BlueJ.
 * The configuration information is spread over several files: <BR>
 * <BR>
 *  &lt;bluej_home>/lib/bluej.defs <BR>
 *  &lt;user_home>/.bluej/bluej.properties <BR>
 *  command line arguments in form -D&lt;prop>=&lt;val> <BR>
 * <BR>
 * bluej.defs - contains system definitions which are not user specific<BR>
 * bluej.properties - contains user specific settings.
 *    Settings here override settings in bluej.defs <BR>
 * command line arguments - contains per-launch specific settings.
 *    Settings here override settings in bluej.properties <BR>
 * <BR>
 * There is also a set of language specific labels 
 * in a directory named after the language
 *  &lt;bluej_home>/lib/&lt;language>/labels
 *
 * @author Michael Cahill
 * @author Michael Kolling
 * @author Andrew Patterson
 */
public final class Config
{
    public static final String nl = System.getProperty("line.separator");
    public static final String osname = System.getProperty("os.name", "");
    public static final String DEFAULT_LANGUAGE = "english";
    public static final String BLUEJ_OPENPACKAGE = "bluej.openPackage";
    public static final String bluejDebugLogName = "bluej-debuglog.txt";
    public static final String greenfootDebugLogName = "greenfoot-debuglog.txt";
    // Bit ugly having it here, but it's needed by MiscPrefPanel (which may just be in BlueJ)
    // and by Greenfoot
    public static final KeyCodeCombination GREENFOOT_SET_PLAYER_NAME_SHORTCUT = 
        new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
    /** name of the icons file for the VM on Mac */
    private static final String BLUEJ_DEBUG_DOCK_ICON = "vm.icns";
    private static final String GREENFOOT_DEBUG_DOCK_ICON = "greenfootvm.icns";
    /** name of the VM in the dock on Mac */
    private static final String BLUEJ_DEBUG_DOCK_NAME = "BlueJ Virtual Machine";
    private static final String GREENFOOT_DEBUG_DOCK_NAME = "Greenfoot";
    public static Properties moeSystemProps;  // moe (editor) properties
    public static Properties moeUserProps;    // moe (editor) properties

    // We only want at most one BooleanProperty per property-name,
    // so we have to keep track of the ones we've made:
    private static final Map<String, BooleanProperty> booleanProperties = new HashMap<>();

    // bluej configuration properties hierarchy
    // (command overrides user which overrides system)
    public static String language;      // message language (english, ...)
    public static Rectangle screenBounds; // maximum dimensions of screen
    public static String debugLogName = bluejDebugLogName;
    public static List<String> fontOptions = new ArrayList<>();
    // a border for components with keyboard focus
    private static Border focusBorder;
    // a border for components without keyboard focus
    private static Border normalBorder;
    private static Properties systemProps;      // bluej.defs
    private static Properties userProps;        // <user home>/bluej.properties
    private static Properties greenfootProps;   // greenfoot.defs
    private static Properties commandProps;     // specified on the command line
    private static Properties initialCommandLineProps; // The properties
                                                       // specified on the
                                                       // command line
    private static Properties langProps;        // international labels
    private static Properties langVarProps;     // language label variables (APPNAME)
    private static File bluejLibDir;
    private static File userPrefDir;
    private static File templateDir;
    /** The greenfoot subdirectory of the "lib"-directory*/ 
    private static File greenfootLibDir;
    private static boolean initialised = false;
    private static boolean isGreenfoot = false;
    private static List<String> debugVMArgs = new ArrayList<>();
    /** whether this is the debug vm or not. */
    private static boolean isDebugVm = true; // Default to true, will be corrected on main VM
    public static final String EDITOR_COUNT_JAVA = "session.numeditors.java";
    public static final String EDITOR_COUNT_STRIDE = "session.numeditors.stride";
    public static final String MESSAGE_LATEST_SEEN = "bluej.latest.msg";
    private static long MAX_DEBUG_LOG_SIZE = 1048576;

    /**
     * Initialisation of BlueJ configuration. Must be called at startup.
     * This method finds and opens the configuration files.
     * 
     * This is only called from the main/server VM
     * 
     * See also initializeVMside().
     */
    public static void initialise(File bluejLibDir, Properties tempCommandLineProps,
                                  boolean bootingGreenfoot)
    {
        initialise(bluejLibDir, tempCommandLineProps, bootingGreenfoot, true);
        // Load any debug vm args (only needed on server VM):
        initDebugVMArgs();
    }
    
    /**
     * Initialisation of BlueJ configuration. Must be called at startup.
     * This method finds and opens the configuration files.<p>
     * 
     * See also initializeVMside().
     */
    private static void initialise(File bluejLibDir, Properties tempCommandLineProps,
                                  boolean bootingGreenfoot, boolean createUserhome)
    {
        if(initialised)
            return;

        initialised = true;
        
        initialCommandLineProps = tempCommandLineProps;
        
        isGreenfoot = bootingGreenfoot;

        screenBounds = calculateScreenBounds();

        // construct paths for the configuration directories
        Config.bluejLibDir = bluejLibDir;
        Config.greenfootLibDir = new File(bluejLibDir, "greenfoot");
        
        // setup our heirarchy of property objects if it is not done yet:
        if(systemProps == null)
        {
            isDebugVm = false;
            
            // top level is the system properties loaded from bluej.defs
            systemProps = loadDefs("bluej.defs", System.getProperties());
            
            // next level is the greenfoot propeties (if we are running greenfoot)
            // and then the user propeties (not loaded yet)
            if(isGreenfoot()) {
                greenfootProps = loadDefs("greenfoot.defs", systemProps);
                userProps = new Properties(greenfootProps);
            }
            else {
                userProps = new Properties(systemProps);
            }
        }
        
        // then there is the command line properties
        commandProps = new Properties(userProps);
        
        // copy in all our command line properties (done first
        // incase the bluej.userHome property is one specified)
        commandProps.putAll(tempCommandLineProps);
        commandProps.setProperty("bluej.libdir", bluejLibDir.getAbsolutePath());
        
        if (createUserhome) {

            // get user home directory
            initUserHome();

            // add user specific definitions (bluej.properties or greenfoot.properties)
            loadProperties(getApplicationName().toLowerCase(), userProps);

            // set a new name for the log file if we are running in greenfoot mode
            if(isGreenfoot) {
                debugLogName = greenfootDebugLogName;
            }

            checkDebug(userPrefDir);
        }
        
        initLanguage();

        moeSystemProps = loadDefs("moe.defs", System.getProperties());
        moeUserProps = new Properties(moeSystemProps);
        loadProperties("moe", moeUserProps);  // add user specific editor definitions

        // Whether or not to use the screen menu bar on a Mac. This only affects Swing, so should
        // not have any effect on BlueJ/Greenfoot itself in current versions, but just in case
        // extensions display Swing frames we'll still set it:
        String macOSscreenMenuBar = Config.getPropString("bluej.macos.screenmenubar", "true");
        System.setProperty("apple.laf.useScreenMenuBar", macOSscreenMenuBar);      

        Config.setVMLocale();
        
        // Create a property containing the BlueJ version string
        // put it in command_props so it won't be saved to a file
        commandProps.setProperty("bluej.version", Boot.BLUEJ_VERSION);
    }

    /**
     * Determine the configured language, or detect the language from the locale.
     * Fall back to the DEFAULT_LANGUAGE if language cannot be determined.
     * Load language-specific labels for the determined language.
     */
    private static void initLanguage()
    {
        language = commandProps.getProperty("bluej.language", null);
        
        // If no language is set, try to auto-detect from locale:
        if (language == null) {
            language = DEFAULT_LANGUAGE;
            try {
                String iso3lang = Locale.getDefault().getISO3Language();

                for (int i = 1; ; i++) {
                    String langString = Config.getPropString("bluej.language" + i, null);
                    if (langString == null) {
                        break;
                    }

                    // The format of a language string is:
                    //    internal-name:display-name:iso3cc
                    // The iso3cc (ISO country code) is optional.

                    int colonIndex = langString.indexOf(':');
                    if (colonIndex == -1) {
                        continue; // don't understand this one
                    }

                    int secondColon = langString.indexOf(':', colonIndex + 1);
                    if (secondColon == -1) {
                        continue;
                    }

                    if (langString.substring(secondColon + 1).equals(iso3lang)) {
                        language = langString.substring(0, colonIndex);
                        Config.putPropString("bluej.language", language);
                        break;
                    }
                }

                Debug.log("Detected language \"" + language + "\" based on iso639-2 code \"" + iso3lang + "\"");
            }
            catch (MissingResourceException mre) {
                Debug.log("Using default language \"" + language + "\"");
            }
        }
        
        langProps = loadLanguageLabels(language);
    }
    
    /**
     * Initialise the user home (try and create directories if necessary).
     * <p>
     * We try the bluej.userHome property, or default to the system user.home
     * property if that is not set. If the result is not writable we try
     * bluej.userHome1, bluej.userHome2, etc.
     */
    private static void initUserHome()
    {
        File userHome;
        String homeDir = getPropString("bluej.userHome", "$user.home");
        userHome = new File(homeDir);

        String prefDirName = getBlueJPrefDirName();
        
        // get user specific bluej property directory (in user home)
        userPrefDir = new File(userHome, prefDirName);

        int nameCounter = 1;
        do {
            if (! userPrefDir.isDirectory()) {
                if (userPrefDir.mkdirs()) {
                    // successfully created the preferences directory
                    break;
                }
            }
            else if (userPrefDir.canWrite()) {
                break;
            }
            
            nameCounter++;
            String propertyName = "bluej.userHome" + nameCounter;
            homeDir = getPropString(propertyName, null);
            if (homeDir == null) {
                break;
            }
            userHome = new File(homeDir);
            userPrefDir = new File(userHome, prefDirName);
        }
        while (true);
        
        if (homeDir == null) {
            // Now we're in trouble... just user user.home, and hope it's writable.
            homeDir = System.getProperty("user.home");
            userHome = new File(homeDir);
            userPrefDir = new File(userHome, prefDirName);
        }
    }

    /**
     * Alternative to "initialise", to be used in the debugee-VM by
     * applications which require it (ie. greenfoot).
     */    
    public static void initializeVMside(File bluejLibDir,
            File userConfigDir,
            BlueJPropStringSource propSource)
    {
        isDebugVm = true;
        Config.userPrefDir = userConfigDir;

        // Set up the properties so that they use the properties from the
        // BlueJVM
        systemProps = new Properties() {
            @Override
            public String getProperty(String key)
            {
                return propSource.getBlueJPropertyString(key, null);
            }

            @Override
            public String getProperty(String key, String def)
            {
                return propSource.getBlueJPropertyString(key, def);
            }
        };
        userProps = new Properties(systemProps) {
            @Override
            public synchronized Object setProperty(String key, String val)
            {
                // Debug VM should not be setting properties:
                Debug.printCallStack("Internal error: setting user property on debug VM");
                return null;
            }
            
            @Override
            public String getProperty(String key)
            {
                return propSource.getBlueJPropertyString(key, null);
            }

            @Override
            public String getProperty(String key, String def)
            {
                return propSource.getBlueJPropertyString(key, def);
            }
        };
        initialise(bluejLibDir, new Properties(), true, false);
    }
    
    /**
     * Get the properties that were given on the command line and used 
     * to initialise bluej.Config.
     */
    public static Properties getInitialCommandLineProperties()
    {
        return initialCommandLineProps;
    }    
    
    /**
     * Initializer for use in Greenfoot's standalone scenario viewer if you 
     * export a scenario as an app or applet.
     */
    public static void initializeStandalone(BlueJPropStringSource propSource)
    {
        if(initialised)
            return;
    
        initialised = true;
        Config.isGreenfoot = true;
        
        langProps =  new Properties() {
            @Override
            public String getProperty(String key)
            {
                return propSource.getLabel(key);
            }
            
            @Override
            public String getProperty(String key, String def)
            {
                return propSource.getLabel(key);
            }
        };
        commandProps = langProps;
    }
    
    public static boolean isInitialised() 
    {
        return initialised;
    }
    
    /**
     * Get the name of icons file for the debug VM (Mac).
     */
    public static String getVMIconsName()
    {
        if(isGreenfoot()) {
            return GREENFOOT_DEBUG_DOCK_ICON;
        }
        return BLUEJ_DEBUG_DOCK_ICON;
    }
    
    /**
     * Get the name of the debug VM to appear in the dock (Mac).
     */
    public static String getVMDockName()
    {
        if (isGreenfoot()) {
            return GREENFOOT_DEBUG_DOCK_NAME;
        }
        return BLUEJ_DEBUG_DOCK_NAME;
    }
    
    /**
     * True if this is the debugVM or false if not.
     */
    public static boolean isDebugVM() 
    {
        return isDebugVm;
    }
    
    /**
     * Tell us whether we are running on MacOS
     */
    public static boolean isMacOS()
    {
        return osname.startsWith("Mac");
    }
    
    private static boolean osVersionNumberAtLeast(int... target)
    {
        return versionAtLeast(System.getProperty("os.version"), target);
    }

    private static boolean javaVersionNumberAtLeast(int... target)
    {
        return versionAtLeast(System.getProperty("java.specification.version"), target);
    }

    private static boolean versionAtLeast(String version, int[] target)
    {
        String[] versionChunks = version.split("\\.");
        for (int i = 0; i < target.length; i++)
        {
            if (versionChunks.length <= i)
                return false; // Play safe
            if (target[i] < Integer.parseInt(versionChunks[i]))
                return true;
            if (target[i] > Integer.parseInt(versionChunks[i]))
                return false;
        }
        // Must be equal
        return true;
    }

    /**
     * Tell us whether we are running on MacOS 10.5 (Leopard) or later
     */
    public static boolean isMacOSLeopard()
    {
        return osname.startsWith("Mac") &&
                osVersionNumberAtLeast(10, 5);
    }
    
    /**
     * Tell use whether we are running on MacOS 10.6 (Snow Leopard) or later
     */
    public static boolean isMacOSSnowLeopard()
    {
        return osname.startsWith("Mac") &&
            osVersionNumberAtLeast(10, 6);
    }
    
    /**
     * Tell us whether we are running on MS Windows
     */
    public static boolean isWinOS()
    {
        return osname.startsWith("Windows");
    }
    
    /**
     * True if OS is Windows Vista or newer.
     */
    public static boolean isModernWinOS()
    {
        return isWinOS()
                && osVersionNumberAtLeast(6, 0);
    }
    
    /**
     * Tell us whether we are running on Linux
     */
    public static boolean isLinux()
    {
        return osname.startsWith("Linux");
    }
    
    /**
     * Tell us whether we are running on Solaris
     */
    public static boolean isSolaris()
    {
        return osname.startsWith("Solaris");
    }

    /**
     * Tell us whether we are running on a Java VM that supports Java 7 features.
     */
    public static boolean isJava17()
    {
        return javaVersionNumberAtLeast(1, 7);
    }
    
    /**
     * Tell us whether we are running OpenJDK.
     */
    public static boolean isOpenJDK()
    {
        return System.getProperty("java.runtime.name").startsWith("OpenJDK");
    }
    
    /**
     * Whether we need to make all dialogs resizable, due to bug:
     * https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8198761
     */
    public static boolean makeDialogsResizable()
    {
        // The bug only affects Linux:
        return Config.isLinux();
    }
    
    /**
     * Return the name of a directory within the user's home directory
     * that should be used for storing BlueJ user preferences.
     * 
     * @return The path of the preference directory relative to the user's home
     */
    private static String getBlueJPrefDirName()
    {
        String programName = "bluej";
        if(isGreenfoot) {
            programName = "greenfoot";
        }
        if(isMacOS()) {
            return "Library/Preferences/org." + programName;
        }
        else if(isWinOS()) {
            return programName;
        }
        else {
            return "." + programName;
        }
    }
    
    /**
     * Get the name of this application.
     */
    public static String getApplicationName()
    {
        if(isGreenfoot) {
            return "Greenfoot";
        }
        return "BlueJ";
    }
    
    /**
     * Get the screen size information
     */
    private static Rectangle calculateScreenBounds()
    {
        // Don't throw an exception if we're testing in headless mode:
        if (GraphicsEnvironment.isHeadless())
        {
            return new Rectangle(0, 0, 1280, 1024);
        }
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        return new Rectangle(d);
    }
    
    /**
     * Check whether we want to see debug information. If not, redirect it to
     * a file.
     */
    private static void checkDebug(File userdir)
    {
        if (!isDebugVM()) {
            if (!"true".equals(commandProps.getProperty("bluej.debug"))) {
                File debugLogFile = new File(userdir, debugLogName);
                // simple diversion of output stream to a log file
                try {
                    boolean append = debugLogFile.exists() && debugLogFile.length() < MAX_DEBUG_LOG_SIZE;
                    
                    PrintStream outStream = new PrintStream(new FileOutputStream(debugLogFile, append));
                    System.setOut(outStream);
                    System.setErr(outStream);
                    Debug.setDebugStream(new OutputStreamWriter(outStream));
                    
                    if (append)
                    {
                        Debug.message("====\n\n====");
                    }

                    Debug.message(getApplicationName() + " run started: " + new Date());
                    if(isGreenfoot()) {
                        Debug.message("Greenfoot version: " + Boot.GREENFOOT_VERSION);
                    }
                    else {
                        Debug.message("BlueJ version " + Boot.BLUEJ_VERSION);
                    }
                    Debug.message("Java version " + System.getProperty("java.version"));
                    Debug.message("JavaFX version " + System.getProperty("javafx.runtime.version"));
                    Debug.message("Virtual machine: " +
                            System.getProperty("java.vm.name") + " " +
                            System.getProperty("java.vm.version") +
                            " (" + System.getProperty("java.vm.vendor") + ")");
                    Debug.message("Running on: " + System.getProperty("os.name") +
                            " " + System.getProperty("os.version") +
                            " (" + System.getProperty("os.arch") + ")");
                    Debug.message("Java Home: " + System.getProperty("java.home"));            
                    Debug.message("----");    
                    return;
                }
                catch (IOException e) {
                    Debug.reportError("Warning: Unable to create debug log file.");
                }
            }
        }
        
        // We get here if:
        // - we are on the debug VM (and in Greenfoot) or
        // - bluej.debug=true or
        // - creating the debug log failed
        Debug.setDebugStream(new OutputStreamWriter(System.out));
    }

    /**
     * Called on system exit. Do whatever there is to do before exiting.
     */
    public static void handleExit()
    {
        saveAppProperties();
        saveProperties("moe", "properties.heading.moe", moeUserProps);    
    }

    private static void saveAppProperties()
    {
        final String name = getApplicationName().toLowerCase();
        saveProperties(name, "properties.heading." + name, userProps);
    }

    /**
     * Increases the count of editors opened by one for the given source type,
     * and saves the user properties file.
     */
    public static void recordEditorOpen(SourceType sourceType)
    {
        switch (sourceType)
        {
            case Java:
            {
                int javaEditors = getPropInteger(EDITOR_COUNT_JAVA, 0, userProps);
                javaEditors += 1;
                userProps.setProperty(EDITOR_COUNT_JAVA, Integer.toString(javaEditors));
                saveAppProperties();
            }
            break;
            case Stride:
            {
                int strideEditors = getPropInteger(EDITOR_COUNT_STRIDE, 0, userProps);
                strideEditors += 1;
                userProps.setProperty(EDITOR_COUNT_STRIDE, Integer.toString(strideEditors));
                saveAppProperties();
            }
            break;
            default: break;
        }
    }

    /**
     * Gets the editors count as stored in the properties file.  You should usually
     * use this to get the count from the previous session, then call resetEditorsCount.
     * 
     * @return The number of editors that have been opened for that source type
     *         since the last call to resetEditorsCount.  Returns -1 if the property
     *         is not found
     */
    public static int getEditorCount(SourceType sourceType)
    {
        switch (sourceType)
        {
            case Java: return getPropInteger(EDITOR_COUNT_JAVA, -1, userProps);
            case Stride: return getPropInteger(EDITOR_COUNT_STRIDE, -1, userProps);
            default: return -1;
        }
    }

    /**
     * Resets the editor count (as processed by getEditorCount/recordEditorOpen)
     * in the properties file.  Also saves the properties file.
     */
    public static void resetEditorsCount()
    {
        userProps.setProperty(EDITOR_COUNT_JAVA, "0");
        userProps.setProperty(EDITOR_COUNT_STRIDE, "0");
        saveAppProperties();
    }

    /**
     * Records the date of the latest seen message from the server and saves the bluej.properties file.
     * @param latestSeen The id (start date) of the latest message the user has seen.
     */
    public static void recordLatestSeen(LocalDate latestSeen)
    {
        userProps.setProperty(MESSAGE_LATEST_SEEN, latestSeen.toString());
        saveAppProperties();
    }

    /**
     * Load a BlueJ definition file. This creates a new properties object.
     * The new properties object can be returned directly, or an empty
     * properties object can be returned that has the named definitions as
     * defaults.
     *
     * @param filename  the properties file
     */
    private static Properties loadDefs(String filename, Properties parentProperties)
    {
        File propsFile = new File(bluejLibDir, filename);
        Properties defs = new Properties(parentProperties);

        try {
            defs.load(new FileInputStream(propsFile));
        }
        catch(IOException e) {
            Debug.reportError("Unable to load definitions file: " + propsFile);
        }

        return defs;
    }

    /**
     * Load the label property file for the currently defined language.
     * Install the default language (English) as the default properties
     * as a fallback.
     */
    private static Properties loadLanguageLabels(String language)
    {
        // add the defaults (English)
        Properties labels = loadDefs(DEFAULT_LANGUAGE + File.separator + "labels", System.getProperties());

        // Load frame labels
        String frameLabels = DEFAULT_LANGUAGE + File.separator + "frame-labels";
        File frameLabelFile = new File(bluejLibDir, frameLabels);
        try{
            labels.load(new FileInputStream(frameLabelFile));
        }
        catch(Exception e){
            Debug.reportError("Unable to load greenfoot labels file: " + frameLabelFile);
        }

        // if greenfoot, add specific additional labels
        if(isGreenfoot())
        {
            // load greenfoot labels to default lanbels
            String greenfootLabels = DEFAULT_LANGUAGE + File.separator + "greenfoot/greenfoot-labels";
            File greenfootLabelFile = new File(bluejLibDir, greenfootLabels);
            try{
                labels.load(new FileInputStream(greenfootLabelFile));
            }
            catch(IOException e){
                Debug.reportError("Unable to load greenfoot labels file: " + greenfootLabelFile);
            }
        }
        // add localised labels if necessary...
        if(!DEFAULT_LANGUAGE.equals(language)) {
            String languageFileName = language + File.separator + "labels";
            File languageFile = new File(bluejLibDir, languageFileName);
            try{
                labels.load(new FileInputStream(languageFile));
            }
            catch(Exception e){
                Debug.reportError("Unable to load definitions file: " + languageFile);
            }

            // Load frame labels
            String languageFrameLabels = language + File.separator + "frame-labels";
            File languageFrameLabelFile = new File(bluejLibDir, languageFrameLabels);
            try{
                labels.load(new FileInputStream(languageFrameLabelFile));
            }
            catch(Exception e){
                Debug.reportError("Unable to load frame labels file: " + languageFrameLabelFile);
            }

            if(isGreenfoot()) {
                File greenfootLabels = new File(bluejLibDir, language + File.separator + "greenfoot/greenfoot-labels");
                try{
                    labels.load(new FileInputStream(greenfootLabels));
                }
                catch(Exception e){
                    Debug.reportError("Unable to load greenfoot labels file: " + greenfootLabels);
                }
            }
        }
        return labels;
    }

    /**
     * Load local BlueJ properties. The properties definitions override
     * the defaults found in the definitions file.
     */
    private static void loadProperties(String filename, Properties props)
    {
        File propsFile = new File(userPrefDir, filename + ".properties");

        try {
            props.load(new FileInputStream(propsFile));
        }
        catch (IOException e) {
            // ignore exception - this will happen when file doesn't yet exist
        }
        catch (Exception e) {
            // We have seen IllegalArgumentException when the file has been corrupted and contains
            // an invalid unicode escape sequence (backslash-u-XXXX). For safety we'll catch all
            // exceptions and log an error rather than failing to start.
            Debug.reportError("Exception while loading properties", e);
        }
    }

    /**
     * Save user specific (local) BlueJ properties.
     */
    private static void saveProperties(String filename, String comment, Properties props)
    {
        File propsFile = new File(userPrefDir, filename + ".properties");

        try {
            props.store(new FileOutputStream(propsFile), getString(comment));
        }
        catch(IOException e) {
            Debug.reportError("could not save properties file " + propsFile);
        }
    }
    
    /**
     * Find and return the moe help definitions
     */
    public static Properties getFlowHelp()
    {
        return loadDefs(language + File.separator + "moe.help", System.getProperties());
    }

    /**
     * Get a string list from the language dependent definitions file
     * (eg. "english/labels").  If you pass "mydialog.error" then we look
     * for "mydialog.error1", then "mydialog.error2".  Every consecutive
     * found String is added to the list until we can't find one, at which
     * point we stop.
     */
    public static List<String> getStringList(String stem)
    {
        List<String> r = new ArrayList<>();
        for (int i = 1; ;i++)
        {
            String s = getString(stem + Integer.toString(i), null);
            if (s != null)
                r.add(s);
            else
                return r;
        }
    }

    /**
     * Get a string from the language dependent definitions file
     * (eg. "english/labels").
     */
    public static String getString(String strname)
    {
        return getString(strname, strname);
    }
    
    /**
     * Get a string from the language dependent definitions file
     * (eg. "english/labels"). If not found, return default.
     */
    public static String getString(String strname, String def)
    {
        return getString(strname, def, null);
    }

    /**
     * Get a string from the language dependent definitions file
     * (eg. "english/labels"), replacing local variables.
     * If not found, return default.
     */
    public static String getString(String strname, String def, Properties variables)
    {
        if (langVarProps == null) {
            langVarProps = new Properties();
            langVarProps.put("APPNAME", getApplicationName());
        }

        int index;
        // langProps can be null during testing:
        String str = langProps == null ? def : langProps.getProperty(strname, def);
        if (str != null)
        {
            // remove all underscores
            while ((index = str.indexOf('_')) != -1)
            {
                str = str.substring(0, index) + str.substring(index + 1);
            }
            if ((index = str.indexOf('@')) != -1)
            {
                //remove everything from @
                str = str.substring(0, index);
            }

            if (variables == null) {
                variables = langVarProps;
            }
            else {
                variables.putAll(langVarProps);
            }

            str = PropParser.parsePropString(str, variables);
        }

        return str;
    }
    
    /**
     * Get the mnemonic key for a particular label by looking for an underscore
     * and using the character right after the underscore as the mnemonic key.
     * 
     * @param strname The label name
     * @return Mnemonic or KeyEvent.VK_UNDEFINED if none found.
     */
    public static int getMnemonicKey(String strname)
    {
        int mnemonic;
        String str = langProps.getProperty(strname, strname);
        int index = str.indexOf('_');
        if (index == -1 || (index + 1) >= str.length()) {
            mnemonic = KeyEvent.VK_UNDEFINED;
        }
        else {
            mnemonic = str.codePointAt(index + 1);
        }
        return mnemonic;
    }
    
    /**
     * Check whether a particular label has an accelerator key defined.
     * @param strname  The label name to check
     * @return     True if an accelerator key is defined
     */
    public static boolean hasAcceleratorKey(String strname)
    {
        return langProps.getProperty(strname, strname).indexOf('@') != -1;
    }

    @OnThread(Tag.FX)
    public static KeyCombination getAcceleratorKeyFX(String strname)
    {
        // In principle, hasAcceleratorKey() should be invoked before invoking
        // getAcceleratorKey() to take a suitable action according to the case
        // in place. However, we should check again here as a precaution to avoid
        // any future bug or NPE been thrown for no reason.
        if (!hasAcceleratorKey(strname))
            return null;

        int index;
        List<KeyCombination.Modifier> modifiers = new ArrayList<>();
        modifiers.add(KeyCombination.SHORTCUT_DOWN);
        String str = langProps.getProperty(strname, strname);
        String keyString;
        index = str.indexOf('@');
        index++;
        if(str.charAt(index) == '^') { //then the modifiers is CTRL + SHIFT
            index++;
            modifiers.add(KeyCombination.SHIFT_DOWN);
        }
        keyString = str.substring(index).toUpperCase();
        if(keyString.length() == 1) {
            return new KeyCharacterCombination(keyString, modifiers.toArray(new KeyCombination.Modifier[0]));
        }
        if (keyString.equals("BACK_SPACE"))
            keyString = "Backspace";

        KeyCode keyCode = KeyCode.getKeyCode(keyString);
        if (keyCode != null)
            return new KeyCodeCombination(keyCode, modifiers.toArray(new KeyCombination.Modifier[0]));
        else
        {
            Debug.message("Unknown key: \"" + keyString + "\"");
            return null;
        }
    }

    /**
     * Get a system-dependent string from the BlueJ properties
     * System-dependent strings are properties that can
     * start with an OS ID prefix (though it will default to
     * finding just the plain property name in the case where the
     * system id'ed version does not exist).
     * Returns null if the property does not exist
     */
    public static String getSystemPropString(String propName)
    {
        String sysID;

        if (osname != null && osname.startsWith("Windows 9")) {    // win95/98
            sysID = "win9x";
        }
        else if (osname != null && osname.equals("Windows Me")) { // winME (same as 95/98)
            sysID = "win9x";
        }
        else if (osname != null && osname.startsWith("Windows")) { // NT/2000/XP
            sysID = "win";
        }
        else if (osname != null && osname.startsWith("Linux")) {    // Linux
            sysID = "linux";
        }
        else if (osname != null && osname.startsWith("SunOS")) {   // Solaris
            sysID = "solaris";
        }
        else if (osname != null && osname.startsWith("Mac")) {     // MacOS
            sysID = "macos";
        }
        else {
            sysID = "";
        }

        // try to find it using the sysId prefix
        String value = commandProps.getProperty(sysID + propName);

        // if that failed, just look for the plain property value
        if(value == null) {
            value = commandProps.getProperty(propName);
        }

        return value;
    }

    /**
     * Get a non-language-dependent string from the BlueJ properties
     * ("bluej.defs" or "bluej.properties"). If not defined, the property
     * name is returned unchanged.
     */
    public static String getPropString(String strname)
    {
        // Don't pass strname as the second parameter to getPropString, or
        // variable substitution will be performed.
        String rval = getPropString(strname, null);
        if (rval == null) {
            rval = strname;
        }
        return rval;
    }
    
    /**
     * Get a non-language-dependent string from the BlueJ properties
     * ("bluej.defs" or "bluej.properties") with a default value. Variable
     * substitution ($varname) is performed on the value (and will be
     * performed on the default value if that is used).
     */
    public static String getPropString(String strname, String def)
    {
        return getPropString(strname, def, commandProps);
    }
    
    /**
     * Get a property string from the given properties map, using variable substitution.
     * If the variable is not defined the given default value is used (and variable
     * substitution is performed on it).
     * 
     * @param strname  The name of the property thats value is to be retrieved
     * @param def      The default value to use if the value is not defined
     * @param props    The properties to retrieve the value from
     * @return  The property value after variable substitution
     */
    public static String getPropString(String strname, String def, Properties props)
    {
        String propVal = props.getProperty(strname, def);
        if (propVal == null) {
            propVal = def;
        }
        if (propVal != null) {
            return PropParser.parsePropString(propVal, props);
        }
        return null;
    }

    /**
     * Get a non-language-dependent string from the BlueJ properties
     * "bluej.defs" with a default value. No variable substitution is
     * performed.
     */
    public static String getDefaultPropString(String strname, String def)
    {
        return systemProps.getProperty(strname, def);
    }

    /**
     * Get a non-language dependant integer from the BlueJ properties
     * ("bluej.defs" or "bluej.properties") with a default value
     */
    public static int getPropInteger(String intname, int def)
    {
        int value;
        try {
            value = Integer.parseInt(getPropString(intname, String.valueOf(def)));
        }
        catch(NumberFormatException nfe) {
            return def;
        }
        return value;
    }

    private static int getPropInteger(String intname, int def, Properties props)
    {
        int value;
        try {
            value = Integer.parseInt(getPropString(intname, String.valueOf(def), props));
        }
        catch(NumberFormatException nfe) {
            return def;
        }
        return value;
    }

    /**
     * Gets a boolean value from the BlueJ properties as JavaFX observable
     * property.  Any changes to the property will be saved to the BlueJ properties
     * automatically.
     *
     * Only one instance is created per property name for the lifetime of
     * the program.  This is generally good as you can call this method twice
     * and updates will be reflected in both returned properties (because it
     * will be the same property object!).  But be careful if you bind
     * the property as you may be unbinding an earlier binding.
     */
    @OnThread(Tag.FXPlatform)
    public static BooleanProperty getPropBooleanProperty(String propname)
    {
        return booleanProperties.computeIfAbsent(propname, p -> {
            boolean initial = getPropBoolean(propname);
            SimpleBooleanProperty prop = new SimpleBooleanProperty(initial);
            JavaFXUtil.addChangeListener(prop, b -> putPropBoolean(propname, b));
            return prop;
        });
    }
    
    /**
     * Get a boolean value from the BlueJ properties. The default value is false.
     */
    public static boolean getPropBoolean(String propname)
    {
        return parseBoolean(getPropString(propname, null));
    }
    
    /**
     * Get a boolean value from the BlueJ properties, with the specified default.
     */
    public static boolean getPropBoolean(String propname, boolean def)
    {
        String propval = getPropString(propname);
        if (propval == null) {
            return def;
        }
        return parseBoolean(propval);
    }
    
    /**
     * Parses the string argument as a boolean.  The <code>boolean</code> 
     * returned represents the value <code>true</code> if the string argument 
     * is not <code>null</code> and is equal, ignoring case, to the string 
     * <code>"true"</code>.
     *
     * @param      s   the <code>String</code> containing the boolean
     *                 representation to be parsed
     * @return     the boolean represented by the string argument
     */
    private static boolean parseBoolean(String s) {
        return ((s != null) && s.equalsIgnoreCase("true"));
    }

    /**
     * remove a property value from the BlueJ properties.
     */
    public static String removeProperty(String propertyName)
    {
        return (String)(userProps.remove(propertyName));
    }

    /**
     * Find and return the file name for an image.
     */
    private static File getImageFile(String propname)
    {
        String filename = getPropString(propname, null);

        if (filename != null) {
            return new File(bluejLibDir, "images" + File.separator + filename);
        }

        return null;
    }
    
    /**
     * Find and return the icon for an image, using the definitions in the 
     * properties files to find the actual image.
     */
    @OnThread(Tag.Swing)
    public static ImageIcon getImageAsIcon(String propname)
    {
        try {
            java.net.URL u = getImageFile(propname).toURI().toURL();
            return new ImageIcon(u);
        }
        catch (java.net.MalformedURLException mue) { }
        catch (NullPointerException npe) { }
        return null;
    }

    /**
     * Gets an image as an FX image.  Note that you pass the property name
     * as the parameter, which gets resolved via getPropString.
     * If you don't want this resolution, use getFixedImageAsFXImage
     */
    @OnThread(Tag.FX)
    public static javafx.scene.image.Image getImageAsFXImage(String propname)
    {
        try
        {
            java.net.URL u = getImageFile(propname).toURI().toURL();
            return new javafx.scene.image.Image(u.toString());
        }
        catch (java.net.MalformedURLException mue) { }
        catch (NullPointerException npe) { }
        return null;
    }
    
    /**
     * Return an icon for an image file name, without going through bluej.defs.
     * The parameter specifies the final image name, not a property.
     */
    @OnThread(Tag.Swing)
    public static ImageIcon getFixedImageAsIcon(String filename)
    {
        if (filename == null)
            throw new IllegalArgumentException("Cannot load null image");
        
        File image = new File(bluejLibDir, "images" + File.separator + filename);
        try {
            return new ImageIcon(image.toURI().toURL());
        }
        catch (java.net.MalformedURLException mue) { }
        return null;
    }

    /**
     * Gets the given file name from the images directory as an image.
     * @param filename
     * @return
     */
    @OnThread(Tag.FX)
    public static javafx.scene.image.Image getFixedImageAsFXImage(String filename)
    {
        if (filename == null)
            throw new IllegalArgumentException("Cannot load null image");
        
        File image = new File(bluejLibDir, "images" + File.separator + filename);
        try {
            return new javafx.scene.image.Image(image.toURI().toURL().toString());
        }
        catch (java.net.MalformedURLException mue) { }
        return null;
    }

    /**
     * Find and return an image. The image will have to be tracked. 
     */
    public static Image getImage(String propname)
    {
        try {
            java.net.URL u = getImageFile(propname).toURI().toURL();
            return Toolkit.getDefaultToolkit().createImage(u);
        }
        catch (java.net.MalformedURLException mue) { }
        catch (NullPointerException npe) { }        
        return null;
    }

    /**
     * Find the path to an executable command that may be located
     * in the JDK bin directory, and whose location may optionally
     * be explicitly specified in the properties.
     *
     * The logic goes like this: some tools such as javac, appletviewer
     * etc should be run from the same bin directory as the JDK that
     * launched bluej (rather than the first one in the path which may
     * be of a different version). So for all these properties, if the
     * property DOES NOT exist, we try to locate the executable in the
     * JDK directory and if we can't find it we use just the command
     * name.
     * 
     * If the property DOES exist we return it and it will be resolved
     * by the Runtime.exec call (ie looked for in the current path if
     * the command name is not an absolute path). The specified property
     * may be null, in which case the property is assumed not to exist.
     *
     * This method never returns null (at the very least it returns the
     * executableName)
     */
    public static String getJDKExecutablePath(String propName, String executableName)
    {
        if (executableName == null)
            throw new IllegalArgumentException("must provide an executable name");

        String p = propName == null ? null : getSystemPropString(propName);

        if (p == null) {
            // look for it in the JDK bin directory
            String jdkPathName = System.getProperty("java.home");

            if (jdkPathName != null) {
                // first check the closest bin directory
                File jdkPath = new File(jdkPathName);
                File binPath = new File(jdkPath, "bin");

                // try to find normal (unix??) executable
                File potentialExe = new File(binPath, executableName);
                if(potentialExe.exists())
                    return potentialExe.getAbsolutePath();
                
                // try to find windows executable
                potentialExe = new File(binPath, executableName + ".exe");
                if(potentialExe.exists())
                    return potentialExe.getAbsolutePath();

                // we could be in a JRE directory INSIDE a JDK directory
                // so lets go up one level and try again
                jdkPath = jdkPath.getParentFile();
                if (jdkPath != null) {
                    binPath = new File(jdkPath, "bin");

                    // try to find normal (unix??) executable
                    potentialExe = new File(binPath, executableName);
                    if(potentialExe.exists())
                        return potentialExe.getAbsolutePath();
                        // try to find windows executable
                        potentialExe = new File(binPath, executableName + ".exe");
                        if(potentialExe.exists())
                            return potentialExe.getAbsolutePath();
                }
            }

            return executableName;
        }

        return p;
    }

    /**
     * Return the template directory.
     */
    public static File getTemplateDir()
    {
    	if (templateDir == null) {
    		String path = getPropString("bluej.templatePath", "");
    		if(path.length() == 0) {
    			templateDir = getLanguageFile("templates");
    			if (! templateDir.exists()) {
    				templateDir = getDefaultLanguageFile("templates");
    			}
    			return templateDir;
    		}
    		else {
    			templateDir = new File(path); 
    		}
    	}
        return templateDir;
    }

    /**
     * Find and return the file name for a class template file
     * Format: <template-dir>/<base>.tmpl
     */
    public static File getTemplateFile(String base)
    {
        return new File(getTemplateDir(), base + ".tmpl");
    }

    /**
     * Return the template directory.
     */
    public static File getClassTemplateDir()
    {
        return new File(getTemplateDir(), "newclass");
    }

    /**
     * Find and return the file name for a class template file
     * Format: <template-dir>/<base>.tmpl
     */
    public static File getClassTemplateFile(String base)
    {
        return new File(getClassTemplateDir(), base + ".tmpl");
    }
    
    /**
     * Return the file with language specific text. 
     * For example,
     * <CODE>bluej/lib/english/dialogs</CODE> if base is <CODE>dialogs</CODE>
     * and the current language is <CODE>english</CODE>.
     */
    public static File getLanguageFile(String base)
    {
        return new File(bluejLibDir, language + File.separator + base);
    }

    /**
     * return the default language version of a language resource file
     */
    public static File getDefaultLanguageFile(String base)
    {
        return new File(bluejLibDir, DEFAULT_LANGUAGE + File.separator + base);
    }

    /**
     * Return the file name for a file in the user config directory
     * (<user_home>/.bluej/<base>)
     */
    public static File getUserConfigFile(String base)
    {
        return new File(userPrefDir, base);
    }

    /**
     * Return the user config directory
     * (<user_home>/.bluej)
     */
    public static File getUserConfigDir()
    {
        return userPrefDir;
    }
    
    /**
     * Return a color value from the bluej properties.
     */
    public static Color getItemColour(String itemname)
    {
        try {
            String rgbStr = getPropString(itemname, "255,0,255");
            String rgbVal[] = Utility.split(rgbStr, ",");

            if (rgbVal.length < 3)
                Debug.reportError("Error reading colour ["+itemname+"]");
            else {
                int r = Integer.parseInt(rgbVal[0].trim());
                int g = Integer.parseInt(rgbVal[1].trim());
                int b = Integer.parseInt(rgbVal[2].trim());

                return new Color(r, g, b);
            }
        }
        catch(NumberFormatException e) {
            Debug.reportError("Could not get colour for " + itemname);
        }

        return null;
    }
    
    /**
     * Return a color value from the bluej properties.
     * 
     * If the value is not present, return null
     */
    public static Color getOptionalItemColour(String itemname)
    {
        try {
            String rgbStr = getPropString(itemname, null);
            if (rgbStr == null) {
                return null;
            }
            
            String rgbVal[] = Utility.split(rgbStr, ",");
            if (rgbVal.length < 3) {
                Debug.reportError("Error reading colour ["+itemname+"]");
            }
            else {
                int r = Integer.parseInt(rgbVal[0].trim());
                int g = Integer.parseInt(rgbVal[1].trim());
                int b = Integer.parseInt(rgbVal[2].trim());
                
                return new Color(r, g, b);
            }
        }
        catch(NumberFormatException e) {
            Debug.reportError("Could not get colour for " + itemname);
        }

        return null;
    }

    /**
     * Get a font from a specified property, using the given default font name and
     * the given size. Font name can end with "-bold" to indicate bold style.
     */
    public static Font getFont(String propertyName, String defaultFontName, int size)
    {
        String fontName = getPropString(propertyName, defaultFontName);
        
        int style;
        if(fontName.endsWith("-bold")) {
            style = Font.BOLD;
            fontName = fontName.substring(0, fontName.length()-5);
        }
        else {
            style = Font.PLAIN;
        }
        
        return new Font(fontName, style, size);
    }

    /**
     * Store a point in the config files. The config properties
     * are formed by adding ".x" and ".y" to the itemPrefix.
     */
    private static void putLocation(String itemPrefix, int x, int y)
    {
        putPropInteger(itemPrefix + ".x", Math.max(0, x));
        putPropInteger(itemPrefix + ".y", Math.max(0, y));
    }

    /**
     * Return a point, read from the config files. The config properties
     * are formed by adding ".x" and ".y" to the itemPrefix.
     */
    @OnThread(Tag.FX)
    private static Point2D getLocation(String itemPrefix)
    {
        int x = getPropInteger(itemPrefix + ".x", 16);
        int y = getPropInteger(itemPrefix + ".y", 16);

        return ensureOnScreen(x, y);
    }

    /**
     * Return a point, read from the config files. The config properties
     * are formed by adding ".width" and ".height" to the itemPrefix.
     * Unless both sizes can be found, null is returned.  A minimum size of
     * 50 in both dimensions is enforced.
     */
    private static Dimension getSize(String itemPrefix)
    {
        int w = getPropInteger(itemPrefix + ".width", -1);
        int h = getPropInteger(itemPrefix + ".height", -1);

        if (w == -1 || h == -1)
            return null;

        return new Dimension(Math.max(w, 50), Math.max(h, 50));
    }

    /**
     * Store a size in the config files. The config properties
     * are formed by adding ".width" and ".height" to the itemPrefix.
     */
    private static void putSize(String itemPrefix, int width, int height)
    {
        putPropInteger(itemPrefix + ".width", Math.max(width, 100));
        putPropInteger(itemPrefix + ".height", Math.max(height, 100));
    }

    /**
     * Set a non-language dependant integer for the BlueJ properties
     */
    public static void putPropInteger(String intname, int value)
    {
        String defVal = systemProps.getProperty(intname);
        if (defVal != null) {
            try {
                if (value == Integer.valueOf(defVal).intValue()) {
                    userProps.remove(intname);
                }
            }
            catch (NumberFormatException nfe) { }
        }
        userProps.setProperty(intname, Integer.toString(value));
    }
    
    /**
     * Set a non-language dependant string for the BlueJ properties.
     * If the supplied value is null, the property is removed.
     */
    public static void putPropString(String strname, String value)
    {
        String defVal = systemProps.getProperty(strname);
        if (value != null && (defVal == null || ! defVal.equals(value))) {
            userProps.setProperty(strname, value);
        }
        else {
            userProps.remove(strname);
        }
    }
    
    /**
     * Set a non-language dependent boolean value for the BlueJ properties
     */
    public static void putPropBoolean(String propname, boolean value)
    {
        String sysval = systemProps.getProperty(propname);
        if (Boolean.valueOf(sysval).booleanValue() == value) {
            userProps.remove(propname);
        }
        else {
            userProps.setProperty(propname, String.valueOf(value));
        }
    }
    
    /**
     * Returns the blueJLibDir
     */
    public static File getBlueJLibDir()
    {
        return bluejLibDir;
    }

    /**
     * Returns the greenfoot directory in blueJLibDir
     */
    public static File getGreenfootLibDir()
    {
        return greenfootLibDir;
    }
    
    /**
     * Returns the blueJLibDir
     */
    public static String getBlueJIconPath()
    {
        return bluejLibDir.getPath() + "/images";
    }
    
    /**
     * Checks for optional bluej.defs settings for vm language and
     * country. If either are specified, a Locale object is created
     * and it becomes the default locale for BlueJ. 
     */
    private static void setVMLocale()
    {
        String lang = Config.getPropString("vm.language", null);
        String region = Config.getPropString("vm.country", null);
        
        // nothing specified, its either commented out or no value 
        if((lang == null || "".equals(lang)) && (region == null || "".equals(region)))
            return;
        
        // something has been specified...
        // if only one of region or language is specified only, make the other
        // use the existing default to create the Locale object
        // This gets rid of any dependencies in bluej.defs between the two
        // which is a possible cause of config errors for users
        if(lang == null || lang.equals(""))
            lang = System.getProperty("user.language");
        if(region == null || region.equals(""))
            region = System.getProperty("user.country");
        debugVMArgs.add("-Duser.language=" + lang);
        debugVMArgs.add("-Duser.country=" + region);
        Locale loc = new Locale(lang, region);
        Locale.setDefault(loc);
    }

    /**
     * Initialise debug VM args from bluej config file
     * Should only be called once in Config.initialise(...)
     */
    private static void initDebugVMArgs()
    {
        String args = getPropString("bluej.vm.args");
        if(args != null && !args.equals("bluej.vm.args")) {
            // if there is more than one arg set
            List<String> splitArgs = Utility.dequoteCommandLine(args);
            debugVMArgs.addAll(splitArgs);
        }        
    }
    
    /**
     * debug vm args used for launch of debug vm
     */
    public static List<String> getDebugVMArgs()
    {
        return debugVMArgs;
    }
    
    /**
     * Method to determine if BlueJ is running in greenfoot mode. Greenfoot mode
     * is used when we are launching BlueJ to be used for greenfoot.
     * 
     * To switch on greenfoot mode the property greenfoot should be set to true.
     * This can be done by starting BlueJ with this command line argument
     * -greenfoot=true
     */
    public static final boolean isGreenfoot()
    {
        return isGreenfoot;
    }

    /**
     * Determine whether a file is a ZIP File.
     */
    public static boolean isZipFile(File file)
    {
        try {
            if(file.isDirectory()) {
                return false;
            }
            if(!file.canRead()) {
                throw new IOException();
            }
            if(file.length() < 4) {
                return false;
            }
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            int magicNumber = in.readInt();
            in.close();
            return magicNumber == 0x504b0304;
        }
        catch (IOException exc) {
            Debug.reportError("Could not read file: " + file.getAbsolutePath(), exc);
        }
        return false;
    }

    @OnThread(Tag.FX)
    public static void addEditorStylesheets(Scene scene)
    {
        String[] stylesheetStems = new String[] {
                "frame-style",
                "editor-banners",
                "editor-catalogue",
                "editor-error-bar",
                "editor-error-fix",
                "editor-expression",
                "editor-help",
                "editor-menu",
                "editor-selection",
                "editor-slot-choice",
                "editor-suggestions",
                "editor-tabs",
                "flow",
                "moe",
                "shared"};
        
        for (String stem : stylesheetStems)
        {
            addStylesheet(scene.getStylesheets(), stem);
        }
        addJavaColorsStylesheet(scene.getStylesheets());
    }

    @OnThread(Tag.FX)
    public static void addInspectorStylesheets(Scene scene)
    {
        addStylesheet(scene.getStylesheets(), "inspectors");
    }

    @OnThread(Tag.FX)
    public static void addPopupStylesheets(Parent root)
    {
        addStylesheet(root.getStylesheets(), "popup");
    }

    @OnThread(Tag.FX)
    public static void addTestsStylesheets(Scene scene)
    {
        addStylesheet(scene.getStylesheets(), "tests");
    }

    @OnThread(Tag.FX)
    public static void addTerminalStylesheets(Scene scene)
    {
        addStylesheet(scene.getStylesheets(), "terminal");
    }

    @OnThread(Tag.FX)
    public static void addDebuggerStylesheets(Scene scene)
    {
        addStylesheet(scene.getStylesheets(), "debugger");
        addStylesheet(scene.getStylesheets(), "shared");
    }


    @OnThread(Tag.FX)
    public static void addPMFStylesheets(Scene scene)
    {
        addStylesheet(scene.getStylesheets(), "pkgmgrframe");
    }

    @OnThread(Tag.FX)
    public static void addGreenfootStylesheets(Scene scene)
    {
        addStylesheet(scene.getStylesheets(), "greenfoot");
    }


    @OnThread(Tag.FX)
    private static void addStylesheet(ObservableList<String> sheetList, String stem)
    {
        try
        {
            sheetList.add(new File(bluejLibDir + "/stylesheets", stem + ".css").toURI().toURL().toString());
        }
        catch (MalformedURLException e)
        {
            Debug.reportError(e);
        }
    }

    @OnThread(Tag.FX)
    public static void addDialogStylesheets(Pane dialogPane)
    {
        addStylesheet(dialogPane.getStylesheets(), "dialogs");
        addJavaColorsStylesheet(dialogPane.getStylesheets());
    }

    @OnThread(Tag.FX)
    private static void addJavaColorsStylesheet(ObservableList<String> stylesheets)
    {
        // First add ours, so that it acts as a default:
        addStylesheet(stylesheets, "java-colors");
        // Then add the users, so that it will take precendence:

        File userColorsFile = getUserConfigFile("java-colors.css");
        if (userColorsFile.exists())
        {
            try
            {
                stylesheets.add(userColorsFile.toURI().toURL().toString());
                // Note this, in case we get support requests about weird colours:
                Debug.log("Using user-specified java-colors file.");
            }
            catch (MalformedURLException e)
            {
                Debug.reportError(e);
            }
        }
    }

    @OnThread(Tag.Swing)
    public static Border getFocusBorder()
    {
        if (focusBorder == null)
        {
            focusBorder = new CompoundBorder(new LineBorder(Color.BLACK),
                    new BevelBorder(BevelBorder.LOWERED,
                            new Color(195, 195, 195),
                            new Color(240, 240, 240),
                            new Color(195, 195, 195),
                            new Color(124, 124, 124)));
        }
        return focusBorder;
    }

    @OnThread(Tag.Swing)
    public static Border getNormalBorder()
    {
        if (normalBorder == null)
        {
            normalBorder = new CompoundBorder(new EmptyBorder(1,1,1,1),
                    new BevelBorder(BevelBorder.LOWERED,
                            new Color(195, 195, 195),
                            new Color(240, 240, 240),
                            new Color(124, 124, 124),
                            new Color(195, 195, 195)));
        }
        return normalBorder;
    }

    public static void loadFXFonts()
    {
        if (!fontOptions.isEmpty())
            return;
        
        //fontOptions = Arrays.asList("Droid Sans", "Montserrat", "Noto Sans", "Roboto", "Open Sans", "Source Sans", "Ubuntu");
        //List<String> fontStems = Arrays.asList("NotoSans-Regular", "NotoSans-Bold", "NotoSans-Italic", "NotoSans-BoldItalic");
        for (File file : new File(bluejLibDir + "/fonts").listFiles())
        {
            if (file.getName().toLowerCase().endsWith(".ttf")) {
                try
                {
                    //Debug.message("Loading font: " + file);
                    FileInputStream fis = new FileInputStream(file);
                    final javafx.scene.text.Font font = javafx.scene.text.Font.loadFont(fis, 10);
                    fis.close();
                    if (font == null) {
                        Debug.reportError("Unknown problem loading TTF JavaFX font: " + file.getAbsolutePath());
                    }
                    if (font != null && !fontOptions.contains(font.getFamily()))
                    {
                        fontOptions.add(font.getFamily());
                        Collections.sort(fontOptions);
                    }
                }
                // Catch everything, because we have seen UnsatisfiedLinkError here,
                // and we don't want the the exception to propagate outwards:
                catch (Throwable e)
                {
                    Debug.reportError("Error loading font: " + file.getAbsolutePath(), e);
                }
            }
        }
    }

    @OnThread(Tag.FXPlatform)
    public static void loadAndTrackPosition(Window window, String locationPrefix)
    {
        JavaFXUtil.addChangeListener(window.xProperty(), x -> putLocation(locationPrefix, (int)window.getX(), (int)window.getY()));
        JavaFXUtil.addChangeListener(window.yProperty(), y -> putLocation(locationPrefix, (int)window.getX(), (int)window.getY()));

        Point2D location = getLocation(locationPrefix);
        window.setX(location.getX());
        window.setY(location.getY());
    }

    @OnThread(Tag.FXPlatform)
    public static void loadAndTrackPositionAndSize(Window window, String locationPrefix)
    {
        loadAndTrackPosition(window, locationPrefix);

        JavaFXUtil.addChangeListener(window.widthProperty(), x -> putSize(locationPrefix, (int)window.getWidth(), (int)window.getHeight()));
        JavaFXUtil.addChangeListener(window.heightProperty(), y -> putSize(locationPrefix, (int)window.getWidth(), (int)window.getHeight()));

        Dimension location = getSize(locationPrefix);
        if (location != null)
        {
            window.setWidth(location.width);
            window.setHeight(location.height);
        }
    }

    /**
     * Remembers the position of the split pane's divider.  Assumes there is only one divider (for now).
     * Only call after you've added the split pane items, as otherwise divider won't yet exist.
     *
     *  @param window The window containing the split pane.  We listen to window-showing for setting the split pane.
     */
    @OnThread(Tag.FXPlatform)
    public static void rememberDividerPosition(Window window, SplitPane splitPane, String locationName)
    {
        // We store the double as an integer by multiplying by scale:
        double SCALE = 1_000_000;

        double initialPos = (double)Config.getPropInteger(locationName, (int) (0.5 * SCALE)) / SCALE;

        JavaFXUtil.addChangeListener(splitPane.getDividers().get(0).positionProperty(), pos -> putPropInteger(locationName, (int)(pos.doubleValue() * SCALE)));

        if (window.isShowing())
        {
            splitPane.setDividerPosition(0, initialPos);
        }
        else
        {
            window.addEventHandler(WindowEvent.WINDOW_SHOWN, e ->
            {
                splitPane.setDividerPosition(0, initialPos);
            });
        }
    }

    public static KeyCode getKeyCodeForYesNo(InteractionManager.ShortcutKey keyPurpose)
    {
        switch (keyPurpose)
        {
            case YES_ANYWHERE: return KeyCode.F2;
            case NO_ANYWHERE: return KeyCode.F3;
        }
        return null;
    }

    public static boolean isGreenfootStartupProject(File projectDir)
    {
        return getGreenfootStartupProjectPath().equals(projectDir.getAbsolutePath());
    }

    public static String getGreenfootStartupProjectPath()
    {
        return new File(getBlueJLibDir(), "greenfoot/startupProject").getAbsolutePath();
    }

    @OnThread(Tag.FX)
    public static Point2D ensureOnScreen(int x, int y)
    {
        // First, work out if the point is on any of the screens:
        boolean onScreenAlready = Screen.getScreens().stream().anyMatch(screen -> {
            // We count it as on screen if it's within the bounds, and not within 80 pixels of the right-hand or bottom-hand edge:
            return screen.getVisualBounds().contains(x, y) && screen.getVisualBounds().contains(x + 80, y + 80);
        });

        if (onScreenAlready)
        {
            return new Point2D(x, y);
        }
        else
        {
            // Put it on the top-left of the primary screen:
            return new Point2D(Screen.getPrimary().getBounds().getMinX() + 100, Screen.getPrimary().getBounds().getMinY() + 100);
        }
    }

    @OnThread(Tag.FX)
    public static Node makeStopIcon(boolean large)
    {
        Polygon octagon = new Polygon(
                9, 0,
                22, 0,
                31, 9,
                31, 22,
                22, 31,
                9, 31,
                0, 22,
                0, 9
        );
        if (!large)
        {
            JavaFXUtil.scalePolygonPoints(octagon, 0.5, false);
        }
        JavaFXUtil.addStyleClass(octagon, "octagon");
        Label stop = new Label("STOP");
        stop.setMouseTransparent(true);
        StackPane stackPane = new StackPane(octagon, stop);
        JavaFXUtil.setPseudoclass("bj-large", large, stackPane);
        JavaFXUtil.addStyleClass(stackPane, "stop-icon");
        return stackPane;
    }

    @OnThread(Tag.FX)
    public static Polygon makeArrowShape(boolean shortTail)
    {
        return new Polygon(
                10, 0,
                18, 10,
                10, 20,
                10, 14,
                shortTail ? 4 : 0, 14,
                shortTail ? 4 : 0, 6,
                10, 6
            );
    }

    /**
     * This is almost equivalent to the SourceType in bluej.extensions, but we 
     * don't want Config to depend on that class so we re-create the same idea here.
     */
    public enum SourceType
    {
        Java,
        Stride
    }
}
