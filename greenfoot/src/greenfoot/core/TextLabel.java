/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2013  Poul Henriksen and Michael Kolling 
 
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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

/**
 * Representation for text labels appearing on the world.
 * 
 * @author Davin McCall
 */
public class TextLabel
{
    private int xpos;
    private int ypos;
    private String text;
    private Shape outline;
    private int width;
    
    /**
     * Construct a TextLabel with the given text and position.
     */
    public TextLabel(String s, int xpos, int ypos)
    {
        text = s;
        this.xpos = xpos;
        this.ypos = ypos;
        width = -1;
    }
    
    /**
     * Draw this TextLabel onto a graphics context
     * @param g   The graphics context to render to
     * @param cellsize   The world's cell size
     */
    public void draw(Graphics2D g, int cellsize)
    {
        FontMetrics metrics = g.getFontMetrics();
        
        if (outline == null) {
            getOutline(g.getFont(), g.getFontRenderContext());
            width = outline.getBounds().width;
        }
        
        // Position of base line:
        int ydraw = ypos * cellsize - metrics.getHeight() / 2 + metrics.getAscent() + cellsize / 2;
        
        int xdraw = xpos * cellsize - width / 2 + cellsize / 2;
        
        g.translate(xdraw, ydraw);
        
        g.setColor(Color.WHITE);
        g.fill(outline);
        
        g.setColor(Color.BLACK);
        g.draw(outline);
        
        g.translate(-xdraw, -ydraw);
    }
    
    private Shape getOutline(Font font, FontRenderContext frc)
    {
        if (outline == null) {
            TextLayout tl = new TextLayout(text, font, frc);
            outline = tl.getOutline(null);
        }
        return outline;
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
