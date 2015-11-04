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
package bluej.stride.framedjava.slots;

import java.util.function.Function;

// Package-visible
class PosAndDist
{
    private final CaretPos pos;
    private final double dist;
    
    public PosAndDist() { pos = null; dist = -1; }
    
    public PosAndDist(CaretPos pos, double dist)
    {
        this.pos = pos;
        this.dist = dist;
    }

    public static PosAndDist nearest(PosAndDist a, PosAndDist b)
    {
        if (a.pos == null || a.dist == -1) return b; // Even if they are both blank, this makes sense
        if (b.pos == null || b.dist == -1) return a;
        
        if (a.dist <= b.dist)
            return a;
        else
            return b;
    }
    
    public PosAndDist copyAdjustPos(Function<CaretPos, CaretPos> f)
    {
        return new PosAndDist(f.apply(pos), dist);
    }
    
    public CaretPos getPos()
    {
        return pos;
    }
}
