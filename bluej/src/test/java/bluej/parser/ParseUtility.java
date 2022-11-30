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

import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.PackageResolver;
import bluej.parser.lexer.JavaLexer;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.NodeTree;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * A class with useful methods for testing the parser.
 */
public class ParseUtility
{
    /** The start and end positions (within a document) of an element */
    public record StartEnd(int start, int end) {}

    /**
     * A record with the output of the parse method, below.
     * Gives the document, the parser node, and a map from identifier to integer positions within the string.
     */
    public record Parsed(TestableDocument doc, ParsedCUNode node, Map<String, StartEnd> positions)
    {
        /**
         * Gets the start position within the source of the given String key
         * (see parse method below for more details)
         */
        public int positionStart(String key)
        {
            StartEnd n = positions.get(key);
            if (n == null)
                throw new IllegalStateException("No such key: \"" + key + "\"");
            else
                return n.start();
        }

        /**
         * Gets the end position within the source of the given String key
         * (see parse method below for more details)
         */
        public int positionEnd(String key)
        {
            StartEnd n = positions.get(key);
            if (n == null)
                throw new IllegalStateException("No such key: \"" + key + "\"");
            else
                return n.end();
        }

        /**
         * Gets the content of a given node by referencing its position
         * against the source document that we hold.
         */
        public String nodeContent(NodeTree.NodeAndPosition<?> nap)
        {
            return doc.getFullText().substring(nap.getPosition(), nap.getEnd());
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
    public static Parsed parse(String src, TestEntityResolver resolver)
    {
        JavaLexer tokens = new JavaLexer(new StringReader(src));
        HashMap<String, StartEnd> locations = new HashMap<>();
        LocatableToken token;
        do
        {
            token = tokens.nextToken();
            if (token.getType() == JavaTokenTypes.ML_COMMENT)
            {
                locations.put(token.getText().substring(2, token.getLength() - 2).trim(), new StartEnd(token.getPosition(), token.getPosition() + token.getLength()));
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

    /**
     * Finds the direct child of "parent" within the parse tree of "p"
     * that matches the String comment key "key".
     * @param p The parsed document, from the parse method
     * @param parent The node to search for a direct child within
     * @param key The String comment key to look for (see parse method)
     * @param nodeStartsWithComment Whether the node starts at the start of the comment
     *                              (true for classes and methods, which treat the comment
     *                              as Javadoc even if not using double-star)
     *                              or after the comment (true for everything else)
     * @param expectedNodeType Assert that the node type of the found node is equal to this value
     * @return The found node
     */
    public static NodeTree.NodeAndPosition<ParsedNode> findInnerNode(Parsed p, NodeTree.NodeAndPosition<ParsedNode> parent, String key, boolean nodeStartsWithComment, int expectedNodeType)
    {
        int pos = nodeStartsWithComment ? p.positionStart(key) : p.positionEnd(key);
        // The reason for the +1 is as follows.  Sometimes we have a comment just before
        // a node, for example /*method*/void foo() {}
        // If we find the node at the end position of the "method" comment, we will find
        // the comment itself, which is almost never what we want.  The comments are there
        // as placeholders, not as something we're interested in.  And if our findNodeAt
        // request falls on the border between two elements it chooses the leftmost (earliest)
        // one, hence the comment.  As a slight hack to avoid this, we add one so that we
        // search after the position of interest:
        NodeTree.NodeAndPosition<ParsedNode> nap = parent.getNode().findNodeAt(pos + 1, parent.getPosition());
        assertEquals(pos, nap.getPosition());
        assertEquals(expectedNodeType, nap.getNode().getNodeType());
        return nap;
    }

    /**
     * Finds the body of a method tagged "method-inner" in a method tagged
     * "method" in a class body tagged "class-inner" in a class named "class"
     * See SwitchExpressionTest or other callers for an example of usage.
     */
    public static NodeTree.NodeAndPosition<ParsedNode> findMethodBody(Parsed p)
    {
        return findInnerNode(p,
            findInnerNode(p,
                findInnerNode(p,
                    findInnerNode(p, new NodeTree.NodeAndPosition<>(p.node(), 0, p.node().getSize()),    
                    "class", true, ParsedNode.NODETYPE_TYPEDEF
                ), "class-inner", true, ParsedNode.NODETYPE_NONE
                ), "method", true, ParsedNode.NODETYPE_METHODDEF     
            ), "method-inner", true, ParsedNode.NODETYPE_NONE        
        );
    }
}
