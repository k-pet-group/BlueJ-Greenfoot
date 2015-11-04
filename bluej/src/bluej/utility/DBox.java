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
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * An improved Box class. It allows specifying a default alignment for
 * added components, and has helpful methods for adding spacers.
 * 
 * @author Davin McCall
 */
public class DBox extends JPanel
{
    private float defaultAlignment;
    private int axis;
    
    private boolean xAxisBounded;
    private boolean yAxisBounded;
    
    public static final int X_AXIS = DBoxLayout.X_AXIS;
    public static final int Y_AXIS = DBoxLayout.Y_AXIS;
    
    /**
     * Constructor for DBox, with a specified default alignment. Parameter
     * axis should be one of DBoxLayout.X_AXIS or DBoxLayout.Y_AXIS.
     * Parameter alignmentVal should be 0.0 for left, 0.5 for centered or
     * 1.0 for right alignment. 
     * 
     * @param axis  Specifies layout direction. Either be X_AXIS or Y_AXIS.
     * @param alignmentVal   The default component alignment (0.0 - 1.0)
     */
    public DBox(int axis, float alignmentVal)
    {
        setLayout(new DBoxLayout(axis));
        defaultAlignment = alignmentVal;
        this.axis = axis;
    }
    
    /**
     * Constructor for DBox, which allows putting space between each component.
     */
    public DBox(int axis, int minSpacing, int prefSpacing, float alignmentVal)
    {
        setLayout(new DBoxLayout(axis, minSpacing, prefSpacing));
        defaultAlignment = alignmentVal;
        this.axis = axis;
    }
    
    /**
     * Add a component to this DBox, first setting the alignment to the
     * default alignment for this DBox.
     * 
     * @param c  The JComponent to add.
     */
    public void addAligned(JComponent c)
    {
        if (axis == Y_AXIS) {
            c.setAlignmentX(defaultAlignment);
        }
        else {
            c.setAlignmentY(defaultAlignment);
        }
        add(c);
    }
    
    /**
     * Sets the size bounding for an axis. When bounding is enabled, the maximum size on
     * the axis will be equal to the preferred size.
     * 
     * @param axis   The axis to set the bounding for (X_AXIS or Y_AXIS)
     * @param bounded  True to enable bounding or false to disable
     */
    public void setAxisBounded(int axis, boolean bounded)
    {
        if (axis == X_AXIS) {
            xAxisBounded = bounded;
        }
        else if (axis == Y_AXIS) {
            yAxisBounded = bounded;
        }
    }
    
    /**
     * Add a spacer to the box.
     * 
     * @param size  The size of the spacer
     * @return  The spacer component
     */
    public Component addSpacer(int size)
    {
        JComponent spacer = new JPanel();
        spacer.setBorder(null);

        if (axis == X_AXIS) {
            Dimension d = new Dimension(size, 0);
            spacer.setMaximumSize(d);
            spacer.setPreferredSize(d);
        }
        else {
            Dimension d = new Dimension(0, size);
            spacer.setMaximumSize(d);
            spacer.setPreferredSize(d);
        }
        
        Dimension min = new Dimension(0, 0);
        spacer.setMinimumSize(min);
        add(spacer);
        return spacer;
    }
    
    /* (non-Javadoc)
     * @see java.awt.Component#getMaximumSize()
     */
    @Override
    public Dimension getMaximumSize()
    {
        if (! xAxisBounded && ! yAxisBounded) {
            return super.getMaximumSize();
        }
        else {
            if (xAxisBounded && yAxisBounded) {
                return getPreferredSize();
            }
            else {
                Dimension d = super.getMaximumSize();
                Dimension p = getPreferredSize();
                if (xAxisBounded) {
                    d.width = p.width;
                }
                else {
                    d.height = p.height;
                }
                return d;
            }
        }
    }
}
