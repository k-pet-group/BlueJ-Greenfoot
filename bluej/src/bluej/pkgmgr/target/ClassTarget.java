/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr.target;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.compiler.CompileObserver;
import bluej.compiler.Diagnostic;
import bluej.debugger.DebuggerClass;
import bluej.debugger.gentype.Reflective;
import bluej.debugmgr.objectbench.InvokeListener;
import bluej.editor.Editor;
import bluej.editor.EditorManager;
import bluej.extensions.BClass;
import bluej.extensions.BClassTarget;
import bluej.extensions.BDependency;
import bluej.extensions.ExtensionBridge;
import bluej.extensions.event.ClassEvent;
import bluej.extensions.event.ClassTargetEvent;
import bluej.extmgr.ClassExtensionMenu;
import bluej.extmgr.ExtensionsManager;
import bluej.extmgr.MenuManager;
import bluej.graph.GraphEditor;
import bluej.graph.Moveable;
import bluej.graph.Vertex;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.PackageResolver;
import bluej.parser.entity.ParsedReflective;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedTypeNode;
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
import bluej.pkgmgr.target.role.AbstractClassRole;
import bluej.pkgmgr.target.role.AppletClassRole;
import bluej.pkgmgr.target.role.ClassRole;
import bluej.pkgmgr.target.role.EnumClassRole;
import bluej.pkgmgr.target.role.InterfaceClassRole;
import bluej.pkgmgr.target.role.MIDletClassRole;
import bluej.pkgmgr.target.role.StdClassRole;
import bluej.pkgmgr.target.role.UnitTestClassRole;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileEditor;
import bluej.utility.FileUtility;
import bluej.utility.JavaNames;
import bluej.utility.JavaReflective;
import bluej.utility.JavaUtils;
import bluej.views.ConstructorView;
import bluej.views.MethodView;

/**
 * A class target in a package, i.e. a target that is a class file built from
 * Java source code
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 * @author Bruce Quig
 */
public class ClassTarget extends DependentTarget
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
    private final static Color compbg = new Color(200,150,100);

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

    // cached information obtained by parsing the source code
    // automatically becomes invalidated when the source code is
    // edited
    private SourceInfo sourceInfo = new SourceInfo();
    
    // caches whether the class is abstract. Only accurate when the
    // classtarget state is normal (ie. the class is compiled).
    private boolean isAbstract;
    
    // a flag indicating whether an editor should have the naviview expanded/collapsed
    private Boolean isNaviviewExpanded=null;

    // flag to prevent recursive calls to analyseDependancies()
    private boolean analysing = false;

    private int ghostX;
    private int ghostY;
    private int ghostWidth;
    private int ghostHeight;
    private boolean isDragging = false;
    private boolean isMoveable = true;
    private boolean hasSource;
    
    // Whether the source has been modified since it was last compiled. This
    // starts off as "true", which is a lie, but it prevents setting breakpoints
    // in an initially uncompiled class.
    private boolean modifiedSinceCompile = true;

    private String typeParameters = "";
    
    //properties map to store values used in the editor from the props (if necessary)
    private Map<String,String> properties = new HashMap<String,String>();
    
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
                role = new UnitTestClassRole(true);
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
            else if (template.startsWith("midlet")) {
                role = new MIDletClassRole();
            }            
        }
        setGhostPosition(0, 0);
        setGhostSize(0, 0);
    }

    private BClass singleBClass;  // Every Target has none or one BClass
    private BClassTarget singleBClassTarget; // Every Target has none or one BClassTarget
    
    /**
     * Return the extensions BProject associated with this Project.
     * There should be only one BProject object associated with each Project.
     * @return the BProject associated with this Project.
     */
    public synchronized final BClass getBClass ()
    {
        if ( singleBClass == null ) {
            singleBClass = ExtensionBridge.newBClass(this);
        }
          
        return singleBClass;
    }

    /**
     * Returns the {@link BClassTarget} associated with this {@link ClassTarget}
     * . There should be only one {@link BClassTarget} object associated with
     * each {@link ClassTarget}.
     * 
     * @return The {@link BClassTarget} associated with this {@link ClassTarget}.
     */
    public synchronized final BClassTarget getBClassTarget()
    {
        if (singleBClassTarget == null) {
            singleBClassTarget = ExtensionBridge.newBClassTarget(this);
        }

        return singleBClassTarget;
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
     * Get a reflective for the type represented by this target.
     * 
     * @return  A suitable reflective, or null.
     */
    public Reflective getTypeRefelective()
    {
        // If compiled, return a reflective based on actual reflection
        if (isCompiled()) {
            Class<?> cl = getPackage().loadClass(getQualifiedName());
            if (cl != null) {
                return new JavaReflective(cl);
            }
            else {
                return null;
            }
        }
        
        // Not compiled; try to get a reflective from the parser
        ParsedCUNode node = null;
        getEditor();
        if (editor != null) {
            node = editor.getParsedNode();
        }
        
        if (node != null) {
            ParsedTypeNode ptn = (ParsedTypeNode) node.getTypeNode(getBaseName());
            if (ptn != null) {
                return new ParsedReflective(ptn);
            }
        }
        
        return null;
    }
    
    /**
     * Returns the text which the target is displaying as its label. For normal
     * classes this is just the identifier name. For generic classes the generic
     * parameters are shown as well
     * 
     * @return The displayName value
     */
    @Override
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
    @Override
    public void setState(int newState)
    {
        if (state != newState) {
            getPackage().getProject().removeInspectorInstance(getQualifiedName());
            
            // Notify extensions if necessary. Note we don't distinguish
            // S_COMPILING and S_INVALID.
            if (newState == S_NORMAL) {
                modifiedSinceCompile = false;
                if (editor != null) {
                    editor.reInitBreakpoints();
                }
                ClassEvent event = new ClassEvent(ClassEvent.STATE_CHANGED, getPackage(), getBClass(), true);
                ExtensionsManager.getInstance().delegateEvent(event);
            }
            else if (state == S_NORMAL) {
                ClassEvent event = new ClassEvent(ClassEvent.STATE_CHANGED, getPackage(), getBClass(), false);
                ExtensionsManager.getInstance().delegateEvent(event);
            }
            
            state = newState;
            repaint();
        }
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
     * <p>Avoids changing over the role object if the new one is of the same type.
     * 
     * @param newRole The new role value
     */
    protected void setRole(ClassRole newRole)
    {
        if (role.getRoleName() != newRole.getRoleName()) {
            role = newRole;
        }
    }

    /**
     * Test if a given class is a Junit 4 test class.
     * 
     * <p>In Junit4, test classes can be of any type.
     * The only way to test is to check if it has one of the following annotations:
     * @Before, @Test or @After
     * 
     * @param cl class to test
     */
    @SuppressWarnings("unchecked")
    public static boolean isJunit4TestClass(Class<?> cl)
    {
        ClassLoader clLoader = cl.getClassLoader();
        try {
            Class<? extends Annotation> beforeClass =
                (Class<? extends Annotation>) Class.forName("org.junit.Before", false, clLoader);
            Class<? extends Annotation> afterClass =
                (Class<? extends Annotation>) Class.forName("org.junit.After", false, clLoader);
            Class<? extends Annotation> testClass =
                (Class<? extends Annotation>) Class.forName("org.junit.Test", false, clLoader);

            Method[] methods = cl.getDeclaredMethods();
            for (int i=0; i<methods.length; i++) {
                if (methods[i].getAnnotation(beforeClass) != null) {
                    return true;
                }
                if (methods[i].getAnnotation(afterClass) != null) {
                    return true;
                }
                if (methods[i].getAnnotation(testClass) != null) {
                    return true;
                }
            }

        }
        catch (ClassNotFoundException cnfe) {}
        catch (LinkageError le) {}

        // No suitable annotations found, so not a test class
        return false;
    }
    
    /**
     * Use a variety of tests to determine what our role is.
     * 
     * <p>All tests must be very quick and should not rely on any significant
     * computation (ie. reparsing). If computation is required, the existing
     * role will do for the time being.
     * 
     * @param cl Description of the Parameter
     */
    public void determineRole(Class<?> cl)
    {
        isAbstract = false;
        
        if (cl != null) {
            isAbstract = Modifier.isAbstract(cl.getModifiers());
            
            ClassLoader clLoader = cl.getClassLoader();
            Class<?> junitClass = null;
            Class<?> appletClass = null;
            Class<?> midletClass = null;

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
                
                try {
                    midletClass = clLoader.loadClass("javax.microedition.midlet.MIDlet");
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
                setRole(new UnitTestClassRole(false));
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
            else if ( ( midletClass != null )  &&  ( midletClass.isAssignableFrom(cl) ) ) {
                setRole(new MIDletClassRole());
            }
            else if (isJunit4TestClass(cl)) {
                setRole(new UnitTestClassRole(true));
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
                else if (classInfo.isMIDlet()) {
                    setRole(new MIDletClassRole());
                }          
                else if (classInfo.isUnitTest()) {
                    setRole(new UnitTestClassRole(false));
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
     * @param props the properties object to read
     * @param prefix an internal name used for this target to identify its
     *            properties in a properties file used by multiple targets.
     * @exception NumberFormatException Description of the Exception
     */
    @Override
    public void load(Properties props, String prefix)
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
        else if (MIDletClassRole.MIDLET_ROLE_NAME.equals(type)) {
            setRole(new MIDletClassRole());
        }
        else if (UnitTestClassRole.UNITTEST_ROLE_NAME.equals(type)) {
            setRole(new UnitTestClassRole(false));
        }
        else if (UnitTestClassRole.UNITTEST_ROLE_NAME_JUNIT4.equals(type)) {
            setRole(new UnitTestClassRole(true));
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
        String value=props.getProperty(prefix + ".naviview.expanded");
        if (value!=null){
            setNaviviewExpanded(Boolean.parseBoolean(value));
            setProperty(NAVIVIEW_EXPANDED_PROPERTY, String.valueOf(value));
        }
        
        String typeParams = props.getProperty(prefix + ".typeParameters");
        //typeParams is null only if the properties file is saved by an older
        //version of Bluej, thus the type parameters have to fetched from the code
        if (typeParams == null) {
            analyseSource();    
        }
        else {
            typeParameters = typeParams;
        }
    }

    /**
     * Save information about this class target
     * 
     * 
     * @param props the properties object to save to
     * @param prefix an internal name used for this target to identify its
     *            properties in a properties file used by multiple targets.
     */
    @Override
    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);

        if (getRole().getRoleName() != null) {
            props.put(prefix + ".type", getRole().getRoleName());
        }

        if (editorOpen()) {
            openWithInterface = getEditor().isShowingInterface();          
        }
        //saving the state of the naviview (open/close) to the props 
        //setting the value of the expanded according to the value from the editor (if there is)
        //else if there was a previous setting use that
        if (editorOpen() && getProperty(NAVIVIEW_EXPANDED_PROPERTY)!=null){
            props.put(prefix + ".naviview.expanded", String.valueOf(getProperty(NAVIVIEW_EXPANDED_PROPERTY)));
        }
        else if (isNaviviewExpanded!=null) {
                props.put(prefix + ".naviview.expanded", String.valueOf(isNaviviewExpanded()));
        }
        
        props.put(prefix + ".showInterface", new Boolean(openWithInterface).toString());
        props.put(prefix + ".typeParameters", getTypeParameters());

        getRole().save(props, 0, prefix);
    }

    /**
     * Notification that the source file may have been updated, and so we should
     * reload.
     */
    public void reload()
    {
        hasSource = getSourceFile().canRead();
        if (hasSource) {
            if (editor != null) {
                editor.reloadFile();
            }
            else {
                analyseSource();
            }
        }
    }
    
    /**
     * Check if the compiled class and the source are up to date.
     * (Specifically, check if recompilation is not needed. This will
     * always be considered true if the target has no source).
     * 
     * @return true if they are in sync (or there is no source); otherwise false.
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
        for (Iterator<? extends Dependency> it = dependents(); it.hasNext();) {
            Dependency d = it.next();
            ClassTarget dependent = (ClassTarget) d.getFrom();
            
            if (! dependent.isInvalidState()) {    
                // Invalidate the dependent only if it is not already invalidated. 
                // Will avoid going into an infinite circular loop.
                dependent.invalidate();
            }
        }
    }

    /**
     * Verify whether this class target is an interface class
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
     * @return true if class target is a unit test class, else returns false
     */
    public boolean isUnitTest()
    {
        return (getRole() instanceof UnitTestClassRole);
    }
    
    /**
     * Verify whether this class target represents an Enum
     * 
     * @return true if class target represents an Enum, else false
     */
    public boolean isEnum()
    {
        return (getRole() instanceof EnumClassRole);
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
    public Paint getBackgroundPaint(int width, int height)
    {
        if (state == S_COMPILING) {
            return compbg;
        }
        else {
            return getRole().getBackgroundPaint(width, height);
        }
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
     * Get the name of the documentation (.html) file corresponding to this target.
     */
    public File getDocumentationFile()
    {
        String filename = getSourceFile().getPath();
        String docFilename = getPackage().getProject().getDocumentationFile(filename);
        return new File(docFilename);
    }
    
    /**
     * Get a list of .class files for inner classes.
     */
    public File [] getInnerClassFiles()
    {
        File[] files = getPackage().getPath().listFiles(new InnerClassFileFilter());
        return files;
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
        if (editor == null) {
            String filename = getSourceFile().getPath();
            String docFilename = getPackage().getProject().getDocumentationFile(filename);
            if (! hasSourceCode()) {
                filename = null; // no source - show docs only
                showInterface = true;
                if (! new File(docFilename).exists()) {
                    return null;
                }
            }
            
            Project project = getPackage().getProject();
            EntityResolver resolver = new PackageResolver(project.getEntityResolver(),
                    getPackage().getQualifiedName());
            
            if (editorBounds == null) {
                PkgMgrFrame frame = PkgMgrFrame.findFrame(getPackage());
                if (frame != null) {
                    editorBounds = new Rectangle();
                    editorBounds.x = frame.getX() + 40;
                    editorBounds.y = frame.getY() + 20;
                }
            }
            
            editor = EditorManager.getEditorManager().openClass(filename, docFilename,
                    project.getProjectCharset(),
                    getBaseName() + " - " + project.getProjectName(), this, isCompiled(), editorBounds, resolver,
                    project.getJavadocResolver());
            
            // editor may be null if source has been deleted
            // for example.
            if (editor != null) {
                editor.showInterface(showInterface);
            }           
        }
        return editor;
    }

    /**
     * Ensure that the source file of this class is up-to-date (i.e.
     * that any possible unsaved changes in an open editor window are 
     * saved).
     * 
     * <p>This can cause saveEvent() to be generated, which might move
     * the class to a new package (if the package line has been changed).
     */
    @Override
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
            
            @Override
            public void run() {
                switch (state) {
                    // This is the intial state. Try and load the class.
                    case 0:
                        try {
                            clss = getPackage().getDebugger().getClass(getQualifiedName(), true);
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

    @Override
    public void modificationEvent(Editor editor)
    {
        invalidate();
        if (! modifiedSinceCompile) {
            removeBreakpoints();
            getPackage().getProject().getDebugger().removeBreakpointsForClass(getQualifiedName());
            modifiedSinceCompile = true;
        }
        sourceInfo.setSourceModified();
    }

    @Override
    public void saveEvent(Editor editor)
    {
        ClassInfo info = analyseSource();
        if (info != null) {
            updateTargetFile(info);
        }
        determineRole(null);
    }

    @Override
    public String breakpointToggleEvent(Editor editor, int lineNo, boolean set)
    {
        if (isCompiled() || ! modifiedSinceCompile) {
            String possibleError = getPackage().getDebugger().toggleBreakpoint(getQualifiedName(), lineNo, set, null);
            
            if (possibleError == null && getPackage() != null)
            {
                DataCollector.debuggerBreakpointToggle(getPackage(), getSourceFile(), lineNo, set);
            }
            
            return possibleError;
        }
        else {
            return Config.getString("pkgmgr.breakpointMsg");
        }
    }

    // --- end of EditorWatcher interface ---

    /**
     * Remove all breakpoints in this class.
     */
    public void removeBreakpoints()
    {
        if (editor != null) {
            editor.removeBreakpoints();
        }
    }
    
    /**
     * Re-initialize the breakpoints which have been set in this
     * class.
     */
    public void reInitBreakpoints()
    {
        if (editor != null && isCompiled()) {
            editor.reInitBreakpoints();
        }
    }
    
    /**
     * Remove the step mark in this case
     * (the mark in the editor that shows where execution is)
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
    @Override
    public void compile(final Editor editor)
    {
        if (Config.isGreenfoot()) {
            // Even though we do a package compile, we must let the editor know when
            // the compile finishes, so that it updates its status correctly:
            getPackage().compile(new CompileObserver() {
                
                @Override
                public void startCompile(File[] sources) { }
                
                @Override
                public void endCompile(File[] sources, boolean successful)
                {
                    editor.compileFinished(successful);
                }
                
                @Override
                public boolean compilerMessage(Diagnostic diagnostic) { return false; }
            });
        }
        else {
            getPackage().compile(this, false, new CompileObserver() {
                
                @Override
                public void startCompile(File[] sources) {}

                @Override
                public boolean compilerMessage(Diagnostic diagnostic) { return false; }
                
                @Override
                public void endCompile(File[] sources, boolean succesful)
                {
                    editor.compileFinished(succesful);
                }
            });
        }
        
    }

    /**
     * Called when this class target has just been successfully compiled.
     * 
     * We load the compiled class if possible and check it the compilation has
     * resulted in it taking a different role (ie abstract to applet)
     */
    public void endCompile()
    {
        Class<?> cl = getPackage().loadClass(getQualifiedName());

        determineRole(cl);
        analyseDependencies(cl);
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
                setState(S_INVALID);
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
     * Analyse the source code, and save retrieved information.
     * This includes comments and parameter names for methods/constructors,
     * class name, type parameters, etc.
     * <p>
     * Also causes the class role (normal class, unit test, etc) to be
     * guessed based on the source.
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
            Iterator<String> i = info.getTypeParameterTexts().iterator();
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
     */
    public void analyseDependencies(ClassInfo info)
    {
        // currently we don't remove uses dependencies, but just warn

        //removeAllOutDependencies();
        removeInheritDependencies();
        unflagAllOutDependencies();

        String pkgPrefix = getPackage().getQualifiedName();
        pkgPrefix = (pkgPrefix.length() == 0) ? pkgPrefix : pkgPrefix + ".";
        
        // handle superclass dependency
        if (info.getSuperclass() != null) {
            setSuperClass(info.getSuperclass());
        }

        // handle implemented interfaces
        List<String> vect = info.getImplements();
        for (Iterator<String> it = vect.iterator(); it.hasNext();) {
            String name = it.next();
            addInterface(name);
        }

        // handle used classes
        vect = info.getUsed();
        for (Iterator<String> it = vect.iterator(); it.hasNext();) {
            String name = it.next();
            DependentTarget used = getPackage().getDependentTarget(name);
            if (used != null) {
                if (used.getAssociation() == this || this.getAssociation() == used) {
                    continue;
                }

                getPackage().addDependency(new UsesDependency(getPackage(), this, used), true);
            }
        }

        // check for inconsistent use dependencies
        for (Iterator<UsesDependency> it = usesDependencies(); it.hasNext();) {
            UsesDependency usesDep = ((UsesDependency) it.next());
            if (!usesDep.isFlagged()) {
                getPackage().setStatus(usesArrowMsg + usesDep);
            }
        }
    }

    /**
     * Analyse the current dependencies in the compiled class and update the
     * dependencies in the graphical display accordingly.
     */
    public void analyseDependencies(Class<?> cl)
    {
        if (cl != null) {
            removeInheritDependencies();

            Class<?> superClass = cl.getSuperclass();
            if (superClass != null) {
                setSuperClass(superClass.getName());
            }

            Class<?> [] interfaces = cl.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                addInterface(interfaces[i].getName());
            }
        }
    }
    
    
    /**
     * Set the superclass. This adds an extends dependency to the appropriate class.
     * The old extends dependency (if any) must be removed separately.
     * 
     * @param superName  the fully-qualified name of the superclass
     */
    private void setSuperClass(String superName)
    {
        String pkgPrefix = getPackage().getQualifiedName();
        if (superName.startsWith(pkgPrefix)) {
            // Must account for the final "." in the fully qualified name, if the package is
            // not the default package:
            int prefixLen = pkgPrefix.length();
            prefixLen = prefixLen == 0 ? 0 : prefixLen + 1;
            
            superName = superName.substring(prefixLen);
            DependentTarget superclass = getPackage().getDependentTarget(superName);
            if (superclass != null) {
                getPackage().addDependency(new ExtendsDependency(getPackage(), this, superclass), false);
                if (superclass.getState() != S_NORMAL) {
                    setState(S_INVALID);
                }
            }
        }
    }
    
    /**
     * Add an interface. This adds an implements dependency to the appropriate interface.
     */
    private void addInterface(String interfaceName)
    {
        String pkgPrefix = getPackage().getQualifiedName();
        if (interfaceName.startsWith(pkgPrefix)) {
            int dotlen = pkgPrefix.length();
            // If not the default package, we must account for the extra '.'
            dotlen = (dotlen == 0) ? 0 : (dotlen + 1);
            interfaceName = interfaceName.substring(dotlen);
            DependentTarget interfce = getPackage().getDependentTarget(interfaceName);

            if (interfce != null) {
                getPackage().addDependency(new ImplementsDependency(getPackage(), this, interfce), false);
                if (interfce.getState() != S_NORMAL) {
                    setState(S_INVALID);
                }
            }
        }
    }
    
    /**
     * Notification that the class represented by this class target has changed name.
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
        
        try {
            FileUtility.copyFile(oldSourceFile, newSourceFile);
            
            getPackage().updateTargetIdentifier(this, getIdentifierName(), newName);
            
            String filename = newSourceFile.getAbsolutePath();
            String docFilename = getPackage().getProject().getDocumentationFile(filename);
            getEditor().changeName(newName, filename, docFilename);

            oldSourceFile.delete();
            getClassFile().delete();
            getContextFile().delete();
            getDocumentationFile().delete();

            // this is extremely dangerous code here.. must track all
            // variables which are set when ClassTarget is first
            // constructed and fix them up for new class name
            String oldName = getIdentifierName();
            setIdentifierName(newName);
            setDisplayName(newName);
            updateSize();
            
            // Update the BClass object
            BClass bClass = getBClass();
            ExtensionBridge.ChangeBClassName(bClass, getQualifiedName());
            
            // Update the BClassTarget object
            BClassTarget bClassTarget = getBClassTarget();
            ExtensionBridge.changeBClassTargetName(bClassTarget, getQualifiedName());
            
            // Update all BDependency objects related to this target
            for (Iterator<? extends Dependency> iterator = dependencies(); iterator.hasNext();) {
                Dependency outgoingDependency = iterator.next();
                BDependency bDependency = outgoingDependency.getBDependency();
                ExtensionBridge.changeBDependencyOriginName(bDependency, getQualifiedName());
            }
            
            for (Iterator<? extends Dependency> iterator = dependents(); iterator.hasNext();) {
                Dependency incomingDependency = iterator.next();
                BDependency bDependency = incomingDependency.getBDependency();
                ExtensionBridge.changeBDependencyTargetName(bDependency, getQualifiedName());
            }
            
            DataCollector.renamedClass(getPackage(), oldSourceFile, newSourceFile);
            
            // Inform all listeners about the name change
            ClassEvent event = new ClassEvent(ClassEvent.CHANGED_NAME, getPackage(), getBClass(), oldName);
            ExtensionsManager.getInstance().delegateEvent(event);

            return true;
        }
        catch (IOException ioe) {
            return false;
        }
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
    @Override
    public void popupMenu(int x, int y, GraphEditor graphEditor)
    {
        Class<?> cl = null;

        if (state == S_NORMAL) {
            // handle error causes when loading classes which are compiled
            // but not loadable in the current VM. (Eg if they were compiled
            // for a later VM).
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

        // check that the class loading hasn't changed out state
        if (state == S_NORMAL) {
            menu = createMenu(cl);
            // editor.add(menu);
        }
        else {
            menu = createMenu(null);
            // editor.add(menu);
        }

        if (menu != null) {
            menu.show(graphEditor, x, y);
        }
    }

    /**
     * Creates a popup menu for this class target.
     * 
     * @param cl class object associated with this class target
     * @return the created popup menu object
     */
    protected JPopupMenu createMenu(Class<?> cl)
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
        boolean sourceOrDocExists = hasSourceCode() || getDocumentationFile().exists();
        role.addMenuItem(menu, new EditAction(), sourceOrDocExists);
        role.addMenuItem(menu, new CompileAction(), hasSourceCode());
        role.addMenuItem(menu, new InspectAction(), cl != null);
        role.addMenuItem(menu, new RemoveAction(), true);

        // call on role object to add any options needed at bottom
        role.createRoleMenuEnd(menu, this, state);

        MenuManager menuManager = new MenuManager(menu);
        menuManager.setMenuGenerator(new ClassExtensionMenu(this));
        menuManager.addExtensionMenu(getPackage().getProject());

        return menu;
    }

    /**
     * Action which creates a test
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

        @Override
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
     * Action to open the editor for a classtarget
     */
    private class EditAction extends AbstractAction
    {
        public EditAction()
        {
            putValue(NAME, editStr);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            open();
        }
    }

    /**
     * Action to compile a classtarget
     */
    private class CompileAction extends AbstractAction
    {
        public CompileAction()
        {
            putValue(NAME, compileStr);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            getPackage().compile(ClassTarget.this);
        }
    }

    /**
     * Action to remove a classtarget from its package
     */
    private class RemoveAction extends AbstractAction
    {
        public RemoveAction()
        {
            putValue(NAME, removeStr);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            PkgMgrFrame pmf = PkgMgrFrame.findFrame(getPackage());
            if (pmf.askRemoveClass()) {
                getPackage().getEditor().raiseRemoveTargetEvent(ClassTarget.this);
            }
        }
    }

    /**
     * Action to inspect the static members of a class
     */
    private class InspectAction extends AbstractAction
    {
        public InspectAction()
        {
            putValue(NAME, inspectStr);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (checkDebuggerState()) {
                inspect();
            }
        }
    }

    /**
     * Process a double click on this target. That is: open its editor.
     * 
     * @param evt Description of the Parameter
     */
    @Override
    public void doubleClick(MouseEvent evt)
    {
        open();
    }

    /**
     * @return Returns the ghostX.
     */
    @Override
    public int getGhostX()
    {
        return ghostX;
    }

    /**
     * @return Returns the ghostX.
     */
    @Override
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
    @Override
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
    @Override
    public void setGhostSize(int deltaX, int deltaY)
    {
        ghostWidth = Math.max(getWidth() + deltaX, MIN_WIDTH);
        ghostHeight = Math.max(getHeight() + deltaY, MIN_HEIGHT);
    }

    /**
     * Set the target's position to its ghost position.
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
    public void setSize(int width, int height)
    {
        super.setSize(Math.max(width, MIN_WIDTH), Math.max(height, MIN_HEIGHT));
        setGhostSize(0, 0);
    }
    
    @Override
    public void setVisible(boolean visible)
    {
        if (visible != isVisible()) {
            super.setVisible(visible);
            
            // Inform all listeners about the visibility change
            ClassTargetEvent event = new ClassTargetEvent(this, getPackage(), visible);
            ExtensionsManager.getInstance().delegateEvent(event);
        }
    }

    /**
     * Prepares this ClassTarget for removal from a Package. It removes
     * dependency arrows and calls prepareFilesForRemoval() to remove applicable
     * files.
     */
    private void prepareForRemoval()
    {
        if (editor != null) {
            editor.close();
        }

        // if this target is the assocation for another Target, remove
        // the association
        Iterator<? extends Vertex> it = getPackage().getVertices();
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
            File[] files = getPackage().getPath().listFiles(new InnerClassFileFilter());

            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    files[i].delete();
                }
            }
        }

        List<File> allFiles = getRole().getAllFiles(this);
        for(Iterator<File> i = allFiles.iterator(); i.hasNext(); ) {
            i.next().delete();
        }
    }

    @Override
    public void generateDoc()
    {
        getPackage().generateDocumentation(this);
    }

    @Override
    public void remove()
    {
        File srcFile = getSourceFile();
        prepareForRemoval();
        getPackage().removeTarget(this);
        
        // We must remove after the above, because it might involve saving, 
        // and thus recording edits to the file
        DataCollector.removeClass(getPackage(), srcFile);
    }

    @Override
    public boolean isMoveable()
    {
        return isMoveable;
    }

    /**
     * Set whether this ClassTarget can be moved by the user (dragged around).
     * This is set false for unit tests which are associated with another class.
     * 
     * @see bluej.graph.Moveable#setIsMoveable(boolean)
     */
    @Override
    public void setIsMoveable(boolean isMoveable)
    {
        this.isMoveable = isMoveable;
    }
    
    /**
     * perform interactive method call
     */
    @Override
    public void executeMethod(MethodView mv)
    {
        getPackage().getEditor().raiseMethodCallEvent(this, mv);
    }
    
    /**
     * interactive constructor call
     */
    @Override
    public void callConstructor(ConstructorView cv)
    {
        getPackage().getEditor().raiseMethodCallEvent(this, cv);
    }
    
    /**
     * Method to check state of debug VM (currently running may cause problems)
     * and then give options accordingly. 
     * Returns a value from user about how to continue i.e should the original requested be executed.
     * 
     * @return Whether the original request should be executed (dependent on how the user wants to proceed)
     */
    private boolean checkDebuggerState()
    {
        return PkgMgrFrame.createFrame(getPackage()).checkDebuggerState();
    }

    /**
     * Returns the naviview expanded value from the properties file
     * @return 
     */
    public boolean isNaviviewExpanded() 
    {
        return isNaviviewExpanded;
    }

    /**
     * Sets the naviview expanded value from the properties file to this local variable
     * @param isNaviviewExpanded
     */
    public void setNaviviewExpanded(boolean isNaviviewExpanded) 
    {
        this.isNaviviewExpanded=isNaviviewExpanded;  
    }

    /**
     * Retrieves a property from the editor
     */
    @Override
    public String getProperty(String key) 
    {
        return (String)properties.get(key);
    }

    /**
     * Sets a property for the editor
     */
    @Override
    public void setProperty(String key, String value) 
    {
        properties.put(key, value);
    }
    
    @Override
    public String getTooltipText()
    {
        if (!getSourceInfo().isValid()) {
            return Config.getString("graph.tooltip.classBroken");
        } else {
            return null;
        }
    }
    
    @Override
    public void recordEdit(String latest, boolean includeOneLineEdits)
    {
        DataCollector.edit(getPackage(), getSourceFile(), latest, includeOneLineEdits);
    }
   
}
