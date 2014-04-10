/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr.graphPainter;

import java.awt.*;

import bluej.Config;
import bluej.pkgmgr.target.*;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Utility;

/**
 * Paints a packageTarget
 * 
 * @author fisker
 */
public class PackageTargetPainter
{
    private static final int TAB_HEIGHT = 12;
    private static final int HANDLE_SIZE = 20;

    private static final Color bordercolour = Color.BLACK;

    private static final int TEXT_HEIGHT = GraphPainterStdImpl.TEXT_HEIGHT;
    private static final int TEXT_BORDER = GraphPainterStdImpl.TEXT_BORDER;
    private static final AlphaComposite alphaComposite = GraphPainterStdImpl.alphaComposite;
    private static Composite oldComposite;
    private int tabWidth;

    /**
     * Construct a new PackageTargetPainter.
     */
    public PackageTargetPainter()
    {
    }

    public void paint(Graphics2D g, Target target, boolean hasFocus)
    {
        PackageTarget packageTarget = (PackageTarget) target;
        g.translate(packageTarget.getX(), packageTarget.getY());
        int width = packageTarget.getWidth();
        int height = packageTarget.getHeight();
        drawUMLStyle(g, packageTarget, hasFocus, width, height);
        g.translate(-packageTarget.getX(), -packageTarget.getY());
    }

    public void paintGhost(Graphics2D g, Target target, boolean hasFocus)
    {
        PackageTarget packageTarget = (PackageTarget) target;
        oldComposite = g.getComposite();
        g.translate(packageTarget.getGhostX(), packageTarget.getGhostY());
        int width = packageTarget.getGhostWidth();
        int height = packageTarget.getGhostHeight();

        g.setComposite(alphaComposite);
        drawUMLStyle(g, packageTarget, hasFocus, width, height);
        g.setComposite(oldComposite);
        g.translate(-packageTarget.getGhostX(), -packageTarget.getGhostY());
    }

    /**
     * Draw the package icon.
     */
    private void drawUMLStyle(Graphics2D g, PackageTarget packageTarget, boolean hasFocus, int width, int height)
    {
        tabWidth = packageTarget.getWidth() / 3;
        
        int thickness = 1;  // default
        boolean isSelected = packageTarget.isSelected() && hasFocus;
        if (isSelected)
            thickness = 2;

        Paint fill;
        if (!Config.isRaspberryPi()){
            fill = new GradientPaint(
                    width/4, 0, new Color(229, 183, 173),
                    width*3/4, height, new Color(207, 130, 117));
        }else{
            fill = new Color(253, 157, 145);
        }
        
        
        if (!Config.isRaspberryPi()) drawShadow(g, packageTarget, tabWidth, width, height);

        g.setPaint(fill);
        g.fillRoundRect(0, 0, tabWidth, TAB_HEIGHT+5, 5, 5);
        g.setColor(bordercolour);
        Utility.drawThickRoundRect(g, 0, 0, tabWidth, TAB_HEIGHT+5, 5, thickness);
        // The main rectangles draw on the top of the previous small tab rectangles:
        g.setPaint(fill);
        g.fillRect(0, TAB_HEIGHT, width, height - TAB_HEIGHT);
        g.setColor(bordercolour);
        for (int i = 0; i < thickness; i++) {
            // Draws the rect: (i, TAB_HEIGHT+i, width - 2 * i, height - TAB_HEIGHT - 2 * i)
            // But misses out the top portion on the left for tabWidth
            g.drawLine(tabWidth - (isSelected ? 1 : 0), TAB_HEIGHT + i, width - i, TAB_HEIGHT + i); //top
            g.drawLine(i, height - i, width - i, height - i); //bottom
            g.drawLine(i, TAB_HEIGHT, i, height - i); //left
            g.drawLine(width - i, TAB_HEIGHT + i, width - i, height - i); //right
        }

        g.setFont(getFont(packageTarget));
        Utility.drawCentredText(g, packageTarget.getDisplayName(), TEXT_BORDER, TEXT_BORDER + TAB_HEIGHT + 10,
                width - 2 * TEXT_BORDER, TEXT_HEIGHT);       

        if (isSelected) {
            // Draw lines showing resize tag
            g.drawLine(width - HANDLE_SIZE - 2, height, width, height - HANDLE_SIZE - 2);
            g.drawLine(width - HANDLE_SIZE + 2, height, width, height - HANDLE_SIZE + 2);
        }
    }

    /**
     * Draw the shadow.
     */
    private void drawShadow(Graphics2D g, PackageTarget packageTarget, int tabWidth, int width, int height)
    {
     // A uniform tail-off would have equal values for each,
        // as they all get drawn on top of each other:
        final int shadowAlphas[] = {20, 15, 10, 5, 5};
        for (int i = 0;i < 5;i++) {
            g.setColor(new Color(0, 0, 0, shadowAlphas[i]));
            
            // Tab:
            g.fillRoundRect(2 - i, 4 - i, tabWidth + (2*i) - 1, (TAB_HEIGHT + 5) + (2*i) - 1, 8, 8);
            // Main:
            g.fillRoundRect(2 - i, TAB_HEIGHT + 4 - i, width + (2*i) - 1, height - TAB_HEIGHT + (2*i) - 1, 8, 8);
        }
    }

    private Font getFont(PackageTarget packageTarget)
    {
        return PrefMgr.getTargetFont();
    }
}
