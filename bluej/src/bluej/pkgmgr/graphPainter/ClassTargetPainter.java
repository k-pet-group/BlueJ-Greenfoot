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
package bluej.pkgmgr.graphPainter;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;

import bluej.Config;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.Target;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Utility;
/**
 * Paints a ClassTarget
 * 
 * @author fisker
 * @version $Id: ClassTargetPainter.java 6164 2009-02-19 18:11:32Z polle $
 */

public class ClassTargetPainter
{
    private static final int HANDLE_SIZE = 20;
    private static final String STEREOTYPE_OPEN = "<<";
    private static final String STEREOTYPE_CLOSE = ">>";
    private static final Color textcolor = Config.getItemColour("colour.text.fg");
    private static final Color borderColor = Config.getItemColour("colour.target.border");
    private static final Color compileColor = Config.getItemColour("colour.target.bg.compiling");
    private static final Color stripeColor = Config.getItemColour("colour.target.stripes");
    private static final Image brokenImage = Config.getImageAsIcon("image.class.broken").getImage();
    private static final Font targetFont = PrefMgr.getTargetFont();

    private static final int TEXT_HEIGHT = GraphPainterStdImpl.TEXT_HEIGHT;
    private static final int TEXT_BORDER = GraphPainterStdImpl.TEXT_BORDER;
    private static final Color[] shadowColours = GraphPainterStdImpl.shadowColours;
    private static final AlphaComposite alphaComposite = GraphPainterStdImpl.alphaComposite;
    private static Composite oldComposite;

    /**
     * Construct the ClassTargetPainter
     *  
     */
    public ClassTargetPainter()
    { }

    public void paint(Graphics2D g, ClassTarget classTarget, boolean hasFocus)
    {
        g.translate(classTarget.getX(), classTarget.getY());
        
        int width = classTarget.getWidth();
        int height = classTarget.getHeight();
        
        // draw the stationary class
        drawSkeleton(g, classTarget, width, height);
        drawShadow(g, width, height);
        drawUMLStyle(g, classTarget, hasFocus, width, height);
        // drawRole(g);  // currently, roles don't draw
        g.translate(-classTarget.getX(), -classTarget.getY());
    }

    public void paintGhost(Graphics2D g, Target target, boolean hasFocus)
    {
        ClassTarget classTarget = (ClassTarget) target;
        oldComposite = g.getComposite();
        g.translate(classTarget.getGhostX(), classTarget.getGhostY());
        int width = classTarget.getGhostWidth();
        int height = classTarget.getGhostHeight();
        
        g.setComposite(alphaComposite);
        drawSkeleton(g, classTarget, width, height);
        drawUMLStyle(g, classTarget, hasFocus, width, height);
        // drawRole(g);  // currently, roles don't draw
        g.setComposite(oldComposite);
        g.translate(-classTarget.getGhostX(), -classTarget.getGhostY());
    }

    /**
     * Draw the Coloured rectangle and the borders.
     *  
     */
    private void drawSkeleton(Graphics2D g, ClassTarget classTarget, int width, int height)
    {
        g.setColor(getBackgroundColour(classTarget));
        g.fillRect(0, 0, width, height);
    }

    /**
     * Draw the stereotype, identifier name and the line beneath the identifier
     * name.
     */
    private void drawUMLStyle(Graphics2D g, ClassTarget classTarget, boolean hasFocus, int width, int height)
    {
        // get the Stereotype
        String stereotype = classTarget.getRole().getStereotypeLabel();

        g.setColor(textcolor);
        int currentTextPosY = 2;

        // draw stereotype if applicable
        if (stereotype != null) {
            String stereotypeLabel = STEREOTYPE_OPEN + stereotype + STEREOTYPE_CLOSE;
            Font stereotypeFont = targetFont.deriveFont((float) (targetFont.getSize() - 2));
            g.setFont(stereotypeFont);
            Utility.drawCentredText(g, stereotypeLabel, TEXT_BORDER, currentTextPosY, width - 2
                    * TEXT_BORDER, TEXT_HEIGHT);
            currentTextPosY += TEXT_HEIGHT - 2;
        }

        g.setFont(targetFont);

        // draw the identifiername of the class
        Utility.drawCentredText(g, classTarget.getDisplayName(), TEXT_BORDER, currentTextPosY, width
                - 2 * TEXT_BORDER, TEXT_HEIGHT);
        currentTextPosY += TEXT_HEIGHT;

        drawWarnings(g, classTarget, width, height);

        // draw line beneath the stereotype and indentifiername. The UML-style
        g.setColor(borderColor);
        g.drawLine(0, currentTextPosY, width, currentTextPosY);
        
        boolean drawSelected = classTarget.isSelected() && hasFocus;
        drawBorder(g, drawSelected, width, height);
    }

    /**
     * If the state of the class insn't normal, make it stripped. Write warning
     * if the sourcecode is missing. Display the "broken" image if the
     * sourcecode couldn't be parsed.
     */
    private void drawWarnings(Graphics2D g, ClassTarget classTarget, int width, int height)
    {

        // If the state isn't normal, draw stripes in the rectangle
        String stereotype = classTarget.getRole().getStereotypeLabel();
        if (classTarget.getState() != ClassTarget.S_NORMAL) {
            g.setColor(stripeColor);
            int divider = (stereotype == null) ? 19 : 33;
            Utility.stripeRect(g, 0, divider, width, height - divider, 8, 3);
        }

        // if sourcecode is missing. Write "(no source)" in the diagram
        if (!classTarget.hasSourceCode()) {
            g.setColor(textcolor);
            g.setFont(targetFont.deriveFont((float) (targetFont.getSize() - 2)));
            Utility.drawCentredText(g, "(no source)", TEXT_BORDER, height - 18, width
                    - 2 * TEXT_BORDER, TEXT_HEIGHT);
        }
        // if the sourcecode is invalid, display the "broken" image in diagram
        else if (!classTarget.getSourceInfo().isValid()) {
            g.drawImage(brokenImage, TEXT_BORDER, height - 22, null);
        }
    }

    /**
     * Draw the borders of this target.
     */
    private void drawBorder(Graphics2D g, boolean selected, int width, int height)
    {
        int thickness = 1; // default thickness

        if (selected) {
            thickness = 2; // thickness of borders when class is selected
            // Draw lines showing resize tag
            g.drawLine(width - HANDLE_SIZE - 2, height, width, height - HANDLE_SIZE - 2);
            g.drawLine(width - HANDLE_SIZE + 2, height, width, height - HANDLE_SIZE + 2);
        }
        Utility.drawThickRect(g, 0, 0, width, height, thickness);
    }

    /**
     * Draw a 'shadow' appearance under and to the right of the target.
     */
    private void drawShadow(Graphics2D g, int width, int height)
    {
        // colorchange is expensive on mac, so draworder is by color, not position
        g.setColor(shadowColours[3]);
        g.drawLine(3, height + 1, width, height + 1);//bottom

        g.setColor(shadowColours[2]);
        g.drawLine(4, height + 2, width, height + 2);//bottom
        g.drawLine(width + 1, height + 2, width + 1, 3);//left

        g.setColor(shadowColours[1]);
        g.drawLine(5, height + 3, width + 1, height + 3);//bottom
        g.drawLine(width + 2, height + 3, width + 2, 4);//left

        g.setColor(shadowColours[0]);
        g.drawLine(6, height + 4, width + 2, height + 4); //bottom
        g.drawLine(width + 3, height + 3, width + 3, 5); // left
    }

    /**
     * Return the background colour for this target.
     */
    private Color getBackgroundColour(ClassTarget classTarget)
    {
        if (classTarget.getState() == ClassTarget.S_COMPILING) {
            return compileColor;
        }
        else {
            return classTarget.getRole().getBackgroundColour();
        }
    }
}