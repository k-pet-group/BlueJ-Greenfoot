package bluej.pkgmgr.target;

import bluej.extensions.BClass;
import bluej.extensions.ExtensionBridge;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import bluej.Config;
import bluej.debugger.DebuggerClass;
import bluej.debugmgr.objectbench.InvokeListener;
import bluej.editor.Editor;
import bluej.editor.EditorManager;
import bluej.extmgr.MenuManager;
import bluej.graph.GraphEditor;
import bluej.graph.Moveable;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.SourceInfo;
import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.dependency.ExtendsDependency;
import bluej.pkgmgr.dependency.ImplementsDependency;
import bluej.pkgmgr.dependency.UsesDependency;
import bluej.pkgmgr.target.role.*;
import bluej.prefmgr.PrefMgr;
import bluej.utility.*;
import bluej.views.ConstructorView;
import bluej.views.MethodView;

/**
 * A class target in a package, i.e. a target that is a class file built from
 * Java source code
 * 
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 * @author Bruce Quig
 * @author Damiano Bolla
 * 
 * @version $Id: ClassTarget.java 3745 2006-01-25 01:11:06Z davmac $
 */
public class ClassTarget extends EditableTarget
    implements Moveable, InvokeListener
{
    final static int MIN_WIDTH = 60;
    final static int MIN_HEIGHT = 30;
    private final static String editStr = Config.getString("pkgmgr.classmenu.edit");
    private final static String compileStr = Config.getString("pkgmgr.classmenu.compile");
    private final static String inspectStr = Config.getString("pkgmgr.classmenu.inspect");
    private final static String removeStr = Config.getString("pkgmgr.classmenu.remove");
    private final static String createTestStr = Config.getString("pkgmgr.classmenu.createTest");

    // Define Background Colours
    private final static Color compbg = Config.getItemColour("colour.target.bg.compiling");

    private final static Color colBorder = Config.getItemColour("colour.target.border");
    private final static Color textfg = Config.getItemColour("colour.text.fg");

    private static String usesArrowMsg = Config.getString("pkgmgr.usesArrowMsg");

    // temporary file name extension to trick windows if changing case only in
    // class name
    private static String TEMP_FILE_EXTENSION = "-temp";

    // the role object represents the changing roles that are class
    // target can have ie changing from applet to an interface etc
    // 'role' should never be null
    // role should be accessed using getRole() and set using
    // setRole(). A role should not contain important state information
    // because role objects are thrown away at a whim.
    private ClassRole role = new StdClassRole();

    // a flag indicating whether an editor, when opened for the first
    // time, should display the interface of this class
    private boolean openWithInterface = false;

    // the set of breakpoints set in this class
    protected List breakpoints = new ArrayList();

    // cached information obtained by parsing the source code
    // automatically becomes invalidated when the source code is
    // edited
    private SourceInfo sourceInfo = new SourceInfo();
    
    // caches whether the class is abstract. Only accurate when the
    // classtarget state is normal (ie. the class is compiled).
    private boolean isAbstract;
    
    /**
     * fields used in Tarjan's algorithm:
     */
    public int dfn, link;

    // flag to prevent recursive calls to analyseDependancies()
    private boolean analysing = false;

    private int ghostX;
    private int ghostY;
    private int ghostWidth;
    private int ghostHeight;
    private boolean isDragging = false;
    private boolean isMoveable = true;
    private boolean hasSource;

    private String typeParameters = "";

    /**
     * Create a new class target in package 'pkg'.
     * 
     * @param pkg Description of the Parameter
     * @param baseName Description of the Parameter
     */
    public ClassTarget(Package pkg, String baseName)
    {
        this(pkg, baseName, null);
    }

    /**
     * Create a new class target in package 'pkg'.
     * 
     * @param pkg Description of the Parameter
     * @param baseName Description of the Parameter
     * @param template Description of the Parameter
     */
    public ClassTarget(Package pkg, String baseName, String template)
    {
        super(pkg, baseName);
        hasSource = getSourceFile().canRead();

        // we can take a guess at what the role is going to be for the
        // object based on the start of the template name. If we get this
        // wrong, its no great shame as it'll be fixed the first time they
        // successfully analyse/compile the source.
        if (template != null) {
            if (template.startsWith("applet")) {
                role = new AppletClassRole();
            }
            else if (template.startsWith("unittest")) {
                role = new UnitTestClassRole();
            }
            else if (template.startsWith("abstract")) {
                role = new AbstractClassRole();
            }
            else if (template.startsWith("interface")) {
                role = new InterfaceClassRole();
            }
            else if (template.startsWith("enum")) {
                role = new EnumClassRole();
            }
        }
        setGhostPosition(0, 0);
        setGhostSize(0, 0);
    }

    private BClass singleBClass;  // Every Target has none or one BClass
    
    /**
     * Return the extensions BProject associated with this Project.
     * There should be only one BProject object associated with each Project.
     * @return the BProject associated with this Project.
     */
    public synchronized final BClass getBClass ()
    {
        if ( singleBClass == null )
          singleBClass = ExtensionBridge.newBClass(this);
          
        return singleBClass;
    }


    /**
     * Return the target's name, including the package name. eg.
     * bluej.pkgmgr.Target
     * 
     * @return The qualifiedName value
     */
    public String getQualifiedName()
    {
        return getPackage().getQualifiedName(getBaseName());
    }

    /**
     * Return the target's base name (ie the name without the package name). eg.
     * Target
     * 
     * @return The baseName value
     */
    public String getBaseName()
    {
        return getIdentifierName();
    }

    /**
     * Return information about the source of this class.
     * 
     * @return The source info object.
     */
    public SourceInfo getSourceInfo()
    {
        return sourceInfo;
    }

    /**
     * Returns the text which the target is displaying as its label. For normal
     * classes this is just the identifier name. For generic classes the generic
     * parameters are shown as well
     * 
     * @return The displayName value
     */
    public String getDisplayName()
    {
        return super.getDisplayName() + getTypeParameters();
    }

    /**
     * Returns the type parameters for a generic class as declared in the source
     * file.
     * 
     * @return The typeParameters value
     */
    private String getTypeParameters()
    {
        return typeParameters;
    }

    /**
     * Change the state of this target. The target will be repainted to show the
     * new state.
     * 
     * @param newState The new state value
     */
    public void setState(int newState)
    {
        getPackage().getProject().removeInspectorInstance(getQualifiedName());
        super.setState(newState);
    }

    /**
     * Return the role object for this class target.
     * 
     * @return The role value
     */
    public ClassRole getRole()
    {
        return role;
    }

    /**
     * Set the role for this class target.
     * 
     * Avoids changing over the role object if the new one is of the same type
     * 
     * @param newRole The new role value
     */
    protected void setRole(ClassRole newRole)
    {
        if ((role == null) || !(newRole.getClass().equals(role.getClass()))) {
            role = newRole;
        }
    }

    /**
     * Use a variety of tests to determine what our role is.
     * 
     * All tests must be very quick and should not rely on any significant
     * computation (ie. reparsing). If computation is required, the existing
     * role will do for the time being.
     * 
     * @param cl Description of the Parameter
     */
    public void determineRole(Class cl)
    {
        isAbstract = false;
        
        if (cl != null) {
            isAbstract = Modifier.isAbstract(cl.getModifiers());
            
            ClassLoader clLoader = cl.getClassLoader();
            Class junitClass = null;
            Class appletClass = null;

            // It shouldn't ever be the case that the class is on the bootstrap
            // class path (and was loaded by the bootstrap class loader), unless
            // someone has done something rather strange - but it has happened;
            // see bug # 1017.

            if (clLoader != null) {
                try {
                    junitClass = clLoader.loadClass("junit.framework.TestCase");
                }
                catch (ClassNotFoundException cnfe) {}
                catch (LinkageError le) {}

                try {
                    appletClass = clLoader.loadClass("java.applet.Applet");
                }
                catch (ClassNotFoundException cnfe) {}
                catch (LinkageError le) {}
            }
            
            if (junitClass == null) {
                junitClass = junit.framework.TestCase.class;
            }
            
            if (appletClass == null) {
                appletClass = java.applet.Applet.class;
            }
            
            // As cl is non-null, it is the definitive information
            // source ie. if it thinks its an applet who are we to argue
            // with it.
            if (appletClass.isAssignableFrom(cl)) {
                setRole(new AppletClassRole());
            }
            else if (junitClass.isAssignableFrom(cl)) {
                setRole(new UnitTestClassRole());
            }
            else if (Modifier.isInterface(cl.getModifiers())) {
                setRole(new InterfaceClassRole());
            }
            else if (JavaUtils.getJavaUtils().isEnum(cl)) {
                setRole(new EnumClassRole());
            }
            else if (isAbstract) {
                setRole(new AbstractClassRole());
            }
            else {
                setRole(new StdClassRole());
            }
        }
        else {
            // try the parsed source code
            ClassInfo classInfo = sourceInfo.getInfoIfAvailable();

            if (classInfo != null) {
                if (classInfo.isApplet()) {
                    setRole(new AppletClassRole());
                }
                else if (classInfo.isUnitTest()) {
                    setRole(new UnitTestClassRole());
                }
                else if (classInfo.isInterface()) {
                    setRole(new InterfaceClassRole());
                }
                else if (classInfo.isEnum()) {
                    setRole(new EnumClassRole());
                }
                else if (classInfo.isAbstract()) {
                    setRole(new AbstractClassRole());
                }
                else {
                    // We shouldn't override applet/unit test class roles based only
                    // on source analysis: if they inherit only indirectly from Applet
                    // or UnitTest, source analysis won't give the correct role
                    if (! (role instanceof AppletClassRole) &&
                            ! (role instanceof UnitTestClassRole))
                    {
                        setRole(new StdClassRole());
                    }
                }
            }
            // If no information gained from parsing the file (classInfo = null),
            // then we don't really know the role: let's leave it as it was
        }
    }

    /**
     * Load existing information about this class target
     * 
     * 
     * @param props the properties object to read
     * @param prefix an internal name used for this target to identify its
     *            properties in a properties file used by multiple targets.
     * @exception NumberFormatException Description of the Exception
     */
    public void load(Properties props, String prefix)
        throws NumberFormatException
    {
        super.load(props, prefix);

        // try to determine if any role was set when we saved
        // the class target. Be careful here as if you add role types
        // you need to add them here as well.
        String type = props.getProperty(prefix + ".type");

        String intf = props.getProperty(prefix + ".showInterface");
        openWithInterface = Boolean.valueOf(intf).booleanValue();

        if (AppletClassRole.APPLET_ROLE_NAME.equals(type)) {
            setRole(new AppletClassRole());
        }
        else if (UnitTestClassRole.UNITTEST_ROLE_NAME.equals(type)) {
            setRole(new UnitTestClassRole());
        }
        else if (AbstractClassRole.ABSTRACT_ROLE_NAME.equals(type)) {
            setRole(new AbstractClassRole());
        }
        else if (InterfaceClassRole.INTERFACE_ROLE_NAME.equals(type)) {
            setRole(new InterfaceClassRole());
        }
        else if (EnumClassRole.ENUM_ROLE_NAME.equals(type)) {
            setRole(new EnumClassRole());
        }

        getRole().load(props, prefix);
    }

    /**
     * Save information about this class target
     * 
     * 
     * @param props the properties object to save to
     * @param prefix an internal name used for this target to identify its
     *            properties in a properties file used by multiple targets.
     */
    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);

        if (getRole().getRoleName() != null) {
            props.put(prefix + ".type", getRole().getRoleName());
        }

        if (editorOpen()) {
            openWithInterface = getEditor().isShowingInterface();
        }
        props.put(prefix + ".showInterface", new Boolean(openWithInterface).toString());

        getRole().save(props, 0, prefix);
    }

    /**
     * Check if the compiled class and the source are up to date.
     * (Specifically, check if a recompilation is needed. This will
     * always be false if the target has no source).
     * 
     * @return true if they are in sync otherwise false.
     */
    public boolean upToDate()
    {
        // check if the class file is up to date
        File src = getSourceFile();
        File clss = getClassFile();

        // if just a .class file with no src, it better be up to date
        if (!hasSourceCode()) {
            return true;
        }

        if (!clss.exists() || (src.exists() && (src.lastModified() > clss.lastModified()))) {
            return false;
        }

        return true;
    }

    /**
     * Mark this class as modified, and mark all dependent classes too
     */
    public void invalidate()
    {
        setState(S_INVALID);

        for (Iterator it = dependents(); it.hasNext();) {
            Dependency d = (Dependency) it.next();
            Target dependent = d.getFrom();
            dependent.setState(S_INVALID);
        }
    }

    /**
     * Verify whether this class target is an interface class
     * 
     * 
     * @return true if class target is an interface class, else returns false
     */
    public boolean isInterface()
    {
        return (getRole() instanceof InterfaceClassRole);
    }

    /**
     * Verify whether this class target is an unit test class
     * 
     * 
     * @return true if class target is a unit test class, else returns false
     */
    public boolean isUnitTest()
    {
        return (getRole() instanceof UnitTestClassRole);
    }

    /**
     * Check whether this class target represents an abstract class. This
     * can be true regardless of the role (unit test, applet, standard class).
     * 
     * The return is only valid if isCompiled() is true.
     */
    public boolean isAbstract()
    {
        return isAbstract;
    }
    
    // --- Target interface ---

    /**
     * Gets the backgroundColour attribute of the ClassTarget object
     * 
     * @return The backgroundColour value
     */
    Color getBackgroundColour()
    {
        if (state == S_COMPILING) {
            return compbg;
        }
        else {
            return getRole().getBackgroundColour();
        }
    }

    /**
     * Gets the borderColour attribute of the ClassTarget object
     * 
     * @return The borderColour value
     */
    Color getBorderColour()
    {
        return colBorder;
    }

    /**
     * Gets the textColour attribute of the ClassTarget object
     * 
     * @return The textColour value
     */
    Color getTextColour()
    {
        return textfg;
    }

    /**
     * Gets the font attribute of the ClassTarget object
     * 
     * @return The font value
     */
    Font getFont()
    {
        return PrefMgr.getTargetFont();
    }

    // --- EditableTarget interface ---

    /**
     * Tell whether we have access to the source for this class.
     * 
     * @return Description of the Return Value
     */
    public boolean hasSourceCode()
    {
        return hasSource;
    }

    /**
     * @return the name of the (text) file this target corresponds to.
     */
    public File getSourceFile()
    {
        return new File(getPackage().getPath(), getBaseName() + ".java");
    }

    /**
     * @return the name of the context(.ctxt) file this target corresponds to.
     */
    public File getContextFile()
    {
        return new File(getPackage().getPath(), getBaseName() + ".ctxt");
    }

    /**
     * @return the name of the class (.class) file this target corresponds to.
     */
    public File getClassFile()
    {
        return new File(getPackage().getPath(), getBaseName() + ".class");
    }

    /**
     * @return a FileFilter for all inner class files of this target
     */
    public FileFilter getInnerClassFiles()
    {
        return new InnerClassFileFilter();
    }

    /**
     * Description of the Class
     */
    class InnerClassFileFilter
        implements FileFilter
    {
        /**
         * Description of the Method
         * 
         * @param pathname Description of the Parameter
         * @return Description of the Return Value
         */
        public boolean accept(File pathname)
        {
            return pathname.getName().startsWith(getBaseName() + "$");
        }
    }

    /**
     * Get the editor associated with this class.
     * @return the editor object associated with this target. May be null if
     *         there was a problem opening this editor.
     */
    public Editor getEditor()
    {
        return getEditor(openWithInterface);
    }

    /**
     * Get an editor for this class, either in source view or interface view.
     * 
     * @param showInterface Determine whether to show interface view or 
     *         source view in the editor.
     * @return the editor object associated with this target. May be null if
     *         there was a problem opening this editor.
     */
    private Editor getEditor(boolean showInterface)
    {
        // ClassTarget must have source code if it is to provide an editor
        if (editor == null && hasSourceCode()) {
            String filename = getSourceFile().getPath();
            String docFilename = getPackage().getProject().getDocumentationFile(filename);
            editor = EditorManager.getEditorManager().openClass(filename, docFilename, getBaseName(), this,
                    isCompiled(), breakpoints, getPackage().getProject().getClassLoader(), editorBounds);
            editor.showInterface(showInterface);
        }
        return editor;
    }

    /**
     * Ensure that the source file of this class is up-to-date (i.e.
     * that any possible unsaved changes in an open editor window are 
     * saved).
     */
    public void ensureSaved() throws IOException
    {
        if(editor != null) {
            editor.save();
        }
    }
    
    // --- end of EditableTarget interface ---

    // --- user interface function implementation ---

    /**
     */
    private void inspect()
    {
        new Thread() {
            
            int state = 0;
            DebuggerClass clss;
            
            public void run() {
                switch (state) {
                    // This is the intial state. Try and load the class.
                    case 0:
                        try {
                            clss = getPackage().getDebugger().getClass(getQualifiedName());
                            state = 1;
                            EventQueue.invokeLater(this);
                        }
                        catch (ClassNotFoundException cnfe) {}
                        break;
                
                    // Once this state is reached, we're running on the Swing event queue.
                    case 1:
                        getPackage().getProject().getClassInspectorInstance(clss, getPackage(), PkgMgrFrame.findFrame(getPackage()));
                }
            }
        }.start();
    }

    // --- EditorWatcher interface ---

    public void modificationEvent(Editor editor)
    {
        invalidate();
        removeBreakpoints();
        sourceInfo.setSourceModified();
    }

    public void saveEvent(Editor editor)
    {
        ClassInfo info = analyseSource();
        if (info != null) {
            updateTargetFile(info);
        }
        determineRole(null);
    }

    public String breakpointToggleEvent(Editor editor, int lineNo, boolean set)
    {
        if (isCompiled()) {
            return getPackage().getDebugger().toggleBreakpoint(getQualifiedName(), lineNo, set);
        }
        else {
            return Config.getString("pkgmgr.breakpointMsg");
        }
    }

    // --- end of EditorWatcher interface ---

    /**
     * Description of the Method
     */
    public void removeBreakpoints()
    {
        if (editor != null) {
            editor.removeBreakpoints();
        }
    }

    /**
     * Description of the Method
     */
    public void removeStepMark()
    {
        if (editor != null) {
            editor.removeStepMark();
        }
    }

    /**
     * Gets the compiled attribute of the ClassTarget object
     * 
     * @return The compiled value
     */
    public boolean isCompiled()
    {
        return (state == S_NORMAL);
    }

    /**
     * Description of the Method
     * 
     * @param editor Description of the Parameter
     */
    public void compile(Editor editor)
    {
        getPackage().compile(this);
    }

    /**
     * Called when this class target has just been successfully compiled.
     * 
     * We load the compiled class if possible and check it the compilation has
     * resulted in it taking a different role (ie abstract to applet)
     */
    public void endCompile()
    {
        Class cl = getPackage().loadClass(getQualifiedName());

        determineRole(cl);
    }

    /**
     * generates a source code skeleton for this class
     */
    public boolean generateSkeleton(String template)
    {
        // delegate to role object
        if (template == null) {
            Debug.reportError("generate class skeleton error");
            return false;
        }
        else {
            boolean success = role.generateSkeleton(template, getPackage(), getBaseName(), getSourceFile().getPath());
            if (success) {
                // skeleton successfully generated
                setState(Target.S_INVALID);
                hasSource = true;
                return true;
            }
            return false;
        }
    }

    /**
     * Description of the Method
     * 
     * @param packageName Description of the Parameter
     * @exception IOException Description of the Exception
     */
    public void enforcePackage(String packageName)
        throws IOException
    {
        if (!JavaNames.isQualifiedIdentifier(packageName)) {
            throw new IllegalArgumentException();
        }

        ClassInfo info = sourceInfo.getInfo(getSourceFile(), getPackage());
        if (info == null) {
            return;
        }

        // We may or may not need to change each of the semi colon selection text,
        // package name selection text, and package statement selection text.
        String semiReplacement = null;
        String nameReplacement = null;
        String pkgStatementReplacement = null;
        
        // Figure out if we need to change anything, and if so, what:
        if (packageName.length() == 0) {
            if (info.hasPackageStatement()) {
                // we must delete all parts of the "package" statement
                semiReplacement = "";
                nameReplacement = "";
                pkgStatementReplacement = "";
            }
            else {
                // if we have no package statement we do not need
                // to do anything to turn it into an anonymous package
                return;
            }
        }
        else {
            if (info.hasPackageStatement()) {
                // it is trivial to make the package name the same
                if (info.getPackage().equals(packageName)) {
                    return;
                }
                // we must change just the package name
                nameReplacement = packageName;
            }
            else {
                // we must insert all the "package" statement
                semiReplacement = ";\n\n";
                nameReplacement = packageName;
                pkgStatementReplacement = "package ";
            }
        }

        // Change the relevant parts of the file
        FileEditor fed = new FileEditor(getSourceFile());

        if (semiReplacement != null) {
            Selection selSemi = info.getPackageSemiSelection();
            fed.replaceSelection(selSemi, semiReplacement);
        }
        
        if (nameReplacement != null) {
            Selection selName = info.getPackageNameSelection();
            fed.replaceSelection(selName, nameReplacement);
        }

        if (pkgStatementReplacement != null) {
            Selection selStatement = info.getPackageStatementSelection();
            fed.replaceSelection(selStatement, pkgStatementReplacement);
        }

        // save changes back to disk
        fed.save();
    }

    /**
     * Analyse the source code, and save retrieived information.
     */
    public ClassInfo analyseSource()
    {
        if (analysing) {
            return null;
        }

        analysing = true;

        ClassInfo info = sourceInfo.getInfo(getSourceFile(), getPackage());

        // info will be null if the source was unparseable
        if (info != null) {
            // the following may update the package display but it
            // will not modify the classes source code
            determineRole(null);
            setTypeParameters(info);
            analyseDependencies(info);
        }

        // getPackage().repaint();

        analysing = false;
        return info;
    }
    
    /**
     * Change file name and package to match that found in the source file.
     * @param info  The information from source analysis
     */
    private void updateTargetFile(ClassInfo info)
    {
        if (analyseClassName(info)) {
            if (nameEqualsIgnoreCase(info.getName())) {
                // this means file has same name but different case
                // to trick Windows OS to do a name change we need to
                // rename to temp name and then rename to desired name
                doClassNameChange(info.getName() + TEMP_FILE_EXTENSION);
            }
            doClassNameChange(info.getName());
        }
        if (analysePackageName(info)) {
            doPackageNameChange(info.getPackage());
        }
    }

    /**
     * Sets the typeParameters attribute of the ClassTarget object
     * 
     * @param info The new typeParameters value
     */
    public void setTypeParameters(ClassInfo info)
    {
        String newTypeParameters = "";
        if (info.hasTypeParameter()) {
            Iterator i = info.getTypeParameterTexts().iterator();
            newTypeParameters = "<" + i.next();
           
            while (i.hasNext()) {
                newTypeParameters += "," + i.next();
            }
            newTypeParameters += ">";
        }
        if (!newTypeParameters.equals(typeParameters)) {
            typeParameters = newTypeParameters;
            updateSize();
        }
    }

    /**
     * Analyses class name of Classtarget with that of parsed src file. Aim is
     * to detect any textual changes of class name and modify resources to suit
     * 
     * 
     * @param info contains parsed class information
     * @return true if class name is different
     */
    public boolean analyseClassName(ClassInfo info)
    {
        String newName = info.getName();

        if ((newName == null) || (newName.length() == 0)) {
            return false;
        }

        return (!getBaseName().equals(newName));
    }

    /**
     * Check whether the package name has been changed by comparing the package
     * name in the information from the parser with the current package name
     */
    public boolean analysePackageName(ClassInfo info)
    {
        String newName = info.getPackage();

        return (!getPackage().getQualifiedName().equals(newName));
    }

    /**
     * Analyse the current dependencies in the source code and update the
     * dependencies in the graphical display accordingly.
     * 
     * @param info Description of the Parameter
     */
    public void analyseDependencies(ClassInfo info)
    {
        // currently we don't remove uses dependencies, but just warn

        //removeAllOutDependencies();
        removeInheritDependencies();
        unflagAllOutDependencies();

        // handle superclass dependency
        if (info.getSuperclass() != null) {
            DependentTarget superclass = getPackage().getDependentTarget(info.getSuperclass());
            if (superclass != null) {
                getPackage().addDependency(new ExtendsDependency(getPackage(), this, superclass), false);
            }
        }

        // handle implemented interfaces
        List vect = info.getImplements();
        for (Iterator it = vect.iterator(); it.hasNext();) {
            String name = (String) it.next();
            DependentTarget interfce = getPackage().getDependentTarget(name);

            if (interfce != null) {
                getPackage().addDependency(new ImplementsDependency(getPackage(), this, interfce), false);
            }
        }

        // handle used classes
        vect = info.getUsed();
        for (Iterator it = vect.iterator(); it.hasNext();) {
            String name = (String) it.next();
            DependentTarget used = getPackage().getDependentTarget(name);
            if (used != null) {
                if (used.getAssociation() == this || this.getAssociation() == used) {
                    continue;
                }

                getPackage().addDependency(new UsesDependency(getPackage(), this, used), true);

            }
        }

        // check for inconsistent use dependencies
        for (Iterator it = usesDependencies(); it.hasNext();) {
            UsesDependency usesDep = ((UsesDependency) it.next());
            if (!usesDep.isFlagged()) {
                getPackage().setStatus(usesArrowMsg + usesDep);
            }
        }
    }

    /**
     * Check to see that name has not changed. If name has changed then update
     * details. Return true if the name has changed.
     * 
     * @param newName Description of the Parameter
     * @return Description of the Return Value
     */
    private boolean doClassNameChange(String newName)
    {
        //need to check that class does not already exist
        if (getPackage().getTarget(newName) != null) {
            getPackage().showError("duplicate-name");
            return false;
        }

        File newSourceFile = new File(getPackage().getPath(), newName + ".java");
        File oldSourceFile = getSourceFile();

        if (FileUtility.copyFile(oldSourceFile, newSourceFile)) {

            getPackage().updateTargetIdentifier(this, getIdentifierName(), newName);
            getEditor().changeName(newName, newSourceFile.getPath());

            role.prepareFilesForRemoval(this, oldSourceFile.getPath(), getClassFile().getPath(), getContextFile()
                    .getPath());

            // this is extremely dangerous code here.. must track all
            // variables which are set when ClassTarget is first
            // constructed and fix them up for new class name
            setIdentifierName(newName);
            setDisplayName(newName);
            updateSize();

            return true;
        }
        return false;
    }

    /**
     * Checks for ClassTarget name equality if case is ignored.
     * 
     * 
     * @param newName
     * @return true if name is equal ignoring case.
     */
    private boolean nameEqualsIgnoreCase(String newName)
    {
        return (getBaseName().equalsIgnoreCase(newName));
    }

    /**
     * Change the package of a class target to something else.
     * 
     * 
     * @param newName the new fully qualified package name
     */
    private void doPackageNameChange(String newName)
    {
        Project proj = getPackage().getProject();

        Package dstPkg = proj.getPackage(newName);

        if (dstPkg == null) {
            DialogManager.showError(null, "package-name-invalid");
        }
        else {
            // fix for bug #382. Potentially could clash with a package
            // in the destination package with the same name
            if (dstPkg.getTarget(getBaseName()) != null) {
                DialogManager.showError(null, "package-name-clash");
                // fall through to enforcePackage, below.
            }
            else if (DialogManager.askQuestion(null, "package-name-changed") == 0) {
                dstPkg.importFile(getSourceFile());
                prepareForRemoval();
                getPackage().removeTarget(this);
                close();
                return;
            }
        }

        // all non working paths lead here.. lets fix the package line
        // up so it is back to what we expect
        try {
            enforcePackage(getPackage().getQualifiedName());
            getEditor().reloadFile();
        }
        catch (IOException ioe) {}
    }

    /**
     * Resizes the class so the entire classname + type parameter are visible
     *  
     */
    private void updateSize()
    {
        int width = calculateWidth(getDisplayName());
        setSize(width, getHeight());
        repaint();
    }

    /**
     * Construct a popup menu for the class target, including caching of
     * results.
     */
    protected JPopupMenu menu = null;
    boolean compiledMenu = false;

    /**
     * Post the context menu for this target.
     * 
     * @param x  the x coordinate for the menu, relative to graph editor
     * @param y  the y coordinate for the menu, relative to graph editor
     */
    public void popupMenu(int x, int y)
    {
        Class cl = null;
        GraphEditor editor = getPackage().getEditor();

        if (state == S_NORMAL) {
            // handle error causes when loading 1.4 compiled classes
            // on a 1.3 VM
            // we detect the error, remove the class file, and invalidate
            // to allow them to be recompiled
            cl = getPackage().loadClass(getQualifiedName());
            if (cl == null) {
                // trouble loading the class
                // remove the class file and invalidate the target
                if (hasSourceCode()) {
                    getClassFile().delete();
                    invalidate();
                }
            }
        }

        if (menu != null) {
            editor.remove(menu);
        }

        // check that the class loading hasn't changed out state
        if (state == S_NORMAL) {
            menu = createMenu(cl);
            editor.add(menu);
        }
        else {
            menu = createMenu(null);
            editor.add(menu);
        }

        if (menu != null) {
            menu.show(editor, x, y);
        }
    }

    /**
     * Creates a popup menu for this class target.
     * 
     * 
     * @param cl class object associated with this class target
     * @return the created popup menu object
     */
    protected JPopupMenu createMenu(Class cl)
    {
        JPopupMenu menu = new JPopupMenu(getBaseName() + " operations");

        // call on role object to add any options needed at top
        role.createRoleMenu(menu, this, cl, state);

        if (cl != null) {
            if (role.createClassConstructorMenu(menu, this, cl)) {
                menu.addSeparator();
            }
        }

        if (cl != null) {
            if (role.createClassStaticMenu(menu, this, cl)) {
                menu.addSeparator();
            }
        }
        role.addMenuItem(menu, new EditAction(), hasSourceCode());
        role.addMenuItem(menu, new CompileAction(), hasSourceCode());
        role.addMenuItem(menu, new InspectAction(), cl != null);
        role.addMenuItem(menu, new RemoveAction(), true);

        // call on role object to add any options needed at bottom
        role.createRoleMenuEnd(menu, this, state);

        MenuManager menuManager = new MenuManager(menu);
        menuManager.setAttachedObject(this);
        menuManager.addExtensionMenu(getPackage().getProject());

        return menu;
    }

    /**
     * Description of the Class
     */
    public class CreateTestAction extends AbstractAction
    {
        /**
         * Constructor for the CreateTestAction object
         */
        public CreateTestAction()
        {
            putValue(NAME, createTestStr);
        }

        /**
         * Description of the Method
         * 
         * @param e Description of the Parameter
         */
        public void actionPerformed(ActionEvent e)
        {
            PkgMgrFrame pmf = PkgMgrFrame.findFrame(getPackage());

            if (pmf != null) {
                String testClassName = getIdentifierName() + "Test";
                pmf.createNewClass(testClassName, "unittest", true);
                // we want to check that the previous called actually
                // created a unit test class as a name clash with an existing
                // class would not. This prevents a non unit test becoming
                // associated with a class unintentionally
                Target target = getPackage().getTarget(testClassName);
                ClassTarget ct = null;
                if (target instanceof ClassTarget) {
                    ct = (ClassTarget) target;
                    if (ct != null && ct.isUnitTest()) {
                        setAssociation((DependentTarget) getPackage().getTarget(getIdentifierName() + "Test"));
                    }
                }
                updateAssociatePosition();
                getPackage().getEditor().revalidate();
                getPackage().getEditor().repaint();

            }
        }
    }

    /**
     * Description of the Class
     */
    private class EditAction extends AbstractAction
    {
        /**
         * Constructor for the EditAction object
         */
        public EditAction()
        {
            putValue(NAME, editStr);
        }

        /**
         * Description of the Method
         * 
         * @param e Description of the Parameter
         */
        public void actionPerformed(ActionEvent e)
        {
            open();
        }
    }

    /**
     * Description of the Class
     */
    private class CompileAction extends AbstractAction
    {
        /**
         * Constructor for the CompileAction object
         */
        public CompileAction()
        {
            putValue(NAME, compileStr);
        }

        /**
         * Description of the Method
         * 
         * @param e Description of the Parameter
         */
        public void actionPerformed(ActionEvent e)
        {
            getPackage().compile(ClassTarget.this);
        }
    }

    /**
     * Description of the Class
     */
    private class RemoveAction extends AbstractAction
    {
        /**
         * Constructor for the RemoveAction object
         */
        public RemoveAction()
        {
            putValue(NAME, removeStr);
        }

        /**
         * Description of the Method
         * 
         * @param e Description of the Parameter
         */
        public void actionPerformed(ActionEvent e)
        {
            PkgMgrFrame pmf = PkgMgrFrame.findFrame(getPackage());
            if (pmf.askRemoveClass()) {
                getPackage().getEditor().raiseRemoveTargetEvent(ClassTarget.this);
            }
        }
    }

    /**
     * Description of the Class
     */
    private class InspectAction extends AbstractAction
    {
        /**
         * Constructor for the InspectAction object
         */
        public InspectAction()
        {
            putValue(NAME, inspectStr);
        }

        /**
         * Description of the Method
         * 
         * @param e Description of the Parameter
         */
        public void actionPerformed(ActionEvent e)
        {
            inspect();
        }
    }

    /**
     * Process a double click on this target. That is: open its editor.
     * 
     * @param evt Description of the Parameter
     */
    public void doubleClick(MouseEvent evt)
    {
        open();
    }

    /**
     * @return Returns the ghostX.
     */
    public int getGhostX()
    {
        return ghostX;
    }

    /**
     * @return Returns the ghostX.
     */
    public int getGhostY()
    {
        return ghostY;
    }

    /**
     * @return Returns the ghostX.
     */
    public int getGhostWidth()
    {
        return ghostWidth;
    }

    /**
     * @return Returns the ghostX.
     */
    public int getGhostHeight()
    {
        return ghostHeight;
    }

    /**
     * Set the position of the ghost image given a delta to the real size.
     * 
     * @param deltaX The new ghostPosition value
     * @param deltaY The new ghostPosition value
     */
    public void setGhostPosition(int deltaX, int deltaY)
    {
        this.ghostX = getX() + deltaX;
        this.ghostY = getY() + deltaY;
    }

    /**
     * Set the size of the ghost image given a delta to the real size.
     * 
     * @param deltaX The new ghostSize value
     * @param deltaY The new ghostSize value
     */
    public void setGhostSize(int deltaX, int deltaY)
    {
        ghostWidth = Math.max(getWidth() + deltaX, MIN_WIDTH);
        ghostHeight = Math.max(getHeight() + deltaY, MIN_HEIGHT);
    }

    /**
     * Set the target's position to its ghost position.
     */
    public void setPositionToGhost()
    {
        super.setPos(ghostX, ghostY);
        setSize(ghostWidth, ghostHeight);
        isDragging = false;
    }

    /**
     * Ask whether we are currently dragging.
     * 
     * @return The dragging value
     */
    public boolean isDragging()
    {
        return isDragging;
    }

    /**
     * Set whether or not we are currently dragging this class (either moving or
     * resizing).
     * 
     * @param isDragging The new dragging value
     */
    public void setDragging(boolean isDragging)
    {
        this.isDragging = isDragging;
    }

    /**
     * Set the position of this target.
     * 
     * @param x The new pos value
     * @param y The new pos value
     */
    public void setPos(int x, int y)
    {
        super.setPos(x, y);
        setGhostPosition(0, 0);
    }

    /**
     * Set the size of this target.
     * 
     * @param width The new size value
     * @param height The new size value
     */
    public void setSize(int width, int height)
    {
        super.setSize(Math.max(width, MIN_WIDTH), Math.max(height, MIN_HEIGHT));
        setGhostSize(0, 0);
    }

    /**
     * Prepares this ClassTarget for removal from a Package. It removes
     * dependency arrows and calls prepareFilesForRemoval() to remove applicable
     * files.
     *  
     */
    private void prepareForRemoval()
    {
        if (editor != null) {
            editor.close();
        }

        // if this target is the assocation for another Target, remove
        // the association
        Iterator it = getPackage().getVertices();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof DependentTarget) {
                DependentTarget d = (DependentTarget) o;
                if (this.equals(d.getAssociation())) {
                    d.setAssociation(null);
                }
            }
        }

        // flag dependent Targets as invalid
        invalidate();

        removeAllInDependencies();
        removeAllOutDependencies();

        // remove associated files (.class, .java and .ctxt)
        prepareFilesForRemoval();
    }

    /**
     * Removes applicable files (.class, .java and .ctxt) prior to this
     * ClassTarget being removed from a Package.
     */
    public void prepareFilesForRemoval()
    {
        if (getSourceFile().exists()) {
            // remove all inner class files starting with the same name as
            // sourceFile$
            File[] files = getPackage().getPath().listFiles(getInnerClassFiles());

            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    files[i].delete();
                }
            }
        }

        getRole().prepareFilesForRemoval(this, getSourceFile().getPath(), getClassFile().getPath(),
                getContextFile().getPath());
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.editor.EditorWatcher#generateDoc()
     */
    /**
     * Description of the Method
     */
    public void generateDoc()
    {
        getPackage().generateDocumentation(this);
    }

    /**
     * Description of the Method
     */
    public void remove()
    {
        prepareForRemoval();
        getPackage().removeClass(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.graph.Moveable#isMoveable()
     */
    /**
     * Gets the moveable attribute of the ClassTarget object
     * 
     * @return The moveable value
     */
    public boolean isMoveable()
    {
        return isMoveable;
    }

    /*
     * Set whether this ClassTarget can be moved by the user (dragged around).
     * This is set false for unit tests which are assosciated with another class.
     * 
     * @see bluej.graph.Moveable#setIsMoveable(boolean)
     */
    public void setIsMoveable(boolean isMoveable)
    {
        this.isMoveable = isMoveable;
    }
    
    /**
     * perform interactive method call
     */
    public void executeMethod(MethodView mv)
    {
        getPackage().getEditor().raiseMethodCallEvent(this, mv);
    }
    
    /**
     * interactive constructor call
     */
    public void callConstructor(ConstructorView cv)
    {
        getPackage().getEditor().raiseMethodCallEvent(this, cv);
    }
}