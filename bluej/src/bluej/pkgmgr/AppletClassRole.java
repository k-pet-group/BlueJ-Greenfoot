package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.DialogManager;
import bluej.utility.BlueJFileReader;
import bluej.utility.FileUtility;
import bluej.prefmgr.PrefMgr;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

/**
 * An Applet class role in a package, i.e. a target that is a Applet class file
 * built from Java source code.
 *
 * @author Bruce Quig
 * @version $Id: AppletClassRole.java 1778 2003-04-10 05:04:18Z ajp $
 */
public class AppletClassRole extends ClassRole
{
    public static final String APPLET_ROLE_NAME = "AppletTarget";
    
    private RunAppletDialog dialog;

    static final String runAppletStr = Config.getString("pkgmgr.classmenu.runApplet");
    static final String htmlComment = Config.getString("pkgmgr.runApplet.htmlComment");

    static final String APPLETVIEWER_COMMAND =
                            Config.getJDKExecutablePath("appletViewer.command", "appletviewer");

    //static final String HTML_EXTENSION = ".html";
    static final String THIS_DIRECTORY = ".";
    static final String URL_PREFIX = "file://localhost/";
    static final int DEFAULT_APPLET_WIDTH = 500;
    static final int DEFAULT_APPLET_HEIGHT = 500;

    private String[] appletParams;
    private int appletHeight;
    private int appletWidth;

    /**
     * Create the class role.
     */
    public AppletClassRole()
    {
        appletHeight = DEFAULT_APPLET_HEIGHT;
        appletWidth = DEFAULT_APPLET_WIDTH;
    }

    public String getRoleName()
    {
        return APPLET_ROLE_NAME;
    }

    public String getStereotypeLabel()
    {
        return "applet";
    }

    /**
     * Save this AppletClassRole details to file
     * @param props the properties object that stores target information
     * @param prefix prefix for this target for identification
     */
    public void save(Properties props, int modifiers, String prefix)
    {
        super.save(props, modifiers, prefix);
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
     * Generate a popup menu for this class role.
     *
     * @param   menu    the menu to add items to
     * @param   ct      the ClassTarget we are constructing the role for
     * @param   state   whether the target is COMPILED etc.
     * @return  true if we added any menu tiems, false otherwise
     */
    protected boolean createRoleMenu(JPopupMenu menu, ClassTarget ct, int state)
    {
        // add run applet option
        addMenuItem(menu, new AppletAction(ct.getPackage().getEditor(),ct),
                     (state == Target.S_NORMAL));
        menu.addSeparator();

        return true;
    }

    private class AppletAction extends AbstractAction
    {
        private Target t;
        private PackageEditor ped;

        public AppletAction(PackageEditor ped, Target t)
        {
            super(runAppletStr);
            this.ped = ped;
            this.t = t;
        }

        public void actionPerformed(ActionEvent e)
        {
            ped.raiseRunTargetEvent(t, null);
        }
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
    public void run(PkgMgrFrame parent, ClassTarget ct, String param)
    {
        String name = ct.getQualifiedName();
        Package pkg = ct.getPackage();

        if(dialog == null) {
            dialog = new RunAppletDialog(parent, name);
            // add params that originated from pkg properties
            if(appletParams != null)
                dialog.setAppletParameters(appletParams);
            dialog.setAppletHeight(appletHeight);
            dialog.setAppletWidth(appletWidth);
        }
        if(dialog.display()) {
            int execOption = dialog.getAppletExecutionOption();
            if(execOption == RunAppletDialog.GENERATE_PAGE_ONLY) {
                // generate HTML page for Applet using selected path and file name
                File generatedFile = chooseWebPage(parent);
                if(generatedFile != null)
                    createWebPage(generatedFile, name, URL_PREFIX + pkg.getPath().getPath());
            }
            else {
                File destFile = new File(pkg.getProject().getProjectDir(), name + ClassTarget.HTML_EXTENSION);

                createWebPage(destFile, name, ".");

                // Run applet as an external process
                String url = URL_PREFIX + destFile.getPath();

                if(execOption == RunAppletDialog.EXEC_APPLETVIEWER) {
                    try {
                        String[] execCommand = {APPLETVIEWER_COMMAND, url};
                        PkgMgrFrame.displayMessage(Config.getString("pkgmgr.appletInViewer"));

                        Process applet =
                            Runtime.getRuntime().exec(execCommand);
                    } catch (Exception e) {
                        pkg.showError("appletviewer-error");
                        Debug.reportError("Exception thrown in execution of appletviewer");
                        e.printStackTrace();
                    }
                }
                else {
                    // start in Browser
                    PkgMgrFrame.displayMessage(Config.getString("pkgmgr.appletInBrowser"));
                    Utility.openWebBrowser(destFile.getPath());
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
    private File chooseWebPage(JFrame frame)
    {
        String fullFileName = FileUtility.getFileName(frame,
                                Config.getString("pkgmgr.chooseWebPage.title"),
                                Config.getString("pkgmgr.chooseWebPage.buttonLabel"), 
                                false, null, false);

        if (fullFileName == null)
            DialogManager.showError(frame, "error-no-name");

        return new File(fullFileName);
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
    private void createWebPage(File outputFile, String appletName, String appletCodeBase)
    {
        updateAppletProperties();
        generateHTMLSkeleton(outputFile, appletName, appletCodeBase, dialog.getAppletWidth(), dialog.getAppletHeight(), appletParams);
    }

    /**
     * Creates a HTML Skeleton that contains this JApplet using
     * parameters input by user in RunAppletDialog class.
     *
     * @param name              the fully qualified name of the applet class
     * @param outputFileName    the name of the generated HTML file
     * @param appletCodeBase    code base to be included in applet tag (canot be null)
     * @param width             specified width of applet
     * @param height            specified height of applet
     * @param parameters        optional applet parameters
     */
    private void generateHTMLSkeleton(File outputFile, String appletName, String appletCodeBase, String width, String height, String[] parameters)
    {
        Hashtable translations = new Hashtable();

        translations.put("TITLE", appletName);
        translations.put("COMMENT", htmlComment);
        translations.put("CLASSFILE", appletName + ".class");
        // whilst it would be nice to be able to have no codebase, it is in the
        // HTML template file and hence even if we define no CODEBASE here, it
        // will appear in the resulting HTML anyway (as CODEBASE=$CODEBASE)
        translations.put("CODEBASE", appletCodeBase);
        translations.put("APPLETWIDTH", width);
        translations.put("APPLETHEIGHT", height);

        StringBuffer allParameters = new StringBuffer();
        for(int index = 0; index < parameters.length; index++)
            allParameters.append("\t" + parameters[index] + "\n");

        translations.put("PARAMETERS", allParameters.toString());

        File tmplFile = Config.getTemplateFile("html");

        try {
            BlueJFileReader.translateFile(tmplFile, outputFile, translations);
        } catch(IOException e) {
            Debug.reportError("Exception during file translation from " +
                              tmplFile + " to " + outputFile);
            e.printStackTrace();
        }
    }

    /**
     * Removes applicable files (.class, .java and .ctxt) prior to
     * this AppletClassRole being removed from a Package.
     */
    public void prepareFilesForRemoval(ClassTarget ct, String sourceFile,
                                       String classFile, String contextFile)
    {
        super.prepareFilesForRemoval(ct, sourceFile, classFile, contextFile);

        // remove associated HTML file if exists (lives in the root project
        // directory!)
        File htmlFile = new File(ct.getPackage().getProject().getProjectDir(), 
                                   ct.getQualifiedName() + ClassTarget.HTML_EXTENSION);
        if (htmlFile.exists())
            htmlFile.delete();
    }

    public void draw(Graphics2D g, ClassTarget ct, int x, int y, int width,
                     int height)
    {
    }

}
