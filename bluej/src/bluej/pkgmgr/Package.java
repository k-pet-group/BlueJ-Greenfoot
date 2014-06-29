/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2012,2013,2014  Michael Kolling and John Rosenberg 
 
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import bluej.Config;
import bluej.collect.DataCollectionCompileObserverWrapper;
import bluej.collect.DataCollector;
import bluej.compiler.CompileObserver;
import bluej.compiler.Diagnostic;
import bluej.compiler.EventqueueCompileObserver;
import bluej.compiler.JobQueue;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerThread;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.SourceLocation;
import bluej.debugmgr.CallHistory;
import bluej.debugmgr.Invoker;
import bluej.editor.Editor;
import bluej.extensions.BDependency;
import bluej.extensions.BPackage;
import bluej.extensions.ExtensionBridge;
import bluej.extensions.event.CompileEvent;
import bluej.extensions.event.DependencyEvent;
import bluej.extmgr.ExtensionsManager;
import bluej.graph.Edge;
import bluej.graph.Graph;
import bluej.parser.AssistContent;
import bluej.parser.CodeSuggestions;
import bluej.parser.ParseUtils;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;
import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.dependency.ExtendsDependency;
import bluej.pkgmgr.dependency.ImplementsDependency;
import bluej.pkgmgr.dependency.UsesDependency;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.DependentTarget;
import bluej.pkgmgr.target.EditableTarget;
import bluej.pkgmgr.target.PackageTarget;
import bluej.pkgmgr.target.ParentPackageTarget;
import bluej.pkgmgr.target.ReadmeTarget;
import bluej.pkgmgr.target.Target;
import bluej.pkgmgr.target.TargetCollection;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.JavaNames;
import bluej.utility.MultiIterator;
import bluej.utility.SortedProperties;
import bluej.utility.filefilter.JavaClassFilter;
import bluej.utility.filefilter.JavaSourceFilter;
import bluej.utility.filefilter.SubPackageFilter;

/**
 * A Java package (collection of Java classes).
 * 
 * @author Michael Kolling
 * @author Axel Schmolitzky
 * @author Andrew Patterson
 */
public final class Package extends Graph
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
    private SortedProperties lastSavedProps = new SortedProperties();

    /** all the targets in a package */
    private TargetCollection targets;

    /** all the uses-arrows in a package */
    private List<Dependency> usesArrows;

    /** all the extends-arrows in a package */
    private List<Dependency> extendsArrows;

    /** Holds the choice of "from" target for a new dependency */
    private DependentTarget fromChoice;

    /** the CallHistory of a package */
    private CallHistory callHistory;

    /** whether extends-arrows should be shown */
    private boolean showExtends = true;
    /** whether uses-arrows should be shown */
    private boolean showUses = true;

    /**
     * needed when debugging with breakpoints to see if the editor window needs
     * to be brought to the front
     */
    private String lastSourceName = "";

    /** state constant - normal state */
    public static final int S_IDLE = 0;
    /** state constant - choose the "from" target of a "uses" dependency arrow */
    public static final int S_CHOOSE_USES_FROM = 1;
    /** state constant - choose the "to" target for a "uses" dependency arrow */
    public static final int S_CHOOSE_USES_TO = 2;
    /** state constant - choose the "from" target of an "extends" arrow */
    public static final int S_CHOOSE_EXT_FROM = 3;
    /** state constant - choose the "to" target for an "extends" arrow */
    public static final int S_CHOOSE_EXT_TO = 4;

    /** determines the maximum length of the CallHistory of a package */
    public static final int HISTORY_LENGTH = 6;

    /** the state a package can be in (one of the S_* values) */
    private int state = S_IDLE;

    private PackageEditor editor;
    
    /** File pointing at the directory for this package */
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

        init();
    }

    private void init()
        throws IOException
    {
        targets = new TargetCollection();
        usesArrows = new ArrayList<Dependency>();
        extendsArrows = new ArrayList<Dependency>();
        callHistory = new CallHistory(HISTORY_LENGTH);
        dir = new File(project.getProjectDir(), getRelativePath().getPath());
        load();
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

    private BPackage singleBPackage;  // Every Package has none or one BPackage
    
    /**
     * Return the extensions BPackage associated with this Package.
     * There should be only one BPackage object associated with each Package.
     * @return the BPackage associated with this Package.
     */
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
    public String getId()
    {
        return getPath().getPath();
    }

    /**
     * Return this package's base name (eg util) ("" for the unnamed package)
     */
    public String getBaseName()
    {
        return baseName;
    }

    /**
     * Return the qualified name of an identifier in this package (eg
     * java.util.Random if given Random)
     */
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
    public ReadmeTarget getReadmeTarget()
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
    public File getPath() 
    {
        return dir;
    }

    /**
     * Return our parent package or null if we are the unnamed package.
     */
    public Package getParent()
    {
        return parentPackage;
    }

    /**
     * Returns the sub-package if this package is "boring". Our definition of
     * boring is that the package has no classes in it and only one sub package.
     * If this package is not boring, this method returns null.
     */
    protected Package getBoringSubPackage()
    {
        PackageTarget pt = null;

        for (Iterator<Target> e = targets.iterator(); e.hasNext();) {
            Target target = (Target) e.next();

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
    protected List<Package> getChildren(boolean getUncached)
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

    public void repaint()
    {
        if (editor != null) {
            editor.repaint();
        }
    }

    void setEditor(PackageEditor editor)
    {
        this.editor = editor;
    }
    
    public PackageEditor getEditor()
    {
        return editor;
    }

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
    public Target[] getSelectedTargets()
    {
        Target[] targetArray = new Target[0];
        LinkedList<Target> list = new LinkedList<Target>();
        for (Iterator<Target> it = getVertices(); it.hasNext();) {
            Target target = it.next();
            if (target.isSelected()) {
                list.add(target);
            }
        }
        return (Target[]) list.toArray(targetArray);
    }

    /**
     * Get the selected Dependencies.
     * 
     * @return The currently selected dependency or null.
     */
    public Dependency getSelectedDependency()
    {
        for (Iterator<? extends Edge> it = getEdges(); it.hasNext();) {
            Edge edge = it.next();
            if (edge instanceof Dependency && edge.isSelected()) {
                return (Dependency) edge;
            }
        }
        return null;
    }

    /**
     * Returns the {@link Dependency} with the specified <code>origin</code>,
     * <code>target</code> and <code>type</code> or <code>null</code> if there
     * is no such dependency.
     * 
     * @param origin
     *            The origin of the dependency.
     * @param target
     *            The target of the dependency.
     * @param type
     *            The type of the dependency (there may be more than one
     *            dependencies with the same origin and target but different
     *            types).
     * @return The {@link Dependency} with the specified <code>origin</code>,
     *         <code>target</code> and <code>type</code> or <code>null</code> if
     *         there is no such dependency.
     */
    public Dependency getDependency(DependentTarget origin, DependentTarget target, BDependency.Type type)
    {
        List<Dependency> dependencies = new ArrayList<Dependency>();

        switch (type) {
            case USES :
                dependencies = usesArrows;
                break;
            case IMPLEMENTS :
            case EXTENDS :
                dependencies = extendsArrows;
                break;
            case UNKNOWN :
                // If the type of the dependency is UNKNOWN, the requested
                // dependency does not exist anymore. In this case the method
                // returns null.
                return null;
        }

        for (Dependency dependency : dependencies) {
            DependentTarget from = dependency.getFrom();
            DependentTarget to = dependency.getTo();

            if (from.equals(origin) && to.equals(target)) {
                return dependency;
            }
        }

        return null;
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
        File srcFiles[] = path.listFiles(new JavaSourceFilter());
        File classFiles[] = path.listFiles(new JavaClassFilter());

        Set<String> interestingSet = new HashSet<String>();

        // process all *.java files
        for (int i = 0; i < srcFiles.length; i++) {
            // remove all __SHELL*.java files (temp files created by us)
            if (srcFiles[i].getName().startsWith(Invoker.SHELLNAME)) {
                srcFiles[i].delete();
                continue;
            }
            String javaFileName = JavaNames.stripSuffix(srcFiles[i].getName(), ".java");

            // check if the name would be a valid java name
            if (!JavaNames.isIdentifier(javaFileName))
                continue;

            // files with a $ in them signify inner classes (which we want to
            // ignore)
            if (javaFileName.indexOf('$') == -1)
                interestingSet.add(javaFileName);
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
    public void load()
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
            Debug.reportError("Error loading from package file " + packageFile + ": " + e);
            e.printStackTrace();
            return;
        }

        for (int i = 0; i < numTargets; i++) {
            Target target;
            String type = lastSavedProps.getProperty("target" + (i + 1) + ".type");
            String identifierName = lastSavedProps.getProperty("target" + (i + 1) + ".name");

            if ("PackageTarget".equals(type))
                target = new PackageTarget(this, identifierName);
            else {
                target = new ClassTarget(this, identifierName);
            }

            target.load(lastSavedProps, "target" + (i + 1));
            propTargets.put(identifierName, target);
        }

        addImmovableTargets();
        List<Target> targetsToPlace = new ArrayList<Target>();
        
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
                targetsToPlace.add(target);
            }

            addTarget(target);
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
                targetsToPlace.add(target);
            }
            addTarget(target);
        }
        
        // Find an empty spot for any targets which didn't already have
        // a position
        for (Target t : targetsToPlace) {
            findSpaceForVertex(t);
        }
        
        // Start with all classes in the normal (compiled) state.
        Iterator<Target> targetIt = targets.iterator();
        for ( ; targetIt.hasNext();) {
            Target target = targetIt.next();
            if (target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget) target;
                ct.setState(ClassTarget.S_NORMAL);
            }
        }

        // Fix up dependency information
        for (int i = 0; i < numDependencies; i++) {
            Dependency dep = null;
            String type = lastSavedProps.getProperty("dependency" + (i + 1) + ".type");

            if ("UsesDependency".equals(type))
                dep = new UsesDependency(this);

            if (dep != null) {
                dep.load(lastSavedProps, "dependency" + (i + 1));
                addDependency(dep, false);
            }
        }
        recalcArrows();

        // Update class states. We do this before updating roles (or anything else
        // which analyses the source) because the analysis does symbol resolution, and
        // that depends on having the correct compiled state.
        LinkedList<ClassTarget> invalidated = new LinkedList<ClassTarget>();
        targetIt = targets.iterator();
        for ( ; targetIt.hasNext();) {
            Target target = targetIt.next();

            if (target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget) target;
                if (ct.isCompiled() && !ct.upToDate()) {
                    ct.setState(ClassTarget.S_INVALID);
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
                    if (dep.isCompiled()) {
                        dep.setState(ClassTarget.S_INVALID);
                        invalidated.add(dep);
                    }
                }
            }
        }
        
        // Update class roles
        targetIt = targets.iterator();
        for ( ; targetIt.hasNext();) {
            Target target = targetIt.next();

            if (target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget) target;
                if (ct.isCompiled()) {
                    Class<?> cl = loadClass(ct.getQualifiedName());
                    ct.determineRole(cl);
                    ct.analyseDependencies(cl);
                    if (cl == null) {
                        ct.setState(ClassTarget.S_INVALID);
                    }
                }
                else {
                    ct.analyseSource();
                    try {
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
        findSpaceForVertex(t);
    }
    
    /**
     * Add our immovable targets (the readme file, and possibly a link to the
     * parent package)
     */ 
    private void addImmovableTargets()
    {
        // which goes to the parent package)
        //        if (isUnnamedPackage()) {
        //            Target t = new ReadmeTarget(this);
        //            t.setPos(FIXED_TARGET_X,FIXED_TARGET_Y);
        //            addTarget(t);
        //        }
        //        else {
        //            Target t = new ParentPackageTarget(this);
        //            t.setPos(FIXED_TARGET_X,FIXED_TARGET_Y);
        //            addTarget(t);
        //        }
        Target t = new ReadmeTarget(this);
        //Take special care of ReadmeTarget
        //see ReadmeTarget.isSaveable for explanation
        t.load(lastSavedProps, "readme");
        t.setPos(FIXED_TARGET_X, FIXED_TARGET_Y);
        addTarget(t);
        if (!isUnnamedPackage()) {
            t = new ParentPackageTarget(this);
            findSpaceForVertex(t);
            addTarget(t);
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

            Target target = targets.get(subDirs[i].getName());

            if (target == null) {
                Target newtarget = addPackage(subDirs[i].getName());
                findSpaceForVertex(newtarget);
            }
        }

        Set<String> interestingSet = findTargets(getPath());

        for (Iterator<String> it = interestingSet.iterator(); it.hasNext();) {
            String targetName = it.next();

            Target target = targets.get(targetName);

            if (target == null) {
                Target newtarget = addClass(targetName);
                findSpaceForVertex(newtarget);
            }
        }

        for (Iterator<Target> it = targets.iterator(); it.hasNext();) {
            Target target = (Target) it.next();

            if (target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget) target;
                ct.analyseSource();
            }
        }
        
        //Update class roles, and their state
        for (Iterator<Target> it = targets.iterator(); it.hasNext();) {
            Target target = (Target) it.next();

            if (target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget) target;

                Class<?> cl = loadClass(ct.getQualifiedName());
                if (cl != null) {
                    ct.determineRole(cl);
                    if (ct.upToDate()) {
                        ct.setState(ClassTarget.S_NORMAL);
                    }
                    else {
                        ct.setState(ClassTarget.S_INVALID);
                    }
                }
            }
        }

        graphChanged();
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
        packageFile.load(props);

        int numTargets = 0;

        try {
            numTargets = Integer.parseInt(props.getProperty("package.numTargets", "0"));
        }
        catch (Exception e) {
            Debug.reportError("Error loading from bluej package file " + packageFile + ": " + e);
            e.printStackTrace();
            return;
        }
        
        for (int i = 0; i < numTargets; i++) {
            Target target = null;
            String identifierName = props.getProperty("target" + (i + 1) + ".name");
            int x = Integer.parseInt(props.getProperty("target" + (i + 1) + ".x"));
            int y = Integer.parseInt(props.getProperty("target" + (i + 1) + ".y"));
            int height = Integer.parseInt(props.getProperty("target" + (i + 1) + ".height"));
            int width = Integer.parseInt(props.getProperty("target" + (i + 1) + ".width"));
            target = getTarget(identifierName);
            if (target != null){
                target.setPos(x, y);
                target.setSize(width, height);
            }
        }
        repaint();
    }

    /**
     * Save this package to disk. The package is saved to the standard package
     * file.
     */
    public void save(Properties frameProperties)
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

        for (int i = 0; i < usesArrows.size(); i++) { // uses arrows
            Dependency d = (Dependency) usesArrows.get(i);
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

        return;
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
        if (fileName.endsWith(".java")) // it's a Java source file
            className = fileName.substring(0, fileName.length() - 5);
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

        findSpaceForVertex(t);
        t.analyseSource();
        
        DataCollector.addClass(this, destFile);

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

    public Iterator<Target> getVertices()
    {
        return targets.sortediterator();
    }

    public Iterator<? extends Edge> getEdges()
    {
        List<Iterator<? extends Edge>> iterations = new ArrayList<Iterator<? extends Edge>>();

        if (showUses)
            iterations.add(usesArrows.iterator());
        if (showExtends)
            iterations.add(extendsArrows.iterator());

        return new MultiIterator<Edge>(iterations);
    }

    /**
     * Return a List of all ClassTargets that have the role of a unit test.
     */
    public List<ClassTarget> getTestTargets()
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
     * The standard compile user function: Find and compile all uncompiled
     * classes.
     */
    public void compile(CompileObserver compObserver)
    {
        if (!checkCompile()) {
            return;
        }

        Set<ClassTarget> toCompile = new HashSet<ClassTarget>();

        try {
            // build the list of targets that need to be compiled
            for (Target target : targets.toArray()) {
                if (target instanceof ClassTarget) {
                    ClassTarget ct = (ClassTarget) target;
                    if (ct.isInvalidState() && ! ct.isQueued()) {
                        ct.ensureSaved();
                        toCompile.add(ct);
                        ct.setQueued(true);
                    }
                }
            }

            project.removeClassLoader();
            project.newRemoteClassLoaderLeavingBreakpoints();
            doCompile(toCompile, new PackageCompileObserver(compObserver));
        }
        catch (IOException ioe) {
            // Abort compile
            Debug.log("Error saving class before compile: " + ioe.getLocalizedMessage());
            for (ClassTarget ct : toCompile) {
                ct.setQueued(false);
            }
        }
    }
    
    /**
     * The standard compile user function: Find and compile all uncompiled
     * classes.
     */
    public void compile()
    {
        compile((CompileObserver)null);
    }
    
    /**
     * Compile a single class.
     */
    public void compile(ClassTarget ct)
    {
        compile(ct, false, null);
    }
    
    /**
     * Compile a single class.
     */
    public void compile(ClassTarget ct, boolean forceQuiet, CompileObserver compObserver)
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
        if (ct.hasSourceCode()) {
            ct.setInvalidState(); // to force compile
        }
        else {
            ct = null;
        }

        if (assocTarget != null) {
            assocTarget.setInvalidState();
        }

        if (ct != null || assocTarget != null) {
            project.removeClassLoader();
            project.newRemoteClassLoaderLeavingBreakpoints();

            // Clear-down the compiler Warning dialog box singleton
            bluej.compiler.CompilerWarningDialog.getDialog().reset();

            if (ct != null) {
                CompileObserver observer;
                if (forceQuiet) {
                    observer = new QuietPackageCompileObserver(compObserver);
                } else {
                    observer = new PackageCompileObserver(compObserver);
                }
                searchCompile(ct, observer);
            }

            if (assocTarget != null) {
                searchCompile(assocTarget, new QuietPackageCompileObserver(null));
            }
        }
    }

    /**
     * Compile a single class quietly.
     */
    public void compileQuiet(ClassTarget ct)
    {
        if (!isDebuggerIdle()) {
            return;
        }

        ct.setInvalidState(); // to force compile
        searchCompile(ct, new QuietPackageCompileObserver(null));
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
        for (Iterator<Target> it = targets.iterator(); it.hasNext();) {
            Target target = it.next();
            if (target instanceof ClassTarget) {
                compileTargets.add((ClassTarget) target);
            }
        }

        try {
            for (Iterator<ClassTarget> i = compileTargets.iterator(); i.hasNext(); ) {
                ClassTarget ct = i.next();
                // we don't want to try and compile if it is a class target without src
                if (ct.hasSourceCode()) {
                    ct.ensureSaved();
                    ct.setState(ClassTarget.S_INVALID);
                    ct.setQueued(true);
                }
                else {
                    i.remove();
                }
            }
            project.removeClassLoader();
            project.newRemoteClassLoader();
            
            // Clear-down the compiler Warning dialog box singleton
            bluej.compiler.CompilerWarningDialog.getDialog().reset();

            doCompile(compileTargets, new PackageCompileObserver(null));
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
        // the collection - hence the toArray() call here:
        for (Target target : targets.toArray()) {
            if (target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget) target;
                Editor ed = ct.getEditor();
                // Editor can be null eg. class file and no src file
                if(ed != null) {
                    ed.save();
                }
            }
        }
    }
    
    /**
     * Compile a class together with its dependencies, as necessary.
     */
    private void searchCompile(ClassTarget t, CompileObserver observer)
    {
        if (! t.isInvalidState() || t.isQueued()) {
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

                Iterator<? extends Dependency> dependencies = head.dependencies();

                while (dependencies.hasNext()) {
                    Dependency d = (Dependency) dependencies.next();
                    if (!(d.getTo() instanceof ClassTarget)) {
                        continue;
                    }

                    ClassTarget to = (ClassTarget) d.getTo();
                    if (to.isInvalidState() && ! to.isQueued() && toCompile.add(to)) {
                        to.ensureSaved();
                        to.setQueued(true);
                        queue.add(to);
                    }
                }
            }

            doCompile(toCompile, observer);
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
    private void doCompile(Collection<ClassTarget> targetList, CompileObserver observer)
    {
        observer = new EventqueueCompileObserver(observer);
        if (targetList.isEmpty()) {
            return;
        }

        File[] srcFiles = new File[targetList.size()];
        
        int i = 0;
        for (ClassTarget ct : targetList) {
            srcFiles[i++] = ct.getSourceFile();
        }
        
        JobQueue.getJobQueue().addJob(srcFiles, new DataCollectionCompileObserverWrapper(project, observer), project.getClassLoader(), project.getProjectDir(),
                ! PrefMgr.getFlag(PrefMgr.SHOW_UNCHECKED), project.getProjectCharset());
    }

    /**
     * Returns true if the debugger is not busy. This is true if it is either
     * IDLE, or has not been completely constructed (NOTREADY).
     */
    public boolean isDebuggerIdle()
    {
        int status = getDebugger().getStatus();
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
        for (Iterator<Target> it = targets.iterator(); it.hasNext();) {
            Target target = it.next();

            if (target instanceof ClassTarget) {
                ((ClassTarget) target).reInitBreakpoints();
            }
        }
    }

    /**
     * Remove all step marks in all classes.
     */
    public void removeStepMarks()
    {
        for (Iterator<Target> it = targets.iterator(); it.hasNext();) {
            Target target = (Target) it.next();

            if (target instanceof ClassTarget)
                ((ClassTarget) target).removeStepMark();
        }
    }

    public void addTarget(Target t)
    {
        if (t.getPackage() != this)
            throw new IllegalArgumentException();

        targets.add(t.getIdentifierName(), t);
        graphChanged();
    }

    public void removeTarget(Target t)
    {
        targets.remove(t.getIdentifierName());
        removedSelectableElement(t);
        t.setRemoved();
        graphChanged();
    }

    /**
     * Changes the Target identifier. Targets are stored in a hashtable with
     * their name as the key. If class name changes we need to remove the target
     * and add again with the new key.
     */
    public void updateTargetIdentifier(Target t, String oldIdentifier, String newIdentifier)
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
    public void removeArrow(Dependency d)
    {
        if (!(d instanceof UsesDependency)) {
            userRemoveDependency(d);
        }
        removeDependency(d, true);
        graphChanged();
    }

    /**
     * Add a dependancy in this package. The dependency is also added to the
     * individual targets involved.
     */
    public void addDependency(Dependency d, boolean recalc)
    {
        DependentTarget from = d.getFrom();
        DependentTarget to = d.getTo();

        if (from == null || to == null) {
            // Debug.reportError("Found invalid dependency - ignored.");
            return;
        }

        if (d instanceof UsesDependency) {
            int index = usesArrows.indexOf(d);
            if (index != -1) {
                ((UsesDependency) usesArrows.get(index)).setFlag(true);
                return;
            }
            else
                usesArrows.add(d);
        }
        else {
            if (extendsArrows.contains(d))
                return;
            else
                extendsArrows.add(d);
        }

        from.addDependencyOut(d, recalc);
        to.addDependencyIn(d, recalc);

        // Inform all listeners about the added dependency
        DependencyEvent event = new DependencyEvent(d, this, DependencyEvent.Type.DEPENDENCY_ADDED);
        ExtensionsManager.getInstance().delegateEvent(event);
    }

    /**
     * A user initiated addition of an "implements" clause from a class to
     * an interface
     *
     * @pre d.getFrom() and d.getTo() are both instances of ClassTarget
     */
    public void userAddImplementsClassDependency(Dependency d)
    {
        ClassTarget from = (ClassTarget) d.getFrom(); // a class
        ClassTarget to = (ClassTarget) d.getTo(); // an interface
        Editor ed = from.getEditor();
        try {
            ed.save();
            
            ClassInfo info = from.getSourceInfo().getInfo(from.getSourceFile(), this);
            if (info != null) {
                
                Selection s1 = info.getImplementsInsertSelection();
                ed.setSelection(s1.getLine(), s1.getColumn(), s1.getEndLine(), s1.getEndColumn());
                
                if (info.hasInterfaceSelections()) {
                    // if we already have an implements clause then we need to put a
                    // comma and the interface name but not before checking that we
                    // don't already have it
                    
                    List<String> exists = getInterfaceTexts(ed, info.getInterfaceSelections());
                    
                    // XXX make this equality check against full package name
                    if (!exists.contains(to.getBaseName()))
                        ed.insertText(", " + to.getBaseName(), false);
                }
                else {
                    // otherwise we need to put the actual "implements" word
                    // and the interface name
                    ed.insertText(" implements " + to.getBaseName(), false);
                }
                ed.save();
            }
        }
        catch (IOException ioe) {
            showMessageWithText("generic-file-save-error", ioe.getLocalizedMessage());
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
        ClassTarget from = (ClassTarget) d.getFrom(); // an interface
        ClassTarget to = (ClassTarget) d.getTo(); // an interface
        Editor ed = from.getEditor();
        try {
            ed.save();

            ClassInfo info = from.getSourceInfo().getInfo(from.getSourceFile(), this);

            if (info != null) {
                Selection s1 = info.getExtendsInsertSelection();
                ed.setSelection(s1.getLine(), s1.getColumn(), s1.getEndLine(), s1.getEndColumn());
                
                if (info.hasInterfaceSelections()) {
                    // if we already have an extends clause then we need to put a
                    // comma and the interface name but not before checking that we
                    // don't
                    // already have it
                    
                    List<String> exists = getInterfaceTexts(ed, info.getInterfaceSelections());
                    
                    // XXX make this equality check against full package name
                    if (!exists.contains(to.getBaseName()))
                        ed.insertText(", " + to.getBaseName(), false);
                }
                else {
                    // otherwise we need to put the actual "extends" word
                    // and the interface name
                    ed.insertText(" extends " + to.getBaseName(), false);
                }
                ed.save();
            }
        }
        catch (IOException ioe) {
            showMessageWithText("generic-file-save-error", ioe.getLocalizedMessage());
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
        ClassTarget from = (ClassTarget) d.getFrom();
        ClassTarget to = (ClassTarget) d.getTo();
        Editor ed = from.getEditor();
        try {
            ed.save();

            ClassInfo info = from.getSourceInfo().getInfo(from.getSourceFile(), this);

            if (info != null) {
                if (info.getSuperclass() == null) {
                    Selection s1 = info.getExtendsInsertSelection();
                    
                    ed.setSelection(s1.getLine(), s1.getColumn(), s1.getEndLine(), s1.getEndColumn());
                    ed.insertText(" extends " + to.getBaseName(), false);
                }
                else {
                    Selection s1 = info.getSuperReplaceSelection();
                    
                    ed.setSelection(s1.getLine(), s1.getColumn(), s1.getEndLine(), s1.getEndColumn());
                    ed.insertText(to.getBaseName(), false);
                }
                ed.save();
            }
        }
        catch (IOException ioe) {
            showMessageWithText("generic-file-save-error", ioe.getLocalizedMessage());
        }
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
        Editor ed = from.getEditor();
        try {
            ed.save();

            ClassInfo info = from.getSourceInfo().getInfo(from.getSourceFile(), this);
            if (info != null) {
                Selection s1 = null;
                
                if (d instanceof ImplementsDependency) {
                    List<Selection> vsels;
                    List<String> vtexts;
                    
                    vsels = info.getInterfaceSelections();
                    vtexts = getInterfaceTexts(ed, vsels);
                    int where = vtexts.indexOf(to.getBaseName());
                    
                    // we have a special case if we deleted the first bit of an
                    // "implements" clause, yet there are still clauses left.. we have
                    // to delete the following "," instead of the preceding one.
                    if (where == 1 && vsels.size() > 2)
                        where = 2;
                    
                    if (where > 0) { // should always be true
                        s1 = (Selection) vsels.get(where - 1);
                        s1.combineWith((Selection) vsels.get(where));
                    }
                }
                else if (d instanceof ExtendsDependency) {
                    // a class extends
                    s1 = info.getExtendsReplaceSelection();
                    s1.combineWith(info.getSuperReplaceSelection());
                }
                
                // delete the text from the end backwards so that our
                if (s1 != null) {
                    ed.setSelection(s1.getLine(), s1.getColumn(), s1.getEndLine(), s1.getEndColumn());
                    ed.insertText("", false);
                }
                
                ed.save();
            }
        }
        catch (IOException ioe) {
            showMessageWithText("generic-file-save-error", ioe.getLocalizedMessage());
        }
    }
    
    /**
     * Using a list of selections, retrieve a list of text strings from the editor which
     * correspond to those selections.
     * TODO this is usually used to get the implemented interfaces, but it is a clumsy way
     *      to do that.
     */
    private List<String> getInterfaceTexts(Editor ed, List<Selection> selections)
    {
        List<String> r = new ArrayList<String>(selections.size());
        Iterator<Selection> i = selections.iterator();
        while (i.hasNext()) {
            Selection sel = i.next();
            String text = ed.getText(new bluej.parser.SourceLocation(sel.getLine(), sel.getColumn()),
                    new bluej.parser.SourceLocation(sel.getEndLine(), sel.getEndColumn()));
            
            // check for type arguments: don't include them in the text
            int taIndex = text.indexOf('<');
            if (taIndex != -1)
                text = text.substring(0, taIndex);
            text = text.trim();
            
            r.add(text);
        }
        return r;
    }

    /**
     * Remove a dependency from this package. The dependency is also removed
     * from the individual targets involved.
     */
    public void removeDependency(Dependency d, boolean recalc)
    {
        if (d instanceof UsesDependency)
            usesArrows.remove(d);
        else
            extendsArrows.remove(d);

        DependentTarget from = d.getFrom();
        from.removeDependencyOut(d, recalc);

        DependentTarget to = d.getTo();
        to.removeDependencyIn(d, recalc);

        removedSelectableElement(d);

        // Inform all listeners about the removed dependency
        DependencyEvent event = new DependencyEvent(d, this, DependencyEvent.Type.DEPENDENCY_REMOVED);
        ExtensionsManager.getInstance().delegateEvent(event);
    }

    /**
     * Lay out the arrows between targets.
     */
    private void recalcArrows()
    {
        Iterator<Target> it = getVertices();
        while (it.hasNext()) {
            Target t = it.next();

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
    public Target getTarget(String identifierName)
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
    public DependentTarget getDependentTarget(String identifierName)
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
    public final ArrayList<ClassTarget> getClassTargets()
    {
        ArrayList<ClassTarget> risul = new ArrayList<ClassTarget>();

        for (Iterator<Target> it = targets.iterator(); it.hasNext();) {
            Target target = (Target) it.next();

            if (target instanceof ClassTarget) {
                risul.add((ClassTarget) target);
            }
        }
        return risul;
    }

    /**
     * Return a List of Strings with names of all classes in this package.
     */
    public List<String> getAllClassnames()
    {
        List<String> names = new ArrayList<String>();
        
        for (Iterator<Target> it = targets.iterator(); it.hasNext();) {
            Target t = it.next();

            if (t instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget) t;
                names.add(ct.getBaseName());
            }
        }
        return names;
    }

    /**
     * Return a List of Strings with names of all classes in this package that
     * has accompanying source.
     */
    public List<String> getAllClassnamesWithSource()
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

    public void setShowUses(boolean state)
    {
        showUses = state;
    }

    public void setShowExtends(boolean state)
    {
        showExtends = state;
    }

    public void setState(int state)
    {
        this.state = state;
    }

    public int getState()
    {
        return state;
    }

    /**
     * Test whether a file instance denotes a BlueJ or Greenfoot package directory depending on which mode we are in.
     * 
     * @param f
     *            the file instance that is tested for denoting a BlueJ package.
     * @return true if f denotes a directory and a BlueJ package.
     */
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
    public static boolean isPackageFileName(String name)
    {
        if(Config.isGreenfoot())
            return GreenfootProjectFile.isProjectFileName(name);
        else 
            return BlueJPackageFile.isPackageFileName(name);
    }

    /**
     * Called when in an interesting state (e.g. adding a new dependency) and a
     * target is selected. Calling with 'null' as parameter resets to idle state.
     */
    public void targetSelected(Target t)
    {
        if(t == null) {
            if(getState() != S_IDLE) {
                setState(S_IDLE);
                setStatus(" ");
            }
            return;
        }

        switch(getState()) {
            case S_CHOOSE_USES_FROM :
                if (t instanceof DependentTarget) {
                    fromChoice = (DependentTarget) t;
                    setState(S_CHOOSE_USES_TO);
                    setStatus(chooseUsesTo);
                }
                else {
                    setState(S_IDLE);
                    setStatus(" ");
                }
                break;

            case S_CHOOSE_USES_TO :
                if (t != fromChoice && t instanceof DependentTarget) {
                    setState(S_IDLE);
                    addDependency(new UsesDependency(this, fromChoice, (DependentTarget) t), true);
                    setStatus(" ");
                }
                break;

            case S_CHOOSE_EXT_FROM :

                if (t instanceof DependentTarget) {
                    fromChoice = (DependentTarget) t;
                    setState(S_CHOOSE_EXT_TO);
                    setStatus(chooseInhTo);
                }
                else {
                    setState(S_IDLE);
                    setStatus(" ");
                }
                break;

            case S_CHOOSE_EXT_TO :
                if (t != fromChoice) {
                    setState(S_IDLE);
                    if (t instanceof ClassTarget && fromChoice instanceof ClassTarget) {

                        ClassTarget from = (ClassTarget) fromChoice;
                        ClassTarget to = (ClassTarget) t;

                        // if the target is an interface then we have an
                        // implements dependency
                        if (to.isInterface()) {
                            Dependency d = new ImplementsDependency(this, from, to);

                            if (from.isInterface()) {
                                userAddImplementsInterfaceDependency(d);
                            }
                            else {
                                userAddImplementsClassDependency(d);
                            }

                            addDependency(d, true);
                        }
                        else {
                            // an extends dependency can only be from a class to
                            // another class
                            if (!from.isInterface() && !to.isEnum() && !from.isEnum()) {
                                Dependency d = new ExtendsDependency(this, from, to);
                                userAddExtendsClassDependency(d);
                                addDependency(d, true);
                            }
                            else {
                                // TODO display an error dialog or status
                            }
                        }
                    }
                    setStatus(" ");
                }
                break;

            default :
                // e.g. deleting arrow - selecting target ignored
                break;
        }
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
     * A thread has hit a breakpoint or done a step. Organise display (highlight
     * line in source, pop up exec controls).
     */
    public boolean showSource(String sourcename, int lineNo, String threadName, boolean breakpoint)
    {
        String msg = " ";

        if (breakpoint)
            msg = "Thread \"" + threadName + "\" stopped at breakpoint.";

        boolean bringToFront = !sourcename.equals(lastSourceName);
        lastSourceName = sourcename;

        if (!showEditorMessage(new File(getPath(), sourcename).getPath(), lineNo, msg, false, bringToFront,
                true, null) && breakpoint) {
            showMessageWithText("break-no-source", sourcename);
        }

        return bringToFront;
    }
    
    public static interface MessageCalculator
    {
        // This should produce something half-way helpful if null is passed:
        public String calculateMessage(Editor e);
    }
    
    /**
     * Display an error message associated with a specific line in a class. This
     * is done by opening the class's source, highlighting the line and showing
     * the message in the editor's information area.
     */
    private boolean showEditorMessage(String filename, int lineNo, final String message, boolean beep,
            boolean bringToFront, boolean setStepMark, String help)
    {
        return showEditorMessage(filename, lineNo, new MessageCalculator() {
            public String calculateMessage(Editor e) { return message; }
          }, beep, bringToFront, setStepMark, help);
    }

    /**
     * Display an enhanced error message associated with a specific line in a
     * class. This is done by opening the class's source, highlighting the line
     * and showing the message in the editor's information area.
     */
    private boolean showEditorMessage(String filename, int lineNo, MessageCalculator messageCalc, boolean beep,
            boolean bringToFront, boolean setStepMark, String help)
    {
        String fullName = getProject().convertPathToPackageName(filename);
        if (fullName == null) {
            return false;
        }
        
        String packageName = JavaNames.getPrefix(fullName);
        String className = JavaNames.getBase(fullName);

        ClassTarget t = null;

        // check if the error is from a file belonging to another package
        if (! packageName.equals(getQualifiedName())) {

            Package pkg = getProject().getPackage(packageName);
            
            if (pkg != null) {
                PkgMgrFrame pmf = PkgMgrFrame.findFrame(pkg);

                if ((pmf = PkgMgrFrame.findFrame(pkg)) == null) {
                    pmf = PkgMgrFrame.createFrame(pkg);
                }

                pmf.setVisible(true);

                t = (ClassTarget) pkg.getTarget(className);
            }
        }
        else {
            t = (ClassTarget) getTarget(className);
        }

        if (t == null) {
            return false;
        }

        Editor editor = t.getEditor();
        if (editor != null) {
            if (bringToFront || !editor.isShowing()) {
                t.open();
            }
            editor.displayMessage(messageCalc.calculateMessage(editor), lineNo, 0, beep, setStepMark, help);
        }
        else {
            Debug.message(t.getDisplayName() + ", line" + lineNo + ": " + messageCalc.calculateMessage(null));
        }
        return true;
    }
    
    /**
     * Display a compiler diagnostic (error or warning) in the appropriate editor window.
     * 
     * @param diagnostic   The diagnostic to display
     * @param messageCalc  The message "calculator", which returns a modified version of the message;
     *                     may be null, in which case the original message is shown unmodified.
     */
    private boolean showEditorDiagnostic(Diagnostic diagnostic, MessageCalculator messageCalc)
    {
        String fileName = diagnostic.getFileName();
        if (fileName == null) {
            return false;
        }
        
        String fullName = getProject().convertPathToPackageName(diagnostic.getFileName());
        if (fullName == null) {
            return false;
        }
        
        String packageName = JavaNames.getPrefix(fullName);
        String className = JavaNames.getBase(fullName);

        ClassTarget t = null;

        // check if the error is from a file belonging to another package
        if (! packageName.equals(getQualifiedName())) {

            Package pkg = getProject().getPackage(packageName);
            
            if (pkg != null) {
                PkgMgrFrame pmf = PkgMgrFrame.findFrame(pkg);

                if ((pmf = PkgMgrFrame.findFrame(pkg)) == null) {
                    pmf = PkgMgrFrame.createFrame(pkg);
                }

                pmf.setVisible(true);

                t = (ClassTarget) pkg.getTarget(className);
            }
        }
        else {
            t = (ClassTarget) getTarget(className);
        }

        if (t == null) {
            return false;
        }

        Editor editor = t.getEditor();
        if (editor != null) {
            editor.setVisible(true);
            if (messageCalc != null) {
                diagnostic.setMessage(messageCalc.calculateMessage(editor));
            }
            editor.displayDiagnostic(diagnostic);
        }
        else {
            Debug.message(t.getDisplayName() + ", line" + diagnostic.getStartLine() +
                    ": " + diagnostic.getMessage());
        }
        return true;
    }

    /**
     * A breakpoint in this package was hit.
     */
    public void hitBreakpoint(DebuggerThread thread)
    {
        showSource(thread.getClassSourceName(0), thread.getLineNumber(0), thread.getName(), true);

        getProject().getExecControls().showHide(true);
        getProject().getExecControls().makeSureThreadIsSelected(thread);
    }

    /**
     * Execution stopped by someone pressing the "halt" button or we have just
     * done a "step".
     */
    public void hitHalt(DebuggerThread thread)
    {
        int frame = thread.getSelectedFrame();
        if (showSource(thread.getClassSourceName(frame), thread.getLineNumber(frame), thread.getName(), false)) {
            getProject().getExecControls().setVisible(true);
        }

        getProject().getExecControls().showHide(true);
        getProject().getExecControls().makeSureThreadIsSelected(thread);
    }

    /**
     * Display a source file from this package at the specified position.
     */
    public void showSourcePosition(String sourceName, int lineNumber)
    {
        if (showSource(sourceName, lineNumber, null, false)) {
            getProject().getExecControls().setVisible(true);
        }
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
            String filename = new File(getPath(), loc.getFileName()).getPath();
            int lineNo = loc.getLineNumber();
            done = showEditorMessage(filename, lineNo, message, true, true, false, "exception");
            if (firstTime && !done) {
                message += " (in " + loc.getClassName() + ")";
                firstTime = false;
            }
        }
        if (!done) {
            SourceLocation loc = (SourceLocation) stack.get(0);
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
        showEditorMessage(className, lineNumber, "", false, true, false, "exception");
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
        implements CompileObserver
    {
        protected CompileObserver chainObserver;
        
        /**
         * Construct a new QuietPackageCompileObserver. The chained observer (if
         * specified) is notified when the compilation ends.
         */
        public QuietPackageCompileObserver(CompileObserver chainObserver)
        {
            this.chainObserver = chainObserver;
        }
        
        private void markAsCompiling(File[] sources)
        {
            for (int i = 0; i < sources.length; i++) {
                String fileName = sources[i].getPath();
                String fullName = getProject().convertPathToPackageName(fileName);

                if (fullName != null) {
                    Target t = getTarget(JavaNames.getBase(fullName));

                    if (t instanceof ClassTarget) {
                        ClassTarget ct = (ClassTarget) t;
                        ct.setState(ClassTarget.S_COMPILING);
                    }
                }
            }
        }

        private void sendEventToExtensions(String filename, int [] errorPosition, String message, int eventType)
        {
            File [] sources;
            if (filename != null) {
                sources = new File[1];
                sources[0] = new File(filename);
            }
            else {
                sources = new File[0];
            }
            CompileEvent aCompileEvent = new CompileEvent(eventType, sources);
            aCompileEvent.setErrorPosition(errorPosition);
            aCompileEvent.setErrorMessage(message);
            ExtensionsManager.getInstance().delegateEvent(aCompileEvent);
        }

        /**
         * A compilation has been started. Mark the affected classes as being
         * currently compiled.
         */
        @Override
        public void startCompile(File[] sources)
        {
            // Send a compilation starting event to extensions.
            CompileEvent aCompileEvent = new CompileEvent(CompileEvent.COMPILE_START_EVENT, sources);
            ExtensionsManager.getInstance().delegateEvent(aCompileEvent);

            // Set BlueJ status bar message
            setStatus(compiling);

            // Change view of source classes
            markAsCompiling(sources);
        }

        @Override
        public boolean compilerMessage(Diagnostic diagnostic)
        {
            int [] errorPosition = new int[4];
            errorPosition[0] = (int) diagnostic.getStartLine();
            errorPosition[1] = (int) diagnostic.getStartColumn();
            errorPosition[2] = (int) diagnostic.getEndLine();
            errorPosition[3] = (int) diagnostic.getEndColumn();
            if (diagnostic.getType() == Diagnostic.ERROR) {
                errorMessage(diagnostic.getFileName(), errorPosition, diagnostic.getMessage());
            }
            else {
                warningMessage(diagnostic.getFileName(), errorPosition, diagnostic.getMessage());
            }
            return false;
        }
        
        private void errorMessage(String filename, int [] errorPosition, String message)
        {
            // Send a compilation Error event to extensions.
            sendEventToExtensions(filename, errorPosition, message, CompileEvent.COMPILE_ERROR_EVENT);
        }

        private void warningMessage(String filename, int [] errorPosition, String message)
        {
            // Send a compilation Error event to extensions.
            sendEventToExtensions(filename, errorPosition, message, CompileEvent.COMPILE_WARNING_EVENT);
        }

        /**
         * Compilation has ended. Mark the affected classes as being normal
         * again.
         */
        @Override
        public void endCompile(File[] sources, boolean successful)
        {
            for (int i = 0; i < sources.length; i++) {
                String filename = sources[i].getPath();

                String fullName = getProject().convertPathToPackageName(filename);
                if (fullName == null) {
                    continue;
                }

                ClassTarget t = (ClassTarget) targets.get(JavaNames.getBase(fullName));

                if (t == null) {
                    continue;
                }

                boolean newCompiledState = successful;

                if (successful) {
                    t.endCompile();

                    //check if there already exists a class in a library with that name 
                    Class<?> c = loadClass(getQualifiedName(t.getIdentifierName()));
                    if (c!=null){
                        if (! checkClassMatchesFile(c, t.getClassFile())) {
                            String conflict=Package.getResourcePath(c);
                            DialogManager.showMessageWithPrefixText(null, "compile-class-library-conflict", t.getIdentifierName()+":", conflict);
                        }
                    }

                    /*
                     * compute ctxt files (files with comments and parameters
                     * names)
                     */
                    try {
                        ClassInfo info = t.getSourceInfo().getInfo(t.getSourceFile(), t.getPackage());

                        if (info != null) {
                            OutputStream out = new FileOutputStream(t.getContextFile());
                            info.getComments().store(out, "BlueJ class context");
                            out.close();
                        }
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    // Empty class files should not be marked compiled,
                    // even though compilation is "successful".
                    newCompiledState &= t.upToDate();
                }

                t.setState(newCompiledState ? ClassTarget.S_NORMAL : ClassTarget.S_INVALID);
                t.setQueued(false);
                if (successful && t.editorOpen())
                    t.getEditor().setCompiled(true);
            }
            setStatus(compileDone);
            graphChanged();

            // Send a compilation done event to extensions.
            int eventId = successful ? CompileEvent.COMPILE_DONE_EVENT : CompileEvent.COMPILE_FAILED_EVENT;
            CompileEvent aCompileEvent = new CompileEvent(eventId, sources);
            ExtensionsManager.getInstance().delegateEvent(aCompileEvent);
            
            if (chainObserver != null) {
                chainObserver.endCompile(sources, successful);
            }
        }
    }
    
    private static class MisspeltMethodChecker implements MessageCalculator
    {
        private static final int MAX_EDIT_DISTANCE = 2;
        private final String message;
        private int lineNumber;
        private int column;
        private Project project;

        public MisspeltMethodChecker(String message, int column, int lineNumber, Project project)
        {
            this.message = message;
            this.column = column;
            this.lineNumber = lineNumber;
            this.project = project;
        }
        
        private static String chopAtOpeningBracket(String name)
        {
            int openingBracket = name.indexOf('(');
            if (openingBracket >= 0)
                return name.substring(0,openingBracket);
            else
                return name;
        }

        private String getLine(Editor e)
        {
            return e.getText(new bluej.parser.SourceLocation(lineNumber, 1), new bluej.parser.SourceLocation(lineNumber, e.getLineLength(lineNumber-1)));
        }
        
        private int getLineStart(Editor e)
        {
            return e.getOffsetFromLineColumn(new bluej.parser.SourceLocation(lineNumber, 1));
        }
        
        // Levenshtein distance, taken from http://www.merriampark.com/ld.htm
        private static int editDistance(String s, String t)
        {
            int d[][]; // matrix
            int n; // length of s
            int m; // length of t
            int i; // iterates through s
            int j; // iterates through t
            char s_i; // ith character of s
            char t_j; // jth character of t
            int cost; // cost

            // Step 1
            n = s.length ();
            m = t.length ();
            if (n == 0) {
                return m;
            }
            if (m == 0) {
                return n;
            }
            d = new int[n+1][m+1];

            // Step 2
            for (i = 0; i <= n; i++) {
                d[i][0] = i;
            }
            for (j = 0; j <= m; j++) {
                d[0][j] = j;
            }

            // Step 3
            for (i = 1; i <= n; i++) {
                s_i = s.charAt (i - 1);

                // Step 4
                for (j = 1; j <= m; j++) {
                    t_j = t.charAt (j - 1);

                    // Step 5
                    if (s_i == t_j) {
                        cost = 0;
                    }
                    else {
                        cost = 1;
                    }

                    // Step 6
                    d[i][j] = Math.min(Math.min(d[i-1][j]+1, d[i][j-1]+1), d[i-1][j-1] + cost);
                }
            }

            // Step 7
            return d[n][m];
        }
        
        @Override
        public String calculateMessage(Editor e)
        {
            if (e == null) {
                return message;
            }
            
            String missing = chopAtOpeningBracket(message.substring(message.lastIndexOf(' ') + 1));

            String lineText = getLine(e);
            
            // The column from the diagnostic object assumes tabs are 8 spaces; convert to
            // a line position:
            int pos = convertColumn(lineText, column) + getLineStart(e);
            
            LinkedList<String> maybeTheyMeant = new LinkedList<String>();
            CodeSuggestions suggests = e.getParsedNode().getExpressionType(pos, e.getSourceDocument());
            AssistContent[] values = ParseUtils.getPossibleCompletions(suggests, project.getJavadocResolver());
            if (values != null) {
                for (AssistContent a : values) {
                    String name = chopAtOpeningBracket(a.getDisplayName());

                    if (editDistance(name.toLowerCase(), missing.toLowerCase()) <= MAX_EDIT_DISTANCE) {
                        maybeTheyMeant.addLast(a.getDisplayName());
                    }
                }
            }

            if (maybeTheyMeant.isEmpty()) {
                return message;
            } else {
                String augmentedMessage = message + "; maybe you meant: " + maybeTheyMeant.getFirst();
                maybeTheyMeant.removeFirst();
                for (String sugg : maybeTheyMeant) {
                    augmentedMessage += " or " + sugg;
                }
                return augmentedMessage;
            }
        }
        
        /** 
         * Convert a column where a tab is counted as 8 to a column where a tab is counted
         * as 1
         */
        private static int convertColumn(String string, int column)
        {
            int ccount = 0; // count of characters
            int lpos = 0;   // count of columns (0 based)

            int tabIndex = string.indexOf('\t');
            while (tabIndex != -1 && lpos < column - 1) {
                lpos += tabIndex - ccount;
                ccount = tabIndex;
                if (lpos >= column - 1) {
                    break;
                }
                lpos = ((lpos + 8) / 8) * 8;  // tab!
                ccount += 1;
                tabIndex = string.indexOf('\t', ccount);
            }

            ccount += column - lpos;
            return ccount;
        }
    }

    /**
     * The same, but also display error/warning messages for the user
     */
    private class PackageCompileObserver extends QuietPackageCompileObserver
    {
        private boolean hadError;
        
        /**
         * Construct a new PackageCompileObserver. The chained observer (if specified)
         * is notified when the compilation ends.
         */
        public PackageCompileObserver(CompileObserver chainObserver)
        {
            super(chainObserver);
        }
        
        @Override
        public void startCompile(File[] sources)
        {
            hadError = false;
            super.startCompile(sources);
        }
        
        @Override
        public boolean compilerMessage(Diagnostic diagnostic)
        {
            super.compilerMessage(diagnostic);
            if (diagnostic.getType() == Diagnostic.ERROR) {
                return errorMessage(diagnostic);
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
        private boolean errorMessage(Diagnostic diagnostic)
        {
            if (! hadError) {
                hadError = true;
                boolean messageShown;

                if (diagnostic.getFileName() == null) {
                    showMessageWithText("compiler-error", diagnostic.getMessage());
                    return true;
                }
                
                String message = diagnostic.getMessage();
                // See if we can help the user a bit more if they've mis-spelt a method:
                if (message.contains("cannot find symbol - method")) {
                    messageShown = showEditorDiagnostic(diagnostic,
                            new MisspeltMethodChecker(message,
                                    (int) diagnostic.getStartColumn(),
                                    (int) diagnostic.getStartLine(),
                                    project));
                } else {
                    messageShown = showEditorDiagnostic(diagnostic, null);
                }
                // Display the error message in the source editor
                if (!messageShown) {
                    showMessageWithText("error-in-file", diagnostic.getFileName() + ":" +
                            diagnostic.getStartLine() + "\n" + message);
                }
                
                return true;
            }
            
            return false;
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
            // Add this message-fragment to, and display, the warning dialog
            bluej.compiler.CompilerWarningDialog.getDialog().addWarningMessage(message);
            
            return true;
        }
        
        @Override
        public void endCompile(File[] sources, boolean successful)
        {
            super.endCompile(sources, successful);
            
            // Display status dialog for accessibility. If chainObserver is set, we assume
            // that the chained observer will fulfill this responsibility instead.
            if (successful && chainObserver == null && PrefMgr.getFlag(PrefMgr.ACCESSIBILITY_SUPPORT)) {
                if (getEditor().isVisible()) {
                    DialogManager.showText(getEditor(), Config.getString("pkgmgr.accessibility.compileDone"));
                }
            }
        }
    }

    // ---- end of bluej.compiler.CompileObserver interfaces ----

    /**
     * closeAllEditors - closes all currently open editors within package Should
     * be run whenever a package is removed from PkgFrame.
     */
    public void closeAllEditors()
    {
        // ToArray has been used here rather than Iterator, to avoid
        // ConcurrentModificationException which happened on closing
        // BlueJ main frame after renaming a class without compile.
        for (Target target : targets.toArray()) {
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

}
