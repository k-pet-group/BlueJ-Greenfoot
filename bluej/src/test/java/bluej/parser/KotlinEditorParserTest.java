/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013,2014,2019,2022  Michael Kolling and John Rosenberg 
 
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

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.MethodReflective;
import bluej.extensions2.SourceType;
import bluej.parser.entity.*;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class KotlinEditorParserTest
{
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
     * Generate a compilation unit node based on some source code.
     */
    private ParsedCUNode cuForSource(String sourceCode, String pkg)
    {
        EntityResolver resolver = new PackageResolver(this.resolver, pkg);
        TestableDocument document = new TestableDocument(resolver, SourceType.Kotlin);
        document.enableParser(true);
        document.insertString(0, sourceCode);
        return document.getParser();
    }

    @Test
    public void test1()
    {
        String sourceCode = ""
            + "class A\n"       // position 0
            + "{\n"             // position 8 
            + "   class B\n"    // position 10 
            + "    {\n"         // position 21 
            + "    }\n"
            + "}\n";
            
        ParsedCUNode pcuNode = cuForSource(sourceCode, "");
        NodeAndPosition<ParsedNode> classNP = pcuNode.findNodeAtOrAfter(0, 0);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNP.getNode().getNodeType());
        assertEquals(0, classNP.getPosition());
        
        NodeAndPosition<ParsedNode> innerNP = classNP.getNode().findNodeAtOrAfter(9, 0);
        
        NodeAndPosition<ParsedNode> classBNP = innerNP.getNode().findNodeAtOrAfter(innerNP.getPosition(),
                innerNP.getPosition());
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classBNP.getNode().getNodeType());
        assertEquals(13, classBNP.getPosition());
    }

    /**
     * Test that a method defined inside a class is recognized properly.
     */
    @Test
    public void test2()
    {
        String aClassSrc = "class A {\n" +
                "  fun hello() : String {\n" +
                "    return \"hello\";\n" +
                "  }\n" +
                "}\n";

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);

        EntityResolver resolver = new PackageResolver(this.resolver, "");
        TypeEntity aClassEnt = resolver.resolvePackageOrClass("A", null).resolveAsType();
        GenTypeClass aClass = aClassEnt.getType().asClass();
        Map<String,Set<MethodReflective>> methods = aClass.getReflective().getDeclaredMethods();
        Set<MethodReflective> mset = methods.get("hello");
        assertNotNull(mset);
        assertEquals(1, mset.size());

        MethodReflective method = mset.iterator().next();
        assertEquals("java.lang.String", method.getReturnType().toString(false));
    }


}
