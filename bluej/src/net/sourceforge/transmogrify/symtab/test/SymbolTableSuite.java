/*
Copyright (C) 2001  ThoughtWorks, Inc

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
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package net.sourceforge.transmogrify.symtab.test;

// $Id: SymbolTableSuite.java 1014 2001-11-30 03:28:10Z ajp $

import junit.framework.*;

import net.sourceforge.transmogrify.symtab.parser.test.*;

public class SymbolTableSuite extends TestSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(net.sourceforge.transmogrify.symtab.parser.test.ParserSuite.suite());
    suite.addTest(net.sourceforge.transmogrify.symtab.printer.test.PrinterSuite.suite());
    suite.addTest(new TestSuite(MethodSpecificityTest.class));
    suite.addTest(new TestSuite(SuperInterfaceMethodResolutionTest.class));
    suite.addTest(new TestSuite(MultiplyInheritedInterfaceTest.class));
    suite.addTest(new TestSuite(DegenerateElseBlocksTest.class));
    suite.addTest(new TestSuite(DegenerateForTest.class));
    suite.addTest(new TestSuite(QualifiedImplementsClauseTest.class));
    suite.addTest(new TestSuite(BinaryAndModOperatorTest.class));
    suite.addTest(new TestSuite(ImportInnerClassTest.class));
    suite.addTest(new TestSuite(QualifiedConstructorTest.class));
    suite.addTest(new TestSuite(AnonymousClassConstructorArgumentsTest.class));
    suite.addTest(new TestSuite(SuperclassOfAnonymousInnerClassTest.class));
    suite.addTest(new TestSuite(SynchronizedBlockTest.class));
    suite.addTest(new TestSuite(DefaultConstructorTest.class));
    suite.addTest(new TestSuite(PublicInnerClassTest.class));
    suite.addTest(new TestSuite(ArrayCompatibleWithObjectTest.class));
    suite.addTest(new TestSuite(MethodOnSubinterfaceTest.class));
    suite.addTest(new TestSuite(PrimitiveClassesTest.class));
    suite.addTest(new TestSuite(ArrayAsVariableTypeTest.class));
    suite.addTest(new TestSuite(ArraysAsMethodTypeTest.class));
    suite.addTest(new TestSuite(InterfaceCompatibleWithObjectTest.class));
    suite.addTest(new TestSuite(ThrowExpressionTest.class));
    suite.addTest(new TestSuite(ArrayInitializerTest.class));
    suite.addTest(new TestSuite(ArraysInExternalMethodsTest.class));
    suite.addTest(new TestSuite(LiteralClassTest.class));
    suite.addTest(new TestSuite(ExtendsExternalClassTest.class));
    suite.addTest(new TestSuite(ExceptionsInMethodsTest.class));
    suite.addTest(new TestSuite(DegenerateConditionalsTest.class));
    suite.addTest(new TestSuite(ExternalMethodsTest.class));
    suite.addTest(new TestSuite(QuestionTest.class));
    suite.addTest(new TestSuite(ProtectedMemberLookupTest.class));
    suite.addTest(new TestSuite(DoWhileLoopTest.class));
    suite.addTest(new TestSuite(SuperinterfaceTest.class));
    suite.addTest(new TestSuite(ExceptionsTest.class));
    suite.addTest(new TestSuite(MethodsWithNullTest.class));
    suite.addTest(new TestSuite(ExternalDefinitionsTest.class));
    suite.addTest(new TestSuite(InnerClassTest.class));
    suite.addTest(new TestSuite(AnonymousLocalScopeTest.class));
    suite.addTest(new TestSuite(InitializerTest.class));
    suite.addTest(new TestSuite(PrimitiveHierarchyTest.class));
    suite.addTest(new TestSuite(QualifiedClassNameTest.class));
    suite.addTest(new TestSuite(QueryEngineTest.class));
    suite.addTest(new TestSuite(GetWordAtOccurrenceTest.class));
    suite.addTest(new TestSuite(MethodSignatureTest.class));
    suite.addTest(new TestSuite(InheritanceTest.class));
    suite.addTest(new TestSuite(StaticsTest.class));
    suite.addTest(new TestSuite(ExpressionResolutionTest.class));
    suite.addTest(new TestSuite(ForLoopTest.class));
    suite.addTest(new TestSuite(EmptyForLoopsTest.class));
    suite.addTest(new TestSuite(CommaForLoopsTest.class));
    suite.addTest(new TestSuite(VarTypesTest.class));
    suite.addTest(new TestSuite(MethodLocalVarTest.class));
    suite.addTest(new TestSuite(MethodReturnTypeTest.class));
    suite.addTest(new TestSuite(IfElseTest.class));
    suite.addTest(new TestSuite(WhileLoopTest.class));
    suite.addTest(new TestSuite(ThisAndSuperTest.class));
    suite.addTest(new TestSuite(LabelDefTest.class));
    suite.addTest(new TestSuite(ConstructorTest.class));
    suite.addTest(new TestSuite(SwitchStatementTest.class));
    suite.addTest(new TestSuite(ImplementorTest.class));
    suite.addTest(new TestSuite(TryCatchTest.class));
    suite.addTest(new TestSuite(ImportTest.class));
    suite.addTest(new TestSuite(MethodExceptionTest.class));
    suite.addTest(new TestSuite(StringLiteralTest.class));
    suite.addTest(new TestSuite(StarImportTest.class));
    suite.addTest(new TestSuite(BackPointerTest.class));
    suite.addTest(new TestSuite(ArrayTest.class));
    suite.addTest(new TestSuite(ParameterReferencesTest.class));
    suite.addTest(new TestSuite(VariableDefTest.class));
    suite.addTest(new TestSuite(ImplicitPackageTest.class));
    suite.addTest(new TestSuite(CastingTest.class));
    suite.addTest(new TestSuite(NameConflictTest.class));
    suite.addTest(new TestSuite(StringConcatenationTest.class));

    return suite;
  }

  public static void main(String [] args) {
    junit.swingui.TestRunner.main(new String [] { SymbolTableSuite.class.getName() });
  }
}
