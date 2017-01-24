/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014,2015,2016,2017  Michael Kolling and John Rosenberg
 
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


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.collect.DiagnosticWithShown;
import bluej.collect.StrideEditReason;
import bluej.compiler.CompileInputFile;
import bluej.compiler.CompileObserver;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.compiler.Diagnostic;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerResult;
import bluej.debugger.gentype.Reflective;
import bluej.debugmgr.objectbench.InvokeListener;
import bluej.editor.Editor;
import bluej.editor.EditorManager;
import bluej.editor.TextEditor;
import bluej.editor.stride.FrameEditor;
import bluej.extensions.BClass;
import bluej.extensions.BClassTarget;
import bluej.extensions.BDependency;
import bluej.extensions.ExtensionBridge;
import bluej.extensions.SourceType;
import bluej.extensions.event.ClassEvent;
import bluej.extensions.event.ClassTargetEvent;
import bluej.extmgr.ClassExtensionMenu;
import bluej.extmgr.ExtensionsManager;
import bluej.extmgr.FXMenuManager;
import bluej.parser.ParseFailure;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.PackageResolver;
import bluej.parser.entity.ParsedReflective;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedTypeNode;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;
import bluej.pkgmgr.JavadocResolver;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PackageEditor;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.SourceInfo;
import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.dependency.ExtendsDependency;
import bluej.pkgmgr.dependency.ImplementsDependency;
import bluej.pkgmgr.dependency.UsesDependency;
import bluej.pkgmgr.target.role.AbstractClassRole;
import bluej.pkgmgr.target.role.ClassRole;
import bluej.pkgmgr.target.role.EnumClassRole;
import bluej.pkgmgr.target.role.InterfaceClassRole;
import bluej.pkgmgr.target.role.StdClassRole;
import bluej.pkgmgr.target.role.UnitTestClassRole;
import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.ast.Parser;
import bluej.stride.framedjava.convert.ConversionWarning;
import bluej.stride.framedjava.convert.ConvertResultDialog;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.TopLevelCodeElement;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileEditor;
import bluej.utility.FileUtility;
import bluej.utility.JavaNames;
import bluej.utility.JavaReflective;
import bluej.utility.JavaUtils;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.ResizableCanvas;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.stage.Stage;
import javafx.stage.Window;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A class target in a package, i.e. a target that is a class file built from
 * Java source code
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 * @author Bruce Quig
 */
public class ClassTarget extends DependentTarget
    implements InvokeListener
{
    final static int MIN_WIDTH = 60;
    final static int MIN_HEIGHT = 30;
    private final static String editStr = Config.getString("pkgmgr.classmenu.edit");
    private final static String compileStr = Config.getString("pkgmgr.classmenu.compile");
    private final static String inspectStr = Config.getString("pkgmgr.classmenu.inspect");
    private final static String removeStr = Config.getString("pkgmgr.classmenu.remove");
    private final static String convertToJavaStr = Config.getString("pkgmgr.classmenu.convertToJava");
    private final static String convertToStrideStr = Config.getString("pkgmgr.classmenu.convertToStride");
    private final static String createTestStr = Config.getString("pkgmgr.classmenu.createTest");
    private final static String launchFXStr = Config.getString("pkgmgr.classmenu.launchFX");

    private static final String STEREOTYPE_OPEN = ""; //"<<";
    private static final String STEREOTYPE_CLOSE = ""; //">>";
    private static final double RESIZE_CORNER_GAP = 4;


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
    @OnThread(value = Tag.Any,requireSynchronized = true)
    private ClassRole role = new StdClassRole();

    // a flag indicating whether an editor, when opened for the first
    // time, should display the interface of this class
    @OnThread(value = Tag.Any,requireSynchronized = true)
    private boolean openWithInterface = false;

    // cached information obtained by parsing the source code
    // automatically becomes invalidated when the source code is
    // edited
    private SourceInfo sourceInfo = new SourceInfo();
    
    // caches whether the class is abstract. Only accurate when the
    // classtarget state is normal (ie. the class is compiled).
    @OnThread(value = Tag.Any,requireSynchronized = true)
    private boolean isAbstract;
    
    // a flag indicating whether an editor should have the naviview expanded/collapsed
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private Optional<Boolean> isNaviviewExpanded = Optional.empty();

    @OnThread(value = Tag.Any,requireSynchronized = true)
    private final List<Integer> cachedBreakpoints = new ArrayList<>();
    
    // flag to prevent recursive calls to analyseDependancies()
    private boolean analysing = false;

    @OnThread(Tag.FXPlatform)
    private boolean isMoveable = true;
    private SourceType sourceAvailable;
    // Part of keeping track of number of editors opened, for Greenfoot phone home:
    private boolean hasBeenOpened = false;

    @OnThread(value = Tag.Any, requireSynchronized = true)
    private String typeParameters = "";
    
    //properties map to store values used in the editor from the props (if necessary)
    @OnThread(value = Tag.Any,requireSynchronized = true)
    private Map<String,String> properties = new HashMap<String,String>();
    // Keep track of whether the editor is open or not; we get a lot of
    // potential open events, and don't want to keep recording ourselves as re-opening
    private boolean recordedAsOpen = false;
    @OnThread(Tag.Any)
    private final AtomicBoolean visible = new AtomicBoolean(true);
    private static final String MENU_STYLE_INBUILT = "class-action-inbuilt";
    @OnThread(Tag.Any)
    private static String[] pseudos;

    // The body of the class target which goes hashed, etc:
    @OnThread(Tag.FX)
    private ResizableCanvas canvas;
    @OnThread(Tag.FXPlatform)
    private Label stereotypeLabel;
    @OnThread(Tag.FXPlatform)
    private boolean isFront = true;
    @OnThread(Tag.FX)
    private static Image greyStripeImage;
    @OnThread(Tag.FX)
    private static Image redStripeImage;
    private static final int GREY_STRIPE_SEPARATION = 12;
    // How far between rows of stripes:
    private static final int RED_STRIPE_SEPARATION = 16;
    private static final int STRIPE_THICKNESS = 3;
    @OnThread(Tag.FX)
    private static final Color RED_STRIPE = Color.rgb(170, 80, 60);
    @OnThread(Tag.FX)
    private static final Color GREY_STRIPE = Color.rgb(158, 139, 116);
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private boolean showingInterface;
    @OnThread(Tag.FXPlatform)
    private boolean drawingExtends = false;
    @OnThread(Tag.FXPlatform)
    private Label nameLabel;

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

        if (pseudos == null)
        {
            pseudos = Utility.mapList(Arrays.<Class<? extends ClassRole>>asList(StdClassRole.class, UnitTestClassRole.class, AbstractClassRole.class, InterfaceClassRole.class, EnumClassRole.class), ClassTarget::pseudoFor).toArray(new String[0]);
        }

        Platform.runLater(() -> {
            JavaFXUtil.addStyleClass(pane, "class-target");

            nameLabel = new Label(baseName);
            JavaFXUtil.addStyleClass(nameLabel, "class-target-name");
            nameLabel.setMaxWidth(9999.0);
            stereotypeLabel = new Label();
            stereotypeLabel.setMaxWidth(9999.0);
            stereotypeLabel.visibleProperty().bind(stereotypeLabel.textProperty().isNotEmpty());
            stereotypeLabel.managedProperty().bind(stereotypeLabel.textProperty().isNotEmpty());
            JavaFXUtil.addStyleClass(stereotypeLabel, "class-target-extra");
            pane.setTop(new VBox(stereotypeLabel, nameLabel));
            canvas = new ResizableCanvas() {
                @Override
                @OnThread(Tag.FX)
                public void resize(double width, double height)
                {
                    super.resize(width, height);
                    redraw();
                }
            };
            pane.setCenter(canvas);
        });

        // This must come after GUI init because it might try to affect GUI:
        calcSourceAvailable();

        // we can take a guess at what the role is going to be for the
        // object based on the start of the template name. If we get this
        // wrong, its no great shame as it'll be fixed the first time they
        // successfully analyse/compile the source.
        if (template != null) {
            if (template.startsWith("unittest")) {
                setRole(new UnitTestClassRole(true));
            }
            else if (template.startsWith("abstract")) {
                setRole(new AbstractClassRole());
            }
            else if (template.startsWith("interface")) {
                setRole(new InterfaceClassRole());
            }
            else if (template.startsWith("enum")) {
                setRole(new EnumClassRole());
            }
            else {
                setRole(new StdClassRole());
            }

        }
    }
    
    private void calcSourceAvailable()
    {
        if (getFrameSourceFile().canRead())
            sourceAvailable = SourceType.Stride;
        else if (getJavaSourceFile().canRead())
            sourceAvailable = SourceType.Java;
        else
        {
            sourceAvailable = SourceType.NONE;
            // Can't have been modified since compile since there's no source to modify:
            setState(State.COMPILED);
        }
    }

    private BClass singleBClass;  // Every Target has none or one BClass
    private BClassTarget singleBClassTarget; // Every Target has none or one BClassTarget
    // Set from Swing thread but read on FX for display:
    
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
     * Returns the {@link BClassTarget} associated with this {@link ClassTarget}.
     * There should be only one {@link BClassTarget} object associated with
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
    @OnThread(Tag.Any)
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
    @OnThread(Tag.Any)
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
        if (getEditor() != null) {
            TextEditor textEditor = editor.assumeText();
            if (textEditor != null) {
                node = textEditor.getParsedNode();
            }
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
    @OnThread(Tag.Any)
    private synchronized String getTypeParameters()
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
    public void setState(State newState)
    {
        if (getState() != newState)
        {
            String qualifiedName = getQualifiedName();
            @OnThread(Tag.Any) Project proj = getPackage().getProject();
            Platform.runLater(() -> proj.removeInspectorInstance(qualifiedName));
            
            if (editor != null && newState != State.NEEDS_COMPILE) {
                editor.compileFinished(newState == State.COMPILED, true);
            }
            
            // Notify extensions if necessary.
            if (newState == State.COMPILED)
            {
                if (editor != null)
                {
                    editor.reInitBreakpoints();
                }
            }
            ClassEvent event = new ClassEvent(ClassEvent.STATE_CHANGED, getPackage(), getBClass(), newState == State.COMPILED, newState == State.HAS_ERROR);
            ExtensionsManager.getInstance().delegateEvent(event);

            Platform.runLater(() -> {redraw();});
            super.setState(newState);
        }
    }

    public void markCompiling(boolean clearErrorState)
    {
        if (clearErrorState && getState() == State.HAS_ERROR)
            setState(State.NEEDS_COMPILE);

        if (getSourceType() == SourceType.Stride) {
            getEditor(); // Create editor if necessary
        }
        if (editor != null)
        {
            if (editor.compileStarted()) {
                markKnownError();
            }
        }
    }

    /**
     * Return the role object for this class target.
     * 
     * @return The role value
     */
    @OnThread(Tag.Any)
    public synchronized ClassRole getRole()
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
    protected synchronized final void setRole(ClassRole newRole)
    {
        if (role == null || role.getRoleName() != newRole.getRoleName()) {
            role = newRole;

            String select = pseudoFor(role.getClass());
            String stereotype = role.getStereotypeLabel();
            boolean shouldBeFront = role == null || !(role instanceof UnitTestClassRole);
            Platform.runLater(() -> {
                isFront = shouldBeFront;
                JavaFXUtil.selectPseudoClass(pane, Arrays.asList(pseudos).indexOf(select), pseudos);
                if (stereotype != null)
                    stereotypeLabel.setText(STEREOTYPE_OPEN + stereotype + STEREOTYPE_CLOSE);
                else
                    stereotypeLabel.setText("");
            });
        }
    }

    @OnThread(Tag.Any)
    private static String pseudoFor(Class<? extends ClassRole> aClass)
    {
        // AbstractClassRole becomes bj-abstract, etc
        String name = aClass.getSimpleName();
        if (name.endsWith("ClassRole"))
            name = name.substring(0, name.length() - "ClassRole".length());
        return "bj-" + name.toLowerCase();
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
        if (cl != null) {
            boolean isAbs;
            synchronized (this)
            {
                isAbstract = Modifier.isAbstract(cl.getModifiers());
                isAbs = isAbstract;
            }
            
            ClassLoader clLoader = cl.getClassLoader();
            Class<?> junitClass = null;

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
            }
            
            if (junitClass == null) {
                junitClass = junit.framework.TestCase.class;
            }

            // As cl is non-null, it is the definitive information
            // source ie. if it thinks its an applet who are we to argue
            // with it.
            if (junitClass.isAssignableFrom(cl)) {
                setRole(new UnitTestClassRole(false));
            }
            else if (Modifier.isInterface(cl.getModifiers())) {
                setRole(new InterfaceClassRole());
            }
            else if (JavaUtils.getJavaUtils().isEnum(cl)) {
                setRole(new EnumClassRole());
            }
            else if (isAbs) {
                setRole(new AbstractClassRole());
            }
            else if (isJunit4TestClass(cl)) {
                setRole(new UnitTestClassRole(true));
            }
            else {
                setRole(new StdClassRole());
            }
        }
        else {
            synchronized (this)
            {
                isAbstract = false;
            }
            
            // try the parsed source code
            ClassInfo classInfo = sourceInfo.getInfoIfAvailable();

            if (classInfo != null) {
                if (classInfo.isUnitTest()) {
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
                    synchronized (this)
                    {
                        if (!(role instanceof UnitTestClassRole))
                        {
                            setRole(new StdClassRole());
                        }
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
        synchronized (this)
        {
            openWithInterface = Boolean.valueOf(intf).booleanValue();
        }

        if (UnitTestClassRole.UNITTEST_ROLE_NAME.equals(type)) {
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
            synchronized (this)
            {
                typeParameters = typeParams;
            }
        }

        synchronized (this)
        {
            cachedBreakpoints.clear();
            try
            {
                for (int i = 0; ; i++)
                {
                    String s = props.getProperty(prefix + ".breakpoint." + Integer.toString(i), "");
                    if (s != null && !s.isEmpty())
                    {
                        cachedBreakpoints.add(Integer.parseInt(s));
                    } else
                        break;
                }
            } catch (NumberFormatException e)
            {
                Debug.reportError("Error parsing breakpoint line number", e);
            }
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
    @OnThread(Tag.FXPlatform)
    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);

        if (getRole().getRoleName() != null) {
            props.put(prefix + ".type", getRole().getRoleName());
        }

        boolean intf;
        synchronized (this)
        {
            openWithInterface = showingInterface;
            intf = openWithInterface;

            //saving the state of the naviview (open/close) to the props
            //setting the value of the expanded according to the value from the editor (if there is)
            //else if there was a previous setting use that
            if (getProperty(NAVIVIEW_EXPANDED_PROPERTY) != null)
            {
                props.put(prefix + ".naviview.expanded", String.valueOf(getProperty(NAVIVIEW_EXPANDED_PROPERTY)));
            } else if (isNaviviewExpanded.isPresent())
            {
                props.put(prefix + ".naviview.expanded", String.valueOf(isNaviviewExpanded()));
            }
            props.put(prefix + ".typeParameters", getTypeParameters());
        }
        
        props.put(prefix + ".showInterface", Boolean.valueOf(intf).toString());


        List<Integer> breakpoints;
        if (editor != null && editor instanceof FrameEditor)
        {
            breakpoints = ((FrameEditor)editor).getBreakpoints();
        }
        else
        {
            synchronized (this)
            {
                breakpoints = cachedBreakpoints;
            }
        }
        for (int i = 0; i < breakpoints.size(); i++)
        {
            props.put(prefix + ".breakpoint." + i, breakpoints.get(i).toString());
        }
        
        getRole().save(props, 0, prefix);
    }

    /**
     * Notification that the source file may have been updated, and so we should
     * reload.
     */
    public void reload()
    {
        calcSourceAvailable();
        if (sourceAvailable != SourceType.NONE) {
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
        if (sourceAvailable == SourceType.NONE) {
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
        markModified();
        for (Dependency d : dependents()) {
            ClassTarget dependent = (ClassTarget) d.getFrom();
            
            if (dependent.isCompiled()) {
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
    @OnThread(Tag.Any)
    public synchronized boolean isAbstract()
    {
        return isAbstract;
    }


    // --- EditableTarget interface ---

    /**
     * Tell whether we have access to the source for this class.
     * 
     * @return Description of the Return Value
     */
    public boolean hasSourceCode()
    {
        return sourceAvailable != SourceType.NONE;
    }
    
    public SourceType getSourceType()
    {
        return sourceAvailable;
    }

    /**
     * @return the name of the Java file this target corresponds to. In the case of a Stride target this is
     *          the file generated during compilation.
     */
    public File getJavaSourceFile()
    {
        return new File(getPackage().getPath(), getBaseName() + "." + SourceType.Java.toString().toLowerCase());
    }
    
    /**
     * @return the name of the Stride file this target corresponds to. This is only valid for Stride targets.
     */
    public File getFrameSourceFile()
    {
        return new File(getPackage().getPath(), getBaseName() + "." + SourceType.Stride.toString().toLowerCase());
    }
    
    @SuppressWarnings("incomplete-switch")
    @Override
    public File getSourceFile()
    {
        switch (sourceAvailable)
        {
            case Java: return getJavaSourceFile();
            case Stride: return getFrameSourceFile();
        }
        return null;
    }

    @OnThread(Tag.Any)
    public boolean isVisible()
    {
        return visible.get();
    }

    public void markCompiled()
    {
        setState(State.COMPILED);
    }

    public static class SourceFileInfo
    {
        public final File file;
        public final SourceType sourceType;

        public SourceFileInfo(File file, SourceType sourceType)
        {
            this.file = file;
            this.sourceType = sourceType;
        }
    }

    /**
     * If this is a Java class, returns the .java source file only.
     * If this is a Stride class, returns the .stride and .java source files, *in that order*.
     * This is a strict requirement in the call in DataCollectorImpl, do not change the order.
     */
    public Collection<SourceFileInfo> getAllSourceFilesJavaLast()
    {
        List<SourceFileInfo> list = new ArrayList<>();
        if (sourceAvailable.equals(SourceType.Stride)) {
            list.add(new SourceFileInfo(getFrameSourceFile(), SourceType.Stride));
        }
        list.add(new SourceFileInfo(getJavaSourceFile(), SourceType.Java));
        return list;
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
        // We ask for Java source file, regardless of source type,
        // because we're only using it to derive the .html file with the same stub:
        String filename = getJavaSourceFile().getPath();
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
    @OnThread(value = Tag.Swing, ignoreParent = true)
    class InnerClassFileFilter
        implements FileFilter
    {
        /**
         * Description of the Method
         * 
         * @param pathname Description of the Parameter
         * @return Description of the Return Value
         */
        @Override
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
    @Override
    // TODO should be Swing_WaitsForFX
    @OnThread(Tag.Swing)
    public Editor getEditor()
    {
        boolean withInterface;
        synchronized (this)
        {
            withInterface = this.openWithInterface;
        }
        return getEditor(withInterface);
    }

    /**
     * Get an editor for this class, either in source view or interface view.
     * 
     * @param showInterface Determine whether to show interface view or 
     *         source view in the editor.
     * @return the editor object associated with this target. May be null if
     *         there was a problem opening this editor.
     */
    // TODO should be Swing_WaitsForFX
    @OnThread(Tag.Swing)
    private Editor getEditor(boolean showInterface) // TODO remove the ignoreParent = true, and tag calls properly
    {
        // ClassTarget must have source code if it is to provide an editor
        if (editor == null) {
            final String filename;
            String docFilename = getDocumentationFile().getPath();
            if (sourceAvailable == SourceType.NONE) {
                filename = null; // no source - show docs only
                showInterface = true;
                if (! new File(docFilename).exists()) {
                    return null;
                }
            }
            else
            {
                filename = getSourceFile().getPath();
            }

            Project project = getPackage().getProject();
            EntityResolver resolver = new PackageResolver(project.getEntityResolver(),
                    getPackage().getQualifiedName());

            if (sourceAvailable == SourceType.Java || sourceAvailable == SourceType.NONE) {
                editor = EditorManager.getEditorManager().openClass(filename, docFilename,
                        project.getProjectCharset(),
                        getBaseName(), project::getDefaultFXTabbedEditor, this, isCompiled(), resolver,
                        project.getJavadocResolver(), this::recordEditorOpen);
            }
            else if (sourceAvailable == SourceType.Stride) {
                final CompletableFuture<Editor> q = new CompletableFuture<>();
                // need to pull some parameters out while on the Swing thread:
                File frameSourceFile = getFrameSourceFile();
                File javaSourceFile = getJavaSourceFile();
                JavadocResolver javadocResolver = project.getJavadocResolver();
                Package pkg = getPackage();
                final Runnable openCallback = this::recordEditorOpen;
                Platform.runLater(() -> {
                    q.complete(new FrameEditor(frameSourceFile, javaSourceFile, this, resolver, javadocResolver, pkg, openCallback));
                });

                try {
                    editor = q.get();
                } catch (InterruptedException | ExecutionException e) {
                    Debug.reportError(e);
                }
            }
            
            // editor may be null if source has been deleted
            // for example.
            if (editor != null) {
                editor.showInterface(showInterface);
            }
        }
        return editor;
    }

    /**
     * Records that the editor for this class target has been opened
     * (i.e. actually made visible on screen).  Further calls after
     * the first call will be ignored.
     */
    private void recordEditorOpen()
    {
        if (!hasBeenOpened)
        {
            hasBeenOpened = true;
            switch (sourceAvailable)
            {
                case Java:
                    Config.recordEditorOpen(Config.SourceType.Java);
                    break;
                case Stride:
                    Config.recordEditorOpen(Config.SourceType.Stride);
                    break;
                default:
                    break;
            }
        }
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
        // When creating a Stride class, we need to load the editor
        // in order to save and generate the Java code (or at least,
        // that's an easy way to do it).  Not necessary for Java classes:
        if(editor == null && sourceAvailable == SourceType.Stride) {
            getEditor();
        }
        super.ensureSaved();
    }

    // --- end of EditableTarget interface ---

    // --- user interface function implementation ---

    /**
     * Open an inspector window for the class represented by this target.
     */
    private void inspect()
    {
        new Thread() {
            
            DebuggerClass clss;
            
            @Override
            public void run() {
                // Try and load the class.
                try {
                    DebuggerClass clss = getPackage().getDebugger().getClass(getQualifiedName(), true);
                    PkgMgrFrame pmf = PkgMgrFrame.findFrame(getPackage());
                    Project proj = getPackage().getProject();
                    Platform.runLater(() -> {
                        proj.getClassInspectorInstance(clss, getPackage(), pmf.getFXWindow(), ClassTarget.this.getNode());
                    });
                }
                catch (ClassNotFoundException cnfe) {}
            }
        }.start();
    }

    // --- EditorWatcher interface ---

    @Override
    public void modificationEvent(Editor editor)
    {
        invalidate();
        if (isCompiled()) {
            removeBreakpoints();
            if (getPackage().getProject().getDebugger() != null)
            {
                getPackage().getProject().getDebugger().removeBreakpointsForClass(getQualifiedName());
            }
            setState(State.NEEDS_COMPILE);
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
    public String breakpointToggleEvent(int lineNo, boolean set)
    {
        if (isCompiled()) {
            String possibleError = getPackage().getDebugger().toggleBreakpoint(getQualifiedName(), lineNo, set, null);
            Debug.message("Setting breakpoint: " + getQualifiedName() + ":" + lineNo);
            
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
    
    @Override
    public void clearAllBreakpoints()
    {
        Package pkg = getPackage();
        if (pkg != null) // Can happen during removal
            pkg.getDebugger().removeBreakpointsForClass(getQualifiedName());
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
        else if (isCompiled() && sourceAvailable == SourceType.Stride)
        {
            // In Stride, breakpoints persist with the saved source,
            // and we should set them even if the editor is not open.
            // So we cache them as properties and use that here if
            // the editor has not been opened yet:
            List<Integer> breakpoints;
            synchronized (this)
            {
                breakpoints = new ArrayList<>(this.cachedBreakpoints);
            }
            for (Integer line : breakpoints)
            {
                breakpointToggleEvent(line, true);
            }
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
        return getState() == State.COMPILED;
    }

    /**
     * Description of the Method
     * 
     * @param editor Description of the Parameter
     */
    @Override
    public void compile(final Editor editor, CompileReason reason, CompileType type)
    {
        final CompileObserver compObserver = new CompileObserver()
        {
            @Override
            public void startCompile(CompileInputFile[] sources, CompileReason reason, CompileType type)
            {
            }

            @Override
            public void endCompile(CompileInputFile[] sources, boolean successful, CompileType type)
            {
                editor.compileFinished(successful, type.keepClasses());
            }

            @Override
            public void compilerMessage(Diagnostic diagnostic, CompileType type)
            {
            }
        };

        if (Config.isGreenfoot()) {
            //In Greenfoot compiling always compiles the whole package, and at least
            // compiles this target:
            setState(State.NEEDS_COMPILE);

            // Even though we do a package compile, we must let the editor know when
            // the compile finishes, so that it updates its status correctly:

            getPackage().compile(compObserver, reason, type);
        }
        else {
            getPackage().compile(this, false, compObserver, reason, type);
        }
        
    }

    @Override
    @OnThread(Tag.Any)
    public void scheduleCompilation(boolean immediate, CompileReason reason, CompileType type)
    {
        if (Config.isGreenfoot())
            getPackage().getProject().scheduleCompilation(immediate, reason, type, getPackage());
        else
            getPackage().getProject().scheduleCompilation(immediate, reason, type, this);
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
    public boolean generateSkeleton(String template, SourceType sourceType)
    {
        // delegate to role object
        if (template == null) {
            Debug.reportError("generate class skeleton error");
            return false;
        }
        else {
            boolean success;
            switch (sourceType) {
                case Java:
                    synchronized (this)
                    {
                        success = role.generateSkeleton(template, getPackage(), getBaseName(), getJavaSourceFile().getPath());
                    }
                    break;
                case Stride:
                    addStride(Loader.buildTopLevelElement(template, getPackage().getProject().getEntityResolver(),
                            getBaseName(), getPackage().getBaseName()));
                    success = true;
                    break;
                default:
                    success = false;
            }

            if (success) {
                // skeleton successfully generated
                setState(State.NEEDS_COMPILE);
                sourceAvailable = sourceType;
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

        ClassInfo info = sourceInfo.getInfo(getJavaSourceFile(), getPackage());

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
        synchronized (this)
        {
            if (!newTypeParameters.equals(typeParameters))
            {
                typeParameters = newTypeParameters;
                Platform.runLater(() ->
                {
                    updateSize();
                });
            }
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

                getPackage().addDependency(new UsesDependency(getPackage(), this, used));
            }
        }

        // check for inconsistent use dependencies
        for (UsesDependency usesDep : usesDependencies()) {
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
                getPackage().addDependency(new ExtendsDependency(getPackage(), this, superclass));
                if (superclass.getState() != State.COMPILED) {
                    markModified();
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
                getPackage().addDependency(new ImplementsDependency(getPackage(), this, interfce));
                if (interfce.getState() != State.COMPILED) {
                    markModified();
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

        File newSourceFile = new File(getPackage().getPath(), newName + "." + getSourceType().toString().toLowerCase());
        File oldSourceFile = getSourceFile();
        
        try {
            FileUtility.copyFile(oldSourceFile, newSourceFile);
            
            getPackage().updateTargetIdentifier(this, getIdentifierName(), newName);
            
            String filename = newSourceFile.getAbsolutePath();
            String javaFilename;
            if (getSourceType().equals(SourceType.Stride)) {
                final File javaFile = new File(getPackage().getPath(), newName + "." + SourceType.Java.toString().toLowerCase());
                javaFilename = javaFile.getAbsolutePath();

                // Also copy the Java file across:
                FileUtility.copyFile(getJavaSourceFile(), javaFile);
            }
            else {
                javaFilename = filename;
            }
            String docFilename = getPackage().getProject().getDocumentationFile(javaFilename);
            getEditor().changeName(newName, filename, javaFilename, docFilename);

            deleteSourceFiles();
            getClassFile().delete();
            getContextFile().delete();
            getDocumentationFile().delete();

            // this is extremely dangerous code here.. must track all
            // variables which are set when ClassTarget is first
            // constructed and fix them up for new class name
            String oldName = getIdentifierName();
            setIdentifierName(newName);
            setDisplayName(newName);
            Platform.runLater(() -> {updateSize();});
            
            // Update the BClass object
            BClass bClass = getBClass();
            ExtensionBridge.ChangeBClassName(bClass, getQualifiedName());
            
            // Update the BClassTarget object
            BClassTarget bClassTarget = getBClassTarget();
            ExtensionBridge.changeBClassTargetName(bClassTarget, getQualifiedName());
            
            // Update all BDependency objects related to this target
            for (Dependency outgoingDependency : dependencies()) {
                BDependency bDependency = outgoingDependency.getBDependency();
                ExtensionBridge.changeBDependencyOriginName(bDependency, getQualifiedName());
            }
            
            for (Dependency incomingDependency : dependents()) {
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
     * Delete all the source files (edited and generated) for this target.
     */
    private void deleteSourceFiles()
    {
        if (getSourceType().equals(SourceType.Stride)) {
            getJavaSourceFile().delete();
        }
        getSourceFile().delete();
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
     * @param newName the new fully qualified package name
     */
    private void doPackageNameChange(String newName)
    {
        Project proj = getPackage().getProject();

        Package dstPkg = proj.getPackage(newName);

        boolean packageInvalid = dstPkg == null;
        boolean packageNameClash = dstPkg != null && dstPkg.getTarget(getBaseName()) != null;
        
        Platform.runLater(() -> {
            
            if (packageInvalid)
            {
                DialogManager.showErrorFX(null, "package-name-invalid");
            }
            else
            {
                // fix for bug #382. Potentially could clash with a package
                // in the destination package with the same name
                if (packageNameClash)
                {
                    DialogManager.showErrorFX(null, "package-name-clash");
                    // fall through to enforcePackage, below.
                }
                else if (DialogManager.askQuestionFX(null, "package-name-changed") == 0)
                {
                    SwingUtilities.invokeLater(() -> {
                        dstPkg.importFile(getSourceFile());
                        prepareForRemoval();
                        getPackage().removeTarget(this);
                        close();
                    });
                    return;
                }
            }

            SwingUtilities.invokeLater(() -> {
                // all non working paths lead here.. lets fix the package line
                // up so it is back to what we expect
                try
                {
                    enforcePackage(getPackage().getQualifiedName());
                    getEditor().reloadFile();
                }
                catch (IOException ioe)
                {
                }
            });
        });
    }

    /**
     * Resizes the class so the entire classname + type parameter are visible
     *  
     */
    @OnThread(Tag.FXPlatform)
    private void updateSize()
    {
        SwingUtilities.invokeLater(() -> {
            String displayName = getDisplayName();
            Platform.runLater(() -> {
                int width = calculateWidth(displayName);
                setSize(width, getHeight());
                repaint();
            });
        });
    }

    /**
     * Post the context menu for this target.
     * 
     * @param x  the x coordinate for the menu, relative to graph editor
     * @param y  the y coordinate for the menu, relative to graph editor
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void popupMenu(int x, int y, PackageEditor graphEditor)
    {
        SwingUtilities.invokeLater(() ->
        {
            Class<?> cl = null;

            if (getState() == State.COMPILED)
            {
                // handle error causes when loading classes which are compiled
                // but not loadable in the current VM. (Eg if they were compiled
                // for a later VM).
                // we detect the error, remove the class file, and invalidate
                // to allow them to be recompiled
                cl = getPackage().loadClass(getQualifiedName());
                if (cl == null)
                {
                    // trouble loading the class
                    // remove the class file and invalidate the target
                    if (sourceAvailable != SourceType.NONE)
                    {
                        getClassFile().delete();
                        invalidate();
                    }
                }
            }

            // check that the class loading hasn't changed out state
            if (getState() != State.COMPILED)
                cl = null;
            // Need a bunch of info from the Swing thread before hopping to FX:
            Class<?> clFinal = cl;
            final ClassRole roleFinal;
            synchronized (this)
            {
                roleFinal = role;
            }
            SourceType sourceAvailableFinal = sourceAvailable;
            boolean docExists = getDocumentationFile().exists();
            ExtensionsManager extMgr = ExtensionsManager.getInstance();
            Platform.runLater(() -> {
                withMenu(clFinal,  roleFinal, sourceAvailableFinal, docExists, menu -> {
                    showingMenu(menu);
                    menu.show(pane, x, y);
                }, extMgr);
            });
        });
    }

    /**
     * Creates a popup menu for this class target.
     * 
     * @param extMgr
     * @param cl class object associated with this class target
     * @return the created popup menu object
     */
    @OnThread(Tag.FXPlatform)
    protected void withMenu(Class<?> cl, ClassRole roleRef, SourceType source, boolean docExists, FXPlatformConsumer<ContextMenu> withMenu, ExtensionsManager extMgr)
    {
        final ContextMenu menu = new ContextMenu();

        // call on role object to add any options needed at top
        roleRef.createRoleMenu(menu.getItems(), this, cl, getState());

        if (cl != null)
        {
            if (Application.class.isAssignableFrom(cl))
            {
                menu.getItems().add(JavaFXUtil.withStyleClass(JavaFXUtil.makeMenuItem(launchFXStr,() -> {
                    PackageEditor ed = getPackage().getEditor();
                    Window fxWindow = ed.getFXWindow();
                    SwingUtilities.invokeLater(() -> {
                        DebuggerResult result = getPackage().getDebugger().launchFXApp(cl.getName());
                        switch (result.getExitStatus())
                        {
                            case Debugger.NORMAL_EXIT:
                                DebuggerObject obj = result.getResultObject();
                                ed.raisePutOnBenchEvent(fxWindow, obj, obj.getGenType(), null, false, Optional.empty());
                                break;
                        }
                    });
                }, null), MENU_STYLE_INBUILT));
            }
            
            if (roleRef.createClassConstructorMenu(menu.getItems(), this, cl)) {
                menu.getItems().add(new SeparatorMenuItem());
            }
            
            if (roleRef.createClassStaticMenu(menu.getItems(), this, source != SourceType.NONE, cl)) {
                menu.getItems().add(new SeparatorMenuItem());
            }
        }
        boolean sourceOrDocExists = source != SourceType.NONE || docExists;
        menu.getItems().add(new EditAction(sourceOrDocExists));
        menu.getItems().add(new CompileAction(source != SourceType.NONE));
        menu.getItems().add(new InspectAction(cl != null));
        menu.getItems().add(new RemoveAction());
        if (source == SourceType.Stride)
            menu.getItems().add(new ConvertToJavaAction());
        else if (source == SourceType.Java && roleRef.canConvertToStride())
            menu.getItems().add(new ConvertToStrideAction());

        // call on role object to add any options needed at bottom
        roleRef.createRoleMenuEnd(menu.getItems(), this, getState());

        FXMenuManager menuManager = new FXMenuManager(menu, extMgr, new ClassExtensionMenu(this));
        SwingUtilities.invokeLater(() -> {
            menuManager.addExtensionMenu(getPackage().getProject());

            Platform.runLater(() -> {withMenu.accept(menu);});
        });
    }

    /**
     * Action which creates a test
     */
    @OnThread(Tag.FXPlatform)
    public class CreateTestAction extends MenuItem
    {
        /**
         * Constructor for the CreateTestAction object
         */
        public CreateTestAction()
        {
            super(createTestStr);
            setOnAction(e -> SwingUtilities.invokeLater(() -> actionPerformed(e)));
            JavaFXUtil.addStyleClass(this, MENU_STYLE_INBUILT);
        }
        
        @OnThread(Tag.Swing)
        private void actionPerformed(ActionEvent e)
        {
            PkgMgrFrame pmf = PkgMgrFrame.findFrame(getPackage());

            if (pmf != null) {
                String testClassName = getIdentifierName() + "Test";
                pmf.createNewClass(testClassName, "unittest", SourceType.Java, true, -1, -1);
                // we want to check that the previous called actually
                // created a unit test class as a name clash with an existing
                // class would not. This prevents a non unit test becoming
                // associated with a class unintentionally
                Target target = getPackage().getTarget(testClassName);
                DependentTarget assoc = null;
                if (target instanceof ClassTarget) {
                    ClassTarget ct = (ClassTarget) target;
                    if (ct != null && ct.isUnitTest()) {
                        assoc = (DependentTarget) getPackage().getTarget(getIdentifierName() + "Test");
                    }
                }
                DependentTarget assocFinal = assoc;
                PackageEditor pkgEd = getPackage().getEditor();
                Platform.runLater(() -> {
                    if (assocFinal != null)
                        setAssociation(assocFinal);
                    updateAssociatePosition();
                    pkgEd.repaint();
                });

            }
        }
    }

    /**
     * Action to open the editor for a classtarget
     */
    @OnThread(Tag.FXPlatform)
    private class EditAction extends MenuItem
    {
        public EditAction(boolean enable)
        {
            super(editStr);
            setOnAction(e -> SwingUtilities.invokeLater(() -> open()));
            setDisable(!enable);
            JavaFXUtil.addStyleClass(this, MENU_STYLE_INBUILT);
        }
    }

    /**
     * Action to compile a classtarget
     */
    @OnThread(Tag.FXPlatform)
    private class CompileAction extends MenuItem
    {
        public CompileAction(boolean enable)
        {
            super(compileStr);
            setOnAction(e -> SwingUtilities.invokeLater(() -> {
                getPackage().compile(ClassTarget.this, CompileReason.USER, CompileType.EXPLICIT_USER_COMPILE);
            }));
            setDisable(!enable);
            JavaFXUtil.addStyleClass(this, MENU_STYLE_INBUILT);
        }
    }

    /**
     * Action to remove a classtarget from its package
     */
    @OnThread(Tag.FXPlatform)
    private class RemoveAction extends MenuItem
    {
        public RemoveAction()
        {
            super(removeStr);
            setOnAction(e -> SwingUtilities.invokeLater(() -> actionPerformed(e)));
            JavaFXUtil.addStyleClass(this, MENU_STYLE_INBUILT);
        }

        @OnThread(Tag.Swing)
        private void actionPerformed(ActionEvent e)
        {
            PkgMgrFrame pmf = PkgMgrFrame.findFrame(getPackage());
            Platform.runLater(() -> {
                if (pmf.askRemoveClass())
                {
                    SwingUtilities.invokeLater(() -> getPackage().getEditor().raiseRemoveTargetEvent(ClassTarget.this));
                }
            });
        }
    }

    /**
     * Action to inspect the static members of a class
     */
    @OnThread(Tag.FXPlatform)
    private class InspectAction extends MenuItem
    {
        public InspectAction(boolean enable)
        {
            super(inspectStr);
            setOnAction(e -> SwingUtilities.invokeLater(() -> actionPerformed(e)));
            setDisable(!enable);
            JavaFXUtil.addStyleClass(this, MENU_STYLE_INBUILT);
        }

        @OnThread(Tag.Swing)
        private void actionPerformed(ActionEvent e)
        {
            if (checkDebuggerState()) {
                inspect();
            }
        }
    }

    @OnThread(Tag.FXPlatform)
    private class ConvertToJavaAction extends MenuItem
    {
        public ConvertToJavaAction()
        {
            super(convertToJavaStr);
            setOnAction(this::actionPerformed);
            JavaFXUtil.addStyleClass(this, MENU_STYLE_INBUILT);
        }

        private void actionPerformed(ActionEvent e)
        {
            if (JavaFXUtil.confirmDialog("convert.to.java.title", "convert.to.java.message", (Stage)ClassTarget.this.pane.getScene().getWindow(), true))
            {
                SwingUtilities.invokeLater(() -> removeStride());
            }
        }
    }

    @OnThread(Tag.FXPlatform)
    public class ConvertToStrideAction extends MenuItem
    {
        public ConvertToStrideAction()
        {
            super(convertToStrideStr);
            setOnAction(e -> SwingUtilities.invokeLater(() -> convertToStride()));
            JavaFXUtil.addStyleClass(this, MENU_STYLE_INBUILT);
        }
    }
    
    @OnThread(Tag.Swing)
    public void convertToStride()
    {
        File javaSourceFile = getJavaSourceFile();
        Platform.runLater(() -> {
            Stage window = null;
            if (pane.getScene() != null)
                window = (Stage)pane.getScene().getWindow();
            if (JavaFXUtil.confirmDialog("convert.to.stride.title", "convert.to.stride.message", window, true))
            {
                try
                {
                    Parser.ConversionResult javaConvertResult = Parser.javaToStride(Files.readAllLines(javaSourceFile.toPath()).stream().collect(Collectors.joining("\n")), Parser.JavaContext.TOP_LEVEL, false);
                    if (!javaConvertResult.getWarnings().isEmpty())
                    {
                        new ConvertResultDialog(javaConvertResult.getWarnings().stream().map(ConversionWarning::getMessage).collect(Collectors.toList())).showAndWait();
                    }
                    List<CodeElement> elements = javaConvertResult.getElements();
                    
                    if (elements.size() != 1 || !(elements.get(0) instanceof TopLevelCodeElement))
                    {
                        JavaFXUtil.errorDialog("convert.to.stride.error.title", "convert.to.stride.error.message");
                        return; // Abort
                    }
                    SwingUtilities.invokeLater(() -> addStride((TopLevelCodeElement)elements.get(0)));    
                }
                catch (IOException | ParseFailure pf)
                {
                    new ConvertResultDialog(pf.getMessage()).showAndWait();
                    // Abort the conversion
                }
            }
        });
    }

    /**
     * Process a double click on this target. That is: open its editor.
     * 
     * @param evt Description of the Parameter
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void doubleClick()
    {
        SwingUtilities.invokeLater(() -> {open();});
    }
    /**
     * Set the size of this target.
     * 
     * @param width The new size value
     * @param height The new size value
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void setSize(int width, int height)
    {
        int w = Math.max(width, MIN_WIDTH);
        int h = Math.max(height, MIN_HEIGHT);
        super.setSize(w, h);
        if(assoc != null)
            assoc.setSize(w, h);
    }
    
    @OnThread(Tag.FXPlatform)
    public void setVisible(boolean vis)
    {
        if (vis != this.visible.get()) {
            this.visible.set(vis);
            pane.setVisible(vis);
            
            SwingUtilities.invokeLater(() -> {
                // Inform all listeners about the visibility change
                ClassTargetEvent event = new ClassTargetEvent(this, getPackage(), vis);
                ExtensionsManager.getInstance().delegateEvent(event);
            });
        }
    }

    @OnThread(Tag.FX)
    @Override
    protected void redraw()
    {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        g.clearRect(0, 0, width, height);

        // Draw either grey or red stripes:
        if (getState() != State.COMPILED)
        {
            // We could draw the stripes manually each time, but that
            // could get quite time-consuming when we have lots of classes.
            // Instead, we create an image of the given stripes which
            // we can draw tiled to save time.
            if (hasKnownError())
            {
                // Red stripes
                int size = RED_STRIPE_SEPARATION * 10;
                if (redStripeImage == null)
                {
                    redStripeImage = JavaFXUtil.createImage((int)size, (int)size, gImage -> {
                        JavaFXUtil.stripeRect(gImage, 0, 0, size, size, RED_STRIPE_SEPARATION - STRIPE_THICKNESS, STRIPE_THICKNESS, false, RED_STRIPE);
                        JavaFXUtil.stripeRect(gImage, 0, 0, size, size, RED_STRIPE_SEPARATION - STRIPE_THICKNESS, STRIPE_THICKNESS, true, RED_STRIPE);
                    });
                }
                g.setFill(new ImagePattern(redStripeImage, 0, 0, size, size, false));
                g.fillRect(0, 0, width, height);
            }
            else
            {
                int size = GREY_STRIPE_SEPARATION * 10;
                // Grey stripes
                if (greyStripeImage == null)
                {
                    greyStripeImage = JavaFXUtil.createImage(size, size, gImage -> {
                        JavaFXUtil.stripeRect(gImage, 0, 0, size, size, GREY_STRIPE_SEPARATION - STRIPE_THICKNESS, STRIPE_THICKNESS, false, GREY_STRIPE);
                    });
                }
                g.setFill(new ImagePattern(greyStripeImage, 0, 0, size, size, false));
                g.fillRect(0, 0, width, height);
            }
        }

        if (this.selected && isResizable())
        {
            g.setStroke(javafx.scene.paint.Color.BLACK);
            g.setLineDashes();
            g.setLineWidth(1.0);
            // Draw the marks in the corner to indicate resizing is possible:
            g.strokeLine(width - RESIZE_CORNER_SIZE, height, width, height - RESIZE_CORNER_SIZE);
            g.strokeLine(width - RESIZE_CORNER_SIZE + RESIZE_CORNER_GAP, height, width, height - RESIZE_CORNER_SIZE + RESIZE_CORNER_GAP);
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
        for (Target o : getPackage().getVertices())
        {
            if (o instanceof DependentTarget) {
                DependentTarget d = (DependentTarget) o;
                if (this.equals(d.getAssociation())) {
                    Platform.runLater(() -> {d.setAssociation(null);});
                }
            }
        }

        // flag dependent Targets as invalid
        invalidate();
        removeAllInDependencies();
        removeAllOutDependencies();
        // remove associated files (.frame, .class, .java and .ctxt)
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
        Package pkg = getPackage();
        pkg.removeTarget(this);
        
        // We must remove after the above, because it might involve saving, 
        // and thus recording edits to the file
        DataCollector.removeClass(pkg, srcFile);


        // In Greenfoot we don't do detailed dependency tracking, so we just recompile the whole
        // package if any class is removed:
        if (Config.isGreenfoot())
            pkg.rebuild();
    }
    
    public void removeStride()
    {
        if (sourceAvailable != SourceType.Stride)
            throw new IllegalStateException("Cannot convert non-Stride from Stride to Java");

        if(editor != null) {
            editor.close();
            try {
                editor.saveJavaWithoutWarning();
            } catch (IOException e) {
                Debug.reportError(e);
            }
            editor = null;
        }

        File srcFile = getSourceFile();
        if (srcFile.exists()) {
            srcFile.delete();
        }
        sourceAvailable = SourceType.Java;

        // getSourceFile() will now return the Java file:
        DataCollector.convertStrideToJava(getPackage(), srcFile, getSourceFile());
    }
    
    public void addStride(TopLevelCodeElement element)
    {
        if (editor != null)
        {
            editor.close();
            editor = null;
        }
        File strideFile = getFrameSourceFile();
        try
        {
            Files.write(strideFile.toPath(), Arrays.asList(Utility.splitLines(Utility.serialiseCodeToString(element.toXML()))), Charset.forName("UTF-8"));
        }
        catch (IOException e)
        {
            Debug.reportError(e);
            Platform.runLater(() ->
                JavaFXUtil.errorDialog(Config.getString("convert.to.stride.title"), e.getMessage())
            );
            return;
        }
        sourceAvailable = SourceType.Stride;
        //DataCollector.convertJavaToStride(getPackage(), srcFile, getSourceFile());
    }

    @OnThread(Tag.FXPlatform)
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
    @OnThread(Tag.FXPlatform)
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
        return PkgMgrFrame.createFrame(getPackage(), null).checkDebuggerState();
    }

    /**
     * Returns the naviview expanded value from the properties file
     * @return 
     */
    @OnThread(Tag.Any)
    public synchronized boolean isNaviviewExpanded()
    {
        return isNaviviewExpanded.orElse(false);
    }

    /**
     * Sets the naviview expanded value from the properties file to this local variable
     * @param isNaviviewExpanded
     */
    public synchronized void setNaviviewExpanded(boolean isNaviviewExpanded)
    {
        this.isNaviviewExpanded = Optional.of(isNaviviewExpanded);
    }

    /**
     * Retrieves a property from the editor
     */
    @Override
    @OnThread(Tag.Any)
    public synchronized String getProperty(String key)
    {
        return properties.get(key);
    }

    /**
     * Sets a property for the editor
     */
    @Override
    public synchronized void setProperty(String key, String value)
    {
        properties.put(key, value);
    }
    
    @Override
    public void recordEdit(SourceType sourceType, String latest, boolean includeOneLineEdits, StrideEditReason reason)
    {
        if (sourceType == SourceType.Java)
        {
            DataCollector.edit(getPackage(), getJavaSourceFile(), latest, includeOneLineEdits, null);
        }
        else if (sourceType == SourceType.Stride && this.sourceAvailable == SourceType.Stride)
        {
            DataCollector.edit(getPackage(), getFrameSourceFile(), latest, includeOneLineEdits, reason);
        }
        else
        {
            Debug.message("Attempting to send " + sourceType + " source when available is: " + this.sourceAvailable);
        }
    }

    @Override
    public void recordClose()
    {
        DataCollector.closeClass(getPackage(), getSourceFile());
        recordedAsOpen = false;
    }

    @Override
    public void recordOpen()
    {
        if (recordedAsOpen == false)
        {
            DataCollector.openClass(getPackage(), getSourceFile());
            recordedAsOpen = true;
        }
    }

    @Override
    public void recordSelected()
    {
        DataCollector.selectClass(getPackage(), getSourceFile());
    }

    public CompileInputFile getCompileInputFile()
    {
        return new CompileInputFile(getJavaSourceFile(), getSourceFile());
    }

    /**
     * Mark that there is a known compilation error with this target.
     * (Mark is cleared when state is set to COMPILING).
     */
    public void markKnownError()
    {
        // Errors are marked as part of compilation, so we expect that a suitable ClassEvent
        // is generated when compilation finishes; no need for it here.
        setState(State.HAS_ERROR);
    }
    
    /**
     * Check whether there was a compilation error for this target, last time
     * compilation was attempted.
     */
    @OnThread(Tag.Any)
    public boolean hasKnownError()
    {
        return getState() == State.HAS_ERROR;
    }

    @Override
    public void recordShowErrorMessage(int identifier, List<String> quickFixes)
    {
        DataCollector.showErrorMessage(getPackage(), identifier, quickFixes);
    }

    @Override
    public void recordShowErrorIndicator(int identifier)
    {
        DataCollector.showErrorIndicator(getPackage(), identifier);
    }

    @Override
    public void recordEarlyErrors(List<DiagnosticWithShown> diagnostics)
    {
        if (diagnostics.isEmpty())
            return;

        DataCollector.compiled(getPackage().getProject(), getPackage(), new CompileInputFile[] {getCompileInputFile()}, diagnostics, false, CompileReason.EARLY, SourceType.Stride);
    }

    @Override
    public void recordLateErrors(List<DiagnosticWithShown> diagnostics)
    {
        if (diagnostics.isEmpty())
            return;

        DataCollector.compiled(getPackage().getProject(), getPackage(), new CompileInputFile[] {getCompileInputFile()}, diagnostics, false, CompileReason.LATE, SourceType.Stride);
    }

    @Override
    public void recordFix(int errorIdentifier, int fixIndex)
    {
        DataCollector.fixExecuted(getPackage(), errorIdentifier, fixIndex);
    }

    @Override
    public void recordCodeCompletionStarted(Integer lineNumber, Integer columnNumber, String xpath, Integer subIndex, String stem)
    {
        DataCollector.codeCompletionStarted(this, lineNumber, columnNumber, xpath, subIndex, stem);
    }

    @Override
    public void recordCodeCompletionEnded(Integer lineNumber, Integer columnNumber, String xpath, Integer elementOffset, String stem, String replacement)
    {
        DataCollector.codeCompletionEnded(this, lineNumber, columnNumber, xpath, elementOffset, stem, replacement);
    }

    @Override
    public void recordUnknownCommandKey(String enclosingFrameXpath, int cursorIndex, char key)
    {
        DataCollector.unknownFrameCommandKey(this, enclosingFrameXpath, cursorIndex, key);
    }

    @Override
    public @OnThread(Tag.FXPlatform) boolean isFront()
    {
        return isFront;
    }

    @Override
    public synchronized void showingInterface(boolean showing)
    {
        this.showingInterface = showing;
    }

    @Override
    public @OnThread(Tag.FXPlatform) void setCreatingExtends(boolean drawingExtends)
    {
        // Don't call super; we don't want to darken ourselves
        this.drawingExtends = drawingExtends;
    }

    @Override
    public @OnThread(Tag.FXPlatform) boolean cursorAtResizeCorner(MouseEvent e)
    {
        // Don't allow resize if we are picking an extends arrow:
        return super.cursorAtResizeCorner(e) && !drawingExtends;
    }

    @Override
    public void setDisplayName(String name)
    {
        super.setDisplayName(name);
        // Don't just use name; getDisplayName adds template params info
        String newDisplayName = getDisplayName();
        Platform.runLater(() -> nameLabel.setText(newDisplayName));
    }
}
