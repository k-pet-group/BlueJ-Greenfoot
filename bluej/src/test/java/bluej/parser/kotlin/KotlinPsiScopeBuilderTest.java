/*
 This file is part of the BlueJ program.
 Copyright (C) 2025,2026  Michael Kolling and John Rosenberg

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

    // Helpers

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
     * Find the inner node of a container, skipping any comment nodes
     * that may precede it (e.g., KDoc attached to the declaration).
     */
    private NodeAndPosition<ParsedNode> findInner(NodeAndPosition<ParsedNode> containerNp)
    {
        NodeAndPosition<ParsedNode> np = containerNp.getNode().findNodeAtOrAfter(
            containerNp.getPosition(), containerNp.getPosition());
        // Skip past comment nodes to find the actual inner node.
        // findNodeAtOrAfter finds nodes whose END >= pos, so we use +1
        // to advance strictly past the current node.
        while (np != null && np.getNode().getNodeType() == ParsedNode.NODETYPE_COMMENT)
        {
            int afterNode = np.getPosition() + np.getSize() + 1;
            np = containerNp.getNode().findNodeAtOrAfter(afterNode, containerNp.getPosition());
        }
        assertNotNull("Container should have inner node (after skipping comments)", np);
        assertTrue("Child should be inner (after skipping comments)",
            np.getNode().isInner());
        assertEquals("Inner node should be NODETYPE_NONE",
            ParsedNode.NODETYPE_NONE, np.getNode().getNodeType());
        return np;
    }

    /**
     * Navigate through the inner node to find the first non-comment content child.
     */
    private NodeAndPosition<ParsedNode> firstContentChild(
            NodeAndPosition<ParsedNode> containerNp)
    {
        NodeAndPosition<ParsedNode> innerNp = findInner(containerNp);
        NodeAndPosition<ParsedNode> np = innerNp.getNode().findNodeAtOrAfter(
            innerNp.getPosition(), innerNp.getPosition());
        // Skip comment nodes to find the first scope child
        while (np != null && np.getNode().getNodeType() == ParsedNode.NODETYPE_COMMENT)
        {
            int afterNode = np.getPosition() + np.getSize() + 1;
            np = innerNp.getNode().findNodeAtOrAfter(afterNode, innerNp.getPosition());
        }
        return np;
    }

    // Tests: Basic class

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

    // Tests: Function declaration

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

    // Tests: Inner nodes

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

    // Tests: Control flow → scope nodes

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

    // Tests: Edge cases

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

    // Tests: Offset accuracy

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

    // Tests: Complex Kotlin patterns

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

    // Tests: Relative position accuracy (nested scopes)

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

    // Tests: Comment nodes

    @Test
    public void testFileLevelBlockCommentCreatesCommentNode()
    {
        // In Kotlin PSI, a block comment before a class is absorbed into the
        // class declaration's text range. So the comment appears as a child of
        // the TYPEDEF container, not as a direct file-level child.
        String source = "/* file-level comment */\nclass Foo { }";
        KotlinParsedCUNode root = buildScopeTree(source);

        // First child at file level should be the class (comment is inside it)
        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull("Should have the class node", classNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());

        // The block comment should be the first child of the class container
        NodeAndPosition<ParsedNode> commentNp = classNp.getNode().findNodeAtOrAfter(
            classNp.getPosition(), classNp.getPosition());
        assertNotNull("Class container should have comment child", commentNp);
        assertEquals("Block comment should be NODETYPE_COMMENT",
            ParsedNode.NODETYPE_COMMENT, commentNp.getNode().getNodeType());
        assertEquals("Comment should start at position 0", 0, commentNp.getPosition());
        assertEquals("Comment size should match '/* file-level comment */'",
            "/* file-level comment */".length(), commentNp.getSize());
    }

    @Test
    public void testFileLevelKDocCreatesCommentNode()
    {
        String source = "/** KDoc comment */\nclass Foo { }";
        KotlinParsedCUNode root = buildScopeTree(source);

        // First child at file level should be the class (KDoc is attached to declaration)
        // KDoc is a child of the KtClass PSI element, so it should appear
        // as a COMMENT node inside the TYPEDEF container
        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());

        // The KDoc should be the first child of the class container
        NodeAndPosition<ParsedNode> docNp = classNp.getNode().findNodeAtOrAfter(
            classNp.getPosition(), classNp.getPosition());
        assertNotNull("Class container should have KDoc comment child", docNp);
        assertEquals("KDoc should be NODETYPE_COMMENT",
            ParsedNode.NODETYPE_COMMENT, docNp.getNode().getNodeType());
    }

    @Test
    public void testEolCommentInsideBlockCreatesCommentNode()
    {
        String source = "fun test() { // line comment\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        assertNotNull(methodNp);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, methodNp.getNode().getNodeType());

        // Find inner, then the comment inside it
        NodeAndPosition<ParsedNode> innerNp = findInner(methodNp);
        NodeAndPosition<ParsedNode> commentNp = innerNp.getNode().findNodeAtOrAfter(
            innerNp.getPosition(), innerNp.getPosition());
        assertNotNull("Method inner should contain a comment node", commentNp);
        assertEquals("EOL comment should be NODETYPE_COMMENT",
            ParsedNode.NODETYPE_COMMENT, commentNp.getNode().getNodeType());
    }

    @Test
    public void testBlockCommentInsideClassBodyCreatesCommentNode()
    {
        // In Kotlin PSI, a block comment before a function in a class body is
        // absorbed into the function's text range. So the comment appears as a
        // child of the METHODDEF container, not of the class inner node.
        String source = "class Foo {\n    /* block comment */\n    fun bar() { }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        // class → inner → method (comment is inside the method container)
        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());

        // The method includes the block comment in its text range
        NodeAndPosition<ParsedNode> methodNp = firstContentChild(classNp);
        assertNotNull("Class inner should contain method node", methodNp);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, methodNp.getNode().getNodeType());

        // The block comment should be the first child of the method container
        NodeAndPosition<ParsedNode> commentNp = methodNp.getNode().findNodeAtOrAfter(
            methodNp.getPosition(), methodNp.getPosition());
        assertNotNull("Method container should have comment child", commentNp);
        assertEquals("Block comment should be NODETYPE_COMMENT",
            ParsedNode.NODETYPE_COMMENT, commentNp.getNode().getNodeType());
    }

    @Test
    public void testKDocAttachedToMethodCreatesCommentNodeInsideMethodContainer()
    {
        String source = "class Foo {\n    /**\n     * Method doc.\n     */\n    fun bar() { }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        // class → inner → method (KDoc is attached, so it's inside the METHODDEF container)
        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        NodeAndPosition<ParsedNode> methodNp = firstContentChild(classNp);
        assertNotNull("Class inner should contain method node", methodNp);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, methodNp.getNode().getNodeType());

        // The KDoc should be the first child of the method container
        NodeAndPosition<ParsedNode> docNp = methodNp.getNode().findNodeAtOrAfter(
            methodNp.getPosition(), methodNp.getPosition());
        assertNotNull("Method container should have KDoc child", docNp);
        assertEquals("KDoc attached to method should be NODETYPE_COMMENT",
            ParsedNode.NODETYPE_COMMENT, docNp.getNode().getNodeType());
    }

    @Test
    public void testMultiLineKDocWithKeywordsDoesNotCreateScopeNodes()
    {
        // This is the canonical bug scenario: keywords inside comments
        // should NOT create scope/keyword nodes
        String source = """
                /**
                 * This class is awesome.
                 * It handles if conditions and for loops.
                 */
                class SomeClass { }""";
        KotlinParsedCUNode root = buildScopeTree(source);

        // The class is the first child at file level (KDoc is attached to it)
        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);
        assertEquals("Should be the class node",
            ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());

        // Inside the class container, the first child should be the KDoc comment
        NodeAndPosition<ParsedNode> docNp = classNp.getNode().findNodeAtOrAfter(
            classNp.getPosition(), classNp.getPosition());
        assertNotNull("Should have KDoc as first child of class container", docNp);
        assertEquals("KDoc should be NODETYPE_COMMENT",
            ParsedNode.NODETYPE_COMMENT, docNp.getNode().getNodeType());

        // Verify the KDoc spans the full comment text
        int kdocStart = source.indexOf("/**");
        int kdocEnd = source.indexOf("*/") + 2;
        assertEquals("KDoc should start at the /** position",
            kdocStart, docNp.getPosition());
        assertEquals("KDoc size should span the entire comment",
            kdocEnd - kdocStart, docNp.getSize());
    }

    @Test
    public void testExistingTestWithKDocStillWorks()
    {
        // Regression: the existing testKDocClassWithMethodAndIfPositions test
        // should continue to work — the class and method nodes must still exist
        // even though we now also create comment nodes.
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

        // Class should still be the first child at file level
        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull("Should have class node", classNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());

        // The class inner should still have a method
        NodeAndPosition<ParsedNode> classInner = findInner(classNp);
        // Find the method — it may not be the first child if there's a KDoc
        // in the class body. Walk children to find METHODDEF.
        NodeAndPosition<ParsedNode> methodNp = null;
        NodeAndPosition<ParsedNode> np = classInner.getNode().findNodeAtOrAfter(
            classInner.getPosition(), classInner.getPosition());
        while (np != null)
        {
            if (np.getNode().getNodeType() == ParsedNode.NODETYPE_METHODDEF)
            {
                methodNp = np;
                break;
            }
            // Move past this node — findNodeAtOrAfter finds nodes whose END >= pos,
            // so +1 ensures we advance strictly past the current node.
            int afterNode = np.getPosition() + np.getSize() + 1;
            np = classInner.getNode().findNodeAtOrAfter(afterNode, classInner.getPosition());
        }
        assertNotNull("Class inner should still contain method node", methodNp);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, methodNp.getNode().getNodeType());

        // Method should still have an if inside it
        NodeAndPosition<ParsedNode> methodInner = findInner(methodNp);
        NodeAndPosition<ParsedNode> ifNp = methodInner.getNode().findNodeAtOrAfter(
            methodInner.getPosition(), methodInner.getPosition());
        assertNotNull("Method inner should contain if-node", ifNp);
        assertEquals(ParsedNode.NODETYPE_SELECTION, ifNp.getNode().getNodeType());
    }

    // Tests: Secondary constructors

    @Test
    public void testSecondaryConstructorCreatesMethodNode()
    {
        String source = "class Foo {\n    constructor(x: Int) { println(x) }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());

        // Constructor is inside the class's inner node
        NodeAndPosition<ParsedNode> ctorNp = firstContentChild(classNp);
        assertNotNull("Class inner should contain a constructor node", ctorNp);
        assertEquals("Constructor should be NODETYPE_METHODDEF",
            ParsedNode.NODETYPE_METHODDEF, ctorNp.getNode().getNodeType());

        // Constructor should have an inner node
        NodeAndPosition<ParsedNode> innerNp = findInner(ctorNp);
        assertTrue("Inner should be within constructor bounds",
            innerNp.getPosition() > ctorNp.getPosition()
            && innerNp.getEnd() < ctorNp.getEnd());
    }

    @Test
    public void testSecondaryConstructorWithDelegation()
    {
        String source = "class Foo(val x: Int) {\n    constructor(x: Int, y: Int) : this(x) { println(y) }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);

        // Constructor is the first content child (primary constructor is skipped)
        NodeAndPosition<ParsedNode> ctorNp = firstContentChild(classNp);
        assertNotNull("Class inner should contain secondary constructor", ctorNp);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, ctorNp.getNode().getNodeType());

        // The scope should cover the full constructor text including delegation call
        int ctorStart = source.indexOf("constructor");
        assertEquals("Constructor scope should start at 'constructor' keyword",
            ctorStart, ctorNp.getPosition());
        assertTrue("Constructor scope should include delegation call",
            ctorNp.getEnd() > source.indexOf("this(x)"));
    }

    @Test
    public void testSecondaryConstructorWithNestedControlFlow()
    {
        String source = "class Foo {\n    constructor(x: Int) {\n        if (x > 0) { println(x) }\n    }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        NodeAndPosition<ParsedNode> ctorNp = firstContentChild(classNp);
        assertNotNull("Should have constructor", ctorNp);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, ctorNp.getNode().getNodeType());

        // Find the if inside the constructor's inner node
        NodeAndPosition<ParsedNode> ctorInner = findInner(ctorNp);
        NodeAndPosition<ParsedNode> ifNp = ctorInner.getNode().findNodeAtOrAfter(
            ctorInner.getPosition(), ctorInner.getPosition());
        assertNotNull("Constructor inner should contain if-node", ifNp);
        assertEquals("if should be NODETYPE_SELECTION",
            ParsedNode.NODETYPE_SELECTION, ifNp.getNode().getNodeType());
    }

    // Tests: Primary constructors (skipped)

    @Test
    public void testPrimaryConstructorSkipped()
    {
        String source = "class Foo(val x: Int) { }";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());

        // The class inner should have NO children — primary constructor has no scope
        NodeAndPosition<ParsedNode> classInner = findInner(classNp);
        NodeAndPosition<ParsedNode> childNp = classInner.getNode().findNodeAtOrAfter(
            classInner.getPosition(), classInner.getPosition());
        assertNull("Primary constructor should NOT create a scope node", childNp);
    }

    @Test
    public void testPrimaryConstructorWithExplicitKeyword()
    {
        String source = "class Foo constructor(val x: Int) { }";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());

        // Even with explicit 'constructor' keyword, no scope node for primary constructor
        NodeAndPosition<ParsedNode> classInner = findInner(classNp);
        NodeAndPosition<ParsedNode> childNp = classInner.getNode().findNodeAtOrAfter(
            classInner.getPosition(), classInner.getPosition());
        assertNull("Explicit primary constructor should NOT create a scope node", childNp);
    }

    // Tests: Init blocks

    @Test
    public void testInitBlockCreatesMethodNode()
    {
        String source = "class Foo {\n    init { println(\"hi\") }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);

        NodeAndPosition<ParsedNode> initNp = firstContentChild(classNp);
        assertNotNull("Class inner should contain an init block node", initNp);
        assertEquals("Init block should be NODETYPE_METHODDEF",
            ParsedNode.NODETYPE_METHODDEF, initNp.getNode().getNodeType());

        // Init block should have an inner node
        NodeAndPosition<ParsedNode> innerNp = findInner(initNp);
        assertTrue("Inner should be within init block bounds",
            innerNp.getPosition() > initNp.getPosition()
            && innerNp.getEnd() < initNp.getEnd());
    }

    @Test
    public void testInitBlockWithNestedControlFlow()
    {
        String source = "class Foo {\n    init {\n        for (i in 1..10) { println(i) }\n    }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        NodeAndPosition<ParsedNode> initNp = firstContentChild(classNp);
        assertNotNull("Should have init block", initNp);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, initNp.getNode().getNodeType());

        // Find the for loop inside the init block's inner node
        NodeAndPosition<ParsedNode> initInner = findInner(initNp);
        NodeAndPosition<ParsedNode> forNp = initInner.getNode().findNodeAtOrAfter(
            initInner.getPosition(), initInner.getPosition());
        assertNotNull("Init inner should contain for-node", forNp);
        assertEquals("for should be NODETYPE_ITERATION",
            ParsedNode.NODETYPE_ITERATION, forNp.getNode().getNodeType());
    }

    @Test
    public void testMultipleInitBlocks()
    {
        String source = "class Foo {\n    init { println(\"first\") }\n    init { println(\"second\") }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);

        NodeAndPosition<ParsedNode> classInner = findInner(classNp);

        // First init block
        NodeAndPosition<ParsedNode> init1Np = classInner.getNode().findNodeAtOrAfter(
            classInner.getPosition(), classInner.getPosition());
        assertNotNull("Should have first init block", init1Np);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, init1Np.getNode().getNodeType());

        // Second init block — search after the first one ends
        int afterFirst = init1Np.getPosition() + init1Np.getSize() + 1;
        NodeAndPosition<ParsedNode> init2Np = classInner.getNode().findNodeAtOrAfter(
            afterFirst, classInner.getPosition());
        assertNotNull("Should have second init block", init2Np);
        assertEquals("Second init should also be NODETYPE_METHODDEF",
            ParsedNode.NODETYPE_METHODDEF, init2Np.getNode().getNodeType());

        // Both should have inner nodes
        findInner(init1Np);
        findInner(init2Np);
    }

    @Test
    public void testInitBlockAndConstructorOrdering()
    {
        String source = "class Foo {\n    init { println(\"init\") }\n    constructor(x: Int) { println(x) }\n    fun bar() { }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);

        NodeAndPosition<ParsedNode> classInner = findInner(classNp);

        // First: init block
        NodeAndPosition<ParsedNode> np1 = classInner.getNode().findNodeAtOrAfter(
            classInner.getPosition(), classInner.getPosition());
        assertNotNull("Should have first scope node (init)", np1);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, np1.getNode().getNodeType());
        assertTrue("First node should be the init block",
            np1.getPosition() == source.indexOf("init {"));

        // Second: constructor
        int afterFirst = np1.getPosition() + np1.getSize() + 1;
        NodeAndPosition<ParsedNode> np2 = classInner.getNode().findNodeAtOrAfter(
            afterFirst, classInner.getPosition());
        assertNotNull("Should have second scope node (constructor)", np2);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, np2.getNode().getNodeType());
        assertEquals("Second node should be the constructor",
            source.indexOf("constructor"), np2.getPosition());

        // Third: method
        int afterSecond = np2.getPosition() + np2.getSize() + 1;
        NodeAndPosition<ParsedNode> np3 = classInner.getNode().findNodeAtOrAfter(
            afterSecond, classInner.getPosition());
        assertNotNull("Should have third scope node (method)", np3);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, np3.getNode().getNodeType());
        assertEquals("Third node should be the method",
            source.indexOf("fun bar"), np3.getPosition());
    }

    // Tests: Companion object transparency

    @Test
    public void testCompanionObjectTransparency()
    {
        String source = "class Foo {\n    companion object {\n        fun create(): Foo = Foo()\n    }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());

        // The companion object's fun create() should be promoted to the class body level
        // — no intermediate TYPEDEF for the companion object
        NodeAndPosition<ParsedNode> methodNp = firstContentChild(classNp);
        assertNotNull("Companion method should be promoted to class level", methodNp);
        assertEquals("Promoted method should be NODETYPE_METHODDEF",
            ParsedNode.NODETYPE_METHODDEF, methodNp.getNode().getNodeType());
    }

    @Test
    public void testNamedCompanionObjectTransparency()
    {
        String source = "class Foo {\n    companion object Factory {\n        fun create(): Foo = Foo()\n    }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());

        // Named companion object should also be transparent
        NodeAndPosition<ParsedNode> methodNp = firstContentChild(classNp);
        assertNotNull("Named companion method should be promoted to class level", methodNp);
        assertEquals("Promoted method should be NODETYPE_METHODDEF",
            ParsedNode.NODETYPE_METHODDEF, methodNp.getNode().getNodeType());
    }

    // Tests: Containment checks

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
