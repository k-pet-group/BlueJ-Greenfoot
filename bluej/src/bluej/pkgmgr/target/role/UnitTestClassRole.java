package bluej.pkgmgr.target.role;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.lang.reflect.*;
import java.util.*;

import javax.swing.*;

import antlr.BaseAST;
import bluej.Config;
import bluej.debugger.*;
import bluej.editor.Editor;
import bluej.parser.ast.*;
import bluej.pkgmgr.*;
import bluej.pkgmgr.target.*;
import bluej.prefmgr.PrefMgr;
import bluej.testmgr.TestDisplayFrame;
import bluej.utility.*;

/**
 * A role object for Junit unit tests.
 *
 * @author  Andrew Patterson based on AppletClassRole
 * @version $Id: UnitTestClassRole.java 2131 2003-07-24 02:22:23Z bquig $
 */
public class UnitTestClassRole extends ClassRole
{
    public static final String UNITTEST_ROLE_NAME = "UnitTestTarget";

    private static final String popupPrefix = Config.getString("pkgmgr.test.popup.testPrefix");
	private static final String testAll = Config.getString("pkgmgr.test.popup.testAll");
	private static final String createTest = Config.getString("pkgmgr.test.popup.createTest");
	private static final String benchToFixture = Config.getString("pkgmgr.test.popup.benchToFixture");
	private static final String fixtureToBench = Config.getString("pkgmgr.test.popup.fixtureToBench");
    
    /**
     * Create the unit test class role.
     */
    public UnitTestClassRole()
    {
    }

    public String getRoleName()
    {
        return UNITTEST_ROLE_NAME;
    }

    public String getStereotypeLabel()
    {
        return "unit test";
    }

    public Color getBackgroundColour()
    {
        return Config.getItemColour("colour.class.bg.unittest");
    }

    private boolean isJUnitTestMethod(Method m)
    {
        // look for reasons to not include this method as a test case
        if (!m.getName().startsWith("test"))
            return false;
        if (!Modifier.isPublic(m.getModifiers()))
            return false;
        if (m.getParameterTypes().length != 0)
            return false;
        if (!m.getReturnType().equals(Void.TYPE))
            return false;
        
        return true;
    }
	
    /**
     * Generate a popup menu for this TestClassRole.
     * @param cl the class object that is represented by this target
     * @param editorFrame the frame in which this targets package is displayed
     * @return the generated JPopupMenu
     */
    public boolean createRoleMenu(JPopupMenu menu, ClassTarget ct, Class cl, int state)
    {
		boolean enableTestAll = false;
		
		if (state == Target.S_NORMAL && cl != null) {
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
        addMenuItem(menu, new TestAction(testAll, ct.getPackage().getEditor(),ct),
        			enableTestAll);
        menu.addSeparator();

        return false;
    }

    /**
     * creates a class menu containing any constructors and static methods etc.
     *
     * @param menu the popup menu to add the class menu items to
     * @param cl Class object associated with this class target
     */
    public boolean createClassConstructorMenu(JPopupMenu menu, ClassTarget ct, Class cl)
    {
        boolean hasEntries = false;

        Method[] allMethods = cl.getMethods();
		
        for (int i=0; i < allMethods.length; i++) {
            Method m = allMethods[i];

			if (!isJUnitTestMethod(m))
				continue;
				
            Action testAction = new TestAction(popupPrefix + " " + m.getName().substring(4),
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
        return true;
    }

    /**
     * creates a class menu containing any constructors and static methods etc.
     *
     * @param menu the popup menu to add the class menu items to
     * @param cl Class object associated with this class target
     */
    public boolean createClassStaticMenu(JPopupMenu menu, ClassTarget ct, Class cl)
    {
        addMenuItem(menu, new MakeTestCaseAction(createTest,
                                                    ct.getPackage().getEditor(), ct), true);
        addMenuItem(menu, new BenchToFixtureAction(benchToFixture,
                                                    ct.getPackage().getEditor(), ct), true);
        addMenuItem(menu, new FixtureToBenchAction(fixtureToBench,
                                                    ct.getPackage().getEditor(), ct), true);

        return true;
    }

    public void run(final PkgMgrFrame pmf, final ClassTarget ct, final String param)
    {
    	Thread thr = new Thread() {
    		public void run() {
				doRunTest(pmf, ct, param);
			}
		};		

    	thr.start();
    }

	public void doRunTest(PkgMgrFrame pmf, ClassTarget ct, String param)
	{
		DebuggerTestResult dtr = null;

		if (param != null) {
			// Test a single method
			dtr = pmf.getProject().getDebugger().runTestMethod(ct.getQualifiedName(), param);

			TestDisplayFrame.getTestDisplay().startTest(1);

			if (dtr.isSuccess()) {
				pmf.setStatus(param + " " + Config.getString("pkgmgr.test.succeeded"));
				TestDisplayFrame.getTestDisplay().addResultQuietly(dtr);
			}
			else {
				TestDisplayFrame.getTestDisplay().addResult(dtr);
			}
		}
		else {
			Class cl = pmf.getPackage().loadClass(ct.getQualifiedName());

			if (cl == null)
				return;
				
			// Test the whole class
			Method[] allMethods = cl.getMethods();

			int testCount = 0;

			for (int i=0; i < allMethods.length; i++) {
				if (isJUnitTestMethod(allMethods[i]))
					testCount++;
			}

			TestDisplayFrame.getTestDisplay().startTest(testCount);
				
			for (int i=0; i < allMethods.length; i++) {
				Method m = allMethods[i];

				if (!isJUnitTestMethod(m))
					continue;
					
				dtr = pmf.getProject().getDebugger().runTestMethod(ct.getQualifiedName(), m.getName());

				TestDisplayFrame.getTestDisplay().addResult(dtr);			
			}
		}            
	}
    public void doMakeTestCase(PkgMgrFrame pmf, ClassTarget ct)
    {
        String newTestName = DialogManager.askString(pmf, "unittest-new-test-method");

        if (newTestName == null)
            return;

        if (newTestName.length() == 0) {
            pmf.setStatus(Config.getString("pkgmgr.test.noTestName"));
            return;
        }

        if(! newTestName.startsWith("test")) {
            newTestName = "test" + Character.toTitleCase(newTestName.charAt(0)) + newTestName.substring(1);
        }

		if (!JavaNames.isIdentifier(newTestName)) {
			pmf.setStatus(Config.getString("pkgmgr.test.invalidTestName"));
			return;
		}

        pmf.getProject().removeLocalClassLoader();
        pmf.testRecordingStarted(Config.getString("pkgmgr.test.recording") + " "
        						 + ct.getBaseName() + "." + newTestName + "()");
 
        Editor ed = ct.getEditor();

        Map dobs = pmf.getProject().getDebugger().runTestSetUp(ct.getQualifiedName());

        Iterator it = dobs.entrySet().iterator();
        
        while(it.hasNext()) {
            Map.Entry mapent = (Map.Entry) it.next();

            pmf.putObjectOnBench((String) mapent.getKey(), (DebuggerObject)mapent.getValue(), null);
        }
        
        pmf.getObjectBench().resetRecordingInteractions();
        
        pmf.setTestInfo(newTestName, ct);
     }

    public void doEndMakeTestCase(PkgMgrFrame pmf, ClassTarget ct, String name)
    {
        Editor ed = ct.getEditor();

        try {
            BaseAST ast = (BaseAST) bluej.parser.ast.JavaParser.parseFile(new java.io.FileReader(ct.getSourceFile()));
            BaseAST firstClass = (BaseAST) ast.getFirstChild();

            LocatableAST methodInsert = null;

            methodInsert = (LocatableAST) firstClass.getFirstChild().getNextSibling();

            if (methodInsert != null) {
                ed.setSelection(methodInsert.getLine(), methodInsert.getColumn(), 1);
                
                ed.insertText("\n\tpublic void " + name + "()\n\t{\n" + pmf.getObjectBench().getTestMethod() + "\t}\n}\n", false);
            }
            
            ed.save();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void doFixtureToBench(PkgMgrFrame pmf, ClassTarget ct)
    {
        Editor ed = ct.getEditor();

        Map dobs = pmf.getProject().getDebugger().runTestSetUp(ct.getQualifiedName());

        Iterator it = dobs.entrySet().iterator();
        
        while(it.hasNext()) {
            Map.Entry mapent = (Map.Entry) it.next();

            pmf.putObjectOnBench((String) mapent.getKey(), (DebuggerObject)mapent.getValue(), null);
        }
    }   
    
    /**
     * Convert the objects on the object bench into a test fixture.
     */
    public void doBenchToFixture(PkgMgrFrame pmf, ClassTarget ct)
    {
        if(pmf.getObjectBench().getObjectWrapperCount() == 0)
            return;
                
        Editor ed = ct.getEditor();

        try {
            BaseAST ast = (BaseAST) JavaParser.parseFile(new java.io.FileReader(ct.getSourceFile()));

			// operate on the first class defined in the source file.
			// this could be a mistaken assumption but for unit tests its
			// probably correct
            BaseAST firstClass = (BaseAST) ast.getFirstChild();

            java.util.List variables = null;
            java.util.List setup = null;
            LocatableAST openingBracket = null;
            LocatableAST methodInsert = null;

            openingBracket = (LocatableAST) firstClass.getFirstChild();
            methodInsert = (LocatableAST) firstClass.getFirstChild().getNextSibling();
            
            BaseAST childAST = (BaseAST) methodInsert.getNextSibling();

            while(childAST != null) {
                if(childAST.getType() == UnitTestParserTokenTypes.OBJBLOCK) {
                    
                    variables = UnitTestParser.getVariableSelections(childAST);
                    setup = UnitTestParser.getSetupMethodSelections(childAST);
                    break;
                }               
                childAST = (BaseAST) childAST.getNextSibling();            
            }            

			if (variables != null && variables.size() > 0) {
				if (DialogManager.askQuestion(null, "unittest-fixture-present") == 1)
					return;
			}
            if (variables != null) {
                Iterator it = variables.iterator();
                
                while(it.hasNext()) {
                    LocatableAST firstAST = (LocatableAST) it.next();
                    LocatableAST secondAST = (LocatableAST) it.next();
                    
                    ed.setSelection(firstAST.getLine(), firstAST.getColumn(),
                                        secondAST.getLine(), secondAST.getColumn() + 1);
                    ed.insertText("", false);
                }
            }

            if (setup != null) {
                Iterator it = setup.iterator();
                
                if(it.hasNext()) {
                    LocatableAST firstAST = (LocatableAST) it.next();
                    LocatableAST secondAST = (LocatableAST) it.next();
                    
                    ed.setSelection(firstAST.getLine(), firstAST.getColumn(),
                                        secondAST.getLine(), secondAST.getColumn() + 1);
                    ed.insertText("{\n" + pmf.getObjectBench().getFixtureSetup()
                                     + "\t}", false);
                }
            }

            if (openingBracket != null) {
                ed.setSelection(openingBracket.getLine(), openingBracket.getColumn(), 1);
                
                ed.insertText("{\n" + pmf.getObjectBench().getFixtureDeclaration(), false);
            }
            
            ed.save();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

		pmf.getProject().removeLocalClassLoader();
		pmf.getProject().newRemoteClassLoader();

		pmf.getPackage().compileQuiet(ct);	
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
     * test class is run. Else it refers to a test method that should be run
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

	/**
	 */
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
