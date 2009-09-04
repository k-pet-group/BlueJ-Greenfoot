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
package bluej.editor.moe;

/**
 * This is a replacement for the Token class from jedit.
 * 
 * @author Davin McCall
 */
public class Token
{
    public byte id;     // Token type, one of the constants declared below
    public int length;  // Length of text represented by this token
    public Token next;  // Next token in the chain
    
    public static final byte NULL = 0;
    public static final byte COMMENT1 = 1;  // normal comment
    public static final byte COMMENT2 = 2;  // javadoc comment
    public static final byte COMMENT3 = 3;  // standout comment
    public static final byte KEYWORD1 = 4;
    public static final byte KEYWORD2 = 5;
    public static final byte KEYWORD3 = 6;
    public static final byte PRIMITIVE = 7;
    public static final byte LITERAL1 = 8;
    public static final byte LITERAL2 = 9;
    public static final byte LABEL = 10;
    public static final byte OPERATOR = 11;
    public static final byte INVALID = 12;
    
    /* The number of token ids (above) */
    public static final byte ID_COUNT = 13;
    
    public static final byte END = 100;
    
    public Token(int length, byte id)
    {
        this.id = id;
        this.length = length;
    }
}
