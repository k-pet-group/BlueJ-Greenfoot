/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2013,2016  Michael Kolling and John Rosenberg 
 
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

import java.util.*;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.Target;
import bluej.utility.Debug;
import javafx.application.Platform;
import javafx.scene.shape.Rectangle;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The diagram's marquee (a rectangular drag area for selecting graph elements).
 * 
 * @author fisker
 * @author Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public final class Marquee
{
    private final Package graph;
    private int drag_start_x, drag_start_y;
    private Rectangle currentRect;
    private final SelectionSet selected;
    private boolean active = false;
    private final ArrayList<Target> previouslySelected = new ArrayList<>();

    /**
     * Create a marquee for a given graph.
     */
    public Marquee(Package graph, SelectionSet selection)
    {
        this.graph = graph;
        this.selected = selection;
        currentRect = new Rectangle();
        currentRect.setVisible(false);
    }

    /**
     * Start a marquee selection at point x, y.
     */
    public void start(int x, int y)
    {
        previouslySelected.clear();
        previouslySelected.addAll(selected.getSelected());
        drag_start_x = x;
        drag_start_y = y;
        currentRect.setX(x);
        currentRect.setY(y);
        currentRect.setWidth(0);
        currentRect.setHeight(0);
        currentRect.setVisible(false);
        active = true;
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
        currentRect.setX(x);
        currentRect.setY(y);
        currentRect.setWidth(w);
        currentRect.setHeight(h);
        if (w != 0 || h != 0)
        {
            currentRect.setVisible(true);
        }

        findSelectedVertices(x, y, w, h);
    }

    
    /**
     * Find, and add, all vertices that intersect the specified area.
     */
    private void findSelectedVertices(int x, int y, int w, int h)
    {
        //clear the currently selected
        selected.clear();
        selected.addAll(previouslySelected);

        //find the intersecting vertices
        for (Target v : graph.getVertices()) {
            if (v.getBoundsInEditor().intersects(x, y, w, h)) {
                selected.add(v);
            }
        }
        
        // If none of them are focused, focus one, otherwise keyboard
        // actions won't work:
        if (!selected.isEmpty() && !selected.getSelected().stream().anyMatch(Target::isFocused))
            selected.getAnyVertex().requestFocus();
    }

    /**
     * Stop a current marquee selection.
     */
    public void stop()
    {
        currentRect.setVisible(false);
        active = false;
    }

    /**
     * Tell whether this marquee is currently active.
     */
    public boolean isActive()
    {
        return active;
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