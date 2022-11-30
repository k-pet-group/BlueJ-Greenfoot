/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2013  Michael Kolling and John Rosenberg 
 
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

/**
 * A span between two line/column locations.
 *
 * @author  Andrew Patterson
 */
public class SourceSpan
{
    private SourceLocation start;
    private SourceLocation end;
    
    /**
     * @param start  the line/column location where the span starts
     * @param end    the line/column location where the span ends
     */
    public SourceSpan(SourceLocation start, SourceLocation end)
    {
        this.start = start;
        this.end = end;
    }
    
    /**
     * @param start    the line/column location where the span starts
     * @param numChars the number of characters (assumes span is only on one line)
     */
    public SourceSpan(SourceLocation start, int numChars)
    {
        this.start = start;
        this.end = new SourceLocation(start.getLine(), start.getColumn() + numChars);
    }

    /**
     * Determine if a span crosses more that one line.
     * 
     * @return  true if the span is only on one line.
     */
    public boolean isOneLine()
    {
        return (start.getLine() == end.getLine() );
    }
    
    public SourceLocation getStartLocation()
    {
        return start;
    }

    public int getStartColumn()
    {
        return start.getColumn();
    }

    public int getStartLine()
    {
        return start.getLine();
    }

    public SourceLocation getEndLocation()
    {
        return end;
    }

    public int getEndColumn()
    {
        return end.getColumn();
    }

    public int getEndLine()
    {
        return end.getLine();
    }
    
    @Override
    public String toString()
    {
        return start.toString() + "-" + end.toString();
    }
}
