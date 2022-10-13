/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2013,2014  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.core;

import greenfoot.util.GraphicsUtilities;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Representation for text labels appearing on the world.
 * 
 * @author Davin McCall
 */
public class TextLabel
{
    private final int xpos;
    private final int ypos;
    private final String text;
    private final String[] lines;
    private GraphicsUtilities.MultiLineStringDimensions dimensions;
    
    /**
     * Construct a TextLabel with the given text and position.
     */
    public TextLabel(String s, int xpos, int ypos)
    {
        text = s;
        lines = GraphicsUtilities.splitLines(text);
        this.xpos = xpos;
        this.ypos = ypos;
    }
    
    /**
     * Draw this TextLabel onto a graphics context
     * @param g   The graphics context to render to
     * @param cellsize   The world's cell size
     */
    public void draw(Graphics2D g, int cellsize)
    {
        if (dimensions == null) {
            dimensions = GraphicsUtilities.getMultiLineStringDimensions(lines, Font.BOLD, 25.0);
        }
        
        // Position of base line:
        int ydraw = ypos * cellsize - dimensions.getHeight() / 2 + cellsize / 2;
        
        int xdraw = xpos * cellsize - dimensions.getWidth() / 2 + cellsize / 2;
        
        g.translate(xdraw, ydraw);
        
        GraphicsUtilities.drawOutlinedText(g, dimensions, Color.WHITE, Color.BLACK);
        
        g.translate(-xdraw, -ydraw);
    }
    
    /**
     * Get the X position of this label.
     */
    public int getX()
    {
        return xpos;
    }
    
    /**
     * Get the Y position of this label.
     */
    public int getY()
    {
        return ypos;
    }
    
    /**
     * Get the text of this label.
     */
    public String getText()
    {
        return text;
    }
}
