/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Label used for the SplashWindow for greenfoot.
 * 
 * @author Poul Henriksen
 */
public class GreenfootLabel extends SplashLabel
{
    public GreenfootLabel()
    {
        super("greenfootsplash.jpg");
    }

    public void paintComponent(Graphics g)
    {
        BufferedImage image = getImage();
        g.drawImage(image, 0, 0, null);
        g.setColor(new Color(164, 198, 102));
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D)g;
            RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.addRenderingHints(hints);
        }
        g.drawString("Version " + Boot.GREENFOOT_VERSION, 50, 192);
    }
}
