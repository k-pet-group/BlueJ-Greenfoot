package bluej.pkgmgr;

import bluej.groupwork.ui.StatusFrame;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import javax.swing.JFrame;
import java.awt.Window;

import bluej.BlueJEvent;
import bluej.Boot;
import bluej.Config;
import bluej.classmgr.BPClassLoader;
import bluej.debugger.*;
import bluej.debugmgr.ExecControls;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.inspector.ClassInspector;
import bluej.debugmgr.inspector.Inspector;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.debugmgr.inspector.ObjectInspector;
import bluej.debugmgr.inspector.ResultInspector;
import bluej.editor.Editor;
import bluej.extensions.BProject;
import bluej.extensions.ExtensionBridge;
import bluej.extmgr.ExtensionsManager;
import bluej.groupwork.CodeFileFilter;
import bluej.groupwork.Repository;
import bluej.groupwork.TeamSettingsController;
import bluej.groupwork.actions.TeamActionGroup;
import bluej.groupwork.ui.CommitCommentsFrame;
import bluej.groupwork.ui.TeamSettingsDialog;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.Target;
import bluej.prefmgr.PrefMgr;
import bluej.prefmgr.PrefMgrDialog;
import bluej.terminal.Terminal;
import bluej.testmgr.record.ClassInspectInvokerRecord;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.JavaNames;
import bluej.utility.Utility;
import bluej.views.View;


/**
 * A BlueJ Project.
 *
 * @author  Michael Kolling
 * @author  Axel Schmolitzky
 * @author  Andrew Patterson
 * @author  Bruce Quig
 * @version $Id: Project.java 4780 2006-12-22 04:14:21Z bquig $
 */
public class Project implements DebuggerListener, InspectorManager 
{
    /**
     * Collection of all open projects. The canonical name of the project
     * directory (as a File object) is used as the key.
     */
    private static Map projects = new HashMap();
    public static final int NEW_PACKAGE_DONE = 0;
    public static final int NEW_PACKAGE_EXIST = 1;
    public static final int NEW_PACKAGE_BAD_NAME = 2;
    public static final int NEW_PACKAGE_NO_PARENT = 3;

    public static final String projectLibDirName = "+libs";

    /* ------------------- end of static declarations ------------------ */

    // instance fields

    /** the path of the project directory. */
    private File projectDir;

    /** collection of open packages in this project
      (indexed by the qualifiedName of the package).
       The unnamed package ie root package of the package tree
       can be obtained by retrieving "" from this collection */
    private Map packages;

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
    
    // the TeamSettingsController for this project
    private TeamSettingsController teamSettingsController = null;
    
    private CommitCommentsFrame commitCommentsFrame = null;
    private StatusFrame statusFrame = null;
        
    private boolean isSharedProject;

    // team actions
    private TeamActionGroup teamActions;
    

    /* ------------------- end of field declarations ------------------- */

    /**
     * Construct a project in the directory projectDir.
     * This must contain the root bluej.pkg of a nested package
     * (this should by its nature be the unnamed package).
     */
    private Project(File projectDir)
    {
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
        // The debugger should have a new classLoader, may it go into the getDebuggerImpl ?
        debugger.newClassLoader(getClassLoader());
        debugger.addDebuggerListener(this);
        debugger.launch();

        docuGenerator = new DocuGenerator(this);
        
        // Check whether this is a shared project
        String cfgFilePath = projectDir.getAbsolutePath() + "/team.defs";
        File cfgFile = new File(cfgFilePath);
        isSharedProject = cfgFile.isFile();
        teamActions = new TeamActionGroup(isSharedProject);
    }

    /**
     * Check if the path given is either a directory with a bluej pkg file or
     * the name of a bluej pkg file.
     *
     * @param projectPath
     *            a string representing the path to check. This can either be a
     *            directory name or the filename of a bluej.pkg file.
     */
    public static boolean isProject(String projectPath) 
    {
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
    public static Project openProject(String projectPath) 
    {
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
    public static void closeProject(Project project) 
    {
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
    public static void cleanUp(Project project) 
    {
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
    public static boolean createNewProject(String projectPath) 
    {
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
    public static int getOpenProjectCount() 
    {
        return projects.size();
    }

    /**
     * Gets the set of currently open projects. It is an accessor only
     * @return a Set containing all open projects.
     */
    public static Collection getProjects() 
    {
        return projects.values();
    }

    /**
     * Given a Projects key returns the Project objects describing this projects.
     */
    public static Project getProject(File projectKey) 
    {
        return (Project) projects.get(projectKey);
    }

    /**
     * Check if the given path contains a BlueJ project.
     * 
     * @param projectPath
     */
    public static boolean isBlueJProject(String projectPath) 
    {
        File startingDir = null;
        try {
            startingDir = pathIntoStartingDirectory(projectPath);
        }
        catch(IOException ioe)
        {
            return false;
        }

        if (startingDir == null) {
            return false;
        }

        return Package.isBlueJPackage(startingDir);
    }
    
    /**
     * Helper function to take a path (either a directory or a file)
     * and return either the canonical path to the directory
     * (in the case of a bluej.pkg file passed in, return the directory containing
     * the file. Returns null if file is not a bluej.pkg file or if the
     * directory/file does not exist.
     */
    private static File pathIntoStartingDirectory(String projectPath)
        throws IOException 
    {
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
     * Update an inspector, make sure it's visible, and bring it to
     * the front.
     * 
     * @param inspector  The inspector to update and show
     */
    private void updateInspector(final Inspector inspector)
    {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                inspector.update();
                inspector.updateLayout();
                inspector.setVisible(true);
                inspector.bringToFront();
            }
        });
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
        String name, Package pkg, InvokerRecord ir, JFrame parent) 
    {
        ObjectInspector inspector = (ObjectInspector) inspectors.get(obj);

        if (inspector == null) {
            inspector = new ObjectInspector(obj, this, name, pkg, ir, parent);
            inspectors.put(obj, inspector);
        }

        updateInspector(inspector);

        return inspector;
    }
    
    /**
     * Get the inspector for the given object. Object can be a DebuggerObject, or a
     * fully-qualified class name.
     * 
     * @param obj  The object whose inspector to retrieve
     * @return the inspector, or null if no inspector is open
     */
    public Inspector getInspector(Object obj) 
    {
        return (Inspector) inspectors.get(obj);
    }

    /**
     * Remove an inspector from the list of inspectors for this project
     * @param obj the inspector.
     */
    public void removeInspector(DebuggerObject obj) 
    {
        inspectors.remove(obj);
    }
    
    /**
     * Remove an inspector from the list of inspectors for this project
     * @param obj the inspector. 
     */
    public void removeInspector(DebuggerClass cls) 
    {
        inspectors.remove(cls.getName());
    }

    /**
     * Removes an inspector instance from the collection of inspectors
     * for this project. It firstly retrieves the inspector object and
     * then calls its doClose method.
     * @param obj
     */
    public void removeInspectorInstance(Object obj) 
    {
        Inspector inspect = getInspector(obj);

        if (inspect != null) {
            inspect.doClose(false);
        }
    }

    /**
     * Removes all inspector instances for this project.
     * This is used when VM is reset or the project is recompiled.
     *
     */
    public void removeAllInspectors() 
    {
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
        Package pkg, JFrame parent) 
    {
        ClassInspector inspector = (ClassInspector) inspectors.get(clss.getName());

        if (inspector == null) {
            ClassInspectInvokerRecord ir = new ClassInspectInvokerRecord(clss.getName());
            inspector = new ClassInspector(clss, this, pkg, ir, parent);
            inspectors.put(clss.getName(), inspector);
        }

        updateInspector(inspector);

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
        JFrame parent) 
    {
        final ResultInspector inspector = new ResultInspector(obj, this, name, pkg, ir, info,
                    parent);
        inspectors.put(obj, inspector);

        updateInspector(inspector);

        return inspector;
    }

    /**
     * Iterates through all inspectors and updates them
     *
     */
    public void updateInspectors() 
    {
        for (Iterator it = inspectors.values().iterator(); it.hasNext();) {
            Inspector inspector = (Inspector) it.next();
            inspector.update();
        }
    }

    /**
     * Return the name of the project.
     */
    public String getProjectName() 
    {
        return projectDir.getName();
    }

    /**
     * Return the location of the project.
     */
    public File getProjectDir() 
    {
        return projectDir;
    }

    /**
     * Get the project repository. If the user cancels the credentials dialog,
     * or this is not a team project, returns null.
     */
    public Repository getRepository()
    {
    	if (isSharedProject) {
    	    return getTeamSettingsController().getRepository();
        }
        else {
            return null;
        }
    }
    
    /**
     * Return whether the project is located in a readonly directory
     * @return
     */
    public boolean isReadOnly() 
    {
        return !projectDir.canWrite();
    }

    /**
     * A string which uniquely identifies this project
     */
    public String getUniqueId() 
    {
        return String.valueOf(new String("BJID" + getProjectDir().getPath()).hashCode());
    }

    /**
     * The name of the package within the project directory where we first opened
     * this project.
     */
    public String getInitialPackageName() 
    {
        return initialPackageName;
    }

    /**
     * Get an existing package from the project. The package is opened (i.e an
     * new Package is created) if it's not already open. All parent packages on
     * the way to the root of the package tree will also be constructed.
     * 
     * @param qualifiedName package name ie java.util or "" for unnamed package
     * @returns  the package, or null if the package doesn't exist
     */
    public Package getPackage(String qualifiedName)
    {
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
                    // Note, construction of the new package throws IOException if
                    // the directory or bluej.pkg file doesn't exist
                    pkg = new Package(this, JavaNames.getBase(qualifiedName),
                            parent);
                    packages.put(qualifiedName, pkg);
                } else { // parent package does not exist. How can it not exist ?
                    pkg = null;
                }
            } catch (IOException exc) {
                // the package did not exist in this project
                pkg = null;
            }
            
            return pkg;
        }

        throw new IllegalStateException("Project.getPackage()");
    }


    private BProject singleBProject;  // Every Project has none or one BProject
    
    /**
     * Return the extensions BProject associated with this Project.
     * There should be only one BProject object associated with each Project.
     * @return the BProject associated with this Project.
     */
    public synchronized final BProject getBProject ()
    {
        if ( singleBProject == null )
          singleBProject = ExtensionBridge.newBProject(this);
          
        return singleBProject;
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
     * This creates package directories. For the given package, all
     * intermediate package directories (which do not already exist)
     * will be created. A bluej.pkg file will be created for each
     * directory (if it does not already exist).
     * 
     * @param fullName  the fully qualified name of the package to create
     *                  directories for
     */
    public void createPackageDirectory(String fullName)
    {
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
     * Get the names of all packages in this project consisting of rootPackage
     * package and all packages nested below it.
     *
     * @param rootPackage
     *            the root package to consider in looking for nested packages
     * @return a List of String containing the fully qualified names of the
     *         packages.
     */
    private List getPackageNames(Package rootPackage)
    {
        List l = new LinkedList();
        List children;

        l.add(rootPackage.getQualifiedName());

        children = rootPackage.getChildren(true);
        Iterator i = children.iterator();
        
        while (i.hasNext()) {
            Package p = (Package) i.next();
            l.addAll(getPackageNames(p));
        }

        return l;
    }

    /**
     * Get the names of all packages in this project.
     * 
     * @return  a List of String containing the fully qualified names
     *          of the packages in this project.
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

        // Surely we do not want to stack trace if nothing exists. Damiano
        if (frames == null) {
            return;
        }

        for (int i = 0; i < frames.length; i++) {
            frames[i].doSave();
        }
    }

     public void saveAllEditors(){
    	Iterator i = packages.values().iterator();

        while(i.hasNext()) {
            Package pkg = (Package) i.next();
            try {
                pkg.saveFilesInEditors();
            }
            catch(IOException ioe) {
                ioe.printStackTrace();
            }
        } 
    }
    /**
     * Make all the Packages in this project save their graphlayout
     */
    public void saveAllGraphLayout(){
    	Iterator i = packages.values().iterator();
 
        while(i.hasNext()) {
            Package pkg = (Package) i.next();
				pkg.save(null);
        }
    }
    
    /**
     * Make all the Packages in this project, make their editors reload
     * the files they are editing.
     */
    public void reloadFilesInEditors(){
    	Iterator i = packages.values().iterator();

        while(i.hasNext()) {
            Package pkg = (Package) i.next();
				pkg.reloadFilesInEditors();
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
     * make all open package editors clear their selection
     *
     */
    public void clearAllSelections()
    {
    	Iterator i = packages.values().iterator();

        while(i.hasNext()) {
            Package pkg = (Package) i.next();
            PackageEditor editor = pkg.getEditor();
            if (editor != null){
            	editor.clearSelection();
            }
        }
    }
    
    /**
     * Make the grapheditors of this project clear their selection and select
     * the targets given in the parameter targetNames
     * @param targetNames a list of Strings containing target names of the form
     * packages/targetname. For instance myPackage/myClass
     */
    public void selectTargetsInGraphs(List targetNames)
    {
//    	String packageName = "";
//    	String targetName = "";
    	for (Iterator i = targetNames.iterator(); i.hasNext();) {
			String pathAndName = (String) i.next();
//			int index = pathAndName.lastIndexOf('/');
//			if (index > 0) {
//				packageName = pathAndName.substring(0, index);
//				targetName = pathAndName.substring(index, pathAndName.length());
//			}
//			else {
//				targetName = pathAndName;
//			}
//			Package p = getExistingPackage(packageName);
//			PackageEditor packageEditor = p.getEditor();
//	    	Target target = p.getTarget(pathAndName);
			Target target = getTarget(pathAndName);
			if (target != null){
			    PackageEditor packageEditor = target.getPackage().getEditor();
			    packageEditor.addToSelection(target);
			    packageEditor.repaint();
			}
		}
    }
    
    /**
     * Given the path and name of a target in the project, return the target or
     * null if the target doesn't exist
     * @param pathAndName
     * @return the target
     */
    public Target getTarget(String pathAndName)
    {
        String packageName = "";
    	//String targetName = "";
    	int index = pathAndName.lastIndexOf('/');
    	if (index > 0) {
			packageName = pathAndName.substring(0, index);
			//targetName = pathAndName.substring(index, pathAndName.length());
		}
		else {
			//targetName = pathAndName;
		}
		Package p = getPackage(packageName);
		if (p == null){
		    return null;
		}
		// PackageEditor packageEditor = p.getEditor();
    	Target target = p.getTarget(pathAndName);
    	return target;
    }
    
    /**
     * Open the source editor for each target that is selected in its
     * package editor
     *
     */
    public void openEditorsForSelectedTargets(){
    	List selectedTargets = getSelectedTargets();
    	for (Iterator i = selectedTargets.iterator(); i.hasNext(); ){
    		Target target = (Target) i.next();
    		if (target instanceof ClassTarget){
    			ClassTarget classTarget = (ClassTarget) target;
    			Editor editor = classTarget.getEditor();
    			editor.setVisible(true);
    			// make moe select the ======== part of cvs conflicts
    		}
    	}
    	
    }
    
    /**
     * Returns a list of Targets that is seleceted in its package editor
     * @return List list of targets that is selected
     */
    private List getSelectedTargets(){
    	List selectedTargets = new LinkedList();
    	List packageNames = getPackageNames();
    	for (Iterator i = packageNames.iterator(); i.hasNext();) {
			String packageName = (String) i.next();
			Package p = getPackage(packageName);
			selectedTargets.addAll(Arrays.asList(p.getSelectedTargets()));
		}
    	return selectedTargets;
    }
    
    /**
     * Implementation of the "Save As.." user function.
     */
    public void saveAs(PkgMgrFrame frame) {
        // get a file name to save under
        String newName = FileUtility.getFileName(frame,
                Config.getString("pkgmgr.saveAs.title"),
                Config.getString("pkgmgr.saveAs.buttonLabel"), true, null, true);

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
                pmf.setVisible(true);
            } else {
                Debug.message("could not open package under new name");
            }
        }
    }

    /**
     * Explicitly restart the remote debug VM. The VM first gets shut down, and then
     * freshly restarted.
     */
    public void restartVM()
    {
        getDebugger().close(true);
        vmClosed();
        PkgMgrFrame.displayMessage(this, Config.getString("pkgmgr.creatingVM"));
    }

    /**
     * The remote VM for this project has just been initialised and is ready now.
     */
    private void vmReady()
    {
        BlueJEvent.raiseEvent(BlueJEvent.CREATE_VM_DONE, null);
        Utility.bringToFront();  // only works on MacOS currently
    }

    /**
     * The remote VM for this project has just been closed. Remove everything in this
     * project that depended on that VM.
     */
    private void vmClosed()
    {
        // remove breakpoints for all packages
        Iterator i = packages.values().iterator();

        while (i.hasNext()) {
            Package pkg = (Package) i.next();
            pkg.removeBreakpoints();
        }

        // any calls to the debugger made by removeLocalClassLoader
        // will silently fail
        removeClassLoader();
        
        // The configured extra libraries may have changed, so
        // rebuild the class loader (do this now so the new loader
        // will be installed as soon as the VM has restarted).
        newRemoteClassLoader();
    }

    /**
     * Removes the current classloader, and removes
     * references to classes loaded by it (this includes removing
     * the objects from all object benches of this project).
     * Should be run whenever a source file changes
     */
    public void removeClassLoader()
    {
        // There is nothing to do if the current classloader is null.
        if (currentClassLoader == null) {
            return;
        }
        
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
        View.removeAll(currentClassLoader);

        if (! Config.isGreenfoot()) {
            // dispose windows for local classes. Should not run user code
            // on the event queue, so run it in a seperate thread.
            new Thread() {
                public void run() {
                    getDebugger().disposeWindows();
                }
            }.start();
        }

        currentClassLoader = null;
    }

    /**
     * Creates a new debugging VM classloader.
     * Should be run whenever a class file changes.
     */
    public void newRemoteClassLoader()
    {
        getDebugger().newClassLoader(getClassLoader());
    }

    /**
     * Creates a new debugging VM classloader, leaving current breakpoints.
     * Should be run whenever a source file changes.
     */
    public void newRemoteClassLoaderLeavingBreakpoints()
    {
        getDebugger().newClassLoaderLeavingBreakpoints(getClassLoader());
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
    public Class loadClass(String className)
    {
        try {
            return getClassLoader().loadClass(className);
        }
        catch (ClassNotFoundException e) {
            return null;
        }
        catch (LinkageError le) {
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
     * Returns a list of URL having in it all libraries that are in the +libs directory
     * of this project.
     * @return a non null but possibly empty list of URL.
     */
    protected ArrayList getPlusLibsContent () 
    {
        ArrayList risul = new ArrayList();
        
        // the subdirectory of the project which can hold project specific jars and zips
        File libsDirectory = new File(projectDir, projectLibDirName);

        // If it is not a directory or we cannot read it then there is nothing to do.
        if ( ! libsDirectory.isDirectory() || ! libsDirectory.canRead() ) 
          return risul;
          
        // the list of jars and zips we find
        File []libs = libsDirectory.listFiles();

        // If there are no files there then again just return.
        if ( libs==null || libs.length < 1 ) 
          return risul;

        // if we found any jar files in the libs directory then add their URLs
        for(int index=0; index<libs.length; index++) {
            attemptAddLibrary(risul, libs[index]);
        }
        return risul;
    }
    
    /**
     * Attempts to add a library to the given list of libraies.
     * A valid library file is one that is a file, readable, ends either with zip or jar.
     * Before addition the file is transformaed to a URL.
     * @param risul where to add the file
     * @param aFile the file to be added.
     */
    private static final void attemptAddLibrary ( ArrayList risul, File aFile ) 
    {
        if ( aFile == null ) return;
        
        // Is this a normal file and is it readable ?
        if ( ! (aFile.isFile() && aFile.canRead()) ) return;
        
        String libname = aFile.getName().toLowerCase();
        if ( ! (libname.endsWith(".jar") || libname.endsWith(".zip")) ) return;
        
        try {
            risul.add(aFile.toURI().toURL());
        }
        catch(MalformedURLException mue) { 
            Debug.reportError("Project.attemptAddLibrary() malformaed file="+aFile);
        }
    }
      

    /**
     * Returns an array of URLs for all the JAR files located in the lib/userlib directory.
     * The result is calculated every time the method is called, in this way it is possible
     * to capture a change in the library content in a reasonable timing.
     *
     * @return  URLs of the discovered JAR files
     */
    public static final ArrayList getUserlibContent() 
    {
        ArrayList risul = new ArrayList();
        File userLibDir;
        
        // The userlib location may be specified in bluej.defs
        String userLibSetting = Config.getPropString("bluej.userlibLocation", null);
        if (userLibSetting == null) {
            userLibDir = new File(Boot.getInstance().getBluejLibDir(), "userlib");
        }
        else {
            userLibDir = new File(userLibSetting);
        }

        File[] files = userLibDir.listFiles();
        if (files == null) {
            return risul;
        }
        
        for (int index = 0; index < files.length; index++) {
            attemptAddLibrary(risul, files[index]);
        }
 
        return risul;
    }

    /**
     * TIny utility to make code cleaner.
     */
    private void addArrayToList ( ArrayList list, URL []urls)
    {
        for (int index=0; index<urls.length; index++)
            list.add(urls[index]);
    }
    
    /**
     * Return a ClassLoader that should be used to load or reflect on the project classes.
     * The same BClassLoader object is returned until the Project is compiled or the content of the
     * user class list is changed, this is needed to load "compatible" classes in the same classloader space.
     *
     * @return a BClassLoader that provides class loading services for this Project.
     */
    public BPClassLoader getClassLoader()
    {
        if (currentClassLoader != null)
            return currentClassLoader;
        
        ArrayList pathList = new ArrayList();

        try {
            // Junit is always part of the project libraries, only Junit, not the core Bluej.
			//   pathList.add( Boot.getInstance().getJunitLib().toURI().toURL());
            
            // Until the rest of BlueJ is clean we also need to add bluejcore
            // It should be possible to run BlueJ only with Junit
            addArrayToList (pathList, Boot.getInstance().getRuntimeUserClassPath());
    
            // Next part is the libraries that are added trough the config panel.
            pathList.addAll ( PrefMgrDialog.getInstance().getUserConfigLibPanel().getUserConfigContent() );
    
            // Then the libraries that are in the userlib directory
            pathList.addAll ( getUserlibContent() );
            
            // The libraries that are in the project +libs directory
            pathList.addAll ( getPlusLibsContent() );
          
            // The current paroject dir must be added to the project class path too.
            pathList.add(getProjectDir().toURI().toURL());

        }
        catch ( Exception exc ) {
            // Should never happen
            Debug.reportError("Project.getClassLoader() exception: " + exc.getMessage());
            exc.printStackTrace();
        }

        URL [] newUrls = (URL [])pathList.toArray(new URL[pathList.size()]);
        
        // The Project Class Loader should not see the BlueJ classes (the necessary
        // ones have been added to the URL list anyway). So we use the boot loader
        // as parent.
        currentClassLoader = new BPClassLoader(newUrls,Boot.getInstance().getBootClassLoader());

        return currentClassLoader;
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
    public String convertPathToPackageName(String pathname) 
    {
        return JavaNames.convertFileToQualifiedName(getProjectDir(),
            new File(pathname));
    }

    public void removeStepMarks() 
    {
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
                        
                        int newState = event.getNewState();
                        int oldState = event.getOldState();

                        for (int i = 0; i < frames.length; i++)
                            frames[i].setDebuggerState(newState);

                        // check whether we just got a freshly created VM
                        if ((oldState == Debugger.NOTREADY) &&
                                (newState == Debugger.IDLE)) {
                            vmReady();
                        }

                        // check whether a good VM just disappeared
                        if ((oldState == Debugger.IDLE) &&
                                (newState == Debugger.NOTREADY)) {
                            vmClosed();
                        }

                        // check whether we failed to create the VM
                        if (newState == Debugger.LAUNCH_FAILED) {
                            BlueJEvent.raiseEvent(BlueJEvent.CREATE_VM_FAILED, null);
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
    public String toString()
    {
        return "Project:" + getProjectName();
    }

    /**
     * Removes a package (and any sub-packages) from the map of open
     * packages in the project.
     * 
     * @param packageQualifiedName The qualified name of the package.
     */
    public void removePackage(String packageQualifiedName)
    {
        Package pkg = (Package) packages.get(packageQualifiedName);
        if (pkg != null) {
            List childPackages = pkg.getChildren(false);
            Iterator i = childPackages.iterator();
            while (i.hasNext()) {
                Package childPkg = (Package) i.next();
                removePackage(childPkg.getQualifiedName());
            }
            packages.remove(packageQualifiedName);
        }
    }
    
    // ---- teamwork
    
    /**
     * Return the teamwork action group.
     */
    public TeamActionGroup getTeamActions()
    {
        return teamActions;
    }
    
    /**
	 * Determine if project is a team project. 
	 * The method will look for the existence of the team configuration file
	 * team.defs
	 * @return true if the project is a team project
	 */
	public boolean isTeamProject()
    {
		return isSharedProject;
	}
	
	/**
	 * Get an array of Files that resides in the project folders.
	 * @param project the project
	 * @return List of Files 
	 */
	public Set getFilesInProject(boolean includePkgFiles)
    {
		Set files = new HashSet();
		traverseDirsForFiles(files, projectDir, includePkgFiles);
		return files;
	}
	
    /**
     * Get the teams settings controller for this project. Returns null
     * if this is not a shared project.
     */
	public TeamSettingsController getTeamSettingsController()
    {
        if(teamSettingsController == null && isSharedProject) {
            teamSettingsController = new TeamSettingsController(this);
        }
		return teamSettingsController;
	}
	
        
	/**
	 * Get an array of Files containing the directories that exist in the 
	 * project which has not been added to CVS.
	 * @return array of Files
	 */
	public List getDirsInProjectWhichNeedToBeAdded() 
    {
		List files = new LinkedList();
		traverseDirsForDirs(files, projectDir);
		return files;
	}
	
	public boolean isInCVS()
    {
		return isDirInCVS(getProjectDir());
	}
	
	/**
	 * Traverse the directory tree starting in dir an add all the encountered 
	 * files to the List allFiles. The parameter includePkgFiles determine 
	 * whether bluej.pgk files should be added to allFiles as well.
	 * @param allFiles a List to which the method will add the files it meets.
	 * @param dir the directory the search starts from
	 * @param includePkgFiles if true, bluej.pkg files are included as well.
	 */
	private void traverseDirsForFiles(Set allFiles, File dir, boolean includePkgFiles)
    {
		File[] files = dir.listFiles(new CodeFileFilter(getTeamSettingsController().getIgnoreFiles(),includePkgFiles));
		if (files==null){
			return;
		}
		for(int i=0; i< files.length; i++ ){
			if (files[i].isFile()){
				allFiles.add(files[i]);
			}else{
				traverseDirsForFiles(allFiles, files[i], includePkgFiles);
			}
		}
	}
	
	/**
	 * Traverses a directory tree and records the directories it encounters.
	 * Directories named 'CVSROOT' or 'CVS' are ignored along with directories
	 * already in CVS.
	 * @param allDirs a List to which the method will add the dirs it meets.
	 * @param dir the directory the search starts from.
	 */
	private static void traverseDirsForDirs(List allDirs, File dir)
    {
		File[] files = dir.listFiles();
		boolean ok = false;
		if (files == null){
			return;
		}
		for(int i=0; i< files.length; i++ ){
			ok = !files[i].getName().equals("CVSROOT") &&
				 !files[i].getName().equals("CVS") && !isDirInCVS(files[i]);
			if (files[i].isDirectory()){
				if (ok) {
					allDirs.add(files[i]);
				}
				traverseDirsForDirs(allDirs, files[i]);
			}
		}
	}
	
	/**
	 * Determine if a directory is in CVS. The method looks for a subfolder
	 * named 'CVS'
	 * @param dir the directory
	 * @return true if directory is in CVS.
	 */
	private static boolean isDirInCVS(File dir)
    {
		char s = File.separatorChar;
		File cvsDir = new File(dir.getAbsolutePath() + s + "CVS");
		return cvsDir.exists();
	}

    /**
     * Get the team settings dialog for this project. Only call this if the
     * project is a shared project.
     */
    public TeamSettingsDialog getTeamSettingsDialog()
    {
        return getTeamSettingsController().getTeamSettingsDialog();
    }
    
    public CommitCommentsFrame getCommitCommentsDialog()
    {
        // if the commit comments dialog is null or has been associated with a different
        // PkgMgrFrame we need to recreate with appropriate parent. this method is used
        // by CommitCommentAction for centreing and by CommitAction for gaining 
        // commit comment etc.
        if(commitCommentsFrame == null) {
            commitCommentsFrame = new CommitCommentsFrame(this);
        }
        return commitCommentsFrame;
    }
        
    /**
     * Set this project as either shared or non-shared.
     */
    private void setProjectShared(boolean shared)
    {
        isSharedProject = shared;
        teamActions.setTeamMode(shared);
        
        PkgMgrFrame[] frames = PkgMgrFrame.getAllProjectFrames(this);
        if (frames != null) {
            for (int i = 0; i < frames.length; i++) {
                frames[i].updateSharedStatus(shared);
            }
        }
    }
    
    /**
     * Find the package name of the package containing the given file.
     * Might return null if the file isn't in the package. 
     */
    public String getPackageForFile(File f)
    {
        File projdir = getProjectDir();
        
        // First find out the package name...
        String packageName = "";
        File parentDir = f.getParentFile();
        while (! parentDir.equals(projdir)) {
            if (packageName.equals("")) {
                packageName = parentDir.getName();
            }
            else {
                packageName = parentDir.getName() + "." + packageName;
            }
            parentDir = parentDir.getParentFile();
            if (parentDir == null) {
                // file not in project?
                return null;
            }
        }
        
        return packageName;
    }
    
    
    /**
     * Set the team settings controller for this project. This makes the
     * project a shared project (unless the controller is null).
     */
    public void setTeamSettingsController(TeamSettingsController tsc)
    {
        teamSettingsController = tsc;
        if (tsc != null) {
            tsc.setProject(this);
            tsc.writeToProject();
        }
        setProjectShared (tsc != null);
    }

    /**
     * return the associated status window
     */
    public StatusFrame getStatusWindow(Window parent)
    {
        if(statusFrame == null) {
            statusFrame = new StatusFrame(this);
        }
        statusFrame.setLocationRelativeTo(parent);
        return statusFrame;
    }
}
