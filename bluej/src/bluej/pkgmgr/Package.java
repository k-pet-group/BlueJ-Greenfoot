package bluej.pkgmgr;

import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.compiler.*;
import bluej.debugger.*;
import bluej.parser.ClassParser;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;
import bluej.editor.Editor;
import bluej.editor.EditorManager;
import bluej.editor.moe.MoeEditorManager;
import bluej.graph.Graph;
import bluej.graph.Vertex;
import bluej.utility.*;
import bluej.utility.filefilter.*;
import bluej.views.Comment;
import bluej.views.CommentList;
import bluej.classmgr.*;

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.JFrame;
import java.io.*;
import java.util.*;
import java.text.DateFormat;
import java.awt.print.Paper;
import java.awt.print.PageFormat;


/**
 * A Java package (collection of Java classes).
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @author  Axel Schmolitzky
 * @author  Andrew Patterson
 * @version $Id: Package.java 505 2000-05-24 05:44:24Z ajp $
 */
public class Package extends Graph
    implements CompileObserver, MouseListener, MouseMotionListener
{
    /** the title of a package frame with no package loaded */
    public static String noPackageTitle = Config.getString("pkgmgr.noPackage");

    /** message to be shown on the status bar */
    static final String compiling = Config.getString("pkgmgr.compiling");
    /** message to be shown on the status bar */
    static final String compileDone = Config.getString("pkgmgr.compileDone");
    /** message to be shown on the status bar */
    static final String chooseUsesTo = Config.getString("pkgmgr.chooseUsesTo");
    /** message to be shown on the status bar */
    static final String chooseInhTo = Config.getString("pkgmgr.chooseInhTo");

    /** the name of the package file in a package directory that holds
     *  information about the package and its targets. */
    public static final String pkgfileName = "bluej.pkg";
    /** the name of the backup file of the package file */
    public static final String pkgfileBackup = "bluej.pkh";

    /** error code */ public static final int NO_ERROR = 0;
    /** error code */ public static final int FILE_NOT_FOUND = 1;
    /** error code */ public static final int ILLEGAL_FORMAT = 2;
    /** error code */ public static final int COPY_ERROR = 3;
    /** error code */ public static final int CLASS_EXISTS = 4;
    /** error code */ public static final int CREATE_ERROR = 5;

    /** layout constant */ private static final int STARTROWPOS = 20;
    /** layout constant */ private static final int STARTCOLUMNPOS = 20;
    /** layout constant */ private static final int DEFAULTTARGETHEIGHT = 50;
    /** layout constant */ private static final int TARGETGAP = 20;
    /** layout constant */ private static final int RIGHT_LAYOUT_BOUND = 500;

    /** used to size frame when no existing size information can be found */
    private static final int DEFAULTFRAMEHEIGHT = 600;
    /** used to size frame when no existing size information can be found */
    private static final int DEFAULTFRAMEWIDTH = 800;

    /** Interface to editor */
    public static EditorManager editorManager = new MoeEditorManager();
    // static EditorManager editorManager = new RedEditorManager(false);
    // static EditorManager editorManager = new SimpleEditorManager();

    /* the Project this package is in */
    private Project project;

    /* the parent Package object for this package or null if this is the unnamed package
       ie. the root of the package tree */
    private Package parentPackage = null;

    /* base name of package (eg util) ("" for the unnamed package) */
    private String baseName = "";

    /* fully qualified name of pkg (eg java.util) ("" for the unnamed package) */
    private String qualifiedName = "";

    /** all the targets in a package */
    protected Hashtable targets;

    /** all the uses-arrows in a package */
    protected Vector usesArrows;

    /** all the extends-arrows in a package */
    protected Vector extendsArrows;

    /** the currently selected target */
    protected Target selected;

    /** Holds the choice of "from" target for a new dependency */
    protected Target fromChoice;

    /** used during arrow deletion */
    Dependency currentArrow;

    /** the CallHistory of a package */
    private CallHistory callHistory;

    /** whether extends-arrows should be shown */
    protected boolean showExtends = true;
    /** whether uses-arrows should be shown */
    protected boolean showUses = true;

    /** needed when debugging with breakpoints to see if the editor window
     *  needs to be brought to the front */
    private String lastSourceName = "";

    /* filter used for importing packages */
    protected static DirectoryFilter dirsOnly = new DirectoryFilter();
    /* filter used for importing packages */
    protected static JavaSourceFilter javaOnly = new JavaSourceFilter();

    /** state constant */ public static final int S_IDLE = 0;
    /** state constant */ public static final int S_CHOOSE_USES_FROM = 1;
    /** state constant */ public static final int S_CHOOSE_USES_TO = 2;
    /** state constant */ public static final int S_CHOOSE_EXT_FROM = 3;
    /** state constant */ public static final int S_CHOOSE_EXT_TO = 4;
    /** state constant */ public static final int S_DELARROW = 5;

    /** determines the maximum length of the CallHistory of a package */
    public static final int HISTORY_LENGTH = 6;

    /** the state a package can be in (one of the S_* values) */
    private int state = S_IDLE;

    /** is uml notation used */
    // static final String notationStyle = Config.getDefaultPropString("bluej.notation.style", Graph.UML);
    // private boolean isUML =  true;


    /* ------------------- end of field declarations ------------------- */

    /**
     * Create a package of a project with the package name of
     * qualifiedName (ie java.util) and with a parent package of parent
     */
    public Package(Project project, String qualifiedName, Package parent)
    {
        if (parent == null)
            throw new NullPointerException("Package must have a valid parent package");

        if (qualifiedName.length() == 0)
            throw new IllegalArgumentException("unnamedPackage must be created using Package(project)");

        if (!JavaNames.isQualifiedIdentifier(qualifiedName))
            throw new IllegalArgumentException(qualifiedName + " is not a valid qualifiedName for Package");

        this.project = project;
        this.qualifiedName = qualifiedName;
        this.baseName = JavaNames.getBase(qualifiedName);
        this.parentPackage = parent;

        init();
    }

    /**
     * Create the unnamed package of a project
     */
    public Package(Project project)
    {
        this.project = project;
        this.qualifiedName = "";
        this.baseName = "";
        this.parentPackage = null;

        init();
    }

    private void init()
    {
        targets = new Hashtable();
        usesArrows = new Vector();
        extendsArrows = new Vector();
        selected = null;
        callHistory = new CallHistory(HISTORY_LENGTH);
        //if(Graph.BLUE.equals(notationStyle))
        //    isUML = false;
    }

    public boolean isUnnamedPackage()
    {
        return parentPackage == null;
    }

    /**
     * Return the project this package belongs to.
     */
    public Project getProject()
    {
        return project;
    }

    /**
     * Get the unique identifier for this package (it's directory name
     * at present)
     */
    public String getId()
    {
        return getPath().getPath();
    }

    /**
     * Return this package's base name (eg util)
     * ("" for the unnamed package)
     */
    public String getBaseName()
    {
        return baseName;
    }

    /**
     * Return the qualified name of an identifier in this
     * package (eg java.util.Random if given Random)
     */
    public String getQualifiedName(String identifier)
    {
        if(isUnnamedPackage())
            return identifier;
        else
            return getQualifiedName() + "." + identifier;
    }

    /**
     * Return the qualified name of the package (eg. java.util)
     * ("" for the unnamed package)
     */
    public String getQualifiedName()
    {
        Package currentPkg = this;
        String retName = "";

        while(!currentPkg.isUnnamedPackage()) {
            if(retName == "")
                retName = currentPkg.getBaseName();
            else
                retName = currentPkg.getBaseName() + "." + retName;

            currentPkg = currentPkg.getParent();
        }

        return retName;
    }

    private File getRelativePath()
    {
        Package currentPkg = this;
        File retFile = new File(currentPkg.getBaseName());

        /* loop through our parent packages constructing a relative
           path for this file */
        while(!currentPkg.isUnnamedPackage()) {
            currentPkg = currentPkg.getParent();

            retFile = new File(currentPkg.getBaseName(), retFile.getPath());
        }

        return retFile;
    }

    /**
     * Return a file object of the directory location of this package.
     *
     * @return  The file object representing the full path to the
     *          packages directory
     */
    public File getPath()
    {
        /* append our relative path onto the absolute path which our project
           gives us */
        return new File(project.getProjectDir(), getRelativePath().getPath());
    }

    protected Package getParent()
    {
        return parentPackage;
    }



    public void setStatus(String msg)
    {
        //XXX raise an event to show the status somehow for this package
    }

    public void setStatusProject(String msg)
    {
        //XXX raise an event to show the status in all windows belong to this project
    }

  //   public boolean isUML()
//     {
//         return isUML;
//     }

    public void setStatusAll(String msg)
    {
        //XXX raise an event to show the status in all open pkgmgrframes
    }


    public void repaint()
    {
        if(editor != null) {
            editor.revalidate();
            editor.repaint();
        }
    }

    public PackageEditor getEditor()
    {
        return (PackageEditor)editor;
    }

    /**
     * Get the currently selected Target.  Should return null if none are
     * selected.
     *
     * @return the currently selected Target.
     */
    public Target getSelectedTarget()
    {
        return selected;
    }

    /**
     * Create a set of properties for a specified set of classfiles
     * residing in a specified directory.  Invoked when no package
     * file exists in this directory already, or when the files come
     * from an archive library (i.e., ZIP or JAR)
     *
     * @param       classFiles the array of class files to use for the package
     * @param       packageDir the directory in which to create the package
     * @param       fromArchive true if the package is being created for an archive (i.e., ZIP or JAR)
     * @return      the properties describing the new package
     * @exception   IOException if the package file could not be saved
     */
    public static SortedProperties createDefaultPackage(String[] classFiles,
                                                          String packageLocation)
        throws IOException
    {
        boolean fromArchive = false;
        SortedProperties props = new SortedProperties();
        int numberOfTargets = classFiles.length;

        int nbrColumns = (int) Math.sqrt(new Double("" + numberOfTargets).doubleValue());
        int rowPos = STARTROWPOS;
        int columnPos = STARTCOLUMNPOS;
        // try and layout the targets in a grid, one row at a time
        for (int current = 0; current < classFiles.length; current++) {
            String currentFile = classFiles[current];

            if (currentFile.endsWith(".class")) {
                props.put("target" + (current + 1) + ".type", "ClassTarget");
                // trim the .class off the filename for class targets
                props.put("target" + (current + 1) + ".name", currentFile.substring(0, currentFile.indexOf(".class")));
            } else {
                props.put("target" + (current + 1) + ".type", "PackageTarget");
                props.put("target" + (current + 1) + ".name", currentFile);
//                props.put("target" + (current + 1) + ".packageName", packageName + currentFile);
            }

            String fullname = props.get("target" + (current + 1) + ".name").toString();

            int targetWidth = 40 + (int)PrefMgr.getStandardFont().getStringBounds(fullname,
                                                                                  new FontRenderContext(new AffineTransform(), false, false)).getWidth();

            // make width roughly the length of the name
            // = DEFAULTTARGETCHARWIDTH * .length();
            // add extra width for package targets (default equation leaves them too narrow)
            targetWidth += props.get("target" + (current + 1) + ".type").toString().equals("PackageTarget") ? 20 : 0;

            props.put("target" + (current + 1) + ".width", "" + targetWidth);
            props.put("target" + (current + 1) + ".height", "" + DEFAULTTARGETHEIGHT);
            props.put("target" + (current + 1) + ".x", "" + rowPos);
            props.put("target" + (current + 1) + ".y", "" + columnPos);
            if ((current + 1) % nbrColumns == 0) {
                columnPos += DEFAULTTARGETHEIGHT + TARGETGAP;
                rowPos = STARTROWPOS;
            }
            else
                rowPos += targetWidth + TARGETGAP;
        }

        // specify the dimensions large enough to see the entire package
        props.put("package.window.width", "" + DEFAULTFRAMEWIDTH);
        props.put("package.window.height", "" + DEFAULTFRAMEHEIGHT);

        if (fromArchive == false) {
            // throw an exception if we cannot save
            File file = new File(packageLocation, pkgfileName);
            try {
                props.store(new FileOutputStream(file),
                            fromArchive == true ?
                            "Default layout for archive library" :
                            "Default package layout");
            } catch(Exception e) {
                Debug.reportError("could not save properties file: " +
                                  file.getName());
            }
        }

        return props;
    }

    /**
     * Load the elements of a package from a specified directory.
     */
    public void load()
    {
        SortedProperties props = new SortedProperties();
        // read the package properties
        File pkgFile = new File(getPath(), pkgfileName);

        // try to load the package file for this package
        try {
            FileInputStream input = new FileInputStream(pkgFile);

            props = new SortedProperties();
            props.load(input);
        } catch(IOException e) {
            Debug.reportError("Error loading initialisation file" +
                              pkgFile + ": " + e);
        }

        if (props == null)
            return;

        String width_str = props.getProperty("package.window.width", "512");
        String height_str = props.getProperty("package.window.height", "450");
//XXX        frame.setSize(Integer.parseInt(width_str), Integer.parseInt(height_str));

        // This is to make sure that opening a package into an empty frame
        // works properly

/* XXX        frame.invalidate();
        frame.validate(); */


        // read in all the targets contained in this package
        Map propTargets = new HashMap();

        try {
            int numTargets = Integer.parseInt(props.getProperty("package.numTargets", "0"));
            int numDependencies = Integer.parseInt(props.getProperty("package.numDependencies", "0"));

            for(int i = 0; i < numTargets; i++) {
                Target target = null;
                String type = props.getProperty("target" + (i + 1) + ".type");
                String identifierName = props.getProperty("target" + (i + 1) + ".name");

                if("ClassTarget".equals(type) || "AppletTarget".equals(type)) {
                    target = new ClassTarget(this, identifierName, "AppletTarget".equals(type));
                }
                else if("PackageTarget".equals(type))
                    target = new PackageTarget(this, identifierName);

                if(target != null) {
                    //Debug.message("Load target " + target);
                    target.load(props, "target" + (i + 1));
                    //Debug.message("Putting " + identifierName);
                    propTargets.put(identifierName, target);
                }
            }

            File subdirs[] = getPath().listFiles(new SubPackageFilter());

            for(int i=0; i<subdirs.length; i++)
            {
                Target target = (Target) propTargets.get(subdirs[i].getName());

                if(target == null || !(target instanceof PackageTarget))
                    target = new PackageTarget(this, subdirs[i].getName());

                addTarget(target);
            }

            File srcfiles[] = getPath().listFiles(new JavaSourceFilter());

            for(int i=0; i<srcfiles.length; i++)
            {
                String targetName = JavaNames.stripSuffix(srcfiles[i].getName(), ".java");
                Target target = (Target) propTargets.get(targetName);
                if(target == null || !(target instanceof ClassTarget))
                    target = new ClassTarget(this, targetName);

                addTarget(target);
            }

            if (!isUnnamedPackage()) {
                Target t = new ParentPackageTarget(this);
                t.setPos(10,10);
                addTarget(t);
            }

            for(int i = 0; i < numDependencies; i++) {
                Dependency dep = null;
                String type = props.getProperty("dependency" + (i+1) + ".type");

                if("UsesDependency".equals(type))
                    dep = new UsesDependency(this);
                //		else if("ExtendsDependency".equals(type))
                //		    dep = new ExtendsDependency(this);
                //		else if("ImplementsDependency".equals(type))
                //		    dep = new ImplementsDependency(this);

                if(dep != null) {
                    dep.load(props, "dependency" + (i + 1));
                    addDependency(dep, false);
                }
            }
            recalcArrows();
        } catch(Exception e) {
            Debug.reportError("Error loading from file " +
                              pkgFile + ": " + e);
            e.printStackTrace();
            return;
        }

        for(Enumeration e = targets.elements(); e.hasMoreElements(); ) {
            Target target = (Target)e.nextElement();

            if(target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget)target;
                ct.analyseDependencies();
            }
        }

        for(Enumeration e = targets.elements(); e.hasMoreElements(); ) {
            Target t = (Target)e.nextElement();
            if((t instanceof ClassTarget)
               && ((ClassTarget)t).upToDate()) {
                ClassTarget ct = (ClassTarget)t;
//                if (readyToPaint)
                    ct.setState(Target.S_NORMAL);
                // XXX: Need to invalidate things dependent on t
            }
        }
    }

    /**
     * Save this package to disk. The package is saved to the standard
     * package file (bluej.pkg).
     */
    public boolean save()
    {
        /* create the directory if it doesn't exist */
        File dir = getPath();
        if(!dir.exists()) {
            if(!dir.mkdir()) {
                Debug.reportError("Error creating directory " + dir);
                return false;
            }
        }

        File file = new File(dir, pkgfileName);
        if(!file.canWrite())
            return false;
        if(file.exists()) {			// make backup of original
            file.renameTo(new File(getPath(), pkgfileBackup));
        }

        SortedProperties props = new SortedProperties();

        // save targets and dependencies in package

        props.put("package.numDependencies",
                  String.valueOf(usesArrows.size()));

        Enumeration t_enum = targets.elements();            // targets
        int t_count = 0;
        for(int i = 0; t_enum.hasMoreElements(); i++) {
            Target t = (Target)t_enum.nextElement();
            // should use a better method of determining non saved targets
            if(!(t instanceof ParentPackageTarget)) {
                t.save(props, "target" + (i + 1));
                t_count++;
            }
        }
        props.put("package.numTargets", String.valueOf(t_count));


        for(int i = 0; i < usesArrows.size(); i++) {        // uses arrows
            Dependency d = (Dependency)usesArrows.elementAt(i);
            d.save(props, "dependency" + (i + 1));
        }

        try {
            FileOutputStream output = new FileOutputStream(file);
            props.store(output, "BlueJ package file");
        } catch(IOException e) {
            Debug.reportError("Error saving project file " + file + ": " + e);
            return false;
        }

        return true;
    }

    /**
     * Save this package under a new name. Returns an error code out of
     * (NO_ERROR, CREATE_ERROR, COPY_ERROR).
     *
     * @return An error code indicating success or failure.
     */
    public int saveAs(String newname)
    {
//        if (!save(newname))
//            return CREATE_ERROR;

        boolean okay = true;
        Enumeration t_enum = targets.elements();
        for(int i = 0; t_enum.hasMoreElements(); i++) {
            Target t = (Target)t_enum.nextElement();
            okay = okay && t.copyFiles(newname + File.separator);
            if(t instanceof EditableTarget) {
                // if editor is not null close it
                if(((EditableTarget)t).editor != null)
                    ((EditableTarget)t).editor.close();
                // make editor null so existing references to old file are lost
                ((EditableTarget)t).editor = null;
            }

        }
        // PENDING: update all package directives in sources

 //XXX       dirname = newname;

        if(okay)
            return NO_ERROR;
        else
            return COPY_ERROR;
    }

    /**
     * importFile - import a source file into this package as a new
     *  class target. Returns an error code: <br>
     *   NO_ERROR       - everything is fine <br>
     *   FILE_NOT_FOUND - file does not exist <br>
     *   ILLEGAL_FORMAT - the file name does not end in ".java" <br>
     *   CLASS_EXISTS - a class with this name already exists <br>
     *   COPY_ERROR     - could not copy
     */
    public int importFile(String sourcePath)
    {
        // check whether specified class exists and is a java file

        File sourceFile = new File(sourcePath);
        if(! sourceFile.exists())
            return FILE_NOT_FOUND;
        String fileName = sourceFile.getName();

        String className;
        if(fileName.endsWith(".java"))		// it's a Java source file
            className = fileName.substring(0, fileName.length() - 5);
        else
            return ILLEGAL_FORMAT;

        // check whether name is already used
        if(getTarget(className) != null)
            return CLASS_EXISTS;

        // copy class source into package

        String destPath = new File(getPath(),fileName).getPath();
        if(!BlueJFileReader.copyFile(sourcePath, destPath))
            return COPY_ERROR;

        // remove package line in class source

        try {
            ClassTarget.enforcePackage(destPath, getQualifiedName());
        }
        catch(IOException ioe)
            {
                Debug.message(ioe.getLocalizedMessage());
            }

        // create class icon (ClassTarget) for new class

        ClassTarget target = new ClassTarget(this, className);
        addTarget(target);
        target.analyseDependencies();

        return NO_ERROR;
    }

    /**
     * Arrange all the targets in this package in a standard layout.
     * It is guaranteed that after this method is called no targets in the
     * package do overlap.
     * This method is typically called after mass-importing targets, as with
     * importPackage.
     */
    public void doDefaultLayout()
    {
        int horizontal = STARTCOLUMNPOS;
        int vertical = STARTROWPOS;

        // first iterate over the class targets and lay them out in rows
        for (Enumeration e = targets.elements(); e.hasMoreElements(); ) {
            Target target = (Target)e.nextElement();

            if (target instanceof ClassTarget) {
                target.setPos(horizontal, vertical);
                horizontal += target.getWidth() + TARGETGAP;
                if (horizontal > RIGHT_LAYOUT_BOUND) {
                    horizontal = STARTCOLUMNPOS;
                    vertical += Target.DEF_HEIGHT + TARGETGAP;
                }
            }
        }

        // then iterate over the package targets, starting on a new row
        if (horizontal != STARTCOLUMNPOS) {
            horizontal = STARTCOLUMNPOS;
            vertical += Target.DEF_HEIGHT + TARGETGAP;
        }
        for (Enumeration e = targets.elements(); e.hasMoreElements(); ) {
            Target target = (Target)e.nextElement();

            if (target instanceof PackageTarget) {
                target.setPos(horizontal, vertical);
                horizontal += target.getWidth() + TARGETGAP;
                if (horizontal > RIGHT_LAYOUT_BOUND) {
                    horizontal = STARTCOLUMNPOS;
                    vertical += Target.DEF_HEIGHT + TARGETGAP;
                }
            }
        }
    }

    /**
     * Get the DebuggerClassLoader for this package.
     * The DebuggerClassLoader load classes on the remote VM
     * (the machine used for user code execution).
     */
    public synchronized DebuggerClassLoader getRemoteClassLoader()
    {
        return getProject().getRemoteClassLoader();
    }

    /**
     * Loads a class using the current class loader.
     * Creates a classloader if none currently exists.
     */
    public Class loadClass(String className)
    {
        return getProject().loadClass(className);
    }


    public Enumeration getVertices()
    {
        return targets.elements();
    }

    public Enumeration getEdges()
    {
        Vector enumerations = new Vector();

        if(showUses)
            enumerations.addElement(usesArrows.elements());
        if(showExtends)
            enumerations.addElement(extendsArrows.elements());

        return new MultiEnumeration(enumerations);
    }

    /**
     *  The standard compile user function: Find and compile all uncompiled
     *  classes.
     */
    public void compile()
    {
        if(!checkCompile())
            return;

        Vector toCompile = new Vector();

        for(Enumeration e = targets.elements(); e.hasMoreElements(); ) {
            Target target = (Target)e.nextElement();

            if(target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget)target;
                if (ct.editorOpen())
                    ct.getEditor().save();
                if(ct.getState() == Target.S_INVALID)
                    toCompile.addElement(ct);
            }
        }
        compileSet(toCompile);
    }

    /**
     *  Compile a single class.
     */
    public void compile(ClassTarget ct)
    {
        if(!checkCompile())
            return;

        if (ct.editorOpen())
            ct.getEditor().save();
        ct.setState(Target.S_INVALID);		// to force compile

        searchCompile(ct, 1, new Stack());
    }


    /**
     * Force compile of all classes. Called by user function "rebuild".
     */
    public void rebuild()
    {
        if(!checkCompile())
            return;

        Vector v = new Vector();

        for(Enumeration e = targets.elements(); e.hasMoreElements(); ) {
            Target target = (Target)e.nextElement();

            if(target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget)target;
                if (ct.editorOpen())
                    ct.getEditor().save();
                ct.setState(Target.S_INVALID);
                ct.analyseDependencies();
                v.addElement(ct);
            }
        }
        doCompile(v);
    }


    private void compileSet(Vector toCompile)
    {
        for(int i = toCompile.size() - 1; i >= 0; i--)
            searchCompile((ClassTarget)toCompile.elementAt(i), 1,
                          new Stack());
    }


    /** Use Tarjan's algorithm to construct compiler Jobs **/
    private void searchCompile(ClassTarget t, int dfcount, Stack stack)
    {
        if((t.getState() != Target.S_INVALID) || t.isFlagSet(Target.F_QUEUED))
            return;

        t.setFlag(Target.F_QUEUED);
        t.dfn = dfcount;
        t.link = dfcount;

        stack.push(t);

        Enumeration dependencies = t.dependencies();

        while(dependencies.hasMoreElements()) {
            Dependency d = (Dependency)dependencies.nextElement();
            if(!(d.getTo() instanceof ClassTarget))
                continue;

            // XXX bad bad bad. Must fix
            if(d.getTo() instanceof ImportedClassTarget)
                continue;

            ClassTarget to = (ClassTarget)d.getTo();

            if(to.isFlagSet(Target.F_QUEUED)) {
                if((to.dfn < t.dfn) && (stack.search(to) != -1))
                    t.link = Math.min(t.link, to.dfn);
            }
            else if(to.getState() == Target.S_INVALID) {
                searchCompile((ClassTarget)to, dfcount + 1, stack);
                t.link = Math.min(t.link, to.link);
            }
        }

        if(t.link == t.dfn) {
            Vector v = new Vector();
            ClassTarget x;

            do {
                x = (ClassTarget)stack.pop();
                v.addElement(x);
            } while(x != t);

            doCompile(v);
        }
    }

    /**
     *  Compile every Target in 'targetList'. Every compilation goes through
     *  this method.
     */
    private void doCompile(Vector targetList)
    {
        String[] files = new String[targetList.size()];
        for(int i = 0; i < targetList.size(); i++) {
            ClassTarget ct = (ClassTarget)targetList.get(i);
            files[i] = ct.getSourceFile().getPath();
        }
        removeBreakpoints();

        JobQueue.getJobQueue().addJob(files, this, getProject().getClassPath(),
                                        getProject().getProjectDir().getPath());
    }


    /**
     *  Check whether it's okay to compile.
     */
    private boolean checkCompile()
    {
        if(Debugger.debugger.getStatus() != Debugger.IDLE) {
            showMessage("compile-while-executing");
            return false;
        }
        else
            return true;
    }

    /**
     *  Remove all breakpoints in all classes.
     */
    private void removeBreakpoints()
    {
        for(Enumeration e = targets.elements(); e.hasMoreElements(); ) {
            Target target = (Target)e.nextElement();

            if(target instanceof ClassTarget)
                ((ClassTarget)target).removeBreakpoints();
        }
    }

    /**
     *  Remove all step marks in all classes.
     */
    public void removeStepMarks()
    {
        for(Enumeration e = targets.elements(); e.hasMoreElements(); ) {
            Target target = (Target)e.nextElement();

            if(target instanceof ClassTarget)
                ((ClassTarget)target).removeStepMark();
        }
    }

    public void addTarget(Target t)
    {
        if(t.getPackage() != this)
            throw new IllegalArgumentException();

        targets.put(t.getIdentifierName(), t);
    }

    public void removeTarget(Target t)
    {
        targets.remove(t.getIdentifierName());
    }

    /**
     * Changes the Target identifier. Targets are stored in a hashtable
     * with their name as the key.  If class name changes we need to
     * remove the target and add again with the new key.
     */
/*XXX should we make target identifiers immutable and require a new target to
  be constructed to do this rename??
    public void updateTargetIdentifier(Target t, String newIdentifier)
    {
        if(t == null || newIdentifier == null) {
            Debug.reportError("cannot properly update target name...");
            return;
        }
        targets.remove(t.getIdentifierName());
        targets.put(newIdentifier, t);
    }
*/
    /**
     *  Removes a class from the Package
     *
     *  @param removableTarget   the ClassTarget representing the class to
     *				 be removed.
     */
    public void removeClass(Target removableTarget)
    {
        if(removableTarget instanceof ClassTarget)
            ((ClassTarget)removableTarget).prepareForRemoval();

        removeTarget(removableTarget);
        getEditor().repaint();
        save();
    }

    /**
     *  Add a dependancy in this package. The dependency is also added to the
     *  individual targets involved.
     */
    public void addDependency(Dependency d, boolean recalc)
    {
        Target from = (Target)d.getFrom();
        Target to = (Target)d.getTo();

        if(from == null || to == null) {
            Debug.reportError("Found invalid dependency - ignored.");
            return;
        }

        if(d instanceof UsesDependency) {
            int index = usesArrows.indexOf(d);
            if(index != -1) {
                ((UsesDependency)usesArrows.get(index)).setFlag(true);
                return;
            }
            else
                usesArrows.addElement(d);
        }
        else {
            if(extendsArrows.contains(d))
                return;
            else
                extendsArrows.addElement(d);
        }

        from.addDependencyOut(d, recalc);
        to.addDependencyIn(d, recalc);

    }

    /**
     * A user initiated addition of an "implements" clause from a class to
     * an interface
     *
     * @pre d.getFrom() and d.getTo() are both instances of ClassTarget
     */
    public void userAddImplementsClassDependency(Dependency d)
    {
        ClassTarget from = (ClassTarget)d.getFrom();    // a class
        ClassTarget to = (ClassTarget)d.getTo();        // an interface
        Editor ed = from.getEditor();

        // Debug.message("Implements class dependency from " + from.getName() + " to " + to.getName());

        try {
            ClassInfo info = ClassParser.parse(from.getSourceFile(), getAllClassnames());

            Selection s1 = info.getImplementsInsertSelection();
            ed.setSelection(s1.getLine(), s1.getColumn(), s1.getLength());

            if (info.hasInterfaceSelections()) {
                // if we already have an implements clause then we need to put a
                // comma and the interface name but not before checking that we don't
                // already have it

                Vector exists = info.getInterfaceTexts();

                // XXX make this equality check against full package name
                if(!exists.contains(to.getBaseName()))
                    ed.insertText(", " + to.getBaseName(), false, false);
            } else {
                // otherwise we need to put the actual "implements" word
                // and the interface name
                ed.insertText(" implements " + to.getBaseName(), false, false);
            }
            ed.save();
        }
        catch(Exception e) {
            // exception during parsing so we have to ignore
            // perhaps we should display a message here
            return;
        }
    }

    /**
     * A user initiated addition of an "extends" clause from an interface to
     * an interface
     *
     * @pre d.getFrom() and d.getTo() are both instances of ClassTarget
     */
    public void userAddImplementsInterfaceDependency(Dependency d)
    {
        ClassTarget from = (ClassTarget)d.getFrom();    // an interface
        ClassTarget to = (ClassTarget)d.getTo();        // an interface
        Editor ed = from.getEditor();

        // Debug.message("Implements interface dependency from " + from.getName() + " to " + to.getName());

        try {
            ClassInfo info = ClassParser.parse(from.getSourceFile(), getAllClassnames());

            Selection s1 = info.getExtendsInsertSelection();
            ed.setSelection(s1.getLine(), s1.getColumn(), s1.getLength());

            if (info.hasInterfaceSelections()) {
                // if we already have an extends clause then we need to put a
                // comma and the interface name but not before checking that we don't
                // already have it

                Vector exists = info.getInterfaceTexts();

                // XXX make this equality check against full package name
                if(!exists.contains(to.getBaseName()))
                    ed.insertText(", " + to.getBaseName(), false, false);
            } else {
                // otherwise we need to put the actual "extends" word
                // and the interface name
                ed.insertText(" extends " + to.getBaseName(), false, false);
            }
            ed.save();
        }
        catch(Exception e) {
            // exception during parsing so we have to ignore
            // perhaps we should display a message here
            return;
        }
    }

    /**
     * A user initiated addition of an "extends" clause from a class to
     * a class
     *
     * @pre d.getFrom() and d.getTo() are both instances of ClassTarget
     */
    public void userAddExtendsClassDependency(Dependency d)
    {
        ClassTarget from = (ClassTarget)d.getFrom();
        ClassTarget to = (ClassTarget)d.getTo();
        Editor ed = from.getEditor();

        try {
            ClassInfo info = ClassParser.parse(from.getSourceFile(), getAllClassnames());

            if (info.getSuperclass() == null) {
                Selection s1 = info.getExtendsInsertSelection();

                ed.setSelection(s1.getLine(), s1.getColumn(), s1.getLength());
                ed.insertText(" extends " + to.getBaseName(), false, false);
            } else {
                Selection s1 = info.getSuperReplaceSelection();

                ed.setSelection(s1.getLine(), s1.getColumn(), s1.getLength());
                ed.insertText(to.getBaseName(), false, false);
            }
            ed.save();
        }
        catch(Exception e) {
            // exception during parsing so we have to ignore
            // perhaps we should display a message here
            return;
        }
    }

    /**
     * A user initiated removal of a dependency
     *
     * @pre d is an instance of an Implements or Extends dependency
     */
    public void userRemoveDependency(Dependency d)
    {
        // if they are not both classtargets then I don't want to know about it
        if (!(d.getFrom() instanceof ClassTarget) ||
            !(d.getTo() instanceof ClassTarget))
            return;

        ClassTarget from = (ClassTarget)d.getFrom();
        ClassTarget to = (ClassTarget)d.getTo();
        Editor ed = from.getEditor();

        try {
            ClassInfo info = ClassParser.parse(from.getSourceFile(), getAllClassnames());
            Selection s1 = null;
            Selection s2 = null;               // set to the selections we wish to delete
            Selection sinsert = null;          // our selection if we want to insert something
            String sinserttext = "";

            if(d instanceof ImplementsDependency)
                {
                    Vector vsels, vtexts;

                    if(info.isInterface())
                        {
                            vsels = info.getInterfaceSelections();
                            vtexts = info.getInterfaceTexts();
                            sinserttext = "extends ";
                        } else {
                            vsels = info.getInterfaceSelections();
                            vtexts = info.getInterfaceTexts();
                            sinserttext = "implements ";
                        }

                    int where = vtexts.indexOf(to.getBaseName());

                    if (where > 0)              // should always be true
                        {
                            s1 = (Selection)vsels.get(where-1);
                            s2 = (Selection)vsels.get(where);
                        }
                    // we have a special case if we deleted the first bit of an "implements"
                    // clause, yet there are still clauses left.. we have to replace the ","
                    // with "implements" (note that there must already be a leading space so we
                    // do not need to insert one but we may need a trailing space)
                    if(where == 1 && vsels.size() > 2) {
                        sinsert = (Selection)vsels.get(where+1);
                    }
                }
            else if(d instanceof ExtendsDependency)
                {
                    // a class extends
                    s1 = info.getExtendsReplaceSelection();
                    s2 = info.getSuperReplaceSelection();
                }

            // delete (maybe insert) text from the end backwards so that our line/col positions
            // for s1 are not mucked up by the deletion
            if(sinsert != null) {
                ed.setSelection(sinsert.getLine(), sinsert.getColumn(), sinsert.getLength());
                ed.insertText(sinserttext, false, false);
            }
            if(s2 != null) {
                ed.setSelection(s2.getLine(), s2.getColumn(), s2.getLength());
                ed.insertText("", false, false);
            }
            if(s1 != null) {
                ed.setSelection(s1.getLine(), s1.getColumn(), s1.getLength());
                ed.insertText("", false, false);
            }

            ed.save();
        }
        catch(Exception e) {
            // exception during parsing so we have to ignore
            // perhaps we should display a message here
            e.printStackTrace();
            Debug.message("Parse error attempting to delete dependency arrow");
            return;
        }
    }


    /**
     *  Remove a dependancy from this package. The dependency is also removed
     *  from the individual targets involved.
     */
    public void removeDependency(Dependency d, boolean recalc)
    {
        if(d instanceof UsesDependency)
            usesArrows.removeElement(d);
        else
            extendsArrows.removeElement(d);

        Target from = (Target)d.getFrom();
        from.removeDependencyOut(d, recalc);

        Target to = (Target)d.getTo();
        to.removeDependencyIn(d, recalc);
    }

    public void recalcArrows()
    {
        Enumeration e = getVertices();
        while(e.hasMoreElements()) {
            Target t = (Target)e.nextElement();

            t.recalcInUses();
            t.recalcOutUses();
        }
    }

    public void setActiveVertex(Vertex v)
    {
        if(selected != null)
            selected.toggleFlag(Target.F_SELECTED);
        selected = (Target)v;
        if(selected != null) {
            // XXX: currently broken
            // int index = targets.indexOf(selected);
            // int last = targets.size() - 1;
            // Swap selected vertex with top
            // targets.setElementAt(targets.elementAt(last), index);
            // targets.setElementAt(selected, last);

            selected.toggleFlag(Target.F_SELECTED);
        }
    }

    /**
     * Return the target with name "tname".
     * @param tname the unqualified (or base) name of a target.
     * @return the target with name "tname" if existent, null otherwise.
     */
    public Target getTarget(String tname)
    {
        if(tname == null)
            return null;
        Target t = (Target)targets.get(tname);
        return t;
    }

    /**
     * Return a vector of Strings with names of all classes and packages
     * in this package.
     */
    public Vector getAllClassnames()
    {
        Vector names = new Vector();

        for(Enumeration e = targets.elements(); e.hasMoreElements(); ) {
            Target t = (Target)e.nextElement();

            if(t instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget)t;
                names.add(ct.getBaseName());
            }
        }
        return names;
    }

    /**
     * Given a file name, find the target that represents that file.
     *
     * @return The target with the given file name or <null> if not found.
     */
    public ClassTarget getTargetFromFilename(String filename)
    {
        for(Enumeration e = targets.elements(); e.hasMoreElements(); )
            {
                Target t = (Target)e.nextElement();
                if(!(t instanceof ClassTarget))
                    continue;

                ClassTarget ct = (ClassTarget)t;

                if(filename.equals(ct.getSourceFile().getPath()))
                    return ct;
            }

        return null;
    }

    public EditableTarget getTargetFromEditor(Editor editor)
    {
        for(Enumeration e = targets.elements(); e.hasMoreElements(); )
            {
                Target t = (Target)e.nextElement();
                if(!(t instanceof EditableTarget))
                    continue;

                EditableTarget et = (EditableTarget)t;

                if(et.usesEditor(editor))
                    return et;
            }

        return null;
    }

    public boolean toggleShowUses()
    {
        showUses = !showUses;
        return showUses;
    }

    public boolean toggleShowExtends()
    {
        showExtends = !showExtends;
        return showExtends;
    }

    public void setState(int state)
    {
        // Clean up after current state, if necessary
        switch(this.state) {
        case S_DELARROW:
            if(currentArrow != null) {
                currentArrow.highlight(getEditor().getGraphics2D());
                currentArrow = null;
            }
            getEditor().removeMouseListener(this);
            getEditor().removeMouseMotionListener(this);
            break;
        }

        this.state = state;

        // Set up new state, if necessary
        switch(this.state) {
        case S_DELARROW:
            getEditor().addMouseListener(this);
            getEditor().addMouseMotionListener(this);
            break;
        }
    }

    public int getState()
    {
        return state;
    }



    /**
     *  Test whether a file instance denotes a BlueJ package directory.
     *  @param f the file instance that is tested for denoting a BlueJ package.
     *  @return true if f denotes a directory and a BlueJ package.
     */
    public static boolean isBlueJPackage(File f)
    {
        if (f == null)
            return false;

        if(!f.isDirectory())
            return false;

        // don't try to test Windows root directories (you'll get in
        // trouble with disks that are not in drives...).

        if(f.getPath().endsWith(":\\"))
            return false;

        File packageFile = new File(f, pkgfileName);
        return (packageFile.exists());
    }

    /**
     * Called when in an interesting state (e.g. adding a new dependency)
     * and a target is selected.
     */
    void targetSelected(Target t)
    {
        switch(getState()) {
        case S_CHOOSE_USES_FROM:
            fromChoice = t;
            setState(S_CHOOSE_USES_TO);
            setStatus(chooseUsesTo);
            break;

        case S_CHOOSE_USES_TO:
            if (t != fromChoice) {
                setState(S_IDLE);
                addDependency(new UsesDependency(this, fromChoice, t), true);
                setStatus("");
            }
            break;

        case S_CHOOSE_EXT_FROM:
            fromChoice = t;
            setState(S_CHOOSE_EXT_TO);
            setStatus(chooseInhTo);
            break;

        case S_CHOOSE_EXT_TO:
            if (t != fromChoice) {
                setState(S_IDLE);
                if(t instanceof ClassTarget && fromChoice instanceof ClassTarget)
                    {
                        ClassTarget from = (ClassTarget)fromChoice;
                        ClassTarget to = (ClassTarget)t;

                        // if the target is an interface then we have an implements
                        // dependency
                        if(to.isInterface())
                            {
                                Dependency d = new ImplementsDependency(this, from, to);

                                if(from.isInterface()) {
                                    userAddImplementsInterfaceDependency(d);
                                } else {
                                    userAddImplementsClassDependency(d);
                                }

                                addDependency(d, true);
                            }
                        else {
                            // an extends dependency can only be from a class to another
                            // class
                            if(!from.isInterface()) {
                                Dependency d = new ExtendsDependency(this, from, to);
                                userAddExtendsClassDependency(d);
                                addDependency(d, true);
                            }
                        }
                    }
                setStatus("");
            }
            break;

        default:
            // e.g. deleting arrow - selecting target ignored
            break;
        }
//XXX        if (getState() == S_IDLE)
//            frame.resetDependencyButtons();
    }

    /**
     * Use the dialog manager to display an error message.
     * The PkgMgrFrame is used to find a parent window so we
     * can correctly offset the dialog.
     */
    public void showError(String msgId)
    {
        PkgMgrFrame.showError(this, msgId);
    }

    /**
     * Use the dialog manager to display a message.
     * The PkgMgrFrame is used to find a parent window so we
     * can correctly offset the dialog.
     */
    public void showMessage(String msgId)
    {
        PkgMgrFrame.showMessage(this, msgId);
    }


    /**
     * Use the dialog manager to display a message with text.
     * The PkgMgrFrame is used to find a parent window so we
     * can correctly offset the dialog.
     */
    public void showMessageWithText(String msgId, String text)
    {
        PkgMgrFrame.showMessageWithText(this, msgId, text);
    }

    /**
     * Report an execption. Usually, we do this through "errorMessage", but
     * if we cannot make sense of the message format, and thus cannot figure
     * out class name and line number, we use this way.
     */
    public void reportException(String text)
    {
        showMessageWithText("exception-thrown", text);
    }

    /**
     * Don't remember the last shown source anymore.
     */
    public void forgetLastSource()
    {
        lastSourceName = "";
    }

    /**
     * A thread has hit a breakpoint or done a step. Organise display
     * (highlight line in source, pop up exec controls).
     */
    public boolean showSource(String sourcename, int lineNo,
                              String threadName, boolean breakpoint)
    {
        String msg = " ";

        if(breakpoint)
            msg = "Thread \"" + threadName + "\" stopped at breakpoint.";

        boolean bringToFront = !sourcename.equals(lastSourceName);
        lastSourceName = sourcename;

        if(! showEditorMessage(new File(getPath(),sourcename).getPath(), lineNo, msg,
                               false, false, bringToFront, true, null))
            showMessageWithText("break-no-source", sourcename);

        return bringToFront;
    }

    /**
     * Display an error message associated with a specific line in a class.
     * This is done by opening the class's source, highlighting the line
     * and showing the message in the editor's information area.
     */
    private boolean showEditorMessage(String filename, int lineNo,
                                      String message, boolean invalidate,
                                      boolean beep, boolean bringToFront,
                                      boolean setStepMark, String help)
    {
        ClassTarget t = getTargetFromFilename(filename);

        if(t == null)
            return false;

        if(invalidate) {
            t.setState(Target.S_INVALID);
            t.unsetFlag(Target.F_QUEUED);
        }

        if(t.getDisplayedView() != Editor.IMPLEMENTATION)
            t.showView(Editor.IMPLEMENTATION);
        else if(bringToFront || !t.getEditor().isShowing())
            t.open();
        Editor editor = t.getEditor();
        if(editor!=null)
            editor.displayMessage(message, lineNo, 0, beep, setStepMark,
                                  help);
        return true;
    }

    // ---- bluej.compiler.CompileObserver interface ----

    /**
     *  A compilation has been started. Mark the affected classes as being
     *  currently compiled.
     */
    public void startCompile(String[] sources)
    {
        setStatus(compiling);
        for(int i = 0; i < sources.length; i++) {
            String filename = sources[i];

            ClassTarget t = getTargetFromFilename(filename);
            if(t != null)
                t.setState(ClassTarget.S_COMPILING);
        }
    }

    /**
     * Display an error message associated with a specific line in a class.
     * This is done by opening the class's source, highlighting the line
     * and showing the message in the editor's information area.
     */
    public void errorMessage(String filename, int lineNo, String message,
                             boolean invalidate)
    {
        if(! showEditorMessage(filename, lineNo, message, invalidate, true,
                               true, false, Config.compilertype))
            showMessageWithText("error-in-file",
                                              filename + ":" + lineNo +
                                              "\n" + message);
    }

    /**
     * Display an exception message. This is almost the same as "errorMessage"
     * except for different help texts.
     */
    public void exceptionMessage(String filename, int lineNo, String message,
                                 boolean invalidate)
    {
        if(! showEditorMessage(filename, lineNo, message, invalidate, true,
                               true, false, "exception"))
            showMessageWithText("error-in-file",
                                              filename + ":" + lineNo +
                                              "\n" + message);
    }

    /**
     *  Compilation has ended.  Mark the affected classes as being
     *  normal again.
     */
    public void endCompile(String[] sources, boolean successful)
    {
        for(int i = 0; i < sources.length; i++) {
            String filename = sources[i];

            ClassTarget t = getTargetFromFilename(filename);

            if (successful) {

                /* compute ctxt files (files with comments and parameters names) */
                try {
                    ClassInfo info = ClassParser.parse(t.getSourceFile().getPath(), getAllClassnames());

                    OutputStream out = new FileOutputStream(t.getContextFile());
                    info.getComments().store(out, "BlueJ class context");
                    out.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            t.setState(successful ? Target.S_NORMAL : Target.S_INVALID);
            t.unsetFlag(Target.F_QUEUED);
            if(successful && t.editorOpen())
                t.getEditor().setCompiled(true);
        }
            setStatus(compileDone);
            getEditor().repaint();

    }

    // ---- end of bluej.compiler.CompileObserver interface ----


    /**
     * Report an exit of a method through "System.exit()" where we expected
     * a result or an object being created.
     */
    public void reportExit(String exitCode)
    {
        showMessageWithText("system-exit", exitCode);
    }

    /**
     * Closes all currently open editors within package.
     */
    private synchronized ClassLoader getLocalClassLoader()
    {
        if(loader == null)
            loader = ClassMgr.getLoader(getDirName());

        return loader;
    }

    /**
     * removeLocalClassLoader - removes the current classloader, and
     *  removes references to classes loaded by it (this includes removing
     *  the objects from the object bench).
     *  Should be run whenever a source file changes
     */
    synchronized void removeLocalClassLoader()
    {
        if(loader != null) {
            // remove objects loaded by this classloader
            ObjectBench bench = getBench();
            ObjectWrapper[] wrappers = bench.getWrappers();
            for(int i = 0; i < wrappers.length; i++) {
                if(wrappers[i].getPackage() == this)
                    bench.remove(wrappers[i], getId());
            }

            // XXX: remove views for classes loaded by this classloader

            loader = null;
        }
    }

    /**
     * getDebuggerClassLoader - get the DebuggerClassLoader for this
     *  package. The DebuggerClassLoader load classes on the remote VM
     *  (the machine used for user code execution).
     */
    public synchronized DebuggerClassLoader getRemoteClassLoader()
    {
        if(debuggerLoader == null)
            debuggerLoader = Debugger.debugger.createClassLoader(getId(), getDirName());
        return debuggerLoader;
    }

    /**
     * removeRemoteClassLoader - removes the remote VM classloader
     *  Should be run whenever a source file changes
     */
    synchronized void removeRemoteClassLoader()
    {
        if(debuggerLoader != null) {
            Debugger.debugger.removeClassLoader(debuggerLoader);
            debuggerLoader = null;
        }
    }

    /**
     * loadClass - loads a class using the current classLoader
     * creates a classloader if none currently exists
     */
    public Class loadClass(String className)
    {
        try {
            return getLocalClassLoader().loadClass(className);
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * closeAllEditors - closes all currently open editors within package
     * Should be run whenever a package is removed from PkgFrame.
     */
    public void closeAllEditors()
    {
        for(Enumeration e = targets.elements(); e.hasMoreElements(); )
        {
            Target t = (Target)e.nextElement();
            if(t instanceof ClassTarget)
            {
                ClassTarget ct = (ClassTarget)t;
                if(ct.editorOpen())
                    ct.getEditor().close();
            }
        }
    }

    /**
     * get history of invocation calls
     * @return CallHistory object
     */
    public CallHistory getCallHistory()
    {
        return callHistory;
    }

    /**
     * Called after a change to a Target
     */
    public void invalidate(Target t)
    {
        if(t instanceof ClassTarget) {
            ClassTarget ct = (ClassTarget)t;

            getProject().removeLocalClassLoader();
            getProject().removeRemoteClassLoader();
        }
    }

    /**
     * find an arrow, given a point on the screen
     */
    Dependency findArrow(int x, int y)
    {
        for(Enumeration e = usesArrows.elements(); e.hasMoreElements(); ) {
            Dependency d = (Dependency)e.nextElement();
            if(d.contains(x, y))
                return d;
        }

        for(Enumeration e = extendsArrows.elements(); e.hasMoreElements(); ) {
            Dependency d = (Dependency)e.nextElement();
            if(d.contains(x, y))
                return d;
        }

        return null;
    }

    // MouseListener interface

    public void mousePressed(MouseEvent evt)
    {
        switch(state) {
        case S_DELARROW:
            Dependency selectedArrow = findArrow(evt.getX(), evt.getY());
            if((currentArrow != null) && (currentArrow != selectedArrow))
                currentArrow.highlight(getEditor().getGraphics2D());
            if(selectedArrow != null)
                {

                    if (!(selectedArrow instanceof UsesDependency))
                        {
                            userRemoveDependency(selectedArrow);
                        }
                    removeDependency(selectedArrow, true);
                    getEditor().repaint();
                }
            currentArrow = null;
            setState(S_IDLE);
            break;
        }
    }

    public void mouseReleased(MouseEvent evt) {}
    public void mouseClicked(MouseEvent evt) {}
    public void mouseEntered(MouseEvent evt) {}
    public void mouseExited(MouseEvent evt) {}

    // MouseMotionListener interface

    public void mouseDragged(MouseEvent evt) {}

    public void mouseMoved(MouseEvent evt)
    {
        switch(state) {
        case S_DELARROW:	// currently deleting an arrow
            Dependency selectedArrow = findArrow(evt.getX(), evt.getY());
            if((currentArrow != null) && (currentArrow != selectedArrow))
                currentArrow.highlight(getEditor().getGraphics2D());
            if((selectedArrow != null) && (currentArrow != selectedArrow))
                selectedArrow.highlight(getEditor().getGraphics2D());
            currentArrow = selectedArrow;
            break;
        }
    }
}
