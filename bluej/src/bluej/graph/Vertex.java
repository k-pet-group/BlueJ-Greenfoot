/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013  Michael Kolling and John Rosenberg 
 
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

import javax.swing.JComponent;

/**
 * General graph vertices
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 */
public abstract class Vertex extends JComponent implements SelectableGraphElement
{
    public Vertex(int x, int y, int width, int height)
    {
        setBounds(x, y, width, height);
    }
    
    
    public void setPos(int x, int y)
    {
        setLocation(x, y);
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