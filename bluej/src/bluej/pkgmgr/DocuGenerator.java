package bluej.pkgmgr;

import bluej.Config;
import bluej.BlueJEvent;
import bluej.pkgmgr.Package;
import bluej.utility.Utility;
import bluej.utility.Debug;
import bluej.utility.DialogManager;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * This class handles documentation generation from inside BlueJ.
 * Documentation can be generated for a whole project or for a single class.
 * For each Project instance there should be one instance of DocuGenerator
 * that takes care of project documentation. Project documentation is written
 * into a directory in the project directory.
 * The documentation for a single class serves merely as a preview option,
 * the documentation is thus generated in a temporary directory.
 *
 * Information in this class belongs to one of three categories: <BR>
 * <BR>
 * Static information - valid for all runs of a generator (e.g. the name
 * (not the path!) of the directory where project documentation is written
 * to).<BR>
 * <BR>
 * Instance information - valid for all generator runs for one project (e.g.
 * the path of the project directory). <BR>
 * <BR>
 * Run-specific information - generated on each run (e.g. the names of the
 * targets, as these might change between several runs). <BR>
 * <BR>
 * Each of these categories can again be divided into tool-dependent (e.g.
 * the name of the documentation generating tool) and tool-independent.
 *
 * @author  Axel Schmolitzky
 * @version $ $
 */
public class DocuGenerator
{
    // static fields - tool-independent
    /** The name of the directory where project documentation is written to. */
    private static String docDirName =
                                Config.getPropString("doctool.outputdir");

    // static fields - tool-dependent
    /** The name (including path) of the documentation tool used. */
    private static String docCommand = Config.getPropString("doctool.command");
    /** javadoc parameters for all runs: include author information, do not
     * generate information about deprecated features, include bottom line,
     * read overview information from README file.
     */
    private static String fixedJavadocParams = " -author -nodeprecated"
          + " -bottom <small><em>Generated&nbsp;by&nbsp;BlueJ</em></small>"
//          + " -overview " + Package.readmeName
          ;
    /** javadoc parameters for preview runs: do not generate an index,
     * a tree, a help.
     */
    private static String tmpJavadocParams = " -noindex -notree -nohelp";

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
     * Construct a documentation generator instance for a project.
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
     * Generate documentation for the whole project. As this is done in
     * a different process this method just returns whether the preconditions
     * for the generation that are immediately testable are fulfilled.
     * @return "" if the external process was started, an error message
     * otherwise.
     */
    public String generateProjectDocu()
    {
        // test whether the documentation directory is accessible.
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
        // get the parameter that enables javadoc to link the generated
        // documentation to the API documentation
        String linkParam = getLinkParam();

        // stick it all together
        String javadocCall = docCommand + sourceParam + destinationParam
                          + titleParams + linkParam + fixedJavadocParams
                          + targets;

        File startPage = new File(docDir,"index.html");
        String fileURL = "file://"+startPage.getPath();

        doCallThenBrowse(javadocCall,fileURL);
        return "";
    }

    /**
     * Generate documentation for the class in file 'filename'. The
     * documentation is generated in a temporary directory only. If the
     * generation was successful the result will be displayed in a web browser.
     * @param filename the fully qualified filename of the class to be
     * documented.
     */
    public static void generateClassDocu(String filename)
    {
        // create a temporary directory and let it be removed on exiting BlueJ
        File docTempDir = new File(System.getProperty("java.io.tmpdir"),
                                                                   "BJdoctmp");
        docTempDir.mkdir();
        docTempDir.deleteOnExit();

        // build the call string
        String javadocCall = docCommand + fixedJavadocParams + tmpJavadocParams
            + " -d " + docTempDir.getPath() + " " + filename;

        // build the URL for the result to be shown
        String className = new File(filename).getName();
        if (className.endsWith(".java"))
            className = className.substring(0,className.indexOf(".java"));
        File htmlFile = new File(docTempDir,className + ".html");
        String url = htmlFile.getPath();

        doCallThenBrowse(javadocCall,url);
    }



    /**
     * Creates a separate thread that starts the external call for faster
     * return to the GUI. If the call was successful the URL given in 'url'
     * will be shown in a web browser.
     * @param call the call to the documentation generating tool.
     * @param url the URL to be shown after successful completion.
     */
    private static void doCallThenBrowse(String call, String url)
    {
        // start the call in a separate thread to allow fast return to GUI.
        Thread starterThread = new Thread(new docuRunStarter(call,url));
        starterThread.setPriority(Thread.MIN_PRIORITY);
        starterThread.start();
        BlueJEvent.raiseEvent(BlueJEvent.GENERATING_DOCU, null);
    }


    /**
     * This class enables to run the external call for a documentation
     * generation in a different thread. An instance of this class gets
     * the string that constitutes the external call as a constructor
     * parameter. The second constructor parameter is the name of the
     * HTML file that should be opened by a web browser if the documentation
     * generation was successful.
     */
    private static class docuRunStarter implements Runnable
    {
        private String docuCall;
        private String showURL;

        public docuRunStarter(String call, String showURL)
        {
            docuCall = call;
            this.showURL = showURL;
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

                Thread outEcho = new EchoThread(docuRun.getInputStream(), System.out);
                Thread errEcho = new EchoThread(docuRun.getErrorStream(), System.out);
                outEcho.start();
                errEcho.start();
                try {
                    docuRun.waitFor();
                    outEcho.join();
                    errEcho.join();
                }
                catch(InterruptedException e) {
                    System.err.println("Interrupted waiting for process");
                }

                if (docuRun.exitValue() == 0) {
                    BlueJEvent.raiseEvent(BlueJEvent.DOCU_GENERATED,null);
                    Utility.openWebBrowser(showURL);
                }
                else {
                    DialogManager.showMessageWithText(null,
                                            "doctool-error", "wow"); //reader.readLine());
                }
            }
            catch (IOException exc) {
                DialogManager.showMessage(null,"severe-doc-trouble");
            }
        }

        private static class EchoThread extends Thread {
            InputStream   readStream;
            OutputStream  echoStream;
            public EchoThread(InputStream r, OutputStream o) {
                readStream = r;
                echoStream = o;
            }
            public void run() {
                try {
                    byte[] buf = new byte[1000];
                    int n;
                    while((n = readStream.read(buf)) != -1)
                        echoStream.write(buf, 0, n);
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
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

    /**
     * javadoc can link the generated documentation to existing documentation.
     * This method constructs the javadoc parameter to set the link to the
     * Java API. To make sure that javadoc is happy we test whether the file
     * that javadoc needs (a list of all package names of the API) is
     * accessible via the link provided in the BlueJ properties file.
     * @return the link parameter if the link is working, "" otherwise.
     */
    private String getLinkParam()
    {
        String docURL = Config.getPropString("bluej.url.javaStdLib");

        // to avoid runtime errors: check whether substring will work
        if (docURL.endsWith("index.html")) {
            // this is the parameter javadoc expects
            String docURLDir=docURL.substring(0,docURL.indexOf("index.html"));

            // test whether this URL is valid: try to read package list file
            try {
                URL packList = new URL(docURLDir + "package-list");
                BufferedReader in = new BufferedReader(
                          new InputStreamReader(packList.openStream()));

                if (in.readLine() != null)
                    // one line of the file could be read, link seems ok
                    return " -link " + docURLDir;
            }
            catch (MalformedURLException e) { }
            catch (IOException e) { }
        }
        return "";
    }
}

