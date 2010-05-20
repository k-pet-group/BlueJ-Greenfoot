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
package bluej.graph;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;

import bluej.Config;
import bluej.pkgmgr.graphPainter.GraphPainterStdImpl;

/**
 * Component to allow editing of general graphs.
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 * @version $Id: GraphEditor.java 7645 2010-05-20 10:54:49Z nccb $
 */
public class GraphEditor extends JComponent
    implements MouseMotionListener, GraphListener
{
    protected static final Color background = Config.getOptionalItemColour("colour.graph.background");
    protected final Color envOpColour = Config.getItemColour("colour.menu.environOp");
    
    private final static Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
    private final static Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
    private final static Cursor resizeCursor = new Cursor(Cursor.SE_RESIZE_CURSOR);

    /**  The grid resolution for graph layout. */
    public static final int GRID_SIZE = 10;

    private Graph graph;
    private GraphPainter graphPainter;
    private MarqueePainter marqueePainter;

    private SelectionController selectionController;

    private Cursor currentCursor = defaultCursor;  // currently shown cursor
    
    /**
     * Create a graph editor.
     * @param graph The graph being edited by this editor.
     */
    public GraphEditor(Graph graph)
    {
        this.graph = graph;
        marqueePainter = new MarqueePainter();
        graphPainter = GraphPainterStdImpl.getInstance();
        selectionController = new SelectionController(this);
        graph.addListener(this);
        setToolTipText(""); // Turn on tool-tips for this component
    }

    /**
     * Start our mouse listener. This is not done in the constructor, because we want 
     * to give others (the PkgMgrFrame) the chance to listen first.
     */
    public void startMouseListening()
    {
        addMouseMotionListener(this);
        addMouseMotionListener(selectionController);
        addMouseListener(selectionController);
        addKeyListener(selectionController);
    }
    
    
    /**
     * Tell how big we would like to be. The preferred size of the graph editor
     * the the size of the edited graph.
     */
    public Dimension getPreferredSize()
    {
        return graph.getMinimumSize();
    }

    /**
     * Tell how big we would like to be. The minimum size of the graph editor
     * the the size of the edited graph.
     */
    public Dimension getMinimumSize()
    {
        return graph.getMinimumSize();
    }

    /**
     * Paint this graph editor (this may be on screen or on a printer).
     */
    public void paint(Graphics g)
    {
        Graphics2D g2D = (Graphics2D) g;
        //draw background
        if (!(g2D instanceof PrintGraphics)) {
            Dimension d = getSize();
            GradientPaint gp = new GradientPaint(
                    d.width/4, 0, new Color(253,253,250),
                    d.width*3/4, d.height, new Color(241,231,196));

            g2D.setPaint(background != null ? background : gp);
            g2D.fillRect(0, 0, d.width, d.height);
        }

        graphPainter.paint(g2D, this);
        marqueePainter.paint(g2D, selectionController.getMarquee());

//        super.paint(g); // for border
    }

    // ---- MouseMotionListener interface: ----

    /**
     * The mouse was dragged.
     */
    public void mouseDragged(MouseEvent evt)
    {
    }

    /**
     * The mouse was moved - check whether we should adjust the cursor.
     */
    public void mouseMoved(MouseEvent evt)
    {
        int x = evt.getX();
        int y = evt.getY();
        SelectableGraphElement element = graph.findGraphElement(x, y);
        Cursor newCursor = defaultCursor;
        if (element != null) {
            if (element.isResizable() && element.isHandle(x, y)) {
                newCursor = resizeCursor;
            }
            else {
                newCursor = handCursor;                
            }
        }
        if(currentCursor != newCursor) {
            setCursor(newCursor);
            currentCursor = newCursor;
        }
    }

    // ---- end of MouseMotionListener interface ----

    /**
     * Process mouse events. This is a bug work-around: we prefer to handle the 
     * mouse events in the mouse listener methods in the selection controller, 
     * but on Windows the isPopupTrigger flag is not correctly set in the 
     * mousePressed event. This method seems to be the only place to reliably get 
     * it. So unfortunately, we need to process the popup trigger here.
     * 
     * This method is called after the corresponding mousePressed method.
     */
    protected void processMouseEvent(MouseEvent evt)
    {
        super.processMouseEvent(evt);
        if (evt.isPopupTrigger())
            selectionController.handlePopupTrigger(evt);
    }


    /**
     * Clear the set of selected classes. (Nothing will be selected after this.)
     */
    public void clearSelection()
    {
        selectionController.clearSelection();
    }

    /**
     * Clear the current selection.
     */
    public void removeFromSelection(SelectableGraphElement element)
    {
        selectionController.removeFromSelection(element);
    }
    
    /**
     * Add to the current selection
     * @param element the element to add
     */
    public void addToSelection(SelectableGraphElement element)
    {
    	selectionController.addToSelection(element);
    }
    
   
    /**
     * Return the rubber band information.
     */
    public RubberBand getRubberBand()
    {
        return selectionController.getRubberBand();
    }

    /**
     * Return the graph currently being edited.
     */
    public Graph getGraph()
    {
        return graph;
    }

    public void popupMenu(int x, int y)
    {
        // by default, do nothing
    }

    private boolean hasFocus;
    
    public boolean hasFocus(){
        return hasFocus;
    }
    
    public void setHasFocus(boolean hasFocus){
        this.hasFocus = hasFocus;
    }
    
    public String getToolTipText(MouseEvent event)
    {
        int x = event.getX();
        int y = event.getY();
        SelectableGraphElement element = graph.findGraphElement(x, y);
        
        if (element == null) {
            return null;
        } else {
            return element.getTooltipText();
        }
    }    
    
    // ---- GraphListener interface ----
    
    public void selectableElementRemoved(SelectableGraphElement element)
    {
        removeFromSelection(element);
    }
    
    public void graphChanged()
    {
        repaint();
    }
}
