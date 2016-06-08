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
package bluej.stride.framedjava.slots;

import java.util.Optional;

import bluej.utility.Utility;

/**
 * A CaretPos is a singly-linked immutable list of integers, representing a caret position
 * in an expression.  The positions are relative to a particular component.  The last
 * integer in the list applies to a field, and any preceding integers refer to a field
 * position in an InfixStructured.
 *
 */
// package-visible
class CaretPos
{
    public final int index; // If this is a field, a caret position.  If this is a compound, it's an index (into fields array)
    // -1 for index is like ".." in paths; remove parent
    public final CaretPos subPos; // Null if and only if this is a field
    
    public CaretPos(int index, CaretPos subPos) { this.index=index;this.subPos = subPos;}
    // For debugging:
    public String toString() { return "" + index + (subPos == null ? "" : "->" + subPos.toString()); }
    
    /**
     * Returns true if this caret position is before (to the left of) the given position.
     * If they are equal, returns false.
     */
    public boolean before(CaretPos p)
    {
        if (index < p.index)
            return true;
        else if (index > p.index)
            return false;
        else if (subPos == null || p.subPos == null)
            return false;
        else
            return subPos.before(p.subPos);
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o instanceof CaretPos)
        {
            CaretPos pos = (CaretPos)o;
            return index == pos.index && ((subPos == null && pos.subPos == null) || (subPos != null && subPos.equals(pos.subPos)));
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return index % 31;
    }

    /**
     * Returns a copy of this CaretPos, with the given CaretPos appended to the end.
     */
    public CaretPos append(CaretPos replacement)
    {
        if (subPos == null)
        {
            return new CaretPos(index, replacement);
        }
        else
        {
            return new CaretPos(index, subPos.append(replacement));
        }
    }
    
    /**
     * Checks if the given CaretPos is a prefix of this list.  If so,
     * returns an integer with the following element from this list.  
     * 
     * That is, if "this" is 3->6->2->5->null, and the parameter is 3->6->null, the method
     * returns Optional.of(2).  If the parameter was 3->5->null, it returns Optional.empty().
     */
    public Optional<Integer> getFollowing(CaretPos outer)
    {
        if (outer == null)
            return Optional.of(index);
        else if (index == outer.index)
            return subPos.getFollowing(outer.subPos);
        else
            return Optional.empty();
    }
    
    /**
     * Returns a normalised copy of this CaretPos.  The value of -1 for index acts as like a ".." in directories,
     * heading up to a parent.  This method normalises a CaretPos where possible, putting those -1
     * into effect.  So if this CaretPos is 3->4->-1->5->7->8->-1->-1->0->null, the method
     * will return 3->5->0->null.
     */
    public CaretPos normalise()
    {
        if (subPos != null && subPos.index == -1)
            return Utility.orNull(subPos.subPos, CaretPos::normalise); // Wipe out us
        else if (subPos != null)
            return new CaretPos(index, subPos.normalise());
        else
            return this;
    }
    
    public static boolean between(CaretPos start, CaretPos end, CaretPos p)
    {
        // If bounds are both empty, must be inbetween
        if (start == null && end == null)
            return true;
        // If at least one bound is present, and target is null, is not inside: 
        else if (p == null)
            return false;
        
        if ((start == null || start.index <= p.index) && (end == null || p.index <= end.index))
        {
            // Matches at this level; dig down.
            // We pass the subpositions for start/end if they match p at this level.  If they don't,
            // e.g. start is 2->2, p is 3->0, we pass null because we know it's past that bound.
            return between(start == null || start.index < p.index ? null : start.subPos, end == null || p.index < end.index ? null : end.subPos, p.subPos);
        }
        else
        {
            return false;
        }
    }
    
    // Gets a copy with the last item removed.  So 3->5->7->null becomes 3->5->null
    public CaretPos init()
    {
        if (subPos == null)
            return null; // This removes last item
        else
            return new CaretPos(index, subPos.init());
    }
}