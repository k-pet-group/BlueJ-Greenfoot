/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.SwingUtilities;

/**
 * SelectionSet holds a set of selected graph elements. By inserting an
 * element into this set, it is automatically set to selected.
 * 
 * @author fisker
 * @author Michael Kolling
 * 
 * @version $Id: SelectionSet.java 6163 2009-02-19 18:09:55Z polle $
 */
public final class SelectionSet
{
    private Set elements = new HashSet();

    /**
     * 
     * @param graphEditor
     */
    public SelectionSet()
    {}

    /**
     * Add an unselected selectable graphElement to the set and
     * set it's 'selected' flag.
     * 
     * @param element  The element to add
     */
    public void add(SelectableGraphElement element)
    {
        if (!element.isSelected()) {
            element.setSelected(true);
            elements.add(element);
        }
    }
    
    /**
     * Add all the elements from another selection set to this one.
     */
    public void addAll(SelectionSet newSet)
    {
        elements.addAll(newSet.elements);
    }

    /**
     * Remove the graphElement and set it's 'selected' flag false.
     * 
     * @param graphElement
     */
    public void remove(SelectableGraphElement element)
    {
        if (element != null) {
            element.setSelected(false);
        }
        elements.remove(element);
    }

    /**
     * Remove all the graphElements from the list. Set each removed grahpElement
     * 'selected' flag to false. Does NOT selfuse remove method.
     */
    public void clear()
    {
        for (Iterator i = elements.iterator(); i.hasNext();) {
            SelectableGraphElement element = (SelectableGraphElement) i.next();
            element.setSelected(false);
        }
        elements.clear();
    }

    /**
     * Perform a double click on the selection.
     * 
     * @param evt  The mouse event that originated this double click.
     */
    public void doubleClick(MouseEvent evt)
    {
        final MouseEvent event = evt;
        for (Iterator i = elements.iterator(); i.hasNext();) {
            final GraphElement element = (GraphElement) i.next();
            Runnable sendClick = new Runnable() {
                public void run() {
                    element.doubleClick(event);
                }
            };

            SwingUtilities.invokeLater(sendClick);
        }        
    }
    
    /**
     * 
     */
    public void move(int deltaX, int deltaY)
    {
        for (Iterator i = elements.iterator(); i.hasNext();) {
            GraphElement element = (GraphElement) i.next();
            if(element instanceof Moveable) {
                Moveable target = (Moveable) element;
                if (target.isMoveable()) {
                    target.setDragging(true);
                    Point delta = restrictDelta(deltaX, deltaY);
                    target.setGhostPosition(delta.x, delta.y);
                }
            }
        }
    }

    /**
     * Restrict the delta so that no target moves out of the screen.
     */
    private Point restrictDelta(int deltaX, int deltaY)
    {
        for (Iterator i = elements.iterator(); i.hasNext();) {
            GraphElement element = (GraphElement) i.next();

            if(element instanceof Moveable) {
                Moveable target = (Moveable) element;

                if(target.getX() + deltaX < 0)
                    deltaX = -target.getX();
                if(target.getY() + deltaY < 0)
                    deltaY = -target.getY();
            }
        }
        return new Point(deltaX, deltaY);
    }


    /**
     * A move gesture (either move or resize) has stopped. Inform all elements
     * in this selection that they shoudl react.
     */
    public void moveStopped()
    {
        for (Iterator i = elements.iterator(); i.hasNext();) {
            GraphElement element = (GraphElement) i.next();
            if(element instanceof Moveable) {
                Moveable moveable = (Moveable) element;
                moveable.setPositionToGhost();
            }
        }        
    }
    

    /**
     * A resize operation has initiated (or continued). Inform al elements
     * that they should react to the resize.
     * 
     * @param deltaX  The current x offset from the start of the resize.
     * @param deltaY  The current y offset from the start of the resize.
     */
    public void resize(int deltaX, int deltaY)
    {
        for (Iterator i = elements.iterator(); i.hasNext();) {
            GraphElement element = (GraphElement) i.next();
            if(element instanceof Moveable) {
                Moveable target = (Moveable) element;
                if (target.isResizable()) {
                    target.setDragging(true);
                    target.setGhostSize(deltaX, deltaY);
                }
            }
        }
    }

    /**
     * Tell whether the selection is empty.
     * 
     * @return  true, if the selection is empty.
     */
    public boolean isEmpty()
    {
        return elements.isEmpty();
    }
    
    /**
     * Tell whether we have a single selection (only one element selected).
     * 
     * @return  true, is exactly one element is selected.
     */
    public boolean isSingle()
    {
        return elements.size() == 1;
    }
    
    /**
     * Change the selection to contain only the specified element.
     * 
     * @param element  The single element to hold in the selection. 
     */
    public void selectOnly(SelectableGraphElement element)
    {
        clear();
        add(element);
    }
    
    /** 
     * Return a random vertex from this selection.
     * @return  An vertex, or null, if none exists.
     */
    public Vertex getAnyVertex()
    {
        for (Iterator i = elements.iterator(); i.hasNext();) {
            GraphElement element = (GraphElement) i.next();
            if(element instanceof Vertex)
                return (Vertex) element;
        }
        return null;
    }

    
    /** 
     * Return a random vertex from this selection.
     * @return  An vertex, or null, if none exists.
     */
    public Edge getAnyEdge()
    {
        for (Iterator i = elements.iterator(); i.hasNext();) {
            GraphElement element = (GraphElement) i.next();
            if(element instanceof Edge)
                return (Edge) element;
        }
        return null;
    }

    
    /**
     * Return the single selected element from this selection. The selection can be 
     * forced to become a single selection by passing 'true' to forceSingle. If the selection
     * is not single, and not forced to be single, null will be returned.
     * 
     * @param forceSingle  If true, reduce this selection to a single selection by selecting
     *                     a random element before proceeding.
     * @return The single element, if this selection is single, or null if the selection
     *         is empty or not single.
     */
    public SelectableGraphElement getSingleElement(boolean forceSingle)
    {
        if(elements.isEmpty())
            return null;

        if(isSingle())
            return (SelectableGraphElement) elements.iterator().next();
        
        if(forceSingle) {
            SelectableGraphElement tmp = (SelectableGraphElement) elements.iterator().next();
            selectOnly(tmp);
            return tmp;
        }
        else {   // not a single selection
            return null;
        }
    }
    
    /**
     * Return an iterator over the selected elements.
     * 
     * @return  The iterator for the selection elements.
     */
    public Iterator iterator()
    {
        return elements.iterator();
    }

    /**
     * Get the number of graphElements in this graphElementManager
     * 
     * @return the number of elements
     */
    public int getSize()
    {
        return elements.size();
    }
}