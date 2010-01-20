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

import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import junit.framework.TestCase;
import bluej.debugger.gentype.JavaType;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.nodes.ParsedCUNode;

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
    private ParsedCUNode cuForSource(String sourceCode)
    {
        MoeSyntaxDocument document = new MoeSyntaxDocument(resolver);
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

        ParsedCUNode aNode = cuForSource(aClassSrc);
        resolver.addCompilationUnit("", aNode);
        
        JavaEntity aClassEnt = resolver.resolvePackageOrClass("A", "");
        Reader r = new StringReader("");
        CompletionParser cp = new CompletionParser(resolver, r, aClassEnt);
        cp.parseExpression();
        
        Map<String,JavaType> fields = cp.getFieldSuggestions();
        JavaType ftype = fields.get("f");
        assertNotNull(ftype);
        assertEquals("int", ftype.toString());
    }
    
    /**
     * Field access - array declarators after field name
     */
    public void testArrayFieldAccess()
    {
        String aClassSrc = "class A {" +
        "  public static int f[] = null;" +
        "}";

        ParsedCUNode aNode = cuForSource(aClassSrc);
        resolver.addCompilationUnit("", aNode);
        
        JavaEntity aClassEnt = resolver.resolvePackageOrClass("A", "");
        Reader r = new StringReader("");
        CompletionParser cp = new CompletionParser(resolver, r, aClassEnt);
        cp.parseExpression();
        
        Map<String,JavaType> fields = cp.getFieldSuggestions();
        JavaType ftype = fields.get("f");
        assertNotNull(ftype);
        assertEquals("int[]", ftype.toString());
    }
    
    /**
     * Test multiple field declarations in one statement.
     */
    public void testMultiFieldAccess()
    {
        String aClassSrc = "class A {" +
        "  public static int f, g[] = null;" +
        "}";

        ParsedCUNode aNode = cuForSource(aClassSrc);
        resolver.addCompilationUnit("", aNode);
        
        JavaEntity aClassEnt = resolver.resolvePackageOrClass("A", "");
        Reader r = new StringReader("");
        CompletionParser cp = new CompletionParser(resolver, r, aClassEnt);
        cp.parseExpression();
        
        Map<String,JavaType> fields = cp.getFieldSuggestions();
        JavaType ftype = fields.get("f");
        assertNotNull(ftype);
        assertEquals("int", ftype.toString());
        
        ftype = fields.get("g");
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

        ParsedCUNode aNode = cuForSource(aClassSrc);
        resolver.addCompilationUnit("", aNode);

        String bClassSrc = "class B { }";
        ParsedCUNode bNode = cuForSource(bClassSrc);
        resolver.addCompilationUnit("", bNode);

        JavaEntity bClassEnt = resolver.resolvePackageOrClass("B", "");
        Reader r = new StringReader("A.");
        CompletionParser cp = new CompletionParser(bNode, r, bClassEnt);
        cp.parseExpression();
        
        Map<String,JavaType> fields = cp.getFieldSuggestions();
        JavaType ftype = fields.get("f");
        assertNotNull(ftype);
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
        
        ParsedCUNode aNode = cuForSource(aClassSrc);
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
        "        o.wait();\n" +   // o. <-- 73
        "    }" +
        "}\n" +
        "}\n";
        
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);
        
        ParsedCUNode aNode = cuForSource(aClassSrc);
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(73, doc);
        assertNotNull(suggests);
        assertEquals("java.lang.Object", suggests.getSuggestionType().toString());
    }
    
    /**
     * Check that forward variable references aren't allowed
     */
    public void test4() throws Exception
    {
        String aClassSrc = "class A {\n" +   //       10 
        "void someMethod() {\n" +            // +20 = 30 
        "    int a = b.hashCode();\n" +      //    int a = b. <-- 44
        "    Object b = new Object();\n" + 
        "}\n" +
        "}\n";
        
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, aClassSrc, null);
        
        ParsedCUNode aNode = cuForSource(aClassSrc);
        resolver.addCompilationUnit("", aNode);
        
        CodeSuggestions suggests = aNode.getExpressionType(44, doc);
        assertNull(suggests);
    }
    
    // Test that multiple fields defined in a single statement are handled correctly,
    // particularly if one in the middle is assigned a complex expression involving an
    // anonymous inner class
    
    // Test that forward references behave the same way as in Java
    // - field definitions may not forward reference other fields in the same class
    // - variables cannot be forward referenced

}
