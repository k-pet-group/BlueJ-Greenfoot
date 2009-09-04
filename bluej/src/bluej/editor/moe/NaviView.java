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
package bluej.editor.moe;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Segment;

/**
 * A Swing component for displaying a miniature version of document and allowing to navigate through
 * it by dragging the visible box.
 * 
 * @author Davin McCall
 */
public class NaviView extends JComponent implements AdjustmentListener, DocumentListener
{
    private Document document;
    private JScrollBar scrollBar;
    
    // The current view window lines
    private int currentViewPos;
    private int currentViewPosBottom;
    
    // The offset from the mouse position to the top of the view area, while dragging
    private int dragOffset;
    
    public NaviView(Document document, JScrollBar scrollBar)
    {
        this.document = document;
        document.addDocumentListener(this);
        this.scrollBar = scrollBar;
        
        scrollBar.addAdjustmentListener(this);
        
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(true);
        setFocusable(true);
        setEnabled(true);
        enableEvents(Event.MOUSE_MOVE  // Needed for tooltips apparently?
                | Event.MOUSE_DOWN
                | Event.MOUSE_DRAG);
        setToolTipText(""); // Otherwise tooltips don't work at all?
    }
    
    public void setDocument(Document document)
    {
        this.document = document;
        document.addDocumentListener(this);
        repaint();
    }
        
    @Override
    public String getToolTipText(MouseEvent event)
    {
        return "Pos: " + event.getY();
    }
    
    public void adjustmentValueChanged(AdjustmentEvent e)
    {
        // The scrollbar position changed
        
        int newTop = sbPositionToLine(e.getValue());
        int newBottom = sbPositionToLine(e.getValue() + scrollBar.getVisibleAmount());
        
        int repaintTop = Math.min(newTop, currentViewPos);
        int repaintBottom = Math.max(newBottom, currentViewPosBottom);
        
        currentViewPos = newTop;
        currentViewPosBottom = newBottom;
        
        repaint(0, repaintTop + getInsets().top, getWidth(), repaintBottom - repaintTop + 1);
    }

    @Override
    protected void processMouseEvent(MouseEvent e)
    {
        super.processMouseEvent(e);
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            int y = e.getY() - getInsets().top;
            if (y > currentViewPos && y < currentViewPosBottom) {
                // clicked within the current view area
                dragOffset = y - currentViewPos;
            }
            else {
                dragOffset = (currentViewPosBottom - currentViewPos) / 2;
                moveView(e.getY());
            }
        }
    }
    
    @Override
    protected void processMouseMotionEvent(MouseEvent e)
    {
        super.processMouseMotionEvent(e);
        if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
            moveView(e.getY());
        }
    }
    
    public void changedUpdate(DocumentEvent e)
    {
    }
    
    public void insertUpdate(DocumentEvent e)
    {
        int offset = e.getOffset();
        int length = e.getLength();
        int firstLine = document.getDefaultRootElement().getElementIndex(offset);
        int lastLine = document.getDefaultRootElement().getElementIndex(offset + length - 1);
        
        if (e.getChange(document.getDefaultRootElement()) != null) {
            // lines have been added
            lastLine = document.getDefaultRootElement().getElementCount() - 1; 
        }
        
        repaint(0l, 0, firstLine + getInsets().top, getWidth(), lastLine - firstLine + 1);
    }
    
    public void removeUpdate(DocumentEvent e)
    {
        int offset = e.getOffset();
        int length = e.getLength();
        int firstLine = document.getDefaultRootElement().getElementIndex(offset);
        int lastLine = document.getDefaultRootElement().getElementIndex(offset + length - 1);
        
        ElementChange ec = e.getChange(document.getDefaultRootElement());
        if (ec != null) {
            // lines have been removed
            lastLine = document.getDefaultRootElement().getElementCount() - 1;
            if (ec.getChildrenRemoved() != null) { 
                lastLine += ec.getChildrenRemoved().length;
            }
            if (ec.getChildrenAdded() != null) {
                lastLine -= ec.getChildrenAdded().length;
            }
        }
        
        repaint(0l, 0, firstLine + getInsets().top, getWidth(), lastLine - firstLine + 1);
    }
    
    /**
     * Move the view (by setting the scrollbar value), according to the given mouse coordinate
     * within the NaviView component.
     */
    private void moveView(int ypos)
    {
        int lineNum = ypos - getInsets().top;
        lineNum -= dragOffset;
        lineNum = Math.max(0, lineNum);
        
        int totalLines = document.getDefaultRootElement().getElementCount();
        int totalAmt = scrollBar.getMaximum() - scrollBar.getMinimum();
        
        int pos = lineNum * totalAmt / totalLines + scrollBar.getMinimum();
        scrollBar.setValue(pos);
    }
    
    /**
     * Convert a scrollbar position to a source line number.
     */
    private int sbPositionToLine(int position)
    {
        int amount = scrollBar.getMaximum() - scrollBar.getMinimum();
        int lines = document.getDefaultRootElement().getElementCount();
        return position * lines / amount;
    }
    
    @Override
    protected void paintComponent(Graphics g)
    {
        Rectangle clipBounds = new Rectangle(new Point(0,0), getSize());
        Insets insets = getInsets();
        g.getClipBounds(clipBounds);
        
        Color foreground = MoeSyntaxDocument.getDefaultColor();
        Color background = MoeSyntaxDocument.getBackgroundColor();
        Color notVisible = new Color((int)(background.getRed() * .9f),
                (int)(background.getGreen() * .9f),
                (int)(background.getBlue() * .9f));
        
        g.setColor(notVisible);
        g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);

        int topLine = sbPositionToLine(scrollBar.getValue());
        int bottomLine = sbPositionToLine(scrollBar.getValue() + scrollBar.getVisibleAmount());
        g.setColor(background);
        g.fillRect(clipBounds.x, topLine + insets.top, clipBounds.width, bottomLine - topLine);
        
        if (document == null) {
            // Should not happen
            return;
        }
        
        Element map = document.getDefaultRootElement();
        Segment lineSeg = new Segment();
        int lines = map.getElementCount();
        
        
        try {
            g.setColor(foreground);
            int firstLine = Math.max(0, clipBounds.y - insets.top);
            int lastLine = Math.max(0, clipBounds.y + clipBounds.height - insets.top);
            lastLine = Math.min(lastLine, lines);
            for (int i = firstLine; i < lastLine; i++) {
                Element lineEl = map.getElement(i);
                int start = lineEl.getStartOffset();
                int end = lineEl.getEndOffset();
                document.getText(start, end - start, lineSeg);
                
                int pos = lineSeg.offset;
                int endPos = pos + lineSeg.count;
                int xpos = insets.left;
                for (int j = pos; j < endPos; j++) {
                    if (! Character.isWhitespace(lineSeg.array[j])) {
                        g.drawLine(xpos, i + insets.top, xpos, i + insets.top);
                        xpos++;
                    }
                    else if (lineSeg.array[j] == '\t') {
                        xpos = (xpos + 4); // TODO use real tab size
                        xpos -= (xpos % 4);
                    }
                    else {
                        xpos++;
                    }
                }
            }
        }
        catch (BadLocationException ble) {
            throw new RuntimeException(ble);
        }
        
        // Draw a border around the visible area
        g.setColor(new Color((int)(background.getRed() * .7f),
                (int)(background.getGreen() * .7f),
                (int)(background.getBlue() * .7f)));
        g.drawRect(0 + insets.left, topLine + insets.top, getWidth() - insets.left - insets.right - 1,
                bottomLine - topLine);
    }
}
