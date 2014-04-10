/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2014  Michael Kolling and John Rosenberg 
 
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

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.junit.Test;

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.debugger.DebuggerObject;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.editor.Editor;
import bluej.parser.SourceLocation;
import bluej.parser.SourceSpan;
import bluej.parser.UnitTestAnalyzer;
import bluej.pkgmgr.PackageEditor;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.TestRunnerThread;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.Target;
import bluej.prefmgr.PrefMgr;
import bluej.testmgr.TestDisplayFrame;
import bluej.testmgr.record.ExistingFixtureInvokerRecord;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.JavaNames;
import bluej.utility.JavaUtils;

/**
 * A role object for Junit unit tests.
 *
 * @author  Andrew Patterson
 */
public class UnitTestClassRole extends ClassRole
{
    public static final String UNITTEST_ROLE_NAME = "UnitTestTarget";
    public static final String UNITTEST_ROLE_NAME_JUNIT4 = "UnitTestTargetJunit4";

    private final Color unittestbg = Config.getOptionalItemColour("colour.class.bg.unittest");

    private static final String testAll = Config.getString("pkgmgr.test.popup.testAll");
    private static final String createTest = Config.getString("pkgmgr.test.popup.createTest");
    private static final String benchToFixture = Config.getString("pkgmgr.test.popup.benchToFixture");
    private static final String fixtureToBench = Config.getString("pkgmgr.test.popup.fixtureToBench");
    
    /** Whether this is a Junit 4 test class. If false, it's a Junit 3 test class. */
    private boolean isJunit4;
    
    /**
     * Create the unit test class role.
     */
    public UnitTestClassRole(boolean isJunit4)
    {
        this.isJunit4 = isJunit4;
    }

    @Override
    public String getRoleName()
    {
        if (isJunit4) {
            return UNITTEST_ROLE_NAME_JUNIT4;
        }
        else {
            return UNITTEST_ROLE_NAME;
        }
    }

    @Override
    public String getStereotypeLabel()
    {
        return "unit test";
    }

    /**
     * Return the intended background colour for this type of target.
     */
    @Override
    public Paint getBackgroundPaint(int width, int height)
    {
        if (unittestbg != null) {
            return unittestbg;
        } else {
            Paint result;
            if (!Config.isRaspberryPi()){
                result = new GradientPaint(
                        0, 0, new Color(197,211,165),
                        0, height, new Color(170,190,140));
            }else{
                result = new Color(184, 201, 153);
            }
            return  result;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isJUnitTestMethod(Method m)
    {
        if (isJunit4) {
            Class<?> cl = m.getDeclaringClass();
            ClassLoader classLoader = cl.getClassLoader();
            try {
                Class<Test> testClass;
                if (classLoader == null) {
                    testClass = org.junit.Test.class;
                }
                else {
                    testClass = (Class<Test>) classLoader.loadClass("org.junit.Test");
                }

                if (m.getAnnotation(testClass) != null) {
                    if (!Modifier.isPublic(m.getModifiers())) return false;
                    if (m.getParameterTypes().length != 0) return false;
                    return true;
                }
            }
            catch (ClassNotFoundException cnfe) {}
            catch (LinkageError le) {}

            // No suitable annotations found, so not a test class
            return false;
        }
        else {
            // look for reasons to not include this method as a test case
            if (!m.getName().startsWith("test")) return false;
            if (!Modifier.isPublic(m.getModifiers())) return false;
            if (m.getParameterTypes().length != 0) return false;
            if (!m.getReturnType().equals(Void.TYPE)) return false;
            return true;
        }
    }
    
    /**
     * Generate a popup menu for this TestClassRole.
     * @param cl the class object that is represented by this target
     * @param editorFrame the frame in which this targets package is displayed
     * @return the generated JPopupMenu
     */
    @Override
    public boolean createRoleMenu(JPopupMenu menu, ClassTarget ct, Class<?> cl, int state)
    {
        boolean enableTestAll = false;

        if (state == ClassTarget.S_NORMAL && cl != null && ! ct.isAbstract()) {
            Method[] allMethods = cl.getMethods();

            for (int i=0; i < allMethods.length; i++) {
                Method m = allMethods[i];

                if (isJUnitTestMethod(m)) {
                    enableTestAll = true;
                    break;
                }
            }
        }

        // add run all tests option
        addMenuItem(menu, new TestAction(testAll, ct.getPackage().getEditor(),ct), enableTestAll);
        menu.addSeparator();

        return false;
    }

    /**
     * creates a class menu containing any constructors and static methods etc.
     *
     * @param menu the popup menu to add the class menu items to
     * @param cl Class object associated with this class target
     */
    @Override
    public boolean createClassConstructorMenu(JPopupMenu menu, ClassTarget ct, Class<?> cl)
    {
        boolean hasEntries = false;

        Method[] allMethods = cl.getMethods();
        
        if (! ct.isAbstract()) {
            for (int i=0; i < allMethods.length; i++) {
                Method m = allMethods[i];
                
                if (!isJUnitTestMethod(m)) {
                    continue;
                }
                
                String rtype;
                try {
                    rtype = JavaUtils.getJavaUtils().getReturnType(m).toString(true);
                }
                catch (ClassNotFoundException cnfe) {
                    rtype = m.getReturnType().getName();
                }
                Action testAction = new TestAction(rtype + " " + m.getName() + "()",
                        ct.getPackage().getEditor(), ct, m.getName());
                
                JMenuItem item = new JMenuItem();
                item.setAction(testAction);
                item.setFont(PrefMgr.getPopupMenuFont());
                menu.add(item);
                hasEntries = true;
            }
            if (!hasEntries) {
                JMenuItem item = new JMenuItem(Config.getString("pkgmgr.test.popup.noTests"));
                item.setFont(PrefMgr.getPopupMenuFont());
                item.setEnabled(false);
                menu.add(item);
            }
        }
        else {
            JMenuItem item = new JMenuItem(Config.getString("pkgmgr.test.popup.abstract"));
            item.setFont(PrefMgr.getPopupMenuFont());
            item.setEnabled(false);
            menu.add(item);
        }
        return true;
    }

    @Override
    public boolean createClassStaticMenu(JPopupMenu menu, ClassTarget ct, Class<?> cl)
    {
        boolean enable = !ct.getPackage().getProject().inTestMode() && ct.hasSourceCode() && ! ct.isAbstract();
            
        addMenuItem(menu, new MakeTestCaseAction(createTest,
                                                    ct.getPackage().getEditor(), ct), enable);
        addMenuItem(menu, new BenchToFixtureAction(benchToFixture,
                                                    ct.getPackage().getEditor(), ct), enable);
        addMenuItem(menu, new FixtureToBenchAction(fixtureToBench,
                                                    ct.getPackage().getEditor(), ct), enable);

        return true;
    }

    @Override
    public void run(final PkgMgrFrame pmf, final ClassTarget ct, final String param)
    {
        if (param != null) {
            // Only running a single test
            TestDisplayFrame.getTestDisplay().startTest(pmf.getProject(), 1);
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
     */
    public void doRunTest(PkgMgrFrame pmf, ClassTarget ct, TestRunnerThread trt)
    {
        Class<?> cl = pmf.getPackage().loadClass(ct.getQualifiedName());
        
        if (cl == null)
            return;
        
        // Test the whole class
        Method[] allMethods = cl.getMethods();
        
        ArrayList<String> testMethods = new ArrayList<String>();
        
        int testCount = 0;
        
        for (int i=0; i < allMethods.length; i++) {
            if (isJUnitTestMethod(allMethods[i])) {
                testCount++;
                testMethods.add(allMethods[i].getName());
            }
        }
        
        String [] testMethodsArr = (String []) testMethods.toArray(new String[testCount]);
        trt.setMethods(testMethodsArr);
        TestDisplayFrame.getTestDisplay().startTest(pmf.getProject(), testCount);
    }
    
    /**
     * Get the count of tests in the test class.
     * @param ct  The ClassTarget of the unit test class
     * @return    the number of tests in the unit test class
     */
    public int getTestCount(ClassTarget ct)
    {
        if (! ct.isCompiled()) {
            return 0;
        }
        
        Class<?> cl = ct.getPackage().loadClass(ct.getQualifiedName());
        if (cl == null) {
            return 0;
        }
        
        Method[] allMethods = cl.getMethods();

        int testCount = 0;

        for (int i=0; i < allMethods.length; i++) {
            if (isJUnitTestMethod(allMethods[i])) {
                testCount++;
            }
        }
        
        return testCount;
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
    public void doMakeTestCase(final PkgMgrFrame pmf, final ClassTarget ct)
    {
        // prompt for a new test name
        String newTestName = DialogManager.askString(pmf, "unittest-new-test-method");

        if (newTestName == null) {
            return;
        }

        if (newTestName.length() == 0) {
            pmf.setStatus(Config.getString("pkgmgr.test.noTestName"));
            return;
        }

        // Junit 3 test methods must start with the word "test"
        if(!isJunit4 && !newTestName.startsWith("test")) {
            newTestName = "test" + Character.toTitleCase(newTestName.charAt(0)) + newTestName.substring(1);
        }

        // and they must be a valid Java identifier
        if (!JavaNames.isIdentifier(newTestName)) {
            pmf.setStatus(Config.getString("pkgmgr.test.invalidTestName"));
            return;
        }

        // find out if the method already exists in the unit test src
        try {
            Charset charset = pmf.getProject().getProjectCharset();
            UnitTestAnalyzer uta = analyzeUnitTest(ct, charset);

            SourceSpan existingSpan = uta.getMethodBlockSpan(newTestName);

            if (existingSpan != null) {
                if (DialogManager.askQuestion(null, "unittest-method-present") == 1) {
                    return;
                }
            }
        }
        catch (IOException ioe) { 
            DialogManager.showErrorWithText(null, "unittest-io-error", ioe.getLocalizedMessage());
            Debug.reportError("Error reading unit test source", ioe);
        }

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
        // Avoid running test setup (which is user code) on the event thread.
        // Run it on a new thread instead.
        new Thread() {
            public void run() {
                
                final Map<String,DebuggerObject> dobs = pmf.getProject().getDebugger().runTestSetUp(ct.getQualifiedName());
                
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        List<DataCollector.NamedTyped> recordObjects = new ArrayList<DataCollector.NamedTyped>();
                        Iterator<Map.Entry<String,DebuggerObject>> it = dobs.entrySet().iterator();
                        
                        while(it.hasNext()) {
                            Map.Entry<String,DebuggerObject> mapent = it.next();
                            DebuggerObject objVal = mapent.getValue();
                            
                            if (! objVal.isNullObject()) {
                                String actualName = pmf.putObjectOnBench(mapent.getKey(), objVal, objVal.getGenType(), null);
                                recordObjects.add(new DataCollector.NamedTyped(actualName, objVal.getClassName()));
                            }
                        }
                        
                        if (recordAsFixtureToBench)
                        {
                            DataCollector.fixtureToObjectBench(pmf.getPackage(), ct.getSourceFile(), recordObjects);
                        }
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
        Editor ed = ct.getEditor();
        String ts = getIndentString();
        try {
            Charset charset = pmf.getProject().getProjectCharset();
            UnitTestAnalyzer uta = analyzeUnitTest(ct, charset);

            SourceSpan existingSpan = uta.getMethodBlockSpan(name);

            if (existingSpan != null) {
                // replace this method (don't replace the method header!)
                ed.setSelection(existingSpan.getStartLine(), existingSpan.getStartColumn(),
                                  existingSpan.getEndLine(), existingSpan.getEndColumn());
                ed.insertText("{\n" + pmf.getObjectBench().getTestMethod(ts + ts) + ts + "}", false);
            }
            else {
                // insert a complete method
                SourceLocation methodInsert = uta.getNewMethodInsertLocation();

                if (methodInsert != null) {
                    ed.setSelection(methodInsert.getLine(), methodInsert.getColumn(), 1);
                    if (isJunit4) {
                        ed.insertText("\n" + ts + "@Test\n" + ts + "public void " + name + "()\n" + ts + "{\n"
                                + pmf.getObjectBench().getTestMethod(ts + ts) + ts + "}\n}\n", false);
                    }
                    else {
                        ed.insertText("\n" + ts + "public void " + name + "()\n" + ts + "{\n"
                                + pmf.getObjectBench().getTestMethod(ts + ts) + ts + "}\n}\n", false);
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
        Editor ed = ct.getEditor();

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
                
        Editor ed = ct.getEditor();
        try {
            Charset charset = pmf.getProject().getProjectCharset();
            UnitTestAnalyzer uta = analyzeUnitTest(ct, charset);

            // find all the fields declared in this unit test class
            List<SourceSpan> variables = uta.getFieldSpans();
            
            // if we already have fields, ask if we are sure we want to get rid of them
            if (variables != null && variables.size() > 0) {
                if (DialogManager.askQuestion(null, "unittest-fixture-present") == 1) {
                    return;
                }
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
                    
                    ed.setSelection(variableSpan.getStartLine(), variableSpan.getStartColumn(),
                                     variableSpan.getEndLine(), variableSpan.getEndColumn());
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
                ed.setSelection(setupSpan.getStartLine(), setupSpan.getStartColumn(),
                                 setupSpan.getEndLine(), setupSpan.getEndColumn());
            } else {
                // otherwise, we will be inserting a brand new setUp() method
                ed.setSelection(fixtureInsertLocation.getLine(),
                                fixtureInsertLocation.getColumn(), 1);
                if (isJunit4) {
                    ed.insertText("{\n" + ts + "@Before\n" + ts + "public void setUp()\n" + ts, false);
                }
                else {
                    ed.insertText("{\n" + ts + "public void setUp()\n" + ts, false);
                }
            }
            
            // insert the code for our setUp() method
            ed.insertText("{\n" + pmf.getObjectBench().getFixtureSetup(ts + ts)
                                + ts + "}", false);

            // insert our new fixture declarations
            ed.setSelection(fixtureInsertLocation.getLine(),
                             fixtureInsertLocation.getColumn(), 1);
                
            ed.insertText("{\n" + pmf.getObjectBench().getFixtureDeclaration(ts), false);
            ed.save();
        }
        catch (IOException ioe) {
            PkgMgrFrame.showMessageWithText(pmf.getPackage(),
                    "generic-file-save-error", ioe.getLocalizedMessage());
        }
        
        pmf.getPackage().compileQuiet(ct);
        
        pmf.getProject().removeClassLoader();
        pmf.getProject().newRemoteClassLoaderLeavingBreakpoints();
    }
    
    /**
     * A base class for all our actions that run on targets.
     */
    private abstract class TargetAbstractAction extends AbstractAction
    {
        protected Target t;
        protected PackageEditor ped;

        public TargetAbstractAction(String name, PackageEditor ped, Target t)
        {
            super(name);
            this.ped = ped;
            this.t = t;
        }
    }

    /**
     * A TestAction is an action that causes a JUnit test to be run on a class.
     * If testName is not provided, it is set to null which means that the whole
     * test class is run; otherwise it refers to a test method that should be run
     * individually.
     */
    private class TestAction extends TargetAbstractAction
    {
        private String testName;

        public TestAction(String actionName, PackageEditor ped, Target t)
        {
            super(actionName, ped, t);
            this.testName = null;
        }
                    
        public TestAction(String actionName, PackageEditor ped, Target t, String testName)
        {
            super(actionName, ped, t);
            this.testName = testName;
        }

        public void actionPerformed(ActionEvent e)
        {
            ped.raiseRunTargetEvent(t, testName);
        }
    }

    private class MakeTestCaseAction extends TargetAbstractAction
    {
        public MakeTestCaseAction(String name, PackageEditor ped, Target t)
        {
            super(name, ped, t);
        }

        public void actionPerformed(ActionEvent e)
        {
            ped.raiseMakeTestCaseEvent(t);
        }
    }

    private class BenchToFixtureAction extends TargetAbstractAction
    {
        public BenchToFixtureAction(String name, PackageEditor ped, Target t)
        {
            super(name, ped, t);
        }

        public void actionPerformed(ActionEvent e)
        {
            ped.raiseBenchToFixtureEvent(t);
        }
    }

    private class FixtureToBenchAction extends TargetAbstractAction
    {
        public FixtureToBenchAction(String name, PackageEditor ped, Target t)
        {
            super(name, ped, t);
        }

        public void actionPerformed(ActionEvent e)
        {
            ped.raiseFixtureToBenchEvent(t);
        }
    }

}
