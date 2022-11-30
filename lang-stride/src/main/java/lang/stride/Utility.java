/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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
package lang.stride;

import java.util.AbstractList;
import java.util.List;

public class Utility
{
    /**
     * Fills an array with an inclusive range
     * 
     * If start < end, range uses step 1
     * If start == end, range is size 1
     * If start > end, array will be empty
     */
    public static List<Integer> makeRange(int start, int end)
    {
        return new AbstractList<Integer>()
        {
            private final boolean empty = start > end;

            @Override
            public int size()
            {
                return empty ? 0 : end - start + 1;
            }

            @Override
            public Integer get(int index)
            {
                if (!empty && index >= 0 && start + index <= end)
                    return start + index;
                else
                    throw new IndexOutOfBoundsException("Not in bounds: " + index);
            }
        };
    }
}
