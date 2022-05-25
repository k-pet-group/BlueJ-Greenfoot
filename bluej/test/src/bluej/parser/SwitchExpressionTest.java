/*
 This file is part of the BlueJ program. 
 Copyright (C) 2022  Michael Kolling and John Rosenberg
 
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
package bluej.parser;

import bluej.JavaFXThreadingRule;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.nodes.NodeTree;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedNode;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static bluej.parser.ParseUtility.Parsed;
import static bluej.parser.ParseUtility.findInnerNode;
import static bluej.parser.ParseUtility.findMethodBody;
import static bluej.parser.ParseUtility.parse;
import static org.junit.Assert.assertEquals;

/**
 * Test switch expressions.  Many tests are borrowed from
 * https://docs.oracle.com/en/java/javase/13/language/switch-expressions.html
 */
public class SwitchExpressionTest
{
    @Rule
    public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

    @BeforeClass
    public static void initConfig()
    {
        InitConfig.init();
    }

    private TestEntityResolver resolver;

    @Before
    public void setUp() throws Exception
    {
        resolver = new TestEntityResolver(new ClassLoaderResolver(this.getClass().getClassLoader()));
    }
    
    /**
     * Wraps the content of a method into a class with a Day enum declaration.
     */
    public String boilerplated(String methodContent)
    {
        return
            """
            /*class*/class Foo {/*class-inner*/
                public enum Day { SUNDAY, MONDAY, TUESDAY,
                                WEDNESDAY, THURSDAY, FRIDAY, SATURDAY; }
                             
                /*method*/void method(Day day) {/*method-inner*/
            """ + methodContent +
            """
                }
            }
            """;
    }

    /**
     * Given a suitably annotated switch statement (see testStandardSwitch() for an example),
     * picks out the node corresponding to the body of the switch.
     */
    private NodeAndPosition<ParsedNode> getSwitchBody(Parsed p)
    {
        NodeAndPosition<ParsedNode> methodBody = findMethodBody(p);
        NodeAndPosition<ParsedNode> switchNode = findInnerNode(p, methodBody, "switch", false, ParsedNode.NODETYPE_SELECTION);
        assertEquals(p.positionStart("end-switch"), switchNode.getEnd());

        NodeAndPosition<ParsedNode> switchExpressionNode = findInnerNode(p, switchNode, "expression", false, ParsedNode.NODETYPE_EXPRESSION);
        assertEquals(p.positionEnd("end-expression"), switchExpressionNode.getEnd());

        ImmutableList<NodeAndPosition<ParsedNode>> switchContent = ImmutableList.copyOf(switchNode.getNode().getChildren(switchNode.getPosition()));
        // The comment, the expression and the body
        assertEquals(3, switchContent.size());
        NodeAndPosition<ParsedNode> switchBody = switchContent.get(2);
        assertEquals(p.positionStart("switch-inner"), switchBody.getPosition());
        return switchBody;
    }
    
    @Test
    public void testStandardSwitch()
    {
        Parsed p = parse(boilerplated(
        """
                        int numLetters = 0;
                        /*switch*/switch (/*expression*/day/*end-expression*/) {/*switch-inner*/
                            case MONDAY:
                            case FRIDAY:
                            case SUNDAY:
                                numLetters = 6;
                                break;
                            case TUESDAY:
                                numLetters = 7;
                                break;
                            case THURSDAY:
                            case SATURDAY:
                                numLetters = 8;
                                break;
                            case WEDNESDAY:
                                numLetters = 9;
                                break;
                            default:
                                throw new IllegalStateException("Invalid day: " + day);
                        }/*end-switch*/
                        System.out.println(numLetters);
                """), resolver);

        NodeAndPosition<ParsedNode> switchBody = getSwitchBody(p);

        // All the lines in the switch -- case, statements, break -- are all direct children: 
        ImmutableList<NodeTree.NodeAndPosition<ParsedNode>> switchBodyContent = ImmutableList.copyOf(switchBody.getNode().getChildren(switchBody.getPosition()));
        // Break and throw are ignored, only the expressions get put in the tree:
        assertEquals(Arrays.asList(
                "/*switch-inner*/", "MONDAY", "FRIDAY", "SUNDAY", "numLetters = 6", "TUESDAY", "numLetters = 7", "THURSDAY", "SATURDAY", "numLetters = 8", "WEDNESDAY", "numLetters = 9", "new IllegalStateException(\"Invalid day: \" + day)"
        ), switchBodyContent.stream().map(nap -> p.nodeContent(nap)).collect(Collectors.toList()));
    }

    @Test
    public void testNewSimpleSwitch1()
    {
        Parsed p = parse(boilerplated(
            """
            /*switch*/switch (/*expression*/day/*end-expression*/)
            {/*switch-inner*/
                case MONDAY, FRIDAY, SUNDAY -> System.out.println(6);
                case TUESDAY                -> System.out.println(7);
                case THURSDAY, SATURDAY     -> System.out.println(8);
                case WEDNESDAY              -> System.out.println(9);
            }/*end-switch*/
            """), resolver);

        NodeAndPosition<ParsedNode> switchBody = getSwitchBody(p);

        // Each case is a direct child: 
        ImmutableList<NodeTree.NodeAndPosition<ParsedNode>> switchBodyContent = ImmutableList.copyOf(switchBody.getNode().getChildren(switchBody.getPosition()));

        assertEquals(Arrays.asList(
            "/*switch-inner*/", "MONDAY", "FRIDAY", "SUNDAY", "System.out.println(6)", "TUESDAY", "System.out.println(7)", "THURSDAY", "SATURDAY", "System.out.println(8)", "WEDNESDAY", "System.out.println(9)"
        ), switchBodyContent.stream().map(nap -> p.nodeContent(nap).trim()).collect(Collectors.toList()));

    }

    @Test
    public void testNewSimpleSwitch2()
    {
        Parsed p = parse(boilerplated(
            """
            /*switch*/switch (/*expression*/day/*end-expression*/)
            {/*switch-inner*/
                case a -> {System.out.println(1);}
                case e -> throw new Exception();
                case f -> {return 2;}
                default -> {if (true) return 7;}
            }/*end-switch*/
            """), resolver);

        NodeAndPosition<ParsedNode> switchBody = getSwitchBody(p);

        // Each case is a direct child: 
        ImmutableList<NodeTree.NodeAndPosition<ParsedNode>> switchBodyContent = ImmutableList.copyOf(switchBody.getNode().getChildren(switchBody.getPosition()));

        assertEquals(Arrays.asList(
            "/*switch-inner*/", "a", "{System.out.println(1);}", "e", "new Exception()", "f", "{return 2;}", "{if (true) return 7;}"
        ), switchBodyContent.stream().map(nap -> p.nodeContent(nap).trim()).collect(Collectors.toList()));
    }

    @Test
    public void testSwitchExpression1()
    {
        Parsed p = parse(boilerplated(
            """
            Day day = Day.WEDNESDAY;
            /*print*/System.out.println(
                /*switch*/switch (/*expression*/day/*end-expression*/) {/*switch-inner*/
                    case MONDAY, FRIDAY, SUNDAY -> 6;
                    case TUESDAY                -> 7;
                    case THURSDAY, SATURDAY     -> 8;
                    case WEDNESDAY              -> 9;
                    default -> throw new IllegalStateException("Invalid day: " + day);
                }/*end-switch*/
            );
            """), resolver);

        NodeTree.NodeAndPosition<ParsedNode> methodBody = findMethodBody(p);
        NodeTree.NodeAndPosition<ParsedNode> methodCallNode = findInnerNode(p, methodBody, "print", false, ParsedNode.NODETYPE_EXPRESSION);
        NodeTree.NodeAndPosition<ParsedNode> switchNode = findInnerNode(p, methodCallNode, "switch", false, ParsedNode.NODETYPE_EXPRESSION);
        switchNode = switchNode.getNode().getChildren(switchNode.getPosition()).next();
        assertEquals(p.positionStart("end-switch"), switchNode.getEnd());

        NodeTree.NodeAndPosition<ParsedNode> switchExpressionNode = findInnerNode(p, switchNode, "expression", false, ParsedNode.NODETYPE_EXPRESSION);
        assertEquals(p.positionEnd("end-expression"), switchExpressionNode.getEnd());

        ImmutableList<NodeTree.NodeAndPosition<ParsedNode>> switchContent = ImmutableList.copyOf(switchNode.getNode().getChildren(switchNode.getPosition()));
        // The comment, the expression and the body
        assertEquals(3, switchContent.size());
        NodeTree.NodeAndPosition<ParsedNode> switchBody = switchContent.get(2);
        assertEquals(p.positionStart("switch-inner"), switchBody.getPosition());

        // All the lines in the switch -- case, statements, break -- are all direct children: 
        ImmutableList<NodeTree.NodeAndPosition<ParsedNode>> switchBodyContent = ImmutableList.copyOf(switchBody.getNode().getChildren(switchBody.getPosition()));
        // Break and throw are ignored, only the expressions get put in the tree:
        assertEquals(Arrays.asList(
            "/*switch-inner*/", "MONDAY", "FRIDAY", "SUNDAY", "6", "TUESDAY", "7", "THURSDAY", "SATURDAY", "8", "WEDNESDAY", "9", "new IllegalStateException(\"Invalid day: \" + day)"
        ), switchBodyContent.stream().map(nap -> p.nodeContent(nap).trim()).collect(Collectors.toList()));

    }

    @Test
    public void testSwitchExpression2()
    {
        Parsed p = parse(boilerplated(
            """
                int k = 7;
                /*assign*/String value = /*plus*/"k is " + /*switch*/switch (/*expression*/k/*end-expression*/) {/*switch-inner*/
                   case  1 -> "one";
                   case  2 -> "two";
                   default -> "many";
                }/*end-switch*/;
                """), resolver);

        NodeTree.NodeAndPosition<ParsedNode> methodBody = findMethodBody(p);
        NodeTree.NodeAndPosition<ParsedNode> varNode = findInnerNode(p, methodBody, "assign", false, ParsedNode.NODETYPE_FIELD);
        NodeTree.NodeAndPosition<ParsedNode> plusNode = findInnerNode(p, varNode, "plus", false, ParsedNode.NODETYPE_EXPRESSION);
        NodeTree.NodeAndPosition<ParsedNode> switchNode = findInnerNode(p, plusNode, "switch", false, ParsedNode.NODETYPE_SELECTION);
        assertEquals(p.positionStart("end-switch"), switchNode.getEnd());

        NodeTree.NodeAndPosition<ParsedNode> switchExpressionNode = findInnerNode(p, switchNode, "expression", false, ParsedNode.NODETYPE_EXPRESSION);
        assertEquals(p.positionEnd("end-expression"), switchExpressionNode.getEnd());

        ImmutableList<NodeTree.NodeAndPosition<ParsedNode>> switchContent = ImmutableList.copyOf(switchNode.getNode().getChildren(switchNode.getPosition()));
        // The comment, the expression and the body
        assertEquals(3, switchContent.size());
        NodeTree.NodeAndPosition<ParsedNode> switchBody = switchContent.get(2);
        assertEquals(p.positionStart("switch-inner"), switchBody.getPosition());

        // All the lines in the switch -- case, statements, break -- are all direct children: 
        ImmutableList<NodeTree.NodeAndPosition<ParsedNode>> switchBodyContent = ImmutableList.copyOf(switchBody.getNode().getChildren(switchBody.getPosition()));
        // Break and throw are ignored, only the expressions get put in the tree:
        assertEquals(Arrays.asList(
            "/*switch-inner*/", "1", "\"one\"", "2", "\"two\"", "\"many\""
        ), switchBodyContent.stream().map(nap -> p.nodeContent(nap).trim()).collect(Collectors.toList()));

    }

    @Test
    public void testSwitchExpression3()
    {
        Parsed p = parse(boilerplated(
            """
            /*assign*/int j = /*switch*/switch (/*expression*/day/*end-expression*/) {/*switch-inner*/
                case MONDAY  -> 0;
                case TUESDAY -> 1;
                default      -> {
                    int k = day.toString().length();
                    int result = f(k);
                    yield result;
                }
            }/*end-switch*/;
            """), resolver);

        NodeTree.NodeAndPosition<ParsedNode> methodBody = findMethodBody(p);
        NodeTree.NodeAndPosition<ParsedNode> varNode = findInnerNode(p, methodBody, "assign", false, ParsedNode.NODETYPE_FIELD);
        NodeTree.NodeAndPosition<ParsedNode> switchNode = findInnerNode(p, varNode, "switch", false, ParsedNode.NODETYPE_EXPRESSION);
        switchNode = switchNode.getNode().getChildren(switchNode.getPosition()).next();
        assertEquals(p.positionStart("end-switch"), switchNode.getEnd());

        NodeTree.NodeAndPosition<ParsedNode> switchExpressionNode = findInnerNode(p, switchNode, "expression", false, ParsedNode.NODETYPE_EXPRESSION);
        assertEquals(p.positionEnd("end-expression"), switchExpressionNode.getEnd());

        ImmutableList<NodeTree.NodeAndPosition<ParsedNode>> switchContent = ImmutableList.copyOf(switchNode.getNode().getChildren(switchNode.getPosition()));
        // The comment, the expression and the body
        assertEquals(3, switchContent.size());
        NodeTree.NodeAndPosition<ParsedNode> switchBody = switchContent.get(2);
        assertEquals(p.positionStart("switch-inner"), switchBody.getPosition());

        // All the lines in the switch -- case, statements, break -- are all direct children: 
        ImmutableList<NodeTree.NodeAndPosition<ParsedNode>> switchBodyContent = ImmutableList.copyOf(switchBody.getNode().getChildren(switchBody.getPosition()));
        // Break and throw are ignored, only the expressions get put in the tree:
        assertEquals(Arrays.asList(
            "/*switch-inner*/", "MONDAY", "0", "TUESDAY", "1",
"""
{
        int k = day.toString().length();
        int result = f(k);
        yield result;
    }"""
        ), switchBodyContent.stream().map(nap -> p.nodeContent(nap).trim()).collect(Collectors.toList()));

    }

    @Test
    public void testSwitchExpression4()
    {
        Parsed p = parse(boilerplated(
            """
            /*assign*/int result = /*switch*/switch (/*expression*/s/*end-expression*/) {/*switch-inner*/
                case "Foo":
                    yield 1;
                case "Bar":
                    yield 2;
                default:
                    System.out.println("Neither Foo nor Bar, hmmm...");
                    yield 0;
            }/*end-switch*/;
            """), resolver);

        NodeTree.NodeAndPosition<ParsedNode> methodBody = findMethodBody(p);
        NodeTree.NodeAndPosition<ParsedNode> varNode = findInnerNode(p, methodBody, "assign", false, ParsedNode.NODETYPE_FIELD);
        NodeTree.NodeAndPosition<ParsedNode> switchNode = findInnerNode(p, varNode, "switch", false, ParsedNode.NODETYPE_EXPRESSION);
        switchNode = switchNode.getNode().getChildren(switchNode.getPosition()).next();
        assertEquals(p.positionStart("end-switch"), switchNode.getEnd());

        NodeTree.NodeAndPosition<ParsedNode> switchExpressionNode = findInnerNode(p, switchNode, "expression", false, ParsedNode.NODETYPE_EXPRESSION);
        assertEquals(p.positionEnd("end-expression"), switchExpressionNode.getEnd());

        ImmutableList<NodeTree.NodeAndPosition<ParsedNode>> switchContent = ImmutableList.copyOf(switchNode.getNode().getChildren(switchNode.getPosition()));
        // The comment, the expression and the body
        assertEquals(3, switchContent.size());
        NodeTree.NodeAndPosition<ParsedNode> switchBody = switchContent.get(2);
        assertEquals(p.positionStart("switch-inner"), switchBody.getPosition());

        // All the lines in the switch -- case, statements, break -- are all direct children: 
        ImmutableList<NodeTree.NodeAndPosition<ParsedNode>> switchBodyContent = ImmutableList.copyOf(switchBody.getNode().getChildren(switchBody.getPosition()));
        // Break and throw are ignored, only the expressions get put in the tree:
        assertEquals(Arrays.asList(
            "/*switch-inner*/", "\"Foo\"", "1", "\"Bar\"", "2", "System.out.println(\"Neither Foo nor Bar, hmmm...\")", "0"
        ), switchBodyContent.stream().map(nap -> p.nodeContent(nap).trim()).collect(Collectors.toList()));

    }

    @Test
    public void testSwitchExpression5()
    {
        // Check lambda in a new switch is associated correctly:
        // case a -> b -> c;
        // should parse as case a -> (b -> c), NOT as case (a -> b) -> c
        Parsed p = parse(boilerplated(
            """
            /*print*/System.out.println(/*switch*/switch (/*expression*/day/*end-expression*/)
            {/*switch-inner*/
                case a -> b -> c;
                case e -> f -> g -> h;
                default -> i -> j;
            }/*end-switch*/);
            """), resolver);

        NodeTree.NodeAndPosition<ParsedNode> methodBody = findMethodBody(p);
        NodeTree.NodeAndPosition<ParsedNode> methodCallNode = findInnerNode(p, methodBody, "print", false, ParsedNode.NODETYPE_EXPRESSION);
        NodeTree.NodeAndPosition<ParsedNode> switchNode = findInnerNode(p, methodCallNode, "switch", false, ParsedNode.NODETYPE_EXPRESSION);
        switchNode = switchNode.getNode().getChildren(switchNode.getPosition()).next();
        assertEquals(p.positionStart("end-switch"), switchNode.getEnd());

        NodeTree.NodeAndPosition<ParsedNode> switchExpressionNode = findInnerNode(p, switchNode, "expression", false, ParsedNode.NODETYPE_EXPRESSION);
        assertEquals(p.positionEnd("end-expression"), switchExpressionNode.getEnd());

        ImmutableList<NodeTree.NodeAndPosition<ParsedNode>> switchContent = ImmutableList.copyOf(switchNode.getNode().getChildren(switchNode.getPosition()));
        // The comment, the expression and the body
        assertEquals(3, switchContent.size());
        NodeTree.NodeAndPosition<ParsedNode> switchBody = switchContent.get(2);
        assertEquals(p.positionStart("switch-inner"), switchBody.getPosition());
        
        // Each case is a direct child: 
        ImmutableList<NodeTree.NodeAndPosition<ParsedNode>> switchBodyContent = ImmutableList.copyOf(switchBody.getNode().getChildren(switchBody.getPosition()));

        assertEquals(Arrays.asList(
            "/*switch-inner*/", "a", "b -> c", "e", "f -> g -> h", "i -> j"
        ), switchBodyContent.stream().map(nap -> p.nodeContent(nap).trim()).collect(Collectors.toList()));
    }
}
