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

import java.awt.*;

/**
 * General graph vertices
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 * @version $Id: Vertex.java 6163 2009-02-19 18:09:55Z polle $
 */
public abstract class Vertex extends SelectableGraphElement
{
    private int x, y; // position
    private int width, height; // size

    /**
     * Create this vertex with given specific position.
     */
    public Vertex(int x, int y, int width, int height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Set the position to the specified coordinates.
     */
    public void setPos(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    /**
     * Set the size to the specified height and width.
     */
    public void setSize(int width, int height)
    {
        this.width = (width > 0 ? width : 10);
        this.height = (height > 0 ? height : 10);
    }

    /**
     * Get this vertex's enclosing rectangle.
     */
    public Rectangle getRectangle()
    {
        return new Rectangle(x, y, width, height);
    }

    /**
     * Get this vertex's x position.
     */
    public int getX()
    {
        return this.x;
    }

    /**
     * Get this vertex's y position.
     */
    public int getY()
    {
        return this.y;
    }

    /**
     * Get this vertex's width.
     */
    public int getWidth()
    {
        return this.width;
    }

    /**
     * Get this vertex's height.
     */
    public int getHeight()
    {
        return this.height;
    }

    /**
     * The default shape for a vertex is a rectangle. Child classes can override
     * this method to define more complex shapes.
     */
    public boolean contains(int x, int y)
    {
        return (getX() <= x) && (x < getX() + getWidth()) && 
               (getY() <= y) && (y < getY() + getHeight());
    }
}