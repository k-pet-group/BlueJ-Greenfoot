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
 ** An Applet class role in a package, i.e. a target that is a Applet class file
 ** built from Java source code.
 **
 ** @author Bruce Quig
 **
 ** @version $Id: AppletClassRole.java 134 1999-06-21 02:34:23Z bruce $
 **/
public class AppletClassRole extends ClassRole 
{
    private RunAppletDialog dialog;

    static final String runAppletStr = Config.getString("pkgmgr.classmenu.runApplet");
    static final String htmlComment = Config.getString("pkgmgr.runApplet.htmlComment");
    static final String HTML_EXTENSION = ".html";
    static final String THIS_DIRECTORY = ".";
    static final String APPLETVIEWER_COMMAND = "appletviewer"; // move to bluej.defs
    static final String URL_PREFIX = "file:";
    



   /**
     * Save this AppletClassRole details to file
     * @param props the properties object that stores target information
     * @param prefix prefix for this target for identification
     */
    public void save(Properties props, int modifiers, String prefix)
    {
	super.save(props, modifiers, prefix);
	props.put(prefix + ".type", "AppletTarget");
    }
	

 /**
     * generates a source code skeleton for this class	
     *
     * @param template the name of the particular class template
     * @param pkg the package that the class target resides in
     * @param name the name of the class
     * @param sourceFile the name of the source file to be generated
     */
    public void generateSkeleton(Package pkg, String name, String sourceFile, boolean isAbstract, boolean isInterface)
    {
	String template = "template.applet";

	// inherited method from ClassRole
	generateSkeleton(template, pkg, name, sourceFile);
    }


    /**
     * Generate a popup menu for this AppletClassRole.
     * @param cl the class object that is represented by this target
     * @param editorFrame the frame in which this targets package is displayed
     * @return the generated JPopupMenu
     */
    protected void createMenu(JPopupMenu menu, ClassTarget ct, int state) {
	// add run applet option
	ct.addMenuItem(menu, runAppletStr, (state == Target.S_NORMAL));
	menu.addSeparator();

    }
	

    /**
     *  modified from ActionListener interface
     * 
     */
    public void actionPerformed(ActionEvent e, ClassTarget ct)
    {
	String cmd = e.getActionCommand();

	if(runAppletStr.equals(cmd)) {
	    runApplet(ct);
	}
    }
    


    /**
     * Runs the applet in the Sun JDK appletviewer.  It generates a HTML page if
     * required to house the applet.
     */
    private void runApplet(ClassTarget ct)
    {
	String name = ct.getName();
	Package pkg = ct.getPackage();

	if(dialog == null)
	    dialog = new RunAppletDialog(ct.getPackage().getFrame(), name);

	if(dialog.display()) {
	    int execOption = dialog.getAppletExecutionOption();
	    if(execOption == RunAppletDialog.GENERATE_PAGE_ONLY) {
		// generate HTML page for Applet
		String generatedFileName = chooseWebPage(pkg.getFrame());
		if(generatedFileName != null) {
		  createWebPage(name, pkg.getBaseDir(), generatedFileName);
		  Debug.message("generated name: " + generatedFileName);
		}
	    }
	    else {

		//createWebPage(htmlFile());
		Debug.message("pkg.getClassFileName(name) = " + pkg.getClassFileName(name));
		Debug.message("pkg.getClassDir() = " + pkg.getClassDir());
		Debug.message("pkg.getBaseDir() = " + pkg.getBaseDir());
		String dir =  new File(pkg.getDirName()).getAbsolutePath();
		Debug.message("absolute?: = " + dir);
		String absoluteFileName = dir + Config.slash + name + HTML_EXTENSION;
		
		createWebPage(name, ".", absoluteFileName);
		// Run applet as an external process

		String execCommand;
		String url = URL_PREFIX + Config.slash + absoluteFileName;

		Debug.message("exec option: " + execOption);	     
		Debug.message("URL: " + url);
		if(execOption == RunAppletDialog.EXEC_APPLETVIEWER) {
		    
		    try {
			execCommand = APPLETVIEWER_COMMAND	
			    + " " + url;
			    Debug.message("Command: " + execCommand);
			    ((PkgMgrFrame)pkg.getFrame()).displayMessage("Executing Applet in appletviewer");
			    Process applet = 
				Runtime.getRuntime().exec(execCommand);
		    
		    } catch (Exception e) {
			System.err.println("Exception thrown in execution of appletviewer");
		    }
		}
		else {
		    // start in Browser
		    ((PkgMgrFrame)pkg.getFrame()).displayMessage("Executing Applet in web browser");
		    //Utility.openWebBrowser( pkg.getClassFileName(name) + HTML_EXTENSION);
		    Utility.openWebBrowser(absoluteFileName);
		}
	    }
	}
    }



   /**
     * Use a file chooser to select a web page name and location.
     */
    private String chooseWebPage(JFrame frame)
    {
	JFileChooser newChooser = new JFileChooser(".");
	newChooser.setDialogTitle("Select HTML page destination");

	int result = newChooser.showSaveDialog(frame);
	String fullFileName = null;
	if (result == JFileChooser.APPROVE_OPTION) {
	    Debug.message("selected " + newChooser.getSelectedFile().getPath());
	    fullFileName = newChooser.getSelectedFile().getPath();
	}
	else if (result == JFileChooser.CANCEL_OPTION)
	    Debug.message("cancel selected");
	else {
	    Utility.showError(frame, "You must specify a valid name.");
	    return null;
	}
	return fullFileName;
    }



   /**
     * Create a HTML page that contains this JApplet using
     * parameters input by user in RunAppletDialog class.
     * @param fileName fileName for HTML file to house Applet
     */
    private void createWebPage(String appletName, String codeBase, String fileName)
    {
	String appletHeight = dialog.getAppletHeight();
	String appletWidth = dialog.getAppletWidth();
	Object[] appletParameters = dialog.getAppletParameters();
	//String codeBase = URL_PREFIX + Config.slash + pkg.getClassDir();
	//String codeBase = URL_PREFIX; // temp only
	generateHTMLSkeleton(appletName, fileName, codeBase, appletWidth, appletHeight, appletParameters);
    }

 //   /**
//      * 
//      */
//     public String htmlFile()
//     {
// 	return pkg.getFileName(name) + HTML_EXTENSION;
//     }
    
 
   /**
     * Creates a HTML Skeleton that contains this JApplet using
     * parameters input by user in RunAppletDialog class.
     */  
    public void generateHTMLSkeleton(String name, String outputFileName, String appletCodeBase, String width, String height, Object[] parameters)
    {
	Hashtable translations = new Hashtable();

	translations.put("TITLE", name);
	translations.put("COMMENT", htmlComment);		
	translations.put("CLASSFILE", name + ".class");
	translations.put("CODEBASE", appletCodeBase);
	translations.put("APPLETWIDTH", width);
	translations.put("APPLETHEIGHT", height);

	StringBuffer allParameters = new StringBuffer();
	for(int index = 0; index < parameters.length; index++)
	    allParameters.append("\t" + (String)parameters[index] + "\n");
	
	translations.put("PARAMETERS", allParameters.toString());
 			
	String filename = Config.getLibFilename("template.html");
		
	try {
	    Utility.translateFile(filename, outputFileName, translations);
	} catch(IOException e) {
	    Debug.reportError("Exception during file translation from " + filename + " to " + outputFileName);
	    e.printStackTrace();
	}
    }


   /**
     * 
     * Removes applicable files (.class, .java and .ctxt) prior to 
     * this AppletClassRole being removed from a Package.
     *
     */
    public void prepareFilesForRemoval(String sourceFile, String classFile, String contextFile)
    {
	super.prepareFilesForRemoval(sourceFile, classFile, contextFile);

	// remove associated HTML file if exists
	File htmlFileName = new File(classFile + HTML_EXTENSION);
	if (htmlFileName.exists())
	    htmlFileName.delete();
    }


  // overloads method in Target super class
    public void draw(Graphics g, ClassTarget ct, int x, int y, int width, int height)
    {
	g.setColor(ct.getTextColour());
	Utility.drawCentredText(g, 
				"www",
				x + Target.TEXT_BORDER, 
				y + height - (Target.TEXT_HEIGHT + Target.TEXT_BORDER),
				width - (2 * Target.TEXT_BORDER), Target.TEXT_HEIGHT);
    
     }

}
