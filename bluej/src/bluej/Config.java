package bluej;

import java.awt.*;
import java.io.*;

import javax.swing.*;
import bluej.utility.*;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * Class to handle application configuration for BlueJ. The configuration
 * information is spread over several files: <BR>
 * <BR>
 *  &lt;bluej_home>/lib/bluej.defs <BR>
 *  &lt;bluej_home>/lib/&lt;language>/labels	(eg "labels.english") <BR>
 *  &lt;user_home>/.bluej/bluej.properties <BR>
 * <BR>
 * "bluej.defs"	- contains system definitions which are not language
 *			  specific and not user specific. <BR>
 * "labels.&lt;language>"	- contains language specific strings <BR>
 * "bluej.properties"	- contains user specific settings. Settings here
 *			  override settings in bluej.defs <BR>
 * "moe.labels.english"- definitions for moe (the editor)
 *
 * @author Michael Cahill
 * @author Michael Kolling
 * @version $Id: Config.java 2024 2003-06-05 06:52:21Z ajp $
 */

public class Config
{
    public static final String nl = System.getProperty("line.separator");

    private static DefaultProperties bluej_props;	// bluej properties
    private static DefaultProperties lang_props;	// The internationalisation
    //  dictionary
    public static DefaultProperties moe_props;		// moe (editor) properties

    private static String bluej_conf_dirname = (File.separatorChar == '/') ?
                                               ".bluej" : "bluej";
    private static File bluej_lib_dir;
    private static File user_conf_dir;

    public static String compilertype;	// current compiler (javac, jikes)
    public static String language;	// message language (english, ...)

    public static Rectangle screenBounds; // maximum dimensions of screen

    private static boolean usingMacOSScreenMenubar;

    public static final String osname = System.getProperty("os.name", "");
    public static final String DEFAULT_LANGUAGE = "english";

    private static boolean initialised = false;
    
    /**
     * Initialisation of BlueJ configuration. Must be called at startup.
     * This method finds and opens the configuration files.
     */
    public static void initialise(File bluej_lib_dir)
    {
        if(initialised)
            return;

        initialised = true;

        screenBounds = calculateScreenBounds();

        // construct paths for the configuration directories

        Config.bluej_lib_dir = bluej_lib_dir;

        bluej_props = loadDefs("bluej.defs", true);	// system definitions

        // get user home directory
        File userHome;
        String homeDir = bluej_props.getProperty("bluej.userHome", "");
        if(homeDir.length() == 0)
            userHome = new File(System.getProperty("user.home"));
        else
            userHome = new File(homeDir);

        // get user specific bluej property directory (in user home)
        user_conf_dir = new File(userHome, bluej_conf_dirname);
        checkUserDir(user_conf_dir);

        loadProperties("bluej", bluej_props);  // add user specific definitions

        // find our language (but default to english if none found)
        language = bluej_props.getProperty("bluej.language", DEFAULT_LANGUAGE);
        lang_props = loadLanguageLabels(language);
        
        
        //lang_props = loadDefs(language + File.separator + "labels", false);

        moe_props = loadDefs("moe.defs", true);
        loadProperties("moe", moe_props);  // add user specific editor definitions
        
        checkDebug(user_conf_dir);

        compilertype = Config.getPropString("bluej.compiler.type");
        if(compilertype.equals("internal"))
            compilertype = "javac";

        String macOSscreenMenuBar = Config.getPropString("bluej.macos.screenmenubar", "true");
        System.setProperty("apple.laf.useScreenMenuBar", macOSscreenMenuBar);
        usingMacOSScreenMenubar = (osname.startsWith("Mac") && macOSscreenMenuBar.equals("true"));
        
        boolean themed = Boolean.valueOf(
            Config.getPropString("bluej.useTheme", "false")).booleanValue();
        if(themed)    
            MetalLookAndFeel.setCurrentTheme(new BlueJTheme());
            
//        try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        }
//        catch (Exception e) { }
    } // initialise

    
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
     * Check, and if necessary create, the user directory (~/.bluej)
     */
    private static void checkUserDir(File userdir)
    {
        if(!userdir.exists())
            userdir.mkdirs();
    }

    /**
     * Check whether we want to see debug information. If not, redirect it to
     * a file.
     */
    private static void checkDebug(File userdir)
    {
        if (! "on".equals(bluej_props.getProperty("debug"))) {
            File debugLogFile = new File(userdir,
                                          bluej_props.getProperty("bluej.debugLog"));
            // simple diversion of output stream to a log file
            try {
                PrintStream outStream =
                    new PrintStream(new FileOutputStream(debugLogFile));
                System.setOut(outStream);
                Debug.message("BlueJ version " + Boot.BLUEJ_VERSION);
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
        saveProperties("bluej", "properties.heading.bluej", bluej_props);
        saveProperties("moe", "properties.heading.moe", moe_props);
    }

    /**
     * Load a BlueJ definition file. This creates a new properties object.
     * The new properties object can be returned directly, or an empty
     * properties object can be returned that has the named definitions as
     * defaults.
     *
     * @param filename  the properties file
     * @param asDefault if true, the definitions are used as defaults for
     *                  an empty properties objects.
     */
    private static DefaultProperties loadDefs(String filename, boolean asDefault)
    {
        File propsFile = new File(bluej_lib_dir, filename);
        DefaultProperties defs = new DefaultProperties();

        try {
            defs.load(new FileInputStream(propsFile));
        }
        catch(Exception e) {
            Debug.reportError("Unable to load definitions file: " + propsFile);
        }

        if(asDefault)
            return new DefaultProperties(defs); // empty props with defs as defaults
        else
            return defs;
    }
    
    /**
     * Load the label property file for the currently defined language.
     * Install the default language (English) as the default properties
     * as a fallback.
     */
    private static DefaultProperties loadLanguageLabels(String language)
    {
        // add the defaults (English)
        DefaultProperties labels = loadDefs(DEFAULT_LANGUAGE + File.separator + "labels", true);
        // add localised labels if necessary...
        if(!DEFAULT_LANGUAGE.equals(language)) {
            String languageFileName = language + File.separator + "labels";
            File languageFile = new File(bluej_lib_dir, languageFileName);
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
    private static void loadProperties(String filename, DefaultProperties props)
    {
        File propsFile = new File(user_conf_dir, filename + ".properties");

        try {
            props.load(new FileInputStream(propsFile));
        }
        catch(Exception e) {
            // ignore
        }
    }

    /**
     * Save user specific (local) BlueJ properties.
     */
    private static void saveProperties(String filename, String comment, DefaultProperties props)
    {
        File propsFile = new File(user_conf_dir, filename + ".properties");

        try {
            props.store(new FileOutputStream(propsFile), getString(comment));
        }
        catch(Exception e) {
            Debug.reportError("Warning: could not save properties file " + propsFile);
        }
    }

    /**
     * Find and return the moe help definitions
     */
    public static DefaultProperties getMoeHelp()
    {
        return loadDefs(language + File.separator + "moe.help", false);
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
     * get a string from the language dependent definitions file
     * (eg. "english/labels") If not found, return default.
     */
    public static String getString(String strname, String def)
    {
        try {
            return lang_props.getProperty(strname, def);
        }
        catch(Exception e) {
            Debug.reportError("Could not get string for " + strname);
            e.printStackTrace(System.err);
            return strname;
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
        String value = bluej_props.getProperty(sysID + propName);

        // if that failed, just look for the plain property value
        if(value == null)
            value = bluej_props.getProperty(propName);

        return value;
    }


    /**
     * Get a non-language-dependent string from the BlueJ properties
     * ("bluej.defs" or "bluej.properties") with a default value
     */
    public static String getPropString(String strname, String def)
    {
        return bluej_props.getProperty(strname, def);
    }

    /**
     * Get a non-language-dependent string from the BlueJ properties
     * "bluej.defs" with a default value
     */
    public static String getDefaultPropString(String strname, String def)
    {
        try {
            return bluej_props.getDefaultProperty(strname, def);
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
            value = Integer.parseInt(bluej_props.getProperty(intname, String.valueOf(def)));
        }
        catch(NumberFormatException nfe) {
            return def;
        }
        return value;
    }

    /**
     * Get a non-language dependant integer from the BlueJ properties
     * "bluej.defs" with a default value
     */
    public static int getDefaultPropInteger(String intname, int def)
    {
        int value;
        try {
            value = Integer.parseInt(bluej_props.getDefaultProperty(intname, String.valueOf(def)));
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
        return (String)(bluej_props.remove(propertyName));
    }

    /**
     * Find and return the file name for an image.
     */
    private static File getImageFile(String propname)
    {
        String filename = bluej_props.getProperty(propname);

        if (filename != null) {
            return new File(bluej_lib_dir, "images" + File.separator + filename);
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
        String path = bluej_props.getProperty("bluej.templatePath" , "");
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
        return new File(bluej_lib_dir, language + File.separator + base);
    }

    /**
     * Return the file name for a file in the user config directory
     * (<user_home>/.bluej/<base>)
     */
    public static File getUserConfigFile(String base)
    {
        return new File(user_conf_dir, base);
    }

    /**
     * Return the user config directory
     * (<user_home>/.bluej
     */
    public static File getUserConfigDir()
    {
        return user_conf_dir;
    }

    /**
     * Return a color value from the bluej properties.
     */
    public static Color getItemColour(String itemname)
    {
        try {
            String rgbStr = bluej_props.getProperty(itemname, "255,0,255");
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
        bluej_props.setProperty(intname, Integer.toString(value));
    }

    /**
     * Set a non-language dependant string for the BlueJ properties
     */
    public static void putPropString(String strname, String value)
    {
        bluej_props.setProperty(strname, value);
    }

    /**
     * Get the Inspector directory for the system
     */
    public static File getSystemInspectorDir()
    {
        return new File(bluej_lib_dir, "inspector");
    }

    /**
     * Returns the blueJLibDir
     */
    public static File getBlueJLibDir()
    {
        return bluej_lib_dir;
    }
}
