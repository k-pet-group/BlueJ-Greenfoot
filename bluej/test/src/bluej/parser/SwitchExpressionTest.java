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
    
    @Test
    public void testStandardSwitch()
    {
        Parsed p = parse(
        """
                /*class*/class Foo {/*class-inner*/
                    public enum Day { SUNDAY, MONDAY, TUESDAY,
                                    WEDNESDAY, THURSDAY, FRIDAY, SATURDAY; }
                                 
                    /*method*/void method(Day day) {/*method-inner*/
                        int numLetters = 0;
                        Day day = Day.WEDNESDAY;
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
                    }
                }
                """, resolver);

        NodeTree.NodeAndPosition<ParsedNode> methodBody = findMethodBody(p);
        NodeTree.NodeAndPosition<ParsedNode> switchNode = findInnerNode(p, methodBody, "switch", false, ParsedNode.NODETYPE_SELECTION);
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
                "/*switch-inner*/", "MONDAY", "FRIDAY", "SUNDAY", "numLetters = 6", "TUESDAY", "numLetters = 7", "THURSDAY", "SATURDAY", "numLetters = 8", "WEDNESDAY", "numLetters = 9", "new IllegalStateException(\"Invalid day: \" + day)"
        ), switchBodyContent.stream().map(nap -> p.nodeContent(nap)).collect(Collectors.toList()));
        
    }
}
