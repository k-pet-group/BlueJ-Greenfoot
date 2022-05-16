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
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.PackageResolver;
import bluej.parser.lexer.JavaLexer;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.ParsedCUNode;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test for code completion, especially around lambdas and
 * features from Java 11-17 inclusive.
 */
public class CompletionTest2
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
     * A record with the output of the parse method, below.
     * Gives the document, the parser node, and a map from identifier to integer positions within the string.
     */
    private record Parsed(TestableDocument doc, ParsedCUNode node, Map<String, Integer> positions)
    {
        public int position(String key)
        {
            Integer n = positions.get(key);
            if (n == null)
                throw new IllegalStateException("No such key: \"" + key + "\"");
            else
                return n;
        }
    }
    
    /**
     * Parses the given Java source code.  Gives back the document, the parser node
     * and a map from identifier to position.  For the map, all / *... * / (I can't put the
     * actual slash-star here as it will end this comment!) comments in the source code
     * have their position stored.  So if you write / * A * / (without any spaces) you
     * get a map from "A" to the integer position of the leading slash in the source
     * (not A itself; you get the start of the comment).  This is useful for calculating
     * the completions at a particular point in the source.
     */
    private Parsed parse(String src)
    {
        JavaLexer tokens = new JavaLexer(new StringReader(src));
        HashMap<String, Integer> locations = new HashMap<>();
        LocatableToken token;
        do
        {
            token = tokens.nextToken();
            if (token.getType() == JavaTokenTypes.ML_COMMENT)
            {
                locations.put(token.getText().substring(2, token.getLength() - 2).trim(), token.getPosition());
            }
        }
        while(token.getType() != JavaTokenTypes.EOF);
        EntityResolver presolver = new PackageResolver(resolver, "");
        TestableDocument document = new TestableDocument(presolver);
        document.enableParser(true);
        document.insertString(0, src);
        TestableDocument doc = document;
        return new Parsed(doc, doc.getParser(), locations);
    }
    
    @Test
    public void testLocations()
    {
        // Test that our location mechanism is working correctly:
        Parsed p = parse("""
                /*A*/int /**B*/x
                /* C */ = 99;/*D*/
                """);
        // Hand-calculated positions:
        assertEquals(Map.of(
            "A",0,
                "*B", 9,
                "C", 17,
                "D", 30
        ), p.positions());
    }
    
    /**
     * Asserts that the type at position "A" in the source, i.e. where
     * / * A * / (without spaces) occurs in the source is the given one.
     * @param expectedTypeName The expected fully-qualified type name, or null if you expect the type to be unavailable
     * @param javaSrc The Java source code                        
     */
    private void assertTypeAtA(String expectedTypeName, String javaSrc)
    {
        Parsed p = parse(javaSrc);
        resolver.addCompilationUnit("", p.node());

        ExpressionTypeInfo suggests = p.node().getExpressionType(p.position("A"), p.doc());
        if (expectedTypeName == null)
        {
            assertNull(suggests);
        }
        else
        {
            assertNotNull(suggests);
            assertEquals(expectedTypeName, suggests.getSuggestionType().toString());
        }
    }
    
    /**
     * Wraps the given Java in a class with some lambda utility functions
     */
    private String withLambdaDefs(String middle)
    {
        return """
               import java.util.function.*;
               public class Foo
               {
                   private static void withInteger(Consumer<Integer> c) {}
               """ + middle + """
               }
               """;
    }

    @Test
    public void testCastLiteral()
    {
        assertTypeAtA("java.lang.Integer", withLambdaDefs( 
    "{((Integer)1)./*A*/toString();}"
        ));
    }
    
    @Test
    public void testLambda()
    {
        // We only support auto complete when type is specified explicitly:
        
        // Inferred type:
        assertTypeAtA(null, withLambdaDefs(
                "{withInteger(x -> x./*A*/toString());}"
        ));
        // Inferred type with var:
        assertTypeAtA(null, withLambdaDefs(
                "{withInteger((var x) -> x./*A*/toString());}"
        ));
        // Explicit type:
        assertTypeAtA("java.lang.Integer", withLambdaDefs(
                "{withInteger((Integer x) -> x./*A*/toString());}"
        ));
        
    }
}
