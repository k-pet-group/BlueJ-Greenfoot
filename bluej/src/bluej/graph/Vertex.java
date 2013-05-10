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
import java.awt.event.MouseEvent;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import javax.swing.JComponent.AccessibleJComponent;

/**
 * General graph vertices
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 */
public abstract class Vertex implements SelectableGraphElement
{
    private final JComponent component;
    
    public Vertex(int x, int y, int width, int height)
    {
        component = new VertexJComponent();
        component.setFocusable(true);
        component.setBounds(x, y, width, height);
        component.setVisible(true);
    }
    
    
    public void setPos(int x, int y)
    {
        component.setLocation(x, y);
    }
            
    
    /**
     * The default shape for a vertex is a rectangle. Child classes can override
     * this method to define more complex shapes.
     */
    public boolean contains(int x, int y)
    {
        return (component.getX() <= x) && (x < component.getX() + component.getWidth()) && 
               (component.getY() <= y) && (y < component.getY() + component.getHeight());
    }


    @Override
    public void remove()
    {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void doubleClick(MouseEvent evt)
    {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void popupMenu(int x, int y, GraphEditor graphEditor)
    {
        // TODO Auto-generated method stub
        
    }


    @Override
    public String getTooltipText()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void setSelected(boolean selected)
    {
        // TODO Auto-generated method stub
        
    }


    @Override
    public boolean isSelected()
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isHandle(int x, int y)
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isResizable()
    {
        // TODO Auto-generated method stub
        return false;
    }
    
    /**
     * Get this vertex's x position.
     */
    public int getX()
    {
        return component.getX();
    }

    /**
     * Get this vertex's y position.
     */
    public int getY()
    {
        return component.getY();
    }

    /**
     * Get this vertex's width.
     */
    public int getWidth()
    {
        return component.getWidth();
    }

    /**
     * Get this vertex's height.
     */
    public int getHeight()
    {
        return component.getHeight();
    }


    public boolean isVisible()
    {
        return component.isVisible();
    }


    public void setVisible(boolean aFlag)
    {
        component.setVisible(aFlag);
    }


    public void setSize(int width, int height)
    {
        component.setSize(width, height);
    }


    public JComponent getComponent()
    {
        return component;
    }


    public Rectangle getBounds()
    {
        return component.getBounds();
    }
    
    /**
     * Gets the display name of the vertex
     */
    protected abstract String getDisplayName();
    
    private class VertexJComponent extends JComponent implements Accessible
    {
        public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleJComponent() {
                    @Override
                    public String getAccessibleName() {
                        return Vertex.this.getDisplayName();
                    }

                    // If we leave the default role, NVDA ignores this component.
                    // List item works, and seemed like an okay fit
                    @Override
                    public AccessibleRole getAccessibleRole() {
                        return AccessibleRole.LIST_ITEM;
                    }                
                    
                };
            }
            return accessibleContext;
        }
    };
    
    public void singleSelected()
    {
        if (!component.hasFocus())
        {
            component.requestFocusInWindow();
        }
    }
}