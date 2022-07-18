/*
 This file is part of the BlueJ program. 
 Copyright (C) 2022  Michael Kolling and John Rosenberg 

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

/**
 * A line, column and position with a String
 * @param line The line, 1 is the top line
 * @param column The column, 1 is the leftmost column
 * @param position The position in the overall String as a character index
 */
@OnThread(Tag.Any)
public record LineColPos(int line, int column, int position)
{
    public LineColPos offsetSameLineBy(int offset)
    {
        return new LineColPos(line, column + offset, position + offset);
    }
}
