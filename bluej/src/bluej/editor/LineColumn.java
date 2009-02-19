/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
package bluej.editor;

/**
 * A LineColumn object groups two pieces of information: the line number and the column number.
 * They represent a position in the editor's text.
 * A text location represents the gap to the left of the position identified, so
 * that (0, 0) is the start of the file, (0, 1) is between the first and
 * second characters in the file, and so on. There is a LineColumn position to
 * the right of the last character on a line. 
 *
 * @definition An invalid LineColumn is one that, at the time of use, points to an area outside the
 * text being edited.
 * 
 * @version $Id: LineColumn.java 6163 2009-02-19 18:09:55Z polle $
 */

/*
 * @author Damiano Bolla, University of Kent at Canterbury, 2004
 */  
public class LineColumn
{
    private int line,column;
    
    /**
     * Create a LineColumn representing the text position at the specified line and column
     *
     * @param  line    a line number starting from 0
     * @param  column  a column number starting from 0
     */
    public LineColumn(int line, int column)
    {
        this.line = line;
        this.column = column;
    }


    /**
     * Sets the line of the text position, leaves the column unchanged.
     *
     * @param  line  the line number starting from zero
     */
    public void setLine(int line)
    {
        this.line = line;
    }


    /**
     * Returns the line of this text position
     *
     * @return    the line number of this text position
     */
    public int getLine()
    {
        return line;
    }


    /**
     * Sets the column where this caret should be, leaves the line unchanged.
     *
     * @param  column  the column number starting from zero
     */
    public void setColumn(int column)
    {
        this.column = column;
    }


    /**
     * Returns the column of this text location
     *
     * @return    the column number of this text location
     */
    public int getColumn()
    {
        return column;
    }


    /**
     * Set both the line and column where of text location
     *
     * @param  line    a line number starting from zero
     * @param  column  a column number starting from zero
     */
    public void setLineColumn(int line, int column)
    {
        this.line = line;
        this.column = column;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string that represents this object status
     */
    public String toString ()
    {
        return "line="+line+" column="+column;
    }
    
}
