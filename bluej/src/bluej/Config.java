package bluej;

import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.DefaultProperties;

import java.awt.Color;
import java.awt.event.KeyEvent;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.JTextComponent;
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

import java.awt.*;

/**
 * Class to handle application configuration for BlueJ. The configuration
 * information is spread over several files: <BR>
 * <BR>
 *  &lt;bluej_home>/lib/bluej.defs <BR>
 *  &lt;bluej_home>/lib/labels.&lt;language>	(eg "labels.english") <BR>
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
 * @version $Id: Config.java 871 2001-04-26 00:56:38Z mik $
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

    // Image to be used for setting the frames icon
    public static Image frameImage = null;

    // Swing JSplitPane divider width constant for uniform look and feel
    public static final int splitPaneDividerWidth = 3;
    // Other general spacing constants. We should try to use these for consistency
    public static final int generalSpacingWidth = 5;
    public static final Border generalBorder =
        BorderFactory.createEmptyBorder(10,10,10,10);
    public static final Border generalBorderWithStatusBar =
        BorderFactory.createEmptyBorder(10,10,0,10);

    public static final Border dialogBorder =
        BorderFactory.createEmptyBorder(12,12,11,11);

    public static final int commandButtonSpacing = 5;
    public static final int commandButtonPadding = 12;

    public static final int componentSpacingSmall = 5;
    public static final int componentSpacingLarge = 11;

    public static final int dialogCommandButtonsVertical = 17;

    private static boolean initialised = false;

    /*
     * Default behaviour for JTextFields is to generate an ActionEvent when
     * "Enter" is pressed. We don't want that. Here, we remove the Enter key
     * from the keymap used by all JTextFields. Then we can use the default
     * button for dialogs (Enter will then activate the default button).
     *
     * NOT NEEDED ANYMORE FOR JDK 1.3 - remove once 1.2 is out of fashion.
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
            userHome = FileSystemView.getFileSystemView().getHomeDirectory();
        else
            userHome = new File(homeDir);

        // get user specific bluej property directory (in user home)
        user_conf_dir = new File(userHome, bluej_conf_dirname);
        checkUserDir(user_conf_dir);

        loadProperties("bluej", bluej_props);  // add user specific definitions

        // find our language (but default to english if none found)
        language = bluej_props.getProperty("bluej.language", "english");
        lang_props = loadDefs(language + File.separator + "labels", false);

        moe_props = loadDefs("moe.defs", false);

        ImageIcon ii = Config.getImageAsIcon("image.icon");
        if(ii != null)
            frameImage = ii.getImage();

        checkDebug(user_conf_dir);

        compilertype = Config.getPropString("bluej.compiler.type");
        if(compilertype.equals("internal"))
            compilertype = "javac";

        // set system look and feel. experimental at this stage.
//         try {
//             UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//         }
//         catch (Exception exc) {
//             Debug.reportError("Could not set system look-and-feel");
//         }

    } // initialise

    /**
     * Get the screen size information
     */
    private static Rectangle calculateScreenBounds()
    {
        //         Rectangle bounds = new Rectangle();
        //         GraphicsEnvironment ge = GraphicsEnvironment.
        //             getLocalGraphicsEnvironment();
        //         GraphicsDevice[] gs = ge.getScreenDevices();
        //         for (int j = 0; j < gs.length; j++) {
        //             GraphicsDevice gd = gs[j];
        //             GraphicsConfiguration[] gc = gd.getConfigurations();
        //             for (int i=0; i < gc.length; i++)
        //                 bounds = bounds.union(gc[i].getBounds());
        //         }
        // for jdk 1.2:
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
                Debug.message("BlueJ version " + Main.BLUEJ_VERSION);
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
        saveProperties("bluej", bluej_props);
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
    private static void saveProperties(String filename, DefaultProperties props)
    {
        File propsFile = new File(user_conf_dir, filename + ".properties");

        try {
            props.store(new FileOutputStream(propsFile),
                        getString("properties.heading"));

        }
        catch(Exception e) {
            Debug.reportError("Warning: could not save properties file " +
                              propsFile);
        }
    }

    /**
     * find and return the moe help definitions
     */
    public static DefaultProperties getMoeHelp()
    {
        return loadDefs(language + File.separator + "moe.help", false);
    }

    /**
     * get a string from the language dependent definitions file
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
     * ("bluej.defs" or "bluej.properties"). System-dependent strings
     * start with an OS ID prefix.
     */
    public static String getSystemPropString(String propName)
    {
        String osname = System.getProperty("os.name");
        //Debug.message(osname);
        String sysID;

        if(osname != null && osname.startsWith("Windows 9"))     // win95/98
            sysID = "win9x";
        else if(osname != null && osname.startsWith("Windows"))  // NT/2000
            sysID = "win";
        else if(osname != null && osname.startsWith("Linux"))    // Linux
            sysID = "linux";
        else if(osname != null && osname.startsWith("SunOS"))    // Solaris
            sysID = "solaris";
        else if(osname != null && osname.startsWith("Mac"))      // MacOS
            sysID = "macos";
        else
            sysID = "";

        String value = bluej_props.getProperty(sysID + propName);
        if(value == null)
            value = bluej_props.getProperty(propName, "");

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

    public static String removeProperty(String propertyName)
    {
        return (String)(bluej_props.remove(propertyName));
    }

    public static File getLibFile(String propname)
    {
        try {
            String filename = bluej_props.getProperty(propname);
            return new File(bluej_lib_dir, filename);
        }
        catch(Exception e) {
            Debug.reportError("Could not get library name: " + propname);
            return null;
        }
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
     * Return the file with language specific text (eg.
     * "bluej/lib/english/dialogs" if base is dialogs)
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

}

