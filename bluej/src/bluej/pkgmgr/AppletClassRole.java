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
 ** @version $Id: AppletClassRole.java 169 1999-07-08 02:02:13Z mik $
 **/
public class AppletClassRole extends ClassRole 
{
    private RunAppletDialog dialog;

    static final String runAppletStr = Config.getString("pkgmgr.classmenu.runApplet");
    static final String htmlComment = Config.getString("pkgmgr.runApplet.htmlComment");
    static final String htmlType = Config.getPropString("bluej.applet.jvm");
    static final String appletType = Config.getPropString("bluej.applet.type");

    static final String HTML_EXTENSION = ".html";
    static final String THIS_DIRECTORY = ".";
    static final String APPLETVIEWER_COMMAND = "appletviewer"; // move to bluej.defs
    static final String URL_PREFIX = "file:";
    
    private String[] appletParams;
    private int appletHeight;
    private int appletWidth;


   /**
     * Save this AppletClassRole details to file
     * @param props the properties object that stores target information
     * @param prefix prefix for this target for identification
     */
    public void save(Properties props, int modifiers, String prefix)
    {
	super.save(props, modifiers, prefix);
	props.put(prefix + ".type", "AppletTarget");
	if(dialog != null) {
	    appletParams = dialog.getAppletParameters();
	    props.put(prefix + ".numberAppletParameters", String.valueOf(appletParams.length));
	    for(int i = 0; i < appletParams.length; i++) {
		props.put(prefix + ".appletParameter" + (i + 1), appletParams[i]);
	    }

	}
	else
	    props.put(prefix + ".numberAppletParameters", String.valueOf(0));

	props.put(prefix + ".appletHeight", String.valueOf(appletHeight));
	props.put(prefix + ".appletWidth", String.valueOf(appletWidth));
    }
	

    /**
     * load existing information about this applet class role
     * @param props the properties object to read
     * @param prefix an internal name used for this target to identify 
     * its properties in a properties file used by multiple targets.
     */
    public void load(Properties props, String prefix) throws NumberFormatException
    {
	String value = props.getProperty(prefix + ".numberAppletParameters");

	int numberParameters = 0;
	if(value != null)
	    numberParameters = Integer.parseInt(value);
	if(numberParameters > 0) {
	    appletParams = new String[numberParameters];
	    for(int i = 0; i < numberParameters; i++) 
		appletParams[i] = props.getProperty(prefix + ".appletParameter" + (i + 1));
	}

	value = props.getProperty(prefix + ".appletHeight");
	if(value != null)
	    appletHeight = Integer.parseInt(value);

	value = props.getProperty(prefix + ".appletWidth");
	if(value != null)
	    appletWidth = Integer.parseInt(value);

    }



    /**
     * generates a source code skeleton for this class	
     *
     * @param template the name of the particular class template
     * @param pkg the package that the class target resides in
     * @param name the name of the class
     * @param sourceFile the name of the source file to be generated
     */
    public void generateSkeleton(Package pkg, String name, String sourceFile,
				 boolean isAbstract, boolean isInterface)
    {
	String template;
	//Debug.message("applet type = " + appletType);

	if(appletType.equals("japplet"))
	    template = "template.japplet";
	else
	    template = "template.applet";

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

	if(runAppletStr.equals(cmd))
	    runApplet(ct);
	
    }
    

    /**
     * Runs the applet using options provided by user RunAppletDialog dialog
     * choices.  Options are:
     *     - generate a package independent HTML page only
     *     - run applet in JDK appletviewer
     *     _ run in web browser
     * 
     * @param ct the class target that is represented by this applet class
     *
     */
    private void runApplet(ClassTarget ct)
    {
	String name = ct.getName();
	Package pkg = ct.getPackage();

	if(dialog == null) {
	    dialog = new RunAppletDialog(ct.getPackage().getFrame(), name);
	    // add params that originated from pkg properties
	    if(appletParams != null)
		dialog.setAppletParameters(appletParams);
	    if(appletHeight != 0)
		dialog.setAppletHeight(appletHeight);
	    if(appletWidth !=0)
		dialog.setAppletWidth(appletWidth);
	}
	if(dialog.display()) {
	    int execOption = dialog.getAppletExecutionOption();
	    if(execOption == RunAppletDialog.GENERATE_PAGE_ONLY) {
		// generate HTML page for Applet using selected path and file name
		String generatedFileName = chooseWebPage(pkg.getFrame());
		if(generatedFileName != null)
		  createWebPage(name, pkg.getBaseDir(), generatedFileName);
	    }
	    else {

		String dir =  new File(pkg.getDirName()).getAbsolutePath();
		String absoluteFileName = dir + Config.slash + name + HTML_EXTENSION;
		
		createWebPage(name, ".", absoluteFileName);
	
		// Run applet as an external process
		String url = URL_PREFIX + absoluteFileName;

		if(execOption == RunAppletDialog.EXEC_APPLETVIEWER) {
		    try {
			String[] execCommand = {APPLETVIEWER_COMMAND, url};
			((PkgMgrFrame)pkg.getFrame()).displayMessage("Executing Applet in appletviewer");
			Process applet = 
			    Runtime.getRuntime().exec(execCommand);
		    } catch (Exception e) {
			Utility.showError(pkg.getFrame(), "Error executing applet in appletviewer.");	
			Debug.reportError("Exception thrown in execution of appletviewer");
			e.printStackTrace();
		    }
		}
		else {
		    // start in Browser
		    ((PkgMgrFrame)pkg.getFrame()).displayMessage("Executing Applet in web browser");
		    Utility.openWebBrowser(absoluteFileName);
		}
	    }
	}
    }



    /**
     * Use a file chooser to select a web page name and location.
     * 
     * @param frame the parent frame for the file chooser
     * @return the full file name for the web page or null
     *         if cancel selected in file chooser 
     */
    private String chooseWebPage(JFrame frame)
    {
	JFileChooser newChooser = new JFileChooser(".");
	newChooser.setDialogTitle("Select HTML page destination");

	int result = newChooser.showSaveDialog(frame);
	String fullFileName = null;
	if (result == JFileChooser.APPROVE_OPTION) {
	    //Debug.message("selected " + newChooser.getSelectedFile().getPath());
	    fullFileName = newChooser.getSelectedFile().getPath();
	}
	else if (result != JFileChooser.CANCEL_OPTION)
	    Utility.showError(frame, "You must specify a valid name.");
	
 	return fullFileName;
    }



  /**
     * Create a HTML page that contains this JApplet using
     * parameters input by user in RunAppletDialog class.
     *
     * @param fileName fileName for HTML file to house Applet
     */
    private void updateAppletProperties()
    {
	try{
	    appletHeight = Integer.parseInt(dialog.getAppletHeight());
	    appletWidth = Integer.parseInt(dialog.getAppletWidth());
	} catch (NumberFormatException nfe) {
	    // add exception handling 
	}
	appletParams = dialog.getAppletParameters();
    }



   /**
     * Create a HTML page that contains this JApplet using
     * parameters input by user in RunAppletDialog class.
     *
     * @param fileName fileName for HTML file to house Applet
     */
    private void createWebPage(String appletName, String codeBase, String fileName)
    {
	updateAppletProperties();
	generateHTMLSkeleton(appletName, fileName, codeBase, dialog.getAppletWidth(), dialog.getAppletHeight(), appletParams);
    }


 
   /**
     * Creates a HTML Skeleton that contains this JApplet using
     * parameters input by user in RunAppletDialog class.
     *
     * @param name the name of the applet class
     * @param outputFileName the name of the generated HTML file
     * @param appletCodeBase code base to be included in applet tag if not null
     * @param width specified width of applet
     * @param height specified height of applet
     * @param parameters optional applet parameters
     *
     */  
    private void generateHTMLSkeleton(String name, String outputFileName, String appletCodeBase, String width, String height, String[] parameters)
    {
	Hashtable translations = new Hashtable();

	translations.put("TITLE", name);
	translations.put("COMMENT", htmlComment);		
	translations.put("CLASSFILE", name + ".class");
	// check for optional codebase tag
	if(appletCodeBase != null)
	    translations.put("CODEBASE", appletCodeBase);
	translations.put("APPLETWIDTH", width);
	translations.put("APPLETHEIGHT", height);


	StringBuffer allParameters = new StringBuffer();
	for(int index = 0; index < parameters.length; index++)
	    allParameters.append("\t" + parameters[index] + "\n");
	
	translations.put("PARAMETERS", allParameters.toString());
	
	String template = "template.html";

	// commented out plugin code until fully implemented
	// if(htmlType.equals("plugin"))
	//     template = "template.htmlplugin";
	// else
	//    template = "template.html";
	   
	String filename = Config.getLibFilename(template);
		
	try {
	    Utility.translateFile(filename, outputFileName, translations);
	} catch(IOException e) {
	    Debug.reportError("Exception during file translation from " + 
			      filename + " to " + outputFileName);
	    e.printStackTrace();
	}
    }


    /**
     * 
     * Removes applicable files (.class, .java and .ctxt) prior to 
     * this AppletClassRole being removed from a Package.
     *
     */
    public void prepareFilesForRemoval(String sourceFile, String classFile, 
				       String contextFile)
    {
	super.prepareFilesForRemoval(sourceFile, classFile, contextFile);

	// remove associated HTML file if exists
	File htmlFileName = new File(classFile + HTML_EXTENSION);
	if (htmlFileName.exists())
	    htmlFileName.delete();
    }


    public void draw(Graphics g, ClassTarget ct, int x, int y, int width, 
		     int height)
    {
	g.setColor(ct.getTextColour());
	Utility.drawCentredText(g, "www",
		x + Target.TEXT_BORDER, 
		y + height - (Target.TEXT_HEIGHT + Target.TEXT_BORDER),
		width - (2 * Target.TEXT_BORDER), Target.TEXT_HEIGHT);
    
    }

}
