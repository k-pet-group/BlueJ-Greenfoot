package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.Debug;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerClassLoader;
import bluej.debugger.Invoker;
import bluej.debugger.ObjectViewer;
import bluej.debugger.ResultWatcher;
import bluej.debugger.ObjectWrapper;
import bluej.editor.Editor;
import bluej.graph.GraphEditor;
import bluej.utility.Utility;
import bluej.views.ConstructorView;
import bluej.views.EditorPrintWriter;
import bluej.views.MemberView;
import bluej.views.MethodView;
import bluej.views.View;
import bluej.views.ViewFilter;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

/** 
 ** @version $Id: AppletTarget.java 124 1999-06-14 07:26:17Z mik $
 ** @author Bruce Quig
 **
 ** An Applet class target in a package, i.e. a target that is a Applet class file
 ** built from Java source code.
 **/
public class AppletTarget extends ClassTarget 
{
    private RunAppletDialog dialog;

    static final String runAppletStr = Config.getString("pkgmgr.classmenu.runApplet");
    static final String htmlComment = Config.getString("pkgmgr.runApplet.htmlComment");
    static final String HTML_EXTENSION = ".html";
    // this needs to change for different platforms
    static final String APPLETVIEWER_COMMAND = "appletviewer"; // move to bluej.defs
    static final String URL_PREFIX = "file:";
 
   /**
     * Create a new Applet class target in package 'pkg'.
     */
    public AppletTarget(Package pkg, String name)
    {
	super(pkg, name);
    }

    /**
     *  Create a new applet target in package 'pkg' without a name. The
     *  name must be set later.
     */
    public AppletTarget(Package pkg)
    {
	super(pkg, null);
    }


    /**
     * Save this AppletTarget details to file
     * @param props the properties object that stores target information
     * @param prefix prefix for this target for identification
     */
    public void save(Properties props, String prefix)
    {
	super.save(props, prefix);
	props.put(prefix + ".type", "AppletTarget");
	props.put(prefix + ".modifiers", Integer.toString(modifiers, 16));
    }
	

//     Color getDefaultBackground()
//     {
// 	return defaultbg;
//     }

    // --- Target interface ---

 //    Color getBackgroundColour()
//     {
//         if(state == S_COMPILING)
// 	    return compbg;
// 	else
// 	    return getDefaultBackground();
//     }

    /**
     * Generate a JApplet skeleton for this target.
     */
    public void generateSkeleton()
    {
	Hashtable translations = new Hashtable();
	translations.put("CLASSNAME", name);
		
	String template = "template.applet";
			
	String pkgname = pkg.getName();
	if((pkgname == Package.noPackage))
	    translations.put("PKGLINE", "");
	else
	    translations.put("PKGLINE", "package " + pkgname + ";" + Config.nl + Config.nl);
			
	String filename = Config.getLibFilename(template);
		
	try {
	    Utility.translateFile(filename, sourceFile(), translations);
	} catch(IOException e) {
	    Debug.reportError("Exception during file translation from " + filename + " to " + sourceFile());
	    e.printStackTrace();
	}
		
	setState(S_INVALID);
    }


    /**
     * Generate a popup menu for this AppletTargert.
     * @param cl the class object that is represented by this target
     * @param editorFrame the frame in which this targets package is displayed
     * @return the generated JPopupMenu
     */
    protected JPopupMenu createMenu(Class cl, JFrame editorFrame) {

	actions = new Hashtable();

	JPopupMenu menu = new JPopupMenu(getName() + " operations");

	// add run applet option
	addMenuItem(menu, runAppletStr, (state == S_NORMAL));
	menu.addSeparator();
	
	// the only popup menu option under the Library Browser should be
	// "open"
	if (editorFrame != null && 
	    (editorFrame instanceof LibraryBrowserPkgMgrFrame)) {
	    addMenuItem(menu, openStr, true);
	    // only add "use" option if the class is compiled and we're not 
	    // running standalone
	    if (!((LibraryBrowserPkgMgrFrame)editorFrame).isStandalone 
		/*&& isCompiled()*/)
		addMenuItem(menu, useStr, true);
	    
	    return menu;
	}
	
	if ((cl != null) && (!isAbstract()))
	    createClassMenu(menu, cl);
	
	addMenuItem(menu, editStr, true);
	addMenuItem(menu, publicStr, (state == S_NORMAL));
	addMenuItem(menu, pkgStr, (state == S_NORMAL));
	addMenuItem(menu, inheritedStr, (state == S_NORMAL));
	menu.addSeparator();
	addMenuItem(menu, compileStr, true);
	addMenuItem(menu, removeStr, true);
	
	return menu;
    }
	

    // -- ActionListener interface --

    public void actionPerformed(ActionEvent e)
    {
	String cmd = e.getActionCommand();

	if(runAppletStr.equals(cmd)) {
	    runApplet();
	}
	else super.actionPerformed(e);
    }
    
    // -- End of ActionListener interface --


   /**
     * Runs the applet in the Sun JDK appletviewer.  It generates a HTML page if
     * required to house the applet.
     */
    public void runApplet()
    {
	if(dialog == null)
	    dialog = new RunAppletDialog(this.pkg.getFrame(), getName());

	if(dialog.display()) {
	    int execOption = dialog.getAppletExecutionOption();
	    if(execOption == RunAppletDialog.GENERATE_PAGE_ONLY) {
		// generate HTML page for Applet
		String generatedFileName = chooseWebPage();
		if(generatedFileName != null)
		  createWebPage(generatedFileName);
	    }
	    else {
		createWebPage(htmlFile());
		// Run applet as an external process
		String execCommand;

		String url = URL_PREFIX 
		             + Config.slash 
		             + pkg.getClassFileName(name) 
		             + HTML_EXTENSION;
			     
		Debug.message("URL: " + url);
		if(execOption == RunAppletDialog.EXEC_APPLETVIEWER) {
		    
		    try {
			execCommand = APPLETVIEWER_COMMAND	
			    + " " + url;
			    Debug.message("Command: " + execCommand);
			    Process applet = 
				Runtime.getRuntime().exec(execCommand);
		    
		    } catch (Exception e) {
			System.err.println("Exception thrown in execution of appletviewer");
		    }
		}
		else
		    // start in Browser
		  Utility.openWebBrowser( pkg.getClassFileName(name) 
		             + HTML_EXTENSION);
	    }
	}
    }



   /**
     * Use a file chooser to select a web page name and location.
     */
    private String chooseWebPage()
    {
	JFileChooser newChooser = new JFileChooser(".");
	newChooser.setDialogTitle("Select HTML page destination");

	int result = newChooser.showSaveDialog(pkg.getFrame());
	String fullFileName = null;
	if (result == JFileChooser.APPROVE_OPTION) {
	    Debug.message("selected " + newChooser.getSelectedFile().getPath());
	    fullFileName = newChooser.getSelectedFile().getPath();
	}
	else if (result == JFileChooser.CANCEL_OPTION)
	    Debug.message("cancel selected");
	else {
	    Utility.showError(pkg.getFrame(), "You must specify a valid name.");
	    return null;
	}
	return fullFileName;
    }



   /**
     * Create a HTML page that contains this JApplet using
     * parameters input by user in RunAppletDialog class.
     * @param fileName fileName for HTML file to house Applet
     */
    private void createWebPage(String fileName)
    {
	String appletHeight = dialog.getAppletHeight();
	String appletWidth = dialog.getAppletWidth();
	Object[] appletParameters = dialog.getAppletParameters();
	String codeBase = URL_PREFIX + Config.slash + pkg.getClassDir();
	generateHTMLSkeleton(fileName, codeBase, appletWidth, appletHeight, appletParameters);
    }

   /**
     * Creates a HTML page that contains this JApplet using
     * parameters input by user in RunAppletDialog class.
     */
    public String htmlFile()
    {
	return pkg.getFileName(name) + HTML_EXTENSION;
    }
    
 
   /**
     * Creates a HTML Skeleton that contains this JApplet using
     * parameters input by user in RunAppletDialog class.
     */  
    public void generateHTMLSkeleton(String outputFileName, String appletCodeBase, String width, String height, Object[] parameters)
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
	    Debug.reportError("Exception during file translation from " + filename + " to " + sourceFile());
	    e.printStackTrace();
	}
    }


   /**
     * 
     * Removes applicable files (.class, .java and .ctxt) prior to 
     * this AppletTarget being removed from a Package.
     *
     */
    public void prepareFilesForRemoval()
    {
	super.prepareFilesForRemoval();

	// remove associated HTML file if exists
	File htmlFileName = new File(htmlFile());
	if (htmlFileName.exists())
	    htmlFileName.delete();
    }


  // overloads method in Target super class
    public void draw(Graphics g)
    {
	//g.setColor(getBackgroundColour());
	g.setColor((Color.green).darker());
	g.fillRect(x, y, width, height);
		
	if(state != S_NORMAL) {
	    g.setColor(shadowCol); // Color.lightGray
	    Utility.stripeRect(g, x, y, width, height, 8, 3);
	}

	g.setColor(textbg);
	g.fillRect(x + TEXT_BORDER, y + TEXT_BORDER,
		   width - 2 * TEXT_BORDER, TEXT_HEIGHT);

	g.setColor(shadowCol);
	drawShadow(g);
		
	g.setColor(getBorderColour());
	g.drawRect(x + TEXT_BORDER, y + TEXT_BORDER,
		   width - 2 * TEXT_BORDER, TEXT_HEIGHT);
	drawBorders(g);
		
	g.setColor(getTextColour());
	g.setFont(getFont());
	Utility.drawCentredText(g, name,
				x + TEXT_BORDER, y + TEXT_BORDER,
				width - 2 * TEXT_BORDER, TEXT_HEIGHT);


	g.setColor(getTextColour());
	Utility.drawCentredText(g, "www",
				x + TEXT_BORDER, y + height - (TEXT_HEIGHT + TEXT_BORDER),
				width - (2 * TEXT_BORDER), TEXT_HEIGHT);
    
    }

}
