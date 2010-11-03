/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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
import java.util.Map;

import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import junit.framework.TestCase;
import bluej.debugger.gentype.FieldReflective;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageResolver;
import bluej.parser.entity.TypeEntity;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedCUNode;
import bluej.pkgmgr.JavadocResolver;

public class CompletionTest extends TestCase
{
    {
        InitConfig.init();
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
    private ParsedCUNode cuForSource(String sourceCode, String pkg)
    {
        EntityResolver presolver = new PackageResolver(resolver, pkg);
        MoeSyntaxDocument document = new MoeSyntaxDocument(presolver);
        document.enableParser(true);
        try {
            document.insertString(0, sourceCode, null);
        }
        catch (BadLocationException ble) {}
        return document.getParser();
    }
    
    /**
     * Basic field access test.
     */
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
    
    public void testCompletionAfterArrayElement() throws Exception
    {
        String aClassSrc = "class A {\n" +         // 0 - 10
        "  public static String s[] = null;\n" +   // 10 - 45 
        "  void m() {\n" +                         // 45 - 58 
        "    s[0].length();\n" +                   // 58 -   s[0]. <- 67 
        "  }\n" +
        "}\n";

        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(67, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.String", suggests.getSuggestionType().toString());
    }
    
    /**
     * Test multiple field declarations in one statement.
     */
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
    public void test3() throws Exception
    {
        String aClassSrc = "class A {\n" +   //       10 
        "void someMethod() {\n" +            // +20 = 30 
        "    Object b = new Object();\n" +   // +29 = 59 
        "    int a = b.hashCode();\n" +      // int a = b. <-- 73
        "}\n" +
        "}\n";
        
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(73, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.Object", suggests.getSuggestionType().toString());
    }
    
    /** Test that a for-loop initializer creates a recognized variable */
    public void testForInitializer() throws Exception
    {
        String aClassSrc = "class A {\n" +   //       10 
        "void someMethod() {\n" +            // +20 = 30 
        "    for (Object o = null ; ; ) {\n" + // +33 = 63
        "        o.wait();\n" +              // o. <-- 73
        "    }" +
        "}\n" +
        "}\n";
        
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(73, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.Object", suggests.getSuggestionType().toString());
    }
    
    /**
     * Test a variable reference.
     */
    public void testVariableRef() throws Exception
    {
        String aClassSrc = "class A {\n" +   //       10 
        "void someMethod() {\n" +            // +20 = 30 
        "    String s = \"hello\";\n" +      // +24 = 54
        "    s.length();" +                  // s. <--  60 
        "    }" +
        "}\n" +
        "}\n";
        
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(60, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.String", suggests.getSuggestionType().toString());
    }
    
    /**
     * Check that forward variable references aren't allowed
     */
    public void testNoBwardVarRef() throws Exception
    {
        String aClassSrc = "class A {\n" +   //       10 
        "void someMethod() {\n" +            // +20 = 30 
        "    int a = b.hashCode();\n" +      //    int a = b. <-- 44
        "    Object b = new Object();\n" + 
        "}\n" +
        "}\n";
        
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(44, doc);
        assertNull(suggests);
    }
    
    /**
     * Test an expression referencing the containing class, and make sure it resolves
     * correctly.
     */
    public void testSelfRef() throws Exception
    {
        String aClassSrc = "package abc;\n" + //       13  
        "class A {\n" +                       // +10 = 23 
        "void someMethod() {\n" +             // +20 = 43 
        "    new A().hashCode();\n" +         //    new A(). <-- 55
        "}\n" +
        "}\n";

        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("abc", aNode);

        CodeSuggestions suggests = aNode.getExpressionType(44, doc);
        assertNotNull(suggests);
        assertEquals("abc.A", suggests.getSuggestionType().toString());
    }
        
    /**
     * Completion from an expression involving inner classes accessing variables 
     * within the inner class
     */
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
        
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(89, doc);
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
        
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(89, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.Integer", suggests.getSuggestionType().toString());

        GenTypeClass accessType = suggests.getAccessType();
        assertNotNull(accessType);
        assertEquals("A$B", accessType.getReflective().getName());
    }
    
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
        
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(93, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.String", suggests.getSuggestionType().toString());

        GenTypeClass accessType = suggests.getAccessType();
        assertNotNull(accessType);
        Reflective outer = accessType.getReflective().getOuterClass();
        assertNotNull(outer);
        assertEquals("A", outer.getName());
    }
    
    public void testPartial() throws Exception
    {
        String aClassSrc = "class A {\n" +   // 0 - 10
            "String s = \"\";\n" +           // 10 - 25 
            "public void m() {\n" +          // 25 - 43 
            "  s.c\n" +                      // 43 - 48  s.c_
            "abcd()\n" +
            "}}";
        
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(48, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.String", suggests.getSuggestionType().toString());
        assertNotNull(suggests.getSuggestionToken());
        assertEquals("c", suggests.getSuggestionToken().getText());
    }
    
    /**
     * This is a regression test. Attempting code completion just after a semicolon
     * could cause an exception.
     */
    public void testAfterStatement() throws Exception
    {
        String aClassSrc = "class A {\n" +   // 0 - 10
            "public void m() {\n" +          // 10 - 28  
            "  this(one,two,three);\n" +     // 28 - 51 
            "}\n" +                          // 51 - 53
            "}\n";                           // 53 - 55
    
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(49, doc);
        assertNotNull(suggests);
        assertEquals("A", suggests.getSuggestionType().toString());
    }

    public void testThisDot() throws Exception
    {
        String aClassSrc = "class A {\n" +   // 0 - 10
            "public void m() {\n" +          // 10 - 28  
            "  this.\n" +                    // 28 - 36  this. <-- 35  
            "}\n" +
            "}\n";
    
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(35, doc);
        assertNotNull(suggests);
        assertEquals("A", suggests.getSuggestionType().toString());
        assertFalse(suggests.isStatic());
    }

    public void testTparCompletion() throws Exception
    {
        String aClassSrc = "class A<T extends String & Runnable> {\n" +   // 0 - 39
        "public void m(T t) {\n" +              // 39 - 60   
        "  (t+4).\n" +                          // 60 -   (t+4). <- 68   
        "}\n" +
        "}\n";

        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);

        CodeSuggestions suggests = aNode.getExpressionType(68, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.String", suggests.getSuggestionType().toString());
        assertFalse(suggests.isStatic());
    }
    
    public void testCompletionOnKeyword1() throws Exception
    {
        String aClassSrc = "class A {\n" +   // 0 - 10
            "public void m() {\n" +          // 10 - 28  
            "  this.for\n" +                 // 28 - 39  this.for <-- 38  
            "}\n" +
            "}\n";
    
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(38, doc);
        assertNotNull(suggests);
        assertEquals("A", suggests.getSuggestionType().toString());
        assertFalse(suggests.isStatic());
        LocatableToken stoken = suggests.getSuggestionToken();
        assertNotNull(stoken);
        assertEquals("for", stoken.getText());
    }
    
    public void testCompletionOnKeyword2() throws Exception
    {
        String aClassSrc = "class A {\n" +   // 0 - 10
            "public void m() {\n" +          // 10 - 28  
            "  this.new\n" +                 // 28 - 39  this.new <-- 38  
            "}\n" +
            "}\n";
    
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(38, doc);
        assertNotNull(suggests);
        assertEquals("A", suggests.getSuggestionType().toString());
        assertFalse(suggests.isStatic());
        LocatableToken stoken = suggests.getSuggestionToken();
        assertNotNull(stoken);
        assertEquals("new", stoken.getText());
    }
    
    public void testCompletionOnKeyword3() throws Exception
    {
        String aClassSrc = "class A {\n" +   // 0 - 10
            "public void m() {\n" +          // 10 - 28  
            "  new\n" +                      // 28 - 34  new <-- 33  
            "}\n" +
            "}\n";
    
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(33, doc);
        assertNotNull(suggests);
        assertEquals("A", suggests.getSuggestionType().toString());
        assertFalse(suggests.isStatic());
        LocatableToken stoken = suggests.getSuggestionToken();
        assertNotNull(stoken);
        assertEquals("new", stoken.getText());
    }

    public void testCompletionOnKeyword4() throws Exception
    {
        String aClassSrc = "class A {\n" +   // 0 - 10
            "public void m() {\n" +          // 10 - 28  
            "  for\n" +                      // 28 - 34  new <-- 33  
            "}\n" +
            "}\n";
    
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);

        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(33, doc);
        assertNotNull(suggests);
        assertEquals("A", suggests.getSuggestionType().toString());
        assertFalse(suggests.isStatic());
        LocatableToken stoken = suggests.getSuggestionToken();
        assertNotNull(stoken);
        assertEquals("for", stoken.getText());
    }

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
    
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);

        ParsedCUNode canvasNode = cuForSource(canvasSrc, "");
        resolver.addCompilationUnit("", canvasNode);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(77, doc);
        assertNotNull(suggests);
        assertEquals("Canvas", suggests.getSuggestionType().toString());
        assertFalse(suggests.isStatic());
        LocatableToken stoken = suggests.getSuggestionToken();
        assertNull(stoken);
    }
    
    public void testCompletionInArrayElement() throws Exception
    {
        String aClassSrc =
            "class A {\n" +   // 0 - 10 
            "  public void m() {\n" +  // 10 - 30
            "    short[] array = new short[s\n" +  // 30 - 62
            "  }\n" +
            "}\n";
        
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(61, doc);
        assertNotNull(suggests);
        assertEquals("A", suggests.getSuggestionType().toString());
        assertEquals("s", suggests.getSuggestionToken().getText());
    }
    
    public void testCompletionOnGenericType() throws Exception
    {
        String aClassSrc =
            "class A {\n" +   // 0 - 10 
            "  public void m() {\n" +  // 10 - 30
            "    java.util.List<?> l = new java.util.LinkedList<Object>();\n" + // 30 - 92
            "    l.size();\n" + // 92 -  98  =  l. 
            "  }\n" +
            "}\n";
        
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(98, doc);
        assertNotNull(suggests);
        assertEquals("java.util.List<?>", suggests.getSuggestionType().toString());
        
        AssistContent [] assists = ParseUtils.getPossibleCompletions(suggests, "", new JavadocResolver() {
            public void getJavadoc(MethodReflective method)
            {
                // We want to check that the return type has an erased type.
                assertNotNull(method.getReturnType().getErasedType());
            }
        });
        
        for (AssistContent assist : assists) {
            assist.getJavadoc();
        }
    }
    
    /**
     * Regression test for bug #288
     */
    public void testInterNewCompletion() throws Exception
    {
        String aClassSrc =
            "class A {\n" +   // 0 - 10 
            "  public void m() {\n" +  // 10 - 30
            "    callMe(new Runnable() { });\n" +  // 30 -    53 <- Runnable[HERE]()
            "  }\n" +
            "}\n";
        
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(53, doc);
        assertNotNull(suggests);
    }
    
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
        
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(56, doc);
        assertNotNull(suggests);
    }
    
    public void testRegression312() throws Exception
    {
        String aClassSrc =
            "class A extends javax.swing.JFrame {\n" +   // 0 - 37
            "  public void g() {\n" +   // 37 - 57 
            "  }\n" +                   
            "}\n";
        
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(57, doc);
        assertNotNull(suggests);
        
        AssistContent[] acontent = ParseUtils.getPossibleCompletions(suggests, "", new JavadocResolver() {
            @Override
            public void getJavadoc(MethodReflective method)
            {
            }
        });
        
        assertNotNull(acontent);
    }
    
    // Yet to do:
    
    // Test that multiple fields defined in a single statement are handled correctly,
    // particularly if one in the middle is assigned a complex expression involving an
    // anonymous inner class
    
    // Test that forward references behave the same way as in Java
    // - field definitions may not forward reference other fields in the same class
    //   (although the declarations are visible!)
    // - variables cannot be forward referenced (declarations are not visible).

}
