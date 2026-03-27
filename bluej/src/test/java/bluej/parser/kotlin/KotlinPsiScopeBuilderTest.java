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
package bluej.parser.kotlin;

import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedNode;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for KotlinPsiScopeBuilder — verifies PSI tree → ParsedNode conversion
 * produces the correct scope tree structure for JavaSyntaxView scope coloring.
 *
 * <p>The scope tree uses a container+inner pattern matching Java's parser:
 * each container (class, method, control flow) has an inner node for the
 * body content between braces. Containers get type-specific coloring,
 * inner nodes get neutral C3+BK coloring.</p>
 */
public class KotlinPsiScopeBuilderTest
{
    private static KtPsiFactory psiFactory;

    @BeforeClass
    public static void setupPsiFactory()
    {
        psiFactory = KotlinEnvironmentManager.getPsiFactory();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private KotlinParsedCUNode buildScopeTree(String source)
    {
        KtFile ktFile = psiFactory.createFile(source);
        KotlinParsedCUNode root = new KotlinParsedCUNode();
        // Set root node size to the full source length
        root.setSize(source.length());
        KotlinPsiScopeBuilder.buildScopesFromFile(ktFile, root, 0);
        return root;
    }

    private NodeAndPosition<ParsedNode> firstChild(ParsedNode parent)
    {
        return parent.findNodeAtOrAfter(0, 0);
    }

    /**
     * Find the inner node of a container. Asserts the first child
     * is indeed an inner node with NODETYPE_NONE.
     */
    private NodeAndPosition<ParsedNode> findInner(NodeAndPosition<ParsedNode> containerNp)
    {
        NodeAndPosition<ParsedNode> np = containerNp.getNode().findNodeAtOrAfter(
            containerNp.getPosition(), containerNp.getPosition());
        assertNotNull("Container should have inner node", np);
        assertTrue("First child of container should be inner",
            np.getNode().isInner());
        assertEquals("Inner node should be NODETYPE_NONE",
            ParsedNode.NODETYPE_NONE, np.getNode().getNodeType());
        return np;
    }

    /**
     * Navigate through the inner node to find the first content child.
     */
    private NodeAndPosition<ParsedNode> firstContentChild(
            NodeAndPosition<ParsedNode> containerNp)
    {
        NodeAndPosition<ParsedNode> innerNp = findInner(containerNp);
        return innerNp.getNode().findNodeAtOrAfter(
            innerNp.getPosition(), innerNp.getPosition());
    }

    // -----------------------------------------------------------------------
    // Tests: Basic class
    // -----------------------------------------------------------------------

    @Test
    public void testClassDeclarationCreatesTypeNode()
    {
        KotlinParsedCUNode root = buildScopeTree("class Foo { }");
        NodeAndPosition<ParsedNode> np = firstChild(root);
        assertNotNull("Should have a child node", np);
        assertEquals("Class should be NODETYPE_TYPEDEF",
            ParsedNode.NODETYPE_TYPEDEF, np.getNode().getNodeType());
    }

    @Test
    public void testInterfaceDeclarationCreatesTypeNode()
    {
        KotlinParsedCUNode root = buildScopeTree("interface Foo { }");
        NodeAndPosition<ParsedNode> np = firstChild(root);
        assertNotNull(np);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, np.getNode().getNodeType());
    }

    @Test
    public void testObjectDeclarationCreatesTypeNode()
    {
        KotlinParsedCUNode root = buildScopeTree("object Singleton { }");
        NodeAndPosition<ParsedNode> np = firstChild(root);
        assertNotNull(np);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, np.getNode().getNodeType());
    }

    // -----------------------------------------------------------------------
    // Tests: Function declaration
    // -----------------------------------------------------------------------

    @Test
    public void testTopLevelFunctionCreatesMethodNode()
    {
        KotlinParsedCUNode root = buildScopeTree("fun main() { }");
        NodeAndPosition<ParsedNode> np = firstChild(root);
        assertNotNull("Should have a child node for function", np);
        assertEquals("Function should be NODETYPE_METHODDEF",
            ParsedNode.NODETYPE_METHODDEF, np.getNode().getNodeType());
    }

    @Test
    public void testClassWithMethodCreatesNestedNodes()
    {
        String source = "class Foo {\n    fun bar() { }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        // First child should be the class
        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());

        // Method is inside the class's inner node
        NodeAndPosition<ParsedNode> methodNp = firstContentChild(classNp);
        assertNotNull("Class inner should contain a method node", methodNp);
        assertEquals("Method inside class should be NODETYPE_METHODDEF",
            ParsedNode.NODETYPE_METHODDEF, methodNp.getNode().getNodeType());
    }

    // -----------------------------------------------------------------------
    // Tests: Inner nodes
    // -----------------------------------------------------------------------

    @Test
    public void testClassHasInnerNode()
    {
        KotlinParsedCUNode root = buildScopeTree("class Foo { }");
        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);

        NodeAndPosition<ParsedNode> innerNp = findInner(classNp);
        assertTrue("Inner should be within class bounds",
            innerNp.getPosition() > classNp.getPosition()
            && innerNp.getEnd() < classNp.getEnd());
    }

    @Test
    public void testFunctionHasInnerNode()
    {
        KotlinParsedCUNode root = buildScopeTree("fun main() { }");
        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        assertNotNull(methodNp);

        NodeAndPosition<ParsedNode> innerNp = findInner(methodNp);
        assertTrue("Inner should be within method bounds",
            innerNp.getPosition() > methodNp.getPosition()
            && innerNp.getEnd() < methodNp.getEnd());
    }

    @Test
    public void testIfBodyHasInnerNode()
    {
        String source = "fun test() { if (true) { println() } }";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        NodeAndPosition<ParsedNode> methodInner = findInner(methodNp);

        // Find the if node inside method's inner
        NodeAndPosition<ParsedNode> ifNp = methodInner.getNode().findNodeAtOrAfter(
            methodInner.getPosition(), methodInner.getPosition());
        assertNotNull(ifNp);
        assertEquals(ParsedNode.NODETYPE_SELECTION, ifNp.getNode().getNodeType());

        // The if should have an inner node for its body block
        NodeAndPosition<ParsedNode> ifInner = findInner(ifNp);
        assertTrue("if inner should be within if bounds",
            ifInner.getPosition() > ifNp.getPosition()
            && ifInner.getEnd() < ifNp.getEnd());
    }

    @Test
    public void testForBodyHasInnerNode()
    {
        String source = "fun test() { for (i in 1..10) { println(i) } }";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        NodeAndPosition<ParsedNode> methodInner = findInner(methodNp);

        NodeAndPosition<ParsedNode> forNp = methodInner.getNode().findNodeAtOrAfter(
            methodInner.getPosition(), methodInner.getPosition());
        assertNotNull(forNp);
        assertEquals(ParsedNode.NODETYPE_ITERATION, forNp.getNode().getNodeType());

        NodeAndPosition<ParsedNode> forInner = findInner(forNp);
        assertTrue("for inner should be within for bounds",
            forInner.getPosition() > forNp.getPosition()
            && forInner.getEnd() < forNp.getEnd());
    }

    // -----------------------------------------------------------------------
    // Tests: Control flow → scope nodes
    // -----------------------------------------------------------------------

    @Test
    public void testIfExpressionCreatesSelectionNode()
    {
        String source = "fun test() { if (true) { println() } }";
        KotlinParsedCUNode root = buildScopeTree(source);

        // root → method → inner → if
        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        assertNotNull(methodNp);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, methodNp.getNode().getNodeType());

        NodeAndPosition<ParsedNode> methodInner = findInner(methodNp);
        NodeAndPosition<ParsedNode> ifNp = methodInner.getNode().findNodeAtOrAfter(
            methodInner.getPosition(), methodInner.getPosition());
        assertNotNull("Method inner should contain the if-node", ifNp);
        assertEquals("if should be NODETYPE_SELECTION",
            ParsedNode.NODETYPE_SELECTION, ifNp.getNode().getNodeType());
    }

    @Test
    public void testWhenExpressionCreatesSelectionNode()
    {
        String source = "fun test() { when (1) { 1 -> println() } }";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        assertNotNull(methodNp);

        NodeAndPosition<ParsedNode> methodInner = findInner(methodNp);
        NodeAndPosition<ParsedNode> whenNp = methodInner.getNode().findNodeAtOrAfter(
            methodInner.getPosition(), methodInner.getPosition());
        assertNotNull("Method inner should contain the when-node", whenNp);
        assertEquals("when should be NODETYPE_SELECTION",
            ParsedNode.NODETYPE_SELECTION, whenNp.getNode().getNodeType());
    }

    @Test
    public void testForExpressionCreatesIterationNode()
    {
        String source = "fun test() { for (i in 1..10) { println(i) } }";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        assertNotNull(methodNp);

        NodeAndPosition<ParsedNode> methodInner = findInner(methodNp);
        NodeAndPosition<ParsedNode> forNp = methodInner.getNode().findNodeAtOrAfter(
            methodInner.getPosition(), methodInner.getPosition());
        assertNotNull("Method inner should contain the for-node", forNp);
        assertEquals("for should be NODETYPE_ITERATION",
            ParsedNode.NODETYPE_ITERATION, forNp.getNode().getNodeType());
    }

    @Test
    public void testWhileExpressionCreatesIterationNode()
    {
        String source = "fun test() { while (true) { break } }";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        assertNotNull(methodNp);

        NodeAndPosition<ParsedNode> methodInner = findInner(methodNp);
        NodeAndPosition<ParsedNode> whileNp = methodInner.getNode().findNodeAtOrAfter(
            methodInner.getPosition(), methodInner.getPosition());
        assertNotNull("Method inner should contain the while-node", whileNp);
        assertEquals("while should be NODETYPE_ITERATION",
            ParsedNode.NODETYPE_ITERATION, whileNp.getNode().getNodeType());
    }

    // -----------------------------------------------------------------------
    // Tests: Edge cases
    // -----------------------------------------------------------------------

    @Test
    public void testEmptyFileProducesNoChildren()
    {
        KotlinParsedCUNode root = buildScopeTree("");
        NodeAndPosition<ParsedNode> np = firstChild(root);
        assertNull("Empty file should have no children", np);
    }

    @Test
    public void testFileWithOnlyImportsHasNoScopeChildren()
    {
        KotlinParsedCUNode root = buildScopeTree("import kotlin.math.sqrt");
        NodeAndPosition<ParsedNode> np = firstChild(root);
        assertNull("Import-only file should have no scope children", np);
    }

    @Test
    public void testNestedClassesCreateNestedTypeNodes()
    {
        String source = "class Outer {\n    class Inner { }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> outerNp = firstChild(root);
        assertNotNull(outerNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, outerNp.getNode().getNodeType());

        // Inner class is inside the outer's inner node
        NodeAndPosition<ParsedNode> innerClassNp = firstContentChild(outerNp);
        assertNotNull("Outer class inner should contain nested class", innerClassNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, innerClassNp.getNode().getNodeType());
    }

    // -----------------------------------------------------------------------
    // Tests: Offset accuracy
    // -----------------------------------------------------------------------

    @Test
    public void testClassNodePositionMatchesSource()
    {
        String source = "class Foo { }";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> np = firstChild(root);
        assertNotNull(np);
        assertEquals("Class node should start at position 0", 0, np.getPosition());
        assertEquals("Class node size should match source length",
            source.length(), np.getSize());
    }

    @Test
    public void testFunctionNodePositionMatchesSource()
    {
        String source = "fun main() { }";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> np = firstChild(root);
        assertNotNull(np);
        assertEquals("Function node should start at position 0", 0, np.getPosition());
        assertEquals("Function node size should match source length",
            source.length(), np.getSize());
    }

    // -----------------------------------------------------------------------
    // Tests: Complex Kotlin patterns
    // -----------------------------------------------------------------------

    @Test
    public void testFullClassWithMethodAndControlFlow()
    {
        String source = """
            class Foo {
                fun bar() {
                    if (true) {
                        for (i in 1..10) {
                            println(i)
                        }
                    }
                }
            }""";
        KotlinParsedCUNode root = buildScopeTree(source);

        // class → inner → method
        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull("Should have class node", classNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());

        NodeAndPosition<ParsedNode> methodNp = firstContentChild(classNp);
        assertNotNull("Class inner should contain method", methodNp);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, methodNp.getNode().getNodeType());
    }

    // -----------------------------------------------------------------------
    // Tests: Relative position accuracy (nested scopes)
    // -----------------------------------------------------------------------

    @Test
    public void testMethodInsideClassHasCorrectAbsolutePosition()
    {
        // Class doesn't start at 0 — preceded by package declaration
        String source = "package test\n\nclass Foo {\n    fun bar() { }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);

        int classStart = source.indexOf("class");
        assertEquals("Class absolute position", classStart, classNp.getPosition());

        // Method inside the class's inner node
        NodeAndPosition<ParsedNode> methodNp = firstContentChild(classNp);
        assertNotNull("Class inner should contain method", methodNp);

        int funStart = source.indexOf("fun");
        assertEquals("Method absolute position should match 'fun' keyword",
            funStart, methodNp.getPosition());
        assertTrue("Method should end at or before class end",
            methodNp.getEnd() <= classNp.getEnd());
    }

    @Test
    public void testIfInsideMethodInsideClassHasCorrectPosition()
    {
        // Three levels of nesting: class > method > if
        String source = "package test\n\nclass Foo {\n    fun bar() {\n        if (true) {\n            println()\n        }\n    }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        // Walk: root → class → classInner → method → methodInner → if
        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());

        NodeAndPosition<ParsedNode> methodNp = firstContentChild(classNp);
        assertNotNull("Class inner should contain method", methodNp);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, methodNp.getNode().getNodeType());

        NodeAndPosition<ParsedNode> methodInner = findInner(methodNp);
        NodeAndPosition<ParsedNode> ifNp = methodInner.getNode().findNodeAtOrAfter(
            methodInner.getPosition(), methodInner.getPosition());
        assertNotNull("Method inner should contain if-node", ifNp);
        assertEquals(ParsedNode.NODETYPE_SELECTION, ifNp.getNode().getNodeType());

        // Verify absolute positions match source
        int ifStart = source.indexOf("if (true)");
        assertEquals("if absolute position should match source",
            ifStart, ifNp.getPosition());
        assertTrue("if should be within method bounds",
            ifNp.getPosition() >= methodNp.getPosition()
            && ifNp.getEnd() <= methodNp.getEnd());
    }

    @Test
    public void testKDocClassWithMethodAndIfPositions()
    {
        // Exact scenario from user bug report: KDoc + class + KDoc + method + if
        String source = """
                /**
                 * Class doc.
                 */
                class SomeClass {
                    /**
                     * Method doc.
                     */
                    fun sampleMethod(y: Int): Int {
                        if (y == 0) {
                            return 0
                        }
                        return y
                    }
                }""";
        KotlinParsedCUNode root = buildScopeTree(source);

        // Class
        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull("Should have class node", classNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());

        // Method — inside class's inner node
        int funPos = source.indexOf("fun ");
        NodeAndPosition<ParsedNode> methodNp = firstContentChild(classNp);
        assertNotNull("Class inner should contain method node", methodNp);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, methodNp.getNode().getNodeType());
        assertTrue("Method should start at or before 'fun' keyword",
            methodNp.getPosition() <= funPos);
        assertTrue("Method should end after 'return y'",
            methodNp.getEnd() > source.indexOf("return y"));

        // If — inside method's inner node
        int ifPos = source.indexOf("if (y == 0)");
        NodeAndPosition<ParsedNode> methodInner = findInner(methodNp);
        NodeAndPosition<ParsedNode> ifNp = methodInner.getNode().findNodeAtOrAfter(
            methodInner.getPosition(), methodInner.getPosition());
        assertNotNull("Method inner should contain if-node", ifNp);
        assertEquals(ParsedNode.NODETYPE_SELECTION, ifNp.getNode().getNodeType());
        assertEquals("if absolute position should match source",
            ifPos, ifNp.getPosition());
    }

    @Test
    public void testDeepNestingPositions()
    {
        // Four levels: class > method > if > for
        String source = "package x\nclass A {\n    fun b() {\n        if (true) {\n            for (i in 1..3) {\n                println(i)\n            }\n        }\n    }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        // root → class → classInner → method → methodInner → if → ifInner → for
        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);

        NodeAndPosition<ParsedNode> methodNp = firstContentChild(classNp);
        assertNotNull("Should find method", methodNp);

        NodeAndPosition<ParsedNode> methodInner = findInner(methodNp);
        NodeAndPosition<ParsedNode> ifNp = methodInner.getNode().findNodeAtOrAfter(
            methodInner.getPosition(), methodInner.getPosition());
        assertNotNull("Should find if inside method inner", ifNp);
        assertEquals(ParsedNode.NODETYPE_SELECTION, ifNp.getNode().getNodeType());

        NodeAndPosition<ParsedNode> ifInner = findInner(ifNp);
        NodeAndPosition<ParsedNode> forNp = ifInner.getNode().findNodeAtOrAfter(
            ifInner.getPosition(), ifInner.getPosition());
        assertNotNull("Should find for inside if inner", forNp);
        assertEquals(ParsedNode.NODETYPE_ITERATION, forNp.getNode().getNodeType());

        // Verify absolute positions
        assertEquals("if position", source.indexOf("if (true)"), ifNp.getPosition());
        assertEquals("for position", source.indexOf("for (i"), forNp.getPosition());

        // Verify nesting containment
        assertTrue("for is within if bounds",
            forNp.getPosition() >= ifNp.getPosition()
            && forNp.getEnd() <= ifNp.getEnd());
        assertTrue("if is within method bounds",
            ifNp.getPosition() >= methodNp.getPosition()
            && ifNp.getEnd() <= methodNp.getEnd());
    }

    @Test
    public void testNodePositionWithinParentBounds()
    {
        // Verify that every child node falls within its parent's range
        String source = "class Outer {\n    fun doStuff() {\n        while (true) {\n            if (false) { break }\n        }\n    }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        // root → class → classInner → method → methodInner → while → whileInner → if
        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);
        assertTrue("Class starts within root",
            classNp.getPosition() >= 0 && classNp.getEnd() <= root.getSize());

        NodeAndPosition<ParsedNode> methodNp = firstContentChild(classNp);
        assertNotNull(methodNp);
        assertTrue("Method starts within class",
            methodNp.getPosition() >= classNp.getPosition()
            && methodNp.getEnd() <= classNp.getEnd());

        NodeAndPosition<ParsedNode> methodInner = findInner(methodNp);
        NodeAndPosition<ParsedNode> whileNp = methodInner.getNode().findNodeAtOrAfter(
            methodInner.getPosition(), methodInner.getPosition());
        assertNotNull(whileNp);
        assertEquals(ParsedNode.NODETYPE_ITERATION, whileNp.getNode().getNodeType());
        assertTrue("While starts within method",
            whileNp.getPosition() >= methodNp.getPosition()
            && whileNp.getEnd() <= methodNp.getEnd());

        NodeAndPosition<ParsedNode> whileInner = findInner(whileNp);
        NodeAndPosition<ParsedNode> ifNp = whileInner.getNode().findNodeAtOrAfter(
            whileInner.getPosition(), whileInner.getPosition());
        assertNotNull(ifNp);
        assertEquals(ParsedNode.NODETYPE_SELECTION, ifNp.getNode().getNodeType());
        assertTrue("If starts within while",
            ifNp.getPosition() >= whileNp.getPosition()
            && ifNp.getEnd() <= whileNp.getEnd());
    }
}
