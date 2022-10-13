/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2014,2015,2017,2022  Michael Kolling and John Rosenberg
 
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
    private final LineColPos begin;
    private final LineColPos end;
    private LocatableToken hiddenBefore;
    private final int type;
    private final String text;
    
    public LocatableToken(int t, String txt, LineColPos begin, LineColPos end)
    {
        type = t;
        text = txt;
        this.begin = begin;
        this.end = end;
    }
    
    public int getEndColumn()
    {
        return end.column();
    }
    
    public int getEndLine()
    {
        return end.line();
    }
    
    public int getLine()
    {
        return begin.line();
    }

    public LocatableToken adjustStart(int offset)
    {
        // Assume same line:
        return new LocatableToken(type, text, begin.offsetSameLineBy(offset), end.offsetSameLineBy(offset));
    }
    
    public int getColumn()
    {
        return begin.column();
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
        return end.position() - begin.position();
    }
    
    public int getPosition()
    {
        return begin.position();
    }
    
    public int getEndPosition()
    {
        return end.position();
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
        return "LocatableToken{" +
            "begin=" + begin +
            ", end=" + end +
            ", hiddenBefore=" + hiddenBefore +
            ", type=" + type +
            ", text='" + text + '\'' +
            '}';
    }
}
