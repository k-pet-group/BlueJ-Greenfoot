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
package bluej.extensions.editor;

/**
 * A TextLocation object groups two pieces of information: a line number and a column number.
 * They represent a position in the editor's text.
 * A text location represents the gap to the left of the position identified, so
 * that (0, 0) is the start of the file, (0, 1) is between the first and
 * second characters in the file, and so on. There is a TextLocation position to
 * the right of the last character on a line. The column value of this
 * position can be determined using Editor.getLineLength(int line).
 *
 * When applied to a particular edited text, a TextLocation may be <em>invalid</em>. 
 * That is, at the time of use, it points to an area outside the text being edited.
 * 
 * @version $Id: TextLocation.java 6215 2009-03-30 13:28:25Z polle $
 */

/*
 * @author Damiano Bolla, University of Kent at Canterbury, 2004
 */  
public class TextLocation
{
    private int line,column;
    
    /**
     * Create a TextLocation representing the text position at the specified line and column
     *
     * @param  line    a line number starting from 0
     * @param  column  a column number starting from 0
     */
    public TextLocation(int line, int column)
    {
        this.line = line;
        this.column = column;
    }


    /**
     * Sets the line number of this text position, leaving the column unchanged.
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
     * Sets the column of this text position, leaving the line number unchanged.
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
     * Set both the line number and column of this text location
     *
     * @param  line    a line number starting from zero
     * @param  column  a column number starting from zero
     */
    public void setPosition(int line, int column)
    {
        this.line = line;
        this.column = column;    
    }
    
    /**
     * Returns a string representation of this text location.
     *
     * @return a string that represents this object status
     */
    public String toString ()
    {
        return "line="+line+" column="+column;
    }
    
}
