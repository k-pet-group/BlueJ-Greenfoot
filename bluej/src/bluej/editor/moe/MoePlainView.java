/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011,2012  Michael Kolling and John Rosenberg 

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
package bluej.editor.moe;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.PlainDocument;
import javax.swing.text.Position.Bias;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * A better, less buggy version of PlainView, with additional support for a left margin appearing
 * before the text. This was written with reference to PlainView and emulates it to some degree.
 * 
 * @author Davin McCall
 */
public class MoePlainView extends View
{
    private Font currentFont;      // current font
    protected FontMetrics metrics; // Metrics of the current font
    private Element longestLine;   // The longest known line (or null if not currently known)
    protected int tabSize;         // The current tab width (pixels)
    
    private Segment segment = new Segment();
    
    private int leftMargin;    // The left margin (before the text)
    
    /**
     * Constructs a BetterPlainView for the specified element.
     */
    public MoePlainView(Element elem)
    {
        super(elem);
    }
    
    /**
     * Constructs a BetterPlainView for the specified element, and with the specified left margin.
     */
    public MoePlainView(Element elem, int leftMargin)
    {
        super(elem);
        this.leftMargin = leftMargin;
    }

    @Override
    public float getPreferredSpan(int axis)
    {
        checkMetrics();
        switch (axis) {
        case View.X_AXIS:
            return getLineWidth(getLongestLine()) + leftMargin;
        case View.Y_AXIS:
            return getElement().getElementCount() * metrics.getHeight();
        default:
            return 0;
        }
    }
    
    static protected class MoeTabExpander implements TabExpander
    {
        private int tabSize;
        private int leftMargin;
        
        public MoeTabExpander(int tabSize, int leftMargin)
        {
            this.tabSize = tabSize;
            this.leftMargin = leftMargin;
        }
        
        @Override
        public float nextTabStop(float x, int tabOffset)
        {
            if (tabSize == 0) {
                return x;
            }
            int ntabs = (((int) x) - leftMargin) / tabSize;
            return leftMargin + 0 + ((ntabs + 1) * tabSize);
        }
    }
    
    @Override
    public Shape modelToView(int pos, final Shape a, Bias b)
            throws BadLocationException
    {
        checkMetrics();
        Element map = getElement();
        int lineIndex = map.getElementIndex(pos);
        Element line = map.getElement(lineIndex);
        
        // Get the text in the line, up to the given point
        Segment s = segment;
        Document doc = getDocument();
        doc.getText(line.getStartOffset(), pos - line.getStartOffset(), s);
        
        TabExpander tabExpander = new MoeTabExpander(tabSize, leftMargin);
        
        // Note that PlainView always returns a rectangle of width 1, at the start of the
        // character position, which is what the highlight painters expect; So, even though it seems
        // more correct to return a rectangle containing the entire character, we also just return
        // a rectangle of width 1.
        int tpos = Utilities.getTabbedTextWidth(s, metrics, leftMargin, tabExpander, line.getStartOffset());
        tpos += leftMargin;
        Rectangle aBounds = a.getBounds();
        return new Rectangle(aBounds.x + tpos, aBounds.y + lineIndex * metrics.getHeight(), 1, metrics.getHeight());
    }
        
    @Override
    public int viewToModel(float x, float y, Shape a, Bias[] biasReturn)
    {
        checkMetrics();
        Rectangle aBounds = a.getBounds();
        
        if (y < aBounds.y) {
            biasReturn[0] = Bias.Forward;
            return 0;
        }
        else if (y > aBounds.height + aBounds.y) {
            biasReturn[0] = Bias.Backward;
            return getDocument().getLength();
        }
        
        // Otherwise, the y-point represents a line:
        int lindex = ((int) y - aBounds.y) / metrics.getHeight();
        if (lindex >= getElement().getElementCount()) {
            biasReturn[0] = Bias.Backward;
            return getDocument().getLength();
        }
        else if (lindex < 0) {
            biasReturn[0] = Bias.Forward;
            return 0;
        }
        
        Element line = getElement().getElement(lindex);
        
        if (x <= aBounds.x) {
            biasReturn[0] = Bias.Forward;
            return line.getStartOffset();
        }
        else if (x > aBounds.x + aBounds.width) {
            biasReturn[0] = Bias.Backward;
            return line.getEndOffset() - 1;
        }
        
        Segment s = segment;
        int lineStart = line.getStartOffset();
        try {
            getDocument().getText(lineStart, line.getEndOffset() - lineStart - 1, s);
        }
        catch (BadLocationException ble) {
            // can't happen...??
            throw new RuntimeException(ble);
        }
        
        TabExpander tx = new MoeTabExpander(tabSize, leftMargin);
        int offset = Utilities.getTabbedTextOffset(s, metrics, leftMargin, (int)x - aBounds.x, tx, lineStart);
        
        biasReturn[0] = Bias.Forward;
        return Math.min(lineStart + offset, getDocument().getLength());
    }
    
    @Override
    public void paint(Graphics g, Shape allocation)
    {
        checkMetrics();
        
        // Paint the text lines, 
        // and the highlights.
        
        JTextComponent host = (JTextComponent) getContainer();
        Highlighter h = host.getHighlighter();
        g.setFont(host.getFont());
        
        Rectangle clip = g.getClipBounds();
        Rectangle abounds = allocation.getBounds();
        
        int topLine = (clip.y - abounds.y) / metrics.getHeight();
        int bottomLine = (clip.y + clip.height - abounds.y) / metrics.getHeight();
        int maxLine = getElement().getElementCount() - 1;
        
        LayeredHighlighter lh = (h instanceof LayeredHighlighter) ? (LayeredHighlighter) h : null;
        
        if (topLine > maxLine || bottomLine < 0) {
            return;
        }
        
        bottomLine = Math.min(bottomLine, maxLine);
        topLine = Math.max(topLine, 0);
        
        int mheight = metrics.getHeight();
        int ypos = abounds.y + topLine * mheight;
        int textBase = metrics.getAscent();
        
        g.setColor(getTextColor());
        g.setFont(host.getFont());
        for (int i = topLine; i <= bottomLine; i++) {
            Element line = getElement().getElement(i);
            if (lh != null) {
                // Set up a clip region for just the current line. Stops MoeHighlighter from painting the
                // highlights over other lines.
                Rectangle lineClip = new Rectangle(abounds.x, ypos, abounds.width, mheight);
                Rectangle.intersect(lineClip, clip, lineClip);
                g.setClip(lineClip);
                lh.paintLayeredHighlights(g, line.getStartOffset(), line.getEndOffset() - 1, allocation, host, this);
            }
            drawLine(i, g, abounds.x + leftMargin, ypos + textBase);
            ypos += mheight;
        }
        g.setClip(clip); // restore original clip bounds
    }
    
    /**
     * Get the colour for drawing text.
     */
    protected Color getTextColor()
    {
        JTextComponent host = (JTextComponent) getContainer();
        return (host.isEnabled()) ? host.getForeground() : host.getDisabledTextColor();
    }
    
    /**
     * Draw a line of text.
     * @param lineIndex  The line number
     * @param g          The graphics context on which to draw
     * @param x          The x-position
     * @param y          The y-position (of the text baseline)
     */
    protected void drawLine(int lineIndex, Graphics g, int x, int y)
    {
        try {
            Element line = getElement().getElement(lineIndex);
            getDocument().getText(line.getStartOffset(), line.getEndOffset() - line.getStartOffset(), segment);
            TabExpander tx = new MoeTabExpander(tabSize, x);
            Utilities.drawTabbedText(segment, x, y, g, tx, line.getStartOffset());
        }
        catch (BadLocationException ble) {
            throw new RuntimeException(ble);
        }
    }
    
    /**
     * Make sure font metrics information cached by the view is up-to-date.
     */
    protected void checkMetrics()
    {
        Component host = getContainer();
        Font f = host.getFont();
        if (currentFont != f) {
            // The font changed, we need to recalculate the
            // longest line.
            currentFont = f;
            metrics = getContainer().getFontMetrics(currentFont);
            longestLine = null;
            tabSize = getTabSize() * metrics.charWidth('m');
        }
    }
    
    /**
     * Get the width of the given line, not counting margins.
     */
    protected int getLineWidth(Element line)
    {
        try {
            getDocument().getText(line.getStartOffset(), line.getEndOffset() - line.getStartOffset(), segment);
            TabExpander tx = new MoeTabExpander(tabSize, leftMargin);
            int width = Utilities.getTabbedTextWidth(segment, metrics, leftMargin, tx, line.getStartOffset());
            return width;
        }
        catch (BadLocationException ble) {
            throw new RuntimeException(ble);
        }
    }
    
    /**
     * Get the longest line element (find it first if necessary).
     */
    private Element getLongestLine()
    {
        if (longestLine == null) {
            determineLongestLine();
        }
        return longestLine;
    }
    
    /**
     * Determine which line is the longest line, and store in {@code longestLine}.
     */
    private void determineLongestLine()
    {
        Element rootElement = getElement();
        int lineCount = rootElement.getElementCount();
        
        longestLine = null;
        int maxLineWidth = -1;
        
        for (int i = 0; i < lineCount; i++) {
            Element line = rootElement.getElement(i);
            int lineWidth = getLineWidth(line);
            if (lineWidth > maxLineWidth) {
                longestLine = line;
                maxLineWidth = lineWidth;
            }
        }
    }

    /**
     * Get the tab size (in characters), according to the appropriate document attribute.
     */
    protected int getTabSize()
    {
        Integer i = (Integer) getDocument().getProperty(PlainDocument.tabSizeAttribute);
        return (i != null) ? i.intValue() : 8;
    }
    
    /**
     * Mark an (inclusive) line range as in need of repaint.
     * 
     * @param line0   The lowest damaged line number
     * @param line1   The highest damaged line number >= line0
     * @param a       The view allocation
     * @param host    The component hosting the view
     */
    protected void damageLineRange(int line0, int line1, Shape a, Component host)
    {
        if (line1 < line0 || a == null) {
            return;
        }

        Rectangle abounds = a.getBounds();
        int rx = abounds.x;
        int ry = abounds.y + line0 * metrics.getHeight();
        int rw = abounds.width;
        int rh = (line1 - line0 + 1) * metrics.getHeight();
        
        host.repaint(rx, ry, rw, rh);
    }

    /**
     * Mark the appropriate areas as needing a repaint after changes to the document.
     */
    protected void updateDamage(DocumentEvent changes, Shape a, ViewFactory f)
    {
        checkMetrics();
        Component host = getContainer();
        Element elem = getElement();
        
        DocumentEvent.ElementChange ec = changes.getChange(elem);
        Element[] added = (ec != null) ? ec.getChildrenAdded() : null;
        Element[] removed = (ec != null) ? ec.getChildrenRemoved() : null;
        
        if (((added != null) && (added.length > 0)) || 
            ((removed != null) && (removed.length > 0))) {
            // lines were added or removed...
            if (added != null && longestLine != null) {
                int currentMaxWidth = getLineWidth(longestLine);
                for (int i = 0; i < added.length; i++) {
                    int width = getLineWidth(added[i]);
                    if (width > currentMaxWidth) {
                        currentMaxWidth = width;
                        longestLine = added[i];
                    }
                }
            }
            if (removed != null) {
                for (int i = 0; i < removed.length; i++) {
                    if (removed[i] == longestLine) {
                        longestLine = null;
                        break;
                    }
                }
            }
            preferenceChanged(null, true, true);
            host.repaint();
        } else {
            // No elements (lines) were added or removed; the change might be an insert/remove confined
            // to a single line.
            Element map = getElement();
            int line = map.getElementIndex(changes.getOffset());
            int endLine = map.getElementIndex(changes.getOffset() + changes.getLength());
            damageLineRange(line, endLine, a, host);
            
            getLongestLine();
            if (changes.getType() == DocumentEvent.EventType.INSERT) {
                // check to see if the line is longer than current
                // longest line.
                int w = getLineWidth(longestLine);
                Element e = map.getElement(line);
                if (e == longestLine) {
                    preferenceChanged(null, true, false);
                } else if (getLineWidth(e) > w) {
                    longestLine = e;
                    preferenceChanged(null, true, false);
                }
            } else if (changes.getType() == DocumentEvent.EventType.REMOVE) {
                if (map.getElement(line) == longestLine) {
                    // removed from longest line... recalc
                    determineLongestLine();
                    preferenceChanged(null, true, false);
                }                       
            }
        }
    }
    
    @Override
    public void insertUpdate(DocumentEvent changes, Shape a, ViewFactory f)
    {
        updateDamage(changes, a, f);
    }

    @Override
    public void removeUpdate(DocumentEvent changes, Shape a, ViewFactory f)
    {
        updateDamage(changes, a, f);
    }

    @Override
    public void changedUpdate(DocumentEvent changes, Shape a, ViewFactory f)
    {
        updateDamage(changes, a, f);
    }
}
