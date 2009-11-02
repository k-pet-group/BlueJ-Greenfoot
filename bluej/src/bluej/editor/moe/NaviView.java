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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.ToolTipManager;
import javax.swing.text.Document;
import javax.swing.text.View;
import javax.swing.text.Position.Bias;

import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

/**
 * "NaviView" component. Displays a miniature version of the document in the editor, and allows moving
 * through the document by clicking/dragging or cycling the mouse wheel.
 * 
 * @author Davin Mccall
 */
public class NaviView extends JPanel implements AdjustmentListener
{
    private Document document;
    private JEditorPane editorPane;
    
    private JScrollBar scrollBar;
    
    /** Current view position in terms of this component's co-ordinate space */
    private int currentViewPos;
    /** Current view position (bottom) in terms of this component's co-ordinate space */
    private int currentViewPosBottom;
    
    private int dragOffset;
    private boolean haveToolTip = false;
    
    public NaviView(Document document, JScrollBar scrollBar)
    {
        this.scrollBar = scrollBar;
        editorPane = new NVDrawPane(this);
        
        setDocument(document);
        
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        enableEvents(MouseEvent.MOUSE_WHEEL_EVENT_MASK);
        setFocusable(true);
        ToolTipManager.sharedInstance().registerComponent(this);
    }
    
    /**
     * Set the document displayed in this NaviView.
     */
    public void setDocument(Document document)
    {
        scrollBar.removeAdjustmentListener(this);
        this.document = document;
        editorPane.setDocument(document);
        if (document != null) {
            scrollBar.addAdjustmentListener(this);
        }
    }
    
    /**
     * Get the document displayed by this NaviView.
     */
    private Document getDocument()
    {
        return document;
    }
    
    @Override
    public Dimension getMinimumSize()
    {
        // Overcome the awkward default situation where the minimum size becomes larger
        // than the preferred size.
        return getPreferredSize();
    }
    
    @Override
    public void setVisible(boolean flag)
    {
        if (! flag) {
            scrollBar.removeAdjustmentListener(this);
        }
        else {
            if (! isVisible() && document != null) {
                scrollBar.addAdjustmentListener(this);
            }
        }
        super.setVisible(flag);
    }
    
    /**
     * Convert a y-coordinate in the NaviView co-ordinate space to a line number
     * in the document.
     */
    private int yViewToModel(int vpos)
    {
        View view = editorPane.getUI().getRootView(editorPane);
        Insets insets = getInsets();
        vpos -= insets.top;
        int prefHeight = (int) view.getPreferredSpan(View.Y_AXIS);
        int myHeight = getHeight() - insets.top - insets.bottom;
        if (prefHeight > myHeight) {
            vpos = vpos * prefHeight / myHeight;
        }
        Bias [] breturn = new Bias[1];
        int pos = view.viewToModel(0, vpos, new Rectangle(5,Integer.MAX_VALUE), breturn);
        
        return pos;
    }
    
    /**
     * Repaint model co-ordinates. The given lines must be translated to
     * the NaviView component's co-ordinate space.
     * @param top  The topmost line to repaint
     * @param bottom  The lowest (numerically higher) line to repaint
     */
    public void repaintModel(int top, int bottom)
    {
        if (editorPane == null) {
            return;
        }
        View view = editorPane.getUI().getRootView(editorPane);
        Insets insets = getInsets();
        int prefHeight = (int) view.getPreferredSpan(View.Y_AXIS);
        int myHeight = getHeight() - insets.top - insets.bottom;
        
        if (prefHeight > myHeight) {
            int ptop = top * myHeight / prefHeight;
            int pbottom = (bottom * myHeight + prefHeight - 1) / prefHeight;
            repaint(0, ptop, getWidth(), pbottom - ptop);
        }
        else {
            repaint(0, top, getWidth(), bottom - top);
        }
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.AdjustmentListener#adjustmentValueChanged(java.awt.event.AdjustmentEvent)
     */
    public void adjustmentValueChanged(AdjustmentEvent e)
    {
        View view = editorPane.getUI().getRootView(editorPane);
        int prefHeight = (int) view.getPreferredSpan(View.Y_AXIS);
        Insets insets = getInsets();
        int height = Math.min(prefHeight, getHeight() - insets.top - insets.bottom); 
        
        int topV = e.getValue() * height / (scrollBar.getMaximum()) + insets.top;
        int bottomV = (e.getValue() + scrollBar.getVisibleAmount()) * height
                / scrollBar.getMaximum() + insets.top;

        int repaintTop = Math.min(topV, currentViewPos);
        int repaintBottom = Math.max(bottomV, currentViewPosBottom);

        currentViewPos = topV;
        currentViewPosBottom = bottomV;

        repaint(0, repaintTop, getWidth(), repaintBottom - repaintTop + 1);
    }

    @Override
    public Point getToolTipLocation(MouseEvent event)
    {
        // Returning non-null unconditionally seems to make the tooltip persist as a small square
        // even when getToolTipText() returns null - sigh. (linux, JDK 6.0u13).
        if (haveToolTip) {
            Point ttloc = event.getPoint();
            ttloc.y += 15;
            ttloc.x -= 20;
            return ttloc;
        }
        return null;
    }
    
    @Override
    public String getToolTipText(MouseEvent event)
    {
        // int pos = viewToModel(event.getPoint());
        int pos = yViewToModel(event.getPoint().y);
        MoeSyntaxDocument document = (MoeSyntaxDocument) getDocument();
        ParsedNode pn = document.getParser();
        int startpos = 0;
        while (pn != null) {
            if (pn.getNodeType() == ParsedNode.NODETYPE_METHODDEF) {
                haveToolTip = true;
                return pn.getName();
            }
            
            NodeAndPosition nap = pn.findNodeAtOrAfter(pos, startpos);
            if (nap == null || nap.getPosition() > pos) {
                break;
            }
            pn = nap.getNode();
            startpos = nap.getPosition();
        }
        
        haveToolTip = false;
        return null;
    }
    
    @Override
    protected void processMouseEvent(MouseEvent e)
    {
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            int y = e.getY();
            if (y > currentViewPos && y < currentViewPosBottom) {
                // clicked within the current view area
                dragOffset = y - currentViewPos;
            }
            else {
                dragOffset = (currentViewPosBottom - currentViewPos) / 2;
                moveView(e.getY());
            }
        }
        else {
            super.processMouseEvent(e);
        }
    }
    
    @Override
    protected void processMouseMotionEvent(MouseEvent e)
    {
        if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
            moveView(e.getY());
        }
        else {
            super.processMouseMotionEvent(e);
        }
    }

    @Override
    protected void processMouseWheelEvent(MouseWheelEvent e)
    {
        if (e.getID() == MouseEvent.MOUSE_WHEEL) {
            scrollBar.dispatchEvent(e);
        }
    }
    
    /**
     * Move the view (by setting the scrollbar value), according to the given mouse coordinate
     * within the NaviView component.
     */
    private void moveView(int ypos)
    {
        int modelPos = yViewToModel(ypos - dragOffset);
        int lineNum = getDocument().getDefaultRootElement().getElementIndex(modelPos);
        lineNum = Math.max(0, lineNum);
        
        int totalLines = getDocument().getDefaultRootElement().getElementCount();
        int totalAmt = scrollBar.getMaximum() - scrollBar.getMinimum();
        
        int pos = lineNum * totalAmt / totalLines + scrollBar.getMinimum();
        scrollBar.setValue(pos);
    }
    
    @Override
    protected void paintComponent(Graphics g)
    {   
        Rectangle clipBounds = new Rectangle(new Point(0,0), getSize());
        Insets insets = getInsets();
        g.getClipBounds(clipBounds);
        
        View view = editorPane.getUI().getRootView(editorPane);
        int prefHeight = (int) view.getPreferredSpan(View.Y_AXIS);
        int myHeight = getHeight() - insets.top - insets.bottom;

        Document document = getDocument();
        if (document == null) {
            // Should not happen
            return;
        }

        int docHeight;
        if (prefHeight > myHeight) {
            // scale!
            int width = getWidth() * prefHeight / myHeight;
            docHeight = myHeight;
 
            int ytop = clipBounds.y * prefHeight / myHeight;
            int ybtm = ((clipBounds.y + clipBounds.height) * prefHeight + myHeight - 1) / myHeight;
            int height = ybtm - ytop;
            
            // Create a buffered image to use
            BufferedImage bimage;
            if (g instanceof Graphics2D) {
                bimage = ((Graphics2D) g).getDeviceConfiguration().createCompatibleImage(width, height);
                Map<Object,Object> hints = new HashMap<Object,Object>();
                hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                ((Graphics2D) g).addRenderingHints(hints);
            }
            else {
                bimage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            }
            
            Graphics2D bg = bimage.createGraphics();
            Color background = MoeSyntaxDocument.getBackgroundColor();
            bg.setColor(background);
            bg.fillRect(0, 0, width, height);
            
            Rectangle shape = new Rectangle(width, Integer.MAX_VALUE);
            bg.setClip(0, 0, width, height);
            bg.translate(0, -ytop);
            view.paint(bg, shape);
            g.drawImage(bimage, insets.left, clipBounds.y, getWidth() - insets.left - insets.right,
                    clipBounds.y + clipBounds.height, 0, 0, width, height, null);
        }
        else {
            docHeight = prefHeight;
            
            Color background = MoeSyntaxDocument.getBackgroundColor();
            g.setColor(background);
            g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
                        
            Rectangle shape = new Rectangle(insets.left,
                    insets.top, getWidth() - insets.left - insets.right,
                    myHeight);
            view.paint(g, shape);
        }
        
        // Calculate the visible portion
        int topV = scrollBar.getValue() * docHeight / scrollBar.getMaximum();
        int bottomV = (scrollBar.getValue() + scrollBar.getVisibleAmount()) * docHeight / scrollBar.getMaximum();
        int viewHeight = bottomV - topV;

        // Draw a border around the visible area
        g.setColor(new Color(140, 140, 255));
        g.drawRect(0 + insets.left, topV + insets.top, getWidth() - insets.left - insets.right - 1,
               viewHeight);
    }
    
}
