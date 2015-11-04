/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014  Michael Kolling and John Rosenberg 
 
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
package bluej.utility;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;

import javax.swing.SizeRequirements;

/**
 * A handy layout manager, similar to BoxLayout but which handles component alignment
 * in a more useful fashion and which provides some handy additional functionality.
 * 
 * @author Davin McCall
 */
public class DBoxLayout implements LayoutManager2
{
    public static int X_AXIS = 0;
    public static int Y_AXIS = 1;

    /** A SizeRequirements object representing an empty size */
    private static final SizeRequirements noSize = new SizeRequirements(0, 0, 0, 0.5f); 
    
    private int axis;
    
    /** minimum required space between components */
    private int minComponentSpacing;
    /** preferred space between components */
    private int componentSpacing;
    
    /** Size requirements in the X axis for each component */
    private SizeRequirements [] sizeReqsX;
    /** Size requirements in the Y axis for each component */
    private SizeRequirements [] sizeReqsY;
    /** Total size requirements in the X axis */
    private SizeRequirements totalReqsX;
    /** Total size requirements in the Y axis */
    private SizeRequirements totalReqsY;
    /** The number of visible components */
    private int visibleCount;
    
    // Fields below here are used during a layout operation, and are meaningless
    // outside the operation.
    
    /** During a layout, this keeps track of the current layout position */
    private int curPos;
    
    // Following three variables track the spacing between components. In some
    // cases it is not an integer, so we need to track the fractional part.
    private boolean needSpaces;
    private int spacingNumerator;
    private int spacingDenom;
    private int spacingRemainder;
    
    /** The insets of the parent component (due to its border) */
    private Insets insets;
        
    /**
     * Construct a DBoxLayout to layout components along the given axis.
     * @param axis   either X_AXIS or Y_AXIS
     */
    public DBoxLayout(int axis)
    {
        this.axis = axis;
    }
    
    /**
     * Construct a DBoxLayout to layout components along the given axis,
     * with the given amount of space between each components. The minimum
     * space requirement will always be inserted between each component; the
     * preferred spacing will be provided only when all components are able
     * to reach their preferred size.
     * 
     * @param axis
     * @param minSpacing
     * @param prefSpacing
     */
    public DBoxLayout(int axis, int minSpacing, int prefSpacing)
    {
        this.axis = axis;
        minComponentSpacing = minSpacing;
        componentSpacing = prefSpacing;
    }
    
    /**
     * Return true if the X_AXIS is the primary layout axis.
     */
    public boolean isXPrimaryAxis()
    {
        return axis == X_AXIS;
    }
    
    /**
     * This is called during a layout operation. It places a component
     * at the current position and updates the current position.
     * 
     * @param c   The component to place
     * @param space    The amount of space given to the component in the
     *                 primary axis direction
     * @param opposedPos   The position in the opposed axis
     * @param opposedSize  The size on the opposed axis
     */
    private void placeComponent(Component c, int space, int opposedPos, int opposedSize)
    {
        // Take spacing between components into account
        int advance = spacingNumerator / spacingDenom;
        spacingRemainder += spacingNumerator % spacingDenom;
        if (spacingRemainder > spacingDenom) {
            spacingRemainder -= spacingDenom;
            advance++;
        }
        
        if (axis == X_AXIS) {
            c.setBounds(curPos + insets.left, opposedPos + insets.top, space, opposedSize);
            curPos += space + advance;
        }
        else {
            c.setBounds(opposedPos + insets.left, curPos + insets.top, opposedSize, space);
            curPos += space + advance;
        }
    }
    
    /**
     * Make sure all size requirement calculations are up-to-date.
     * @param target  The container whose layout we are managing
     */
    private void calcSizeReqs(Container target)
    {
        if (sizeReqsX == null || sizeReqsY == null) {
            Component [] components = target.getComponents();
            recalcSizeReqs(components);
        }
    }
    
    /**
     * Make sure all size requirement calculations are up-to-date.
     * Sets needSpaces variable according to whether spacing may need to
     * be added between components (i.e. there is more than one visible
     * component, and the spacing amount is non-zero) and visibleCount
     * to the number of visible components.
     * 
     * @param components  The components in the container whose layout we
     *                    are managing
     */
    private void calcSizeReqs(Component [] components)
    {
        if (sizeReqsX == null || sizeReqsY == null) {
            recalcSizeReqs(components);
        }
    }
    
    /**
     * Force re-calculation of all size requirements. A dummy requirement
     * for the spacing between components is also calculated if necessary.
     * 
     * @param components  The components in the target container
     */
    private void recalcSizeReqs(Component [] components)
    {
        int extra = (componentSpacing != 0 && components.length != 0) ? 1 : 0;
        sizeReqsX = new SizeRequirements[components.length + extra];
        sizeReqsY = new SizeRequirements[components.length + extra];
        
        totalReqsX = new SizeRequirements(0, 0, 0, 0.5f);
        totalReqsY = new SizeRequirements(0, 0, 0, 0.5f);
        
        visibleCount = 0;
        
        int i;
        for (i = 0; i < components.length; i++) {
            Component component = components[i];
            if (component.isVisible()) {
                visibleCount++;
                Dimension min = component.getMinimumSize();
                Dimension pref = component.getPreferredSize();
                Dimension max = component.getMaximumSize();
                
                sizeReqsX[i] = new SizeRequirements(min.width, pref.width, max.width, component.getAlignmentX());
                sizeReqsY[i] = new SizeRequirements(min.height, pref.height, max.height, component.getAlignmentY());
                
                if (isXPrimaryAxis()) {
                    addReqs(totalReqsX, sizeReqsX[i]);
                    maxReq(totalReqsY, sizeReqsY[i]);
                }
                else {
                    addReqs(totalReqsY, sizeReqsY[i]);
                    maxReq(totalReqsX, sizeReqsX[i]);
                }
            }
            else {
                sizeReqsX[i] = noSize;
                sizeReqsY[i] = noSize;
            }
        }
        
        // adjust for spacing between components
        if (visibleCount > 1 && componentSpacing != 0) {
            needSpaces = true;
            sizeReqsX[i] = new SizeRequirements();
            sizeReqsY[i] = new SizeRequirements();
            int minSpacing = (visibleCount - 1) * minComponentSpacing;
            int spacing = (visibleCount - 1) * componentSpacing;
            if (isXPrimaryAxis()) {
                sizeReqsX[i].minimum = minSpacing;
                sizeReqsX[i].preferred = spacing;
                sizeReqsX[i].maximum = spacing;
                addReqs(totalReqsX, sizeReqsX[i]);
            }
            else {
                sizeReqsY[i].minimum = minSpacing;
                sizeReqsY[i].preferred = spacing;
                sizeReqsY[i].maximum = spacing;
                addReqs(totalReqsY, sizeReqsY[i]);
            }
        }
        else {
            needSpaces = false;
        }
    }

    /**
     * Adjust a dimension to take the given insets into account.
     * 
     * @param d   The dimension to adjust (will be modified)
     * @param insets  The insets to consider
     * @return The adjusted Dimension object
     */
    private Dimension adjustSizeForInsets(Dimension d, Insets insets)
    {
        d.width = (int) Math.min((long) d.width + insets.left
                + insets.right, Integer.MAX_VALUE);
        d.height = (int) Math.min((long) d.height + insets.top
                + insets.bottom, Integer.MAX_VALUE);
        return d;
    }
        
    /**
     * Add one set of size requirements (b) to another (a).
     * @param a  The first set of size requirements (will be modified)
     * @param b  The second set of size requirements
     */
    private void addReqs(SizeRequirements a, SizeRequirements b)
    {
        a.minimum = restrictedAdd(a.minimum, b.minimum);
        a.preferred = restrictedAdd(a.preferred, b.preferred);
        a.maximum = restrictedAdd(a.maximum, b.maximum);
    }
    
    /**
     * Add two integers, but cap the result at Integer.MAX_VALUE
     * @param a  The first integer
     * @param b  The second integer
     * @return  The result
     */
    private int restrictedAdd(int a, int b)
    {
        return (int) Math.min((long) a + b, Integer.MAX_VALUE);
    }

    /**
     * Make sure that one set of size requirements (a) is at least as
     * large as another (b).
     * @param a  The first set of size requirements
     * @param b  The second set of size requirements
     */
    private void maxReq(SizeRequirements a, SizeRequirements b)
    {
        a.minimum = Math.max(a.minimum, b.minimum);
        a.preferred = Math.max(a.preferred, b.preferred);
        a.maximum = Math.max(a.maximum, b.maximum);
    }

    /**
     * Get the "A-size" from a dimension - that is the size along the
     * layout axis.
     * 
     * @param d  The dimension to get the "A-size" from
     * @return  The "A-size" of the dimension
     */
    private int getASize(Dimension d)
    {
        if (axis == X_AXIS) {
            return d.width;
        }
        else {
            return d.height;
        }
    }

    /**
     * Get the "B-size" from a dimension - that is the size along the
     * opposing axis of the layout axis.
     * 
     * @param d  The dimension to get the "B-size" from
     * @return  The "A-size" of the dimension
     */
    private int getBSize(Dimension d)
    {
        if (axis == X_AXIS) {
            return d.height;
        }
        else {
            return d.width;
        }
    }

    /**
     * Return the alignment value of a component along the opposed axis
     * @param component
     * @return
     */
    private float getOpposedAlignment(Component component)
    {
        if (axis == X_AXIS) {
            return component.getAlignmentY();
        }
        else {
            return component.getAlignmentX();
        }
    }
        
    /**
     * Make a duplicate of a SizeRequirements object.
     * @param src  The object to duplicate
     * @return  The duplicate object
     */
    private SizeRequirements copySizeReqs(SizeRequirements src)
    {
        return new SizeRequirements(src.minimum, src.preferred,
                src.maximum, src.alignment);
    }


    // ----- LayoutManager2 interface -----
    
    @Override
    public void addLayoutComponent(Component comp, Object constraints)
    {
    }

    @Override
    public float getLayoutAlignmentX(Container target)
    {
        return 0.5f;
    }

    @Override
    public float getLayoutAlignmentY(Container target)
    {
        return 0.5f;
    }

    @Override
    public void invalidateLayout(Container target)
    {
        sizeReqsX = null;
        sizeReqsY = null;
    }

    @Override
    public void addLayoutComponent(String name, Component comp)
    {
    }

    @Override
    public void removeLayoutComponent(Component comp)
    {
    }
    
    @Override
    public Dimension minimumLayoutSize(Container parent)
    {
        calcSizeReqs(parent);
        Insets insets = parent.getInsets();
        Dimension d = new Dimension(totalReqsX.minimum, totalReqsY.minimum);
        return adjustSizeForInsets(d, insets); 
    }
    
    @Override
    public Dimension preferredLayoutSize(Container parent)
    {
        calcSizeReqs(parent);
        Insets insets = parent.getInsets();
        Dimension d = new Dimension(totalReqsX.preferred, totalReqsY.preferred);
        return adjustSizeForInsets(d, insets); 
    }

    @Override
    public Dimension maximumLayoutSize(Container parent)
    {
        calcSizeReqs(parent);
        Insets insets = parent.getInsets();
        Dimension d = new Dimension(totalReqsX.maximum, totalReqsY.maximum);
        return adjustSizeForInsets(d, insets); 
    }

    @Override
    public void layoutContainer(Container parent)
    {
        Component [] components = parent.getComponents();
        
        if (components.length == 0) {
            // Handle this easy case first; simplifies some of the later
            // logic anyway (avoids the need to explictly check for
            // 0 components when calculating spacing)
            return;
        }
        
        calcSizeReqs(components);
        
        insets = parent.getInsets();
        Dimension parentSize = parent.getSize();
        int availSize = getASize(parentSize);
        int opposedSize = getBSize(parentSize);
        SizeRequirements [] sizeReqs;
        SizeRequirements [] opposedReqs;
        SizeRequirements totalSizeReqs;
        
        if (isXPrimaryAxis()) {
            sizeReqs = sizeReqsX;
            opposedReqs = sizeReqsY;
            totalSizeReqs = copySizeReqs(totalReqsX);
            availSize -= insets.left + insets.right;
            opposedSize -= insets.top + insets.bottom;
        }
        else {
            sizeReqs = sizeReqsY;
            opposedReqs = sizeReqsX;
            totalSizeReqs = copySizeReqs(totalReqsY);
            availSize -= insets.top + insets.bottom;
            opposedSize -= insets.left + insets.right;
        }
        
        // First we need to allocate space to all components.
        // We allocate one additional for the spacing between components.
        int numComponents = components.length + (needSpaces ? 1 : 0);
        int [] space = new int[numComponents];
        int [] diffs = new int[numComponents];
        boolean [] fixed = new boolean[numComponents];
        if (needSpaces) {
            // Reset the spacing minimum/preferred/maximum, as sometimes it
            // gets butchered (and it is reset when sizes are recalculated anyway)
            int minSpace = minComponentSpacing * (visibleCount - 1);
            int prefSpace = componentSpacing * (visibleCount - 1);
            SizeRequirements spaceReqs = sizeReqs[numComponents - 1];
            
            totalSizeReqs.minimum += minSpace - spaceReqs.minimum;
            totalSizeReqs.preferred += prefSpace - spaceReqs.preferred;
            totalSizeReqs.maximum += prefSpace - spaceReqs.maximum;
            
            spaceReqs.minimum = minSpace;
            spaceReqs.preferred = prefSpace;
            spaceReqs.maximum = prefSpace;
        }
        
        if (totalSizeReqs.maximum < availSize) {
            // Center if available size exceeds maximum size
            curPos = (availSize - totalSizeReqs.maximum) / 2;
            availSize = totalSizeReqs.maximum;
        }
        else {
            curPos = 0;
        }

        boolean needAnotherPass;

        adjust:
        do {
            needAnotherPass = false;
            if (totalSizeReqs.minimum >= availSize) {
                // One simple case: We don't have enough space for everything.
                // Just set every component to its minimum size.
                for (int i = 0; i < numComponents; i++) {
                    space[i] = sizeReqs[i].minimum;
                }
            }
            else if (totalSizeReqs.preferred >= availSize) {
                // The available space is greater than the minimum size,
                // but smaller than the preferred size
                
                // First we'll try and shrink the space between components, rather
                // than shrink the components themselves.
                if (needSpaces) {
                    SizeRequirements spaceReqs = sizeReqs[numComponents - 1];
                    int diff = spaceReqs.preferred - spaceReqs.minimum;
                    if (diff + availSize >= totalSizeReqs.preferred) {
                        spaceReqs.preferred -= totalSizeReqs.preferred - availSize;
                        totalSizeReqs.preferred = availSize;
                    }
                    else {
                        spaceReqs.preferred = spaceReqs.minimum;
                        totalSizeReqs.preferred -= diff;
                    }
                }
                
                int totalDiffs = 0;
                int discrepancy = availSize - totalSizeReqs.minimum;
                
                for (int i = 0; i < numComponents; i++) {
                    if (! fixed[i]) {
                        diffs[i] = sizeReqs[i].preferred - sizeReqs[i].minimum;
                        totalDiffs += diffs[i];
                    }
                }
                
                for (int i = 0; i < numComponents; i++) {
                    if (! fixed[i]) {
                        if (diffs[i] != 0) {
                            space[i] = (int) (sizeReqs[i].minimum + (long) diffs[i] * discrepancy / totalDiffs);
                        }
                        else {
                            space[i] = sizeReqs[i].minimum;
                            fixed[i] = true;
                        }
                        if (space[i] > sizeReqs[i].maximum) {
                            space[i] = sizeReqs[i].maximum;
                            totalSizeReqs.minimum -= sizeReqs[i].minimum;
                            totalSizeReqs.preferred -= sizeReqs[i].preferred;
                            totalSizeReqs.maximum -= sizeReqs[i].maximum;
                            fixed[i] = true;
                            availSize -= space[i];
                            needAnotherPass = true;
                            continue adjust;
                        }
                    }
                }
            }
            else {
                // available size is greater than preferred size
                if (totalSizeReqs.preferred == 0) {
                    int numComponentsLeft = 0;
                    int unassignedSpace = availSize;
                    for (int i = 0; i < numComponents; i++) {
                        if (! fixed[i]) {
                            numComponentsLeft++;
                            unassignedSpace -= sizeReqs[i].preferred;
                        }
                    }
                            
                    for (int i = 0; i < numComponents; i++) {
                        if (! fixed[i]) {
                            int toAssign = unassignedSpace / numComponentsLeft;
                            space[i] = restrictedAdd(sizeReqs[i].preferred, toAssign);
                            if (space[i] > sizeReqs[i].maximum) {
                                space[i] = sizeReqs[i].maximum;
                                totalSizeReqs.minimum -= sizeReqs[i].minimum;
                                totalSizeReqs.preferred -= sizeReqs[i].preferred;
                                totalSizeReqs.maximum -= sizeReqs[i].maximum;
                                fixed[i] = true;
                                availSize -= space[i];
                                needAnotherPass = true;
                                continue adjust;
                            }
                        }
                    }
                }
                else {
                    // available size is greater than preferred size;
                    // some components do have a preferred size
                    for (int i = 0; i < numComponents; i++) {
                        if (! fixed[i]) {
                            space[i] = (int) ((long) sizeReqs[i].preferred * availSize / totalSizeReqs.preferred);
                            if (space[i] > sizeReqs[i].maximum) {
                                // Some component maxed out. This means we'll need
                                // another pass.
                                space[i] = sizeReqs[i].maximum;
                                totalSizeReqs.minimum -= sizeReqs[i].minimum;
                                totalSizeReqs.preferred -= sizeReqs[i].preferred;
                                totalSizeReqs.maximum -= sizeReqs[i].maximum;
                                fixed[i] = true;
                                availSize -= space[i];
                                needAnotherPass = true;
                                continue adjust;
                            }
                        }
                    }
                }
            }
        } while (needAnotherPass);
        
        // Now we know how much space to give each component, we can
        // set the position of each component according to its alignment
        // and constraints
        
        if (needSpaces) {
            spacingNumerator = space[numComponents - 1];
            spacingDenom = visibleCount - 1;
        }
        else {
            spacingNumerator = 0;
            spacingDenom = 1;
        }
        spacingRemainder = 0;
        
        for (int i = 0; i < components.length; i++) {
            if (components[i].isVisible()) {
                if (opposedReqs[i].maximum < opposedSize) {
                    // We need to position the component according to
                    // its alignment
                    float alignment = getOpposedAlignment(components[i]);
                    int offset = (int)((opposedSize - opposedReqs[i].maximum) * alignment);
                    placeComponent(components[i], space[i], offset, opposedReqs[i].maximum);
                }
                else {
                    placeComponent(components[i], space[i], 0, opposedSize);
                }
            }
        }
    }

}
