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
import bluej.parser.nodes.ParsedCUNode;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * A class with useful methods for testing the parser.
 */
public class ParseUtility
{

    /**
     * A record with the output of the parse method, below.
     * Gives the document, the parser node, and a map from identifier to integer positions within the string.
     */
    public record Parsed(TestableDocument doc, ParsedCUNode node, Map<String, Integer> positions)
    {
        /**
         * Gets the position within the source of the given String key
         * (see parse method below for more details)
         */
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
    public static Parsed parse(String src, TestEntityResolver resolver)
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
}
