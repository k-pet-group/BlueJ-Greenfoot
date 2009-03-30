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
package bluej.parser.symtab;

import bluej.parser.SourceLocation;
import bluej.parser.SourceSpan;

/*******************************************************************************
 * An occurrence of an indentifier in a file
 ******************************************************************************/
// TODO, get rid of this class, just use SourceSpan
public class Selection /* extends Occurrence */
{
    // private int len;
    private SourceSpan sspan;
    //private String origText;

    //==========================================================================
    //==  Methods
    //==========================================================================

    /**
     * Constructor to define an empty selection at a given location.
     */
    public Selection(int line, int column)
    {
        SourceLocation sl = new SourceLocation(line, column);
        sspan = new SourceSpan(sl, sl);
    }
    
    public Selection(SourceSpan ss)
    {
        sspan = ss;
    }
    
    /**
     * Constructor for a selection which occupies part of a single line.
     * @param line    The line
     * @param column  The starting column
     * @param length  The length of the selection
     */
    public Selection(int line, int column, int length)
    {
        SourceLocation start = new SourceLocation(line, column);
        SourceLocation end = new SourceLocation(line, column + length);
        sspan = new SourceSpan(start, end);
    }
    
    /**
     * Combine two selections. The result will comprise of both the original and
     * the other selection, plus any space in between.
     */
    public void combineWith(Selection other)
    {
        int otherstartl = other.getLine();
        int otherstartc = other.getColumn();
        int mystartl = getLine();
        int mystartc = getColumn();
        
        SourceLocation newStart = null;
        SourceLocation newEnd = null;
        
        if (otherstartl < mystartl || (otherstartl == mystartl && otherstartc < mystartc))
            newStart = other.getStartLocation();
        
        int otherendl = other.getEndLine();
        int otherendc = other.getEndColumn();
        int myendl = getEndLine();
        int myendc = getEndColumn();
        
        if (otherendl > myendl || (otherendl == myendl && otherendc > myendc))
            newEnd = other.getEndLocation();
     
        if (newStart != null || newEnd != null) {
            if (newStart == null)
                newStart = getStartLocation();
            if (newEnd == null)
                newEnd = getEndLocation();
            sspan = new SourceSpan(newStart, newEnd);
        }
    }
    
    public void extendEnd(int line, int column)
    {
        int myline = getEndLine();
        if (line >= myline) {
            int mycol = getEndColumn();
            if (column >= mycol || line > myline) {
                SourceLocation newEnd = new SourceLocation(line, column);
                SourceLocation newStart = getStartLocation();
                sspan = new SourceSpan(newStart, newEnd);
            }
        }
    }
    
    public void extendEnd(SourceLocation sl)
    {
        extendEnd(sl.getLine(), sl.getColumn());
    }

    public int getLine() { return sspan.getStartLine(); }
    public int getColumn() { return sspan.getStartColumn(); }
    public SourceLocation getStartLocation() { return sspan.getStartLocation(); }
    
    public int getEndLine() { return sspan.getEndLine(); }
    public int getEndColumn() { return sspan.getEndColumn(); }
    public SourceLocation getEndLocation() { return sspan.getEndLocation(); }
        
    /** return a string representation of the occurrence */
    public String getLocation() {
        return "[" + sspan.toString() + "]";
    }

    /** return a string representation of the occurrence */
    public String toString() {
        return "Selection " + getLocation();
    }
}
