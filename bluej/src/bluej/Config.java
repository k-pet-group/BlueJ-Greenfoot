package bluej;

import bluej.utility.Debug;
import bluej.utility.Utility;

import java.awt.Color;
import java.awt.event.KeyEvent;
import javax.swing.filechooser.FileSystemView;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.Keymap;
import javax.swing.*;
import javax.swing.border.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
** @version $Id: Config.java 278 1999-11-16 00:58:12Z ajp $
** @author Michael Cahill
** @author Michael Kolling
**
** Class to handle application configuration for BlueJ. The configuration
** information is spread over several files:
**
**  <bluej_home>/bluej.defs
**  <bluej_home>/labels.<language>	(eg "labels.english")
**  <user_home>/.bluej/bluej.properties
**  <bluej_home>/moe.labels.<language>
**
** "bluej.defs"	- contains system definitions which are not language 
**			  specific and not user specific.
** "labels.<language>"	- contains language specific strings
** "bluej.properties"	- contains user specific settings. Settings here
**			  override settings in bluej.defs
** "moe.labels.english"- definitions for moe (the editor)
**/

public class Config
{
    public static final String nl = System.getProperty("line.separator");
    public static final char slash = File.separatorChar;
    public static final char colon = File.pathSeparatorChar;
    public static final String slashstring = File.separator;
    public static final String colonstring = File.pathSeparator;

    private static Properties bluej_props;	// bluej properties
    private static Properties lang_props;	// The internationalisation
    //  dictionary
    public static Properties moe_props;		// moe (editor) properties
    private static String dirname = (slash == '/') ? ".bluej" : "bluej";
    private static String bluej_home;
    private static String sys_confdir;
    private static String user_confdir;
    public static int fontsize;
    public static int editFontsize;
    public static int printFontsize;
    public static int printTitleFontsize;
    public static int printInfoFontsize;

    public static String compilertype;	// current compiler (javac, jikes)
    public static String language;	// message language (english, ...)

    // Swing JSplitPane divider width constant for uniform look and feel
    public static final int splitPaneDividerWidth = 3;
    // Other general spacing constants. We should try to use these for consistency
    public static final int generalSpacingWidth = 5;
    public static final Border generalBorder = BorderFactory.createEmptyBorder(10,10,10,10);
    public static final Border generalBorderWithStatusBar = BorderFactory.createEmptyBorder(10,10,0,10);

    private static boolean initialised = false;

    /*
     * Default behaviour for JTextFields is to generate an ActionEvent when
     * "Enter" is pressed. We don't want that. Here, we remove the Enter key 
     * from the keymap used by all JTextFields. Then we can use the default
     * button for dialogs (Enter will then activate the default button).
     */
    static {
        JTextField f = new JTextField();
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        Keymap map = f.getKeymap();
        map.removeKeyStrokeBinding(enter);
    }


    /**
     * Initialisation of BlueJ configuration. Must be called at startup.
     * This method finds and opens the configuration files.
     */
    public static void initialise(String bluej_home)
    {
        if(initialised)
            return;

        initialised = true;

        // construct paths for the configuration directories

        Config.bluej_home = bluej_home;
        sys_confdir = bluej_home + slash + "lib";
        user_confdir = FileSystemView.getFileSystemView().
            getHomeDirectory().getAbsolutePath() + slash + dirname;

        checkUserDir(user_confdir);

        bluej_props = loadDefs("bluej.defs", true);	// system definitions
        loadProperties("bluej", bluej_props);	// user specific definitions

        language = bluej_props.getProperty("bluej.language");
        lang_props = loadDefs("labels." + language, false);

        moe_props = loadDefs("moe.labels." + language, false);

        fontsize = Integer.parseInt(bluej_props.getProperty("bluej.fontsize","12"));
        editFontsize = Integer.parseInt(bluej_props.getProperty("bluej.fontsize.editor","12"));
        printFontsize = Integer.parseInt(bluej_props.getProperty("bluej.fontsize.printText","10"));
        printTitleFontsize = Integer.parseInt(bluej_props.getProperty("bluej.fontsize.printTitle","14"));
        printInfoFontsize = Integer.parseInt(bluej_props.getProperty("bluej.fontsize.printInfo","10"));
        checkDebug(user_confdir);
        compilertype = Config.getPropString("bluej.compiler.type");
        if(compilertype.equals("internal"))
            compilertype = "javac";

    } // initialise

    /**
     * Check, and if necessary create, the user directory (~/.bluej)
     */
    private static void checkUserDir(String userdir)
    {
        File dir = new File(userdir);
        if(!dir.exists())
            dir.mkdirs();
    }

    /**
     * Check whether we want to see debug information. If not, redirect it to
     * a file.
     */
    private static void checkDebug(String userdir)
    {
        if (! "on".equals(bluej_props.getProperty("debug"))) {
            String debugLogFileName = userdir + slash +
                bluej_props.getProperty("bluej.debugLog");
            // simple diversion of output stream to a log file
            try {
                PrintStream outStream = 
                    new PrintStream(new FileOutputStream(debugLogFileName));
                System.setOut(outStream);
                Debug.message("BlueJ version " + Main.BLUEJ_VERSION);
            } catch (IOException e) {
                Debug.reportError("Warning: Unable to create debug log file.");
            } 
        }
    }

    /**
     * Called on system exit. Do whatever there is to do before exiting.
     */
    public static void handleExit()
    {
        saveProperties("bluej", bluej_props);
    }

    /**
     * Load a BlueJ definition file. This creates a new properties object.
     * The new properties object can be returned directly, or an empty
     * properties object can be returned that has the named definitions as
     * defaults.
     *
     * @param filename	the properties file
     * @param asDefault	if true, the definitions are used as defaults for
     *			an empty properties objects.
     */
    private static Properties loadDefs(String filename, boolean asDefault)
    {
        String fullname = sys_confdir + slash + filename;
        Properties defs = new Properties();

        try {
            FileInputStream input = new FileInputStream(fullname);
            defs.load(input);
        } catch(Exception e) {
            Debug.reportError("Unable to load definitions file: "+fullname);
        }

        if(asDefault)
            return new Properties(defs); // empty props with defs as defaults
        else
            return defs;
    }

    /**
     * Load local BlueJ properties. The properties definitions override
     * the defaults found in the definitions file.
     **/
    private static void loadProperties(String filename, Properties props)
    {
        String fullname = user_confdir + slash + filename + ".properties";

        try {
            FileInputStream input = new FileInputStream(fullname);
            props.load(input);
        } catch(Exception e) {
            // ignore
        }
    }

    /**
     * Save user specific (local) BlueJ properties.
     */
    private static void saveProperties(String filename, Properties props)
    {
        String fullname = user_confdir + slash + filename + ".properties";
        try {
            FileOutputStream output = new FileOutputStream(fullname);
            props.store(output, getString("properties.heading"));
        } catch(Exception e) {
            Debug.reportError("Warning: could not save properties file " + 
                              fullname);
        }
    }

    /**
     * find and return the moe help definitions
     */
    public static Properties getMoeHelp()
    {
        return loadDefs("moe.help." + language, false);
    }

    /**
     * get a string from the language dependent definitions file
     * (eg. "english.defs")
     */
    public static String getString(String strname)
    {
        try {
            return lang_props.getProperty(strname, strname);
        } catch(Exception e) {
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
     * Get a non-language-dependent string from the BlueJ properties
     * ("bluej.defs" or "bluej.properties") with a default value
     */
    public static String getPropString(String strname, String def)
    {
        try {
            return bluej_props.getProperty(strname, def);
        } catch(Exception e) {
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
            value = new Integer(bluej_props.getProperty(intname, String.valueOf(def))).intValue();
        }
        catch(Exception e) {
            Debug.reportError("Could not get integer for " + intname);
            return def;
        }
        return value;
    }



    public static String getLibFilename(String propname)
    {
        try {
            String filename = bluej_props.getProperty(propname);
            return bluej_home + slash + "lib" + slash + filename;
        } catch(Exception e) {
            Debug.reportError("Could not get library name: " + propname);
            return null;
        }
    }

    /**
     * Find and return the file name for an image.
     */
    public static String getImageFilename(String propname)
    {
        try {
            String filename = bluej_props.getProperty(propname);
            return bluej_home + slash + "images" + slash + filename;
        } catch(Exception e) {
            Debug.reportError("Could not find image: " + propname);
            return null;
        }
    }

    /**
     * Find and return the file name for a help file (eg.
     * "bluej/lib/javac.help.english")
     */
    public static String getHelpFilename(String base)
    {
        return sys_confdir + slash + base + ".help." + language;
    }

    /**
     * Return the file name for a file with language specific text (eg.
     * "bluej/lib/dialogs.english")
     */
    public static String getLanguageFilename(String base)
    {
        return sys_confdir + slash + base + "." + language;
    }

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
        } catch(Exception e) {
            Debug.reportError("Could not get colour for " + itemname);
        }

        return null;
    }

    public static String getPath(String propname)
    {
        return getPath(bluej_props, propname, null);
    }

    public static String getPath(String propname, String defpath)
    {
        return getPath(bluej_props, propname, defpath);
    }

    public static String getPath(Properties props, String propname)
    {
        return getPath(props, propname, null);
    }

    public static String getPath(Properties props, String propname, String defPath)
    {
        String canonPath = props.getProperty(propname, defPath);

        if(canonPath == null)
            return null;

        String path = canonPath.replace('/', slash);
        path = path.replace(';', colon);
        return path;
    }

    public static void putPath(String propname, String path)
    {
        putPath(bluej_props, propname, path);
    }

    public static void putPath(Properties props, String propname, String path)
    {
        path = path.replace(slash, '/');
        path = path.replace(colon, ';');
        props.put(propname, path);
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
    
    public static String getSystemConfigDir() {
        return sys_confdir;
    }

    public static String getUserConfigDir() {
        return user_confdir;
    }
}

