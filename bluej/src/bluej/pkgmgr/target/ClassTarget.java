/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021  Michael Kolling and John Rosenberg
 
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


import bluej.Config;
import bluej.collect.DataCollector;
import bluej.collect.DiagnosticWithShown;
import bluej.collect.StrideEditReason;
import bluej.compiler.CompileInputFile;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.compiler.Diagnostic;
import bluej.debugger.*;
import bluej.debugger.gentype.Reflective;
import bluej.debugmgr.objectbench.InvokeListener;
import bluej.editor.Editor;
import bluej.editor.TextEditor;
import bluej.editor.flow.FlowEditor;
import bluej.editor.stride.FrameCatalogue;
import bluej.editor.stride.FrameEditor;
import bluej.extensions2.*;
import bluej.extensions2.event.ClassEvent;
import bluej.extmgr.ExtensionsManager;
import bluej.parser.ParseFailure;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.PackageResolver;
import bluej.parser.entity.ParsedReflective;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedTypeNode;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.*;
import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.dependency.ExtendsDependency;
import bluej.pkgmgr.dependency.ImplementsDependency;
import bluej.pkgmgr.dependency.UsesDependency;
import bluej.pkgmgr.target.actions.*;
import bluej.pkgmgr.target.role.AbstractClassRole;
import bluej.pkgmgr.target.role.ClassRole;
import bluej.pkgmgr.target.role.EnumClassRole;
import bluej.pkgmgr.target.role.InterfaceClassRole;
import bluej.pkgmgr.target.role.StdClassRole;
import bluej.pkgmgr.target.role.UnitTestClassRole;
import bluej.pkgmgr.target.role.UnitTestClassRole.UnitTestFramework;
import bluej.prefmgr.PrefMgr;
import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.ast.Parser;
import bluej.stride.framedjava.convert.ConversionWarning;
import bluej.stride.framedjava.convert.ConvertResultDialog;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.TopLevelCodeElement;
import bluej.stride.generic.Frame;
import bluej.utility.*;
import bluej.utility.javafx.*;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.lang.ClassNotFoundException;

/**
 * A class target in a package, i.e. a target that is a class file built from
 * Java source code
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 * @author Bruce Quig
 */
@OnThread(Tag.FXPlatform)
public class ClassTarget extends DependentTarget
    implements InvokeListener
{
    final static int MIN_WIDTH = 60;
    final static int MIN_HEIGHT = 30;
    public final static String compileStr = Config.getString("pkgmgr.classmenu.compile");
    public final static String inspectStr = Config.getString("pkgmgr.classmenu.inspect");
    public final static String convertToJavaStr = Config.getString("pkgmgr.classmenu.convertToJava");
    public final static String convertToStrideStr = Config.getString("pkgmgr.classmenu.convertToStride");
    public final static String duplicateClassStr = Config.getString("pkgmgr.classmenu.duplicate");
    public final static String createTestStr = Config.getString("pkgmgr.classmenu.createTest");
    private final static String launchFXStr = Config.getString("pkgmgr.classmenu.launchFX");

    private static final String STEREOTYPE_OPEN = "\u00AB"; //"<<";
    private static final String STEREOTYPE_CLOSE = "\u00BB"; //">>";
    private static final double RESIZE_CORNER_GAP = 4;

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
    private Optional<Boolean> isNaviviewExpanded = Optional.empty();

    private final List<Integer> cachedBreakpoints = new ArrayList<>();

    // flag to prevent recursive calls to analyseDependancies()
    private boolean analysing = false;

    // Whether the current compilation is invalid due to edits since compilation began
    private boolean compilationInvalid = false;

    private boolean isMoveable = true;
    private SourceType sourceAvailable;
    // Part of keeping track of number of editors opened, for Greenfoot phone home:
    private boolean hasBeenOpened = false;

    private String typeParameters = "";

    //properties map to store values used in the editor from the props (if necessary)
    private Map<String, String> properties = new HashMap<String, String>();
    // Keep track of whether the editor is open or not; we get a lot of
    // potential open events, and don't want to keep recording ourselves as re-opening
    private boolean recordedAsOpen = false;
    private boolean visible = true;
    private static String[] pseudos;

    // The body of the class target which goes hashed, etc:
    @OnThread(Tag.FX)
    private ResizableCanvas canvas;
    private Label stereotypeLabel;
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
    private boolean showingInterface;
    private boolean drawingExtends = false;
    private Label nameLabel;
    private Label noSourceLabel;

    /**
     * Create a new class target in package 'pkg'.
     *
     * @param pkg      Description of the Parameter
     * @param baseName Description of the Parameter
     */
    public ClassTarget(Package pkg, String baseName)
    {
        this(pkg, baseName, null);
    }

    /**
     * Create a new class target in package 'pkg'.
     *
     * @param pkg      Description of the Parameter
     * @param baseName Description of the Parameter
     * @param template Description of the Parameter
     */
    public ClassTarget(Package pkg, String baseName, String template)
    {
        super(pkg, baseName, "Class");

        if (pseudos == null)
        {
            pseudos = Utility.mapList(Arrays.<Class<? extends ClassRole>>asList(StdClassRole.class, UnitTestClassRole.class, AbstractClassRole.class, InterfaceClassRole.class, EnumClassRole.class), ClassTarget::pseudoFor).toArray(new String[0]);
        }

        JavaFXUtil.addStyleClass(pane, "class-target");
        JavaFXUtil.addStyleClass(pane, "class-target-id-" + baseName);

        nameLabel = new Label(baseName);
        JavaFXUtil.addStyleClass(nameLabel, "class-target-name");
        nameLabel.setMaxWidth(9999.0);
        stereotypeLabel = new Label();
        stereotypeLabel.setMaxWidth(9999.0);
        stereotypeLabel.visibleProperty().bind(stereotypeLabel.textProperty().isNotEmpty());
        stereotypeLabel.managedProperty().bind(stereotypeLabel.textProperty().isNotEmpty());
        JavaFXUtil.addStyleClass(stereotypeLabel, "class-target-extra");
        pane.setTop(new VBox(stereotypeLabel, nameLabel));
        canvas = new ResizableCanvas()
        {
            @Override
            @OnThread(value = Tag.FXPlatform, ignoreParent = true)
            public void resize(double width, double height)
            {
                super.resize(width, height);
                redraw();
            }
        };
        pane.setCenter(canvas);

        // We need to add label to the stack pane element
        // to be used later for visual indication that class lacks source
        noSourceLabel = new Label("");
        StackPane stackPane = new StackPane(pane.getCenter(), noSourceLabel);
        StackPane.setAlignment(noSourceLabel, Pos.TOP_CENTER);
        StackPane.setAlignment(canvas, Pos.CENTER);
        pane.setCenter(stackPane);

        // This must come after GUI init because it might try to affect GUI:
        calcSourceAvailable();

        // we can take a guess at what the role is going to be for the
        // object based on the start of the template name. If we get this
        // wrong, its no great shame as it'll be fixed the first time they
        // successfully analyse/compile the source.
        if (template != null)
        {
            if (template.startsWith("unittest"))
            {
                setRole(new UnitTestClassRole(UnitTestFramework.JUnit5));
            }
            else if (template.startsWith("abstract"))
            {
                setRole(new AbstractClassRole());
            }
            else if (template.startsWith("interface"))
            {
                setRole(new InterfaceClassRole());
            }
            else if (template.startsWith("enum"))
            {
                setRole(new EnumClassRole());
            }
            else
            {
                setRole(new StdClassRole());
            }

        }
        JavaFXUtil.addChangeListener(canvas.sceneProperty(), scene -> {
            JavaFXUtil.runNowOrLater(() -> {
                nameLabel.applyCss();
                updateSize();
            });
        });
    }

    /**
     * Check whether the class has source, and of what type.
     */
    private void calcSourceAvailable()
    {
        if (getFrameSourceFile().canRead())
        {
            sourceAvailable = SourceType.Stride;
            noSourceLabel.setText("");
        }
        else if (getJavaSourceFile().canRead())
        {
            sourceAvailable = SourceType.Java;
            noSourceLabel.setText("");
        }
        else
        {
            sourceAvailable = SourceType.NONE;
            // Can't have been modified since compile since there's no source to modify:
            setState(State.COMPILED);
            noSourceLabel.setText("(" + Config.getString("classTarget.noSource") + ")");
        }
    }

    private BClass singleBClass;  // Every Target has none or one BClass

    /**
     * Return the extensions BProject associated with this Project.
     * There should be only one BProject object associated with each Project.
     *
     * @return the BProject associated with this Project.
     */
    public final BClass getBClass()
    {
        if (singleBClass == null)
        {
            singleBClass = ExtensionBridge.newBClass(this);
        }

        return singleBClass;
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
     * @return A suitable reflective, or null.
     */
    public Reflective getTypeReflective()
    {
        // If compiled, return a reflective based on actual reflection
        if (isCompiled())
        {
            Class<?> cl = getPackage().loadClass(getQualifiedName());
            if (cl != null)
            {
                return new JavaReflective(cl);
            }
            else
            {
                return null;
            }
        }

        // Not compiled; try to get a reflective from the parser
        ParsedCUNode node = null;
        if (getEditor() != null)
        {
            TextEditor textEditor = editor.assumeText();
            if (textEditor != null)
            {
                node = textEditor.getParsedNode();
            }
        }

        if (node != null)
        {
            ParsedTypeNode ptn = (ParsedTypeNode) node.getTypeNode(getBaseName());
            if (ptn != null)
            {
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
        return getBaseName() + getTypeParameters();
    }

    @OnThread(Tag.Any)
    public Package getPackage(){
        return super.getPackage();
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
    public void setState(State newState)
    {
        if (getState() != newState)
        {
            String qualifiedName = getQualifiedName();
            Project proj = getPackage().getProject();
            proj.removeInspectorInstance(qualifiedName);
            
            // Notify extensions if necessary.
            if (newState == State.COMPILED)
            {
                if (editor != null)
                {
                    editor.reInitBreakpoints();
                }
            }
            ClassEvent event = new ClassEvent(getPackage(), getBClass(), newState == State.COMPILED, newState == State.HAS_ERROR);
            ExtensionsManager.getInstance().delegateEvent(event);

            super.setState(newState);
        }
    }

    /**
     * Compilation of the class represented by this target has begun.
     * 
     * @param compilationSequence   compilation sequence identifier which can be used to associate
     *                              related compilation events.
     */
    public void markCompiling(int compilationSequence)
    {
        // The results of compilation will be invalid if the editor contents have not been saved:
        compilationInvalid = (editor != null) ? editor.isModified() : false; 
        
        if (getState() == State.HAS_ERROR)
        {
            setState(State.NEEDS_COMPILE);
        }

        if (getSourceType() == SourceType.Stride)
        {
            getEditor(); // Create editor if necessary
        }
        if (editor != null)
        {
            if (editor.compileStarted(compilationSequence))
            {
                setState(State.HAS_ERROR);
            }
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
    protected final void setRole(ClassRole newRole)
    {
        if (role == null || role.getRoleName() != newRole.getRoleName()) {
            role = newRole;

            String select = pseudoFor(role.getClass());
            String stereotype = role.getStereotypeLabel();
            boolean shouldBeFront = role == null || !(role instanceof UnitTestClassRole);
            isFront = shouldBeFront;
            JavaFXUtil.selectPseudoClass(pane, Arrays.asList(pseudos).indexOf(select), pseudos);
            if (stereotype != null)
                stereotypeLabel.setText(STEREOTYPE_OPEN + stereotype + STEREOTYPE_CLOSE);
            else
                stereotypeLabel.setText("");
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
     * Test if a given class is a JUnit 4 test class.
     * 
     * <p>In Junit4, test classes can be of any type.
     * The only way to test is to check if it has one of the following annotations:
     * @Before, @Test or @After<br/>Note: a test class may only contain the @Before
     * and @After annotations when created with BlueJ, so @Test alone is not a reliable
     * indicator for a test class.
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
     * Test if a given class is a JUnit 5 test class.
     *
     * <p>In Junit5, test classes can be of any type.
     * The only way to test is to check if it has one of the following annotations:
     * @BeforeEach, @Test or @AfterEach.<br/>Note: a test class may only contain the @BeforeEach
     * and @AfterEach annotation when created with BlueJ, so @Test alone is not a reliable
     * indicator for a test class.
     * For JUnit 5 we extend the detection of test classes to these common annotations:
     * @RepeatedTest.
     * @DisplayName.
     * @BeforeAll.
     * @AfterAll.
     * @Tag.
     * @Disabled.
     * @ParameterizedTest
     * @TestFactory
     * @Nested
     * @TestInstance
     * @SelectPackages (class)
     * @SelectClasses (class)

     * @param cl class to test
     */
    @SuppressWarnings("unchecked")
    public static boolean isJunit5TestClass(Class<?> cl)
    {
        ClassLoader clLoader = cl.getClassLoader();
        try
        {
            Class[] matchingMethodAnnotationsClassArray = {
                Class.forName("org.junit.jupiter.api.BeforeEach", false, clLoader),
                Class.forName("org.junit.jupiter.api.BeforeAll", false, clLoader),
                Class.forName("org.junit.jupiter.api.AfterEach", false, clLoader),
                Class.forName("org.junit.jupiter.api.AfterAll", false, clLoader),
                Class.forName("org.junit.jupiter.api.Test", false, clLoader),
                Class.forName("org.junit.jupiter.api.RepeatedTest", false, clLoader),
                Class.forName("org.junit.jupiter.api.DisplayName", false, clLoader),
                Class.forName("org.junit.jupiter.api.Tag", false, clLoader),
                Class.forName("org.junit.jupiter.api.TestFactory", false, clLoader),
                Class.forName("org.junit.jupiter.api.Disabled", false, clLoader),
                Class.forName("org.junit.jupiter.params.ParameterizedTest", false, clLoader),
                Class.forName("org.junit.jupiter.api.Nested", false, clLoader),
                Class.forName("org.junit.jupiter.api.TestInstance", false, clLoader)
            };

            Class[] matchingClassAnnotationsClassArray = {
                Class.forName("org.junit.platform.suite.api.SelectPackages", false, clLoader),
                Class.forName("org.junit.platform.suite.api.SelectClasses", false, clLoader)
            };

            Method[] methods = cl.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++)
            {
                for (Class matchingClass : matchingMethodAnnotationsClassArray)
                {
                    if (methods[i].getAnnotation(matchingClass) != null)
                    {
                        return true;
                    }
                }
            }

            for (Class matchingClass : matchingClassAnnotationsClassArray)
            {
                if (cl.getAnnotation(matchingClass) != null)
                {
                    return true;
                }
            }
        }
        catch (ClassNotFoundException cnfe)
        {
        }
        catch (LinkageError le)
        {
        }

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
            isAbstract = Modifier.isAbstract(cl.getModifiers());
            
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
            if (junitClass.isAssignableFrom(cl))
            {
                setRole(new UnitTestClassRole(UnitTestFramework.JUnit3));
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
            else if (isJunit4TestClass(cl)) {
                setRole(new UnitTestClassRole(UnitTestFramework.JUnit4));
            }
            else if (isJunit5TestClass(cl)) {
                setRole(new UnitTestClassRole(UnitTestFramework.JUnit5));
            }
            else {
                setRole(new StdClassRole());
            }
        }
        else {
            isAbstract = false;
            
            // try the parsed source code
            ClassInfo classInfo = sourceInfo.getInfoIfAvailable();

            if (classInfo != null) {
                if (classInfo.isUnitTest()) {
                    setRole(new UnitTestClassRole(UnitTestFramework.JUnit3));
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
                    if (!(role instanceof UnitTestClassRole))
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

        if (UnitTestClassRole.UNITTEST_ROLE_NAME.equals(type)) {
            setRole(new UnitTestClassRole(UnitTestFramework.JUnit3));
        }
        else if (UnitTestClassRole.UNITTEST_ROLE_NAME_JUNIT4.equals(type)) {
            setRole(new UnitTestClassRole(UnitTestFramework.JUnit4));
        }
        else if (UnitTestClassRole.UNITTEST_ROLE_NAME_JUNIT5.equals(type)) {
            setRole(new UnitTestClassRole(UnitTestFramework.JUnit5));
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
        
        typeParameters = "";
        // parameters will be corrected when class is analysed

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

        boolean intf;
        openWithInterface = showingInterface;
        intf = openWithInterface;

        //saving the state of the naviview (open/close) to the props
        //setting the value of the expanded according to the value from the editor (if there is)
        //else if there was a previous setting use that
        if (getProperty(NAVIVIEW_EXPANDED_PROPERTY) != null)
        {
            props.put(prefix + ".naviview.expanded", String.valueOf(getProperty(NAVIVIEW_EXPANDED_PROPERTY)));
        }
        else if (isNaviviewExpanded.isPresent())
        {
            props.put(prefix + ".naviview.expanded", String.valueOf(isNaviviewExpanded()));
        }
        
        props.put(prefix + ".showInterface", Boolean.valueOf(intf).toString());

        List<Integer> breakpoints;
        if (editor != null && editor instanceof FrameEditor)
        {
            breakpoints = ((FrameEditor)editor).getBreakpoints();
        }
        else
        {
            breakpoints = cachedBreakpoints;
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
     * Checks if the source file was modified in the future, and if so, set the modification time
     * to now.
     */
    public void fixSourceModificationDate()
    {
        // if just a .class file with no src, it better be up to date
        if (sourceAvailable == SourceType.NONE) {
            return;
        }

        File src = getSourceFile();

        // if the src file has last-modified date in the future, then set the last-modified date of
        // the source file to the current time.
        long now = Instant.now().toEpochMilli();
        // Tiny bit of leeway just in case of clock syncs, etc:
        if (src.exists() && (src.lastModified() > now + 1000))
        {
            src.setLastModified(now);
            if (editor != null)
            {
                // Important to use the File's lastModified here, rather than the now variable.
                // Some file systems (e.g. Mac) round to nearest second, so the set/get combo may
                // return a different result.  We want the one from the file system:
                editor.setLastModified(src.lastModified());
            }
        }
    }

    /**
     * Mark this class as modified, and mark all dependent classes too
     */
    public void invalidate()
    {
        invalidateInclDependents(new ArrayList<>());
    }

    /**
     * Implements invalidate().  We must keep track of which classes we've already invalidated
     * as we traverse the dependency graph to prevent an infinite loop (in the case where
     * A depends on B which depends on A).
     * @param alreadyInvalidated The list of already invalidated targets in this call tree (will be modified)
     */
    private void invalidateInclDependents(ArrayList<ClassTarget> alreadyInvalidated)
    {
        // Mark any current compilation as stale:
        compilationInvalid = true;
        
        if (hasSourceCode())
        {
            setState(State.NEEDS_COMPILE);
            if (editor != null)
            {
                // Need to run later because we might be notified mid-edit event:
                JavaFXUtil.runAfterCurrent(() -> editor.removeErrorHighlights());
            }
        }
        
        alreadyInvalidated.add(this);
        
        for (Dependency d : dependents())
        {
            ClassTarget dependent = (ClassTarget) d.getFrom();
            
            if (dependent.hasSourceCode() && !alreadyInvalidated.contains(dependent))
            {
                // Invalidate the dependent only if it is not already invalidated. 
                // Will avoid going into an infinite circular loop.
                dependent.invalidateInclDependents(alreadyInvalidated);
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
    public File getJavaSourceFile() {
        if (null == getPackage())
        {
            return null;
        }
        else
        {
            return new File(getPackage().getPath(), getBaseName() + "." + SourceType.Java.toString().toLowerCase());
        }
    }
    
    /**
     * @return the name of the Stride file this target corresponds to. This is only valid for Stride targets.
     */
    public File getFrameSourceFile()
    {
        if (null == getPackage())
        {
            return null;
        }
        else
        {
            return new File(getPackage().getPath(), getBaseName() + "." + SourceType.Stride.toString().toLowerCase());
        }
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

    public boolean isVisible()
    {
        return visible;
    }

    /**
     * Mark the class as having compiled, either successfully or not.
     */
    public void markCompiled(boolean successful, CompileType compileType)
    {
        if (compilationInvalid)
        {
            // We pass "classesKept" as false since the generated classes are invalid now:
            editor.compileFinished(successful, false);
            return;
        }
        
        if (successful && compileType.keepClasses())
        {
            // If the src file has last-modified date in the future, fix the date.
            // this will remove "uncompiled" stripes on the class
            fixSourceModificationDate();
            
            // Empty class files should not be marked compiled,
            // even though compilation is "successful".
            boolean newCompiledState = upToDate();
            newCompiledState &= !hasKnownError();
            if (newCompiledState)
            {
                setState(State.COMPILED);
            }
        }

        if (editor != null)
        {
            editor.compileFinished(successful, compileType.keepClasses());
            if (isCompiled())
            {
                editor.setCompiled(true);
            }
        }
        
        // Note: we assume that errors have already been marked, so there's no need to mark
        // an error state now for an unsuccessful compilation.
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
     * A filter to find inner class files.
     */
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
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
    public Editor getEditor()
    {
        boolean withInterface;
        withInterface = this.openWithInterface;
        return getEditor(withInterface);
    }

    /**
     * Gets the editor, if it is already open.  If not open, returns
     * null (without attempting to open it, in contrast to getEditor())
     */
    public Editor getEditorIfOpen()
    {
        return editor;
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


            final FXPlatformRunnable openCallback = () -> {
                recordEditorOpen();
                for (TargetListener stateListener : stateListeners)
                {
                    stateListener.editorOpened();
                }
            };
            if (sourceAvailable == SourceType.Java || sourceAvailable == SourceType.NONE) {
                editor = new FlowEditor(newWindow -> {
                    if (newWindow)
                    {
                        return project.createNewFXTabbedEditor();
                    }
                    else
                    {
                        return project.getDefaultFXTabbedEditor();
                    }
                }, getBaseName(), this, resolver, project.getJavadocResolver(), openCallback, PrefMgr.flagProperty(PrefMgr.HIGHLIGHTING), true);
                ((TextEditor)editor).showFile(filename, project.getProjectCharset(), isCompiled(), docFilename);
            }
            else if (sourceAvailable == SourceType.Stride) {
                File frameSourceFile = getFrameSourceFile();
                File javaSourceFile = getJavaSourceFile();
                JavadocResolver javadocResolver = project.getJavadocResolver();
                Package pkg = getPackage();
                editor = new FrameEditor(frameSourceFile, javaSourceFile, this, resolver, javadocResolver, pkg, openCallback);
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
     * 
     * @param parent Parent window.
     * @param animateFromCentre Animate from centre of this node.
     */
    public void inspect(Window parent, Node animateFromCentre)
    {
        Project proj = getPackage().getProject();

        new Thread() {
            @Override
            @OnThread(Tag.Worker)
            public void run() {
                // Try and load the class.
                try {
                    FXPlatformSupplier<DebuggerClass> clss = getPackage().getDebugger().getClass(getQualifiedName(), true);
                    Platform.runLater(() -> proj.getClassInspectorInstance(clss.get(), getPackage(), parent, animateFromCentre));
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
                
        removeBreakpoints();
        if (getPackage().getProject().getDebugger() != null)
        {
            getPackage().getProject().getDebugger().removeBreakpointsForClass(getQualifiedName());
        }
        if (isCompiled())
        {
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
            breakpoints = new ArrayList<>(this.cachedBreakpoints);
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

    @Override
    @OnThread(Tag.FXPlatform)
    public void scheduleCompilation(boolean immediate, CompileReason reason, CompileType type)
    {
        if (Config.isGreenfoot() && type == CompileType.EXPLICIT_USER_COMPILE)
        {
            // We compile the package rather than just the class for explicit compiles in
            // Greenfoot, but mark this target as modified first so that we do also compile
            // this class even if we wouldn't otherwise (and can report the result to the
            // editor, which is expecting to receive it):
            markModified();
            getPackage().getProject().scheduleCompilation(immediate, reason, type, getPackage());
        }
        else
        {
            getPackage().getProject().scheduleCompilation(immediate, reason, type, this);
        }
    }

    /**
     * Called when this class target has just been successfully compiled.
     * 
     * We load the compiled class if possible and check if the compilation has
     * resulted in it taking a different role (ie abstract to applet)
     */
    public void analyseAfterCompile()
    {
        Class<?> cl = getPackage().loadClass(getQualifiedName());

        determineRole(cl);
        analyseDependencies(cl);
        analyseTypeParams(cl);
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
                    success = role.generateSkeleton(template, getPackage(), getBaseName(), getJavaSourceFile().getPath());
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
                noSourceLabel.setText("");
                return true;
            }
            return false;
        }
    }

    /**
     * Inserts a package deceleration in the source file of this class, only if it
     * is not correct or if it does't exist. Also, the default package will be ignored.
     * 
     * @param packageName the package's name
     * @exception IllegalArgumentException if the package name is not a valid java identifier
     */
    public void enforcePackage(String packageName)
        throws IOException
    {
        if (getSourceType() != SourceType.Java)
        {
            // Only force packages in Java files
            return;
        }

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
     * <p>
     * Note: this should only be called once the containing package is loaded, not
     * before. All classes must be present in the package or dependency information
     * will be generated incorrectly during parsing.
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
    private void setTypeParameters(ClassInfo info)
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
        if (!newTypeParameters.equals(typeParameters))
        {
            typeParameters = newTypeParameters;
            updateDisplayName();
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
    private boolean analysePackageName(ClassInfo info)
    {
        String newName = info.getPackage();

        return (!getPackage().getQualifiedName().equals(newName));
    }

    /**
     * Analyse the current dependencies in the source code and update the
     * dependencies in the graphical display accordingly.
     */
    private void analyseDependencies(ClassInfo info)
    {
        // Now that uses dependencies are calculated-only, we remove all of them
        // and add back those which remain:
        removeAllOutDependencies();
        removeInheritDependencies();

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
            if (used != null)
            {
                UsesDependency dependency = new UsesDependency(getPackage(), this, used);
                getPackage().addDependency(dependency);
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
     * Analyse the type parameters from the compiled class and update the display name.
     */
    public <T> void analyseTypeParams(Class<T> cl)
    {
        if (cl != null) {
            String oldTypeParams = typeParameters;
            TypeVariable<Class<T>> [] tvars = cl.getTypeParameters();
            if (tvars.length == 0) {
                typeParameters = "";
            }
            else
            {
                boolean isFirst = true;
                typeParameters = "<";
                for (TypeVariable<?> tvar : tvars) {
                    if (! isFirst) {
                        typeParameters += ",";
                    }
                    isFirst = false;
                    typeParameters += tvar.getName();
                }
                typeParameters += ">";
            }
            
            if (! typeParameters.equals(oldTypeParams)) {
                updateDisplayName();
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
        if (getPackage().getTarget(newName) != null)
        {
            getEditor().writeMessage((Config.getString("editor.info.duplication")));
            return false;
        }

        File oldJavaSourceFile  = getJavaSourceFile();
        File newJavaSourceFile = new File(getPackage().getPath(), newName + "." + SourceType.Java.toString().toLowerCase());

        try {
            String filename;
            File oldFrameSourceFile = null;
            File newFrameSourceFile = null;
            getPackage().updateTargetIdentifier(this, getIdentifierName(), newName);

            if (getSourceType().equals(SourceType.Stride)) {
                newFrameSourceFile = new File(getPackage().getPath(), newName + "." + SourceType.Stride.toString().toLowerCase());
                oldFrameSourceFile = getFrameSourceFile();
                FileUtility.copyFile(oldFrameSourceFile, newFrameSourceFile);
                filename = newFrameSourceFile.getAbsolutePath();
            }
            else {
                filename = newJavaSourceFile.getAbsolutePath();
            }

            // Also copy the Java file across, in all cases:
            FileUtility.copyFile(oldJavaSourceFile, newJavaSourceFile);
            String javaFilename = newJavaSourceFile.getAbsolutePath();
            String docFilename = getPackage().getProject().getDocumentationFile(javaFilename);
            getEditor().changeName(newName, filename, javaFilename, docFilename);

            deleteSourceFiles();
            getClassFile().delete();
            // Delete subclass files like Foo$1.class, Foo$Inner.class
            for (File innerClassFile : getInnerClassFiles())
            {
                innerClassFile.delete();
            }
            
            getContextFile().delete();
            getDocumentationFile().delete();

            // this is extremely dangerous code here.. must track all
            // variables which are set when ClassTarget is first
            // constructed and fix them up for new class name
            String oldName = getIdentifierName();
            setIdentifierName(newName);
            updateDisplayName();
            
            // Update the BClass object
            BClass bClass = getBClass();
            ExtensionBridge.ChangeBClassName(bClass, getQualifiedName());

            DataCollector.renamedClass(getPackage(), oldFrameSourceFile, newFrameSourceFile, oldJavaSourceFile, newJavaSourceFile);

            // Take copy of listeners in case the rename causes new listeners to be added:
            for (TargetListener stateListener : new ArrayList<>(stateListeners))
            {
                stateListener.renamed(newName);
            }

            // Inform all listeners about the name change
            ClassEvent event = new ClassEvent(getPackage(), getBClass(), oldName);
            ExtensionsManager.getInstance().delegateEvent(event);

            return true;
        }
        catch (IOException ioe) {
            return false;
        }
    }

    /**
     * Update the displayed class name (which includes type parameters).
     */
    public void updateDisplayName()
    {
        String newDisplayName = getDisplayName();
        updateSize();
        nameLabel.setText(newDisplayName);
        setDisplayName(newDisplayName);
        updateAccessibleName();
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
                dstPkg.importFile(getSourceFile());
                prepareForRemoval();
                getPackage().removeTarget(this);
                close();
                return;
            }
        }

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
    }

    /**
     * Resizes the class so the entire classname + type parameter are visible
     *  
     */
    private void updateSize()
    {
        String displayName = getDisplayName();
        // Don't make size smaller if user has already resized
        // to larger than is needed for text width:
        int width = calculateWidth(nameLabel, displayName, (int)pane.getPrefWidth());
        setSize(width, getHeight());
        repaint();
    }

    @Override
    public List<? extends AbstractOperation<Target>> getContextOperations()
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
        
        ArrayList<AbstractOperation<Target>> ops = new ArrayList<>();

        // call on role object to add any options needed at top
        ops.addAll(role.getRoleOperationsBegin(this, cl, getState()));
        if (cl != null)
        {
            if (Application.class.isAssignableFrom(cl))
            {
                ops.add(new RunFXApplication(cl));
            }

            ops.addAll(role.getClassConstructorOperations(this, cl));
            ops.addAll(role.getClassStaticOperations(this, cl));
        }

        // call on role object to add any options needed at bottom
        ops.addAll(role.getRoleOperationsEnd(this, getState()));

        boolean sourceOrDocExists = sourceAvailable != SourceType.NONE || getDocumentationFile().exists();
        if (sourceOrDocExists)
            ops.add(new EditAction());
        if (sourceAvailable != SourceType.NONE)
            ops.add(new CompileAction());
        if (cl != null)
            ops.add(new InspectAction(null));
        ops.add(new RemoveClassAction());
        if (sourceAvailable != SourceType.NONE)
            ops.add(new DuplicateClassAction());
        Window parentWindow = pane.getScene().getWindow();
        if (sourceAvailable == SourceType.Stride)
            ops.add(new ConvertToJavaAction(parentWindow));
        else if (sourceAvailable == SourceType.Java && role.canConvertToStride())
            ops.add(new ConvertToStrideAction(parentWindow));

        return ops;
    }

    private void putFXLaunchResult(PackageEditor ed, Window fxWindow, CompletableFuture<FXPlatformSupplier<DebuggerResult>> result)
    {
        result.thenAccept(new Consumer<FXPlatformSupplier<DebuggerResult>>()
        {
            @Override
            @OnThread(Tag.Worker)
            public void accept(FXPlatformSupplier<DebuggerResult> supplier)
            {
                Platform.runLater(() -> {
                    DebuggerResult r = supplier.get();
                    switch (r.getExitStatus())
                    {
                        case Debugger.NORMAL_EXIT:
                            DebuggerObject obj = r.getResultObject();
                            ed.raisePutOnBenchEvent(fxWindow, obj, obj.getGenType(), null, false, Optional.empty());
                            break;
                    }
                });
            }
        });
    }

    /**
     * Converts this Java ClassTarget to Stride, as long as the user
     * says yes to the dialog that this method shows.
     *
     * If warnings (e.g. package-private access converted to protected)
     * are encountered during the conversion, a dialog is shown to the user
     * explaining them.  Most conversion issues (e.g. unconvertable items)
     * are warnings not errors.  Errors, which stop the process, mainly arise
     * from unparseable Java source code.
     */
    public void promptAndConvertJavaToStride(Window window)
    {
        File javaSourceFile = getJavaSourceFile();
        Charset projectCharset = getPackage().getProject().getProjectCharset();
        if (JavaFXUtil.confirmDialog("convert.to.stride.title", "convert.to.stride.message", window, true))
        {
            try
            {
                Parser.ConversionResult javaConvertResult = Parser.javaToStride(Files.readAllLines(javaSourceFile.toPath(), projectCharset).stream().collect(Collectors.joining("\n")), Parser.JavaContext.TOP_LEVEL, false);
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
                addStride((TopLevelCodeElement)elements.get(0));
                DataCollector.convertJavaToStride(getPackage(), javaSourceFile, getFrameSourceFile());
            }
            catch (IOException | ParseFailure pf)
            {
                Debug.reportError(pf);
                new ConvertResultDialog(pf.getMessage()).showAndWait();
                // Abort the conversion
            }
        }
    }

    /**
     * Process a double click on this target. That is: open its editor.
     *
     * @param  openInNewWindow if this is true, the editor opens in a new window
     */
    @Override
    public void doubleClick(boolean openInNewWindow)
    {
        Editor editor = getEditor();
        if(editor == null)
        {
            getPackage().showError("error-open-source");
        }
        editor.setEditorVisible(true, openInNewWindow);
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
        int w = Math.max(width, MIN_WIDTH);
        int h = Math.max(height, MIN_HEIGHT);
        super.setSize(w, h);
        if(assoc != null)
            assoc.setSize(w, h);
    }
    
    public void setVisible(boolean vis)
    {
        if (vis != this.visible) {
            this.visible = vis;
            pane.setVisible(vis);
        }
    }

    @OnThread(Tag.FXPlatform)
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
            g.setFill(hasKnownError() ? getRedStripeFill() : getGreyStripeFill());
            g.fillRect(0, 0, width, height);
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
     * Gets the grey diagonal stripe pattern used for modified classes.
     */
    @OnThread(Tag.FX)
    public static ImagePattern getGreyStripeFill()
    {
        int size = GREY_STRIPE_SEPARATION * 10;
        // Grey stripes
        if (greyStripeImage == null)
        {
            greyStripeImage = JavaFXUtil.createImage(size, size, gImage -> {
                JavaFXUtil.stripeRect(gImage, 0, 0, size, size, GREY_STRIPE_SEPARATION - STRIPE_THICKNESS, STRIPE_THICKNESS, false, GREY_STRIPE);
            });
        }
        return new ImagePattern(greyStripeImage, 0, 0, size, size, false);
    }

    /**
     * Gets the red diagonal stripe pattern used for classes with an error.
     */
    @OnThread(Tag.FX)
    public static ImagePattern getRedStripeFill()
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
        return new ImagePattern(redStripeImage, 0, 0, size, size, false);
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
                    d.setAssociation(null);
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
        File frameSourceFile = getSourceType().equals(SourceType.Stride) ? getFrameSourceFile() : null;
        File javaSourceFile = getJavaSourceFile();
        prepareForRemoval();
        Package pkg = getPackage();
        pkg.removeTarget(this);

        // Inform all listeners about the class removed
        ClassEvent event = new ClassEvent(getPackage(), getBClass());
        ExtensionsManager.getInstance().delegateEvent(event);
        
        // We must remove after the above, because it might involve saving, 
        // and thus recording edits to the file
        DataCollector.removeClass(pkg, frameSourceFile, javaSourceFile);


        // In Greenfoot we don't do detailed dependency tracking, so we just recompile the whole
        // package if any class is removed:
        if (Config.isGreenfoot())
            pkg.rebuild();
    }

    /**
     * Converts this ClassTarget from Stride to Java, by simply
     * deleting the Stride file and keeping the Java file which we've
     * always been generating from Stride for compilation purposes.
     *
     * This method shows no confirmation dialog/prompt; the caller is expected
     * to have taken care of that.
     *
     * Throws an exception if this is not a Stride ClassTarget.
     */
    public void convertStrideToJava()
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

    /**
     * Duplicates the class which is represented by this class target
     */
    public void duplicate()
    {
        String originalClassName = getBaseName();
        SourceType sourceType = getSourceType();
        PkgMgrFrame pmf = PkgMgrFrame.findFrame(getPackage());

        DuplicateClassDialog dialog = new DuplicateClassDialog(pmf.getWindow(), "CopyOf" + originalClassName, sourceType);
        Optional<String> duplicateClassName = dialog.showAndWait();
        duplicateClassName.ifPresent(name -> pmf.duplicateClass(originalClassName, name, getSourceFile(), sourceType));
    }

    /**
     * Adds the given stride code element as a Stride file for this
     * class target, as part of a conversion from Java into Stride, or part
     * of generating a new class from a template.
     *
     * @param element The source code content to put in the new .stride file.
     */
    private void addStride(TopLevelCodeElement element)
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
            JavaFXUtil.errorDialog(Config.getString("convert.to.stride.title"), e.getMessage());
            return;
        }
        sourceAvailable = SourceType.Stride;
    }

    public boolean isMoveable()
    {
        return isMoveable;
    }

    /**
     * Set whether this ClassTarget can be moved by the user (dragged around).
     * This is set false for unit tests which are associated with another class.
     * 
     */
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
        getPackage().callStaticMethodOrConstructor(mv);
    }
    
    /**
     * interactive constructor call
     */
    @Override
    public void callConstructor(ConstructorView cv)
    {
        getPackage().callStaticMethodOrConstructor(cv);
    }
    
    /**
     * Method to check state of debug VM (currently running may cause problems)
     * and then give options accordingly. 
     * Returns a value from user about how to continue i.e should the original requested be executed.
     * 
     * @return Whether the original request should be executed (dependent on how the user wants to proceed)
     */
    public boolean checkDebuggerState()
    {
        return ProjectUtils.checkDebuggerState(getPackage().getProject(), getPackage().getUI().getStage());
    }

    /**
     * Returns the naviview expanded value from the properties file
     * @return 
     */
    public boolean isNaviviewExpanded()
    {
        return isNaviviewExpanded.orElse(false);
    }

    /**
     * Sets the naviview expanded value from the properties file to this local variable
     * @param isNaviviewExpanded
     */
    public void setNaviviewExpanded(boolean isNaviviewExpanded)
    {
        this.isNaviviewExpanded = Optional.of(isNaviviewExpanded);
    }

    /**
     * Retrieves a property from the editor
     */
    @Override
    public String getProperty(String key)
    {
        return properties.get(key);
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
    public void recordJavaEdit(String latest, boolean includeOneLineEdits)
    {
        DataCollector.editJava(getPackage(), getJavaSourceFile(), latest, includeOneLineEdits);
    }

    @Override
    public void recordStrideEdit(String latestJava, String latestStride, StrideEditReason reason)
    {
        DataCollector.editStride(getPackage(), getJavaSourceFile(), latestJava, getFrameSourceFile(), latestStride, reason);
    }

    @Override
    public void recordClose()
    {
        if (hasSourceCode())
        {
            DataCollector.closeClass(getPackage(), getSourceFile());
        }
        recordedAsOpen = false;
    }

    @Override
    public void recordOpen()
    {
        if (recordedAsOpen == false && hasSourceCode())
        {
            DataCollector.openClass(getPackage(), getSourceFile());
            recordedAsOpen = true;
        }
    }

    @Override
    public void recordSelected()
    {
        if (hasSourceCode())
        {
            DataCollector.selectClass(getPackage(), getSourceFile());
        }
    }

    public CompileInputFile getCompileInputFile()
    {
        return new CompileInputFile(getJavaSourceFile(), getSourceFile());
    }

    /**
     * Display a compilation diagnostic (error message), if possible and appropriate. The editor
     * decides if it is appropriate to display the error and may have a policy where eg it only
     * shows a limited number of errors.
     * 
     * @param diagnostic   the compiler-generated diagnostic
     * @param errorIndex   the index of the error in this batch (first error is 0)
     * @param compileType  the type of compilation leading to the error
     * @return    true if the diagnostic was displayed to the user
     */
    public boolean showDiagnostic(Diagnostic diagnostic, int errorIndex, CompileType compileType)
    {
        // If an edit has been made since the compilation started, we don't want to display the
        // error since it may no longer be present, and if it is it will be shown by a later
        // compilation anyway:
        if (compilationInvalid)
        {
            return false;
        }
        
        Editor ed = getEditor();
        if (ed == null)
        {
            return false;
        }
        
        setState(State.HAS_ERROR);
        return ed.displayDiagnostic(diagnostic, errorIndex, compileType);
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
    public void recordShowErrorIndicators(Collection<Integer> identifiers)
    {
        DataCollector.showErrorIndicators(getPackage(), identifiers);
    }

    @Override
    public void recordEarlyErrors(List<DiagnosticWithShown> diagnostics, int compilationIdentifier)
    {
        if (diagnostics.isEmpty())
            return;

        DataCollector.compiled(getPackage().getProject(), getPackage(), new CompileInputFile[] {getCompileInputFile()}, diagnostics, false, CompileReason.EARLY, compilationIdentifier);
    }

    @Override
    public void recordLateErrors(List<DiagnosticWithShown> diagnostics, int compilationIdentifier)
    {
        if (diagnostics.isEmpty())
            return;

        DataCollector.compiled(getPackage().getProject(), getPackage(), new CompileInputFile[] {getCompileInputFile()}, diagnostics, false, CompileReason.LATE, compilationIdentifier);
    }

    @Override
    public void recordFix(int errorIdentifier, int fixIndex)
    {
        DataCollector.fixExecuted(getPackage(), errorIdentifier, fixIndex);
    }

    // See comment for DataCollector.codeCompletionStart
    @Override
    public void recordCodeCompletionStarted(Integer lineNumber, Integer columnNumber, String xpath, Integer subIndex, String stem, int codeCompletionId)
    {
        DataCollector.codeCompletionStarted(this, lineNumber, columnNumber, xpath, subIndex, stem, codeCompletionId);
    }

    // See comment for DataCollector.codeCompletionEnded
    @Override
    public void recordCodeCompletionEnded(Integer lineNumber, Integer columnNumber, String xpath, Integer elementOffset, String stem, String replacement, int codeCompletionId)
    {
        DataCollector.codeCompletionEnded(this, lineNumber, columnNumber, xpath, elementOffset, stem, replacement, codeCompletionId);
    }

    @Override
    public void recordUnknownCommandKey(String enclosingFrameXpath, int cursorIndex, char key)
    {
        DataCollector.unknownFrameCommandKey(this, enclosingFrameXpath, cursorIndex, key);
    }

    @Override
    public void recordShowHideFrameCatalogue(String enclosingFrameXpath, int cursorIndex, boolean show, FrameCatalogue.ShowReason reason)
    {
        DataCollector.showHideFrameCatalogue(getPackage().getProject(), getPackage(), enclosingFrameXpath, cursorIndex, show, reason);
    }

    @Override
    public void recordViewModeChange(String enclosingFrameXpath, int cursorIndex, Frame.View oldView, Frame.View newView, Frame.ViewChangeReason reason)
    {
        DataCollector.viewModeChange(getPackage(), getSourceFile(), enclosingFrameXpath, cursorIndex, oldView, newView, reason);
    }

    @Override
    public boolean isFront()
    {
        return isFront;
    }

    @Override
    public void showingInterface(boolean showing)
    {
        this.showingInterface = showing;
    }

    @Override
    public void setCreatingExtends(boolean drawingExtends)
    {
        // Don't call super; we don't want to darken ourselves
        this.drawingExtends = drawingExtends;
    }

    @Override
    public boolean cursorAtResizeCorner(MouseEvent e)
    {
        // Don't allow resize if we are picking an extends arrow:
        return super.cursorAtResizeCorner(e) && !drawingExtends;
    }

    private static class RunFXApplication extends ClassTargetOperation
    {
        private final Class<?> cl;

        public RunFXApplication(Class<?> cl)
        {
            super("runFX", Combine.ONE, null, launchFXStr, MenuItemOrder.RUN_FX, MENU_STYLE_INBUILT);
            this.cl = cl;
        }

        @Override
        protected void execute(ClassTarget target)
        {
            PackageEditor ed = target.getPackage().getEditor();
            Window fxWindow = ed.getFXWindow();
            if (target.getPackage().getProject().getRunOnThread() == null)
            {
                // We've never asked if they want to run on FX; ask now
                int result = DialogManager.askQuestionFX(fxWindow, "run-on-fx");
                if (result == 0)
                    target.getPackage().getProject().setRunOnThread(RunOnThread.FX);
                else
                    target.getPackage().getProject().setRunOnThread(RunOnThread.DEFAULT);
            }

            CompletableFuture<FXPlatformSupplier<DebuggerResult>> result = target.getPackage().getDebugger().launchFXApp(cl.getName());
            target.putFXLaunchResult(ed, fxWindow, result);
        }
    }
}
