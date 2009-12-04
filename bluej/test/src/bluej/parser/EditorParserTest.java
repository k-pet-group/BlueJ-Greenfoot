/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.text.BadLocationException;

import junit.framework.TestCase;
import bluej.Boot;
import bluej.Config;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.MethodReflective;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.parser.entity.ClassEntity;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

public class EditorParserTest extends TestCase
{
    {
        File bluejLibDir = Boot.getBluejLibDir();
        Config.initialise(bluejLibDir, new Properties(), false);
    }
    
    private TestEntityResolver resolver;
    
    @Override
    protected void setUp() throws Exception
    {
        resolver = new TestEntityResolver(new ClassLoaderResolver(this.getClass().getClassLoader()));
    }
    
    @Override
    protected void tearDown() throws Exception
    {
    }
    
    /**
     * Generate a compilation unit node based on some source code.
     */
    private ParsedCUNode cuForSource(String sourceCode)
    {
        MoeSyntaxDocument document = new MoeSyntaxDocument(resolver);
        try {
            document.insertString(0, sourceCode, null);
        }
        catch (BadLocationException ble) {}
        return document.getParser();
    }

    public void test1()
    {
        String sourceCode = ""
            + "class A\n"       // position 0
            + "{\n"             // position 8 
            + "   class B\n"    // position 10 
            + "    {\n"         // position 21 
            + "    }\n"
            + "}\n";
            
        ParsedCUNode pcuNode = cuForSource(sourceCode);
        NodeAndPosition classNP = pcuNode.findNodeAtOrAfter(0, 0);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNP.getNode().getNodeType());
        assertEquals(0, classNP.getPosition());
        
        NodeAndPosition innerNP = classNP.getNode().findNodeAtOrAfter(9, 0);
        
        NodeAndPosition classBNP = innerNP.getNode().findNodeAtOrAfter(innerNP.getPosition(),
                innerNP.getPosition());
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classBNP.getNode().getNodeType());
        assertEquals(13, classBNP.getPosition());
    }
    
    /**
     * Test that a method defined inside a class is recognized properly.
     */
    public void test2()
    {
        String aClassSrc = "class A {\n" +
        "  public String someMethod() {\n" +
        "    return \"hello\";\n" +
        "  }\n" +
        "}\n";

        ParsedCUNode aNode = cuForSource(aClassSrc);
        resolver.addCompilationUnit("", aNode);
        
        ClassEntity aClassEnt = resolver.resolvePackageOrClass("A", "A").resolveAsType();
        GenTypeClass aClass = aClassEnt.getType().getCapture().asClass();
        Map<String,Set<MethodReflective>> methods = aClass.getReflective().getDeclaredMethods();
        Set<MethodReflective> mset = methods.get("someMethod");
        assertEquals(1, mset.size());
        
        MethodReflective method = mset.iterator().next();
        assertEquals("java.lang.String", method.getReturnType().toString(false));
    }

    /**
     * Test that a broken method call doesn't interfere with containing method position/size
     */
    public void test3()
    {
        String aClassSrc = "class A {\n" +   // position 0
        "  public void someMethod() {\n" +   // position 10
        "    methodCall(\n" +                // position 39 
        "  }\n" +                            // position 55 
        "}\n";

        ParsedCUNode aNode = cuForSource(aClassSrc);
        NodeAndPosition classNP = aNode.findNodeAtOrAfter(0, 0);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNP.getNode().getNodeType());
        assertEquals(0, classNP.getPosition());
        
        NodeAndPosition innerNP = classNP.getNode().findNodeAtOrAfter(9, 0);
        
        NodeAndPosition methodNP = innerNP.getNode().findNodeAtOrAfter(
                innerNP.getPosition(), innerNP.getPosition());
        
        assertEquals(12, methodNP.getPosition());
        assertEquals(58, methodNP.getPosition() + methodNP.getSize());
    }

}
