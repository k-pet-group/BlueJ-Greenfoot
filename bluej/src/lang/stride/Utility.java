/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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

public class Utility
{
    /**
     * Fills an array with an inclusive range
     * 
     * If start < end, range uses step 1
     * If start == end, range is size 1
     * If start > end, array will be empty
     */
    public static int[] makeRange(int start, int end)
    {
        if (start > end)
            return new int[0];

        int[] r = new int[end - start + 1];
        int val = start;
        for (int index = 0; index < r.length; index++)
        {
            r[index] = val;
            val += 1;
        }
        return r;
    }
}
