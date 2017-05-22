/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2014,2015,2017  Michael Kolling and John Rosenberg
 
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
package bluej.parser.lexer;

import threadchecker.OnThread;
import threadchecker.Tag;

@OnThread(Tag.Any)
public class LocatableToken
{
    private int beginLine, beginColumn;
    private int endLine, endColumn;
    private LocatableToken hiddenBefore;
    private int type;
    private int position, length; // position and length in original source
    private String text;
    
    public LocatableToken(int t, String txt)
    {
        type = t;
        text = txt;
    }

    public void setEndLineAndCol(int l, int c)
    {
        endLine = l;
        endColumn = c;
    }
    
    public int getEndColumn()
    {
        return endColumn;
    }
    
    public int getEndLine()
    {
        return endLine;
    }
    
    public int getLine()
    {
        return beginLine;
    }

    public void setPosition(int beginLine, int beginColumn, int endLine, int endColumn, int position, int length)
    {
        this.beginLine = beginLine;
        this.beginColumn = beginColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
        this.position = position;
        this.length = length;
    }

    public void adjustStart(int offset)
    {
        // Assume same line:
        beginColumn += offset;
        endColumn += offset;
        position += offset;
    }
    
    public int getColumn()
    {
        return beginColumn;
    }
    
    public int getType()
    {
        return type;
    }
    
    /**
     * Gets the text of the token, with any unicode escapes from the original
     * taken care of.
     * 
     * For example, the original code may have String with the capital S escaped,
     * like "\u0053tring".  In this case, getText() would return "String".
     */
    public String getText()
    {
        return text;
    }
    
    /**
     * Returns the length of the token in the original source.  Note that
     * this is not necessarily the same as getText().length(), because the original
     * token may have contained unicode escapes.  In this case, getText() will
     * return the processed version, without escapes, but getLength() will
     * still return the length of the original token in the document, including
     * all the escapes.
     */
    public int getLength()
    {
        return length;
    }
    
    public int getPosition()
    {
        return position;
    }
    
    public int getEndPosition()
    {
        return position + length;
    }
    
    public void setHiddenBefore(LocatableToken t)
    {
        hiddenBefore = t;
    }
    
    public LocatableToken getHiddenBefore()
    {
        return hiddenBefore;
    }

    @Override
    public String toString()
    {
        return "LocatableToken [beginLine=" + beginLine + ", beginColumn="
                + beginColumn + ", endLine=" + endLine + ", endColumn="
                + endColumn + ", hiddenBefore=" + hiddenBefore + ", type="
                + type + ", position=" + position + ", length=" + length
                + ", text=" + text + "]";
    }
    
    
}
