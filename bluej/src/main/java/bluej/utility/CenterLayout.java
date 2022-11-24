/*
 This file is part of the BlueJ program. 
 Copyright (C) 2005-2009,2011,2013  Poul Henriksen and Michael Kolling 
 
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
import java.awt.LayoutManager;

/**
 * This is a layout class for a container that has exactly one child component.
 * The layout places that child in the centre of the container.
 *
 * @author mik
 * @version 0.2
 */
public class CenterLayout implements LayoutManager
{
    public CenterLayout()
    {
    }
    
    @Override
    public void layoutContainer(Container target)
    {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int maxwidth = target.getWidth() - (insets.left + insets.right);
            int maxheight = target.getHeight() - (insets.top + insets.bottom);

            Component m = target.getComponent(0);
            if (m.isVisible()) {
                Dimension d = m.getPreferredSize();
                d.width = Math.min(d.width, maxwidth);
                d.height = Math.min(d.height, maxheight);
                m.setSize(d);

                int hspace = maxwidth - d.width;
                int xpos = insets.left + (hspace / 2);

                int vspace = maxheight - d.height;
                int ypos = insets.top + (vspace / 2);

                m.setLocation(xpos, ypos);
            }
        }
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
    public Dimension preferredLayoutSize(Container parent)
    {
        Dimension d;
        if (parent.getComponentCount() > 0) {
            Component m = parent.getComponent(0);
            d = m.getPreferredSize();
        }
        else {
            d = new Dimension(0,0);
        }
        Insets insets = parent.getInsets();
        d.height += insets.top + insets.bottom;
        d.width += insets.left + insets.right;
        return d;
    }

    @Override
    public Dimension minimumLayoutSize(Container parent)
    {
        Dimension d;
        if (parent.getComponentCount() > 0) {
            Component m = parent.getComponent(0);
            d = m.getMinimumSize();
        }
        else {
            d = new Dimension(0,0);
        }
        Insets insets = parent.getInsets();
        d.height += insets.top + insets.bottom;
        d.width += insets.left + insets.right;
        return d;
    }
}
