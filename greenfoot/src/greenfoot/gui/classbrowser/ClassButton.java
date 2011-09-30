/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2011  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui.classbrowser;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;

import bluej.utility.Utility;

/**
 * The drawing aspect of the class buttons in the class browser
 * and the import-class dialog
 * 
 * @author neil
 */
public abstract class ClassButton extends JToggleButton implements MouseListener, Selectable
{

    private final Color classColour = new Color(245, 204, 155);
    private static final Color stripeColor = new Color(152,152,152);
    public static final Color[] shadowColours = { new Color(242, 242, 242), 
                                                      new Color(211, 211, 211),
                                                      new Color(189, 189, 189),
                                                      new Color(83, 83, 83)
                                                    };
    private static final int SHADOW = 4;
    private static final int GAP = 2;
    private static final int SELECTED_BORDER = 3;
   
    // Sees if the class is valid
    protected abstract boolean isValidClass();
    // Sees if the class is uncompiled (and thus should be shaded)
    protected abstract boolean isUncompiled();
    // Called when the class button is double-clicked
    protected abstract void doubleClick();
    // Called when a popup should be shown (i.e. when the button is right-clicked/control-clicked)
    protected abstract void maybeShowPopup(MouseEvent e);

    protected void initUI()
    {
        this.addMouseListener(this);
        this.setBorder(BorderFactory.createEmptyBorder(7, 8, 10, 11)); // top,left,bottom,right
        Font font = getFont();
        font = font.deriveFont(13.0f);
        this.setFont(font);
        // this.setFont(PrefMgr.getTargetFont());

        setContentAreaFilled(false);
        setFocusPainted(false);
    }
    
    /**
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    public void paintComponent(Graphics g)
    {
        // Sometimes there are still paint events pending when the gclass
        // has been removed. We can check for that here.
        if (isValidClass()) {
            drawBackground(g);
            super.paintComponent(g);
            
            drawShadow((Graphics2D) g);
            drawBorders((Graphics2D) g);
        }
    }

    private void drawBackground(Graphics g)
    {
        int height = getHeight() - SHADOW - GAP;
        int width = getWidth() - 4;
    
        g.setColor(classColour);
        g.fillRect(0, GAP, width, height);
        
        if(isUncompiled()) {
            g.setColor(stripeColor);
            Utility.stripeRect(g, 0, GAP, width, height, 8, 3);
    
            g.setColor(classColour);
            g.fillRect(7, GAP+7, width-14, height-14);
        }
    }

    /**
     * Draw a 'shadow' appearance under and to the right of the target.
     */
    protected void drawShadow(Graphics2D g)
    {
        int height = getHeight() - SHADOW;
        int width = getWidth() - 4;
        
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width + 4, GAP);   // blank for gap above class
        g.fillRect(0, height, 6, height + SHADOW);
        g.fillRect(width, 0, width + 3, 10);
        
        // colorchange is expensive on mac, so draworder is by color, not position
        g.setColor(shadowColours[3]);
        g.drawLine(3, height, width, height);//bottom
    
        g.setColor(shadowColours[2]);
        g.drawLine(4, height + 1, width, height + 1);//bottom
        g.drawLine(width + 1, height + 2, width + 1, 3 + GAP);//right
    
        g.setColor(shadowColours[1]);
        g.drawLine(5, height + 2, width + 1, height + 2);//bottom
        g.drawLine(width + 2, height + 3, width + 2, 4 + GAP);//right
    
        g.setColor(shadowColours[0]);
        g.drawLine(6, height + 3, width + 2, height + 3); //bottom
        g.drawLine(width + 3, height + 3, width + 3, 5 + GAP); // right
    }

    /**
     * Draw the borders of this target.
     */
    protected void drawBorders(Graphics2D g)
    {
        g.setColor(Color.BLACK);
        int thickness = isSelected() ? SELECTED_BORDER : 1;
        Utility.drawThickRect(g, 0, GAP, getWidth() - 4, getHeight() - SHADOW - GAP - 1, thickness);
    }
    
    // Override's Component's method
    public boolean isFocusable()
    {
        return false;
    }

    /**
     * Mouse-click on this class view. Chek for double-click and handle.
     */
    public void mouseClicked(MouseEvent e)
    {
        if (e.getClickCount() > 1 && ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
            doubleClick();
        }
    }

    public void mouseEntered(MouseEvent e)
    { }

    public void mouseExited(MouseEvent e)
    { }

    /**
     * The mouse was pressed on the component. Do what you have to do.
     */
    public void mousePressed(MouseEvent e)
    {
        select();
        maybeShowPopup(e);
    }

    /**
     * Selects the component after you've released the mouse.
     *
     * Copies the mousePressed behaviour, to handle Windows behaviour
     * where a right-click is not considered a mousePressed event.
     */
    public void mouseReleased(MouseEvent e)
    {
        select();
        maybeShowPopup(e);
    }

    /**
     *  Clears the UI for this ClassView
     */
    protected void clearUI()
    {
        this.removeAll();
    }
}
