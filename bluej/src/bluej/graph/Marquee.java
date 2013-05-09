/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2013  Michael Kolling and John Rosenberg 
 
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
package bluej.graph;

import java.awt.*;
import java.util.*;

/**
 * The diagram's marquee (a rectangular drag area for selecting graph elements).
 * 
 * @author fisker
 * @author Michael Kolling
 */
public final class Marquee
{
    private Graph graph;
    private int drag_start_x, drag_start_y;
    private Rectangle currentRect;
    private SelectionSet selected = null;

    /**
     * Create a marquee for a given graph.
     */
    public Marquee(Graph graph)
    {
        this.graph = graph;
    }

    /**
     * Start a marquee selection at point x, y.
     */
    public void start(int x, int y)
    {
        drag_start_x = x;
        drag_start_y = y;
        selected = new SelectionSet();
    }

    /**
     * Place the marquee from its starting point to the coordinate (drag_x,
     * drag_y). The marquee must have been started before this method is called.
     * 
     * @param drag_x  The x coordinate of the current drag position 
     * @param drag_y  The y coordinate of the current drag position 
     * @return  The set of graph elements selected by this marquee
     */
    public void move(int drag_x, int drag_y)
    {
        int x = drag_start_x;
        int y = drag_start_y;
        int w = drag_x - drag_start_x;
        int h = drag_y - drag_start_y;
        //Rectangle can't handle negative numbers, modify coordinates
        if (w < 0)
            x = x + w;
        if (h < 0)
            y = y + h;
        w = Math.abs(w);
        h = Math.abs(h);
        currentRect = new Rectangle(x, y, w, h);

        findSelectedVertices(x, y, w, h);
    }

    
    /**
     * Find, and add, all vertices that intersect the specified area.
     */
    private void findSelectedVertices(int x, int y, int w, int h)
    {
        //clear the currently selected
        selected.clear();

        //find the intersecting vertices
        for (Iterator<? extends Vertex> it = graph.getVertices(); it.hasNext();) {
            Vertex v = it.next();
            if (v.getBounds().intersects(x, y, w, h)) {
                selected.add(v);
            }
        }
    }

    /**
     * Stop a current marquee selection.
     */
    public SelectionSet stop()
    {
        currentRect = null;
        SelectionSet tmp = selected;
        selected = null;
        return tmp;
    }

    /**
     * Tell whether this marquee is currently active.
     */
    public boolean isActive()
    {
        return selected != null;
    }
    
    /**
     * Return the currently visible rectangle of this marquee.
     * If the marquee is not currently drawn, return null.
     * 
     * @return The marquee's rectangle, or null if not visible.
     */
    public Rectangle getRectangle()
    {
        return currentRect;
    }
}