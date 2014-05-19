/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2013,2014  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.pkgmgr;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import bluej.BlueJEvent;
import bluej.Boot;
import bluej.Config;
import bluej.classmgr.BPClassLoader;
import bluej.collect.DataCollector;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerEvent;
import bluej.debugger.DebuggerListener;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerThread;
import bluej.debugmgr.ExecControls;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.inspector.ClassInspector;
import bluej.debugmgr.inspector.Inspector;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.debugmgr.inspector.ObjectInspector;
import bluej.debugmgr.inspector.ResultInspector;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.editor.Editor;
import bluej.extensions.BProject;
import bluej.extensions.ExtensionBridge;
import bluej.extmgr.ExtensionsManager;
import bluej.groupwork.Repository;
import bluej.groupwork.TeamSettingsController;
import bluej.groupwork.actions.TeamActionGroup;
import bluej.groupwork.ui.CommitCommentsFrame;
import bluej.groupwork.ui.StatusFrame;
import bluej.groupwork.ui.TeamSettingsDialog;
import bluej.groupwork.ui.UpdateFilesFrame;
import bluej.parser.entity.EntityResolver;
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
import bluej.utility.FileUtility.WriteCapabilities;
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
 */
public class Project implements DebuggerListener, InspectorManager 
{
    /**
     * Collection of all open projects. The canonical name of the project
     * directory (as a File object) is used as the key.
     */
    private static Map<File,Project> projects = new HashMap<File,Project>();
    public static final int NEW_PACKAGE_DONE = 0;
    public static final int NEW_PACKAGE_EXIST = 1;
    public static final int NEW_PACKAGE_BAD_NAME = 2;
    public static final int NEW_PACKAGE_NO_PARENT = 3;

    public static final String projectLibDirName = "+libs";
    
    /** Property specifying location of JDK source */
    private static final String JDK_SOURCE_PATH_PROPERTY = "bluej.jdk.source";
    
    private static final String PROJECT_CHARSET_PROP = "project.charset";
    private static final String PROJECT_IS_JAVA_ME_PROP = "package.isJavaMEproject";
    
    /* ------------------- end of static declarations ------------------ */

    // instance fields

    /** the path of the project directory. */
    private File projectDir;

    /** collection of open packages in this project
      (indexed by the qualifiedName of the package).
       The unnamed package ie root package of the package tree
       can be obtained by retrieving "" from this collection */
    private Map<String, Package> packages;

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
    private Map<Object,Inspector> inspectors;
    
    private boolean inTestMode = false;
    private BPClassLoader currentClassLoader;
    private List<URL> libraryUrls;
    
    // the TeamSettingsController for this project
    private TeamSettingsController teamSettingsController = null;
    
    private CommitCommentsFrame commitCommentsFrame = null;
    private UpdateFilesFrame updateFilesFrame = null;
    private StatusFrame statusFrame = null;
    
    /** If true, this project is connected with a source repository */
    private boolean isSharedProject;

    // team actions
    private TeamActionGroup teamActions;  
    
    // Flag signalling whether this is a Java Micro Edition project
    private boolean isJavaMEproject = false;    
    
    /** Resolve javadoc for this project */
    private JavadocResolver javadocResolver;
    
    /** Where to find JDK and other library sources (for extracting javadoc) */
    private List<DocPathEntry> sourcePath;
    
    /** Project character set for source files etc */
    private Charset characterSet;

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

        Debug.log("Opening project: " + projectDir.toString());
        
        // Make JDK's javadoc available, if we can find the source
        javadocResolver = new ProjectJavadocResolver(this);
        sourcePath = new ArrayList<DocPathEntry>();
        File javaHome = Boot.getInstance().getJavaHome();
        File jdkSourceZip = new File(javaHome, "src.zip");
        if (jdkSourceZip.isFile()) {
            sourcePath.add(new DocPathEntry(jdkSourceZip, ""));
        }
        else {
            File javaHomeParent = javaHome.getParentFile();
            jdkSourceZip = new File(javaHomeParent, "src.zip");
            if (jdkSourceZip.exists()) {
                sourcePath.add(new DocPathEntry(jdkSourceZip, ""));
            }
            else {
                // Mac OS X uses "src.jar" with a "src" prefix
                jdkSourceZip = new File(javaHome, "src.jar");
                if (jdkSourceZip.exists()) {
                    sourcePath.add(new DocPathEntry(jdkSourceZip, "src"));
                }
            }
        }
        
        String jdkSourcePath = Config.getPropString(JDK_SOURCE_PATH_PROPERTY, null);
        if (jdkSourcePath != null) {
            sourcePath.add(new DocPathEntry(new File(jdkSourcePath), ""));
        }
        
        this.projectDir = projectDir;
        libraryUrls = getLibrariesClasspath();
        inspectors = new HashMap<Object,Inspector>();
        packages = new TreeMap<String, Package>();
        docuGenerator = new DocuGenerator(this);

        try {
            Package unnamed = new Package(this);
            Properties props = unnamed.getLastSavedProperties();
            setProjectProperties(props);
            packages.put("", unnamed);
            unnamed.refreshPackage();
        } catch (IOException exc) {
            Debug.reportError("could not read package file (unnamed package)");
        }

        debugger = Debugger.getDebuggerImpl(getProjectDir(), getTerminal());
        debugger.setUserLibraries(libraryUrls.toArray(new URL[libraryUrls.size()]));
        debugger.newClassLoader(getClassLoader());
        debugger.addDebuggerListener(this);
        debugger.launch();

        // Check whether this is a shared project
        File ccfFile = new File(projectDir.getAbsoluteFile(), "team.defs");
        isSharedProject = ccfFile.isFile();
        teamActions = new TeamActionGroup(isSharedProject);
    }

    /**
     * Check if the path given is either a directory with a project file or if
     * it is the project file itself (project.greenfoot or package.bluej).
     * 
     * @param projectPath a string representing the path to check. This can
     *            either be a directory name or the filename of a project
     *            file.
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

        return (Package.isPackage(startingDir));
    }

    /**
     * Open a BlueJ project.
     *
     * @param projectPath
     *            a string representing the path to open. This can either be a
     *            directory name or the filename of a bluej.pkg file.
     * @param parent 
     *            Component used as parent if we need to show any messages.
     *            Can be null.
     * @return the Project representing the BlueJ project that has this
     *         directory within it or null if there were no bluej.pkg files in
     *         the specified directory.
     */
    public static Project openProject(String projectPath, Component parent) 
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
        if (Package.isPackage(startingDir)) {
            File curDir = startingDir;
            File lastDir = null;

            startingPackageName = "";

            while ((curDir != null) && Package.isPackage(curDir)) {
                if (lastDir != null) {
                    String lastdirName = lastDir.getName();

                    if (!JavaNames.isIdentifier(lastdirName)) {
                        break;
                    }

                    startingPackageName = "." + lastdirName + startingPackageName;
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
        
        boolean readOnly = false;

        if(Config.isModernWinOS()) {
            WriteCapabilities capabilities = FileUtility.getVistaWriteCapabilities(projectDir);
            switch (capabilities) {
            case VIRTUALIZED_WRITE:
                Utility.bringToFront(null);
                DialogManager.showMessage(parent, "project-is-virtualized");
                break;
            case READ_ONLY:
                readOnly = true;
                break;
            case NORMAL_WRITE:
                break;
            default:
                break;
            }
        }
        else if (!projectDir.canWrite()) {
            readOnly = true;
        }
        
        String greenfootStartupProjectDir = new File(Config.getBlueJLibDir(), "greenfoot/startupProject").getAbsolutePath();
        
        boolean isGreenfootStartupProject = greenfootStartupProjectDir.equals(projectDir.getAbsolutePath()); 
        // Suppress the read-only warning if we know they are opening the Greenfoot startup project
        
        if (readOnly && !isGreenfootStartupProject) {
            Utility.bringToFront(null);
            DialogManager.showMessageWithText(parent, "project-is-readonly", new String [] {projectDir.toString()});
            
            // Prompt user to "Save elsewhere"

            boolean done = false;

            while (!done)
            {
                // Get a file name to save under
                File newName = FileUtility.getDirName(null,
                        Config.getString("pkgmgr.saveAs.title"),
                        Config.getString("pkgmgr.saveAs.buttonLabel"),
                        new JFileChooser().getFileSystemView().getDefaultDirectory(),
                        false, true);

                if (newName != null) {
                    int result = FileUtility.copyDirectory(projectDir, newName);

                    switch (result) {
                    case FileUtility.NO_ERROR:
                        // It worked, use this as the new project:
                        projectDir = newName;
                        done = true;
                        break;

                    case FileUtility.DEST_EXISTS_NOT_DIR:
                        DialogManager.showError(null, "directory-exists-file");
                        break;
                    case FileUtility.DEST_EXISTS_NON_EMPTY:
                        DialogManager.showError(null, "directory-exists-non-empty");
                        break;

                    case FileUtility.SRC_NOT_DIRECTORY:
                    case FileUtility.COPY_ERROR:
                        DialogManager.showError(null, "cannot-save-project");

                        break;
                    }
                }
                else {
                    done = true; // if they pressed cancel, just continue with old project
                }
            }
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
        }
        else {
            proj.initialPackageName = startingPackageName;
        }
        
        ExtensionsManager.getInstance().projectOpening(proj);
        DataCollector.projectOpened(proj, ExtensionsManager.getInstance().getLoadedExtensions(proj));
        

        return proj;
    }

    /**
     * CleanUp the mess left by a project that has now been closed and
     * throw it away.
     */
    public static void cleanUp(Project project) 
    {
        DataCollector.projectClosed(project);
        
        if (project.hasExecControls()) {
            project.getExecControls().dispose();
        }

        if (project.terminal != null) {
            project.terminal.dispose();
        }
        
        if (project.statusFrame != null) {
            project.statusFrame.dispose();
        }

        project.removeAllInspectors();
        project.getDebugger().removeDebuggerListener(project);
        project.getDebugger().close(false);

        PrefMgr.addRecentProject(project.getProjectDir().getAbsolutePath());
        projects.remove(project.getProjectDir());
    }

    /**
     * Create a new project in the directory specified by projectPath.
     * This name must be a directory that does not already exist.
     * For Java ME projects, create directory res/icons in the project folder,
     * and copy the default icon file for midlets from lib/images into res/icons.
     *
     * @param   projectPath     a string representing the path in which
     *                          to make the new project
     * @param   isJavaMEproj    whether or not the project is a Java Micro Edition project
     * @return                  a boolean indicating success or failure
     */
    public static boolean createNewProject(String projectPath, boolean isJavaMEproj ) 
    {
        if (projectPath != null) {
            // check whether name is already in use
            File dir = new File(projectPath);

            if (dir.exists() && (!dir.isDirectory() || dir.list().length > 0)) {
                return false;
            }

            if (dir.exists() || dir.mkdir()) {
                File newreadmeFile = new File(dir, Package.readmeName);
                
                if ( isJavaMEproj ) {
                    File iconsDirectory = new File( dir, MIDletDeployer.ICONS_DIR );   
                    if ( iconsDirectory.mkdirs( ) ) {                        
                        File defaultMidletIconFile = new File( iconsDirectory, 
                                                               MIDletDeployer.DEFAULT_MIDLET_ICON );
                        File iconDefaultSource     = new File( Config.getBlueJLibDir( ), 
                                                               MIDletDeployer.DEFAULT_LIB_ME_ICON );
                        try {
                            FileUtility.copyFile( iconDefaultSource, defaultMidletIconFile );
                        }
                        catch (IOException ioe) {
                            Debug.reportError( "Could not copy default icon file into Project directory.");
                        }
                    } 
                }
                PackageFile pkgFile = PackageFileFactory.getPackageFile(dir);
                try {
                    if (pkgFile.create()) {
                        Properties props = new Properties();
                        props.put(PROJECT_CHARSET_PROP, "UTF-8");
                        if (isJavaMEproj) {
                            props.put(PROJECT_IS_JAVA_ME_PROP, "true");
                        }
                        try {
                            pkgFile.save(props);
                            FileUtility.copyFile(Config.getTemplateFile(
                                    "readme"), newreadmeFile);
                            return true;
                        }
                        catch (IOException ioe) {
                            // TODO should propagate this exception
                            Debug.message("I/O error while creating project.");
                        }
                    }
                } catch (IOException ioe) {
                    // TODO should propagate this exception
                }
            }
        }

        Debug.message("Unable to create project directory: " + projectPath);
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
     * Gets the set of currently open projects. It is an accessor only.
     * @return a Set containing all open projects.
     */
    public static Collection<Project> getProjects() 
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
     * Check whether the project is a Java Micro Edition project.
     */
    public boolean isJavaMEProject()
    {
        return isJavaMEproject;
    }
    
    /**
     * Get the character set that should be used for reading and writing source files
     * in this project. 
     */
    public Charset getProjectCharset()
    {
        return characterSet;
    }
    
    /**
     * Get the project properties to be written to storage when the project is saved.
     */
    public Properties getProjectProperties()
    {
        Properties p = new Properties();
        p.put(PROJECT_CHARSET_PROP, characterSet.name());
        if (isJavaMEproject) {
            p.put(PROJECT_IS_JAVA_ME_PROP, "true");
        }
        return p;
    }
    
    /**
     * Restore project properties (called just after project is opened). 
     */
    private void setProjectProperties(Properties props)
    {
        String charsetName = props.getProperty(PROJECT_CHARSET_PROP);
        if (charsetName != null) {
            try {
                characterSet = Charset.forName(charsetName);
            }
            catch (IllegalCharsetNameException icne) {
                Debug.log("Illegal project character set name: " + charsetName);
            }
            catch (UnsupportedCharsetException ucse) {
                Debug.log("Unsupported project character set: " + charsetName);
            }
        }
        if (characterSet == null) {
            characterSet = Charset.defaultCharset();
            props.put(PROJECT_CHARSET_PROP, characterSet.name());
        }
        
        String isJavaMe = props.getProperty(PROJECT_IS_JAVA_ME_PROP, "false");
        isJavaMEproject = isJavaMe.equals("true");
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
            if (Package.isPackageFileName(startingDir.getName())) {
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
        inspector.update();
        inspector.setVisible(true);
        inspector.bringToFront();
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
            inspector.setVisible(true);
        }
        else {
            updateInspector(inspector);
        }
        
        // See if it is on the bench:
        String benchName = null;
        for (ObjectWrapper ow : PkgMgrFrame.findFrame(pkg).getObjectBench().getObjects())
        {
            if (ow.getObject().equals(obj))
                benchName = ow.getName();
        }
        
        DataCollector.inspectorObjectShow(pkg, inspector, benchName, obj.getClassName(), name);

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
        Inspector inspector = inspectors.remove(obj);
        DataCollector.inspectorHide(this, inspector);
    }
    
    /**
     * Remove an inspector from the list of inspectors for this project
     * @param obj the inspector. 
     */
    public void removeInspector(DebuggerClass cls) 
    {
        Inspector inspector = inspectors.remove(cls.getName());
        DataCollector.inspectorHide(this, inspector);
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
        for (Inspector inspector : inspectors.values()) {
            inspector.setVisible(false);
            inspector.dispose();
            DataCollector.inspectorHide(this, inspector);
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
            inspector.setVisible(true);
        }
        else {
            updateInspector(inspector);
        }
        
        DataCollector.inspectorClassShow(pkg, inspector, clss.getName());

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
        final ResultInspector inspector = new ResultInspector(obj, this, name, pkg, ir, info);
        inspectors.put(obj, inspector);

        DialogManager.centreWindow(inspector, parent);
        inspector.setVisible(true);
        inspector.bringToFront();

        return inspector;
    }

    /**
     * Iterates through all inspectors and updates them
     *
     */
    public void updateInspectors() 
    {
        for (Iterator<Inspector> it = inspectors.values().iterator(); it.hasNext();) {
            Inspector inspector = it.next();
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
     * Return the source path for the project. The source path contains the JDK source,
     * if available, and the source for any other libraries which have been explicitly
     * added.
     */
    public List<DocPathEntry> getSourcePath()
    {
        return sourcePath;
    }
    
    /**
     * Get the project repository. If the user cancels the credentials dialog,
     * or this is not a team project, returns null.
     */
    public Repository getRepository()
    {
        if (isSharedProject) {
            return getTeamSettingsController().getRepository(true);
        }
        else {
            return null;
        }
    }      
        
    /**
     * A string which uniquely identifies this project
     */
    public String getUniqueId() 
    {
        return String.valueOf(new String("BJID" + getProjectDir().getPath()).hashCode());
    }

    /**
     * Get the name of the package represented by the directory which was specified
     * as the directory to open when this project was opened.
     */
    public String getInitialPackageName() 
    {
        return initialPackageName;
    }

    /**
     * Get an existing package from the project. The package is opened (i.e a new
     * Package object is constructed) if it's not already open. All parent packages on
     * the way to the root of the package tree will also be constructed.
     * 
     * @param qualifiedName package name i.e. java.util or "" for unnamed package
     * @returns  the package, or null if the package doesn't exist (directory
     *           doesn't exist, or doesn't contain bluej.pkg file)
     */
    public Package getPackage(String qualifiedName)
    {
        Package existing = packages.get(qualifiedName);

        if (existing != null) {
            // The unnamed package is always already open, so that case is
            // handled here.
            return existing;
        }

        if (qualifiedName.length() > 0) {
            Package pkg;

            try {
                Package parent = getPackage(JavaNames.getPrefix(qualifiedName));

                if (parent != null) {
                    // Note, construction of the new package throws IOException if
                    // the directory or bluej.pkg file doesn't exist
                    pkg = new Package(this, JavaNames.getBase(qualifiedName),
                            parent);
                    packages.put(qualifiedName, pkg);
                    pkg.refreshPackage();
                } else { // parent package does not exist. How can it not exist ?
                    pkg = null;
                }
            } catch (IOException exc) {
                // the package did not exist in this project
                pkg = null;
            }
            
            return pkg;
        }

        // Default package is not in the package cache. This should never happen...
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
     * Returns a package from the project. The package must have already been
     * opened.
     *
     * @param qualifiedName package name ie java.util or "" for unnamed package
     * @return null if the named package cannot be found
     */
    public Package getCachedPackage(String qualifiedName)
    {
        return packages.get(qualifiedName);
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
            PackageFile pkgFile = PackageFileFactory.getPackageFile(newPkgDir);
            try {
                pkgFile.create();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            while (st.hasMoreTokens()) {
                newPkgDir = new File(newPkgDir, st.nextToken());
                prepareCreateDir(newPkgDir);

                pkgFile = PackageFileFactory.getPackageFile(newPkgDir);
                try {
                    pkgFile.create();
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
    public int newPackage(String qualifiedName)
    {
        if (qualifiedName == null) {
            return NEW_PACKAGE_BAD_NAME;
        }

        Package existing = packages.get(qualifiedName);

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
            pkg.refreshPackage();
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
    private List<String> getPackageNames(Package rootPackage)
    {
        List<String> l = new LinkedList<String>();
        List<Package> children;

        l.add(rootPackage.getQualifiedName());

        children = rootPackage.getChildren(true);
        Iterator<Package> i = children.iterator();
        
        while (i.hasNext()) {
            Package p = i.next();
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
    public List<String> getPackageNames()
    {
        return getPackageNames(getPackage(""));
    }

    /**
     * Generate documentation for the whole project.
     * @return "" if everything was alright, an error message otherwise.
     */
    public String generateDocumentation()
    {
        return docuGenerator.generateProjectDocu();
    }

    public String getDocumentationFile(String filename)
    {
        return docuGenerator.getDocuPath(filename);
    }

    /**
    * Generate the documentation for the file in 'filename'
     * @param filename
     */
    public void generateDocumentation(String filename)
    {
        docuGenerator.generateClassDocu(filename);
    }

    /**
     * Save all open packages of this project. This doesn't save files open
     * in editor windows - use saveAllEditors() for that.
     */
    public void saveAll()
    {
        PkgMgrFrame[] frames = PkgMgrFrame.getAllProjectFrames(this);

        if (frames == null) {
            return;
        }

        for (int i = 0; i < frames.length; i++) {
            frames[i].doSave();
            frames[i].setStatus(Config.getString("pkgmgr.packageSaved"));
        }
    }

    /**
     * Request all open editor windows for the current project to save their
     * contents (if modified).
     */
    public void saveAllEditors() throws IOException
    {
        Iterator<Package> i = packages.values().iterator();
        IOException exception = null;

        while(i.hasNext()) {
            Package pkg = (Package) i.next();
            try {
                pkg.saveFilesInEditors();
            }
            catch(IOException ioe) {
                exception = ioe;
                Debug.reportError("Error while trying to save editor file:", ioe);
            }
        }
        
        if (exception != null) {
            // Propagate the exception - let the caller know that something went wrong.
            throw exception;
        }
    }
    
    /**
     * Reload all constructed packages of this project.
     *
     * <p>This function is used after a major change to the contents
     * of the project directory ie an import.
     */
    public void reloadAll()
    {
        Iterator<Package> i = packages.values().iterator();

        while (i.hasNext()) {
            Package pkg = (Package) i.next();

            pkg.reload();
        }
    }

    /**
     * Make all open package editors clear their selection
     */
    public void clearAllSelections()
    {
        Iterator<Package> i = packages.values().iterator();

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
     * the targets given in the parameter targets.
     * 
     * @param targets a list of Targets
     */
    public void selectTargetsInGraphs(List<Target> targets)
    {
        for (Iterator<Target> i = targets.iterator(); i.hasNext();) {
            Target target = i.next();
            if (target != null){
                PackageEditor packageEditor = target.getPackage().getEditor();
                packageEditor.addToSelection(target);
                packageEditor.repaint();
            }
        }
    }
    
    /**
     * Given a fully-qualified target name, return the target or null if the target
     * doesn't exist.
     * 
     * <p>Use ReadmeTarget.README_ID ("@README") as the target base name to get the
     * readme target for a package.
     * 
     * Given the path and name of a target in the project, return the target or
     * null if the target doesn't exist
     * @param pathAndName
     * @return the target
     */
    public Target getTarget(String targetId)
    {
        String packageName = "";
        int index = targetId.lastIndexOf('.');
        if (index > 0) {
            packageName = targetId.substring(0, index);
            targetId = targetId.substring(index + 1);
        }
        Package p = getPackage(packageName);
        if (p == null) {
            return null;
        }
        Target target = p.getTarget(targetId);
        return target;
    }
    
    /**
     * Open the source editor for each target that is selected in its
     * package editor
     */
    public void openEditorsForSelectedTargets()
    {
        List<Target> selectedTargets = getSelectedTargets();
        for (Iterator<Target> i = selectedTargets.iterator(); i.hasNext(); ){
            Target target = i.next();
            if (target instanceof ClassTarget){
                ClassTarget classTarget = (ClassTarget) target;
                Editor editor = classTarget.getEditor();
                if (editor != null) {
                    editor.setVisible(true);
                    // TODO: make moe select the ======== part of cvs conflicts
                }
            }
        }
    }
    
    /**
     * Get a list of selected targets (in all packages)
     * @return a list of the selected targets
     */
    private List<Target> getSelectedTargets()
    {
        List<Target> selectedTargets = new LinkedList<Target>();
        List<String> packageNames = getPackageNames();
        for (Iterator<String> i = packageNames.iterator(); i.hasNext();) {
            String packageName = i.next();
            Package p = getPackage(packageName);
            selectedTargets.addAll(Arrays.asList(p.getSelectedTargets()));
        }
        return selectedTargets;
    }
    
    /**
     * Explicitly restart the remote debug VM. The VM first gets shut down, and then
     * restarted.
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
        
        Package pkg = null;
        Iterator<Package> i = packages.values().iterator();
        while (i.hasNext()) {
            pkg = (Package) i.next();
            pkg.reInitBreakpoints();           
        }
        PkgMgrFrame frame = PkgMgrFrame.getMostRecent();
        if (frame != null) {
            Utility.bringToFront(frame);
        }        
    }

    /**
     * The remote VM for this project has just been closed. Remove everything in this
     * project that depended on that VM.
     */
    private void vmClosed()
    {
        // any calls to the debugger made by removeLocalClassLoader
        // will silently fail
        removeClassLoader();
        
        // The configured extra libraries may have changed, so
        // rebuild the class loader (do this now so the new loader
        // will be installed as soon as the VM has restarted).
        newRemoteClassLoader();
        
        libraryUrls = getLibrariesClasspath();
        debugger.setUserLibraries(libraryUrls.toArray(new URL[libraryUrls.size()]));
        
        // Breakpoints will be re-initialized once the new VM has
        // actually started.
    }

    /**
     * Removes the current classloader, and removes
     * references to classes loaded by it (this includes removing
     * the objects from all object benches of this project).
     */
    public void removeClassLoader()
    {
        // There is nothing to do if the current classloader is null.
        if (currentClassLoader == null) {
            return;
        }
        
        clearObjectBenches();

        // get rid of any inspectors that are open that were not cleaned up
        // as part of removing objects from the bench
        removeAllInspectors();

        // remove views for classes loaded by this classloader
        View.removeAll(currentClassLoader);

        if (! Config.isGreenfoot()) {
            // dispose windows for local classes. Should not run user code
            // on the event queue, so run it in a separate thread.
            new Thread() {
                public void run() {
                    getDebugger().disposeWindows();
                }
            }.start();
        }

        currentClassLoader = null;
    }

    /**
     * Clears the objects from all object benches belonging to this project.
     */
    public void clearObjectBenches()
    {
        // remove bench objects for all frames in this project
        PkgMgrFrame[] frames = PkgMgrFrame.getAllProjectFrames(this);

        for (int i = 0; i < frames.length; i++) {
            frames[i].getObjectBench().removeAllObjects(getUniqueId());
            frames[i].clearTextEval();
        }
    }

    /**
     * Creates a new debugging VM classloader.
     * Breakpoints are discarded.
     */
    public void newRemoteClassLoader()
    {
        getDebugger().newClassLoader(getClassLoader());
    }

    /**
     * Creates a new debugging VM classloader, leaving current breakpoints.
     */
    public void newRemoteClassLoaderLeavingBreakpoints()
    {
        getDebugger().newClassLoader(getClassLoader());

        Iterator<Package> i = packages.values().iterator();
        while (i.hasNext()) {
            Package pkg = (Package) i.next();
            pkg.reInitBreakpoints();
        }
    }

    public Debugger getDebugger()
    {
        return debugger;
    }

    public boolean hasExecControls()
    {
        return execControls != null;
    }

    public ExecControls getExecControls()
    {
        if (execControls == null) {
            execControls = new ExecControls(this, getDebugger());
        }

        return execControls;
    }

    public boolean hasTerminal()
    {
        return terminal != null;
    }

    public Terminal getTerminal()
    {
        if (terminal == null) {
            terminal = new Terminal(this);
        }

        return terminal;
    }

    /**
     * Loads a class using the current classLoader
     */
    public Class<?> loadClass(String className)
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

    public boolean inTestMode()
    {
        return inTestMode;
    }

    public void setTestMode(boolean mode)
    {
        inTestMode = mode;
    }
  
    /**
     * Return a list of URL of the Java ME libraries specified in the 
     * configuration files. The libraries are physically located in the 'lib'  
     * subdirectory of the Wireless Toolkit directory. 
     * @param type   "optional" or "core", the type of libraries to process
     * @return a non null but possibly empty list of URL.
     */
    protected List<URL> getJavaMELibraries( String type ) 
    {
        List<URL> risul = new ArrayList<URL>( );
        String toolkitDir = Config.getPropString( "bluej.javame.toolkit.dir", null );
        
        String libs;   //string of java me libraries to parse
        if ( type.equals( "core" ) )
            libs = Config.getPropString( "bluej.javame.corelibraries", null );
        else if ( type.equals( "optional" ) )
            libs = Config.getPropString( "bluej.javame.optlibraries", null );
        else
            libs = null;
        
        if ( toolkitDir != null  &&  libs != null )
        {            
            String libDir = toolkitDir + File.separator + "lib" + File.separator;
            StringTokenizer st = new StringTokenizer( libs );
            while ( st.hasMoreTokens( ) ) {
                try {
                    File file = new File( libDir + st.nextToken( ) );
                    risul.add( file.toURI( ).toURL( ) );
                }
                catch( MalformedURLException mue ) { 
                    Debug.reportError( st.nextToken( ) + " is a Java ME malformed file." );
                }
            }
        }  
        return risul;
    }
        
    /**
     * Returns a list of URL having in it all libraries that are in the +libs directory
     * of this project.
     * @return a non null but possibly empty list of URL.
     */
    protected ArrayList<URL> getPlusLibsContent () 
    {
        ArrayList<URL> risul = new ArrayList<URL>();
        
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
     * Attempts to add a library to the given list of libraries.
     * A valid library file is one that is a file, readable, ends either with zip or jar.
     * Before addition the file is transformed to a URL.
     * @param risul where to add the file
     * @param aFile the file to be added.
     */
    private static final void attemptAddLibrary ( ArrayList<URL> risul, File aFile ) 
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
    public static final ArrayList<URL> getUserlibContent() 
    {
        ArrayList<URL> risul = new ArrayList<URL>();
        File userLibDir;
        
        // The userlib location may be specified in bluej.defs
        String userLibSetting = Config.getPropString("bluej.userlibLocation", null);
        if (userLibSetting == null) {
            userLibDir = new File(Boot.getBluejLibDir(), "userlib");
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
       
        ArrayList<URL> pathList = new ArrayList<URL>();
        
        List<URL> coreLibs = new ArrayList<URL>(); //Java ME core libraries
        List<URL> optLibs  = new ArrayList<URL>(); //java ME optional libraries

        try {
            Collections.addAll(pathList, Boot.getInstance().getRuntimeUserClassPath());
    
            pathList.addAll(libraryUrls);
            
            // The current project dir must be added to the project class path too.
            pathList.add(getProjectDir().toURI().toURL());
            
            //Add Java ME jars if this is a Java ME project. 
            if ( isJavaMEproject ) { 
                coreLibs = getJavaMELibraries( "core"     ) ;
                optLibs  = getJavaMELibraries( "optional" ) ;                
                pathList.addAll( coreLibs );  
                pathList.addAll( optLibs );         
            }
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
        currentClassLoader = new BPClassLoader( newUrls,
                     Boot.getInstance().getBootClassLoader(), isJavaMEproject );
        
        currentClassLoader.setJavaMEcoreLibs(coreLibs);
        currentClassLoader.setJavaMEoptLibs (optLibs);
        
        return currentClassLoader;
    }
    
    /**
     * Get the classpath for libraries - those specified in preferences, in the project's +libs,
     * in the BlueJ userlib folder, etc. This doesn't include the BlueJ runtime.
     * 
     * <p>Most of the time, the list in {@code libraryUrls} should be used instead of calling this
     * method, as it represents the libraries known to the currently executing VM.
     */
    private List<URL> getLibrariesClasspath()
    {
        ArrayList<URL> pathList = new ArrayList<URL>();
        
        // Next part is the libraries that are added trough the config panel.
        pathList.addAll(PrefMgrDialog.getInstance().getUserConfigLibPanel().getUserConfigContent());
        
        // Then the libraries that are in the userlib directory
        pathList.addAll(getUserlibContent());
        
        // The libraries that are in the project +libs directory
        pathList.addAll(getPlusLibsContent());
        
        return pathList;
    }

    /**
     * Get an entity resolver which can be used to resolve symbols for this project.
     * 
     * @return an entity resolver which resolves symbols from classes in this project,
     *         and from the classpath.
     */
    public EntityResolver getEntityResolver()
    {
        return new ProjectEntityResolver(this);
    }
    
    /**
     * Get a javadoc resolver, which can be used to retrieve comments for methods.
     */
    public JavadocResolver getJavadocResolver()
    {
        return javadocResolver;
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
        Iterator<Package> i = packages.values().iterator();

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
    public void processDebuggerEvent(final DebuggerEvent event, boolean skipUpdate)
    {
        if (skipUpdate) {
            return;
        }
        
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
                        removeStepMarks();
                        vmClosed();
                    }

                    // check whether we failed to create the VM
                    if (newState == Debugger.LAUNCH_FAILED) {
                        BlueJEvent.raiseEvent(BlueJEvent.CREATE_VM_FAILED, null);
                    }

                    return;
                }

                DebuggerThread thr = event.getThread();
                if (thr == null) {
                    return; // Not a thread event
                }
                String packageName = JavaNames.getPrefix(thr.getClass(0));
                Package pkg = getPackage(packageName);

                if (pkg != null) {
                    switch (event.getID()) {
                    case DebuggerEvent.THREAD_BREAKPOINT:
                        pkg.hitBreakpoint(thr);
                        break;

                    case DebuggerEvent.THREAD_HALT_UNKNOWN:
                    case DebuggerEvent.THREAD_HALT_STEP_INTO:
                    case DebuggerEvent.THREAD_HALT_STEP_OVER:
                        pkg.hitHalt(thr);
                        break;
                    }
                }
                
                switch (event.getID())
                {
                    case DebuggerEvent.THREAD_HALT_UNKNOWN:
                        DataCollector.debuggerHalt(Project.this, thr.getName(), ExecControls.getFilteredStack(thr.getStack()));
                        break;
                    case DebuggerEvent.THREAD_HALT_STEP_INTO:
                        DataCollector.debuggerStepInto(Project.this, thr.getName(), ExecControls.getFilteredStack(thr.getStack()));
                        break;
                    case DebuggerEvent.THREAD_HALT_STEP_OVER:
                        DataCollector.debuggerStepOver(Project.this, thr.getName(), ExecControls.getFilteredStack(thr.getStack()));
                        break;
                    case DebuggerEvent.THREAD_BREAKPOINT:
                        DataCollector.debuggerHitBreakpoint(Project.this, thr.getName(), ExecControls.getFilteredStack(thr.getStack()));
                        break;
                }
            }
        });
    }
    
    /**
     * Show the source code at a particular position
     */
    public void showSource(String className, String sourceName, int lineNumber)
    {
        String packageName = JavaNames.getPrefix(className);
        Package pkg = getPackage(packageName);
        if (pkg != null) {
            pkg.showSourcePosition(sourceName, lineNumber);
        }
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
        Package pkg = packages.get(packageQualifiedName);
        if (pkg != null) {
            List<Package> childPackages = pkg.getChildren(false);
            Iterator<Package> i = childPackages.iterator();
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
     * @param includePkgFiles   true if package layout files should be included
     * @param includeDirs       true if directories should be included
     * @return List of File objects 
     */
    public Set<File> getFilesInProject(boolean includePkgFiles, boolean includeDirs)
    {
        Set<File> files = new LinkedHashSet<File>();
        if (includeDirs) {
            files.add(projectDir);
        }
        traverseDirsForFiles(files, projectDir, includePkgFiles, includeDirs);
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
     * Traverse the directory tree starting in dir an add all the encountered 
     * files to the List allFiles. The parameter includePkgFiles determine 
     * whether bluej.pkg files should be added to allFiles as well.
     * @param allFiles a List to which the method will add the files it meets.
     * @param dir the directory the search starts from
     * @param includePkgFiles if true, bluej.pkg files are included as well.
     */
    private void traverseDirsForFiles(Set<File> allFiles, File dir, boolean includePkgFiles,
            boolean includeDirs)
    {
        TeamSettingsController teamSettingsController = getTeamSettingsController();
        File[] files = dir.listFiles(teamSettingsController == null ? null : teamSettingsController.getFileFilter(includePkgFiles));
        if (files==null){
            return;
        }
        for(int i=0; i< files.length; i++ ){
            if (files[i].isFile()) {
                allFiles.add(files[i]);
            } else {
                if (includeDirs) {
                    allFiles.add(files[i]);
                }
                traverseDirsForFiles(allFiles, files[i], includePkgFiles, includeDirs);
            }
        }
    }

    /**
     * Get the team settings dialog for this project. Only call this if the
     * project is a shared project.
     */
    public TeamSettingsDialog getTeamSettingsDialog()
    {
        return getTeamSettingsController().getTeamSettingsDialog();
    }
    
    /**
     * Get the commit dialog for this project
     */
    public CommitCommentsFrame getCommitCommentsDialog()
    {
        // lazy instantiation of commit comments frame
        if(commitCommentsFrame == null) {
            commitCommentsFrame = new CommitCommentsFrame(this);
        }
        return commitCommentsFrame;
    }
    
    /**
     * Get the update dialog for this project
     */
    public UpdateFilesFrame getUpdateDialog()
    {
        if (updateFilesFrame == null) {
            updateFilesFrame = new UpdateFilesFrame(this);
        }
        return updateFilesFrame;
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
     * Might return null if the file isn't in the package, or the directory the
     * file is in doesn't translate to a valid package name. However, may
     * return a valid package name which doesn't actually exist as a package
     * in the project.
     */
    public String getPackageForFile(File f)
    {
        File projdir = getProjectDir();
        
        // First find out the package name...
        String packageName = "";
        File parentDir = f.getParentFile();
        while (! parentDir.equals(projdir)) {
            String parentName = parentDir.getName();
            if (!JavaNames.isIdentifier(parentName)) {
                return null;
            }
            
            if (packageName.equals("")) {
                packageName = parentName;
            }
            else {
                packageName = parentName + "." + packageName;
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
            statusFrame.setLocationRelativeTo(parent);
        }
        return statusFrame;
    }
    
    /**
     * Prepare for the deletion of a directory inside the project. This is
     * a notification which allows the team management code to save the
     * version control metadata elsewhere, if necessary.
     */
    public boolean prepareDeleteDir(File dir)
    {
        TeamSettingsController tsc = getTeamSettingsController();
        if (tsc != null) {
            return tsc.prepareDeleteDir(dir);
        }
        else {
            return true;
        }
    }
    
    /**
     * Prepare for the creation of a directory inside the project. This is a 
     * notification which allows the team management code to perform any
     * necessary metadata actions.
     */
    public void prepareCreateDir(File dir)
    {
        TeamSettingsController tsc = getTeamSettingsController();
        if (tsc != null) {
            tsc.prepareCreateDir(dir);
        }
    }

    /*
     * @see bluej.debugger.DebuggerListener#examineDebuggerEvent(bluej.debugger.DebuggerEvent)
     */
    @Override
    public boolean examineDebuggerEvent(DebuggerEvent e)
    {
        return false;
    }
}
