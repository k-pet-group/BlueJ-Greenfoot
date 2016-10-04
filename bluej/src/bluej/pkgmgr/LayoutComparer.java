/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013,2015,2016  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr;

import java.util.Comparator;

import bluej.pkgmgr.dependency.*;
import bluej.pkgmgr.target.*;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An ordering on targets to make layout nicer (reduce line intersections, etc.). This
 * class defines an ordering between any two Dependency objects "A" and "B", relative
 * to a third "central" Dependency. The ordering determines the relative position of
 * each dependency line as it enters the side (top, left, bottom, right) of the central
 * Dependency.
 *  
 * <p>The area around the central dependency is divided into 4 quadrants:
 * 
 * <pre>
 *     0  |  1
 *   ----[+]----
 *     2  |  3
 * </pre>
 * 
 * If the two dependencies being compared, A and B, are in different quadrants qA and qB,
 * then A < B iff qA < qB and vice versa. On the other hand if qA == qB then the ordering
 * depends on whether we are drawing lines in to the central Dependency or out of it 
 * ("in" lines come into either side of the Target, "out" lines go from the top or bottom;
 * in either case the line goes into the edge which is closest to the target from which it
 * is drawn, and the ordering is only important between dependencies into/out of the same
 * edge).
 * 
 * <p>
 * So, if qA == qB, then:<br>
 * For "in" dependencies, A < B if Ax < Bx and vice versa;<br>
 * For "out" dependencies, A < B if Ay < By and vice versa.<br>
 * However, for quadrant 0 and 3 the ordering is reversed; because arrow lines drawn
 * to/from quadrants 0 and 3 turn the opposite way to those in adjacent quadrants
 * (1 and 2), the order must change to avoid those lines from crossing.
 *
 * @author Michael Cahill
 */
@OnThread(Tag.FXPlatform)
public class LayoutComparer implements Comparator<Dependency>
{
    DependentTarget centre;
    boolean in;

    public LayoutComparer(DependentTarget centre, boolean in)
    {
        this.centre = centre;
        this.in = in;
    }

    /**
     * Order <a> and <b> depending on their relative positions
     * and their positions relative to the centre
     *
     * Note: this is designed to reduce intersections when drawing lines.
     */
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public int compare(Dependency a, Dependency b)
    {
        DependentTarget ta = in ? a.getFrom() : a.getTo();
        DependentTarget tb = in ? b.getFrom() : b.getTo();

        int ax = ta.getX() + ta.getWidth()/2;
        int ay = ta.getY() + ta.getHeight()/2;
        int bx = tb.getX() + tb.getWidth()/2;
        int by = tb.getY() + tb.getHeight()/2;

        if((ax == bx) && (ay == by))
            return 0;

        int cx = centre.getX() + centre.getWidth()/2;
        int cy = centre.getY() + centre.getHeight()/2;

        return compare(ax, ay, bx, by, cx, cy);
    }

    /**
     * Separate method to allow testing:
     */
    protected int compare(int ax, int ay, int bx, int by, int cx, int cy)
    {
        if((ax == bx) && (ay == by))
            return 0;

        boolean a_above = (ay < cy);
        boolean a_left = (ax < cx);
        int a_quad = (a_above ? 0 : 2) + (a_left ? 0 : 1);
        boolean b_above = (by < cy);
        boolean b_left = (bx < cx);
        int b_quad = (b_above ? 0 : 2) + (b_left ? 0 : 1);

        if(a_quad != b_quad) // different quadrants
            return (a_quad > b_quad) ? 1 : -1;
        
        // otherwise, we're in the same quadrant:
        int result = in ? compareInt(ax,bx) : compareInt(ay, by);
        
        // if a_above == a_left, qA and qB are either 0 or 3 (top left
        // or bottom right). Since arrows drawn from/to this quadrant
        // turn the other way to arrows in the adjacent quadrant, we
        // reverse the ordering calculated above.
        
        return (a_above == a_left) ? -result : result;
    }
    
    /**
     * Compare two integers and return a result indicating that the first is less than (-1),
     * equal to (0) or greater than (1) the second.
     */
    private int compareInt(int a, int b)
    {
        if (a == b) {
            return 0;
        }
        
        return (a < b) ? -1 : 1;
    }
}
