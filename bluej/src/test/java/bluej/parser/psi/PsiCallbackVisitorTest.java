/*
 This file is part of the BlueJ program. 
 Copyright (C) 2024  Michael Kolling and John Rosenberg
 
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
package bluej.parser.psi;

import org.jetbrains.kotlin.psi.*;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for {@link PsiCallbackVisitor} Phase 2 implementation.
 * 
 * <p><b>Phase 2 Focus:</b> Tests traversal logging and state management WITHOUT
 * callback invocation. Phase 3 tests will add callback integration testing.</p>
 * 
 * <p>This test suite validates:</p>
 * <ul>
 *   <li>Correct PSI tree traversal (depth-first)</li>
 *   <li>Traversal logging for all declaration types</li>
 *   <li>State management (scope push/pop balance)</li>
 *   <li>Null safety and error handling</li>
 *   <li>Nesting level tracking</li>
 * </ul>
 * 
 * <h3>Test Categories</h3>
 * <ul>
 *   <li><b>Basic Traversal Tests:</b> Individual declaration visits</li>
 *   <li><b>State Management Tests:</b> Scope push/pop operations</li>
 *   <li><b>Validation Tests:</b> State balance verification</li>
 *   <li><b>Null Safety Tests:</b> Graceful null handling</li>
 *   <li><b>Traversal Log Tests:</b> Log content verification</li>
 *   <li><b>Complex Scenarios:</b> Nested structures, realistic patterns</li>
 * </ul>
 * 
 * @see PsiCallbackVisitor Implementation under test
 * @see VisitorState State management being tested
 */
@Ignore("This should be now tested via the callback adapter")
public class PsiCallbackVisitorTest {
//
//    // ==================== Basic Traversal Tests ====================
//
//    /**
//     * Tests that visitKtFile logs file traversal.
//     */
//    @Test
//    public void visitKtFile_logsTraversal() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//        KtFile mockFile = mock(KtFile.class);
//        when(mockFile.getName()).thenReturn("TestFile.kt");
//
//        visitor.visitKtFile(mockFile);
//
//        List<String> log = visitor.getTraversalLog();
//        assertEquals(1, log.size());
//        assertTrue(log.get(0).contains("VISIT: FILE: TestFile.kt"));
//    }
//
//    /**
//     * Tests that visitClass logs class visit.
//     */
//    @Test
//    public void visitClass_logsClassVisit() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//        KtClass mockClass = mock(KtClass.class);
//        when(mockClass.getName()).thenReturn("MyClass");
//
//        visitor.visitClass(mockClass);
//
//        List<String> log = visitor.getTraversalLog();
//        assertEquals(1, log.size());
//        assertTrue(log.get(0).contains("VISIT: CLASS: MyClass"));
//    }
//
//    /**
//     * Tests that visitNamedFunction logs function visit.
//     */
//    @Test
//    public void visitNamedFunction_logsFunctionVisit() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//        KtNamedFunction mockFunction = mock(KtNamedFunction.class);
//        when(mockFunction.getName()).thenReturn("calculate");
//
//        visitor.visitNamedFunction(mockFunction);
//
//        List<String> log = visitor.getTraversalLog();
//        assertEquals(1, log.size());
//        assertTrue(log.get(0).contains("VISIT: FUNCTION: calculate"));
//    }
//
//    /**
//     * Tests that visitProperty logs property visit.
//     */
//    @Test
//    public void visitProperty_logsPropertyVisit() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//        KtProperty mockProperty = mock(KtProperty.class);
//        when(mockProperty.getName()).thenReturn("count");
//
//        visitor.visitProperty(mockProperty);
//
//        List<String> log = visitor.getTraversalLog();
//        assertEquals(1, log.size());
//        assertTrue(log.get(0).contains("VISIT: PROPERTY: count"));
//    }
//
//    /**
//     * Tests that visitObjectDeclaration logs object visit.
//     */
//    @Test
//    public void visitObjectDeclaration_logsObjectVisit() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//        KtObjectDeclaration mockObject = mock(KtObjectDeclaration.class);
//        when(mockObject.getName()).thenReturn("Singleton");
//
//        visitor.visitObjectDeclaration(mockObject);
//
//        List<String> log = visitor.getTraversalLog();
//        assertEquals(1, log.size());
//        assertTrue(log.get(0).contains("VISIT: OBJECT: Singleton"));
//    }
//
//    // ==================== State Management Tests ====================
//
//    /**
//     * Tests that visitClass pushes and pops scope correctly.
//     */
//    @Test
//    public void visitClass_pushesAndPopsScope() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//        KtClass mockClass = mock(KtClass.class);
//        when(mockClass.getName()).thenReturn("MyClass");
//
//        VisitorState state = visitor.getState();
//        assertEquals(0, state.getNestingDepth());
//
//        visitor.visitClass(mockClass);
//
//        // After visitClass completes, state should be balanced (popped)
//        assertEquals(0, state.getNestingDepth());
//        assertTrue(state.isStackBalanced());
//    }
//
//    /**
//     * Tests that visitNamedFunction pushes and pops scope correctly.
//     */
//    @Test
//    public void visitNamedFunction_pushesAndPopsScope() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//        KtNamedFunction mockFunction = mock(KtNamedFunction.class);
//        when(mockFunction.getName()).thenReturn("myMethod");
//
//        VisitorState state = visitor.getState();
//        assertEquals(0, state.getNestingDepth());
//
//        visitor.visitNamedFunction(mockFunction);
//
//        // After visitNamedFunction completes, state should be balanced
//        assertEquals(0, state.getNestingDepth());
//        assertTrue(state.isStackBalanced());
//    }
//
//    /**
//     * Tests that nested declarations maintain balanced state.
//     *
//     * <p>Critical test for try-finally pattern correctness.</p>
//     */
//    @Test
//    public void nestedDeclarations_maintainsBalancedState() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//        KtClass outerClass = mock(KtClass.class);
//        when(outerClass.getName()).thenReturn("Outer");
//
//        KtClass innerClass = mock(KtClass.class);
//        when(innerClass.getName()).thenReturn("Inner");
//
//        // Visit outer class
//        visitor.visitClass(outerClass);
//
//        // State should be balanced after outer class
//        assertTrue(visitor.getState().isStackBalanced());
//
//        // Visit inner class
//        visitor.visitClass(innerClass);
//
//        // State should still be balanced
//        assertTrue(visitor.getState().isStackBalanced());
//    }
//
//    // ==================== Validation Tests ====================
//
//    /**
//     * Tests that validateState returns true after balanced traversal.
//     */
//    @Test
//    public void validateState_trueAfterBalancedTraversal() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//        KtClass mockClass = mock(KtClass.class);
//        when(mockClass.getName()).thenReturn("MyClass");
//
//        visitor.visitClass(mockClass);
//
//        assertTrue(visitor.validateState());
//    }
//
//    /**
//     * Tests that getState returns visitor state.
//     */
//    @Test
//    public void getState_returnsVisitorState() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//
//        VisitorState state = visitor.getState();
//
//        assertNotNull(state);
//        assertTrue(state.isStackBalanced());
//    }
//
//    /**
//     * Tests that validateState throws exception if state is unbalanced.
//     *
//     * <p>This test manually corrupts state to verify exception behavior.
//     * In normal usage, state should never be unbalanced due to try-finally.</p>
//     */
//    @Test(expected = IllegalStateException.class)
//    public void validateState_throwsIfUnbalanced() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//        VisitorState state = visitor.getState();
//
//        // Manually corrupt state (push without pop)
//        state.pushScope(mock(KtClass.class));
//
//        // Should throw IllegalStateException
//        visitor.validateState();
//    }
//
//    // ==================== Null Safety Tests ====================
//
//    /**
//     * Tests that visitClass handles null element gracefully.
//     */
//    @Test
//    public void visitClass_nullElement_handlesGracefully() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//
//        visitor.visitClass(null);
//
//        // Should not crash, log should be empty
//        assertEquals(0, visitor.getTraversalLog().size());
//        assertTrue(visitor.getState().isStackBalanced());
//    }
//
//    /**
//     * Tests that visitNamedFunction handles null element gracefully.
//     */
//    @Test
//    public void visitNamedFunction_nullElement_handlesGracefully() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//
//        visitor.visitNamedFunction(null);
//
//        // Should not crash, log should be empty
//        assertEquals(0, visitor.getTraversalLog().size());
//        assertTrue(visitor.getState().isStackBalanced());
//    }
//
//    /**
//     * Tests that visitProperty handles null element gracefully.
//     */
//    @Test
//    public void visitProperty_nullElement_handlesGracefully() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//
//        visitor.visitProperty(null);
//
//        assertEquals(0, visitor.getTraversalLog().size());
//        assertTrue(visitor.getState().isStackBalanced());
//    }
//
//    /**
//     * Tests that visitObjectDeclaration handles null element gracefully.
//     */
//    @Test
//    public void visitObjectDeclaration_nullElement_handlesGracefully() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//
//        visitor.visitObjectDeclaration(null);
//
//        assertEquals(0, visitor.getTraversalLog().size());
//        assertTrue(visitor.getState().isStackBalanced());
//    }
//
//    /**
//     * Tests that visitKtFile handles null file gracefully.
//     */
//    @Test
//    public void visitKtFile_nullFile_handlesGracefully() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//
//        visitor.visitKtFile(null);
//
//        assertEquals(0, visitor.getTraversalLog().size());
//    }
//
//    // ==================== Traversal Log Tests ====================
//
//    /**
//     * Tests that getTraversalLog returns correct sequence.
//     */
//    @Test
//    public void getTraversalLog_returnsCorrectSequence() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//
//        KtClass mockClass = mock(KtClass.class);
//        when(mockClass.getName()).thenReturn("MyClass");
//
//        KtNamedFunction mockFunction = mock(KtNamedFunction.class);
//        when(mockFunction.getName()).thenReturn("myMethod");
//
//        visitor.visitClass(mockClass);
//        visitor.visitNamedFunction(mockFunction);
//
//        List<String> log = visitor.getTraversalLog();
//
//        assertEquals(2, log.size());
//        assertTrue(log.get(0).contains("CLASS"));
//        assertTrue(log.get(1).contains("FUNCTION"));
//    }
//
//    /**
//     * Tests that getTraversalLog returns unmodifiable copy.
//     *
//     * <p>Returned list should be immutable to prevent external modification.</p>
//     */
//    @Test(expected = UnsupportedOperationException.class)
//    public void getTraversalLog_returnsUnmodifiableCopy() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//
//        List<String> log = visitor.getTraversalLog();
//
//        // Attempt to modify should throw exception
//        log.add("VISIT: INVALID");
//    }
//
//    /**
//     * Tests traversal log with anonymous declarations (null names).
//     */
//    @Test
//    public void traversalLog_withAnonymousDeclarations() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//
//        KtClass mockClass = mock(KtClass.class);
//        when(mockClass.getName()).thenReturn(null);
//
//        KtNamedFunction mockFunction = mock(KtNamedFunction.class);
//        when(mockFunction.getName()).thenReturn(null);
//
//        visitor.visitClass(mockClass);
//        visitor.visitNamedFunction(mockFunction);
//
//        List<String> log = visitor.getTraversalLog();
//
//        assertEquals(2, log.size());
//        assertTrue(log.get(0).contains("<anonymous>"));
//        assertTrue(log.get(1).contains("<anonymous>"));
//    }
//
//    // ==================== Complex Scenarios ====================
//
//    /**
//     * Tests nested classes with correct traversal order.
//     *
//     * <p>Simulates: class Outer { class Inner { } }</p>
//     */
//    @Test
//    public void nestedClasses_correctTraversalOrder() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//
//        KtClass outerClass = mock(KtClass.class);
//        when(outerClass.getName()).thenReturn("Outer");
//
//        KtClass innerClass = mock(KtClass.class);
//        when(innerClass.getName()).thenReturn("Inner");
//
//        visitor.visitClass(outerClass);
//        visitor.visitClass(innerClass);
//
//        List<String> log = visitor.getTraversalLog();
//
//        assertEquals(2, log.size());
//        assertTrue(log.get(0).contains("Outer"));
//        assertTrue(log.get(1).contains("Inner"));
//
//        // State should be balanced
//        assertTrue(visitor.validateState());
//    }
//
//    /**
//     * Tests mixed declarations with correct state management.
//     *
//     * <p>Simulates: class MyClass { fun method() { } val property }</p>
//     */
//    @Test
//    public void mixedDeclarations_correctStateManagement() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//
//        KtClass mockClass = mock(KtClass.class);
//        when(mockClass.getName()).thenReturn("MyClass");
//
//        KtNamedFunction mockFunction = mock(KtNamedFunction.class);
//        when(mockFunction.getName()).thenReturn("method");
//
//        KtProperty mockProperty = mock(KtProperty.class);
//        when(mockProperty.getName()).thenReturn("property");
//
//        visitor.visitClass(mockClass);
//        visitor.visitNamedFunction(mockFunction);
//        visitor.visitProperty(mockProperty);
//
//        List<String> log = visitor.getTraversalLog();
//
//        assertEquals(3, log.size());
//        assertTrue(log.get(0).contains("CLASS"));
//        assertTrue(log.get(1).contains("FUNCTION"));
//        assertTrue(log.get(2).contains("PROPERTY"));
//
//        // All state should be balanced
//        assertTrue(visitor.validateState());
//    }
//
//    /**
//     * Tests realistic file structure traversal.
//     *
//     * <p>Simulates typical Kotlin file with file → class → members pattern.</p>
//     */
//    @Test
//    public void realisticFileStructure_traversesCorrectly() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//
//        KtFile mockFile = mock(KtFile.class);
//        when(mockFile.getName()).thenReturn("MyFile.kt");
//
//        KtClass mockClass = mock(KtClass.class);
//        when(mockClass.getName()).thenReturn("MyClass");
//
//        KtProperty mockProperty1 = mock(KtProperty.class);
//        when(mockProperty1.getName()).thenReturn("field1");
//
//        KtNamedFunction mockMethod = mock(KtNamedFunction.class);
//        when(mockMethod.getName()).thenReturn("calculate");
//
//        KtProperty mockProperty2 = mock(KtProperty.class);
//        when(mockProperty2.getName()).thenReturn("field2");
//
//        // Traverse in typical order
//        visitor.visitKtFile(mockFile);
//        visitor.visitClass(mockClass);
//        visitor.visitProperty(mockProperty1);
//        visitor.visitNamedFunction(mockMethod);
//        visitor.visitProperty(mockProperty2);
//
//        List<String> log = visitor.getTraversalLog();
//
//        assertEquals(5, log.size());
//        assertTrue(log.get(0).contains("FILE"));
//        assertTrue(log.get(1).contains("CLASS"));
//        assertTrue(log.get(2).contains("PROPERTY: field1"));
//        assertTrue(log.get(3).contains("FUNCTION: calculate"));
//        assertTrue(log.get(4).contains("PROPERTY: field2"));
//
//        assertTrue(visitor.validateState());
//    }
//
//    /**
//     * Tests object declaration with members.
//     *
//     * <p>Simulates: object Singleton { fun method() }</p>
//     */
//    @Test
//    public void objectWithMembers_traversesCorrectly() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//
//        KtObjectDeclaration mockObject = mock(KtObjectDeclaration.class);
//        when(mockObject.getName()).thenReturn("Singleton");
//
//        KtNamedFunction mockMethod = mock(KtNamedFunction.class);
//        when(mockMethod.getName()).thenReturn("getInstance");
//
//        visitor.visitObjectDeclaration(mockObject);
//        visitor.visitNamedFunction(mockMethod);
//
//        List<String> log = visitor.getTraversalLog();
//
//        assertEquals(2, log.size());
//        assertTrue(log.get(0).contains("OBJECT: Singleton"));
//        assertTrue(log.get(1).contains("FUNCTION: getInstance"));
//
//        assertTrue(visitor.validateState());
//    }
//
//    /**
//     * Tests that state is independent across multiple visitors.
//     */
//    @Test
//    public void multipleVisitors_independentState() {
//        PsiCallbackVisitor visitor1 = new PsiCallbackVisitor();
//        PsiCallbackVisitor visitor2 = new PsiCallbackVisitor();
//
//        KtClass mockClass = mock(KtClass.class);
//        when(mockClass.getName()).thenReturn("MyClass");
//
//        // Visit with first visitor
//        visitor1.visitClass(mockClass);
//
//        // Second visitor should have empty log
//        assertEquals(1, visitor1.getTraversalLog().size());
//        assertEquals(0, visitor2.getTraversalLog().size());
//
//        // Both should have balanced state
//        assertTrue(visitor1.validateState());
//        assertTrue(visitor2.validateState());
//    }
//
//    /**
//     * Tests deeply nested structure (5 levels).
//     *
//     * <p>Verifies state management with realistic nesting depth.</p>
//     */
//    @Test
//    public void deeplyNestedStructure_maintainsBalance() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//
//        // Create nested structure: class → class → class → function → property
//        KtClass class1 = mock(KtClass.class);
//        when(class1.getName()).thenReturn("Level1");
//
//        KtClass class2 = mock(KtClass.class);
//        when(class2.getName()).thenReturn("Level2");
//
//        KtClass class3 = mock(KtClass.class);
//        when(class3.getName()).thenReturn("Level3");
//
//        KtNamedFunction function = mock(KtNamedFunction.class);
//        when(function.getName()).thenReturn("method");
//
//        KtProperty property = mock(KtProperty.class);
//        when(property.getName()).thenReturn("field");
//
//        visitor.visitClass(class1);
//        visitor.visitClass(class2);
//        visitor.visitClass(class3);
//        visitor.visitNamedFunction(function);
//        visitor.visitProperty(property);
//
//        assertEquals(5, visitor.getTraversalLog().size());
//        assertTrue(visitor.validateState());
//    }
//
//    /**
//     * Tests that state provides correct context during traversal.
//     *
//     * <p>Verifies that state methods can be queried during traversal
//     * for Phase 3 callback context.</p>
//     */
//    @Test
//    public void stateContext_availableDuringTraversal() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//        VisitorState state = visitor.getState();
//
//        // Initial state
//        assertEquals(0, state.getNestingDepth());
//        assertFalse(state.isInsideClass());
//
//        // After visiting various elements, state resets (due to try-finally)
//        KtClass mockClass = mock(KtClass.class);
//        when(mockClass.getName()).thenReturn("MyClass");
//
//        visitor.visitClass(mockClass);
//
//        // State should be back to balanced after visit completes
//        assertEquals(0, state.getNestingDepth());
//        assertTrue(state.isStackBalanced());
//    }
//
//    /**
//     * Tests empty file traversal.
//     */
//    @Test
//    public void emptyFile_logsOnlyFile() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//
//        KtFile mockFile = mock(KtFile.class);
//        when(mockFile.getName()).thenReturn("Empty.kt");
//
//        visitor.visitKtFile(mockFile);
//
//        List<String> log = visitor.getTraversalLog();
//
//        assertEquals(1, log.size());
//        assertTrue(log.get(0).contains("FILE: Empty.kt"));
//        assertTrue(visitor.validateState());
//    }
//
//    /**
//     * Tests that multiple file visits accumulate in log.
//     */
//    @Test
//    public void multipleFileVisits_accumulateInLog() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//
//        KtFile file1 = mock(KtFile.class);
//        when(file1.getName()).thenReturn("File1.kt");
//
//        KtFile file2 = mock(KtFile.class);
//        when(file2.getName()).thenReturn("File2.kt");
//
//        visitor.visitKtFile(file1);
//        visitor.visitKtFile(file2);
//
//        List<String> log = visitor.getTraversalLog();
//
//        assertEquals(2, log.size());
//        assertTrue(log.get(0).contains("File1.kt"));
//        assertTrue(log.get(1).contains("File2.kt"));
//    }
//
//    /**
//     * Tests that state remains balanced even when exception occurs during traversal.
//     * Critical for validating try-finally pattern correctness.
//     */
//    @Test
//    public void exceptionDuringTraversal_maintainsStateBalance() {
//        PsiCallbackVisitor visitor = new PsiCallbackVisitor();
//        KtClass mockClass = mock(KtClass.class);
//        when(mockClass.getName()).thenThrow(new RuntimeException("Simulated error"));
//
//        try {
//            visitor.visitClass(mockClass);
//            fail("Expected exception to be thrown");
//        } catch (RuntimeException e) {
//            // Expected - exception should propagate
//        }
//
//        // Critical: State must still be balanced despite exception
//        assertTrue(visitor.validateState());
//        assertTrue(visitor.getState().isStackBalanced());
//    }
}