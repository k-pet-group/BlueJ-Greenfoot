package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

/** 
 ** A class role in a class target, providing behaviour specific to 
 ** particular class types 
 ** 
 ** @author Bruce Quig
 ** @version $Id: ClassRole.java 149 1999-06-30 01:22:09Z bruce $
 **
 **/
public abstract class ClassRole
{

    /**
     * save details about the class target variant this
     * role represents.
     *
     * @param props the properties object associated with this target and role 
     * @param modifiers modifiers for 
     * @param prefix prefix to identifiy this role's target
     *
     */
    public void save(Properties props, int modifiers, String prefix)
    {
	props.put(prefix + ".modifiers", Integer.toString(modifiers, 16));
    }


    /**
     * load existing information about this class role
     * @param props the properties object to read
     * @param prefix an internal name used for this target to identify 
     * its properties in a properties file used by multiple targets.
     */
    public abstract void load(Properties props, String prefix) throws NumberFormatException;




    /**
     * abstract class that generates source code skeleton for class
     *
     * @param pkg the package that the class target belongs to 
     * @param name name of the class
     * @param sourceFile full name of the source file to be created
     * @param isAbstract boolean value that is true if class is abstract
     * @param isInterface boolean value that is true if class is an interface 
     *
     */
    public abstract void generateSkeleton(Package pkg, String name, String sourceFile, 
					  boolean isAbstract, boolean isInterface);



    /**
     * generates a source code skeleton for this class	
     *
     * @param template the name of the particular class template
     * @param pkg the package that the class target resides in
     * @param name the name of the class
     * @param sourceFile the name of the source file to be generated
     */
    public void generateSkeleton(String template, Package pkg, String name, String sourceFile )
    {
	Hashtable translations = new Hashtable();
	translations.put("CLASSNAME", name);
		
	String pkgname = pkg.getName();
	if((pkgname == Package.noPackage))
	    translations.put("PKGLINE", "");
	else
	    translations.put("PKGLINE", "package " + pkgname + ";" + Config.nl + Config.nl);
			
	String filename = Config.getLibFilename(template);
		
	try {
	    Utility.translateFile(filename, sourceFile, translations);
	} catch(IOException e) {
	    Utility.showError(pkg.getFrame(), 
			      "The default skeleton for the class could not be\n" +
			      "generated. This may be because of configuration\n" +
			      "error in the setup of BlueJ, or because of file\n" +
			      "system access problems.");
	    Debug.reportError("The default skeleton for the class could not be generated");
	    e.printStackTrace();
	}
		
	//setState(Target.S_INVALID);
    }

    
    /**
     * adds role specific items to the popup menu for this class target.
     *
     * @param menu the menu object to add to
     * @param ct ClassTarget object associated with this class role
     * @param state the state of the ClassTarget
     *
     * @return the created popup menu object
     */	
    protected abstract void createMenu(JPopupMenu menu, ClassTarget ct, int state);


    /**
     *  modified from ActionListener interface
     * 
     */
    public abstract void actionPerformed(ActionEvent e, ClassTarget ct);



   /**
     * 
     * Removes applicable files (.class, .java and .ctxt) prior to 
     * this ClassRole being removed from a Package.
     *
     */
    public void prepareFilesForRemoval(String sourceFile, String classFile, String contextFile)
    {
	File sourceFileName = new File(sourceFile);
	if (sourceFileName.exists())
	    sourceFileName.delete(); 

	File classFileName = new File(classFile); 
	if (classFileName.exists())
	    classFileName.delete();
 	
	File contextFileName = new File(contextFile);
	if (contextFileName.exists())
	    contextFileName.delete(); 
    }

    /**
     *  Draw this target, including its box, border, shadow and text.
     */
    public abstract void draw(Graphics g, ClassTarget ct, int x, int y, int width, int height);

}
