package bluej.pkgmgr;

import bluej.Config;
import java.util.Properties;
import java.awt.Color;
import java.awt.Font;

/** 
 ** @version $Id: ImportedClassTarget.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** This target represents a class that was imported into the package from
 ** a library. For these classes, the target lives in the current package,
 ** but the source and the class of the target are in other locations.
 **/

public class ImportedClassTarget extends ClassTarget
{
    static Color abstractbg = defaultbg.brighter();
    static Color compcolour = Config.getItemColour("colour.target.bg.compiling");
    static Font normalFont = new Font("SansSerif", Font.BOLD, Config.fontsize);
    static Font invalidFont = new Font("SansSerif", Font.BOLD | Font.ITALIC, Config.fontsize);
	
    String sourcePkgDir;
    String sourcePkgName;

    /**
     * Create an ImportedClassTarget.
     *
     * @param pkg  the package this target belongs to
     * @param name  the name of the target
     * @param sourcePackageName  the name of the package this class belongs was defined in
     * @param sourcePackageDir  the directory where the class's source is stored
     */
    public ImportedClassTarget(Package pkg, String name, 
			       String sourcePkgName, String sourcePkgDir)
    {
	super(pkg, name);
	this.sourcePkgDir = sourcePkgDir;
	this.sourcePkgName = sourcePkgName;
    }

    /**
     * Create an ImportedClassTarget without specifying the details.
     * "load" should be called on this target to load the details from file.
     */
    public ImportedClassTarget(Package pkg)
    {
	this(pkg, null, null, null);
    }

    /**
     * Load the target's detail from a properties definition.
     *
     * @param props  The properties object holding the details.
     * @param prefix  An internal name of this target to identify its properties.
     */
    public void load(Properties props, String prefix) throws NumberFormatException
    {
	super.load(props, prefix);

	sourcePkgDir = Config.getPath(props, prefix + ".srcPkgDir");
	sourcePkgName = props.getProperty(prefix + ".srcPkgName");
    }

    /**
     * Save the target's details to a properties definition.
     *
     * @param props  The properties object storing the details.
     * @param prefix  An internal name of this target to identify its properties.
     */
    public void save(Properties props, String prefix)
    {
	super.save(props, prefix);

	props.put(prefix + ".type", "ImportedClassTarget"); // overwrites type from superclass
	Config.putPath(props, prefix + ".srcPkgDir", sourcePkgDir);
	props.put(prefix + ".srcPkgName", sourcePkgName);
    }

    /**
     * Copy all the files belonging to this target to a new location.
     * For imported class targets, no files need to be copied.
     *
     * @arg directory The directory to copy into
     */
    public boolean copyFiles(String directory)
    {
	// do nothing
	return true;
    }

    /**
     * Return the background colour of this target.
     */
    Color getBackgroundColour()
    {
	return isAbstract() ? abstractbg : librarybg;
    }

    /**
     * Return the full path name of the source file of this target.
     */
    public String sourceFile()
    {
	return sourcePkgDir + Config.slash + name + ".java";
    }
 
    /**
     * 
     * Removes applicable files (.class, .java and .ctxt) prior to 
     * this ClassTarget being removed from a Package.  At present there 
     * are no files.  This function overloads it's super class's method 
     * which does remove files.
     *
     */
    public void prepareFilesForRemoval()
    {
	// Nothing happens here.
    }

}
