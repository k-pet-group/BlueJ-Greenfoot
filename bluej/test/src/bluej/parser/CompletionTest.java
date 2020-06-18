/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014,2015,2016,2017,2019,2020  Michael Kolling and John Rosenberg
 
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

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;


import bluej.JavaFXThreadingRule;
import bluej.debugger.gentype.FieldReflective;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageResolver;
import bluej.parser.entity.TypeEntity;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedCUNode;
import bluej.pkgmgr.JavadocResolver;
import bluej.utility.JavaReflective;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class CompletionTest
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
        return documentForSource(sourceCode, pkg).getParser();
    }
    
    /**
     * Get a TestableDocument (with parser enabled) for the given source code.
     */
    private TestableDocument documentForSource(String sourceCode, String pkg)
    {
        EntityResolver presolver = new PackageResolver(resolver, pkg);
        TestableDocument document = new TestableDocument(presolver);
        document.enableParser(true);
        document.insertString(0, sourceCode);
        return document;
    }
    
    /**
     * Basic field access test.
     */
    @Test
    public void testFieldAccess()
    {
        String aClassSrc = "class A {" +
        "  public static int f = 0;" +
        "}";

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        EntityResolver resolver = new PackageResolver(this.resolver, "");
        JavaEntity aClassEnt = resolver.resolvePackageOrClass("A", null);
        assertNotNull(aClassEnt);
        TypeEntity typeEnt = aClassEnt.resolveAsType();
        assertNotNull(typeEnt);
        
        GenTypeClass gtc = typeEnt.getClassType();
        assertNotNull(gtc);
        assertEquals("A", gtc.toString());
        FieldReflective fref = gtc.getReflective().getDeclaredFields().get("f");
        assertNotNull(fref);
        assertEquals("int", fref.getType().toString());
        assertEquals(Modifier.PUBLIC | Modifier.STATIC, fref.getModifiers());
    }
    
    /**
     * Field access - array declarators after field name
     */
    @Test
    public void testArrayFieldAccess()
    {
        String aClassSrc = "class A {" +
        "  public static int f[] = null;" +
        "}";

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        EntityResolver resolver = new PackageResolver(this.resolver, "");
        JavaEntity aClassEnt = resolver.resolvePackageOrClass("A", null);
        
        Map<String,FieldReflective> fields = aClassEnt.resolveAsType().getClassType().getReflective().getDeclaredFields();
        JavaType ftype = fields.get("f").getType();
        assertEquals("int[]", ftype.toString());
    }

    @Test
    public void testCompletionAfterArrayElement() throws Exception
    {
        String aClassSrc = "class A {\n" +         // 0 - 10
        "  public static String s[] = null;\n" +   // 10 - 45 
        "  void m() {\n" +                         // 45 - 58 
        "    s[0].length();\n" +                   // 58 -   s[0]. <- 67 
        "  }\n" +
        "}\n";

        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(67, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.String", suggests.getSuggestionType().toString());
    }
    
    /**
     * Test multiple field declarations in one statement.
     */
    @Test
    public void testMultiFieldAccess()
    {
        String aClassSrc = "class A {" +
        "  public static int f, g[] = null;" +
        "}";

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        EntityResolver resolver = new PackageResolver(this.resolver, "");
        JavaEntity aClassEnt = resolver.resolvePackageOrClass("A", null);
        Reader r = new StringReader("");
        CompletionParser cp = new CompletionParser(resolver, r, aClassEnt);
        cp.parseExpression();

        Map<String,FieldReflective> fields = aClassEnt.resolveAsType().getClassType().getReflective().getDeclaredFields();
        JavaType ftype = fields.get("f").getType();
        assertEquals("int", ftype.toString());
        
        ftype = fields.get("g").getType();
        assertNotNull(ftype);
        assertEquals("int[]", ftype.toString());
    }
    
    /**
     * Access of a static field from another class
     */
    @Test
    public void test2()
    {
        String aClassSrc = "class A {" +
        "  public static int f = 0;" +
        "}";

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);

        String bClassSrc = "class B { }";
        ParsedCUNode bNode = cuForSource(bClassSrc, "");
        resolver.addCompilationUnit("", bNode);

        EntityResolver resolver = new PackageResolver(this.resolver, "");
        JavaEntity bClassEnt = resolver.resolvePackageOrClass("B", null);
        Reader r = new StringReader("A.");
        CompletionParser cp = new CompletionParser(bNode, r, bClassEnt);
        cp.parseExpression();

        Map<String,FieldReflective> fields = cp.getSuggestionType().asClass().getReflective().getDeclaredFields();
        JavaType ftype = fields.get("f").getType();
        assertEquals("int", ftype.toString());
    }
    
    /**
     * Completion from an expression involving a local variable
     */
    @Test
    public void test3() throws Exception
    {
        String aClassSrc = "class A {\n" +   //       10 
        "void someMethod() {\n" +            // +20 = 30 
        "    Object b = new Object();\n" +   // +29 = 59 
        "    int a = b.hashCode();\n" +      // int a = b. <-- 73
        "}\n" +
        "}\n";
        
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(73, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.Object", suggests.getSuggestionType().toString());
    }

    /**
     * Completion from an expression involving a local variable declared as "var"
     */
    @Test
    public void test3var() throws Exception
    {
        String aClassSrc = "class A {\n" +   //       10 
        "void someMethod() {\n" +            // +20 = 30 
        "    var b = new Object();\n" +      // +26 = 56 
        "    int a = b.hashCode();\n" +      // int a = b. <-- 70
        "}\n" +
        "}\n";
        
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(70, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.Object", suggests.getSuggestionType().toString());
    }

    /**
     * Completion from an expression involving a local variable declared as "var"
     */
    @Test
    public void test4var() throws Exception
    {
        String aClassSrc = "class A {\n" +
                "void someMethod() {\n" +
                "    var b = \"hello\";\n" +
                "    int a = b.length();\n" +
                "}\n" +
                "}\n";

        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);

        ExpressionTypeInfo suggests = aNode.getExpressionType(aClassSrc.indexOf("length()"), doc);
        assertNotNull(suggests);
        assertEquals("java.lang.String", suggests.getSuggestionType().toString());
    }

    /**
     * Completion from an expression involving a local variable declared as "var"
     */
    @Test
    public void test5var() throws Exception
    {
        String aClassSrc = "class A {\n" +
                "void someMethod() {\n" +
                "    var b = java.util.List.of(\"hello\");\n" +
                "    int a = b.get(0).length();\n" +
                "}\n" +
                "}\n";

        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);

        ExpressionTypeInfo suggestsList = aNode.getExpressionType(aClassSrc.indexOf("get(0)"), doc);
        assertNotNull(suggestsList);
        assertEquals("java.util.List<java.lang.String>", suggestsList.getSuggestionType().toString());
        ExpressionTypeInfo suggests = aNode.getExpressionType(aClassSrc.indexOf("length()"), doc);
        assertNotNull(suggests);
        assertEquals("java.lang.String", suggests.getSuggestionType().toString());
    }
    
    /** Test that a for-loop initializer creates a recognized variable */
    @Test
    public void testForInitializer() throws Exception
    {
        String aClassSrc = "class A {\n" +   //       10 
        "void someMethod() {\n" +            // +20 = 30 
        "    for (Object o = null ; ; ) {\n" + // +33 = 63
        "        o.wait();\n" +              // o. <-- 73
        "    }" +
        "}\n" +
        "}\n";
        
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(73, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.Object", suggests.getSuggestionType().toString());
    }

    /**
     * Test that a for-loop initializer, declared with "var", creates a recognised variable
     */
    @Test
    public void testForInitializerVar() throws Exception
    {
        String aClassSrc = "class A {\n" +   //       10 
        "void someMethod() {\n" +            // +20 = 30 
        "    for (var o = new Object() ; ; ) {\n" +  // +38 = 68
        "        o.wait();\n" +              // o. <-- 78
        "    }" +
        "}\n" +
        "}\n";
        
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(78, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.Object", suggests.getSuggestionType().toString());
    }
    
    /**
     * Test a variable reference.
     */
    @Test
    public void testVariableRef() throws Exception
    {
        String aClassSrc = "class A {\n" +   //       10 
        "void someMethod() {\n" +            // +20 = 30 
        "    String s = \"hello\";\n" +      // +24 = 54
        "    s.length();" +                  // s. <--  60 
        "    }" +
        "}\n" +
        "}\n";
        
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(60, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.String", suggests.getSuggestionType().toString());
    }
    
    /**
     * Check that forward variable references aren't allowed
     */
    @Test
    public void testNoBwardVarRef() throws Exception
    {
        String aClassSrc = "class A {\n" +   //       10 
        "void someMethod() {\n" +            // +20 = 30 
        "    int a = b.hashCode();\n" +      //    int a = b. <-- 44
        "    Object b = new Object();\n" + 
        "}\n" +
        "}\n";
        
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(44, doc);
        assertNull(suggests);
    }
    
    /**
     * Test an expression referencing the containing class, and make sure it resolves
     * correctly.
     */
    @Test
    public void testSelfRef() throws Exception
    {
        String aClassSrc = "package abc;\n" + //       13  
        "class A {\n" +                       // +10 = 23 
        "void someMethod() {\n" +             // +20 = 43 
        "    new A().hashCode();\n" +         //    new A(). <-- 55
        "}\n" +
        "}\n";

        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("abc", aNode);

        ExpressionTypeInfo suggests = aNode.getExpressionType(44, doc);
        assertNotNull(suggests);
        assertEquals("abc.A", suggests.getSuggestionType().toString());
    }
        
    /**
     * Completion from an expression involving inner classes accessing variables 
     * within the inner class
     */
    @Test
    public void testInnerClasses() throws Exception
    {
        String aClassSrc = "class A {\n" +                  //10   
        "String test=\"fg\";\n" +                           //+18=28
        "class B {\n" +                                     //+10=38
        "Integer temp=new Integer(\"1\");\n" +              //+31=69
        "void bluej() {\n" +                                //+15=84
        "temp.hashCode();\n" +                              //+5=89  ---> temp.            
        "}\n" +
        "}\n" +
        "}\n";
        
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(89, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.Integer", suggests.getSuggestionType().toString());
        
        GenTypeClass accessType = suggests.getAccessType();
        assertNotNull(accessType);
        assertEquals("A$B", accessType.getReflective().getName());
    }
    
    /**
     * Completion from an expression involving inner classes accessing variables 
     * within the outer class
     */
    @Test
    public void testInnerClasses2() throws Exception
    {
        String aClassSrc = "class A {\n" +                  //10   
        "Integer test=new Integer(\"1\");\n" +              //+31=41
        "class B {\n" +                                     //+10=51 
        "String temp=\"fg\";\n" +                           //+18=69 
        "void bluet() {\n" +                                //+15=84 
        "test.hashCode();\n" +                              //+5=89  ---> test.            
        "}\n" +
        "}\n" +
        "}\n";
        
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(89, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.Integer", suggests.getSuggestionType().toString());

        GenTypeClass accessType = suggests.getAccessType();
        assertNotNull(accessType);
        assertEquals("A$B", accessType.getReflective().getName());
    }

    @Test
    public void testInnerClasses3() throws Exception
    {
        String aClassSrc =
            "class A {\n" +                         // 0-10
            "  Runnable r = new Runnable() {\n" +   // 10-42
            "    String x = \"\";\n" +              // 42-61
            "    public void run() {\n" +           // 61-85
            "      x.length();\n" +                 //  x. <-- 93
            "    }\n" +
            "  };\n" +
            "}\n";
        
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(93, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.String", suggests.getSuggestionType().toString());

        GenTypeClass accessType = suggests.getAccessType();
        assertNotNull(accessType);
        Reflective outer = accessType.getReflective().getOuterClass();
        assertNotNull(outer);
        assertEquals("A", outer.getName());
    }

    @Test
    public void testInnerClasses4() throws Exception
    {
        String aClassSrc =
            "class A {\n" +                         // 0-10
            "  Runnable r = new Thread(new String(\"xxxx\")) {\n" +   // 10-58
            "    String x = \"\";\n" +              // 58-77
            "    public void run() {\n" +           // 77-101
            "      this.run();\n" +                 //  this. <-- 112
            "    }\n" +
            "  };\n" +
            "}\n";
        
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(109, doc);
        assertNotNull(suggests);
        GenTypeClass suggestsType = suggests.getSuggestionType().asClass();
        assertNotNull(suggestsType);
        List<GenTypeClass> supers = suggestsType.getReflective().getSuperTypes();
        assertEquals(1, supers.size());
        assertEquals("java.lang.Thread", supers.get(0).toString());
    }

    @Test
    public void testPartial() throws Exception
    {
        String aClassSrc = "class A {\n" +   // 0 - 10
            "String s = \"\";\n" +           // 10 - 25 
            "public void m() {\n" +          // 25 - 43 
            "  s.c\n" +                      // 43 - 48  s.c_
            "abcd()\n" +
            "}}";
        
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(48, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.String", suggests.getSuggestionType().toString());
        assertNotNull(suggests.getSuggestionToken());
        assertEquals("c", suggests.getSuggestionToken().getText());
    }
    
    /**
     * This is a regression test. Attempting code completion just after a semicolon
     * could cause an exception.
     */
    @Test
    public void testAfterStatement() throws Exception
    {
        String aClassSrc = "class A {\n" +   // 0 - 10
            "public void m() {\n" +          // 10 - 28  
            "  this(one,two,three);\n" +     // 28 - 51 
            "}\n" +                          // 51 - 53
            "}\n";                           // 53 - 55
    
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(49, doc);
        assertNotNull(suggests);
        assertEquals("A", suggests.getSuggestionType().toString());
    }

    @Test
    public void testThisDot() throws Exception
    {
        String aClassSrc = "class A {\n" +   // 0 - 10
            "public void m() {\n" +          // 10 - 28  
            "  this.\n" +                    // 28 - 36  this. <-- 35  
            "}\n" +
            "}\n";
    
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(35, doc);
        assertNotNull(suggests);
        assertEquals("A", suggests.getSuggestionType().toString());
        assertFalse(suggests.isStatic());
    }

    @Test
    public void testTparCompletion() throws Exception
    {
        String aClassSrc = "class A<T extends String & Runnable> {\n" +   // 0 - 39
        "public void m(T t) {\n" +              // 39 - 60   
        "  (t+4).\n" +                          // 60 -   (t+4). <- 68   
        "}\n" +
        "}\n";

        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);

        ExpressionTypeInfo suggests = aNode.getExpressionType(68, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.String", suggests.getSuggestionType().toString());
        assertFalse(suggests.isStatic());
    }

    @Test
    public void testTparCompletion2() throws Exception
    {
        String aClassSrc = "class A<T extends String & Runnable> {\n" +   // 0 - 39
        "public void m(T t) {\n" +              // 39 - 60   
        "  (t+4).\n" +                          // 60 -   (t+4). <- 68   
        "}\n" +
        "}\n";

        TestableDocument doc = documentForSource(aClassSrc, "");
        ParsedCUNode aNode = doc.getParser();
        resolver.addCompilationUnit("", aNode);
        
        // Now rename the "T" tpar to "U"
        doc.remove(8, 1);
        doc.insertString(8, "U");
        doc.getParser();
        doc.remove(53, 1);
        doc.insertString(53, "U");
        aNode = doc.getParser();
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(68, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.String", suggests.getSuggestionType().toString());
        assertFalse(suggests.isStatic());
    }

    @Test
    public void testTparCompletion3() throws Exception
    {
        String aClassSrc = "class A<T extends String & Runnable> {\n" +   // 0 - 39
        "public void m(T t) {\n" +              // 39 - 60   
        "  (t+4).\n" +                          // 60 -   (t+4). <- 68   
        "}\n" +
        "}\n";

        TestableDocument doc = documentForSource(aClassSrc, "");
        ParsedCUNode aNode = doc.getParser();
        resolver.addCompilationUnit("", aNode);
        
        // Now rename the "T" tpar to "U" (in the method first)
        doc.remove(53, 1);
        doc.insertString(53, "U");
        doc.getParser();
        doc.remove(8, 1);
        doc.insertString(8, "U");
        aNode = doc.getParser();
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(68, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.String", suggests.getSuggestionType().toString());
        assertFalse(suggests.isStatic());
    }

    @Test
    public void testTparCompletion4() throws Exception
    {
        String aClassSrc = "class A<T extends String, U extends T> {\n" +   // 0 - 41
        "public void m(U u) {\n" +              // 41 - 62   
        "  u.\n" +                              // 62 -   u. <- 66   
        "}\n" +
        "}\n";

        TestableDocument doc = documentForSource(aClassSrc, "");
        ParsedCUNode aNode = doc.getParser();
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(66, doc);
        assertNotNull(suggests);
        GenTypeSolid stsolid = suggests.getSuggestionType().asSolid();
        assertNotNull(stsolid);
        GenTypeClass [] stypes = stsolid.getReferenceSupertypes();
        assertEquals(1, stypes.length);
        assertEquals("java.lang.String", stypes[0].toString());
        assertFalse(suggests.isStatic());
    }

    @Test
    public void testTparCompletion5() throws Exception
    {
        String aClassSrc = "class A {\n" +      // 0 - 10
        "public <T extends String, U extends T> void m(U u) {\n" +  // 10 - 63
        "  u.\n" +                              // 63 -   u. <- 67   
        "}\n" +
        "}\n";

        TestableDocument doc = documentForSource(aClassSrc, "");
        ParsedCUNode aNode = doc.getParser();
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(67, doc);
        assertNotNull(suggests);
        GenTypeSolid stsolid = suggests.getSuggestionType().asSolid();
        assertNotNull(stsolid);
        GenTypeClass [] stypes = stsolid.getReferenceSupertypes();
        assertEquals(1, stypes.length);
        assertEquals("java.lang.String", stypes[0].toString());
        assertFalse(suggests.isStatic());
    }

    @Test
    public void testCompletionOnKeyword1() throws Exception
    {
        String aClassSrc = "class A {\n" +   // 0 - 10
            "public void m() {\n" +          // 10 - 28  
            "  this.for\n" +                 // 28 - 39  this.for <-- 38  
            "}\n" +
            "}\n";
    
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(38, doc);
        assertNotNull(suggests);
        assertEquals("A", suggests.getSuggestionType().toString());
        assertFalse(suggests.isStatic());
        LocatableToken stoken = suggests.getSuggestionToken();
        assertNotNull(stoken);
        assertEquals("for", stoken.getText());
    }

    @Test
    public void testCompletionOnKeyword2() throws Exception
    {
        String aClassSrc = "class A {\n" +   // 0 - 10
            "public void m() {\n" +          // 10 - 28  
            "  this.new\n" +                 // 28 - 39  this.new <-- 38  
            "}\n" +
            "}\n";
    
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(38, doc);
        assertNotNull(suggests);
        assertEquals("A", suggests.getSuggestionType().toString());
        assertFalse(suggests.isStatic());
        LocatableToken stoken = suggests.getSuggestionToken();
        assertNotNull(stoken);
        assertEquals("new", stoken.getText());
    }

    @Test
    public void testCompletionOnKeyword3() throws Exception
    {
        String aClassSrc = "class A {\n" +   // 0 - 10
            "public void m() {\n" +          // 10 - 28  
            "  new\n" +                      // 28 - 34  new <-- 33  
            "}\n" +
            "}\n";
    
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(33, doc);
        assertNotNull(suggests);
        assertEquals("A", suggests.getSuggestionType().toString());
        assertFalse(suggests.isStatic());
        LocatableToken stoken = suggests.getSuggestionToken();
        assertNotNull(stoken);
        assertEquals("new", stoken.getText());
    }

    @Test
    public void testCompletionOnKeyword4() throws Exception
    {
        String aClassSrc = "class A {\n" +   // 0 - 10
            "public void m() {\n" +          // 10 - 28  
            "  for\n" +                      // 28 - 34  for <-- 33  
            "}\n" +
            "}\n";
    
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(33, doc);
        assertNotNull(suggests);
        assertEquals("A", suggests.getSuggestionType().toString());
        assertFalse(suggests.isStatic());
        LocatableToken stoken = suggests.getSuggestionToken();
        assertNotNull(stoken);
        assertEquals("for", stoken.getText());
    }

    @Test
    public void testCompletionResolution() throws Exception
    {
        String canvasSrc = "class Canvas { }\n";
        String aClassSrc =
            "import java.awt.*;\n" +    // 0 - 19     
            "class A {\n" +             // 19 - 29 
            "  Canvas canvas;\n" +      // 29 - 46 
            "  public void m() {\n" +   // 46 - 66
            "    canvas.\n" +           //  canvas.  <--  77 
            "  }\n" +
            "}\n";
    
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);

        ParsedCUNode canvasNode = cuForSource(canvasSrc, "");
        resolver.addCompilationUnit("", canvasNode);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(77, doc);
        assertNotNull(suggests);
        assertEquals("Canvas", suggests.getSuggestionType().toString());
        assertFalse(suggests.isStatic());
        LocatableToken stoken = suggests.getSuggestionToken();
        assertNull(stoken);
    }

    @Test
    public void testCompletionResolution2() throws Exception
    {
        String aClassSrc =
            "import static javax.swing.text.DefaultEditorKit.*;\n" +    // 0 - 51     
            "class A {\n" +             // 51 - 61 
            "  BeepAction ba;\n" +      // 61 - 78  
            "  public void m() {\n" +   // 78 - 98
            "    ba.\n" +           //  ba.  <--  105 
            "  }\n" +
            "}\n";
    
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(105, doc);
        assertNotNull(suggests);
        assertEquals("javax.swing.text.DefaultEditorKit.BeepAction", suggests.getSuggestionType().toString());
        assertFalse(suggests.isStatic());
        LocatableToken stoken = suggests.getSuggestionToken();
        assertNull(stoken);
    }

    @Test
    public void testCompletionInArrayElement() throws Exception
    {
        String aClassSrc =
            "class A {\n" +   // 0 - 10 
            "  public void m() {\n" +  // 10 - 30
            "    short[] array = new short[s\n" +  // 30 - 62
            "  }\n" +
            "}\n";
        
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(61, doc);
        assertNotNull(suggests);
        assertEquals("A", suggests.getSuggestionType().toString());
        assertEquals("s", suggests.getSuggestionToken().getText());
    }

    @Test
    public void testCompletionOnGenericType() throws Exception
    {
        String aClassSrc =
            "class A {\n" +   // 0 - 10 
            "  public void m() {\n" +  // 10 - 30
            "    java.util.List<?> l = new java.util.LinkedList<Object>();\n" + // 30 - 92
            "    l.size();\n" + // 92 -  98  =  l. 
            "  }\n" +
            "}\n";
        
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(98, doc);
        assertNotNull(suggests);
        assertEquals("java.util.List<?>", suggests.getSuggestionType().toString());
        
        AssistContent [] assists = ParseUtils.getPossibleCompletions(suggests, new JavadocResolver() {
            public String getJavadoc(String moduleName, String name) { throw new IllegalStateException(); }
            
            public void getJavadoc(Reflective declType, Collection<? extends ConstructorOrMethodReflective> methods)
            {
                // We want to check that the return type has an erased type.
                for (ConstructorOrMethodReflective method : methods) {
                    assertNotNull(((MethodReflective)method).getReturnType().getErasedType());
                }
            }

            @Override
            public boolean getJavadocAsync(ConstructorOrMethodReflective method,
                    AsyncCallback callback, Executor executor)
            {
                throw new RuntimeException("Not implemented in test stub.");
            }
            
        }, null, aNode.getContainingMethodOrClassNode(98));
        
        for (AssistContent assist : assists) {
            assist.getJavadoc();
        }
    }
    
    /**
     * Regression test for bug #288
     */
    @Test
    public void testInterNewCompletion() throws Exception
    {
        String aClassSrc =
            "class A {\n" +   // 0 - 10 
            "  public void m() {\n" +  // 10 - 30
            "    callMe(new Runnable() { });\n" +  // 30 -    53 <- Runnable[HERE]()
            "  }\n" +
            "}\n";
        
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(53, doc);
        assertNotNull(suggests);
    }

    @Test
    public void testPartialExpressionCompletion() throws Exception
    {
        String aClassSrc =
            "class A {\n" +             // 0 - 10
            "  public void g() {\n" +   // 10 - 30
            "    String s = \"\";\n" +  // 30 - 49
            "    s.l\n" +               //   s.l <---  56 
            "    s.length();\n" +
            "  }\n" +
            "}\n";
        
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(56, doc);
        assertNotNull(suggests);
    }

    @Test
    public void testRegression312() throws Exception
    {
        String aClassSrc =
            "class A extends javax.swing.JFrame {\n" +   // 0 - 37
            "  public void g() {\n" +   // 37 - 57 
            "  }\n" +                   
            "}\n";
        
        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(57, doc);
        assertNotNull(suggests);
        
        AssistContent[] acontent = ParseUtils.getPossibleCompletions(suggests, new JavadocResolver() {
            @Override
            public void getJavadoc(Reflective declType, Collection<? extends ConstructorOrMethodReflective> method)
            {
            }

            @Override
            public String getJavadoc(String moduleName, String typeName)
            {
                throw new RuntimeException("Not implemented in test stub.");
            }

            @Override
            public boolean getJavadocAsync(ConstructorOrMethodReflective method,
                    AsyncCallback callback, Executor executor)
            {
                throw new RuntimeException("Not implemented in test stub.");
            }
        }, null, aNode.getContainingMethodOrClassNode(57));
        
        assertNotNull(acontent);
    }

    @Test
    public void testRegression340() throws Exception
    {
        String aClassSrc =
            "class A {\n" +            // 0 - 10
            "  public void g() {\n" +   // 10 - 30
            "    someMethod(new int[] {new String().length, 45});\n" +  //  }.  <-- 80
            "  }\n" +                   
            "}\n";

        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        // In ticket #340 this causes an EmptyStackException:
        ExpressionTypeInfo suggests = aNode.getExpressionType(80, doc);
        assertNotNull(suggests);
    }

    @Test
    public void testCompletionAfterAnonClass() throws Exception
    {
        String aClassSrc =
            "class A {\n" +            // 0 - 10
            "  public void g() {\n" +   // 10 - 30
            "    new Thread() {\n" +    // 30 - 49
            "      public void run() {\n" +  // 49 - 75
            "        int x = 5 + 6;\n" +  // 75 - 98
            "      }\n" +               // 98 - 106
            "    }.start();\n" +        //  }. <-- 112
            "  }\n" +                   
            "}\n";

        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(112, doc);
        assertNotNull(suggests);
        assertTrue(new GenTypeClass(new JavaReflective(Thread.class)).isAssignableFrom(suggests.getSuggestionType()));
    }

    @Test
    public void testAfterArrayInitList() throws Exception
    {
        String aClassSrc =
            "class A {\n" +            // 0 - 10
            "  public void g() {\n" +   // 10 - 30
            "    int l = new String[]{\"one\",\"two\"}.length;\n" +  //  }.  <-- 68
            "  }\n" +                   
            "}\n";

        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(68, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.String[]", suggests.getSuggestionType().toString());
    }

    @Test
    public void test360() throws Exception
    {
        String cSrc = "package tpkg;\n" +
                "class C {\n" +
                "  public static void doNothing() {}\n" +
                "}\n";
        
        ParsedCUNode cNode = documentForSource(cSrc, "tpkg").getParser();
        resolver.addCompilationUnit("tpkg", cNode);
        
        String aSrc = "import tpkgxx.*;\n" +   // 0 - 17
                "class A {\n" +                // 17 - 27
                "  public A() {\n" +           // 27 - 42
                "    class B {\n" +            // 42 - 56
                "      public B() {\n" +       // 56 - 75 
                "        C.\n" +               // 75 -     C. <-- 85
                "      }\n" +
                "  }\n" +
                "}\n";

        
        TestableDocument aDoc = documentForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        ExpressionTypeInfo suggests = aNode.getExpressionType(85, aDoc);
        assertEquals(".", aDoc.getFullText().substring(84, 85));  // check position calculation
        assertNull(suggests);
        
        // Fix import:
        aDoc.remove(11, 2);
        aNode = aDoc.getParser();
        suggests = aNode.getExpressionType(83, aDoc);
        assertNotNull(suggests);
        assertEquals("tpkg.C", suggests.getSuggestionType().toString());
    }

    @Test
    public void testVarargsParam() throws Exception
    {
        String aClassSrc =
                "class A {\n" +            // 0 - 10
                "  public void g(String ... s) {\n" +   // 10 - 42
                "    System.out.print(s.length);\n" +  //  s.  <-- 65
                "  }\n" +                   
                "}\n";

        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
            
        ExpressionTypeInfo suggests = aNode.getExpressionType(66, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.String[]", suggests.getSuggestionType().toString());        
    }

    @Test
    public void testMultiFieldDecWithInner() throws Exception
    {
        String aClassSrc =
                "class A {\n" +            // 0 - 10
                "  Runnable r, b = new Runnable() {\n" +  // 10 - 45
                "    public void run() {\n" +             // 45 - 69
                "      c.run();\n" +                      // 69 - 84   c. <- 77
                "    }\n" +                               // 84 - 90 
                "  }, c = new Thread() {;\n" +            // 90 - 115
                "    public void run() {\n" +             // 115 - 139 
                "       b.run();\n" +                     // 139 - 155  b. <- 148
                "    }\n" +
                "  };\n" +
                "}\n";

        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
            
        ExpressionTypeInfo suggests = aNode.getExpressionType(77, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.Runnable", suggests.getSuggestionType().toString());
        
        suggests = aNode.getExpressionType(148, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.Runnable", suggests.getSuggestionType().toString());
    }

    @Test
    public void testRegression571()
    {
        // Exception when completing on invalid code:
        
        String aClassSrc =
                "static int count = 0;\n";

        TestableDocument doc = new TestableDocument();
        doc.insertString(0, aClassSrc);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
            
        // In BlueJ 3.1.6 this throws a NullPointerException:
        aNode.getExpressionType(13, doc);
    }
}
