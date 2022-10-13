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
 * A line/column location in a source file.
 *
 * Note that all line/column numbers start counting from 1.
 *
 * @author  Andrew Patterson
 */
public class SourceLocation
{
    private int line;
    private int column;
    
    public SourceLocation(int line, int column)
    {
        if (line < 1 || column < 1) {
            throw new IllegalArgumentException("line/column numbers must be > 0");
        }

        this.line = line;
        this.column = column;
    }

    /**
     * Gets the line number of this location
     */
    public int getLine()
    {
        return line;
    }

    /**
     * gets the column where this node reside
     * @return <code>int</code>
     */
    public int getColumn()
    {
        return column;
    }

    @Override
    public String toString()
    {   
        return "<" + line + "," + column + ">";
    }
}
