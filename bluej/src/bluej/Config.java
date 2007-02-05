package bluej;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.StringTokenizer;

import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.*;
import javax.swing.plaf.metal.MetalLookAndFeel;

import bluej.utility.*;

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
 * command line arguemtns - contains per-launch specific settings.
 *    Settings here override settings in bluej.properties <BR>
 * <BR>
 * There is also a set of language specific labels 
 * in a directory named after the language
 *  &lt;bluej_home>/lib/&lt;language>/labels
 *
 * @author Michael Cahill
 * @author Michael Kolling
 * @author Andrew Patterson
 * @version $Id: Config.java 4836 2007-02-05 00:52:34Z davmac $
 */

public final class Config
{
    public static final String nl = System.getProperty("line.separator");

    // bluej configuration properties heirarchy
    // (command overrides user which overrides system)
    
    private static Properties system_props;  // bluej.defs
    private static Properties user_props;    // <user home>/bluej.properties
    private static Properties command_props; // specified on the command line
    
    public static Properties moe_system_props;  // moe (editor) properties
    public static Properties moe_user_props;    // moe (editor) properties
    
    private static Properties lang_props;	// international labels

    private static BlueJPropStringSource propSource; // source for properties

    private static File bluejLibDir;
    private static File userPrefDir;
    /** The greenfoot subdirectory of the "lib"-directory*/ 
    private static File greenfootLibDir;
    
    public static String compilertype;	// current compiler (javac, jikes)
    public static String language;	// message language (english, ...)

    public static Rectangle screenBounds; // maximum dimensions of screen

    private static boolean usingMacOSScreenMenubar;

    public static final String osname = System.getProperty("os.name", "");
    public static final String DEFAULT_LANGUAGE = "english";
    public static final String BLUEJ_OPENPACKAGE = "bluej.openPackage";
    public static final String bluejDebugLogName = "bluej-debuglog.txt";
    public static final String greenfootDebugLogName = "greenfoot-debuglog.txt";
    public static String debugLogName = bluejDebugLogName;
    
    private static boolean initialised = false;
    
    /** name of the icons file for the VM on Mac */
    private static String vmIconsFile;
    /** name of the VM in the dock on Mac */
    private static String vmName;
    
    protected static final int SHORTCUT_MASK =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    
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
   
    private static Color selectionColour;
    private static List debugVMArgs = new ArrayList();

    /**
     * Initialisation of BlueJ configuration. Must be called at startup.
     * This method finds and opens the configuration files.<p>
     * 
     * See also initializeVMside().
     */
    public static void initialise(File bluejLibDir, Properties tempCommandLineProps)
    {
        if(initialised)
            return;

        initialised = true;

        screenBounds = calculateScreenBounds();

        // construct paths for the configuration directories
        Config.bluejLibDir = bluejLibDir;
        Config.greenfootLibDir = new File(bluejLibDir, "greenfoot");
        
        // setup our heirarchy of property objects
        
        // top level is the system properties loaded from bluej.defs
        system_props = loadDefs("bluej.defs");
        
        // next level is the user propeties (not loaded yet)
        user_props = new Properties(system_props);
        
        // then there is the command line properties
        command_props = new Properties(user_props);
        
        // copy in all our command line properties (done first
        // incase the bluej.userHome property is one specified)
        command_props.putAll(tempCommandLineProps);
        command_props.setProperty("bluej.libdir", bluejLibDir.getAbsolutePath());
        
        // get user home directory
        {
            File userHome;
            String homeDir = getPropString("bluej.userHome", "$user.home");
            userHome = new File(homeDir);

            // get user specific bluej property directory (in user home)
            userPrefDir = new File(userHome, getBlueJPrefDirName());

            if(!userPrefDir.isDirectory()) {
                userPrefDir.mkdirs();
            }
        }

        // add user specific definitions
        loadProperties("bluej", user_props);

        // set a new name for the log file if we are running in greenfoot mode
        if(isGreenfoot()) {
            debugLogName = greenfootDebugLogName;
        }
        
        checkDebug(userPrefDir);
        
        // find our language (but default to english if none found)
        language = command_props.getProperty("bluej.language", DEFAULT_LANGUAGE);
        lang_props = loadLanguageLabels(language);

        moe_system_props = loadDefs("moe.defs");
        moe_user_props = new Properties(moe_system_props);
        loadProperties("moe", moe_user_props);  // add user specific editor definitions

        compilertype = Config.getPropString("bluej.compiler.type");
        if(compilertype.equals("internal")) {
            compilertype = "javac";
        }

        // Whether or not to use the screen menu bar on a Mac
        String macOSscreenMenuBar = Config.getPropString("bluej.macos.screenmenubar", "true");
        // The value of the BlueJ property overrides the system setting
        System.setProperty("apple.laf.useScreenMenuBar", macOSscreenMenuBar);      

        usingMacOSScreenMenubar = (isMacOS() && macOSscreenMenuBar.equals("true"));
        
        boolean themed = Config.getPropBoolean("bluej.useTheme");
        if(themed) {    
            MetalLookAndFeel.setCurrentTheme(new BlueJTheme());
        }

        String laf = Config.getPropString("bluej.lookAndFeel", "default");
        setLookAndFeel(laf);
        
        //read any debug vm args
        initDebugVMArgs();
        Config.setVMLocale();
        
        // Create a property containing the BlueJ version string
        // put it in command_props so it won't be saved to a file
        command_props.setProperty("bluej.version", Boot.BLUEJ_VERSION);
    } // initialise

    /**
     * Alternative to "initialise", to be used in the debugee-VM by
     * applications which require it (ie. greenfoot).
     */
    public static void initializeVMside(File bluejLibDir, BlueJPropStringSource propSource)
    {
        if(initialised)
            return;
    
        initialised = true;
        Config.bluejLibDir = bluejLibDir;
        Config.greenfootLibDir = new File(bluejLibDir, "greenfoot");
        Config.propSource = propSource;
        screenBounds = calculateScreenBounds();
        
        system_props = new Properties() {
          public String getProperty(String key)
          {
              return Config.propSource.getBlueJPropertyString(key, null);
          }
          
          public String getProperty(String key, String def)
          {
              return Config.propSource.getBlueJPropertyString(key, def);
          }
        };
        user_props = new Properties(system_props) {
            public Object setProperty(String key, String val) {
                String rval = getProperty(key);
                Config.propSource.setUserProperty(key, val);
                return rval;
            }
        };
        command_props = new Properties(user_props);
        
        lang_props =  new Properties() {
            public String getProperty(String key)
            {
                return Config.propSource.getLabel(key);
            }
            
            public String getProperty(String key, String def)
            {
                return Config.propSource.getLabel(key);
            }
        };

        // get user home directory
        {
            File userHome;
            String homeDir = command_props.getProperty("bluej.userHome", null);
            if(homeDir == null)
                userHome = new File(System.getProperty("user.home"));
            else
                userHome = new File(homeDir);

            // get user specific bluej property directory (in user home)
            userPrefDir = new File(userHome, getBlueJPrefDirName());

            if(!userPrefDir.isDirectory())
                userPrefDir.mkdirs();
        }

        // compiler type
        
        compilertype = Config.getPropString("bluej.compiler.type");
        if(compilertype.equals("internal"))
            compilertype = "javac";
    }
    
    public static boolean isInitialised() {
        return initialised;
    }
    
    /**
     * Set the name of icons file for the debug VM (Mac).
     */
    public static void setVMIconsName(String name)
    {
        vmIconsFile = name;
    }
    
    /**
     * Get the name of icons file for the debug VM (Mac).
     */
    public static String getVMIconsName()
    {
        return vmIconsFile;
    }
    
    /**
     * Set the name of the debug VM to appear in the dock (Mac).
     */
    public static void setVMDockName(String name)
    {
        vmName = name;
    }
    
    /**
     * Get the name of the debug VM to appear in the dock (Mac).
     */
    public static String getVMDockName()
    {
        return vmName;
    }
    
    /**
     * Tell us whether we are running on MacOS
     */
    public static boolean isMacOS()
    {
        return osname.startsWith("Mac");
    }
    
    /**
     * Tell us whether we are running on MS Windows
     */
    public static boolean isWinOS()
    {
        return osname.startsWith("Windows");
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
        if(isGreenfoot()) {
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
        if (!"true".equals(command_props.getProperty("bluej.debug"))) {
            File debugLogFile = new File(userdir, debugLogName);
            // simple diversion of output stream to a log file
            try {
                PrintStream outStream =
                    new PrintStream(new FileOutputStream(debugLogFile));
                System.setOut(outStream);
                System.setErr(outStream);
                Debug.message("BlueJ run started: " + new Date());
                Debug.message("BlueJ version " + Boot.BLUEJ_VERSION + "    Java version " + 
                                    System.getProperty("java.version"));
                Debug.message("Virtual machine: " +
                                    System.getProperty("java.vm.name") + " " +
                                    System.getProperty("java.vm.version") +
                                    " (" + System.getProperty("java.vm.vendor") + ")");
                Debug.message("Running on: " + System.getProperty("os.name") +
                                    " " + System.getProperty("os.version") +
                                    " (" + System.getProperty("os.arch") + ")");
                Debug.message("Java Home: " + System.getProperty("java.home"));            
                Debug.message("----");            
            }
            catch (IOException e) {
                Debug.reportError("Warning: Unable to create debug log file.");
            }
        }
    }

    /**
     * Called on system exit. Do whatever there is to do before exiting.
     */
    public static void handleExit()
    {
        saveProperties("bluej", "properties.heading.bluej", user_props);
        saveProperties("moe", "properties.heading.moe", moe_user_props);
    }

    /**
     * Load a BlueJ definition file. This creates a new properties object.
     * The new properties object can be returned directly, or an empty
     * properties object can be returned that has the named definitions as
     * defaults.
     *
     * @param filename  the properties file
     */
    private static Properties loadDefs(String filename)
    {
        File propsFile = new File(bluejLibDir, filename);
        Properties defs = new Properties(System.getProperties());

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
        Properties labels = loadDefs(DEFAULT_LANGUAGE + File.separator + "labels");
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
        return loadDefs(language + File.separator + "moe.help");
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
        int index;
        String str = lang_props.getProperty(strname, def);
        // remove all underscores
        while( (index = str.indexOf('_')) != -1){
            str = str.substring(0, index) + str.substring(index+1);
        }
        if ((index = str.indexOf('@')) != -1){ 
            //remove everything from @
            str = str.substring(0, index);
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
        String str = lang_props.getProperty(strname, strname);
        int index = str.indexOf('_');
        if (index == -1 || (index + 1) >= str.length()) {
            mnemonic = KeyEvent.VK_UNDEFINED;
        }
        else {
            mnemonic = getKeyCodeAt(str, index + 1);
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
        return lang_props.getProperty(strname, strname).indexOf('@') != -1;
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
        String str = lang_props.getProperty(strname, strname);
        String keyString;
        index = str.indexOf('@');
        index++;
        if(str.charAt(index) == '^') { //then the modifiers is CTRL + SHIFT
            index++;
            modifiers |= KeyEvent.SHIFT_MASK;
        }
        keyString = str.substring(index).toUpperCase();
        if(keyString.length() == 1) {
            return KeyStroke.getKeyStroke(getKeyCodeAt(keyString, 0), modifiers);
        }
        else {
            KeyStroke k1= KeyStroke.getKeyStroke(keyString);
            return KeyStroke.getKeyStroke(k1.getKeyCode(), modifiers);
        }
    }
    
    /**
     * Gets the keycode at the given position. On Java 1.5 this will be the code
     * point which can also handle supplementary characters. On previous java
     * versions it will return the BMP (Basic Multilingual Plane) character.
     * 
     * @return The keycode
     * @throws IndexOutOfBoundsException if the index argument is negative or
     *             not less than the length of this string.
     */
    private static int getKeyCodeAt(String str, int index)
    {
        // Currently the only use for this method is for retrieving mnemonics
        // and accelerator keys. I have no idea if supplementary characters will
        // ever be used for mnemonics or accelerators, but at least it should be
        // supported.
        // Poul Henriksen 11/04-2006
        int code;
        if (isJava15()) {
            // Supplementary unicode characters are only supported in Java
            // 1.5
            code = str.codePointAt(index);
        }
        else {
            // Will get the BMP (Basic Multilingual Plane) character and
            // hence will not work with supplementary characters
            code = str.charAt(index);
        }
        return code;
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
        String value = command_props.getProperty(sysID + propName);

        // if that failed, just look for the plain property value
        if(value == null) {
            value = command_props.getProperty(propName);
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
        return getPropString(strname, def, command_props);
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
        else {
            return null;
        }
    }
    
    /**
     * Get a non-language-dependent string from the BlueJ properties
     * "bluej.defs" with a default value. No variable substitution is
     * performed.
     */
    public static String getDefaultPropString(String strname, String def)
    {
        return system_props.getProperty(strname, def);
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
        return Boolean.parseBoolean(getPropString(propname, null));
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
        else {
            return Boolean.parseBoolean(propval);
        }
    }
    
    /**
     * remove a property value from the BlueJ properties.
     */
    public static String removeProperty(String propertyName)
    {
        return (String)(user_props.remove(propertyName));
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
     * Find and return the icon for an image.
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
        String path = command_props.getProperty("bluej.templatePath" , "");
        if(path.length() == 0)
            return getLanguageFile("templates/newclass");
        else
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
        String defVal = system_props.getProperty(intname);
        if (defVal != null) {
            try {
                if (value == Integer.valueOf(defVal).intValue()) {
                    user_props.remove(intname);
                }
            }
            catch (NumberFormatException nfe) { }
        }
        user_props.setProperty(intname, Integer.toString(value));
    }

    /**
     * Set a non-language dependant string for the BlueJ properties
     */
    public static void putPropString(String strname, String value)
    {
        String defVal = system_props.getProperty(strname);
        if (defVal == null || ! defVal.equals(value)) {
            user_props.setProperty(strname, value);
        }
        else {
            user_props.remove(strname);
        }
    }
    
    /**
     * Set a non-language dependent boolean value for the BlueJ properties
     */
    public static void putPropBoolean(String propname, boolean value)
    {
        String sysval = system_props.getProperty(propname);
        if (Boolean.valueOf(sysval).booleanValue() == value) {
            user_props.remove(propname);
        }
        else {
            user_props.setProperty(propname, String.valueOf(value));
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
     *
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
            // if system specified
            if(laf.equals("system")) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            else if(laf.equals("crossplatform")) {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            }
            
            // do the "default, ie. let BlueJ decide
            // Windows - System l&F, Linux & Solaris - cross-platform
            else {
                if (isWinOS()){
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
                // treat Linux and Solaris the same at the moment
                else if(isLinux() || isSolaris()) {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
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
            List splitArgs = splitVMArgs(args);
	        debugVMArgs.addAll(splitArgs);
        }        
    }
    
     /**
     * Splits VM args String into separate args including handling quotes
     * used for file paths with spaces. 
     * @param str - the string to be split
     * @returns	an array of Strings
     */
    private static List splitVMArgs(String str)
    {
        boolean inQuote = false;
        List strings = new ArrayList();
        StringTokenizer t = new StringTokenizer(str, " ", true);
        while(t.hasMoreTokens()) {
            String arg = t.nextToken();
            // if we have a "
            if(arg.indexOf("\"")!= -1) {
                inQuote = true;
                while(t.hasMoreTokens() && inQuote == true) {
                    String next = t.nextToken();
                    arg = arg + next;
                    if(next.indexOf("\"") != -1)
                        inQuote = false;
                }
                strings.add(arg); 
            }
            else if(!arg.equals(" "))
                strings.add(arg);
        }
        return strings;
    }
    
    /**
     * debug vm args used for launch of debug vm
     */
    public static List getDebugVMArgs()
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
    public static boolean isGreenfoot()
    {
        return getPropBoolean("greenfoot");
    }
}
