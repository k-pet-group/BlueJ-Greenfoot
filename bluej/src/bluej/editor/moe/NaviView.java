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
import java.awt.Image;
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

import bluej.Config;
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
    private static final Image frame = Config.getImageAsIcon("image.editor.naviview.frame").getImage();
    private static final int frw = 5;  // frame width
    
    private Document document;
    private JEditorPane editorPane;
    
    private JScrollBar scrollBar;
    
    /** Current view position in terms of this component's co-ordinate space */
    private int currentViewPos;
    /** Current view position (bottom) in terms of this component's co-ordinate space */
    private int currentViewPosBottom;
    
    private int dragOffset;
    private boolean haveToolTip = false;
    
    private BufferedImage imgBuffer;
    
    public NaviView(Document document, JScrollBar scrollBar)
    {
        this.scrollBar = scrollBar;
        editorPane = new NVDrawPane(this);
        
        setDocument(document);
        setDoubleBuffered(false);
        
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
        vpos -= insets.top + frw;
        int prefHeight = (int) view.getPreferredSpan(View.Y_AXIS);
        int myHeight = getHeight() - insets.top - insets.bottom - frw*2;
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
        int myHeight = getHeight() - insets.top - insets.bottom - frw*2;
        
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
        int height = Math.min(prefHeight, getHeight() - insets.top - insets.bottom - 2*frw); 
        
        int topV = e.getValue() * height / (scrollBar.getMaximum()) + insets.top + frw;
        int bottomV = (e.getValue() + scrollBar.getVisibleAmount()) * height
                / scrollBar.getMaximum() + insets.top + frw;

        int repaintTop = Math.min(topV, currentViewPos);
        int repaintBottom = Math.max(bottomV, currentViewPosBottom);

        currentViewPos = topV;
        currentViewPosBottom = bottomV;
        
        repaint(0, repaintTop - frw, getWidth(), repaintBottom - repaintTop + 2 + frw*2);
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
        createImgBuffer(g);
        Rectangle clipBounds = new Rectangle(new Point(0,0), getSize());
        Insets insets = getInsets();
        g.getClipBounds(clipBounds);
        
        View view = editorPane.getUI().getRootView(editorPane);
        int prefHeight = (int) view.getPreferredSpan(View.Y_AXIS);
        int myHeight = getHeight() - insets.top - insets.bottom - frw*2;

        Document document = getDocument();
        if (document == null) {
            // Should not happen
            return;
        }

        int docHeight = Math.min(myHeight, prefHeight);

        // Calculate the visible portion
        // topV = the topmost visible line (in local coordinate space)
        // bottomV = the bottommost visible line (in local coordinate space)
        int topV = insets.top + frw + scrollBar.getValue() * docHeight / scrollBar.getMaximum();
        int bottomV = insets.top + frw + ((scrollBar.getValue() + scrollBar.getVisibleAmount()) * docHeight + (scrollBar.getMaximum() - 1)) / scrollBar.getMaximum();
        int viewHeight = bottomV - topV;
        
        Color background = MoeSyntaxDocument.getBackgroundColor();

        Graphics2D tg = imgBuffer.createGraphics();
        tg.setClip(new Rectangle(clipBounds.x - insets.left, clipBounds.y - insets.top, clipBounds.width, clipBounds.height));
        
        if (prefHeight > myHeight) {
            // scale!
            int width = (getWidth() - insets.left - insets.right - frw*2) * prefHeight / myHeight;
 
            int ytop = (clipBounds.y - insets.top - frw) * prefHeight / myHeight;
            int ybtm = ((clipBounds.y + clipBounds.height - insets.top - frw) * prefHeight + myHeight - 1) / myHeight;
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
            bg.setColor(background);
            bg.fillRect(0, 0, width, height);
            
            Rectangle shape = new Rectangle(frw, 0, width, prefHeight);
            bg.setClip(0, 0, width, height);
            bg.translate(-frw, -ytop);
            view.paint(bg, shape);
            
            tg.drawImage(bimage, frw, clipBounds.y - insets.top,
                    getWidth() - insets.left - insets.right - frw,
                    clipBounds.y + clipBounds.height - insets.top, 0, 0, width, height, null);
        }
        else {
            // Scaling not necessary
            int w = getWidth() - insets.left - insets.right - frw*2;
            int h = myHeight;
            
            Rectangle rb = new Rectangle();
            rb.x = Math.max(frw, clipBounds.x - insets.left);
            rb.y = Math.max(frw, clipBounds.y - insets.top);
            rb.width = Math.min(w - rb.x + frw, clipBounds.width);
            rb.height = Math.min(h - rb.y + frw, clipBounds.height);
            
            tg.setClip(rb);
            tg.setColor(background);
            tg.fillRect(rb.x, rb.y, rb.width, rb.height);
            
            // Draw the code on the buffer image:
            Rectangle bufferBounds = new Rectangle (frw,frw,w,h);
            view.paint(tg, bufferBounds);
            
            tg.setClip(new Rectangle(clipBounds.x - insets.left, clipBounds.y - insets.top, clipBounds.width, clipBounds.height));
        }
        
        // Clear the border area (frw width)
        tg.setColor(background);
        tg.fillRect(0, 0, getWidth() - insets.left - insets.right, frw);
        tg.fillRect(0, 0, frw, docHeight + frw);
        tg.fillRect(getWidth() - insets.right - insets.left - frw, 0, frw, docHeight + frw);        

        tg.setColor(getBackground());
        tg.fillRect(0, docHeight + frw, getWidth() - insets.left - insets.right, myHeight + insets.top + insets.bottom + frw*2);
                   
        // Darken the area outside the viewport (above)
        tg.setColor(new Color(0, 0, 0, 0.15f));
        if (topV > clipBounds.y) {
            tg.fillRect(clipBounds.x - insets.left, clipBounds.y - insets.top, clipBounds.width, topV - clipBounds.y);
        }

        // Darken the area outside the viewport (below)
        int docBottom = docHeight + frw + insets.top;
        if (bottomV < docBottom) {
            tg.fillRect(clipBounds.x - insets.left, bottomV - insets.top, clipBounds.width, docBottom - bottomV);
        }
        
        // Fill the area between the document end and bottom of the component
        if (docBottom < clipBounds.y + clipBounds.height) {
            Color myBgColor = getBackground();
            // This odd statement is necessary to avoid a weird Mac OS X bug
            // (OS X 10.6.2, Java 1.6.0_17) with repaint which occurs when
            // a tooltip is showing.
            tg.setColor(new Color(myBgColor.getRGB()));
            tg.fillRect(clipBounds.x - insets.left, docBottom - insets.top, clipBounds.width,
                    clipBounds.y + clipBounds.height - docBottom);
        }

        // Draw a border around the visible area
        int fx1 = 0;
        int fy1 = topV - frw - insets.top;
        int fx2 = getWidth() - insets.right - insets.left;
        int fy2 = fy1 + viewHeight + frw*2;
        
        int fh = frame.getHeight(null);
        int fw = frame.getWidth(null);
        
        // top - left corner, straight, right corner
        tg.drawImage(frame, fx1, fy1, fx1+5, fy1+5, 0, 0, 5, 5, null);
        tg.drawImage(frame, fx1+5, fy1, fx2-5, fy1+5, 5, 0, fw - 5, 5, null);
        tg.drawImage(frame, fx2-5, fy1, fx2, fy1+5, fw-5, 0, fw, 5, null);
        
        // sides
        tg.drawImage(frame, fx1, fy1+5, fx1+5, fy2-5, 0, 5, 5, fh-5, null);
        tg.drawImage(frame, fx2-5, fy1+5, fx2, fy2-5, fw-5, 5, fw, fh-5, null);
        
        // bottom - left corner, straight, right corner
        tg.drawImage(frame, fx1, fy2-5, fx1+5, fy2, 0, fh-5, 5, fh, null);
        tg.drawImage(frame, fx1+5, fy2-5, fx2-5, fy2, 5, fh-5, fw-5, fh, null);
        tg.drawImage(frame, fx2-5, fy2-5, fx2, fy2, fw-5, fh-5, fw, fh, null);

        g.drawImage(imgBuffer, insets.left, insets.top, null);
    }
    
    public void createImgBuffer(Graphics g)
    {
        Insets insets = getInsets();
        int w = getWidth() - insets.left - insets.right;
        int h = getHeight() - insets.top - insets.bottom;
        
        if (imgBuffer != null) {
            if (imgBuffer.getHeight() >= h && imgBuffer.getWidth() >= w) {
                return;
            }
        }
        
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            imgBuffer = g2d.getDeviceConfiguration().createCompatibleImage(w, h);
        }
        else {
            imgBuffer = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        }
    }
}
