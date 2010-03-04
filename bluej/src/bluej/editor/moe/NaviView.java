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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
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
        if (editorPane == null || imgBuffer == null) {
            return;
        }
        View view = editorPane.getUI().getRootView(editorPane);
        Insets insets = getInsets();
        int prefHeight = (int) view.getPreferredSpan(View.Y_AXIS);
        int myHeight = getHeight() - insets.top - insets.bottom - frw*2;
        
        if (prefHeight > myHeight) {
            int ptop = top * myHeight / prefHeight;
            int pbottom = (bottom * myHeight + prefHeight - 1) / prefHeight;
            paintImgBuffer(ptop, pbottom);
            repaint(0, ptop + insets.top + frw, getWidth(), pbottom - ptop);
        }
        else {
            paintImgBuffer(top, bottom);
            repaint(0, top + insets.top + frw, getWidth(), bottom - top);
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

        repaint(0, topV - frw, getWidth(), bottomV - topV + 2 + frw*2);
        repaint(0, currentViewPos - frw, getWidth(), currentViewPosBottom - currentViewPos + 2 + frw*2);

        currentViewPos = topV;
        currentViewPosBottom = bottomV;
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
    
    /**
     * Paint to the backing image buffer.
     * 
     * @param top  The top line (in model co-ordinates) to paint
     * @param bottom  The bottom line (in model co-ordinates) to paint
     */
    private void paintImgBuffer(int top, int bottom)
    {
        int myHeight = imgBuffer.getHeight();
        View view = editorPane.getUI().getRootView(editorPane);
        int prefHeight = (int) view.getPreferredSpan(View.Y_AXIS);

        Color background = MoeSyntaxDocument.getBackgroundColor();
        
        Graphics2D g = imgBuffer.createGraphics();

        if (prefHeight > myHeight) {
            // scale!
            int width = (imgBuffer.getWidth() - frw*2) * prefHeight / myHeight;
 
            int ytop = top * prefHeight / myHeight;
            int ybtm = (bottom * prefHeight + myHeight - 1) / myHeight;
            int height = ybtm - ytop;
            
            if (height > 800) {
                height = 800;
                ybtm = ytop + 800;
                int newbottom = top + (height * myHeight / prefHeight);
                if (newbottom <= top) {
                    newbottom = top + 1;
                    ybtm = (newbottom* prefHeight + myHeight - 1) / myHeight;
                    height = ybtm - ytop; 
                }
                enqueueRepaint(newbottom, bottom);
                bottom = newbottom;
            }
            
            if (height < 1) {
                height = 1;
                ybtm = ytop + 1;
                bottom = top + (height * myHeight / prefHeight);
            }
            
            // Create a buffered image to use
            BufferedImage bimage = ((Graphics2D) g).getDeviceConfiguration().createCompatibleImage(width, height);
            Map<Object,Object> hints = new HashMap<Object,Object>();
            hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            ((Graphics2D) g).addRenderingHints(hints);
            
            Graphics2D bg = bimage.createGraphics();
            bg.setColor(background);
            bg.fillRect(0, 0, width, height);
            
            Rectangle shape = new Rectangle(frw, 0, width, prefHeight);
            bg.setClip(0, 0, width, height);
            bg.translate(-frw, -ytop);
            view.paint(bg, shape);
            
            g.drawImage(bimage, frw, top,
                    imgBuffer.getWidth() - frw,
                    bottom, 0, 0, width, height, null);
            
            bg.dispose();
        }
        else {
            // Scaling not necessary
            int w = imgBuffer.getWidth() - frw*2;
            int h = myHeight;
            
            Rectangle rb = new Rectangle();
            rb.x = frw;
            rb.y = Math.max(frw, top);
            rb.width = imgBuffer.getWidth() - frw*2;
            rb.height = bottom - top;
            
            g.setClip(rb);
            g.setColor(background);
            g.fillRect(rb.x, rb.y, rb.width, rb.height);
            
            // Draw the code on the buffer image:
            Rectangle bufferBounds = new Rectangle (frw,frw,w,h);
            view.paint(g, bufferBounds);
        }
        
        g.dispose();
    }
    
    private List<Integer> tops = new ArrayList<Integer>();
    private List<Integer> bottoms = new ArrayList<Integer>();
    
    private void enqueueRepaint(int top, int bottom)
    {
        ListIterator<Integer> i = tops.listIterator();
        ListIterator<Integer> j = bottoms.listIterator();
        while (i.hasNext()) {
            int etop = i.next();
            int ebtm = j.next();
            if (top < etop) {
                bottom = Math.min(bottom, etop);
            }
            else if (bottom > ebtm) {
                top = Math.max(top, ebtm);
            }
            else {
                // fully contained
                return;
            }
        }
        
        tops.add(top);
        bottoms.add(bottom);
    }
    
    @Override
    protected void paintComponent(Graphics g)
    {   
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
        
        createImgBuffer(g, prefHeight > myHeight);
        g.drawImage(imgBuffer, insets.left, insets.top, null);
        
        Color background = MoeSyntaxDocument.getBackgroundColor();
        
        int lx = insets.left;
        int rx = getWidth() - insets.right;
        int ty = insets.top;
        
        // Clear the border area (frw width)
        g.setColor(background);
        g.fillRect(lx, ty, rx - lx, frw);
        g.fillRect(lx, ty, frw, docHeight + frw);
        g.fillRect(rx - frw, ty, frw, docHeight + frw);        

        g.setColor(getBackground());
        g.fillRect(lx, docHeight + frw + insets.top, rx - lx, myHeight - docHeight + frw);
                   
        // Darken the area outside the viewport (above)
        g.setColor(new Color(0, 0, 0, 0.15f));
        if (topV > clipBounds.y) {
            g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, topV - clipBounds.y);
        }

        // Darken the area outside the viewport (below)
        int docBottom = docHeight + frw + insets.top;
        if (bottomV < docBottom) {
            g.fillRect(clipBounds.x, bottomV, clipBounds.width, docBottom - bottomV);
        }
        
        // Fill the area between the document end and bottom of the component
        if (docBottom < clipBounds.y + clipBounds.height) {
            Color myBgColor = getBackground();
            // This odd statement is necessary to avoid a weird Mac OS X bug
            // (OS X 10.6.2, Java 1.6.0_17) with repaint which occurs when
            // a tooltip is showing.
            g.setColor(new Color(myBgColor.getRGB()));
            g.fillRect(clipBounds.x, docBottom, clipBounds.width,
                    clipBounds.y + clipBounds.height - docBottom);
        }

        // Draw a border around the visible area
        int fx1 = lx;
        int fy1 = topV - frw;
        int fx2 = rx;
        int fy2 = bottomV;
        
        int fh = frame.getHeight(null);
        int fw = frame.getWidth(null);
        
        // top - left corner, straight, right corner
        g.drawImage(frame, fx1, fy1, fx1+5, fy1+5, 0, 0, 5, 5, null);
        g.drawImage(frame, fx1+5, fy1, fx2-5, fy1+5, 5, 0, fw - 5, 5, null);
        g.drawImage(frame, fx2-5, fy1, fx2, fy1+5, fw-5, 0, fw, 5, null);
        
        // sides
        g.drawImage(frame, fx1, fy1+5, fx1+5, fy2, 0, 5, 5, fh-5, null);
        g.drawImage(frame, fx2-5, fy1+5, fx2, fy2, fw-5, 5, fw, fh-5, null);
        
        // bottom - left corner, straight, right corner
        g.drawImage(frame, fx1, fy2, fx1+5, fy2+5, 0, fh-5, 5, fh, null);
        g.drawImage(frame, fx1+5, fy2, fx2-5, fy2+5, 5, fh-5, fw-5, fh, null);
        g.drawImage(frame, fx2-5, fy2, fx2, fy2+5, fw-5, fh-5, fw, fh, null);
        
        if (! tops.isEmpty()) {
            int rtop = tops.remove(0);
            int rbottom = bottoms.remove(0);
            paintImgBuffer(rtop, rbottom);
            repaint(0, rtop, getWidth(), rbottom - rtop);
        }
    }
    
    public void createImgBuffer(Graphics g, boolean scaling)
    {
        Insets insets = getInsets();
        int w = getWidth() - insets.left - insets.right;
        int h = getHeight() - insets.top - insets.bottom;
                
        if (imgBuffer != null) {
            if (imgBuffer.getHeight() == h && imgBuffer.getWidth() == w) {
                return;
            }
        }
        
        BufferedImage oldImgBuffer = imgBuffer;
        
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            imgBuffer = g2d.getDeviceConfiguration().createCompatibleImage(w, h);
        }
        else {
            imgBuffer = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        }
        
        // Create the new image buffer and paint the old one onto it if possible.
        Graphics2D g2d = imgBuffer.createGraphics();
        if (oldImgBuffer == null) {
            g2d.setColor(getBackground());
            g2d.fillRect(0, 0, imgBuffer.getWidth(), imgBuffer.getHeight());
            paintImgBuffer(0, imgBuffer.getHeight());
        }
        else if (! scaling) {
            g2d.drawImage(oldImgBuffer, 0, 0, null);
            paintImgBuffer(oldImgBuffer.getHeight(), imgBuffer.getHeight());
        }
        else {
            g2d.drawImage(oldImgBuffer, 0, 0, imgBuffer.getWidth(), imgBuffer.getHeight(),
                    0, 0, oldImgBuffer.getWidth(), oldImgBuffer.getHeight(), null);
            paintImgBuffer(0, imgBuffer.getHeight());
        }
        g2d.dispose();
    }
}
