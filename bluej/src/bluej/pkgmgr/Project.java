package bluej.pkgmgr;

import bluej.Boot;
import bluej.Config;

import bluej.classmgr.BPClassLoader;
import bluej.classmgr.ClassMgr;
import bluej.classmgr.ClassPath;
import bluej.classmgr.ProjectClassLoader;

import bluej.debugger.*;

import bluej.debugmgr.ExecControls;
import bluej.debugmgr.ExpressionInformation;

import bluej.debugmgr.inspector.*;

import bluej.extmgr.ExtensionsManager;

import bluej.prefmgr.PrefMgr;

import bluej.terminal.Terminal;

import bluej.testmgr.record.ClassInspectInvokerRecord;
import bluej.testmgr.record.InvokerRecord;

import bluej.utility.*;

import bluej.views.View;

import java.awt.EventQueue;

import java.io.File;
import java.io.IOException;

import java.net.URL;

import java.util.*;

import javax.swing.JFrame;


/**
 * A BlueJ Project.
 *
 * @author  Michael Kolling
 * @author  Axel Schmolitzky
 * @author  Andrew Patterson
 * @author  Bruce Quig
 * @version $Id: Project.java 3470 2005-07-18 13:49:30Z damiano $
 */
public class Project implements DebuggerListener {
    /**
     * Collection of all open projects. the canonical name of the project
     * directory is used as the key.
     */
    private static Map projects = new HashMap();
    public static final int NEW_PACKAGE_DONE = 0;
    public static final int NEW_PACKAGE_EXIST = 1;
    public static final int NEW_PACKAGE_BAD_NAME = 2;
    public static final int NEW_PACKAGE_NO_PARENT = 3;

    /* ------------------- end of static declarations ------------------ */

    // instance fields

    /** the path of the project directory. */
    private File projectDir;

    /** collection of open packages in this project
      (indexed by the qualifiedName of the package).
       The unnamed package ie root package of the package tree
       can be obtained by retrieving "" from this collection */
    private Map packages;

    /** a ClassLoader for the local virtual machine */
    private ProjectClassLoader loader;

    /** the debugger for this project */
    private Debugger debugger;

    /** the ExecControls for this project */
    private ExecControls execControls = null;

    /** the Terminal for this project */
    private Terminal terminal = null;

    /** the documentation generator for this project. */
    private DocuGenerator docuGenerator;

    /** when a project is opened, the user may specify a
       directory deep into the projects directory structure.
       BlueJ will correctly find the top of this package
       heirarchy but the bit of the name left over will be
       put into this variable
       ie if opening /home/user/foo/com/sun where
       /home/user/foo is the project directory, this variable
       will be set to com.sun */
    private String initialPackageName = "";

    /** This holds all object inspectors and class inspectors
        for a project. It should only hold object inspectors that
        have a wrapper on the object bench. Inspectors of fields of
        object inspectors should be handled at the object wrapper level */
    private Map inspectors;
    private boolean inTestMode = false;
    private BPClassLoader currentClassLoader;
    private URL[] currentUrls; // will be removed at the end of class loading refactoring.
    private boolean compileStarted; // Used to decide if to generate a new ClassLoader.

    /* ------------------- end of field declarations ------------------- */

    /**
     * Construct a project in the directory projectDir.
     * This must contain the root bluej.pkg of a nested package
     * (this should by its nature be the unnamed package).
     */
    private Project(File projectDir) {
        if (projectDir == null) {
            throw new NullPointerException();
        }

        this.projectDir = projectDir;
        inspectors = new HashMap();
        packages = new TreeMap();

        try {
            packages.put("", new Package(this));
        } catch (IOException exc) {
            Debug.reportError("could not read package file (unnamed package)");
        }

        debugger = Debugger.getDebuggerImpl(getProjectDir(), getTerminal());
        debugger.addDebuggerListener(this);
        debugger.launch();

        docuGenerator = new DocuGenerator(this);
    }

    /**
     * Check if the path given is either a directory with a bluej pkg file or
     * the name of a bluej pkg file.
     *
     * @param projectPath
     *            a string representing the path to check. This can either be a
     *            directory name or the filename of a bluej.pkg file.
     */
    public static boolean isProject(String projectPath) {
        File startingDir;

        try {
            startingDir = pathIntoStartingDirectory(projectPath);
        } catch (IOException ioe) {
            return false;
        }

        if (startingDir == null) {
            return false;
        }

        return (Package.isBlueJPackage(startingDir));
    }

    /**
     * Open a BlueJ project.
     *
     * @param projectPath
     *            a string representing the path to open. This can either be a
     *            directory name or the filename of a bluej.pkg file.
     * @return the Project representing the BlueJ project that has this
     *         directory within it or null if there were no bluej.pkg files in
     *         the specified directory.
     */
    public static Project openProject(String projectPath) {
        String startingPackageName;
        File projectDir;
        File startingDir;

        try {
            startingDir = pathIntoStartingDirectory(projectPath);
        } catch (IOException ioe) {
            Debug.message("could not resolve directory " + projectPath);

            //ioe.printStackTrace();
            return null;
        }

        if (startingDir == null) {
            // Debug.message("attempt to open " + projectPath + " as a project failed");
            return null;
        }

        // if there is an existing bluej package file here we
        // need to find the root directory of the project
        // (and while we are at it we will construct the qualified
        //  package name that lets us open the PkgMgrFrame at the
        //  right point)
        if (Package.isBlueJPackage(startingDir)) {
            File curDir = startingDir;
            File lastDir = null;

            startingPackageName = "";

            while ((curDir != null) && Package.isBlueJPackage(curDir)) {
                if (lastDir != null) {
                    String lastdirName = lastDir.getName();

                    if (!JavaNames.isIdentifier(lastdirName)) {
                        break;
                    }

                    startingPackageName = "." + lastdirName +
                        startingPackageName;
                }

                lastDir = curDir;
                curDir = curDir.getParentFile();
            }

            if (startingPackageName.length() > 0) {
                if (startingPackageName.charAt(0) == '.') {
                    startingPackageName = startingPackageName.substring(1);
                }
            }

            // lastDir is now the directory holding the topmost bluej
            // package file in the directory heirarchy
            projectDir = lastDir;

            if (projectDir == null) {
                projectDir = startingDir;
            }
        } else {
            // Debug.message("no BlueJ package file found in directory " + startingDir);
            return null;
        }

        // check whether it already exists
        Project proj = (Project) projects.get(projectDir);

        if (proj == null) {
            proj = new Project(projectDir);
            projects.put(projectDir, proj);
        }

        if (startingPackageName.equals("")) {
            Package startingPackage = proj.getPackage("");

            while (startingPackage != null) {
                Package sub = startingPackage.getBoringSubPackage();

                if (sub == null) {
                    break;
                }

                startingPackage = sub;
            }

            proj.initialPackageName = startingPackage.getQualifiedName();
        } else {
            proj.initialPackageName = startingPackageName;
        }

        ExtensionsManager.getInstance().projectOpening(proj);

        return proj;
    }

    /**
     * Remove a project from the collection of currently open projects.
     */
    public static void closeProject(Project project) {
        PkgMgrFrame[] frames = PkgMgrFrame.getAllProjectFrames(project);

        if (frames != null) {
            for (int i = 0; i < frames.length; i++) {
                frames[i].doClose(true);
            }
        }

        // closing the last frame will cause a call to 'cleanUp'
    }

    /**
     * CleanUp the mess left by a project that has now been closed and
     * throw it away.
     */
    public static void cleanUp(Project project) {
        if (project.hasExecControls()) {
            project.getExecControls().dispose();
        }

        if (project.terminal != null) {
            project.terminal.dispose();
        }

        project.removeAllInspectors();
        project.getDebugger().removeDebuggerListener(project);
        project.getDebugger().close(false);

        PrefMgr.addRecentProject(project.getProjectDir().getAbsolutePath());
        projects.remove(project.getProjectDir());
    }

    /**
     * Create a new project in the directory specified by projectPath.
     * This name must be a directory that does not already exist
     *
     * @param   projectPath     a string representing the path in which
     *                          to make the new project
     * @return                  a boolean indicating success or failure
     */
    public static boolean createNewProject(String projectPath) {
        if (projectPath != null) {
            // check whether name is already in use
            File dir = new File(projectPath);

            if (dir.exists()) {
                return false;
            }

            if (dir.mkdir()) {
                File newpkgFile = new File(dir, Package.pkgfileName);
                File newreadmeFile = new File(dir, Package.readmeName);

                try {
                    if (newpkgFile.createNewFile()) {
                        if (FileUtility.copyFile(Config.getTemplateFile(
                                        "readme"), newreadmeFile)) {
                            return true;
                        } else {
                            Debug.message("could not copy readme template");
                        }
                    }
                } catch (IOException ioe) {
                }
            }
        }

        return false;
    }

    /**
     * Returns the number of open projects
     */
    public static int getOpenProjectCount() {
        return projects.size();
    }

    /**
     * Gets the set of currently open projects. It is an accessor only
     * @return a Set containing all open projects.
     */
    public static Collection getProjects() {
        return projects.values();
    }

    /**
     * Given a Projects key returns the Project objects describing this projects.
     */
    public static Project getProject(Object projectKey) {
        return (Project) projects.get(projectKey);
    }

    /**
     * workaround method to get a project from the project list.
     * Added so that if only one project is open you could call this
     * to access that project.  This was added to allow an inspector window
     * created from within the debugger to access custom inspectors for a
     * project.
     * @return an open project (may return null if no projects open)
     *
     */
    public static Project getProject() {
        if (projects.size() == 1) {
            Collection projectColl = projects.values();
            Iterator it = projectColl.iterator();

            if (it.hasNext()) {
                return (Project) it.next();
            }
        }

        return null;
    }

    /**
     * Helper function to take a path (either a directory or a file)
     * and return either the canonical path to the directory
     * (in the case of a bluej.pkg file passed in, return the directory containing
     * the file. Returns null if file is not a bluej.pkg file or if the
     * directory/file does not exist.
     */
    private static File pathIntoStartingDirectory(String projectPath)
        throws IOException {
        File startingDir;

        startingDir = new File(projectPath).getCanonicalFile();

        if (startingDir.isDirectory()) {
            return startingDir;
        }

        /* allow a bluej.pkg file to be specified. In this case,
           we immediately find the parent directory and use that as the
           starting directory */
        if (startingDir.isFile()) {
            if (startingDir.getName().equals(Package.pkgfileName)) {
                return startingDir.getParentFile();
            }
        }

        return null;
    }

    /**
     * Return an ObjectInspector for an object.
     *
     * @param obj
     *            The object displayed by this viewer
     * @param name
     *            The name of this object or "null" if the name is unobtainable
     * @param pkg
     *            The package all this belongs to
     * @param ir
     *            the InvokerRecord explaining how we got this result/object if
     *            null, the "get" button is permanently disabled
     * @param parent
     *            The parent frame of this frame
     * @return The Viewer value
     */
    public ObjectInspector getInspectorInstance(DebuggerObject obj,
        String name, Package pkg, InvokerRecord ir, JFrame parent) {
        ObjectInspector inspector = (ObjectInspector) inspectors.get(obj);

        if (inspector == null) {
            inspector = new ObjectInspector(obj, this, name, pkg, ir, parent);
            inspectors.put(obj, inspector);
        }

        final ObjectInspector insp = inspector;
        EventQueue.invokeLater(new Runnable() {
                public void run() {
                    insp.update();
                    insp.setVisible(true);
                    insp.bringToFront();
                }
            });

        return inspector;
    }

    /**
     * Get the inspector
     * @param obj
     * @return the inspector
     */
    public Inspector getInspector(Object obj) {
        return (Inspector) inspectors.get(obj);
    }

    /**
     * Remove an inspector from the list of inspectors for this project
     * @param obj the inspector
     */
    public void removeInspector(Object obj) {
        inspectors.remove(obj);
    }

    /**
     * Removes an inspector instance from the collection of inspectors
     * for this project. It firstly retrieves the inspector object and
     * then calls it's doClose method to
     * @param obj
     */
    public void removeInspectorInstance(Object obj) {
        Inspector inspect = getInspector(obj);

        if (inspect != null) {
            inspect.doClose();
        }
    }

    /**
     * Removes all inspector instances for this project.
     * This is used when VM is reset or the project is recompiled.
     *
     */
    public void removeAllInspectors() {
        for (Iterator it = inspectors.values().iterator(); it.hasNext();) {
            Inspector inspector = (Inspector) it.next();
            inspector.setVisible(false);
            inspector.dispose();
        }

        inspectors.clear();
    }

    /**
     * Return a ClassInspector for a class. The inspector is visible.
     *
     * @param clss
     *            The class displayed by this viewer
     * @param name
     *            The name of this object or "null" if it is not on the object
     *            bench
     * @param pkg
     *            The package all this belongs to
     * @param getEnabled
     *            if false, the "get" button is permanently disabled
     * @param parent
     *            The parent frame of this frame
     * @return The Viewer value
     */
    public ClassInspector getClassInspectorInstance(DebuggerClass clss,
        Package pkg, JFrame parent) {
        ClassInspector inspector = (ClassInspector) inspectors.get(clss.getName());

        if (inspector == null) {
            ClassInspectInvokerRecord ir = new ClassInspectInvokerRecord(clss.getName());
            inspector = new ClassInspector(clss, this, pkg, ir, parent);
            inspectors.put(clss.getName(), inspector);
        }

        final Inspector insp = inspector;
        EventQueue.invokeLater(new Runnable() {
                public void run() {
                    insp.update();
                    insp.setVisible(true);
                    insp.bringToFront();
                }
            });

        return inspector;
    }

    /**
     * Return an ObjectInspector for an object. The inspector is visible.
     *
     * @param obj
     *            The object displayed by this viewer
     * @param name
     *            The name of this object or "null" if the name is unobtainable
     * @param pkg
     *            The package all this belongs to
     * @param ir
     *            the InvokerRecord explaining how we got this result/object if
     *            null, the "get" button is permanently disabled
     * @param info
     *            The information about the the expression that gave this result
     * @param parent
     *            The parent frame of this frame
     * @return The Viewer value
     */
    public ResultInspector getResultInspectorInstance(DebuggerObject obj,
        String name, Package pkg, InvokerRecord ir, ExpressionInformation info,
        JFrame parent) {
        ResultInspector inspector = (ResultInspector) pkg.getProject()
                                                         .getInspector(obj);

        if (inspector == null) {
            inspector = new ResultInspector(obj, this, name, pkg, ir, info,
                    parent);
            inspectors.put(obj, inspector);
        }

        final ResultInspector insp = inspector;
        insp.update();
        EventQueue.invokeLater(new Runnable() {
                public void run() {
                    insp.setVisible(true);
                    insp.bringToFront();
                }
            });

        return inspector;
    }

    /**
     * Iterates through all inspectors and updates them
     *
     */
    public void updateInspectors() {
        for (Iterator it = inspectors.values().iterator(); it.hasNext();) {
            Inspector inspector = (Inspector) it.next();
            inspector.update();
        }
    }

    /**
     * Return the name of the project.
     */
    public String getProjectName() {
        return projectDir.getName();
    }

    /**
     * Return the location of the project.
     */
    public File getProjectDir() {
        return projectDir;
    }

    /**
     * Return whether the project is located in a readonly directory
     * @return
     */
    public boolean isReadOnly() {
        return !projectDir.canWrite();
    }

    /**
     * A string which uniquely identifies this project
     */
    public String getUniqueId() {
        return String.valueOf(new String("BJID" + getProjectDir().getPath()).hashCode());
    }

    /**
     * The name of the package within the project directory where we first opened
     * this project.
     */
    public String getInitialPackageName() {
        return initialPackageName;
    }

    /**
     * Returns or creates a package (tree) in this project.
     * It will construct the package if it needs or return it from the cache.
     * All parent packages on the way to the root of the package tree will also be constructed.
     * This method does not check if the user really wanted to create parent packages.
     * This method assumes that package directory are already set up.
     *
     * @param qualifiedName package name ie java.util or "" for unnamed package
     */
    public Package getPackage(String qualifiedName) {
        Package existing = (Package) packages.get(qualifiedName);

        if (existing != null) {
            return existing;
        }

        if (qualifiedName.length() > 0) // should always be true (the unnamed package
                                        // always exists in the package collection)
         {
            Package pkg;

            try {
                Package parent = getPackage(JavaNames.getPrefix(qualifiedName));

                if (parent != null) {
                    pkg = new Package(this, JavaNames.getBase(qualifiedName),
                            parent);
                    packages.put(qualifiedName, pkg);
                } else { // parent package does not exist. How can it not exist ?
                    pkg = null;
                }
            } catch (IOException exc) {
                // the package did not exist in this project
                pkg = null;
            } catch (IllegalArgumentException iae) {
                iae.printStackTrace();
                pkg = null;
            }

            return pkg;
        }

        throw new IllegalStateException("Project.getPackage()");
    }

    /**
     * Returns a package from the project.
     *
     * @param qualifiedName package name ie java.util or "" for unnamed package
     * @return null if the named package cannot be found
     */
    public Package getCachedPackage(String qualifiedName) {
        return (Package) packages.get(qualifiedName);
    }

    /**
     * This creates package directories.
     */
    public void createPackageDirectory(String fullName) {
        // construct the directory name for the new package
        StringTokenizer st = new StringTokenizer(fullName, ".");
        File newPkgDir = getProjectDir();

        while (st.hasMoreTokens())
            newPkgDir = new File(newPkgDir, st.nextToken());

        // now actually construct the directories and add the bluej
        // package marker files
        if (newPkgDir.isDirectory() || newPkgDir.mkdirs()) {
            st = new StringTokenizer(fullName, ".");
            newPkgDir = getProjectDir();

            File newPkgFile = new File(newPkgDir, Package.pkgfileName);

            try {
                newPkgFile.createNewFile();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            while (st.hasMoreTokens()) {
                newPkgDir = new File(newPkgDir, st.nextToken());
                newPkgFile = new File(newPkgDir, Package.pkgfileName);

                try {
                    newPkgFile.createNewFile();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    /**
     * Returns a new package with the given fully qualified name. Once
     * NEW_PACKAGE_DONE is returned you can use getPackage to get the actual
     * package.
     *
     * @param qualifiedName
     *            Ex. java.util or "" for unnamed package
     * @return Project.NEW_PACKAGE_DONE, Project.NEW_PACKAGE_EXIST,
     *         Project.NEW_PACKAGE_BAD_NAME
     */
    public int newPackage(String qualifiedName) {
        if (qualifiedName == null) {
            return NEW_PACKAGE_BAD_NAME;
        }

        Package existing = (Package) packages.get(qualifiedName);

        if (existing != null) {
            return NEW_PACKAGE_EXIST;
        }

        // The zero len (unqualified) package should always exist.
        if (qualifiedName.length() < 1) {
            return NEW_PACKAGE_BAD_NAME;
        }

        // The above named package does not exist, lets create it.
        try {
            Package parent = getPackage(JavaNames.getPrefix(qualifiedName));

            if (parent == null) {
                return NEW_PACKAGE_NO_PARENT;
            }

            // Before creating the package you have to create the directory
            // Maybe it should go into the new Package(...)
            createPackageDirectory(qualifiedName);

            Package pkg = new Package(this, JavaNames.getBase(qualifiedName),
                    parent);
            packages.put(qualifiedName, pkg);
        } catch (IOException exc) {
            return NEW_PACKAGE_BAD_NAME;
        }

        return NEW_PACKAGE_DONE;
    }

    /**
     * Returns the existing package with the given fully qualified name, or
     * null if it doesn't exist.
     *
     * @param packageName   The fully qualified name of the package to look for
     * @return    The requested package (or null)
     */
    public Package getExistingPackage(String packageName) {
        Package r = getPackage(""); // start at root package

        while (packageName.length() != 0) {
            // Get the next component in the fully qualified package name.
            String nextName;
            int firstPos = packageName.indexOf('.');

            if (firstPos == -1) {
                nextName = packageName;
                packageName = "";
            } else {
                nextName = packageName.substring(0, firstPos);
                packageName = packageName.substring(firstPos + 1);
            }

            // Search the children of the current package to find the next
            // component.
            List children = r.getChildren();
            Iterator i = children.iterator();
            Package child = null;

            while (i.hasNext()) {
                child = (Package) i.next();

                if (child.getBaseName().equals(nextName)) {
                    break;
                }
            }

            if (child == null) {
                return null;
            }

            r = child;
        }

        return r;
    }

    /**
     * Get the names of all packages in this project consisting of rootPackage
     * package and all packages nested below it.
     *
     * @param rootPackage
     *            the root package to consider in looking for nested packages
     * @return an array of String containing the fully qualified names of the
     *         packages.
     */
    private List getPackageNames(Package rootPackage) {
        List l = new LinkedList();
        List children;

        l.add(rootPackage.getQualifiedName());

        children = rootPackage.getChildren();

        if (children != null) {
            Iterator i = children.iterator();

            while (i.hasNext()) {
                Package p = (Package) i.next();
                l.addAll(getPackageNames(p));
            }
        }

        return l;
    }

    /**
     * Get the names of all packages in this project.
     * @return an array of String containing the fully qualified names
     * of the packages in this project.
     */
    public List getPackageNames() {
        return getPackageNames(getPackage(""));
    }

    /**
     * Generate documentation for the whole project.
     * @return "" if everything was alright, an error message otherwise.
     */
    public String generateDocumentation() {
        return docuGenerator.generateProjectDocu();
    }

    public String getDocumentationFile(String filename) {
        return docuGenerator.getDocuPath(filename);
    }

    /**
    * Generate the documentation for the file in 'filename'
     * @param filename
     */
    public void generateDocumentation(String filename) {
        docuGenerator.generateClassDocu(filename);
    }

    /**
     * Save all open packages of this project.
     */
    public void saveAll() {
        PkgMgrFrame[] frames = PkgMgrFrame.getAllProjectFrames(this);

        // Shurely we do not want to stack trace if nothing exists. Damiano
        if (frames == null) {
            return;
        }

        for (int i = 0; i < frames.length; i++) {
            frames[i].doSave();
        }
    }

    /**
     * Reload all constructed packages of this project.
     *
     * This function is used after a major change to the contents
     * of the project directory ie an import.
     */
    public void reloadAll() {
        Iterator i = packages.values().iterator();

        while (i.hasNext()) {
            Package pkg = (Package) i.next();

            pkg.reload();
        }
    }

    /**
     * Implementation of the "Save As.." user function.
     */
    public void saveAs(PkgMgrFrame frame) {
        // get a file name to save under
        String newName = FileUtility.getFileName(frame,
                Config.getString("pkgmgr.saveAs.title"),
                Config.getString("pkgmgr.saveAs.buttonLabel"), false, null, true);

        if (newName != null) {
            saveAll();

            int result = FileUtility.copyDirectory(getProjectDir().getPath(),
                    newName);

            switch (result) {
            case FileUtility.NO_ERROR:
                break;

            case FileUtility.DEST_EXISTS:
                DialogManager.showError(frame, "directory-exists");

                return;

            case FileUtility.SRC_NOT_DIRECTORY:
            case FileUtility.COPY_ERROR:
                DialogManager.showError(frame, "cannot-copy-package");

                return;
            }

            closeProject(this);

            // open new project
            Project openProj = openProject(newName);

            if (openProj != null) {
                // This is a wizard get 311003 Damiano
                Package pkg = openProj.getPackage(openProj.getInitialPackageName());

                PkgMgrFrame pmf = PkgMgrFrame.createFrame(pkg);
                pmf.show();
            } else {
                Debug.message("could not open package under new name");
            }
        }
    }

    /**
     * Explicitly restart the remote debug VM. The VM first gets shut down, and then
     * freshly restarted.
     */
    public void restartVM() {
        getDebugger().close(true);
        vmClosed();
        PkgMgrFrame.displayMessage(this, Config.getString("pkgmgr.creatingVM"));
    }

    /**
     * The remote VM for this project has just been initialised and is ready now.
     */
    private void vmReady() {
        PkgMgrFrame.displayMessage(Project.this,
            Config.getString("pkgmgr.creatingVMDone"));
        Utility.bringToFront(); // only works on MacOS currently
    }

    /**
     * The remote VM for this project has just been closed. Remove everything in this
     * project that depended on that VM.
     */
    private void vmClosed() {
        // remove breakpoints for all packages
        Iterator i = packages.values().iterator();

        while (i.hasNext()) {
            Package pkg = (Package) i.next();
            pkg.removeBreakpoints();
        }

        // any calls to the debugger made by removeLocalClassLoader
        // will silently fail
        removeLocalClassLoader();
    }

    /**
     * Get the ClassLoader for this project.
     * The ClassLoader load classes on the local VM.
     */
    public synchronized ProjectClassLoader getLocalClassLoader() {
        if (loader == null) {
            loader = ClassMgr.getProjectLoader(getProjectDir());
        }

        return loader;
    }

    /**
     * Removes the current classloader, and removes
     * references to classes loaded by it (this includes removing
     * the objects from all object benches of this project).
     * Should be run whenever a source file changes
     */
    public synchronized void removeLocalClassLoader() {
        if (loader != null) {
            // remove bench objects for all frames in this project
            PkgMgrFrame[] frames = PkgMgrFrame.getAllProjectFrames(this);

            for (int i = 0; i < frames.length; i++) {
                frames[i].getObjectBench().removeAllObjects(getUniqueId());
                frames[i].clearTextEval();
            }

            // get rid of any inspectors that are open that were not cleaned up
            // as part of removing objects from the bench
            removeAllInspectors();

            // remove views for classes loaded by this classloader
            View.removeAll(loader);

            // dispose windows for local classes. Should not run user code
            // on the event queue, so run it in a seperate thread.
            new Thread() {
                    public void run() {
                        getDebugger().disposeWindows();
                    }
                }.start();

            loader = null;
        }
    }

    /**
     * Removes the remote VM classloader.
     * Should be run whenever a source file changes.
     */
    public synchronized void newRemoteClassLoader() {
        getDebugger().newClassLoader(getProjectDir().getPath());
    }

    /**
     * Removes the remote VM classloader.
     * Should be run whenever a source file changes.
     */
    public synchronized void newRemoteClassLoaderLeavingBreakpoints() {
        getDebugger().newClassLoaderLeavingBreakpoints(getProjectDir().getPath());
    }

    public Debugger getDebugger() {
        return debugger;
    }

    public boolean hasExecControls() {
        return execControls != null;
    }

    public ExecControls getExecControls() {
        if (execControls == null) {
            execControls = new ExecControls(this, getDebugger());
        }

        return execControls;
    }

    public boolean hasTerminal() {
        return terminal != null;
    }

    public Terminal getTerminal() {
        if (terminal == null) {
            terminal = new Terminal(this);
        }

        return terminal;
    }

    /**
     * Loads a class using the current classLoader
     */
    public Class loadClass(String className) {
        try {
            return getLocalClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();

            return null;
        }
    }

    public boolean inTestMode() {
        return inTestMode;
    }

    public void setTestMode(boolean mode) {
        inTestMode = mode;
    }

    /**
     * Compare to arrays of urls to see if they are the same.
     * Note that is the order of the array is different then the two are considered different.
     * Note that there is an extra check for compileStarted in it that may suggest a different name to the method.
     * @param original URLs to compare against.
     * @param compare URLs to compare with the original.
     * @return true if the two arrays are the same.
     */
    private boolean sameUrls(URL[] original, URL[] compare) {
        if (original == null) {
            return false;
        }

        if (compileStarted) {
            // If we have a compilation started then we have to assume the usrls are different.
            return false;
        }

        if (original.length != compare.length) {
            return false;
        }

        for (int index = 0; index < original.length; index++)
            if (!original[index].equals(compare[index])) {
                return false;
            }

        return true;
    }

    /**
     * Return a ClassLoader that should be used to load or reflect on the project classes.
     * The same BClassLoader object is returned until the Project is compiled or the content of the
     * user class list is changed, this is needed to load "compatible" classes in the same classloader space.
     * Note: there is a threading issue here if you are using this method from different threads, normally this method should be
     * called from a swing thread, but it needs to be checked with the rest of the code.
     * Note2: since this is called from extensions it is probably best to make it synchronized to avoid any threading issues.
     *
     * @return a BClassLoader that provides class loading services for this Project.
     */
    public synchronized BPClassLoader getClassLoader() {
        // At the moment I do this to find out what has changed. It will be done differently at the end.
        ClassPath allcp = ClassMgr.getClassMgr().getAllClassPath();
        allcp.addClassPath(getLocalClassLoader().getAsClassPath());

        URL[] newUrls = allcp.getURLs();

        if (sameUrls(currentUrls,newUrls)) {
            return currentClassLoader;
        }

        // The Project Class Loader must not "see" the BlueJ classes, this is teh reason to 
        // have BClassLoader created with the boot loader as parent.
        currentClassLoader = new BPClassLoader(newUrls,Boot.getInstance().getBootClassLoader());
        currentUrls = newUrls;
        compileStarted = false; // Clear the flag.

        return currentClassLoader;
    }

    /**
     * A Package should tell to a project when a compilation has started.
     * When a package of a project is recompiled the class loader for that project must be recreated
     * and in order to do so the Project should know if something has been recompiled.
     * Note: there is a time gap between compilation start and end, in theory no classLoader can be retrieved
     * until the compilation ends, in practice it does not make any difference.
     */
    void setCompileStarted() {
        compileStarted = true;
    }

    /**
     * Convert a filename into a fully qualified Java name.
     * Returns null if the file is outside the project
     * directory.
     *
     * The behaviour of this function is not guaranteed if
     * you pass in a directory name. It is meant for filenames
     * like /foo/bar/p1/s1/TestName.java
     */
    public String convertPathToPackageName(String pathname) {
        return JavaNames.convertFileToQualifiedName(getProjectDir(),
            new File(pathname));
    }

    public void removeStepMarks() {
        // remove step marks for all packages
        Iterator i = packages.values().iterator();

        while (i.hasNext()) {
            Package pkg = (Package) i.next();
            pkg.removeStepMarks();
        }

        return;
    }

    // ---- DebuggerListener interface ----

    /**
     * A debugger event was fired. Analyse which event it was, and take
     * appropriate action.
     */
    public void debuggerEvent(final DebuggerEvent event) {
        EventQueue.invokeLater(new Runnable() {
                public void run() {
                    if (event.getID() == DebuggerEvent.DEBUGGER_STATECHANGED) {
                        PkgMgrFrame[] frames = PkgMgrFrame.getAllProjectFrames(Project.this);

                        if (frames == null) {
                            return;
                        }

                        for (int i = 0; i < frames.length; i++)
                            frames[i].setDebuggerState(event.getNewState());

                        // check whether we just got a freshly created VM
                        if ((event.getOldState() == Debugger.NOTREADY) &&
                                (event.getNewState() == Debugger.IDLE)) {
                            vmReady();
                        }

                        // check whether a good VM just disappeared
                        if ((event.getOldState() == Debugger.IDLE) &&
                                (event.getNewState() == Debugger.NOTREADY)) {
                            vmClosed();
                        }

                        return;
                    }

                    if (event.getID() == DebuggerEvent.DEBUGGER_REMOVESTEPMARKS) {
                        removeStepMarks();

                        return;
                    }

                    DebuggerThread thr = event.getThread();
                    String packageName = JavaNames.getPrefix(thr.getClass(0));
                    Package pkg = getPackage(packageName);

                    if (pkg != null) {
                        switch (event.getID()) {
                        case DebuggerEvent.THREAD_BREAKPOINT:
                            pkg.hitBreakpoint(thr);

                            break;

                        case DebuggerEvent.THREAD_HALT:
                            pkg.hitHalt(thr);

                            break;

                        //case DebuggerEvent.THREAD_CONTINUE:
                        //	break;
                        case DebuggerEvent.THREAD_SHOWSOURCE:
                            pkg.showSourcePosition(thr);

                            break;
                        }
                    }
                }
            });
    }

    // ---- end of DebuggerListener interface ----

    /**
     * String representation for debugging.
     */
    public String toString() {
        return "Project:" + getProjectName();
    }

    /**
     * Removes a packageTarget from the map of packages in the project.
     * @param packageQualifiedName The qualified name of the package.
     *
     */
    public void removePackage(String packageQualifiedName) {
        packages.remove(packageQualifiedName);
    }
}
