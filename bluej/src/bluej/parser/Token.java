/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2019  Michael Kolling and John Rosenberg 
 
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

import com.google.common.collect.ImmutableSet;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * This is a replacement for the Token class from jedit.
 * 
 * @author Davin McCall
 */
@OnThread(Tag.Any)
public class Token
{
    public TokenType id;     // Token type, one of the constants declared below
    public int length;  // Length of text represented by this token
    public Token next;  // Next token in the chain

    @OnThread(Tag.Any)
    public static enum TokenType
    {
        DEFAULT, COMMENT_NORMAL, COMMENT_JAVADOC, COMMENT_SPECIAL, KEYWORD1, KEYWORD2, KEYWORD3, PRIMITIVE, STRING_LITERAL, CHAR_LITERAL, LABEL, OPERATOR, INVALID, END;

        public String getCSSClass()
        {
            return "token-" + name().toLowerCase().replace("_", "-");
        }

        private static ImmutableSet<String> ALL_CLASSES;

        public static ImmutableSet<String> allCSSClasses()
        {
            if (ALL_CLASSES == null)
            {
                ALL_CLASSES = ImmutableSet.copyOf(Arrays.stream(values()).map(TokenType::getCSSClass).collect(Collectors.toList()));
            }
            return ALL_CLASSES;
        }
    }
    
    public Token(int length, TokenType id)
    {
        this.id = id;
        this.length = length;
    }
}
