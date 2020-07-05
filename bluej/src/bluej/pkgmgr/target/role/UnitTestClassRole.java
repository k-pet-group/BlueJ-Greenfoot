/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2014,2016,2017,2019,2020  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr.target.role;

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.debugger.DebuggerObject;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.editor.TextEditor;
import bluej.parser.SourceLocation;
import bluej.parser.SourceSpan;
import bluej.parser.UnitTestAnalyzer;
import bluej.pkgmgr.PackageEditor;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.TestRunnerThread;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.DependentTarget.State;
import bluej.pkgmgr.target.Target;
import bluej.pkgmgr.target.actions.ClassTargetOperation;
import bluej.terminal.Terminal;
import bluej.testmgr.TestDisplayFrame;
import bluej.testmgr.record.ExistingFixtureInvokerRecord;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.JavaNames;
import bluej.utility.JavaUtils;
import bluej.utility.javafx.AbstractOperation;
import bluej.utility.javafx.FXPlatformSupplier;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.dialog.InputDialog;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.RepeatedTest;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bluej.pkgmgr.target.EditableTarget.MENU_STYLE_INBUILT;

/**
 * A role object for Junit unit tests.
 *
 * @author  Andrew Patterson
 */
public class UnitTestClassRole extends ClassRole
{
    public static final String UNITTEST_ROLE_NAME = "UnitTestTarget";
    public static final String UNITTEST_ROLE_NAME_JUNIT4 = "UnitTestTargetJunit4";
    public static final String UNITTEST_ROLE_NAME_JUNIT5 = "UnitTestTargetJunit5";

    private static final String testAll = Config.getString("pkgmgr.test.popup.testAll");
    private static final String createTest = Config.getString("pkgmgr.test.popup.createTest");
    private static final String benchToFixture = Config.getString("pkgmgr.test.popup.benchToFixture");
    private static final String fixtureToBench = Config.getString("pkgmgr.test.popup.fixtureToBench");
    
    /** Unit test framework:
     *  JUnit 5, 4 or 3
     */
    public enum UnitTestFramework{
        JUnit5,
        JUnit4,
        JUnit3
    }

    @OnThread(Tag.Any)
    private UnitTestFramework unitTestFramework;
    
    /**
     * Create the unit test class role.
     */
    public UnitTestClassRole(UnitTestFramework framework)
    {
        unitTestFramework = framework;
    }

    @Override
    @OnThread(Tag.Any)
    public String getRoleName()
    {
        String roleName = null;
        switch (unitTestFramework) {
            case JUnit5:
                roleName = UNITTEST_ROLE_NAME_JUNIT5;
                break;
            case JUnit4:
                roleName = UNITTEST_ROLE_NAME_JUNIT4;
                break;
            case JUnit3:
                roleName = UNITTEST_ROLE_NAME;
                break;
        }
        return roleName;
    }

    @Override
    @OnThread(Tag.Any)
    public String getStereotypeLabel()
    {
        return "unit test";
    }


    @SuppressWarnings("unchecked")
    @OnThread(Tag.Any)
    private boolean isJUnitTestMethod(Method m)
    {
        Class<?> cl = m.getDeclaringClass();
        ClassLoader classLoader = cl.getClassLoader();

        switch (unitTestFramework)
        {
            case JUnit5:
                Class<org.junit.jupiter.api.Test> testClassJU5;
                Class<ParameterizedTest> paramTestClassJU5;
                Class<RepeatedTest> repeatedTestClassJU5;
                Class<Disabled> disabledTestClassJU5;
                try
                {
                    if (classLoader == null)
                    {
                        testClassJU5 = org.junit.jupiter.api.Test.class;
                        paramTestClassJU5 = ParameterizedTest.class;
                        repeatedTestClassJU5 = RepeatedTest.class;
                        disabledTestClassJU5 = Disabled.class;
                    }
                    else
                    {
                        testClassJU5 = (Class<org.junit.jupiter.api.Test>) classLoader.loadClass("org.junit.jupiter.api.Test");
                        paramTestClassJU5 = (Class<ParameterizedTest>) classLoader.loadClass("org.junit.jupiter.params.ParameterizedTest");
                        repeatedTestClassJU5 = (Class<RepeatedTest>) classLoader.loadClass("org.junit.jupiter.api.RepeatedTest");
                        disabledTestClassJU5 = (Class<Disabled>) classLoader.loadClass("org.junit.jupiter.api.Disabled");
                    }

                    // Disabled methods are excluded from the tests
                    if (m.getAnnotation(disabledTestClassJU5) == null &&
                        (m.getAnnotation(testClassJU5) != null || m.getAnnotation(paramTestClassJU5) != null || m.getAnnotation(repeatedTestClassJU5) != null))
                    {
                        if (!Modifier.isPublic(m.getModifiers())) return false;
                        if (m.getAnnotation(paramTestClassJU5) != null && m.getParameterTypes().length == 0) return false;
                        // A @Test or @RepeatTest test method can be with or without arguments.. No need to check.
                        return true;
                    }
                }
                catch (ClassNotFoundException cnfe)
                {
                }
                catch (LinkageError le)
                {
                }

                // No suitable annotations found, so not a test class
                break;
            case JUnit4:
                Class<Test> testClassJU4;
                cl = m.getDeclaringClass();
                classLoader = cl.getClassLoader();
                try
                {
                    if (classLoader == null)
                    {
                        testClassJU4 = org.junit.Test.class;
                    }
                    else
                    {
                        testClassJU4 = (Class<Test>) classLoader.loadClass("org.junit.Test");
                    }

                    if (m.getAnnotation(testClassJU4) != null)
                    {
                        if (!Modifier.isPublic(m.getModifiers())) return false;
                        if (m.getParameterTypes().length != 0) return false;
                        return true;
                    }
                }
                catch (ClassNotFoundException cnfe)
                {
                }
                catch (LinkageError le)
                {
                }

                // No suitable annotations found, so not a test class
                break;
            case JUnit3:
                // look for reasons to not include this method as a test case
                if (!m.getName().startsWith("test")) return false;
                if (!Modifier.isPublic(m.getModifiers())) return false;
                if (m.getParameterTypes().length != 0) return false;
                if (!m.getReturnType().equals(Void.TYPE)) return false;
                return true;
        }

        return false;
    }
    
    /**
     * Generate a popup menu for this TestClassRole.
     * @param menu the menu
     * @param ct the BlueJ class object target
     * @param cl the class object that is represented by this target
     * @param state the current state of BlueJ
     * @return false
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public List<ClassTargetOperation> getRoleOperationsBegin(ClassTarget ct, Class<?> cl, State state)
    {
        boolean enableTestAll = false;
        if (!ct.getPackage().getProject().inTestMode() && state == State.COMPILED && cl != null && ! ct.isAbstract()) {
            enableTestAll = (Arrays.stream(cl.getMethods())
                    .filter(this::isJUnitTestMethod)
                    .count() > 0);
        }

        // add run all tests option
        if (enableTestAll)
            return List.of(new TestAction(testAll, AbstractOperation.MenuItemOrder.TEST_ALL, ct.getPackage().getEditor(), null));
        
        return List.of();
    }
    
    /**
     * creates a class menu containing any constructors and static methods etc.
     *
     * @param cl Class object associated with this class target
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public List<ClassTargetOperation> getClassConstructorOperations(ClassTarget ct, Class<?> cl)
    {
        boolean hasEntries = false;

        Stream<Method> allMethods = Arrays.stream(cl.getMethods())
                .filter(this::isJUnitTestMethod)
                .sorted(Comparator.comparing(Method::getName));
        
        ArrayList<ClassTargetOperation> ops = new ArrayList<>();
        
        if (! ct.isAbstract()) {
            // If we have a lot of items, we should create a submenu to fold some items in
            // 28 is a wild guess for now.
            int itemHeight = 28;
            int itemsOnScreen = (int)Config.screenBounds.getHeight() / itemHeight;
            int sizeLimit = itemsOnScreen / 2;

            for(Method m : allMethods.collect(Collectors.toList())){
                String rtype;
                try {
                    rtype = JavaUtils.getJavaUtils().getReturnType(m).toString(true);
                }
                catch (ClassNotFoundException cnfe) {
                    rtype = m.getReturnType().getName();
                }
                // With Junit5, tests can have arguments, so we need to add them in the action name so they can be run properly
                StringBuilder args = new StringBuilder("(");
                if (m.getParameterTypes().length > 0)
                {
                    args.append(Arrays.stream(m.getParameterTypes()).map(Class::getName).collect(Collectors.joining(", ")));
                }
                args.append(")");
                TargetAbstractAction testAction = new TestAction(rtype + " " + m.getName() + args, AbstractOperation.MenuItemOrder.RUN_METHOD,
                        ct.getPackage().getEditor(), m.getName()+args);

                ops.add(testAction);
                hasEntries = true;
            }
            if (!hasEntries) {
                ops.add(new DummyDisabledOperation(Config.getString("pkgmgr.test.popup.noTests"), AbstractOperation.MenuItemOrder.RUN_METHOD));
            }
        }
        else {
            ops.add(new DummyDisabledOperation(Config.getString("pkgmgr.test.popup.abstract"), AbstractOperation.MenuItemOrder.RUN_METHOD));
        }
        return ops;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public List<ClassTargetOperation> getClassStaticOperations(ClassTarget ct,  Class<?> cl)
    {
        if (!ct.getPackage().getProject().inTestMode() && ct.hasSourceCode() && ! ct.isAbstract())
        {
            return List.of(
                    new MakeTestCaseAction(ct.getPackage().getEditor()),
                    new BenchToFixtureAction(ct.getPackage().getEditor()),
                    new FixtureToBenchAction(ct.getPackage().getEditor()));
        }
        return List.of();
    }

    @Override
    @OnThread(Tag.Any)
    public boolean canConvertToStride()
    {
        return false; // annotations needed for JUnit are not supported
    }

    @Override
    public void run(final PkgMgrFrame pmf, final ClassTarget ct, final String param)
    {
        Terminal terminal = pmf.getPackage().getProject().getTerminal();
        if (terminal.clearOnMethodCall())
        {
            terminal.clear();

        }
        if (param != null) {
            // Only running a single test
            Project proj = pmf.getProject();
            TestDisplayFrame.getTestDisplay().startTest(proj, 1);
        }

        new TestRunnerThread(pmf, ct, param).start();
    }
    
    /**
     * Set up a test run. This just involves going through the methods in the class
     * and creating a list of those which are test methods.
     * 
     * @param pmf   The package manager frame
     * @param ct    The class target
     * @param trt   The test runner thread
     * @return The list of test methods in the class, or null if we could not find out
     */
    public List<String> startRunTest(PkgMgrFrame pmf, ClassTarget ct, TestRunnerThread trt)
    {
        Class<?> cl = pmf.getPackage().loadClass(ct.getQualifiedName());

        if (cl == null)
            return null;

        // Test the whole class:
        List<String> testMethods = Arrays.stream(cl.getMethods())
            .filter(this::isJUnitTestMethod)
            .map(Method::getName)
            .sorted()
            .collect(Collectors.toList());

        Project proj = pmf.getProject();
        TestDisplayFrame.getTestDisplay().startTest(proj, testMethods.size());
        return testMethods;
    }

    /**
     * Get the count of test methods in the test class.
     * This only counts the methods per name: if a JUnit 5 test
     * can run the method several times it is still counted as one.
     * @param ct  The ClassTarget of the unit test class
     * @return    the number of test methods in the unit test class
     */
    public int getTestCount(ClassTarget ct)
    {
        if (!ct.isCompiled()) {
            return 0;
        }

        Class<?> cl = ct.getPackage().loadClass(ct.getQualifiedName());
        if (cl == null) {
            return 0;
        }

        return (int) Arrays.stream(cl.getMethods()).filter(this::isJUnitTestMethod).count();
    }

    /**
     * Start the construction of a test method.
     * 
     * This method prompts the user for a test method name and then sets up
     * all the variables for constructing a new test method.
     * 
     * @param pmf  the PkgMgrFrame this is all occurring in
     * @param ct   the ClassTarget of the unit test class
     */
    @OnThread(Tag.FXPlatform)
    public void doMakeTestCase(final PkgMgrFrame pmf, final ClassTarget ct)
    {
        // prompt for a new test name
        String newTestName = new TestNameDialog("unittest-new-test-method", "").showAndWait().orElse(null);
        if (newTestName == null)
            return;


        // find out if the method already exists in the unit test src
        try {
            Charset charset = pmf.getProject().getProjectCharset();
            UnitTestAnalyzer uta = analyzeUnitTest(ct, charset);

            SourceSpan existingSpan = uta.getMethodBlockSpan(newTestName);

            if (existingSpan != null)
            {
                if (DialogManager.askQuestionFX(pmf.getWindow(), "unittest-method-present") == 1)
                {
                    // Don't do anything
                }
                else
                {
                    finishTestCase(pmf, ct, newTestName);
                }
            }
            else
            {
                finishTestCase(pmf, ct, newTestName);
            }
        }
        catch (IOException ioe) {
            DialogManager.showErrorWithTextFX(pmf.getWindow(), "unittest-io-error", ioe.getLocalizedMessage());
            Debug.reportError("Error reading unit test source", ioe);
            finishTestCase(pmf, ct, newTestName);
        }
    }

    @OnThread(Tag.FXPlatform)
    private void finishTestCase(PkgMgrFrame pmf, ClassTarget ct, String newTestName)
    {
        pmf.testRecordingStarted(Config.getString("pkgmgr.test.recording") + " "
                + ct.getBaseName() + "." + newTestName + "()");

        pmf.getProject().removeClassLoader();

        runTestSetup(pmf, ct, false);

        pmf.getObjectBench().resetRecordingInteractions();
        pmf.setTestInfo(newTestName, ct);
    }

    /**
     * Analyze a unit test file.
     * @param ct  The classtarget representing the unit test class to analyze
     * @return  A UnitTestAnalyzer object with information about the unit test class
     * @throws IOException  if the source file can't be saved or read
     */
    private UnitTestAnalyzer analyzeUnitTest(ClassTarget ct, Charset fileEncoding) throws IOException
    {
        ct.ensureSaved();
        
        UnitTestAnalyzer uta = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(ct.getSourceFile());
            Reader reader = new InputStreamReader(fis, fileEncoding);
            uta = new UnitTestAnalyzer(reader);
        }
        catch (FileNotFoundException fnfe) {
            throw fnfe;
        }
        finally {
            if (fis != null) {
                try {
                    fis.close();
                }
                catch (IOException ioe) {
                    // shouldn't happen
                    Debug.reportError(ioe);
                }
            }
        }
        
        return uta;
    }
    
    /**
     * Run the test setup.
     * @param pmf  The package manager frame to run the setup in
     * @param ct   The classtarget for the test class
     */
    private void runTestSetup(final PkgMgrFrame pmf, final ClassTarget ct, final boolean recordAsFixtureToBench)
    {
        Project project = pmf.getProject();

        // Avoid running test setup (which is user code) on the event thread.
        // Run it on a new thread instead.
        new Thread() {
            @OnThread(value = Tag.Worker, ignoreParent = true)
            public void run() {

                final FXPlatformSupplier<Map<String, DebuggerObject>> dobs = project.getDebugger().runTestSetUp(ct.getQualifiedName());
                
                Platform.runLater(() -> {
                    List<DataCollector.NamedTyped> recordObjects = new ArrayList<DataCollector.NamedTyped>();
                    Iterator<Map.Entry<String,DebuggerObject>> it = dobs.get().entrySet().iterator();

                    while(it.hasNext()) {
                        Map.Entry<String,DebuggerObject> mapent = it.next();
                        DebuggerObject objVal = mapent.getValue();

                        if (! objVal.isNullObject()) {
                            String actualName = pmf.putObjectOnBench(mapent.getKey(), objVal, objVal.getGenType(), null, Optional.empty());
                            recordObjects.add(new DataCollector.NamedTyped(actualName, objVal.getClassName()));
                        }
                    }

                    if (recordAsFixtureToBench)
                    {
                        DataCollector.fixtureToObjectBench(pmf.getPackage(), ct.getSourceFile(), recordObjects);
                    }
                });
            }
        }.start();
    }

    private static final String spaces = "                                 ";
    
    /**
     * Get a string of whitespace corresponding to an indentation.
     */
    private String getIndentString()
    {
        int ts = Math.min(Config.getPropInteger("bluej.editor.tabsize", 4), spaces.length());
        return spaces.substring(0, ts);
    }
    
    /**
     * End the construction of a test method.
     * <p>
     * This method is responsible for actually created the source code for a
     * just-recorded test method.
     * 
     * @param pmf   the PkgMgrFrame this is all occurring in
     * @param ct    the ClassTarget of the unit test class
     * @param name  the name of the test method we are writing out
     */
    public void doEndMakeTestCase(PkgMgrFrame pmf, ClassTarget ct, String name)
    {
        TextEditor ed = ct.getEditor().assumeText();
        String ts = getIndentString();
        try {
            Charset charset = pmf.getProject().getProjectCharset();
            UnitTestAnalyzer uta = analyzeUnitTest(ct, charset);

            SourceSpan existingSpan = uta.getMethodBlockSpan(name);

            if (existingSpan != null) {
                // replace this method (don't replace the method header!)
                ed.setSelection(new SourceLocation(existingSpan.getStartLine(), existingSpan.getStartColumn()),
                                  new SourceLocation(existingSpan.getEndLine(), existingSpan.getEndColumn()));
                ed.insertText("{\n" + pmf.getObjectBench().getTestMethod(ts + ts) + ts + "}", false);
            }
            else
            {
                // insert a complete method
                SourceLocation methodInsert = uta.getNewMethodInsertLocation();

                if (methodInsert != null)
                {
                    ed.setSelection(new SourceLocation(methodInsert.getLine(), methodInsert.getColumn()), new SourceLocation(methodInsert.getLine(), methodInsert.getColumn() + 1));
                    switch (unitTestFramework)
                    {
                        case JUnit5:
                        case JUnit4:
                            ed.insertText("\n" + ts + "@Test\n" + ts + "public void " + name + "()\n" + ts + "{\n"
                                    + pmf.getObjectBench().getTestMethod(ts + ts) + ts + "}\n}\n", false);
                            break;
                        case JUnit3:
                            ed.insertText("\n" + ts + "public void " + name + "()\n" + ts + "{\n"
                                    + pmf.getObjectBench().getTestMethod(ts + ts) + ts + "}\n}\n", false);
                            break;
                    }
                }
            }
            
            ed.save();
        }
        catch (IOException ioe) {
            PkgMgrFrame.showMessageWithText(pmf.getPackage(), "generic-file-save-error", ioe.getLocalizedMessage());
        }
    }
    
    /**
     * Turn the fixture declared in a unit test class into a set of
     * objects on the object bench.
     * 
     * @param pmf  the PkgMgrFrame that will hold the object bench
     * @param ct   the ClassTarget of the unit test class
     */
    public void doFixtureToBench(PkgMgrFrame pmf, ClassTarget ct)
    {
        TextEditor ed = ct.getEditor().assumeText();

        // our first step is to save all the existing code that creates the
        // fixture into a special invoker record
        // this can then be used to recreate this fixture from the object
        // bench if needed
        ExistingFixtureInvokerRecord existing = new ExistingFixtureInvokerRecord();
        
        try {
            Charset charset = pmf.getProject().getProjectCharset();
            UnitTestAnalyzer uta = analyzeUnitTest(ct, charset);

            // iterate through all the declarations of fields (fixture items) in the class
            List<SourceSpan> fixtureSpans = uta.getFieldSpans();
            ListIterator<SourceSpan> it = fixtureSpans.listIterator();
                
            while(it.hasNext()) {
                SourceSpan variableSpan = it.next();
                String fieldDecl = ed.getText(variableSpan.getStartLocation(), variableSpan.getEndLocation()); 
                existing.addFieldDeclaration(fieldDecl);
            }

            // find the source code of the "setUp" method
            SourceSpan setUpSpan = uta.getMethodBlockSpan("setUp");

            if (setUpSpan != null) {
                String setUpWithBrackets = ed.getText(setUpSpan.getStartLocation(), setUpSpan.getEndLocation());
                // copy everything between the opening { and the final }
                String setUpWithoutBrackets = 
                        setUpWithBrackets.substring(setUpWithBrackets.indexOf('{') + 1,
                                                    setUpWithBrackets.lastIndexOf('}')).trim();
                existing.setSetupMethod(setUpWithoutBrackets);
            }
            
        }
        catch (IOException ioe) {
            PkgMgrFrame.showMessageWithText(pmf.getPackage(), "generic-file-save-error", ioe.getLocalizedMessage());
        }
        
        runTestSetup(pmf, ct, true);
        
        pmf.getObjectBench().addInteraction(existing);
    }   
    
    /**
     * Convert the objects on the object bench into a test fixture.
     */
    public void doBenchToFixture(PkgMgrFrame pmf, ClassTarget ct)
    {
        if(pmf.getObjectBench().getObjectCount() == 0) {
            return;
        }
                
        TextEditor ed = ct.getEditor().assumeText();
        try {
            Charset charset = pmf.getProject().getProjectCharset();
            UnitTestAnalyzer uta = analyzeUnitTest(ct, charset);

            // find all the fields declared in this unit test class
            List<SourceSpan> variables = uta.getFieldSpans();
            
            // if we already have fields, ask if we are sure we want to get rid of them
            if (variables != null && variables.size() > 0) {
                boolean shouldContinue = DialogManager.askQuestionFX(null, "unittest-fixture-present") != 1;

                if (!shouldContinue)
                    return;
            }

            // if we have fields, we need to nuke them
            // we need to make sure we delete these in reverse order (from the last
            // field to the first) or else when we delete them, we change the line
            // numbers for the following ones
            if (variables != null) {
                // start iterating from the last element
                ListIterator<SourceSpan> it = variables.listIterator(variables.size());
                
                while(it.hasPrevious()) {
                    SourceSpan variableSpan = (SourceSpan) it.previous();
                    
                    ed.setSelection(new SourceLocation(variableSpan.getStartLine(), variableSpan.getStartColumn()),
                                     new SourceLocation(variableSpan.getEndLine(), variableSpan.getEndColumn()));
                    ed.insertText("", false);
                }
                
                // to get correct locations for rewriting setUp(), we need to reparse
                uta = analyzeUnitTest(ct, charset);
            }

            // find a location to insert new methods
            SourceLocation fixtureInsertLocation = uta.getFixtureInsertLocation();
            
            // sanity check.. this shouldn't ever be null but if it is, lets not
            // make it worse by trying to edit the source
            if (fixtureInsertLocation == null) {
                return;
            }
            
            {
                List<String> names = new ArrayList<String>();
                for (ObjectWrapper obj : pmf.getObjectBench().getObjects())
                {
                    names.add(obj.getName());
                }
                DataCollector.objectBenchToFixture(pmf.getPackage(), ct.getSourceFile(), names);
            }
            
            // find the curly brackets for the setUp() method
            SourceSpan setupSpan = uta.getMethodBlockSpan("setUp");

            String ts = getIndentString();
            
            // rewrite the setUp() method of the unit test (if it exists)
            if (setupSpan != null) {
                ed.setSelection(new SourceLocation(setupSpan.getStartLine(), setupSpan.getStartColumn()),
                                 new SourceLocation(setupSpan.getEndLine(), setupSpan.getEndColumn()));
            } else {
                // otherwise, we will be inserting a brand new setUp() method
                ed.setSelection(
                    new SourceLocation(fixtureInsertLocation.getLine(), fixtureInsertLocation.getColumn()),
                    new SourceLocation(fixtureInsertLocation.getLine(), fixtureInsertLocation.getColumn() + 1));
                switch (unitTestFramework)
                {
                    case JUnit5:
                        ed.insertText("{\n" + ts + "@BeforeEach\n" + ts + "public void setUp()\n" + ts, false);
                        break;
                    case JUnit4:
                        ed.insertText("{\n" + ts + "@Before\n" + ts + "public void setUp()\n" + ts, false);
                        break;
                    case JUnit3:
                        ed.insertText("{\n" + ts + "public void setUp()\n" + ts, false);
                        break;
                }
            }
            
            // insert the code for our setUp() method
            ed.insertText("{\n" + pmf.getObjectBench().getFixtureSetup(ts + ts)
                                + ts + "}", false);

            // insert our new fixture declarations
            ed.setSelection(
                new SourceLocation(fixtureInsertLocation.getLine(), fixtureInsertLocation.getColumn()),
                new SourceLocation(fixtureInsertLocation.getLine(), fixtureInsertLocation.getColumn() + 1));
                
            ed.insertText("{\n" + pmf.getObjectBench().getFixtureDeclaration(ts), false);
            ed.save();
        }
        catch (IOException ioe) {
            PkgMgrFrame.showMessageWithText(pmf.getPackage(),
                    "generic-file-save-error", ioe.getLocalizedMessage());
        }
        
        pmf.getPackage().compileQuiet(ct, CompileReason.MODIFIED, CompileType.INTERNAL_COMPILE);
        
        pmf.getProject().removeClassLoader();
        pmf.getProject().newRemoteClassLoaderLeavingBreakpoints();
    }
    
    /**
     * A base class for all our actions that run on targets.
     */
    @OnThread(Tag.FXPlatform)
    private abstract class TargetAbstractAction extends ClassTargetOperation
    {
        protected PackageEditor ped;

        public TargetAbstractAction(String name, String label, MenuItemOrder menuItemOrder, PackageEditor ped)
        {
            super(name, Combine.ONE, null, label, menuItemOrder, MENU_STYLE_INBUILT);
            this.ped = ped;
        }
    }

    /**
     * A TestAction is an action that causes a JUnit test to be run on a class.
     * If testName is not provided, it is set to null which means that the whole
     * test class is run; otherwise it refers to a test method that should be run
     * individually.
     */
    @OnThread(Tag.FXPlatform)
    private class TestAction extends TargetAbstractAction
    {
        private String testName;
                    
        public TestAction(String label, MenuItemOrder menuItemOrder, PackageEditor ped, String testName)
        {
            super("test_" + testName, label, menuItemOrder, ped);
            this.testName = testName;
        }

        @Override
        protected void execute(ClassTarget target)
        {
            ped.runTest(target, testName);
        }
    }

    @OnThread(Tag.FXPlatform)
    private class MakeTestCaseAction extends TargetAbstractAction
    {
        public MakeTestCaseAction(PackageEditor ped)
        {
            super("makeTestCase", createTest, MenuItemOrder.MAKE_TEST_CASE, ped);
        }

        @Override
        protected void execute(ClassTarget target)
        {
            ped.makeTestCase(target);
        }
    }

    @OnThread(Tag.FXPlatform)
    private class BenchToFixtureAction extends TargetAbstractAction
    {
        public BenchToFixtureAction(PackageEditor ped)
        {
            super("benchToFixture", benchToFixture, MenuItemOrder.BENCH_TO_FIXTURE, ped);
        }

        @Override
        protected void execute(ClassTarget target)
        {
            ped.benchToFixture(target);
        }
    }

    @OnThread(Tag.FXPlatform)
    private class FixtureToBenchAction extends TargetAbstractAction
    {
        public FixtureToBenchAction(PackageEditor ped)
        {
            super("fixtureToBench", fixtureToBench, MenuItemOrder.FIXTURE_TO_BENCH, ped);
        }

        @Override
        protected void execute(ClassTarget target)
        {
            ped.fixtureToBench(target);
        }
    }

    @OnThread(Tag.FXPlatform)
    private class TestNameDialog extends InputDialog<String>
    {
        public TestNameDialog(String dialogLabel, String prompt)
        {
            super(dialogLabel, prompt, "test-name-dialog");
            // the test method name is blank by default, so the OK button should be disabled
            setOKEnabled(false);
        }

        @Override
        protected String convert(String newTestName)
        {
            // Junit 3 test methods must start with the word "test"
            if((unitTestFramework == UnitTestFramework.JUnit3) && !newTestName.startsWith("test"))
            {
                return "test" + Character.toTitleCase(newTestName.charAt(0)) + newTestName.substring(1);
            }
            else
                return newTestName;
        }

        @Override
        protected boolean validate(String oldInput, String newTestName)
        {
            if (newTestName.length() == 0) {
                setOKEnabled(false);
            }
            // Must be a valid Java identifier:
            else if (!JavaNames.isIdentifier(convert(newTestName)))
            {
                setErrorText(Config.getString("pkgmgr.test.invalidTestName"));
                setOKEnabled(false);
            }
            else
            {
                setErrorText("");
                setOKEnabled(true);
            }
            return true; //always allow
        }
    }

    public static class DummyDisabledOperation extends ClassTargetOperation
    {
        public DummyDisabledOperation(String label, MenuItemOrder menuItemOrder)
        {
            super(label, Combine.ONE, null, label, menuItemOrder);
            this.enabled = false;
        }

        @Override
        protected void execute(ClassTarget target)
        {
            // Will not be called
        }
    }
}
