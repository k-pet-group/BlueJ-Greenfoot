package bluej;

import bluej.utility.Debug;
import bluej.utility.Utility;

import java.awt.Color;
import java.awt.event.KeyEvent;
import javax.swing.filechooser.FileSystemView;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.Keymap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 ** @version $Id: Config.java 63 1999-05-04 00:03:10Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** Class to handle application configuration for BlueJ. The configuration
 ** information is spread over three files:
 **
 **  <bluej_home>/bluej.defs
 **  <bluej_home>/<language>.defs	(eg "english.defs")
 **  <user_home>/.bluej/bluej.properties
 **
 ** "bluej.defs"	- contains system definitions which are not language 
 **			specific and not user specific.
 ** "<language>.defs"	- contains language specific strings
 ** "bluej.properties"	- contains user specific settings. Settings here
 **			override settings in bluej.defs
 **/

public class Config
{
    public static final String nl = System.getProperty("line.separator");
    public static final char slash = File.separatorChar;
    public static final char colon = File.pathSeparatorChar;

    public static final String syslibs_file = "syslibs.properties";
    public static final String standardClasses = "classes.zip";

    public static String Version;

    private static Properties props;		// bluej properties
    private static Properties lang;		// The internationalisation
						//  dictionary
    private static String dirname = (slash == '/') ? ".bluej" : "bluej";
    private static String bluej_home;
    private static String sys_confdir;
    private static String user_confdir;
    public static int fontsize;
    public static int editFontsize;
    public static int printFontsize;
    public static int printTitleFontsize;
    public static int printInfoFontsize;
	
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

	props = loadDefs("bluej");		// system definitions
	loadProperties("bluej", props);		// user specific definitions

	String langDefs = props.getProperty("bluej.language");
	lang = loadDefs(langDefs);

	Version = getString("main.version");
	fontsize = Integer.parseInt(props.getProperty("bluej.fontsize","12"));
	editFontsize = Integer.parseInt(props.getProperty("bluej.fontsize.editor","12"));
	printFontsize = Integer.parseInt(props.getProperty("bluej.fontsize.printText","10"));
	printTitleFontsize = Integer.parseInt(props.getProperty("bluej.fontsize.printTitle","14"));
	printInfoFontsize = Integer.parseInt(props.getProperty("bluej.fontsize.printInfo","10"));
	checkDebug(user_confdir);

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
	if (! "on".equals(props.getProperty("debug"))) {
	    String debugLogFileName = userdir + slash +
				      props.getProperty("bluej.debugLog");
	    // simple diversion of output stream to a log file
	    try {
		PrintStream outStream = 
		    new PrintStream(new FileOutputStream(debugLogFileName));
		System.setOut(outStream);
		System.setErr(outStream);
		Debug.message("BlueJ " + Version);
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
	saveProperties("bluej", props);
    }
	
    /**
     * Load global BlueJ definitions. This creates a properties object
     * that has the definitions as defaults.
     */
    private static Properties loadDefs(String filename)
    {
	String fullname = sys_confdir + slash + filename + ".defs";
	Properties defs = new Properties();
		
	try {
	    FileInputStream input = new FileInputStream(fullname);

	    defs.load(input);
	} catch(Exception e) {
	    Debug.reportError("Unable to load definitions file: "+fullname);
	}
		
	return new Properties(defs); // empty props with defs as defaults
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
	    props.save(output, getString("properties.heading"));
	} catch(Exception e) {
	    Debug.reportError("Warning: could not save properties file " + 
				fullname);
	}
    }
	
    /**
     * get a string from the language dependent definitions file
     * (eg. "english.defs")
     */
    public static String getString(String strname)
    {
	try {
	    return lang.getProperty(strname, strname);
	} catch(Exception e) {
	    System.err.println("Something went wrong trying to getString for " + strname);
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
	try {
	    return props.getProperty(strname, strname);
	} catch(Exception e) {
	    System.err.println("Something went wrong trying to getString for " + strname);
	    e.printStackTrace(System.err);
	    return strname;
	}
    }

    public static String getLibFilename(String propname)
    {
	try {
	    String filename = props.getProperty(propname);
		
	    return bluej_home + slash + "lib" + slash + filename;
	} catch(Exception e) {
	    System.err.println("Something went wrong trying to getLibFilename for " + propname);
	    e.printStackTrace(System.err);
	    return null;
	}
    }
	
    public static String getImageFilename(String propname)
    {
	try {
	    String filename = props.getProperty(propname);
		
	    String fullfile = bluej_home + slash + "images" + slash + filename;
	    // Debug.message("getImageFilename(" + propname + "): returning " + fullfile);
	    return fullfile;
	} catch(Exception e) {
	    System.err.println("Something went wrong trying to getImageFilename for " + propname);
	    e.printStackTrace(System.err);
	    return null;
	}
    }
	
    public static Color getItemColour(String itemname)
    {
	try {
	    String rgbStr = props.getProperty(itemname, "255,0,255");
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
	    System.err.println("Something went wrong trying to getItemColour for " + itemname);
	    e.printStackTrace(System.err);
	}
		
	return null;
    }
	
    public static String getPath(String propname)
    {
	return getPath(props, propname, null);
    }
	
    public static String getPath(String propname, String defpath)
    {
	return getPath(props, propname, defpath);
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
	putPath(props, propname, path);
    }
	
    public static void putPath(Properties props, String propname, String path)
    {
	path = path.replace(slash, '/');
	path = path.replace(colon, ';');
	props.put(propname, path);
    }

    public static String getSystemConfigDir() {
	return sys_confdir;
    }
	
    public static String getUserConfigDir() {
	return user_confdir;
    }
}

