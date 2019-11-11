/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013,2014,2019  Michael Kolling and John Rosenberg 
 
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

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bluej.JavaFXThreadingRule;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.MethodReflective;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.PackageResolver;
import bluej.parser.entity.TypeEntity;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class EditorParserTest
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
     * Generate a compilation unit node based on some source code.
     */
    private ParsedCUNode cuForSource(String sourceCode, String pkg)
    {
        EntityResolver resolver = new PackageResolver(this.resolver, pkg);
        TestableDocument document = new TestableDocument(resolver);
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

    @Test
    public void test1a()
    {
        String sourceCode = ""
                + "class A\n"       // position 0
                + "{\n"             // position 8 
                + "   class B\n"    // position 10 
                + "    {\n"         // position 21 
                + "    }\n"
                + "}\n";
                
        ParsedCUNode pcuNode = cuForSource(sourceCode, "");
        
        resolver.addCompilationUnit("", pcuNode);
        
        TypeEntity aEntity = pcuNode.resolvePackageOrClass("A", null).resolveAsType();
        assertNotNull(aEntity);
        TypeEntity bEntity = aEntity.getPackageOrClassMember("B");
        assertNotNull(bEntity);
    }
    
    /**
     * Test that a method defined inside a class is recognized properly.
     */
    @Test
    public void test2()
    {
        String aClassSrc = "class A {\n" +
        "  public String someMethod() {\n" +
        "    return \"hello\";\n" +
        "  }\n" +
        "}\n";

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        EntityResolver resolver = new PackageResolver(this.resolver, "");
        TypeEntity aClassEnt = resolver.resolvePackageOrClass("A", null).resolveAsType();
        GenTypeClass aClass = aClassEnt.getType().asClass();
        Map<String,Set<MethodReflective>> methods = aClass.getReflective().getDeclaredMethods();
        Set<MethodReflective> mset = methods.get("someMethod");
        assertEquals(1, mset.size());
        
        MethodReflective method = mset.iterator().next();
        assertEquals("java.lang.String", method.getReturnType().toString(false));
    }

    /**
     * Test that a broken method call doesn't interfere with containing method position/size
     */
    @Test
    public void test3()
    {
        String aClassSrc = "class A {\n" +   // position 0
        "  public void someMethod() {\n" +   // position 10
        "    methodCall(\n" +                // position 39 
        "  }\n" +                            // position 55 
        "}\n";

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        NodeAndPosition<ParsedNode> classNP = aNode.findNodeAtOrAfter(0, 0);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNP.getNode().getNodeType());
        assertEquals(0, classNP.getPosition());
        
        NodeAndPosition<ParsedNode> innerNP = classNP.getNode().findNodeAtOrAfter(9, 0);
        
        NodeAndPosition<ParsedNode> methodNP = innerNP.getNode().findNodeAtOrAfter(
                innerNP.getPosition(), innerNP.getPosition());
        
        assertEquals(12, methodNP.getPosition());
        assertEquals(58, methodNP.getPosition() + methodNP.getSize());
    }
    
    /**
     * Test parsing a broken source doesn't break the parser...
     */
    @Test
    public void testBroken()
    {
        String sourceCode = ""
            + "class A\n"       // position 0
            + "{\n"             // position 8
            + "  A() {\n"
            + "    int\n"
            + "  }"
            + "}\n";
            
        ParsedCUNode pcuNode = cuForSource(sourceCode, "");
        assertNotNull(pcuNode);
    }

    @Test
    public void testSuperclass()
    {
        String sourceCode = ""
            + "class A { }\n";
        ParsedCUNode aNode = cuForSource(sourceCode, "");
        PackageOrClass apoc = aNode.resolvePackageOrClass("A", null);
        assertNotNull(apoc);
        TypeEntity aTyent = apoc.resolveAsType();
        assertNotNull(aTyent);
        GenTypeClass aClass = aTyent.getType().asClass();
        assertNotNull(aClass);
        List<GenTypeClass> supers = aClass.getReflective().getSuperTypes();
        assertEquals(1, supers.size());
        assertEquals("java.lang.Object", supers.get(0).toString());
        resolver.addCompilationUnit("", aNode);
        
        sourceCode = "class B extends A {}\n";
        ParsedCUNode bNode = cuForSource(sourceCode, "");
        PackageOrClass bpoc = bNode.resolvePackageOrClass("B", null);
        assertNotNull(bpoc);
        TypeEntity bTyent = bpoc.resolveAsType();
        assertNotNull(bTyent);
        GenTypeClass bClass = bTyent.getType().asClass();
        assertNotNull(bClass);
        supers = bClass.getReflective().getSuperTypes();
        assertEquals(1, supers.size());
        assertEquals("A", supers.get(0).toString());
    }

    @Test
    public void testImport()
    {
        String abcSrc = "package xyz; public class abc { public static class def { }}";
        ParsedCUNode abcNode = cuForSource(abcSrc, "xyz");
        resolver.addCompilationUnit("xyz", abcNode);
        
        String defSrc = "package abc; public class def { }";
        ParsedCUNode defNode = cuForSource(defSrc, "abc");
        resolver.addCompilationUnit("abc", defNode);
        
        String tSrc = "package xyz; import abc.def; class T { public static def field; }";
        ParsedCUNode tNode = cuForSource(tSrc, "xyz");
        resolver.addCompilationUnit("xyz", tNode);
        
        TypeEntity tent = resolver.resolveQualifiedClass("xyz.T");
        assertNotNull(tent);
        
        JavaEntity fEnt = tent.getSubentity("field", null);
        assertNotNull(fEnt);
        JavaEntity fVal = fEnt.resolveAsValue();
        assertNotNull(fVal);
        assertEquals("abc.def", fVal.getType().toString());
    }

    @Test
    public void testSLComment()
    {
        String sourceCode = ""
            + "class A\n"       // 0 - 8
            + "{\n"             // 8 - 10
            + "  A() {\n"       // 10 -  18 
            + "    int a; // comment\n"  // comment starts at 11 + 18 = 29 
            + "  }"
            + "}\n";
            
        ParsedCUNode pcuNode = cuForSource(sourceCode, "");
        assertNotNull(pcuNode);
        
        NodeAndPosition<ParsedNode> top = new NodeAndPosition<ParsedNode>(pcuNode, 0, pcuNode.getSize());
        NodeAndPosition<ParsedNode> nap = top.getNode().findNodeAt(29, top.getPosition());
        while (nap != null) {
            top = nap;
            nap = top.getNode().findNodeAt(29, top.getPosition());
        }
        
        assertEquals(29, top.getPosition());
        assertEquals(39, top.getEnd());
    }

    @Test
    public void testRecursiveTypedef()
    {
        String sourceCode = "interface Sort<T extends Comparable<T>> { }\n";

        ParsedCUNode pcuNode = cuForSource(sourceCode, "");
        assertNotNull(pcuNode);
        
        // ParsedNode sortNode = pcuNode.getTypeNode("Sort");
        PackageOrClass poc = pcuNode.resolvePackageOrClass("Sort", null);
        assertNotNull(poc);
        TypeEntity tent = poc.resolveAsType();
        assertNotNull(tent);
        
        InfoParser.parse(new StringReader(sourceCode), resolver, "");
    }

    @Test
    public void testClassModifiers()
    {
        String sourceCode = ""
            + "interface A\n"
            + "{\n"
            + "}\n";
            
        ParsedCUNode pcuNode = cuForSource(sourceCode, "");
        resolver.addCompilationUnit("", pcuNode);
        
        TypeEntity aTent = resolver.resolveQualifiedClass("A");
        GenTypeClass aClass = aTent.getClassType();
        
        assertFalse(aClass.getReflective().isPublic());
        assertFalse(aClass.getReflective().isStatic());
        assertTrue(aClass.isInterface());
        
        sourceCode = ""
            + "public static class B\n"
            + "{\n"
            + "}\n";
        
        pcuNode = cuForSource(sourceCode, "");
        this.resolver.addCompilationUnit("", pcuNode);

        TypeEntity bTent = resolver.resolveQualifiedClass("B");
        GenTypeClass bClass = bTent.getClassType();
        
        assertTrue(bClass.getReflective().isPublic());
        assertTrue(bClass.getReflective().isStatic());
        assertFalse(bClass.isInterface());
    }

    @Test
    public void testIfNesting()
    {
        String sourceCode = ""
                + "class A\n"    // 0 - 8 
                + "{\n"             //   8 - 10
                + "  void method() {\n"  // 10 - 28
                + "    if (true)\n"      // 28 - 42
                + "      if (true)\n"       // 42 - 58
                + "        hashCode();\n"   // 58 - 78
                + "      else\n"            // 78 - 89 
                + "        hashCode();\n"   // 89 - 109 
                + "    else\n"              // 109 - 118 
                + "      hashCode();\n"     // 118 - 136
                + "  }\n"                   // 126 - 130 
                + "}\n";                    // 130 - 132
                
        ParsedCUNode pcuNode = cuForSource(sourceCode, "");
        resolver.addCompilationUnit("", pcuNode);
            
        NodeAndPosition<ParsedNode> nap = pcuNode.findNodeAt(0, 0);  // class
        nap = nap.getNode().findNodeAt(9, nap.getPosition());        // class inner
        nap = nap.getNode().findNodeAt(12, nap.getPosition());       // method
        nap = nap.getNode().findNodeAt(27, nap.getPosition());       // method inner

        nap = nap.getNode().findNodeAt(32, nap.getPosition());       // outer if
        assertEquals(32, nap.getPosition());
        assertEquals(135, nap.getEnd());
        
        nap = nap.getNode().findNodeAt(48, nap.getPosition());       // inner if
        assertEquals(48, nap.getPosition());
        assertEquals(108, nap.getEnd());
    }

    @Test
    public void testTicket467()
    {
        String sourceCode = ""
                + "class A\n"    // 0 - 8 
                + "{\n"             //   8 - 10
                + "  void method() {\n"  // 10 - 28
                + "    while (true)\n"      // 28 - 45
                + "      if (true) {\n"     // 45 - 63
                + "      }\n"               // 63 - 71
                + "  }\n"                   // 71 - 75 
                + "}\n";                    // 75 - 77
                
        ParsedCUNode pcuNode = cuForSource(sourceCode, "");
        resolver.addCompilationUnit("", pcuNode);
            
        NodeAndPosition<ParsedNode> nap = pcuNode.findNodeAt(0, 0);  // class
        nap = nap.getNode().findNodeAt(9, nap.getPosition());        // class inner
        nap = nap.getNode().findNodeAt(12, nap.getPosition());       // method
        nap = nap.getNode().findNodeAt(27, nap.getPosition());       // method inner

        nap = nap.getNode().findNodeAt(32, nap.getPosition());       // outer while
        assertEquals(32, nap.getPosition());
        assertEquals(70, nap.getEnd());
        
        nap = nap.getNode().findNodeAt(51, nap.getPosition());       // inner if
        assertEquals(51, nap.getPosition());
        assertEquals(70, nap.getEnd());
    }

    @Test
    public void testTryCatch()
    {
        String sourceCode = ""
                + "class A\n"    // 0 - 8 
                + "{\n"             //   8 - 10
                + "  void method() {\n"  // 10 - 28
                + "    try {\n"          // 28 - 38 
                + "    }\n"              // 38 - 44 
                + "    catch(E e) {  }\n"  // 44 - 64 
                + "  }\n"                  // 64 - 68
                + "}\n";                   // 68 - 70
                
        ParsedCUNode pcuNode = cuForSource(sourceCode, "");
        resolver.addCompilationUnit("", pcuNode);
            
        NodeAndPosition<ParsedNode> nap = pcuNode.findNodeAt(0, 0);  // class
        nap = nap.getNode().findNodeAt(9, nap.getPosition());        // class inner
        nap = nap.getNode().findNodeAt(12, nap.getPosition());       // method
        nap = nap.getNode().findNodeAt(27, nap.getPosition());       // method inner

        NodeAndPosition<ParsedNode> tryCatch = nap.getNode().findNodeAt(32, nap.getPosition());       // try-catch
        assertEquals(32, tryCatch.getPosition());
        assertEquals(63, tryCatch.getEnd());
        
        nap = tryCatch.getNode().findNodeAt(37, tryCatch.getPosition());       // try inner
        assertEquals(37, nap.getPosition());
        assertEquals(42, nap.getEnd());
        
        nap = tryCatch.getNode().findNodeAt(60, tryCatch.getPosition());       // catch inner
        assertEquals(60, nap.getPosition());
        assertEquals(62, nap.getEnd());
    }

    @Test
    public void testWhile()
    {
        String sourceCode = ""
                + "class A\n"    // 0 - 8 
                + "{\n"             //   8 - 10
                + "  void method() {\n"  // 10 - 28
                + "    while(true) {\n"  // 28 - 46
                + "      // nothing\n"   // 46 - 63 
                + "    }\n"              // 63 - 69 
                + "  }\n"                //
                + "}\n";                 //
                
        ParsedCUNode pcuNode = cuForSource(sourceCode, "");
        resolver.addCompilationUnit("", pcuNode);
            
        NodeAndPosition<ParsedNode> nap = pcuNode.findNodeAt(0, 0);  // class
        nap = nap.getNode().findNodeAt(9, nap.getPosition());        // class inner
        nap = nap.getNode().findNodeAt(12, nap.getPosition());       // method
        nap = nap.getNode().findNodeAt(27, nap.getPosition());       // method inner

        nap = nap.getNode().findNodeAt(32, nap.getPosition());     // while outer
        assertEquals(32, nap.getPosition());
        assertEquals(68, nap.getEnd());
        
        nap = nap.getNode().findNodeAt(45, nap.getPosition());     // while inner
        assertEquals(45, nap.getPosition());
        assertEquals(67, nap.getEnd());
    }

    @Test
    public void testLambda()
    {
        String sourceCode = ""
                + "class A\n" // 0 - 8 
                + "{\n" //   8 - 10
                + "  void method() {\n" // 10 - 28
                + "    Arrays.asList(\"A\", \"B\", \"C\").stream().map(s ->{/n" // 28 - 80
                + "      return s.toLowerCase();\n" // 80 - 110
                + "      }).collect(Collectors.joining(\",\"));\n" // 110 - 153
                + "  }\n" // 153 - 157 
                + "}\n";  // 157 - 159

        ParsedCUNode pcuNode = cuForSource(sourceCode, "");
        resolver.addCompilationUnit("", pcuNode);

        NodeAndPosition<ParsedNode> nap = pcuNode.findNodeAt(0, 0);  // class
        nap = nap.getNode().findNodeAt(9, nap.getPosition());        // class inner
        nap = nap.getNode().findNodeAt(12, nap.getPosition());       // method
        nap = nap.getNode().findNodeAt(27, nap.getPosition());       // method inner
        nap = nap.getNode().findNodeAt(32, nap.getPosition());       // Arrays
        nap = nap.getNode().findNodeAt(74, nap.getPosition());       // map(
        nap = nap.getNode().findNodeAt(78, nap.getPosition());       // Lambda
        nap = nap.getNode().findNodeAt(79, nap.getPosition());       // outer Lambda
        assertEquals(79, nap.getPosition());                         //Lambda open bracket
        assertEquals(117, nap.getEnd());                             //Lambda close bracket

    }
}
