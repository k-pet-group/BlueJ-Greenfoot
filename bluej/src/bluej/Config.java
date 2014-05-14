/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014  Michael Kolling and John Rosenberg 
 
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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.Scanner;

import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.metal.MetalLookAndFeel;

import bluej.utility.Debug;
import bluej.utility.Utility;

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

    public static Properties moeSystemProps;  // moe (editor) properties
    public static Properties moeUserProps;    // moe (editor) properties

    public static String compilertype = "javac";  // current compiler (javac, jikes)
    public static String language;      // message language (english, ...)

    public static Rectangle screenBounds; // maximum dimensions of screen

    public static final String osname = System.getProperty("os.name", "");
    public static final String DEFAULT_LANGUAGE = "english";
    public static final String BLUEJ_OPENPACKAGE = "bluej.openPackage";
    public static final String bluejDebugLogName = "bluej-debuglog.txt";
    public static final String greenfootDebugLogName = "greenfoot-debuglog.txt";
    public static String debugLogName = bluejDebugLogName;
    
    private static Boolean isRaspberryPi = null;

    public static final Color ENV_COLOUR = new Color(152,32,32);


    // a border for components with keyboard focus
    public static final Border focusBorder = new CompoundBorder(new LineBorder(Color.BLACK),
            new BevelBorder(BevelBorder.LOWERED,
                    new Color(195, 195, 195),
                    new Color(240, 240, 240),
                    new Color(195, 195, 195),
                    new Color(124, 124, 124)));

    // a border for components without keyboard focus
    public static final Border normalBorder = new CompoundBorder(new EmptyBorder(1,1,1,1),
            new BevelBorder(BevelBorder.LOWERED,
                    new Color(195, 195, 195),
                    new Color(240, 240, 240),
                    new Color(124, 124, 124),
                    new Color(195, 195, 195)));


    // bluej configuration properties hierarchy
    // (command overrides user which overrides system)
    
    private static Properties systemProps;      // bluej.defs
    private static Properties userProps;        // <user home>/bluej.properties
    private static Properties greenfootProps;   // greenfoot.defs
    private static Properties commandProps;     // specified on the command line

    private static Properties initialCommandLineProps; // The properties
                                                       // specified on the
                                                       // command line
    private static Properties langProps;        // international labels
    private static Properties langVarProps;     // language label variables (APPNAME)

    private static BlueJPropStringSource propSource; // source for properties

    private static File bluejLibDir;
    private static File userPrefDir;
    /** The greenfoot subdirectory of the "lib"-directory*/ 
    private static File greenfootLibDir;
    
    private static boolean usingMacOSScreenMenubar;

    private static boolean initialised = false;
    private static boolean isGreenfoot = false;
    
    /** name of the icons file for the VM on Mac */
    private static final String BLUEJ_DEBUG_DOCK_ICON = "vm.icns";
    private static final String GREENFOOT_DEBUG_DOCK_ICON = "greenfootvm.icns";
    
    /** name of the VM in the dock on Mac */
    private static final String BLUEJ_DEBUG_DOCK_NAME = "BlueJ Virtual Machine";
    private static final String GREENFOOT_DEBUG_DOCK_NAME = "Greenfoot";
    
    protected static final int SHORTCUT_MASK =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    
    // Bit ugly having it here, but it's needed by MiscPrefPanel (which may just be in BlueJ)
    // and by Greenfoot
    public static final KeyStroke GREENFOOT_SET_PLAYER_NAME_SHORTCUT =
        KeyStroke.getKeyStroke(KeyEvent.VK_P, SHORTCUT_MASK | InputEvent.SHIFT_DOWN_MASK);
    
    private static Color selectionColour;
    private static Color selectionColour2;
    private static Color highlightColour;
    private static Color highlightColour2;
    private static List<String> debugVMArgs = new ArrayList<String>();
    
    /** whether this is the debug vm or not. */
    private static boolean isDebugVm = true; // Default to true, will be corrected on main VM

    /**
     * Initialisation of BlueJ configuration. Must be called at startup.
     * This method finds and opens the configuration files.<p>
     * 
     * See also initializeVMside().
     */
    public static void initialise(File bluejLibDir, Properties tempCommandLineProps,
                                  boolean bootingGreenfoot)
    {
        initialise(bluejLibDir, tempCommandLineProps, bootingGreenfoot, true);
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

        // Whether or not to use the screen menu bar on a Mac
        String macOSscreenMenuBar = Config.getPropString("bluej.macos.screenmenubar", "true");
        // The value of the BlueJ property overrides the system setting
        System.setProperty("apple.laf.useScreenMenuBar", macOSscreenMenuBar);      

        usingMacOSScreenMenubar = (isMacOS() && macOSscreenMenuBar.equals("true"));
        
        boolean themed = Config.getPropBoolean("bluej.useTheme");
        if(themed) {    
            MetalLookAndFeel.setCurrentTheme(new BlueJTheme());
        }

        String laf = Config.getPropString("bluej.lookAndFeel", "bluejdefault");
        setLookAndFeel(laf);
        
        //read any debug vm args
        initDebugVMArgs();
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
            Properties tempCommandLineProps, boolean bootingGreenfoot,
            BlueJPropStringSource propSource)
    {
        isDebugVm = true;
        Config.propSource = propSource;
        Config.userPrefDir = userConfigDir;

        // Set up the properties so that they use the properties from the
        // BlueJVM
        systemProps = new Properties() {
            @Override
            public String getProperty(String key)
            {
                return Config.propSource.getBlueJPropertyString(key, null);
            }

            @Override
            public String getProperty(String key, String def)
            {
                return Config.propSource.getBlueJPropertyString(key, def);
            }
        };
        userProps = new Properties(systemProps) {
            @Override
            public Object setProperty(String key, String val)
            {
                String rval = getProperty(key);
                Config.propSource.setUserProperty(key, val);
                return rval;
            }
            
            @Override
            public String getProperty(String key)
            {
                return Config.propSource.getBlueJPropertyString(key, null);
            }

            @Override
            public String getProperty(String key, String def)
            {
                return Config.propSource.getBlueJPropertyString(key, def);
            }
        };
        initialise(bluejLibDir, tempCommandLineProps, bootingGreenfoot, false);
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
        Config.propSource = propSource;
        
        langProps =  new Properties() {
            @Override
            public String getProperty(String key)
            {
                return Config.propSource.getLabel(key);
            }
            
            @Override
            public String getProperty(String key, String def)
            {
                return Config.propSource.getLabel(key);
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
    
        /**
         * Tell us whether we are running on Raspberry Pi. The first call of
         * this method performs the check and puts its result in the static
         * variable with the same same. Other calls just return the result
         * stored in the variable.
         */
        public static boolean isRaspberryPi()
            {
                if (Config.isRaspberryPi == null) {
                    boolean result = false;
                    if (Config.isLinux()) {
                        try {
                            Scanner scanner = new Scanner(new File(
                                    "/proc/cpuinfo"));
                            while (scanner.hasNextLine()) {
                                String lineFromFile = scanner.nextLine();
                                if (lineFromFile.contains("BCM2708")) {
                                    result = true;
                                    break;
                                }
                            }
                            scanner.close();
                        } catch (FileNotFoundException fne) {

                        }
                    }
                    Config.isRaspberryPi = result;
                }
                return Config.isRaspberryPi;
            }

    /**
     * Tell us whether we are running on MacOS 10.5 (Leopard) or later
     */
    public static boolean isMacOSLeopard()
    {
        return osname.startsWith("Mac") &&
                System.getProperty("os.version").compareTo("10.5") >= 0;
    }
    
    /**
     * Tell use whether we are running on MacOS 10.6 (Snow Leopard) or later
     */
    public static boolean isMacOSSnowLeopard()
    {
        return osname.startsWith("Mac") &&
                System.getProperty("os.version").compareTo("10.6") >= 0;
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
                && System.getProperty("os.version").compareTo("6.0") >= 0;
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
     * Tell us whether we are running on a Java VM that supports 1.5 features.
     */
    public static boolean isJava15()
    {
        return System.getProperty("java.specification.version").compareTo("1.5") >= 0;
    }
    
    /**
     * Tell us whether we are running on a Java VM that supports Java 6 features.
     */
    public static boolean isJava16()
    {
        return System.getProperty("java.specification.version").compareTo("1.6") >= 0;
    }
    
    /**
     * Tell us whether we are running on a Java VM that supports Java 7 features.
     */
    public static boolean isJava17()
    {
        return System.getProperty("java.specification.version").compareTo("1.7") >= 0;
    }
    
    /**
     * Tell us whether we are running OpenJDK.
     */
    public static boolean isOpenJDK()
    {
        return System.getProperty("java.runtime.name").startsWith("OpenJDK");
    }
    
    /**
     * Tell use whether java 1.5 features are to be used. This allows
     * suppressing 1.5 features when running on a 1.5 VM (for instance to
     * suppress the "unchecked" warnings which occur when compiling legacy
     * code). 
     */
    public static boolean usingJava15()
    {
        // for now, always use 1.5 features where available
        return isJava15();
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
     * Tell us whether we are using a Mac screen menubar
     */
    public static boolean usingMacScreenMenubar()
    {
        return usingMacOSScreenMenubar;
    }
    
    /**
     * Get the screen size information
     */
    private static Rectangle calculateScreenBounds()
    {
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
                    PrintStream outStream = new PrintStream(new FileOutputStream(debugLogFile));
                    System.setOut(outStream);
                    System.setErr(outStream);
                    Debug.setDebugStream(new OutputStreamWriter(outStream));

                    Debug.message(getApplicationName() + " run started: " + new Date());
                    if(isGreenfoot()) {
                        Debug.message("Greenfoot version: " + Boot.GREENFOOT_VERSION);
                    }
                    else {
                        Debug.message("BlueJ version " + Boot.BLUEJ_VERSION);
                    }
                    Debug.message("Java version " + System.getProperty("java.version"));
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
        final String name = getApplicationName().toLowerCase();
        saveProperties(name, "properties.heading." + name, userProps);     
        saveProperties("moe", "properties.heading.moe", moeUserProps);    
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
        catch(Exception e) {
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
        // if greenfoot, add specific additional labels
        if(isGreenfoot())
        {
            // load greenfoot labels to default lanbels
            String greenfootLabels = DEFAULT_LANGUAGE + File.separator + "greenfoot/greenfoot-labels";
            File greenfootLabelFile = new File(bluejLibDir, greenfootLabels);
            try{
                labels.load(new FileInputStream(greenfootLabelFile));
            }
            catch(Exception e){
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
        catch(IOException e) {
            // ignore exception - this will hapen on first run of BlueJ
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
    public static Properties getMoeHelp()
    {
        return loadDefs(language + File.separator + "moe.help", System.getProperties());
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
        if (langVarProps == null) {
            langVarProps = new Properties();
            langVarProps.put("APPNAME", getApplicationName());
        }
        
        int index;
        String str = langProps.getProperty(strname, def);
        // remove all underscores
        while( (index = str.indexOf('_')) != -1){
            str = str.substring(0, index) + str.substring(index+1);
        }
        if ((index = str.indexOf('@')) != -1){ 
            //remove everything from @
            str = str.substring(0, index);
        }
        
        str = PropParser.parsePropString(str, langVarProps);
        
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
    
    /**
     * parses the labels file and creates a KeyStroke with the right accelerator
     * key and modifiers
     * @param strname
     * @return a KeyStroke
     */
    public static KeyStroke getAcceleratorKey(String strname)
    {
        int index;
        int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        String str = langProps.getProperty(strname, strname);
        String keyString;
        index = str.indexOf('@');
        index++;
        if(str.charAt(index) == '^') { //then the modifiers is CTRL + SHIFT
            index++;
            modifiers |= InputEvent.SHIFT_MASK;
        }
        keyString = str.substring(index).toUpperCase();
        if(keyString.length() == 1) {
            return KeyStroke.getKeyStroke(keyString.codePointAt(0), modifiers);
        }
        KeyStroke k1= KeyStroke.getKeyStroke(keyString);
        return KeyStroke.getKeyStroke(k1.getKeyCode(), modifiers);
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
     * Return an icon for an image file name, without going through bluej.defs.
     * The parameter specifies the final image name, not a property.
     */
    public static ImageIcon getFixedImageAsIcon(String filename)
    {
        File image = new File(bluejLibDir, "images" + File.separator + filename);
        try {
            return new ImageIcon(image.toURI().toURL());
        }
        catch (java.net.MalformedURLException mue) { }
        catch (NullPointerException npe) { }
        return null;
    }

    /**
     * Find and return the icon for an image, without using the properties
     * (the name provided is the actual file name in ../lib/images).
     */
    public static ImageIcon getHardImageAsIcon(String filename)
    {
        try {
            File imgFile = new File(bluejLibDir, "images" + File.separator + filename);
            java.net.URL u = imgFile.toURI().toURL();

            return new ImageIcon(u);
        }
        catch (java.net.MalformedURLException mue) { }
        catch (NullPointerException npe) { }
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
        return getLanguageFile("templates");
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
        String path = commandProps.getProperty("bluej.templatePath" , "");
        if(path.length() == 0) {
            return getLanguageFile("templates/newclass");
        }
        return new File(path);
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
        catch(Exception e) {
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
        catch(Exception e) {
            Debug.reportError("Could not get colour for " + itemname);
        }

        return null;
    }
    
    /**
     * Return a color value for selections.
     */
    public static Color getSelectionColour()
    {
        if(selectionColour == null) {
            selectionColour = Config.getItemColour("colour.selection");
        }
        return selectionColour;
    }

    /**
     * Return the second (gradient) color value for selections.
     */
    public static Color getSelectionColour2()
    {
        if(selectionColour2 == null) {
            selectionColour2 = Config.getItemColour("colour.selection2");
        }
        return selectionColour2;
    }

    /**
     * Return a color value for selections.
     */
    public static Color getHighlightColour()
    {
        if(highlightColour == null) {
            highlightColour = Config.getItemColour("colour.highlight");
        }
        return highlightColour;
    }

    /**
     * Return the second (gradient) color value for selections.
     */
    public static Color getHighlightColour2()
    {
        if(highlightColour2 == null) {
            highlightColour2 = Config.getItemColour("colour.highlight2");
        }
        return highlightColour2;
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
    public static void putLocation(String itemPrefix, Point p)
    {
        putPropInteger(itemPrefix + ".x", p.x);
        putPropInteger(itemPrefix + ".y", p.y);
    }

    /**
     * Return a point, read from the config files. The config properties
     * are formed by adding ".x" and ".y" to the itemPrefix.
     */
    public static Point getLocation(String itemPrefix)
    {
        try {
            int x = getPropInteger(itemPrefix + ".x", 16);
            int y = getPropInteger(itemPrefix + ".y", 16);

            if (x > (screenBounds.width - 16))
                x = screenBounds.width - 16;

            if (y > (screenBounds.height - 16))
                y = screenBounds.height - 16;

            return new Point(x,y);
        }
        catch(Exception e) {
            Debug.reportError("Could not get screen location for " + itemPrefix);
        }

        return new Point(16,16);
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
     * Set Look and Feel for BlueJ interface
     * @param laf the l&f specified. Should be one of 3 options:
     * "system", "crossplatform" or "default"
     */
    private static void setLookAndFeel(String laf)
    {
        try {
            if (laf.equals("default")) {
                return;
            }
            
            // if system specified
            if(laf.equals("system")) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                return;
            }
            else if(laf.equals("crossplatform")) {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                usingMacOSScreenMenubar = false; // Screen menu bar requires aqua look-and-feel
                return;
            }
            
            if (! laf.equals("bluejdefault")) {
                LookAndFeelInfo [] lafi = UIManager.getInstalledLookAndFeels();
                for (int i = 0; i < lafi.length; i++) {
                    if (lafi[i].getName().equals(laf)) {
                        UIManager.setLookAndFeel(lafi[i].getClassName());
                        return;
                    }
                }
                
                // Try as a class name
                UIManager.setLookAndFeel(laf);
                return;
            }
            
            // do the "default, ie. let BlueJ decide
            // Windows - System l&F, Linux & Solaris - cross-platform
            if (isWinOS()){
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            // treat Linux and Solaris the same at the moment
            else if(isLinux() || isSolaris()) {
                LookAndFeelInfo [] lafi = UIManager.getInstalledLookAndFeels();
                LookAndFeelInfo nimbus = null;
                for (LookAndFeelInfo lafInstance : lafi) {
                    if (lafInstance.getName().equals("Nimbus")) {
                        nimbus = lafInstance;
                        break;
                    }
                }
                
                if (nimbus != null) {
                    UIManager.setLookAndFeel(nimbus.getClassName());
                }
                else {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                }
            }
        } catch (ClassNotFoundException e) {
            Debug.log("Could not find look-and-feel class: " + e.getMessage());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            Debug.log("Unsupported look-and-feel: " + e.getMessage());
        }
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
}
