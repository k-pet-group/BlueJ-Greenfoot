package bluej;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.*;

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
 * @version $Id: Config.java 2814 2004-07-23 04:22:20Z bquig $
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

    private static File bluejLibDir;
    private static File userPrefDir;

    public static String compilertype;	// current compiler (javac, jikes)
    public static String language;	// message language (english, ...)

    public static Rectangle screenBounds; // maximum dimensions of screen

    private static boolean usingMacOSScreenMenubar;

    public static final String osname = System.getProperty("os.name", "");
    public static final String DEFAULT_LANGUAGE = "english";
    public static final String BLUEJ_OPENPACKAGE = "bluej.openPackage";
    public static final String debugLogName = "bluej-debuglog.txt";
    
    private static boolean initialised = false;
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
     * This method finds and opens the configuration files.
     */
    public static void initialise(File bluejLibDir, Properties tempCommandLineProps)
    {
        if(initialised)
            return;

        initialised = true;

        screenBounds = calculateScreenBounds();

        // construct paths for the configuration directories
        Config.bluejLibDir = bluejLibDir;

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

        // add user specific definitions
        loadProperties("bluej", user_props);

        checkDebug(userPrefDir);
        
        // find our language (but default to english if none found)
        language = command_props.getProperty("bluej.language", DEFAULT_LANGUAGE);
        lang_props = loadLanguageLabels(language);

        moe_system_props = loadDefs("moe.defs");
        moe_user_props = new Properties(moe_system_props);
        loadProperties("moe", moe_user_props);  // add user specific editor definitions

        compilertype = Config.getPropString("bluej.compiler.type");
        if(compilertype.equals("internal"))
            compilertype = "javac";

        // Whether or not to use the screen menu bar on a Mac
        String macOSscreenMenuBar = Config.getPropString("bluej.macos.screenmenubar", "true");
        // The value of the BlueJ property overrides the system setting
        // for Java 1.3:
        System.setProperty("com.apple.macos.useScreenMenuBar", macOSscreenMenuBar);
        // and Java 1.4.1:
        System.setProperty("apple.laf.useScreenMenuBar", macOSscreenMenuBar);      

        usingMacOSScreenMenubar = (isMacOS() && macOSscreenMenuBar.equals("true"));
        
        boolean themed = Boolean.valueOf(
            Config.getPropString("bluej.useTheme", "false")).booleanValue();
        if(themed){    
            MetalLookAndFeel.setCurrentTheme(new BlueJTheme());
        }

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
        
		Config.setVMLocale();
    } // initialise

    
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
     * Tell us whether we are running on a Java VM that supports 1.5 features.
     */
    public static boolean isJava15()
    {
        return System.getProperty("java.vm.version").substring(0,3).compareTo("1.5") >= 0;
    }
    
    /**
     * Return the name of a directory within the user's home directory
     * that should be used for storing BlueJ user preferences.
     * 
     * @return The path of the preference directory relative to the user's home
     */
    private static String getBlueJPrefDirName()
    {
        if(isMacOS())
            return "Library/Preferences/org.bluej";
        else if(isWinOS())
            return "bluej";
        else
            return ".bluej";
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
        Properties defs = new Properties();

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
     * Tests if the specified object is a key in this hashtable.
     * 
     * @param   key   possible key.
     * @return  <code>true</code> if and only if the specified object 
     *          is a key, as determined by the 
     *          <tt>equals</tt> method; <code>false</code> otherwise.
     * @throws  NullPointerException  if the key is <code>null</code>.     
     */
    public static boolean systemContainsKey(String key) {
        return system_props.containsKey(key);
    }
    
    /**
     * Tests if the specified object is a key in this hashtable.
     * 
     * @param   key   possible key.
     * @return  <code>true</code> if and only if the specified object 
     *          is a key, as determined by the 
     *          <tt>equals</tt> method; <code>false</code> otherwise.
     * @throws  NullPointerException  if the key is <code>null</code>.     
     */
    public static boolean userContainsKey(String key) {
        return user_props.containsKey(key);
    }
    
    /**
     * Tests if the specified object is a key in this hashtable.
     * 
     * @param   key   possible key.
     * @return  <code>true</code> if and only if the specified object 
     *          is a key, as determined by the 
     *          <tt>equals</tt> method; <code>false</code> otherwise.
     * @throws  NullPointerException  if the key is <code>null</code>.     
     */
    public static boolean commandContainsKey(String key) {
        return command_props.containsKey(key);
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
     * Use hasMnemonicKey to ensure that the label has a mnemonicKey. If the
     * lable doesn't have a mnemomic an exception will be thrown.
     * @param strname
     * @return
     */
    public static int getMnemonicKey(String strname){
        int index;
		int mnemonic = 0;
        char ch;
        String str = lang_props.getProperty(strname, strname);
        index = str.indexOf('_');
        ch = str.charAt(index + 1);
        String s = ch + "";
        if (index == -1){
        	mnemonic = KeyEvent.VK_UNDEFINED;
        }
        else {
	        // ch is appended to the emptystring to cast the argument to a string.
	        // this is needed because of a bug in AWTKeyStroke.getAWTKeyStroke(char c)
	        mnemonic = KeyStroke.getKeyStroke(s.toUpperCase()).getKeyCode();
        }
        return mnemonic;
        
    }
    
    public static boolean hasAcceleratorKey(String strname){
        return lang_props.getProperty(strname, strname).indexOf('@') != -1;
    }
    
    
    /**
     * parses the labels file and creates a KeyStroke with the right accelerator
     * key and modifiers
     * @param strname
     * @return a KeyStroke
     */
    public static KeyStroke getAcceleratorKey(String strname){
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
            return KeyStroke.getKeyStroke(keyString.charAt(0), modifiers);
        }
        else {
            KeyStroke k1= KeyStroke.getKeyStroke(keyString);
            return KeyStroke.getKeyStroke(k1.getKeyCode(), modifiers);
        }
    }
    
    
        
    /**
     * Get a non-language-dependent string from the BlueJ properties
     * ("bluej.defs" or "bluej.properties")
     */
    public static String getPropString(String strname)
    {
        return getPropString(strname, strname);
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

        if (osname != null && osname.startsWith("Windows 9"))     // win95/98
            sysID = "win9x";
        else if (osname != null && osname.equals("Windows Me"))  // winME (same as 95/98)
            sysID = "win9x";
        else if (osname != null && osname.startsWith("Windows"))  // NT/2000/XP
            sysID = "win";
        else if (osname != null && osname.startsWith("Linux"))    // Linux
            sysID = "linux";
        else if (osname != null && osname.startsWith("SunOS"))    // Solaris
            sysID = "solaris";
        else if (osname != null && osname.startsWith("Mac"))      // MacOS
            sysID = "macos";
        else
            sysID = "";

        // try to find it using the sysId prefix
        String value = command_props.getProperty(sysID + propName);

        // if that failed, just look for the plain property value
        if(value == null)
            value = command_props.getProperty(propName);

        return value;
    }


    /**
     * Get a non-language-dependent string from the BlueJ properties
     * ("bluej.defs" or "bluej.properties") with a default value
     */
    public static String getPropString(String strname, String def)
    {
        return command_props.getProperty(strname, def);
    }

    /**
     * Get a non-language-dependent string from the BlueJ properties
     * "bluej.defs" with a default value
     */
    public static String getDefaultPropString(String strname, String def)
    {
        try {
            return system_props.getProperty(strname, def);
        }
        catch(Exception e) {
            Debug.reportError("Could not get string for " + strname);
            return def;
        }
    }

    /**
     * Get a non-language dependant integer from the BlueJ properties
     * ("bluej.defs" or "bluej.properties") with a default value
     */
    public static int getPropInteger(String intname, int def)
    {
        int value;
        try {
            value = Integer.parseInt(command_props.getProperty(intname, String.valueOf(def)));
        }
        catch(NumberFormatException nfe) {
            return def;
        }
        return value;
    }

    /**
     * Get a non-language dependant integer from the BlueJ properties
     * "bluej.defs" with a default value
     * 
     * This is used to retrieve the 
     */
    public static int getDefaultPropInteger(String intname, int def)
    {
        int value;
        try {
            value = Integer.parseInt(system_props.getProperty(intname, String.valueOf(def)));
        }
        catch(NumberFormatException nfe) {
            return def;
        }
        return value;
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
        String filename = command_props.getProperty(propname);

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
            java.net.URL u = getImageFile(propname).toURL();

            return new ImageIcon(u);
        }
        catch (java.net.MalformedURLException mue) { }
        catch (NullPointerException npe) { }
        return null;
    }

    /**
     * Find the path to an executable command that may be located
     * in the JDK bin directory
     *
     * The logic goes like this, some tools such as javac, appletviewer
     * etc should be run from the same bin directory as the JDK that
     * launched bluej (rather than the first one in the path which may
     * be of a different version). So for all these properties, if the
     * property DOES NOT exist, we try to locate the executable in the
     * JDK directory and if we can't find it we use just the command
     * name.
     * If the property DOES exist we return it and it will be resolved
     * by the Runtime.exec call (ie looked for in the current path if
     * the command name is not an absolute path)
     *
     * This method never returns null (at the very least it returns the
     * executableName)
     */
    public static String getJDKExecutablePath(String propName, String executableName)
    {
        if (executableName == null)
            throw new IllegalArgumentException("must provide an executable name");

        String p = getSystemPropString(propName);

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
            String rgbStr = command_props.getProperty(itemname, "255,0,255");
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
        if(selectionColour == null)
            selectionColour = Config.getItemColour("colour.selection");
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
        user_props.setProperty(intname, Integer.toString(value));
    }

    /**
     * Set a non-language dependant string for the BlueJ properties
     */
    public static void putPropString(String strname, String value)
    {
        user_props.setProperty(strname, value);
    }
    
    /**
     * Get the Inspector directory for the system
     */
    public static File getSystemInspectorDir()
    {
        return new File(bluejLibDir, "inspector");
    }

    /**
     * Returns the blueJLibDir
     */
    public static File getBlueJLibDir()
    {
        return bluejLibDir;
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
        
        // something has been be specified...
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
    
    public static List getDebugVMArgs()
    {
        return debugVMArgs;
    }
}
