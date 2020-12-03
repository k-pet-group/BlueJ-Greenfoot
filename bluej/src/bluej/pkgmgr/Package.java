/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020  Michael Kolling and John Rosenberg
 
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import bluej.debugger.DebuggerObject;
import bluej.views.CallableView;
import com.google.common.collect.Sets;
import javafx.application.Platform;

import bluej.compiler.CompileInputFile;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.pkgmgr.target.CSSTarget;
import bluej.pkgmgr.target.DependentTarget.State;
import bluej.pkgmgr.dependency.ExtendsDependency;
import bluej.pkgmgr.dependency.ImplementsDependency;
import bluej.utility.javafx.JavaFXUtil;
import bluej.Config;
import bluej.collect.DataCollectionCompileObserverWrapper;
import bluej.collect.DataCollector;
import bluej.compiler.*;
import bluej.debugger.*;
import bluej.debugmgr.CallHistory;
import bluej.debugmgr.Invoker;
import bluej.editor.Editor;
import bluej.editor.TextEditor;
import bluej.extensions2.BPackage;
import bluej.extensions2.ExtensionBridge;
import bluej.extensions2.SourceType;
import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.CompileEvent.EventType;
import bluej.extmgr.ExtensionsManager;
import bluej.parser.AssistContent;
import bluej.parser.AssistContent.CompletionKind;
import bluej.parser.ExpressionTypeInfo;
import bluej.parser.ParseUtils;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.symtab.ClassInfo;
import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.dependency.ExtendsDependency;
import bluej.pkgmgr.dependency.ImplementsDependency;
import bluej.pkgmgr.dependency.UsesDependency;
import bluej.pkgmgr.target.*;
import bluej.pkgmgr.target.DependentTarget.State;
import bluej.prefmgr.PrefMgr;
import bluej.utility.*;
import bluej.utility.filefilter.FrameSourceFilter;
import bluej.utility.filefilter.JavaClassFilter;
import bluej.utility.filefilter.JavaSourceFilter;
import bluej.utility.filefilter.SubPackageFilter;
import bluej.utility.javafx.JavaFXUtil;
import bluej.views.CallableView;
import javafx.application.Platform;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * A Java package (collection of Java classes).
 * 
 * @author Michael Kolling
 * @author Axel Schmolitzky
 * @author Andrew Patterson
 */
public final class Package
{
    /** message to be shown on the status bar */
    static final String compiling = Config.getString("pkgmgr.compiling");
    /** message to be shown on the status bar */
    static final String compileDone = Config.getString("pkgmgr.compileDone");
    /** message to be shown on the status bar */
    static final String chooseUsesTo = Config.getString("pkgmgr.chooseUsesTo");
    /** message to be shown on the status bar */
    static final String chooseInhTo = Config.getString("pkgmgr.chooseInhTo");

    /**
     * the name of the package file in a package directory that holds
     * information about the package and its targets.
     */
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private PackageFile packageFile;
    
    /** Readme file name */
    public static final String readmeName = "README.TXT";

    /** error code */
    public static final int NO_ERROR = 0;
    /** error code */
    public static final int FILE_NOT_FOUND = 1;
    /** error code */
    public static final int ILLEGAL_FORMAT = 2;
    /** error code */
    public static final int COPY_ERROR = 3;
    /** error code */
    public static final int CLASS_EXISTS = 4;
    /** error code */
    public static final int CREATE_ERROR = 5;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private final List<Target> targetsToPlace = new ArrayList<>();
    // Has this package been sent for data recording yet?
    private boolean recorded = false;

    /** Reason code for displaying source line */
    private enum ShowSourceReason
    {
        STEP_OR_HALT,    // a step or other halt 
        BREAKPOINT_HIT,  // a breakpoint was hit
        FRAME_SELECTED;   // the stack frame was selected in the debugger
        
        /**
         * Check whether this reason corresponds to a suspension event (breakpoint/step/etc).
         */
        public boolean isSuspension()
        {
            return this == STEP_OR_HALT || this == BREAKPOINT_HIT;
        }
    }
    
    /**
     * In the top left corner of each package we have a fixed target - either a
     * ParentPackageTarget or a ReadmeTarget. These are there locations
     */
    public static final int FIXED_TARGET_X = 10;
    public static final int FIXED_TARGET_Y = 10;

    /** the Project this package is in */
    private final Project project;

    /**
     * the parent Package object for this package or null if this is the unnamed
     * package ie. the root of the package tree
     */
    private final Package parentPackage;

    /** base name of package (eg util) ("" for the unnamed package) */
    private final String baseName;

    /**
     * this properties object contains the properties loaded off disk for this
     * package, or the properties which were most recently saved to disk for
     * this package
     */
    private volatile SortedProperties lastSavedProps = new SortedProperties();

    /** all the targets in a package */
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private final TargetCollection targets;

    /** Holds the choice of "from" target for a new dependency */
    @OnThread(Tag.FXPlatform)
    private DependentTarget fromChoice;

    /** the CallHistory of a package */
    private CallHistory callHistory;

    /**
     * needed when debugging with breakpoints to see if the editor window needs
     * to be brought to the front
     */
    private String lastSourceName = "";
    
    /** determines the maximum length of the CallHistory of a package */
    public static final int HISTORY_LENGTH = 6;
    
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private PackageListener editor;

    private PackageUI ui;
    
    @OnThread(Tag.FXPlatform)    
    private List<PackageListener> listeners = new ArrayList<>();

    //package-visible
    List<UsesDependency> getUsesArrows()
    {
        return usesArrows;
    }

    //package-visible
    List<Dependency> getExtendsArrows()
    {
        return extendsArrows;
    }

    @OnThread(value = Tag.FXPlatform)
    private final List<UsesDependency> usesArrows = new ArrayList<>();

    @OnThread(value = Tag.FXPlatform)
    private final List<Dependency> extendsArrows = new ArrayList<>();
    
    /** True if we currently have a compile queued up waiting for debugger to become idle */
    @OnThread(Tag.FXPlatform)
    private boolean waitingForIdleToCompile = false;
    
    /** Whether we have issued a package compilation, and not yet seen its conclusion */
    private boolean currentlyCompiling = false;
    
    /** Whether a compilation has been queued (behind the current compile job). Only one compile can be queued. */
    private boolean queuedCompile = false;
    private CompileReason queuedReason;
    
    private final List<FXCompileObserver> compileObservers = new ArrayList<>();
    
    /** File pointing at the directory for this package */
    @OnThread(Tag.Any)
    private File dir;

    /* ------------------- end of field declarations ------------------- */

    /**
     * Create a package of a project with the package name of baseName (ie
     * reflect) and with a parent package of parent (which may represent
     * java.lang for instance) If the package file (bluej.pkg) is not found, an
     * IOException is thrown.
     */
    public Package(Project project, String baseName, Package parent)
        throws IOException
    {
        if (parent == null)
            throw new NullPointerException("Package must have a valid parent package");

        if (baseName.length() == 0)
            throw new IllegalArgumentException("unnamedPackage must be created using Package(project)");

        if (!JavaNames.isIdentifier(baseName))
            throw new IllegalArgumentException(baseName + " is not a valid name for a Package");

        this.project = project;
        this.baseName = baseName;
        this.parentPackage = parent;
        this.targets = new TargetCollection();

        init();
    }

    /**
     * Create the unnamed package of a project If the package file (bluej.pkg)
     * is not found, an IOException is thrown.
     */
    public Package(Project project)
        throws IOException
    {
        this.project = project;
        this.baseName = "";
        this.parentPackage = null;
        this.targets = new TargetCollection();
        init();
    }

    private void init()
        throws IOException
    {
        callHistory = new CallHistory(HISTORY_LENGTH);
        dir = new File(project.getProjectDir(), getRelativePath().getPath());
        load();
    }

    @OnThread(Tag.Any)
    public boolean isUnnamedPackage()
    {
        return parentPackage == null;
    }

    /**
     * Return the project this package belongs to.
     */
    @OnThread(Tag.Any)
    public Project getProject()
    {
        return project;
    }

    @OnThread(value = Tag.Any,requireSynchronized = true)
    private BPackage singleBPackage;  // Every Package has none or one BPackage
    
    /**
     * Return the extensions BPackage associated with this Package.
     * There should be only one BPackage object associated with each Package.
     * @return the BPackage associated with this Package.
     */
    @OnThread(Tag.Any)
    public synchronized final BPackage getBPackage ()
    {
        if ( singleBPackage == null )
          singleBPackage = ExtensionBridge.newBPackage(this);
          
        return singleBPackage;
    }


    /**
     * Get the unique identifier for this package (it's directory name at
     * present)
     */
    @OnThread(Tag.Any)
    public String getId()
    {
        return getPath().getPath();
    }

    /**
     * Return this package's base name (eg util) ("" for the unnamed package)
     */
    @OnThread(Tag.Any)
    public String getBaseName()
    {
        return baseName;
    }

    /**
     * Return the qualified name of an identifier in this package (eg
     * java.util.Random if given Random)
     */
    @OnThread(Tag.Any)
    public String getQualifiedName(String identifier)
    {
        if (isUnnamedPackage())
            return identifier;
        else
            return getQualifiedName() + "." + identifier;
    }

    /**
     * Return the qualified name of the package (eg. java.util) ("" for the
     * unnamed package)
     */
    @OnThread(Tag.Any)
    public String getQualifiedName()
    {
        Package currentPkg = this;
        String retName = "";

        while (!currentPkg.isUnnamedPackage()) {
            if (retName.equals("")) {
                retName = currentPkg.getBaseName();
            }
            else {
                retName = currentPkg.getBaseName() + "." + retName;
            }

            currentPkg = currentPkg.getParent();
        }

        return retName;
    }

    /**
     * get the readme target for this package
     *  
     */
    public synchronized ReadmeTarget getReadmeTarget()
    {
        ReadmeTarget readme = (ReadmeTarget) targets.get(ReadmeTarget.README_ID);
        return readme;
    }

    /**
     * Construct a path for this package relative to the project.
     * 
     * @return The relative path.
     */
    private File getRelativePath()
    {
        Package currentPkg = this;
        File retFile = new File(currentPkg.getBaseName());

        /*
         * loop through our parent packages constructing a relative path for
         * this file
         */
        while (!currentPkg.isUnnamedPackage()) {
            currentPkg = currentPkg.getParent();

            retFile = new File(currentPkg.getBaseName(), retFile.getPath());
        }

        return retFile;
    }

    /**
     * Return a file object of the directory location of this package.
     * 
     * @return The file object representing the full path to the packages
     *         directory
     */
    @OnThread(Tag.Any)
    public File getPath() 
    {
        return dir;
    }

    /**
     * Return our parent package or null if we are the unnamed package.
     */
    @OnThread(Tag.Any)
    public Package getParent()
    {
        return parentPackage;
    }

    /**
     * Returns the sub-package if this package is "boring". Our definition of
     * boring is that the package has no classes in it and only one sub package.
     * If this package is not boring, this method returns null.
     */
    protected synchronized Package getBoringSubPackage()
    {
        PackageTarget pt = null;

        for (Iterator<Target> e = targets.iterator(); e.hasNext();) {
            Target target = e.next();

            if (target instanceof ClassTarget)
                return null;

            if ((target instanceof PackageTarget) && !(target instanceof ParentPackageTarget)) {
                // we have found our second sub package
                // this means this package is not boring
                if (pt != null)
                    return null;

                pt = (PackageTarget) target;
            }
        }

        if (pt == null)
            return null;

        return getProject().getPackage(pt.getQualifiedName());
    }

    /**
     * Return an array of package objects which are nested one level below us.
     * 
     * @param getUncached   should be true if unopened packages should be included
     */
    protected synchronized List<Package> getChildren(boolean getUncached)
    {
        List<Package> children = new ArrayList<Package>();

        for (Iterator<Target> e = targets.iterator(); e.hasNext();) {
            Target target = e.next();

            if (target instanceof PackageTarget && !(target instanceof ParentPackageTarget)) {
                PackageTarget pt = (PackageTarget) target;

                Package child;
                if (getUncached) {
                    child = getProject().getPackage(pt.getQualifiedName());
                }
                else {
                    child = getProject().getCachedPackage(pt.getQualifiedName());
                }

                if (child == null)
                    continue;

                children.add(child);
            }
        }

        return children;
    }

    public void setStatus(String msg)
    {
        PkgMgrFrame.displayMessage(this, msg);
    }

    /**
     * Sets the PackageEditor for this package.
     * @param ed The PackageEditor.  Non-null when opening, null when
     *           closing.
     */
    @OnThread(Tag.FXPlatform)
    void setEditor(PackageEditor ed)
    {
        setUI(ed);
        synchronized (this)
        {
            if (this.editor != null)
            {
                removeListener(ed);
            }
            this.editor = ed;
            if (ed != null)
            {
                addListener(ed);
            }
        }
        
        if (ed == null) {
            fireClosedEvent();
        }

        // Note we use ed here, not editor, as editor needs synchronized access
        if (ed != null)
        {
            synchronized (this)
            {
                for (Target t : targets)
                {
                    if (t instanceof ParentPackageTarget)
                    {
                        ed.findSpaceForVertex(t);
                    }
                }
                // Find an empty spot for any targets which didn't already have
                // a position
                for (Target t : targetsToPlace)
                {
                    ed.findSpaceForVertex(t);
                }
                targetsToPlace.clear();
            }
            ed.graphChanged();
        }
    }
    
    /**
     * Get the editor for this package, as a PackageEditor. This should be considered deprecated;
     * use getUI() instead if possible. May return null.
     */
    @OnThread(Tag.Any)
    public synchronized PackageEditor getEditor()
    {
        return (PackageEditor) editor;
    }
    
    /**
     * Set the UI controller for this package.
     */
    public void setUI(PackageUI ui)
    {
        this.ui = ui;
    }
    
    /**
     * Retrieve the UI controller for this package. (May return null if no UI has been set; however,
     * most operations requiring the UI should be performed in contexts where the UI has been set, so
     * it should normally be safe to assume non-null return).
     */
    public PackageUI getUI()
    {
        return ui;
    }

    /**
     * Add a listener for this package.
     */
    public synchronized void addListener(PackageListener pl)
    {
        listeners.add(pl);
    }
    
    /**
     * Remove a listener for this package.
     */
    public synchronized void removeListener(PackageListener pl)
    {
        listeners.remove(pl);
    }
    
    /**
     * Fire a "package closed" event to listeners.
     */
    @OnThread(Tag.FXPlatform)
    private void fireClosedEvent()
    {
        // Note we take a copy of the listener list as listeners will probably be removed during processing.
        List<PackageListener> listenersCopy = new ArrayList<PackageListener>(listeners);
        for (PackageListener l : listenersCopy)
        {
            l.graphClosed();
        }
    }
    
    /**
     * Fire a "graph changed" event to listeners.
     */
    @OnThread(Tag.FXPlatform)
    private void fireChangedEvent()
    {
        for (PackageListener l : listeners)
        {
            l.graphChanged();
        }
    }
    
    /**
     * Get the package properties, as most recently saved. The returned Properties set should be considered
     * immutable.
     */
    @OnThread(Tag.Any)
    public Properties getLastSavedProperties()
    {
        return lastSavedProps;
    }

    /**
     * Get the currently selected Targets. It will return an empty array if no
     * target is selected.
     * 
     * @return the currently selected array of Targets.
     */
    @OnThread(Tag.Any)
    public synchronized List<Target> getSelectedTargets()
    {
        return Utility.filterList(getVertices(), Target::isSelected);
    }

    /**
     * Search a directory for Java source and class files and add their names to
     * a set which is returned. Will delete any __SHELL files which are found in
     * the directory and will ignore any single .class files which do not
     * contain public classes.
     * 
     * The returned set is guaranteed to be only valid Java identifiers.
     */
    private Set<String> findTargets(File path)
    {
        File javaSrcFiles[] = path.listFiles(new JavaSourceFilter());
        File frameSrcFiles[] = path.listFiles(new FrameSourceFilter());
        File classFiles[] = path.listFiles(new JavaClassFilter());

        Set<String> interestingSet = new HashSet<String>();

        // process all *.java files
        for (int i = 0; i < javaSrcFiles.length; i++) {
            // remove all __SHELL*.java files (temp files created by us)
            if (javaSrcFiles[i].getName().startsWith(Invoker.SHELLNAME)) {
                javaSrcFiles[i].delete();
                continue;
            }
            String javaFileName = JavaNames.stripSuffix(javaSrcFiles[i].getName(), "." + SourceType.Java.toString().toLowerCase());

            // check if the name would be a valid java name
            if (!JavaNames.isIdentifier(javaFileName))
                continue;

            // files with a $ in them signify inner classes (which we want to
            // ignore)
            if (javaFileName.indexOf('$') == -1)
                interestingSet.add(javaFileName);
        }
        
        for (int i = 0; i < frameSrcFiles.length; i++) {
            String frameFileName = JavaNames.stripSuffix(frameSrcFiles[i].getName(), "." + SourceType.Stride.toString().toLowerCase());

            // check if the name would be a valid java name
            if (!JavaNames.isIdentifier(frameFileName))
                continue;
            
            interestingSet.add(frameFileName);
        }


        // process all *.class files
        for (int i = 0; i < classFiles.length; i++) {
            // remove all __SHELL*.class files (temp files created by us)
            if (classFiles[i].getName().startsWith(Invoker.SHELLNAME)) {
                classFiles[i].delete();
                continue;
            }
            String classFileName = JavaNames.stripSuffix(classFiles[i].getName(), ".class");

            // check if the name would be a valid java name
            if (!JavaNames.isIdentifier(classFileName))
                continue;

            if (classFileName.indexOf('$') == -1) {
                // add only if there is no corresponding .java file
                if (!interestingSet.contains(classFileName)) {
                    try {
                        Class<?> c = loadClass(getQualifiedName(classFileName));

                        // fix for bug 152
                        // check that this class is a public class which means
                        // that private and package .class files generated
                        // because there are multiple classes defined in a
                        // single file will not add a target
                        if (c != null && Modifier.isPublic(c.getModifiers()))
                            interestingSet.add(classFileName);
                    }
                    catch (LinkageError e) {
                        Debug.message(e.toString());
                    }
                }
            }
        }

        return interestingSet;
    }

    /**
     * Load the elements of a package from a specified directory. If the package
     * file (bluej.pkg) is not found, an IOException is thrown.
     * 
     * <p>This does not cause targets to be loaded. Use refreshPackage() for that.
     */
    public synchronized void load()
        throws IOException
    {
        // read the package properties
        
        packageFile = getPkgFile();
        
        // try to load the package file for this package
        packageFile.load(lastSavedProps);
    }
    
    /**
     * Refresh the targets and dependency arrows in the package, based on whatever
     * is actually on disk.
     */
    public void refreshPackage()
    {
        // read in all the targets contained in this package
        // into this temporary map
        Map<String,Target> propTargets = new HashMap<String,Target>();

        int numTargets = 0, numDependencies = 0;

        try {
            numTargets = Integer.parseInt(lastSavedProps.getProperty("package.numTargets", "0"));
            numDependencies = Integer.parseInt(lastSavedProps.getProperty("package.numDependencies", "0"));
        }
        catch (Exception e) {
            synchronized (this)
            {
                Debug.reportError("Error loading from package file " + packageFile + ": " + e, e);
            }
            return;
        }

        for (int i = 0; i < numTargets; i++) {
            Target target;
            String type = lastSavedProps.getProperty("target" + (i + 1) + ".type");
            String identifierName = lastSavedProps.getProperty("target" + (i + 1) + ".name");

            if ("PackageTarget".equals(type))
            {
                target = new PackageTarget(this, identifierName);
            }
            else if ("CSSTarget".equals(type))
            {
                target = new CSSTarget(this, new File(getPath(), identifierName));
            }
            else
            {
                target = new ClassTarget(this, identifierName);
            }

            target.load(lastSavedProps, "target" + (i + 1));
            propTargets.put(identifierName, target);
        }

        addImmovableTargets();
        
        // make our Package targets reflect what is actually on disk
        // note that we consider this on-disk version the master
        // version so if we have a class target called Foo but we
        // discover a directory call Foo, a PackageTarget will be
        // inserted to replace the ClassTarget
        File subDirs[] = getPath().listFiles(new SubPackageFilter());
        
        for (int i = 0; i < subDirs.length; i++) {
            // first check if the directory name would be a valid package name
            if (!JavaNames.isIdentifier(subDirs[i].getName()))
                continue;

            Target target = propTargets.get(subDirs[i].getName());

            if (target == null || !(target instanceof PackageTarget)) {
                target = new PackageTarget(this, subDirs[i].getName());
                synchronized (this)
                {
                    targetsToPlace.add(target);
                }
            }

            addTarget(target);
        }
        
        // If BlueJ, look for CSS targets:
        if (!Config.isGreenfoot())
        {
            File cssFiles[] = getPath().listFiles(p -> p.getName().endsWith(".css"));
            for (File cssFile : cssFiles)
            {
                Target target = propTargets.get(cssFile.getName());
                if (target == null || !(target instanceof CSSTarget))
                {
                    target = new CSSTarget(this, cssFile);
                    synchronized (this)
                    {
                        targetsToPlace.add(target);
                    }
                }
                addTarget(target);
            }
            
        }        

        // now look for Java source files that may have been
        // added to the directory
        Set<String> interestingSet = findTargets(getPath());

        // also we migrate targets from propTargets across
        // to our real list of targets in this loop.
        Iterator<String> it = interestingSet.iterator();
        while (it.hasNext()) {
            String targetName = it.next();

            Target target = propTargets.get(targetName);
            if (target == null || !(target instanceof ClassTarget)) {
                target = new ClassTarget(this, targetName);
                synchronized (this)
                {
                    targetsToPlace.add(target);
                }
            }
            addTarget(target);
        }

        if (!recorded)
        {
            DataCollector.packageOpened(this);
            recorded = true;
        }


        List<Target> targetsCopy;
        synchronized (this)
        {
            targetsCopy = targets.toList();
        }
        
        // Start with all classes in the normal (compiled) state.
        for (Target target : targetsCopy) {
            if (target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget) target;
                ct.setState(State.COMPILED);
            }
        }

        // Fix up dependency information
        for (int i = 0; i < numDependencies; i++) {
            String type = lastSavedProps.getProperty("dependency" + (i + 1) + ".type");

            if ("UsesDependency".equals(type)) {
                try
                {
                    UsesDependency newDep = new UsesDependency(this, lastSavedProps, "dependency" + (i + 1));
                    addDependency(newDep, false);
                }
                catch (Dependency.DependencyNotFoundException e)
                {
                    Debug.reportError(e);
                }
            }
        }
        recalcArrows();

        // Update class states. We do this before updating roles (or anything else
        // which analyses the source) because the analysis does symbol resolution, and
        // that depends on having the correct compiled state.
        LinkedList<ClassTarget> invalidated = new LinkedList<ClassTarget>();
        for (Target target : targetsCopy) {

            if (target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget) target;
                if (ct.isCompiled() && !ct.upToDate()) {
                    ct.setState(State.NEEDS_COMPILE);
                    invalidated.add(ct);
                }
            }
        }
        
        while (! invalidated.isEmpty()) {
            ClassTarget ct = invalidated.removeFirst();
            for (Dependency dependent : ct.dependentsAsList()) {
                DependentTarget dt = dependent.getFrom();
                if (dt instanceof ClassTarget) {
                    ClassTarget dep = (ClassTarget) dt;
                    if (dep.isCompiled() && dep.hasSourceCode()) {
                        dep.setState(State.NEEDS_COMPILE);
                        invalidated.add(dep);
                    }
                }
            }
        }
        
        // Update class roles
        for (Target target : targetsCopy) {

            if (target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget) target;
                if (ct.isCompiled()) {
                    Class<?> cl = loadClass(ct.getQualifiedName());
                    ct.determineRole(cl);
                    ct.analyseDependencies(cl);
                    ct.analyseTypeParams(cl);
                    if (cl == null) {
                        ct.setState(State.NEEDS_COMPILE);
                    }
                }
                else {
                    ct.analyseSource();
                    try {
                        if ( !ct.getSourceType().equals(SourceType.Stride))
                            ct.enforcePackage(getQualifiedName());
                    }
                    catch (IOException ioe) {
                        Debug.message("Error enforcing class package: " + ioe.getLocalizedMessage());
                    }
                }
            }
        }

        // our associations are based on name so we mustn't deal with
        // them until all classes/packages have been loaded
        for (int i = 0; i < numTargets; i++) {
            String assoc = lastSavedProps.getProperty("target" + (i + 1) + ".association");
            String identifierName = lastSavedProps.getProperty("target" + (i + 1) + ".name");

            if (assoc != null) {
                Target t1 = getTarget(identifierName), t2 = getTarget(assoc);

                if (t1 != null && t2 != null && t1 instanceof DependentTarget) {
                    DependentTarget dt = (DependentTarget) t1;
                    dt.setAssociation((DependentTarget)t2);
                }
            }
        }
    }
    
    /**
     * Returns the file containing information about the package.
     * For BlueJ this is package.bluej (or for older versions bluej.pkg) 
     * and for Greenfoot it is greenfoot.project.
     */
    private PackageFile getPkgFile()
    {
        File dir = getPath();
        return PackageFileFactory.getPackageFile(dir);
    }

    /**
     * Position a target which has been added, based on the layout file
     * (if an entry exists) or find a suitable position otherwise.
     * 
     * @param t  the target to position
     */
    public void positionNewTarget(Target t)
    {
        String targetName = t.getIdentifierName();

        try {
            int numTargets = Integer.parseInt(lastSavedProps.getProperty("package.numTargets", "0"));
            for (int i = 0; i < numTargets; i++) {
                String identifierName = lastSavedProps.getProperty("target" + (i + 1) + ".name");
                if (identifierName.equals(targetName)) {
                    t.load(lastSavedProps, "target" + (i + 1));
                    return;
                }
            }
        }
        catch (NumberFormatException e) {}

        // If we get here, then we didn't find a location for the target
        getEditor().findSpaceForVertex(t);
    }
    
    /**
     * Add our immovable targets (the readme file, and possibly a link to the
     * parent package)
     */ 
    private void addImmovableTargets()
    {
        Target t = new ReadmeTarget(this);
        //Take special care of ReadmeTarget
        //see ReadmeTarget.isSaveable for explanation
        t.load(lastSavedProps, "readme");
        t.setPos(FIXED_TARGET_X, FIXED_TARGET_Y);
        addTarget(t);
        if (!isUnnamedPackage()) {
            final Target parent = new ParentPackageTarget(this);
            PackageEditor ed = getEditor();
            if (ed != null)
                ed.findSpaceForVertex(parent);
            addTarget(parent);
        }

    }

    /**
     * Reload a package.
     * 
     * This means we check the existing directory contents and compare it
     * against the targets we have in the package. Any new directories or java
     * source is added to the package. This function will not remove targets
     * that have had their corresponding on disk counterparts removed.
     * 
     * Any new source files will have their package lines updated to match the
     * package we are in.
     */
    public void reload()
    {
        File subDirs[] = getPath().listFiles(new SubPackageFilter());

        for (int i = 0; i < subDirs.length; i++) {
            // first check if the directory name would be a valid package name
            if (!JavaNames.isIdentifier(subDirs[i].getName()))
                continue;

            Target target;
            synchronized (this)
            {
                target = targets.get(subDirs[i].getName());
            }

            if (target == null) {
                Target newtarget = addPackage(subDirs[i].getName());
                getEditor().findSpaceForVertex(newtarget);
            }
        }

        Set<String> interestingSet = findTargets(getPath());

        for (Iterator<String> it = interestingSet.iterator(); it.hasNext();) {
            String targetName = it.next();

            Target target;
            synchronized (this)
            {
                target = targets.get(targetName);
            }

            if (target == null) {
                Target newtarget = addClass(targetName);
                if (getEditor() != null)
                {
                    getEditor().findSpaceForVertex(newtarget);
                }
            }
        }

        List<Target> targetsCopy;
        synchronized (this)
        {
            targetsCopy = targets.toList();
        }

        for (Target target : targetsCopy)
        {
            if (target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget) target;
                ct.analyseSource();
            }
        }

        //Update class roles, and their state
        for (Target target : targetsCopy)
        {
            if (target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget) target;

                Class<?> cl = loadClass(ct.getQualifiedName());
                if (cl != null) {
                    ct.determineRole(cl);
                    if (ct.upToDate()) {
                        ct.setState(State.COMPILED);
                    }
                    else {
                        ct.setState(State.NEEDS_COMPILE);
                    }
                }
            }
        }

        PackageEditor ed = getEditor();
        if (ed != null)
            ed.graphChanged();
    }
    
    /**
     * ReRead the pkg file and update the position of the targets in the graph
     * @throws IOException
     *
     */
    public void reReadGraphLayout() throws IOException
    {
        // try to load the package file for this package
        SortedProperties props = new SortedProperties();
        synchronized (this)
        {
            packageFile.load(props);
        }

        int numTargets = 0;

        try {
            numTargets = Integer.parseInt(props.getProperty("package.numTargets", "0"));
        }
        catch (Exception e) {
            synchronized (this)
            {
                Debug.printCallStack("Error loading from bluej package file " + packageFile + ": " + e);
            }
            return;
        }
        
        for (int i = 0; i < numTargets; i++) {
            String identifierName = props.getProperty("target" + (i + 1) + ".name");
            int x = Integer.parseInt(props.getProperty("target" + (i + 1) + ".x"));
            int y = Integer.parseInt(props.getProperty("target" + (i + 1) + ".y"));
            int height = Integer.parseInt(props.getProperty("target" + (i + 1) + ".height"));
            int width = Integer.parseInt(props.getProperty("target" + (i + 1) + ".width"));
            Target target = getTarget(identifierName);
            if (target != null){
                target.setPos(x, y);
                target.setSize(width, height);
            }
        }
        repaint();
    }

    @OnThread(Tag.FXPlatform)
    public void repaint()
    {
        PackageEditor ed = getEditor();
        if (ed != null)
        {
            ed.requestLayout();
            JavaFXUtil.runAfterNextLayout(ed.getScene(), ed::repaint);
        }
    }

    /**
     * Save this package to disk. The package is saved to the standard package
     * file.
     */
    @OnThread(Tag.FXPlatform)
    public synchronized void save(Properties frameProperties)
    {
        /* create the directory if it doesn't exist */
        File dir = getPath();
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                Debug.reportError("Error creating directory " + dir);
                return;
            }
        }

        SortedProperties props = new SortedProperties();
        props.putAll(frameProperties);

        // save targets and dependencies in package
        props.put("package.numDependencies", String.valueOf(usesArrows.size()));

        int t_count = 0;

        Iterator<Target> t_enum = targets.iterator();
        while (t_enum.hasNext()) {
            Target t = t_enum.next();
            // should we save this target
            if (t.isSaveable()) {
                t.save(props, "target" + (t_count + 1));
                t_count++;
            }
        }
        props.put("package.numTargets", String.valueOf(t_count));

        //Take special care of ReadmeTarget
        //see ReadmeTarget.isSaveable for explanation
        Target t = getTarget(ReadmeTarget.README_ID);
        t.save(props, "readme");


        for (int i = 0; i < usesArrows.size(); i++)
        { // uses arrows
            Dependency d = usesArrows.get(i);
            d.save(props, "dependency" + (i + 1));
        }

        try {
            packageFile.save(props);
        }
        catch (IOException e) {
            Debug.reportError("Exception when saving package file : " + e);
            return;
        }
        lastSavedProps = props;
    }

    /**
     * Import a source file into this package as a new class target. Returns an
     * error code: NO_ERROR - everything is fine FILE_NOT_FOUND - file does not
     * exist ILLEGAL_FORMAT - the file name does not end in ".java" CLASS_EXISTS -
     * a class with this name already exists COPY_ERROR - could not copy
     */
    public int importFile(File aFile)
    {
        // check whether specified class exists and is a java file

        if (!aFile.exists())
            return FILE_NOT_FOUND;
        String fileName = aFile.getName();

        String className;
        if (fileName.endsWith("." + SourceType.Java.getExtension())) // it's a Java source file
            className = fileName.substring(0, fileName.length() - SourceType.Java.getExtension().length() - 1);
        else if (fileName.endsWith("." + SourceType.Stride.getExtension())) // it's a Stride source file
            className = fileName.substring(0, fileName.length() - SourceType.Stride.getExtension().length() - 1);
        else
            return ILLEGAL_FORMAT;

        // check whether name is already used
        if (getTarget(className) != null)
            return CLASS_EXISTS;

        // copy class source into package

        File destFile = new File(getPath(), fileName);
        try {
            FileUtility.copyFile(aFile, destFile);
        }
        catch (IOException ioe) {
            return COPY_ERROR;
        }

        ClassTarget t = addClass(className);

        // Check for the Vertex in the editor,
        // and create a new editor without focus if it was not available before
        if (null == getEditor())
        {
            PkgMgrFrame pmf = PkgMgrFrame.findFrame(this);
            if (pmf == null)
            {
                pmf = PkgMgrFrame.createFrame(this, null);
            }
        }
        getEditor().findSpaceForVertex(t);
        t.analyseSource();
        
        DataCollector.addClass(this, t);

        return NO_ERROR;
    }

    public ClassTarget addClass(String className)
    {
        // create class icon (ClassTarget) for new class
        ClassTarget target = new ClassTarget(this, className);
        addTarget(target);

        // make package line in class source match our package
        try {
            target.enforcePackage(getQualifiedName());
        }
        catch (IOException ioe) {
            Debug.message(ioe.getLocalizedMessage());
        }

        return target;
    }

    /**
     * Add a new package target to this package.
     * 
     * @param packageName The basename of the package to add
     */
    public PackageTarget addPackage(String packageName)
    {
        PackageTarget target = new PackageTarget(this, packageName);
        addTarget(target);

        return target;
    }

    @OnThread(Tag.Any)
    public Debugger getDebugger()
    {
        return getProject().getDebugger();
    }

    /**
     * Loads a class using the current project class loader.
     * Return null if the class cannot be loaded.
     */
    public Class<?> loadClass(String className)
    {
        return getProject().loadClass(className);
    }

    @OnThread(Tag.Any)
    public synchronized List<Target> getVertices()
    {
        List<Target> r = new ArrayList<>();
        for (Target t : targets)
            r.add(t);
        return r;
    }


    /**
     * Return a List of all ClassTargets that have the role of a unit test.
     */
    public synchronized List<ClassTarget> getTestTargets()
    {
        List<ClassTarget> l = new ArrayList<ClassTarget>();

        for (Iterator<Target> it = targets.iterator(); it.hasNext();) {
            Target target = it.next();

            if (target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget) target;

                if (ct.isUnitTest())
                    l.add(ct);
            }
        }

        return l;
    }
    
    /**
     * Find and compile all uncompiled classes, and get reports of compilation
     * status/results via the specified CompileObserver.
     * <p>
     * In general this should be called only when the debugger is
     * in the idle state (or at least not when executing user code). A new
     * project classloader will be created which can be used to load the
     * newly compiled classes, once they are ready.
     * 
     * @param compObserver  An observer to be notified of compilation progress.
     *                  The callback methods will be called on the Swing EDT.
     *                  The 'endCompile' method will always be called; other
     *                  methods may not be called if the compilation is aborted
     *                  (sources cannot be saved etc).
     */
    public void compile(FXCompileObserver compObserver, CompileReason reason, CompileType type)
    {
        Set<ClassTarget> toCompile = new HashSet<ClassTarget>();

        try
        {
            List<ClassTarget> classTargets;
            // build the list of targets that need to be compiled
            synchronized (this)
            {
                classTargets = getClassTargets();
            }
            for (ClassTarget ct : classTargets)
            {
                if (!ct.isCompiled() && !ct.isQueued())
                {
                    ct.ensureSaved();
                    toCompile.add(ct);
                    ct.setQueued(true);
                }
            }

            if (!toCompile.isEmpty())
            {
                if (type.keepClasses())
                {
                    project.removeClassLoader();
                    project.newRemoteClassLoaderLeavingBreakpoints();
                }
                ArrayList<FXCompileObserver> observers = new ArrayList<>(compileObservers);
                if (compObserver != null)
                {
                    observers.add(compObserver);
                }
                doCompile(toCompile, new PackageCompileObserver(observers), reason, type);
            }
            else {
                if (compObserver != null) {
                    compObserver.endCompile(new CompileInputFile[0], true, type, -1);
                }
            }
        }
        catch (IOException ioe) {
            // Abort compile
            Debug.log("Error saving class before compile: " + ioe.getLocalizedMessage());
            for (ClassTarget ct : toCompile) {
                ct.setQueued(false);
            }
            if (compObserver != null) {
                compObserver.endCompile(new CompileInputFile[0], false, type, -1);
            }
        }
    }
    
    /**
     * Find and compile all uncompiled classes.
     * <p>
     * In general this should be called only when the debugger is
     * in the idle state (or at least not when executing user code). A new
     * project classloader will be created which can be used to load the
     * newly compiled classes, once they are ready.
     */
    public void compile(CompileReason reason, CompileType type)
    {
        if (! currentlyCompiling) { 
            currentlyCompiling = true;
            compile(new FXCompileObserver() {
                // The return of this method will be ignored,
                // as PackageCompileObserver which chains to us, ignores it
                @Override
                @OnThread(Tag.FXPlatform)
                public boolean compilerMessage(Diagnostic diagnostic, CompileType type) { return false; }
                
                @Override
                @OnThread(Tag.FXPlatform)
                public void startCompile(CompileInputFile[] sources, CompileReason reason, CompileType type, int compilationSequence) { }
                
                @Override
                @OnThread(Tag.FXPlatform)
                public void endCompile(CompileInputFile[] sources, boolean succesful, CompileType type2, int compilationSequence)
                {
                    // This will be called on the Swing thread.
                    currentlyCompiling = false;
                    if (queuedCompile) {
                        queuedCompile = false;
                        compile(queuedReason, type);
                        queuedReason = null;
                    }
                }
            }, reason, type);
        }
        else {
            queuedCompile = true;
            queuedReason = reason;
        }
    }
    
    /**
     * Compile a single class.
     */
    public void compile(ClassTarget ct, CompileReason reason, CompileType type)
    {
        compile(ct, false, null, reason, type);
    }
    
    /**
     * Compile a single class.
     */
    public void compile(ClassTarget ct, boolean forceQuiet, FXCompileObserver compObserver, CompileReason reason, CompileType type)
    {
        if (!checkCompile()) {
            return;
        }

        ClassTarget assocTarget = (ClassTarget) ct.getAssociation();
        if (assocTarget != null && ! assocTarget.hasSourceCode()) {
            assocTarget = null;
        }

        // we don't want to try and compile if it is a class target without src
        // it may be better to avoid calling this method on such targets
        if (!ct.hasSourceCode()) {
            ct = null;
        }

        if (ct != null || assocTarget != null) {
            if (type.keepClasses())
            {
                project.removeClassLoader();
                project.newRemoteClassLoaderLeavingBreakpoints();
            }

            if (ct != null) {
                ArrayList<FXCompileObserver> chainedObservers = new ArrayList<>(compileObservers);
                if (compObserver != null)
                {
                    chainedObservers.add(compObserver);
                }
                FXCompileObserver observer;
                if (forceQuiet) {
                    observer = new QuietPackageCompileObserver(chainedObservers);
                } else {
                    observer = new PackageCompileObserver(chainedObservers);
                }
                searchCompile(ct, observer, reason, type);
            }

            if (assocTarget != null) {
                searchCompile(assocTarget, new QuietPackageCompileObserver(Collections.emptyList()), reason, type);
            }
        }
    }

    /**
     * Compile a single class quietly.
     */
    public void compileQuiet(ClassTarget ct, CompileReason reason, CompileType type)
    {
        if (!isDebuggerIdle()) {
            return;
        }

        searchCompile(ct, new QuietPackageCompileObserver(Collections.emptyList()), reason, type);
    }

    /**
     * Force compile of all classes. Called by user function "rebuild".
     */
    public void rebuild()
    {
        if (!checkCompile()) {
            return;
        }

        // Saving a class target can change its name; we need to copy the set of targets
        // first, and iterate through the copied list, to avoid "concurrent" modification
        // problems.
        List<ClassTarget> compileTargets = new ArrayList<ClassTarget>();
        synchronized (this)
        {
            for (Iterator<Target> it = targets.iterator(); it.hasNext(); )
            {
                Target target = it.next();
                if (target instanceof ClassTarget)
                {
                    compileTargets.add((ClassTarget)target);
                }
            }
        }

        try {
            for (Iterator<ClassTarget> i = compileTargets.iterator(); i.hasNext(); ) {
                ClassTarget ct = i.next();
                // we don't want to try and compile if it is a class target without src
                if (ct.hasSourceCode()) {
                    ct.ensureSaved();
                    ct.markModified();
                    ct.setQueued(true);
                }
                else {
                    i.remove();
                }
            }
            if (!compileTargets.isEmpty())
            {
                project.removeClassLoader();
                project.newRemoteClassLoader();

                doCompile(compileTargets, new PackageCompileObserver(compileObservers), CompileReason.REBUILD, CompileType.EXPLICIT_USER_COMPILE);
            }
        }
        catch (IOException ioe) {
            showMessageWithText("file-save-error-before-compile", ioe.getLocalizedMessage());
        }
    }

    /**
     * Have all editors in this package save the file the are showing.
     * Called when doing a cvs operation
     */
    public void saveFilesInEditors() throws IOException
    {
        // Because we call editor.save() on targets, which can result in
        // a renamed class target, we need to iterate through a copy of
        // the collection - hence the new ArrayList call here:
        List<ClassTarget> classTargets;
        synchronized (this)
        {
            classTargets = new ArrayList<>(getClassTargets());
        }
        for (ClassTarget ct : classTargets) {
            Editor ed = ct.getEditor();
            // Editor can be null eg. class file and no src file
            if(ed != null) {
                ed.save();
            }
        }
    }
    
    /**
     * Compile a class together with its dependencies, as necessary.
     */
    private void searchCompile(ClassTarget t, FXCompileObserver observer, CompileReason reason, CompileType type)
    {
        if (t.isQueued()) {
            return;
        }

        Set<ClassTarget> toCompile = new HashSet<ClassTarget>();
        
        try {
            List<ClassTarget> queue = new LinkedList<ClassTarget>();
            toCompile.add(t);
            t.ensureSaved();
            queue.add(t);
            t.setQueued(true);

            while (! queue.isEmpty()) {
                ClassTarget head = queue.remove(0);

                for (Dependency d : head.dependencies()) {
                    if (!(d.getTo() instanceof ClassTarget)) {
                        continue;
                    }

                    ClassTarget to = (ClassTarget) d.getTo();
                    if (!to.isCompiled() && ! to.isQueued() && toCompile.add(to)) {
                        to.ensureSaved();
                        to.setQueued(true);
                        queue.add(to);
                    }
                }
            }

            doCompile(toCompile, observer, reason, type);
        }
        catch (IOException ioe) {
            // Failed to save; abort the compile
            Debug.log("Failed to save source before compile; " + ioe.getLocalizedMessage());
            for (ClassTarget ct : toCompile) {
                ct.setQueued(false);
            }
        }
    }

    /**
     * Compile every Target in 'targetList'. Every compilation goes through this method.
     * All targets in the list should have been saved beforehand.
     */
    private void doCompile(Collection<ClassTarget> targetList, FXCompileObserver edtObserver, CompileReason reason, CompileType type)
    {
        CompileObserver observer = new EventqueueCompileObserverAdapter(new DataCollectionCompileObserverWrapper(project, edtObserver));
        if (targetList.isEmpty()) {
            return;
        }

        List<CompileInputFile> srcFiles = Utility.mapList(targetList, ClassTarget::getCompileInputFile);
        if (srcFiles.size() > 0 && srcFiles.stream().allMatch(CompileInputFile::isValid))
        {
            JobQueue.getJobQueue().addJob(srcFiles.toArray(new CompileInputFile[0]), observer, project.getClassLoader(), project.getProjectDir(),
                ! PrefMgr.getFlag(PrefMgr.SHOW_UNCHECKED), project.getProjectCharset(), reason, type);
        }
    }

    /**
     * Returns true if the debugger is not busy. This is true if it is either
     * IDLE, or has not been completely constructed (NOTREADY).
     */
    public boolean isDebuggerIdle()
    {
        Debugger debugger = getDebugger();
        if (debugger == null) {
            // This method can be called during Project construction, when the debugger
            // has not been created yet. Return true in this case, since the debugger
            // is considered idle while the remote VM is starting.
            return true;
        }
        int status = debugger.getStatus();
        return (status == Debugger.IDLE) || (status == Debugger.NOTREADY);
    }

    /**
     * Check whether it's okay to compile and display a message about it.
     */
    private boolean checkCompile()
    {
        if (isDebuggerIdle())
            return true;

        // The debugger is NOT idle, show a message about it.
        showMessage("compile-while-executing");
        return false;
    }
    
    /**
     * Compile the package, but only when the debugger is in an idle state.
     * @param specificTarget The single classtarget to compile; if null then will compile whole package.
     */
    public void compileOnceIdle(ClassTarget specificTarget, CompileReason reason, CompileType type)
    {
        if (! waitingForIdleToCompile) {
            if (isDebuggerIdle())
            {
                if (specificTarget == null)
                    compile(reason, type);
                else
                    compile(specificTarget, reason, type);
            }
            else {
                waitingForIdleToCompile = true;
                // No lambda as we need to also remove:
                DebuggerListener dlistener = new DebuggerListener() {
                    @Override
                    @OnThread(Tag.Any)
                    public void processDebuggerEvent(DebuggerEvent e, boolean skipUpdate)
                    {
                        if (e.getNewState() == Debugger.IDLE)
                        {
                            getDebugger().removeDebuggerListener(this);
                            // We call compileOnceIdle, not compile, because we might not still be idle
                            // by the time we run on the GUI thread, so we may have to do the whole
                            // thing again:
                            Platform.runLater(() -> {
                                if (waitingForIdleToCompile) {
                                    waitingForIdleToCompile = false;
                                    compileOnceIdle(specificTarget, reason, type);
                                }
                            });
                        }
                    }
                };
                
                getDebugger().addDebuggerListener(dlistener);
                
                // Potential race: the debugger may have gone idle just before we added the listener.
                // Check for that now:
                if (isDebuggerIdle()) {
                    waitingForIdleToCompile = false;
                    compile(reason, type);
                    getDebugger().removeDebuggerListener(dlistener);
                }
            }
        }
    }

    /**
     * Generate documentation for this package.
     * 
     * @return "" if everything was alright, an error message otherwise.
     */
    public String generateDocumentation()
    {
        // This implementation currently just delegates the generation to
        // the project this package is part of.
        return project.generateDocumentation();
    }

    /**
     * Generate documentation for class in this ClassTarget.
     * 
     * @param ct
     *            the class to generate docs for
     */
    public void generateDocumentation(ClassTarget ct)
    {
        // editor file is already saved: no need to save it here
        String filename = ct.getSourceFile().getPath();
        project.generateDocumentation(filename);
    }

    /**
     * Re-initialize breakpoints, necessary after a new class loader is
     * installed.
     */
    public void reInitBreakpoints()
    {
        List<ClassTarget> classTargets;
        synchronized (this)
        {
            classTargets = getClassTargets();
        }
        for (ClassTarget target : classTargets)
        {
            target.reInitBreakpoints();
        }
    }

    /**
     * Remove all step marks in all classes.
     */
    public void removeStepMarks()
    {
        List<ClassTarget> classTargets;
        synchronized (this)
        {
            classTargets = new ArrayList<>(getClassTargets());
        }
        for (ClassTarget target : classTargets)
        {
            target.removeStepMark();
        }
        if (getUI() != null)
        {
            getUI().highlightObject(null);
        }
    }

    public synchronized void addTarget(Target t)
    {
        if (t.getPackage() != this)
            throw new IllegalArgumentException();

        targets.add(t.getIdentifierName(), t);
        fireChangedEvent();
    }

    public synchronized void removeTarget(Target t)
    {
        targets.remove(t.getIdentifierName());
        t.setRemoved();
        fireChangedEvent();
    }

    /**
     * Changes the Target identifier. Targets are stored in a hashtable with
     * their name as the key. If class name changes we need to remove the target
     * and add again with the new key.
     */
    public synchronized void updateTargetIdentifier(Target t, String oldIdentifier, String newIdentifier)
    {
        if (t == null || newIdentifier == null) {
            Debug.reportError("cannot properly update target name...");
            return;
        }
        targets.remove(oldIdentifier);
        targets.add(newIdentifier, t);
    }

    /**
     * remove the arrow representing the given dependency
     * 
     * @param d  the dependency to remove
     */
    @OnThread(Tag.FXPlatform)
    public void removeArrow(Dependency d)
    {
        if (!(d instanceof UsesDependency))
        {
            userRemoveDependency(d);
        }
        removeDependency(d, true);
        getEditor().graphChanged();
    }


    /**
     * A user initiated addition of an "implements" clause from a class to
     * an interface
     *
     * @pre d.getFrom() and d.getTo() are both instances of ClassTarget
     */
    public void userAddImplementsClassDependency(ClassTarget from, ClassTarget to)
    {
        ClassInfo info = from.getSourceInfo().getInfo(from.getJavaSourceFile(), this);
        if (info != null) {
            from.getEditor().addImplements(to.getBaseName(), info);
            from.analyseSource();
        }
    }

    /**
     * A user initiated addition of an "extends" clause from an interface to
     * an interface
     *
     * @pre d.getFrom() and d.getTo() are both instances of ClassTarget
     */
    public void userAddExtendsInterfaceDependency(ClassTarget from, ClassTarget to)
    {
        ClassInfo info = from.getSourceInfo().getInfo(from.getJavaSourceFile(), this);
        from.getEditor().addExtendsInterface(to.getBaseName(), info);
        from.analyseSource();
    }

    /**
     * A user initiated addition of an "extends" clause from a class to
     * a class
     *
     * @pre d.getFrom() and d.getTo() are both instances of ClassTarget
     */
    public void userAddExtendsClassDependency(ClassTarget from, ClassTarget to)
    {
        from.getEditor().setExtendsClass(to.getBaseName(), from.getSourceInfo().getInfo(from.getJavaSourceFile(), this));
        from.analyseSource();
    }

    /**
     * A user initiated removal of a dependency
     *
     * @param d  an instance of an Implements or Extends dependency
     */
    public void userRemoveDependency(Dependency d)
    {
        // if they are not both classtargets then I don't want to know about it
        if (!(d.getFrom() instanceof ClassTarget) || !(d.getTo() instanceof ClassTarget))
            return;

        ClassTarget from = (ClassTarget) d.getFrom();
        ClassTarget to = (ClassTarget) d.getTo();
        ClassInfo info = from.getSourceInfo().getInfo(from.getJavaSourceFile(), this);
        if (d instanceof ImplementsDependency) {
            from.getEditor().removeExtendsOrImplementsInterface(to.getBaseName(), info);
        }
        else if (d instanceof ExtendsDependency) {
            from.getEditor().removeExtendsClass(info);
        }
    }
    
    /**
     * Lay out the arrows between targets.
     */
    @OnThread(Tag.FXPlatform)
    private void recalcArrows()
    {
        for (Target t : getVertices())
        {
            if (t instanceof DependentTarget) {
                DependentTarget dt = (DependentTarget) t;

                dt.recalcInUses();
                dt.recalcOutUses();
            }
        }
    }

    /**
     * Return the target with name "identifierName".
     * 
     * @param identifierName
     *            the unique name of a target.
     * @return the target with name "tname" if existent, null otherwise.
     */
    @OnThread(Tag.Any)
    public synchronized Target getTarget(String identifierName)
    {
        if (identifierName == null)
            return null;
        Target t = targets.get(identifierName);
        return t;
    }

    /**
     * Return the dependent target with name "identifierName".
     * 
     * @param identifierName
     *            the unique name of a target.
     * @return the target with name "tname" if existent and if it is a
     *         DependentTarget, null otherwise.
     */
    public synchronized DependentTarget getDependentTarget(String identifierName)
    {
        if (identifierName == null)
            return null;
        Target t = targets.get(identifierName);

        if (t instanceof DependentTarget)
            return (DependentTarget) t;

        return null;
    }

    
    /**
     * Returns an ArrayList of ClassTargets holding all targets of this package.
     * @return a not null but possibly empty array list of ClassTargets for this package.
     */
    @OnThread(Tag.Any)
    public synchronized final ArrayList<ClassTarget> getClassTargets()
    {
        ArrayList<ClassTarget> risul = new ArrayList<ClassTarget>();

        for (Iterator<Target> it = targets.iterator(); it.hasNext();) {
            Target target = it.next();

            if (target instanceof ClassTarget) {
                risul.add((ClassTarget) target);
            }
        }
        return risul;
    }

    /**
     * Return a List of Strings with names of all classes in this package.
     */
    public synchronized List<String> getAllClassnames()
    {
        return Utility.mapList(getClassTargets(), ClassTarget::getBaseName);
    }

    /**
     * Return a List of Strings with names of all classes in this package that
     * has accompanying source.
     */
    public synchronized List<String> getAllClassnamesWithSource()
    {
        List<String> names = new ArrayList<String>();

        for (Iterator<Target> it = targets.iterator(); it.hasNext();) {
            Target t = it.next();

            if (t instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget) t;
                if (ct.hasSourceCode())
                    names.add(ct.getBaseName());
            }
        }
        return names;
    }

    /**
     * Test whether a file instance denotes a BlueJ or Greenfoot package directory depending on which mode we are in.
     * 
     * @param f
     *            the file instance that is tested for denoting a BlueJ package.
     * @return true if f denotes a directory and a BlueJ package.
     */
    @OnThread(Tag.Any)
    public static boolean isPackage(File f)
    {
        if(Config.isGreenfoot())
            return GreenfootProjectFile.exists(f);
        else 
            return BlueJPackageFile.exists(f);
    }
    
    /**
     * Test whether this name is the name of a package file.
     */
    @OnThread(Tag.Any)
    public static boolean isPackageFileName(String name)
    {
        if(Config.isGreenfoot())
            return GreenfootProjectFile.isProjectFileName(name);
        else 
            return BlueJPackageFile.isPackageFileName(name);
    }

    /**
     * Use the dialog manager to display an error message. The PkgMgrFrame is
     * used to find a parent window so we can correctly offset the dialog.
     */
    public void showError(String msgId)
    {
        PkgMgrFrame.showError(this, msgId);
    }

    /**
     * Use the dialog manager to display a message. The PkgMgrFrame is used to
     * find a parent window so we can correctly offset the dialog.
     */
    public void showMessage(String msgId)
    {
        PkgMgrFrame.showMessage(this, msgId);
    }

    /**
     * Use the dialog manager to display a message with text. The PkgMgrFrame is
     * used to find a parent window so we can correctly offset the dialog.
     */
    public void showMessageWithText(String msgId, String text)
    {
        PkgMgrFrame.showMessageWithText(this, msgId, text);
    }

    /**
     * Don't remember the last shown source anymore.
     */
    public void forgetLastSource()
    {
        lastSourceName = "";
    }

    /**
     * A thread has hit a breakpoint, done a step or selected a frame in the debugger. Display the source
     * code with the relevant line highlighted.
     *
     * Note: source name is the unqualified name of the file (no path attached)
     * 
     * @return true if the debugger display is already taken care of, or
     * false if you still want to show the ExecControls window afterwards.
     */
    @OnThread(Tag.FXPlatform)
    private boolean showSource(DebuggerThread thread, String sourcename, int lineNo, ShowSourceReason reason, String msg, DebuggerObject currentObject)
    {
        boolean bringToFront = !sourcename.equals(lastSourceName);
        lastSourceName = sourcename;

        // showEditorMessage:
        Editor targetEditor = editorForTarget(new File(getPath(), sourcename).getAbsolutePath(), bringToFront);
        if (targetEditor != null)
        {
            if (getUI() != null)
            {
                getUI().highlightObject(currentObject);
            }
            
            return targetEditor.setStepMark(lineNo, msg, reason.isSuspension(), thread);
        }
        else if (reason == ShowSourceReason.BREAKPOINT_HIT) {
            showMessageWithText("break-no-source", sourcename);
        }
        return false;
    }

    /**
     * Show the specified line of the specified source file. Open the editor if necessary.
     * @param sourcename  The source file to show
     * @param lineNo      The line number to show
     * @return  true if the editor was the most recent editor to have a message displayed
     */
    public void showSource(String sourcename, int lineNo)
    {
        String msg = " ";
        
        boolean bringToFront = !sourcename.equals(lastSourceName);
        lastSourceName = sourcename;

        showEditorMessage(new File(getPath(), sourcename).getPath(), lineNo, msg, false, bringToFront);
    }
    
    /**
     * An interface for message "calculators" which can produce enhanced diagnostic messages when
     * given a reference to the editor in which a compilation error occurred.
     */
    public static interface MessageCalculator
    {
        /**
         * Produce a diagnostic message for the given editor.
         * This should produce something half-way helpful if null is passed.
         * 
         * @param e  The editor where the original error occurred (null if it cannot be determined).
         */
        public String calculateMessage(Editor e);
    }
    
    /**
     * Attempt to display (in the corresponding editor) an error message associated with a
     * specific line in a class. This is done by opening the class's source, highlighting the line
     * and showing the message in the editor's information area. If the filename specified does
     * not exist, the message is not shown.
     * 
     * @return true if the message was displayed; false if there was no suitable class.
     */
    private boolean showEditorMessage(String filename, int lineNo, final String message,
            boolean beep, boolean bringToFront)
    {
        Editor targetEditor = editorForTarget(filename, bringToFront);
        if (targetEditor == null)
        {
            Debug.message("Error or exception for source not in project: " + filename + ", line " +
                    lineNo + ": " + message);
            return false;
        }

        targetEditor.displayMessage(message, lineNo, 0);
        return true;
    }

    /**
     * Find or open the Editor for a given source file. The editor is opened and displayed if it is not
     * currently visible. If the source file is in another package, a package editor frame will be
     * opened for that package.
     * 
     * @param filename   The source file name, which should be a full absolute path
     * @param bringToFront  True if the editor should be brought to the front of the window z-order
     * @return  The editor for the given source file, or null if there is no editor.
     */
    private Editor editorForTarget(String filename, boolean bringToFront)
    {
        Target t = getTargetForSource(filename);
        if (! (t instanceof ClassTarget)) {
            return null;
        }

        ClassTarget ct = (ClassTarget) t;
        
        Editor targetEditor = ct.getEditor();
        if (targetEditor != null) {
            if (! targetEditor.isOpen() || bringToFront) {
                ct.open();;
            }
        }
        
        return targetEditor;
    }
    
    /**
     * Find the target for a given source file. If the target is in another package, a package editor
     * frame is opened for the package (if not open already).
     * 
     * @param filename  The source file name
     * @return  The corresponding target, or null if the target doesn't exist.
     */
    private Target getTargetForSource(String filename)
    {
        String fullName = getProject().convertPathToPackageName(filename);
        if (fullName == null) {
            return null;
        }
        
        String packageName = JavaNames.getPrefix(fullName);
        String className = JavaNames.getBase(fullName);
        
        Target t = null;

        // check if the error is from a file belonging to another package
        if (! packageName.equals(getQualifiedName())) {

            Package pkg = getProject().getPackage(packageName);
            
            if (pkg != null) {
                PkgMgrFrame pmf = PkgMgrFrame.findFrame(pkg);

                if (pmf == null) {
                    pmf = PkgMgrFrame.createFrame(pkg, null);
                }

                pmf.setVisible(true);

                t = pkg.getTarget(className);
            }
        }
        else {
            t = getTarget(className);
        }
        
        return t;
    }
    
    /**
     * An enumeration for indicating whether a compilation diagnostic was actually displayed to the
     * user.
     */
    private static enum ErrorShown
    {
        // Reminds me of http://thedailywtf.com/Articles/What_Is_Truth_0x3f_.aspx :-)
        ERROR_SHOWN, ERROR_NOT_SHOWN, EDITOR_NOT_FOUND
    }
    
    /**
     * Display a compiler diagnostic (error or warning) in the appropriate editor window.
     * 
     * @param diagnostic   The diagnostic to display
     * @param messageCalc  The message "calculator", which returns a modified version of the message;
     *                     may be null, in which case the original message is shown unmodified.
     * @param errorIndex The index of the error (first is 0, second is 1, etc)
     * @param compileType The type of the compilation which caused the error.
     */
    private ErrorShown showEditorDiagnostic(Diagnostic diagnostic, MessageCalculator messageCalc, int errorIndex, CompileType compileType)
    {
        String fileName = diagnostic.getFileName();
        if (fileName == null) {
            return ErrorShown.EDITOR_NOT_FOUND;
        }
        
        Target target = getTargetForSource(fileName);
        if (! (target instanceof ClassTarget)) {
            return ErrorShown.EDITOR_NOT_FOUND;
        }
        
        ClassTarget t = (ClassTarget) target;

        Editor targetEditor = t.getEditor();
        if (targetEditor != null) {
            if (messageCalc != null) {
                diagnostic.setMessage(messageCalc.calculateMessage(targetEditor));
            }
            
            if (project.isClosing()) {
                return ErrorShown.ERROR_NOT_SHOWN;
            }
            boolean shown = t.showDiagnostic(diagnostic, errorIndex, compileType);
            return shown ? ErrorShown.ERROR_SHOWN : ErrorShown.ERROR_NOT_SHOWN;
        }
        else {
            Debug.message(t.getDisplayName() + ", line" + diagnostic.getStartLine() +
                    ": " + diagnostic.getMessage());
            return ErrorShown.EDITOR_NOT_FOUND;
        }
    }

    /**
     * A breakpoint in this package was hit.
     */
    @OnThread(Tag.FXPlatform)
    public void hitBreakpoint(DebuggerThread thread, String classSourceName, int lineNumber, DebuggerObject currentObject)
    {
        String msg = null;
        if (PrefMgr.getFlag(PrefMgr.ACCESSIBILITY_SUPPORT)) {
            msg = Config.getString("debugger.accessibility.breakpoint");
            msg = msg.replace("$", thread.getName());
        }
        
        if (!showSource(thread, classSourceName, lineNumber, ShowSourceReason.BREAKPOINT_HIT, msg, currentObject))
        {
            getProject().getExecControls().show();
            getProject().getExecControls().selectThread(thread);
        }
    }

    /**
     * Execution stopped by someone pressing the "halt" button or we have just
     * done a "step".
     */
    @OnThread(Tag.FXPlatform)
    public void hitHalt(DebuggerThread thread, String classSourceName, int lineNumber, DebuggerObject currentObject, boolean breakpoint)
    {
        String msg = null;
        if (PrefMgr.getFlag(PrefMgr.ACCESSIBILITY_SUPPORT)) {
            msg = breakpoint ? Config.getString("debugger.accessibility.breakpoint") : Config.getString("debugger.accessibility.paused");
            msg = msg.replace("$", thread.getName());
        }
        
        ShowSourceReason reason = breakpoint ? ShowSourceReason.BREAKPOINT_HIT : ShowSourceReason.STEP_OR_HALT;
        if (!showSource(thread, classSourceName, lineNumber, reason, msg, currentObject))
        {
            getProject().getExecControls().show();
            getProject().getExecControls().selectThread(thread);
        }
    }

    /**
     * Display a source file from this package at the specified position.
     */
    @OnThread(Tag.FXPlatform)
    public void showSourcePosition(DebuggerThread thread, String sourceName, int lineNumber, DebuggerObject currentObject)
    {
        showSource(thread, sourceName, lineNumber, ShowSourceReason.FRAME_SELECTED, null, currentObject);
    }
    
    /**
     * Display an exception message. This is almost the same as "errorMessage"
     * except for different help texts.
     */
    public void exceptionMessage(ExceptionDescription exception)
    {
        String text = exception.getClassName();
        if (text == null) {
            reportException(exception.getText());
            return;
        }
        
        String message = text + ":\n" + exception.getText();
        List<SourceLocation> stack = exception.getStack();
        
        if ((stack == null) || (stack.size() == 0)) {
            // Stack empty or missing. This can happen when an exception is
            // thrown from the code pad for instance.
            return;
        }

        // using the stack, try to find the source code
        boolean done = false;
        Iterator<SourceLocation> iter = stack.iterator();
        boolean firstTime = true;

        while (!done && iter.hasNext()) {
            SourceLocation loc = iter.next();
            String locFileName = loc.getFileName();
            if (locFileName != null) {
                String filename = new File(getPath(), locFileName).getPath();
                int lineNo = loc.getLineNumber();
                done = showEditorMessage(filename, lineNo, message, true, true);
                if (firstTime && !done) {
                    message += " (in " + loc.getClassName() + ")";
                    firstTime = false;
                }
            }
        }
        if (!done) {
            SourceLocation loc = stack.get(0);
            showMessageWithText("error-in-file", loc.getClassName() + ":" + loc.getLineNumber() + "\n" + message);
        }
    }
    
    /**
     * Displays the given class at the given line number (due to an exception, usually clicked-on stack trace).
     * 
     *  Simpler than the other exceptionMessage method because it requires less details 
     */
    public void exceptionMessage(String className, int lineNumber)
    {
        showEditorMessage(className, lineNumber, "", false, true);
    }

    /**
     * Report an execption. Usually, we do this through "errorMessage", but if
     * we cannot make sense of the message format, and thus cannot figure out
     * class name and line number, we use this way.
     */
    public void reportException(String text)
    {
        showMessageWithText("exception-thrown", text);
    }

    /**
     * Use the resource name in order to return the path of the jar file
     * containing the given resource.
     * <p>
     * If it is not in a jar file it returns the original resource path
     * (URL).
     * 
     * @param c  The class to get the path to
     * @return A string indicating the path of the jar file (if applicable
     * and if not, it returns the path/URL to the resource)
     */
    protected static String getResourcePath(Class<?> c)
    { 
        URL srcUrl = c.getResource(c.getSimpleName()+".class");
        try {
            if (srcUrl != null) {
                if (srcUrl.getProtocol().equals("file")) {
                    File srcFile = new File(srcUrl.toURI());
                    return srcFile.toString();
                }  
                if (srcUrl.getProtocol().equals("jar")){
                    //it should be of this format
                    //jar:file:/path!/class 
                    int classIndex = srcUrl.toString().indexOf("!");
                    String subUrl = srcUrl.toString().substring(4, classIndex);
                    if (subUrl.startsWith("file:")) {
                        return new File(new URI(subUrl)).toString();
                    }
                    
                    if (classIndex!=-1){
                        return srcUrl.toString().substring(4, classIndex);
                    }
                }
            }
            else {
                return null;
            }
        }
        catch (URISyntaxException uriSE) {
            // theoretically we can't get URISyntaxException; the URL should
            // be valid.
        }
        return srcUrl.toString();
    }
    
    /**
     * Check whether a loaded class was actually loaded from the specified class file
     * @param c  The loaded class
     * @param f  The class file to check against (should be a compiled .class file)
     * @return  True if the class was loaded from the specified file; false otherwise
     */
    public static boolean checkClassMatchesFile(Class<?> c, File f)
    {
        try {
            URL srcUrl = c.getResource(c.getSimpleName()+".class");
            if (srcUrl == null) {
                // If we weren't able to load the class file at all, it may have been
                // deleted; this happens when a class is added to a project, then
                // removed, and then another class is added with the same name.
                return true;
            }
            if (srcUrl != null && srcUrl.getProtocol().equals("file")) {
                File srcFile = new File(srcUrl.toURI());
                if (! f.equals(srcFile)) {
                    return false;
                }
            }
            else {
                return false;
            }
        }
        catch (URISyntaxException uriSE) {
            // theoretically we can't get URISyntaxException; the URL should
            // be valid.
        }
        return true;
    }
    
    // ---- bluej.compiler.CompileObserver interfaces ----

    /**
     * Observe compilation jobs and change the PkgMgr interface elements as
     * compilation goes through different stages, but don't display the popups
     * for error/warning messages.
     * Also relay compilation events to any listening extensions.
     */
    private class QuietPackageCompileObserver
        implements FXCompileObserver
    {
        protected List<FXCompileObserver> chainedObservers;
        
        /**
         * Construct a new QuietPackageCompileObserver. The chained observers (if
         * non-empty list) are notified about each event.
         */
        public QuietPackageCompileObserver(List<FXCompileObserver> chainedObservers)
        {
            this.chainedObservers = new ArrayList<>(chainedObservers);
        }
        
        private void markAsCompiling(CompileInputFile[] sources, int compilationSequence)
        {
            for (int i = 0; i < sources.length; i++) {
                String fileName = sources[i].getJavaCompileInputFile().getPath();
                String fullName = getProject().convertPathToPackageName(fileName);

                if (fullName != null) {
                    Target t = getTarget(JavaNames.getBase(fullName));

                    if (t instanceof ClassTarget) {
                        ClassTarget ct = (ClassTarget) t;
                        ct.markCompiling(compilationSequence);
                    }
                }
            }
        }

        private void sendEventToExtensions(String filename, int [] errorPosition, String message, EventType eventType, CompileType type)
        {
            File [] sources;
            if (filename != null) {
                sources = new File[1];
                sources[0] = new File(filename);
            }
            else {
                sources = new File[0];
            }
            CompileEvent aCompileEvent = new CompileEvent(eventType, type.keepClasses(), sources);
            aCompileEvent.setErrorPosition(errorPosition);
            aCompileEvent.setErrorMessage(message);
            ExtensionsManager.getInstance().delegateEvent(aCompileEvent);
        }

        /**
         * A compilation has been started. Mark the affected classes as being
         * currently compiled.
         */
        @Override
        public void startCompile(CompileInputFile[] sources, CompileReason reason, CompileType type, int compilationSequence)
        {
            // Send a compilation starting event to extensions.
            CompileEvent aCompileEvent = new CompileEvent(CompileEvent.EventType.COMPILE_START_EVENT, type.keepClasses(), Utility.mapList(Arrays.asList(sources), CompileInputFile::getJavaCompileInputFile).toArray(new File[0]));
            ExtensionsManager.getInstance().delegateEvent(aCompileEvent);

            // Set BlueJ status bar message
            if (type.keepClasses())
            {
                setStatus(compiling);
            }

            // Change view of source classes.
            markAsCompiling(sources, compilationSequence);

            for (FXCompileObserver chainedObserver : chainedObservers)
            {
                chainedObserver.startCompile(sources, reason, type, compilationSequence);
            }
        }

        @Override
        public boolean compilerMessage(Diagnostic diagnostic, CompileType type)
        {
            int [] errorPosition = new int[4];
            errorPosition[0] = (int) diagnostic.getStartLine();
            errorPosition[1] = (int) diagnostic.getStartColumn();
            errorPosition[2] = (int) diagnostic.getEndLine();
            errorPosition[3] = (int) diagnostic.getEndColumn();
            if (diagnostic.getType() == Diagnostic.ERROR) {
                errorMessage(diagnostic.getFileName(), errorPosition, diagnostic.getMessage(), type);
            }
            else {
                warningMessage(diagnostic.getFileName(), errorPosition, diagnostic.getMessage(), type);
            }

            boolean shown = false;
            for (FXCompileObserver chainedObserver : chainedObservers)
            {
                // Don't inline the next two lines, as we
                // always want to call compilerMessage even if
                // a previous observer showed the method:
                boolean s = chainedObserver.compilerMessage(diagnostic, type);
                shown = shown || s;
            }
            return shown;
        }
        
        private void errorMessage(String filename, int [] errorPosition, String message, CompileType type)
        {
            // Send a compilation Error event to extensions.
            sendEventToExtensions(filename, errorPosition, message, CompileEvent.EventType.COMPILE_ERROR_EVENT, type);
        }

        private void warningMessage(String filename, int [] errorPosition, String message, CompileType type)
        {
            // Send a compilation Error event to extensions.
            sendEventToExtensions(filename, errorPosition, message, CompileEvent.EventType.COMPILE_WARNING_EVENT, type);
        }

        /**
         * Compilation has ended. Mark the affected classes as being normal
         * again.
         */
        @Override
        public void endCompile(CompileInputFile[] sources, boolean successful, CompileType type, int compilationSequence)
        {
            List<ClassTarget> targetsToAnalyse = new ArrayList<>();
            List<ClassTarget> readyToCompileList = new ArrayList<>();
            for (int i = 0; i < sources.length; i++) {
                String filename = sources[i].getJavaCompileInputFile().getPath();

                String fullName = getProject().convertPathToPackageName(filename);
                if (fullName == null) {
                    continue;
                }

                ClassTarget t = (ClassTarget) targets.get(JavaNames.getBase(fullName));

                if (t == null) {
                    continue;
                }

                t.markCompiled(successful, type);
                if (t.getState() == State.COMPILED)
                {
                    targetsToAnalyse.add(t);
                }
                else
                {
                    // To prevent an issue that may happen when classes are batched in one compile job
                    // and an error in one may prevent others being compiled even though they could be 
                    // compiled separately, we check for all the classes that can be compiled and have
                    // no direct/indirect dependencies with compile errors to be compiled
                    if (t.getState() == State.NEEDS_COMPILE && type == CompileType.EXPLICIT_USER_COMPILE)
                    {
                        if (!checkDependecyCompilationError(t))
                        {
                            readyToCompileList.add(t);
                        }
                    }
                }
                t.setQueued(false);
                
                if (t.isCompiled())
                {
                    //check if there already exists a class in a library with that name 
                    Class<?> c = loadClass(getQualifiedName(t.getIdentifierName()));
                    if (c!=null){
                        if (! checkClassMatchesFile(c, t.getClassFile())) {
                            String conflict=Package.getResourcePath(c);
                            String ident = t.getIdentifierName()+":";
                            DialogManager.showMessageWithPrefixTextFX(null, "compile-class-library-conflict", ident, conflict);
                        }
                    }

                    /*
                     * compute ctxt files (files with comments and parameters
                     * names)
                     */
                    try {
                        ClassInfo info = t.getSourceInfo().getInfo(t.getJavaSourceFile(), t.getPackage());

                        if (info != null) {
                            OutputStream out = new FileOutputStream(t.getContextFile());
                            info.getComments().store(out, "BlueJ class context");
                            out.close();
                        }
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            // Compile the classes that have no direct/indirect dependencies that have compile errors
            doCompile(readyToCompileList, this, CompileReason.USER, CompileType.EXPLICIT_USER_COMPILE);

            for (ClassTarget classTarget : targetsToAnalyse)
            {
                classTarget.analyseAfterCompile();
            }
            
            if (type.keepClasses())
            {
                setStatus(compileDone);
            }
            fireChangedEvent();
            
            // Send a compilation done event to extensions.
            EventType eventType = successful ? CompileEvent.EventType.COMPILE_DONE_EVENT : CompileEvent.EventType.COMPILE_FAILED_EVENT;
            CompileEvent aCompileEvent = new CompileEvent(eventType, type.keepClasses(), Utility.mapList(Arrays.asList(sources), CompileInputFile::getJavaCompileInputFile).toArray(new File[0]));
            ExtensionsManager.getInstance().delegateEvent(aCompileEvent);

            for (FXCompileObserver chainedObserver : chainedObservers)
            {
                chainedObserver.endCompile(sources, successful, type, compilationSequence);
            }
        }
    }

    /**
     * The same, but also display error/warning messages for the user
     */
    private class PackageCompileObserver extends QuietPackageCompileObserver
    {
        private int numErrors = 0;
        
        /**
         * Construct a new PackageCompileObserver. The chained observer (if specified)
         * is notified when the compilation ends.
         */
        public PackageCompileObserver(List<FXCompileObserver> chainedObservers)
        {
            super(chainedObservers);
        }
        
        @Override
        public void startCompile(CompileInputFile[] sources, CompileReason reason, CompileType type, int compilationSequence)
        {
            numErrors = 0;
            super.startCompile(sources, reason, type, compilationSequence);
        }
        
        @Override
        public boolean compilerMessage(Diagnostic diagnostic, CompileType type)
        {
            super.compilerMessage(diagnostic, type);
            if (diagnostic.getType() == Diagnostic.ERROR) {
                return errorMessage(diagnostic, type);
            }
            else {
                return warningMessage(diagnostic.getFileName(), (int) diagnostic.getStartLine(),
                        diagnostic.getMessage());
            }
        }
        
        /**
         * Display an error message associated with a specific line in a class.
         * This is done by opening the class's source, highlighting the line and
         * showing the message in the editor's information area.
         */
        private boolean errorMessage(Diagnostic diagnostic, CompileType type)
        {
            numErrors += 1;
            ErrorShown messageShown;

            if (diagnostic.getFileName() == null)
            {
                showMessageWithText("compiler-error", diagnostic.getMessage());
                return true;
            }

            String message = diagnostic.getMessage();
            messageShown = showEditorDiagnostic(diagnostic, null, numErrors - 1, type);

            // Display the error message in the source editor
            switch (messageShown)
            {
                case EDITOR_NOT_FOUND:
                    showMessageWithText("error-in-file", diagnostic.getFileName() + ":" +
                        diagnostic.getStartLine() + "\n" + message);
                    return true;
                case ERROR_SHOWN:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Display a warning message: just a dialog box
         * The dialog accumulates messages until reset() is called, which is
         * done in the methods which the user can invoke to cause compilation
         * Thus all the warnings caused by a "compilation" can be accumulated
         * into a single dialog.
         * If searchCompile() built a single list, we wouldn't need to do this
         */
        private boolean warningMessage(String filename, int lineNo, String message)
        {
            return true;
        }
    }

    // ---- end of bluej.compiler.CompileObserver interfaces ----

    /**
     * closeAllEditors - closes all currently open editors within package Should
     * be run whenever a package is removed from PkgFrame.
     */
    public void closeAllEditors()
    {
        // We take a copy here to avoid
        // ConcurrentModificationException which happened on closing
        // BlueJ main frame after renaming a class without compile.
        List<Target> targetsCopy;
        synchronized (this)
        {
            targetsCopy = new ArrayList<>();
            for (Target t : targets)
                targetsCopy.add(t);
        }
        for (Target target : targetsCopy) {
            if (target instanceof EditableTarget) {
                EditableTarget et = (EditableTarget) target;
                if (et.editorOpen()) {
                    et.getEditor().close();
                }
            }
        }
    }

    /**
     * get history of invocation calls
     * 
     * @return CallHistory object
     */
    public CallHistory getCallHistory()
    {
        return callHistory;
    }

    /**
     * String representation for debugging.
     */
    public String toString()
    {
        return "Package:" + getQualifiedName();
    }

    public SourceType getDefaultSourceType()
    {
        // Our heuristic is: if the package contains any Stride files, the default is Stride,
        // otherwise it's Java
        if (getClassTargets().stream().anyMatch(c -> c.getSourceType() == SourceType.Stride))
            return SourceType.Stride;
        else
            return SourceType.Java;
    }


    public void addDependency(Dependency dependency)
    {
        addDependency(dependency, dependency instanceof UsesDependency);
    }

    public void addDependency(Dependency d, boolean recalc)
    {
        DependentTarget from = d.getFrom();
        DependentTarget to = d.getTo();

        if (from == null || to == null)
        {
            // Debug.reportError("Found invalid dependency - ignored.");
            return;
        }

        if (d instanceof UsesDependency)
        {
            if (usesArrows.contains(d))
            {
                return;
            }
            else
            {
                usesArrows.add((UsesDependency) d);
            }
        }
        else
        {
            if (extendsArrows.contains(d))
            {
                return;
            }
            else
            {
                extendsArrows.add(d);
            }
        }

        DependentTarget from1 = d.getFrom();
        DependentTarget to1 = d.getTo();
        from1.addDependencyOut(d, recalc);
        to1.addDependencyIn(d, recalc);
    }

    public void removeDependency(Dependency dependency, boolean recalc)
    {
        if (dependency instanceof UsesDependency)
        {
            usesArrows.remove(dependency);
        }
        else
        {
            extendsArrows.remove(dependency);
        }

        DependentTarget from = dependency.getFrom();
        DependentTarget to = dependency.getTo();

        from.removeDependencyOut(dependency, recalc);
        to.removeDependencyIn(dependency, recalc);
    }

    /**
     * Call the given method or constructor.
     */
    public void callStaticMethodOrConstructor(CallableView view)
    {
        ui.callStaticMethodOrConstructor(view);
    }

    /**
     * Add an observer to listen to all compilations of this package.
     */
    public void addCompileObserver(FXCompileObserver fxCompileObserver)
    {
        compileObservers.add(fxCompileObserver);
    }

    /**
     * Checks if the class target has dependencies that have compilation errors
     * @param  classTarget the class target whom direct/indirect dependencies will be checked
     * @return true if any of the dependencies or their ancestors have a compilation error
     *         otherwise it returns false
     */
    public boolean checkDependecyCompilationError(ClassTarget classTarget)
    {
        // First calculate all dependencies, no matter how indirect,
        // taking care to avoid an infinite loop in the case there is a mutual dependency:
        LinkedList<ClassTarget> toCalculate = new LinkedList<>();
        toCalculate.add(classTarget);
        Set<ClassTarget> dependencies = Sets.newIdentityHashSet();
        while (!toCalculate.isEmpty())
        {
            ClassTarget t = toCalculate.removeFirst();
            for (Dependency d : t.dependencies())
            {
                ClassTarget to = (ClassTarget) d.getTo();
                // If it's a dependency we haven't encountered before, we'll also
                // need to calculate its dependencies:
                if (dependencies.add(to))
                {
                    toCalculate.add(to);
                }
            }
        }
        
        return dependencies.stream().anyMatch(d -> d.getState() == State.HAS_ERROR && d.hasSourceCode());
    }
}
