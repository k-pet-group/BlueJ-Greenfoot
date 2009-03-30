/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui.export;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.border.LineBorder;

/*
 * Custom Border class to draw just the left and right sides of a lin border.
 *
 * @author Michael Kolling
 * @version $Id: LeftRightBorder.java 6216 2009-03-30 13:41:07Z polle $
 */

public class LeftRightBorder extends LineBorder
{
    
    /**
     * Create a left-right border with width 1 and the given color.
     */
    public LeftRightBorder(Color col)
    {
        super(col);
    }

    /**
     * Paints the border only on the left and right sides.
     */
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Color oldColor = g.getColor();

        g.setColor(getLineColor());
        g.drawLine(x, y, x, height-1);
        g.drawLine(width-1, y, width-1, height-1);
        g.setColor(oldColor);
    }
}
