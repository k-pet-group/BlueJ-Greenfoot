/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2014  Michael Kolling and John Rosenberg 
 
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
package bluej.utility;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;

import javax.swing.JPanel;

/**
 * A small extension to JPanel that uses a gradient fill for the background
 * 
 * This should be passed in a call to setContentPane() for a JFrame to take effect --
 * coupled with setting all panels inside the frame to non-opaque
 * @author Neil Brown
 *
 */
public class GradientFillPanel extends JPanel
{
    public GradientFillPanel(LayoutManager layout)
    {
        super(layout);
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D)g;
            
            int w = getWidth();
            int h = getHeight();
            
            GradientPaint gp = new GradientPaint(
                w/4, 0, new Color(236, 236, 236),
                w*3/4, h, new Color(187, 182, 173));

            g2d.setPaint(gp);
            g2d.fillRect(0, 0, w, h);
        }
    }
}