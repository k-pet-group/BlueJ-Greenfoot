/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010  Michael Kolling and John Rosenberg 
 
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
package greenfoot.platforms.ide;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

/**
 * A label for the world title.
 * 
 * @author Davin McCall
 */
public class WorldLabel extends JLabel
{
    private FontMetrics metrics;
    
    /**
     * Construct a new WorldLabel instance.
     */
    public WorldLabel()
    {
        metrics = getFontMetrics(getFont());
        setOpaque(false);
        setBorder(new EmptyBorder(5, 5, 5, 5));
    }
    
    @Override
    public void paint(Graphics g)
    {
        paintComponent(g);
    }
    
    @Override
    protected void paintComponent(Graphics g)
    {
        Insets insets = getInsets();
        int x = insets.left;
        int y = insets.top;
        int width = getWidth() - x - insets.right;
        int height = getHeight() - y - insets.bottom;
        
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;

            // Use system settings for text rendering (Java 6 only)
            Toolkit tk = Toolkit.getDefaultToolkit(); 
            Map<?,?> desktopHints = (Map<?,?>) (tk.getDesktopProperty("awt.font.desktophints")); 
            if (desktopHints != null) { 
                g2d.addRenderingHints(desktopHints); 
            }
        }
        
        Color c = g.getColor();
        g.setColor(new Color(255,255,192));
        g.fillRoundRect(x, y, width, height, 6, 6);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, y, width - 1, height - 1, 6, 6);
        
        // text
        Rectangle r = metrics.getStringBounds(getText(), g).getBounds();
        int tx = x + (width - r.width) / 2;
        int ty = y + (height - r.height) / 2 + metrics.getAscent();
        g.drawString(getText(), tx, ty); 
        
        g.setColor(c);
    }
    
    @Override
    public Dimension getPreferredSize()
    {
        Rectangle r = metrics.getStringBounds(getText(), getGraphics()).getBounds();
        int prefHeight = r.height + 20; // 10 pixels either side
        int prefWidth = r.width + 30;
        
        return new Dimension(prefWidth, prefHeight);
    }

    @Override
    public Dimension getMaximumSize()
    {
        return getPreferredSize();
    }
    
    @Override
    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }
}
