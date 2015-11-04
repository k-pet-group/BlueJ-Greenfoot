/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014  Michael Kolling and John Rosenberg 
 
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

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;

/**
 * A label which supports multiple lines of text, and which wraps text if
 * it is too long (longer than the label width). An arbitrary wrapping width
 * can also be specified.
 * 
 * @author Davin McCall
 */
public class MultiWrapLabel extends JComponent
{
    private static String lineSep = System.getProperty("line.separator");
    
    private String text;
    private int wrapWidth;
    
    /**
     * Create an empty MultiWrapLabel.
     */
    public MultiWrapLabel()
    {
        text = "";
        wrapWidth = -1;
    }
    
    /**
     * Create a MultiWrapLabel to display the given text.
     */
    public MultiWrapLabel(String text)
    {
        this.text = text;
        wrapWidth = -1;
    }
    
    /**
     * Set the text which this MultiWrapLabel should display.
     */
    public void setText(String text)
    {
        this.text = text;
        invalidate();
    }
    
    /**
     * Set the desired wrap width for this MultiWrapLabel. This also determines
     * the preferred width of the label. Text will be wrapped at the given width,
     * unless the width of the label is less than the given width, in which case
     * text will be wrapped at the width of the label instead.
     * 
     * Specify -1 to indicate there is no preferred wrap width. In this case the
     * preferred width of the label will be the length of the longest line of text
     * in the label, and text will always wrap at the width of the label.
     * 
     * @param wrapWidth  The desired wrapping width, or -1 for no specific width.
     */
    public void setWrapWidth(int wrapWidth)
    {
        this.wrapWidth = wrapWidth;
    }
    
    /* (non-Javadoc)
     * @see java.awt.Component#getMaximumSize()
     */
    @Override
    public Dimension getMaximumSize()
    {
        if (isMaximumSizeSet()) {
            return super.getMaximumSize();
        }
        else {
            return getPreferredSize();
        }
    }
    
    /* (non-Javadoc)
     * @see java.awt.Component#getMinimumSize()
     */
    @Override
    public Dimension getMinimumSize()
    {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        else {
            return getPreferredSize();
        }
    }
    
    /* (non-Javadoc)
     * @see java.awt.Component#getPreferredSize()
     */
    @Override
    public Dimension getPreferredSize()
    {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        else if (wrapWidth != -1) {
            return getPreferredSizeWrapped();
        }
        else {
            // The preferred size is the width of the longest line, and
            // the summed height of all lines.
            double height = 0f;
            double width = 0f;
            
            FontMetrics metrics = getFontMetrics(getFont());
            int npos = 0;
            while (npos < text.length()) {
                int end = text.indexOf(lineSep, npos);
                if (end == -1) {
                    end = text.length();
                }
                String line = text.substring(npos, end);
                
                Rectangle2D lineRect = metrics.getStringBounds(line, getGraphics());
                if (lineRect.getWidth() > width) {
                    width = lineRect.getWidth();
                }
                height += lineRect.getHeight();
                npos = end + lineSep.length();
            }
            
            return new Dimension((int) width + 1, (int) height);
        }
    }
    
    /**
     * Get the preferred size, given that a particular wrapping width is desired.
     */
    private Dimension getPreferredSizeWrapped()
    {
        Graphics ng = getGraphics();
        
        int myWidth = wrapWidth;
        
        FontMetrics metrics = getFontMetrics(getFont());
        int npos = 0;
        int currentY = 0;
        while (npos < text.length()) {
            int end = text.indexOf(lineSep, npos);
            if (end == -1) {
                end = text.length();
            }
            String line = text.substring(npos, end);
            
            Rectangle2D lineRect = metrics.getStringBounds(line, ng);
            int lineAmount;
            int lineHeight = (int) lineRect.getHeight();
            if (lineRect.getWidth() > myWidth) {
                int lbound = 0;
                int hbound = line.length();
                int lboundw = 0;
                int hboundw = (int) lineRect.getWidth();
                
                // Perform an intelligent binary search to find how much of the line
                // can actually fit in the current width
                while (hbound - lbound > 1) {
                    int middle = lbound + (hbound - lbound) * (hboundw - lboundw) / (myWidth - lboundw);
                    if (middle <= lbound) {
                        middle = lbound + 1;
                    }
                    if (middle >= hbound) {
                        middle = hbound - 1;
                    }
                    
                    lineRect = metrics.getStringBounds(line, 0, middle, ng);
                    int middlew = (int) lineRect.getWidth();
                    if (middlew > myWidth) {
                        hbound = middle;
                        hboundw = middlew;
                    }
                    else {
                        lbound = middle;
                        lboundw = middlew;
                        lineHeight = (int) lineRect.getHeight();
                    }
                }

                lineAmount = lbound;

                // Wrap words at word boundaries...
                int priorSpace = line.indexOf(' ', 1);
                while (priorSpace != -1 && priorSpace <= lbound) {
                    lineAmount = priorSpace - 1;
                    priorSpace = line.indexOf(' ', priorSpace + 1);
                }
                
                if (lineAmount <= 0) {
                    lineAmount = 1;
                }
            }
            else {
                lineAmount = line.length();
            }
            
            currentY += lineHeight;
            if (lineAmount < line.length()) {
                npos += lineAmount;
                if (text.charAt(npos) == ' ') {
                    npos++;
                }
            }
            else {
                npos = end + lineSep.length();
            }
        }
        
        return new Dimension(wrapWidth, currentY);
    }
    
    /* (non-Javadoc)
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        Graphics ng = g.create();
        ng.setPaintMode();
        
        int myWidth = getWidth();
        
        if (isOpaque()) {
            ng.setColor(getBackground());
            ng.fillRect(0, 0, myWidth, getHeight());
        }

        if (wrapWidth != -1 && wrapWidth < myWidth) {
            myWidth = wrapWidth;
        }
        
        ng.setFont(getFont());
        FontMetrics metrics = getFontMetrics(getFont());
        int npos = 0;
        int currentX = 0;
        int currentY = 0;
        while (npos < text.length()) {
            int end = text.indexOf(lineSep, npos);
            if (end == -1) {
                end = text.length();
            }
            String line = text.substring(npos, end);
            
            Rectangle2D lineRect = metrics.getStringBounds(line, ng);
            int lineAmount;
            int lineHeight = (int) lineRect.getHeight();
            if (lineRect.getWidth() > myWidth) {
                int lbound = 0;
                int hbound = line.length();
                int lboundw = 0;
                int hboundw = (int) lineRect.getWidth();
                
                // Perform an intelligent binary search to find how much of the line
                // can actually fit in the current width
                while (hbound - lbound > 1) {
                    int middle = lbound + (hbound - lbound) * (hboundw - lboundw) / (myWidth - lboundw);
                    if (middle <= lbound) {
                        middle = lbound + 1;
                    }
                    if (middle >= hbound) {
                        middle = hbound - 1;
                    }
                    
                    lineRect = metrics.getStringBounds(line, 0, middle, ng);
                    int middlew = (int) lineRect.getWidth();
                    if (middlew > myWidth) {
                        hbound = middle;
                        hboundw = middlew;
                    }
                    else {
                        lbound = middle;
                        lboundw = middlew;
                        lineHeight = (int) lineRect.getHeight();
                    }
                }
                
                lineAmount = lbound;
                
                // Wrap words at word boundaries...
                int priorSpace = line.indexOf(' ', 1);
                while (priorSpace != -1 && priorSpace <= lbound) {
                    lineAmount = priorSpace;
                    priorSpace = line.indexOf(' ', priorSpace + 1);
                }
                
                if (lineAmount == 0) {
                    lineAmount = 1;
                }
            }
            else {
                lineAmount = line.length();
            }
            
            float ascent = metrics.getLineMetrics(line, 0, lineAmount, ng).getAscent();
            ng.drawString(line.substring(0, lineAmount), currentX, (int)(currentY + ascent));
            currentY += lineHeight;
            if (lineAmount < line.length()) {
                npos += lineAmount;
                if (text.charAt(npos) == ' ') {
                    npos++;
                }
            }
            else {
                npos = end + lineSep.length();
            }
        }
        
    }
}
