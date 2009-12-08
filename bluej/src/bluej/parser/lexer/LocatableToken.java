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
package bluej.parser.lexer;

public class LocatableToken
{
    private int beginLine, beginColumn;
    private int endLine, endColumn;
    private LocatableToken hiddenBefore;
    private int type;
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

    public void setLine(int line)
    {
        beginLine = line;
    }
    
    public void setColumn(int col)
    {
        beginColumn = col;
    }
    
    public int getColumn()
    {
        return beginColumn;
    }
    
    public int getType()
    {
        return type;
    }
    
    public String getText()
    {
        return text;
    }
    
    public int getLength()
    {
        return endColumn - beginColumn;
    }
    
    public void setHiddenBefore(LocatableToken t)
    {
        hiddenBefore = t;
    }
    
    public LocatableToken getHiddenBefore()
    {
        return hiddenBefore;
    }
}
