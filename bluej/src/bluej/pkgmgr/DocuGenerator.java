package bluej.pkgmgr;

import bluej.Config;
import bluej.BlueJEvent;
import bluej.pkgmgr.Package;
import bluej.utility.Utility;
import bluej.utility.Debug;
import bluej.utility.DialogManager;

import java.util.*;
import java.io.*;

/**
 * A documentation generator for a BlueJ project. For each Project instance
 * there is one instance of DocuGenerator.
 * Information in this class belongs to one of three categories: <BR>
 *
 * static information - valid for all runs of the generator (e.g. the name 
 * (not the path!) of the directory where the documentation is written to).<BR>
 *
 * instance information - valid for all generator runs for one project (e.g.
 * the path of the project directory). <BR>
 *
 * run-specific information - generated on each run (e.g. the names of the
 * targets, as these might change between several runs). <BR>
 *
 * Each of these categories can again be divided into tool-dependent (e.g.
 * the name of the documentation generating tool) and tool-independent.
 *
 * @author  Axel Schmolitzky
 * @version 
 */
public class DocuGenerator
{
    // static fields - tool-independent
    /** The name of the directory where documentation is written to. */
    private static String docDirName =
                                Config.getPropString("doctool.outputdir");

    // static fields - tool-dependent
    /** The name (including path) of the documentation tool used. */
    private static String docCommand = Config.getPropString("doctool.command");
    /** javadoc params for all runs: include author information, do not
     * generate information about deprecated features, include bottom line,
     * read overview information from README file.
     */
    private static String fixedJavadocParams = " -author -nodeprecated"
          + " -bottom <small><em>Generated&nbsp;by&nbsp;BlueJ</em></small>"
//          + " -overview " + Package.readmeName
          ;

    /* ------------------- end of static declarations ------------------ */

    //    instance fields

    // tool-independent instance fields
    /** The project this generator belongs to. */
    private Project project;
    /** The project directory. */
    private File projectDir;
    /** the directory where documentation is written to. */
    private File docDir;
    /** the path of the directory where documentation is written to. */
    private String docDirPath;
    /** the path of the project directory, the root for all sources. */
    private String sourceDirPath;


    // tool-dependent instance fields for javadoc
    /** javadoc param for the destination directory. */
    private String destinationParam;
    /** javadoc param for the root directory of the sources. */
    private String sourceParam;
    /** javadoc params for setting window and project title */
    private String titleParams;



    /* ------------------- end of field declarations ------------------- */

    /**
     * Construct the documentation generator for a project.
     * @param project the project this generator belongs to.
     */
    public DocuGenerator(Project project)
    {
        // setup tool-independent instance information
        this.project = project;
        projectDir = project.getProjectDir();
        docDir = new File(projectDir, docDirName);
        docDirPath = docDir.getPath();
        sourceDirPath = projectDir.getPath();

        // tool-dependent instance information for javadoc
        destinationParam = " -d " + docDirPath;
        sourceParam = " -sourcepath " + sourceDirPath;
        titleParams = " -doctitle " + project.getProjectName()
                    + " -windowtitle " + project.getProjectName();
    }

    /**
     * Generate documentation for the whole project.
     * @return -1 if an exception occured, 0 otherwise.
     */
    public String generateProjectDocu()
    {
        String docDirStatus = testDocDir();
        if (docDirStatus != "")
            return docDirStatus;

        // get the names of all the targets for the documentation tool.
        // first: get all package names
        List packageNames = project.getPackageNames();
        StringBuffer tmp = new StringBuffer();
        for (Iterator names=packageNames.iterator(); names.hasNext(); ) {
            tmp.append(" ");
            tmp.append((String)names.next());
        }
        // second: get class names of classes in unnamed package, if any
        List classNames = project.getPackage(project.getInitialPackageName())
                                                          .getAllClassnames();
        for (Iterator names = classNames.iterator();names.hasNext(); ) {
            tmp.append(" ");
            tmp.append((String)names.next());
        }
        String targets = new String(tmp);
        

        // tool-specific infos for javadoc
        // try to link the generated documentation to the API documentation
        String docURL = Config.getPropString("bluej.url.javaStdLib");
        String linkParam = "";
        if (docURL.endsWith("index.html")) {
            String docURLDir=docURL.substring(0,docURL.indexOf("index.html"));
            linkParam = " -link " + docURLDir;
        }

        // stick it all together
        String javadocCall = docCommand + sourceParam + destinationParam
                          + titleParams + linkParam + fixedJavadocParams
                          + targets;

        String docuCall = javadocCall;

        // start the call in a separate thread to allow fast return to GUI.
        Thread starterThread = new Thread(new docuRunStarter(docuCall));
        starterThread.setPriority(Thread.MIN_PRIORITY);
        starterThread.start();
        BlueJEvent.raiseEvent(BlueJEvent.GENERATING_DOCU, null);
        return "";
    }

    /**
     * This class enables to run the external call for a documentation 
     * generation in a different thread. An instance of this class gets   
     * the string that constitutes the external call as a constructor 
     * parameter.
     */
    private class docuRunStarter implements Runnable
    {
        private String docuCall;

        public docuRunStarter(String call)
        {
            docuCall = call;
        }

        /**
         * Perform the call that was passed in as a constructor parameter.
         * If this call was successful let the result be shown in a browser.
         */
        public void run()
        {
            Process docuRun;
            try {
                Debug.message(docuCall);
                docuRun = Runtime.getRuntime().exec(docuCall);

                // consume the output of the external process, if any
                BufferedReader reader = new BufferedReader(
                              new InputStreamReader(docuRun.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    Debug.message(line);
                }

                docuRun.waitFor();
                if (docuRun.exitValue() == 0) {
                    BlueJEvent.raiseEvent(BlueJEvent.DOCU_GENERATED,null);
                    File startPage = new File(docDir,"index.html");
                    Utility.openWebBrowser(startPage.getPath());
                }
                else {
                    reader = new BufferedReader(
                              new InputStreamReader(docuRun.getErrorStream()));
                    DialogManager.showMessageWithText(null,
                                            "doctool-error",reader.readLine());
                }
            }
            catch (IOException exc) {
                DialogManager.showMessage(null,"severe-doc-trouble");
            }
            catch (InterruptedException exc) {
                DialogManager.showMessage(null,"severe-doc-trouble");
            }
        }
    }


    /**
     * Test whether documentation directory exists in project dir and
     * create it, if necessary.
     * @return "" if directory exists and is accessible, an error message
     * otherwise.
     */
    private String testDocDir()
    {
        if (docDir.exists()) {
            if (!docDir.isDirectory())
                return DialogManager.getMessage("docdir-blocked-by-file");
        }
        else {
            try {
                if (!docDir.mkdir())
                    return DialogManager.getMessage("docdir-not-created");
            }
            catch (SecurityException exc) {
                return DialogManager.getMessage("no-permission-for-docdir");
            }
        }
        return "";
    }
}

