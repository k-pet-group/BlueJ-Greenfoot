package bluej.graph;

import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;

import javax.swing.JComponent;

import bluej.Config;
import bluej.pkgmgr.graphPainter.GraphPainterStdImpl;

/**
 * Canvas to allow editing of general graphs
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 * @version $Id: GraphEditor.java 2775 2004-07-09 15:07:12Z mik $
 */
public class GraphEditor extends JComponent
    implements MouseListener, MouseMotionListener, KeyListener
{
    protected static final Color background = Config.getItemColour("colour.graph.background");

    private final static Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
    private final static Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
    private final static Cursor resizeCursor = new Cursor(Cursor.SE_RESIZE_CURSOR);

    public static final int GRID_SIZE = 10;

    private int lastClickX, lastClickY; //coordinates for the last left clicked
                                        // position
    private Graph graph;
    private GraphPainter graphPainter;

    private Marquee marquee;    // Contains the elements that have been selected
    private MarqueePainter marqueePainter;

    private SelectableGraphElement selectedElement;
    private GraphElementSet selectedSet;

    private GraphElementController graphElementController;

    private boolean readOnly = false;
    private Cursor currentCursor = defaultCursor;  // currently shown cursor
    
    public GraphEditor(Graph graph)
    {
        this.graph = graph;
        selectedElement = null;
        addMouseMotionListener(this);
        marquee = new Marquee(graph);
        marqueePainter = new MarqueePainter();
        selectedSet = new GraphElementSet();
        graphPainter = GraphPainterStdImpl.getInstance();
        graphElementController = new GraphElementController(this, graph, selectedSet);
    }

    public Dimension getPreferredSize()
    {
        return graph.getMinimumSize();
    }

    public Dimension getMinimumSize()
    {
        return graph.getMinimumSize();
    }

    public void paint(Graphics g)
    {
        Graphics2D g2D = (Graphics2D) g;
        //draw background
        if (!(g2D instanceof PrintGraphics)) {
            Dimension d = getSize();
            g2D.setColor(background);
            g2D.fillRect(0, 0, d.width, d.height);
        }

        graphPainter.paint(g2D, this);
        marqueePainter.paint(g2D, marquee);

        super.paint(g); // for border
    }

    /**
     * Finds the Edge that covers the coordinate x,y. If no edge is found, null
     * is returned.
     * 
     * @param x
     *            the x coordinate
     * @param y
     *            the x coordinate
     * @return Edge
     */
    private Edge findEdge(int x, int y)
    {
        GraphElement graphElement = null;
        for (Iterator it = graph.getEdges(); it.hasNext();) {
            graphElement = (GraphElement) it.next();
            if (graphElement.contains(x, y)) {
                return (Edge) graphElement;
            }
        }
        return null;
    }

    /**
     * Finds the Vertex that covers the coordinate x,y. If no vertex is found,
     * null is returned.
     * 
     * @param x
     *            the x coordinate
     * @param y
     *            the x coordinate
     * @return Vertex
     */
    private Vertex findVertex(int x, int y)
    {
        GraphElement currentGraphElement = null;
        GraphElement topGraphElement = null;

        //Try to find a vertex containing the point
        // Rather than breaking when we find the vertex we keep searching
        // which will therefore find the LAST vertex containing the point
        // This turns out to be the vertex which is rendered at the front
        for (Iterator it = graph.getVertices(); it.hasNext();) {
            currentGraphElement = (GraphElement) it.next();
            if (currentGraphElement.contains(x, y)) {
                topGraphElement = currentGraphElement;
            }
        }
        return (Vertex) topGraphElement;
    }

    /**
     * Finds the graphElement that covers the coordinate x,y. If no element is
     * found, null is returned. If a Vertex and an Edge both covers x, y the
     * Vertex will be returned.
     * 
     * @param x
     *            the x coordinate
     * @param y
     *            the x coordinate
     * @return GraphElement
     */
    private SelectableGraphElement findGraphElement(int x, int y)
    {
        SelectableGraphElement graphElement = findVertex(x, y);

        if (graphElement == null) {
            graphElement = findEdge(x, y);
        }
        return graphElement;
    }

    private boolean isMultiselectionKeyDown(MouseEvent evt)
    {
        if (Config.isMacOS()) {
            return evt.isShiftDown() || evt.isMetaDown();
        }
        else {
            return evt.isShiftDown() || evt.isControlDown();
        }

    }

    public Vertex findSingleVertex()
    {
        Iterator selection = selectedSet.iterator();
        Vertex currentVertex = null;
        GraphElement graphElement;
        // if there is a selection we pick a vertex from that
        while (selection.hasNext()) {
            if ((graphElement = (GraphElement) selection.next()) instanceof Vertex) {
                currentVertex = (Vertex) graphElement;
            }
        }
        //      if there is no selection we select an existing vertex
        if (currentVertex == null) {
            Iterator i = graph.getVertices();
            currentVertex = (Vertex) i.next();
        }
        return currentVertex;
    }

    // ---- KeyListener interface ----

    public void keyPressed(KeyEvent evt)
    {
        graphElementController.keyPressed(evt);
    }

    public void keyReleased(KeyEvent evt)
    {
        graphElementController.keyReleased(evt);
    }

    public void keyTyped(KeyEvent evt)
    {}

    // ---- MouseListener interface ----

    public void mouseClicked(MouseEvent evt)
    {
        System.out.println("click");
        if (selectedElement != null) {
            if (evt.getClickCount() > 1 && ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
                selectedElement.doubleClick(evt, this);
            }
            else {
                selectedElement.singleClick(evt, this);
            }
            if (!isMultiselectionKeyDown(evt)) {
                selectedSet.clear();
                selectedSet.add(selectedElement);
            }
        }
    }

    public void mousePressed(MouseEvent evt)
    {
        System.out.println("press");
        lastClickX = evt.getX();
        lastClickY = evt.getY();
        requestFocus();

        marquee.start(lastClickX, lastClickY); //TODO could this be a local
                                               // variable?
        selectedElement = findGraphElement(lastClickX, lastClickY);

        if (selectedElement == null) {
            if (!isMultiselectionKeyDown(evt)) {
                //the background was clicked and multiselectionKey wasn't down
                selectedSet.clear();
            }
        }
        else {
            if (isMultiselectionKeyDown(evt)) {
                //a class was clicked, while multiselectionKey was down.
                if (selectedElement.isSelected()) {
                    // the clicked class was already selected
                    selectedSet.remove(selectedElement);
                }
                else {
                    //the clicked class wasn't selected
                    selectedSet.add(selectedElement);
                }
            }
            else {
                //a class was clicked,while multiselection was up.
                if (! selectedElement.isSelected()) {
                    //the class wasn't selected
                    selectedSet.clear();
                    selectedSet.add(selectedElement);
                }
            }

            //if the graphElement is selectable and it got clicked on a handle,
            //then it is resizing.
            selectedElement.setResizing(selectedElement.isHandle(lastClickX, lastClickY));
            if (selectedElement.isResizing()) {
                selectedSet.clear();
                selectedSet.add(selectedElement);
            }

            // Signal the graphElementController that the mouse was pressed
            if (!evt.isPopupTrigger()
                    && ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
                graphElementController.setActiveGraphElement(selectedElement);
                graphElementController.mousePressed(evt);
            }
        }
    }

    public void mouseReleased(MouseEvent evt)
    {
        System.out.println("release");
        marquee.stop();
        repaint();
        selectedSet.moveAll(marquee.getElements());

        if (selectedElement != null) {
            selectedElement.setResizing(false);
        }
        if (selectedElement != null && ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
            graphElementController.mouseReleased(evt);
        }
    }

    /**
     * The mouse pointer entered this component.
     */
    public void mouseEntered(MouseEvent e)
    {}

    /**
     * The mouse pointer exited this component.
     */
    public void mouseExited(MouseEvent e)
    {}

    // ---- end of MouseListener interface ----

    // ---- MouseMotionListener interface: ----

    public void mouseDragged(MouseEvent evt)
    {
        if (readOnly)
            return;

        if (!evt.isPopupTrigger() && ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
            if (selectedElement == null) {
                marquee.move(evt.getX(), evt.getY());
                repaint();
            }
            else {
                graphElementController.mouseDragged(evt);
            }
        }
    }

    public void mouseMoved(MouseEvent evt)
    {
        int x = evt.getX();
        int y = evt.getY();
        SelectableGraphElement element = findGraphElement(x, y);
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

    protected void processMouseEvent(MouseEvent evt)
    {
        super.processMouseEvent(evt);

        if (evt.isPopupTrigger() && selectedElement != null) {
            selectedSet.clear();
            selectedSet.add(selectedElement);
            selectedElement.popupMenu(lastClickX, lastClickY, this);
        }
    }

    public void setReadOnly(boolean state)
    {
        readOnly = state;
    }

    public static Point snapToGrid(Point point)
    {
        int x_steps = (int) point.getX() / GraphEditor.GRID_SIZE;
        int new_x = x_steps * GraphEditor.GRID_SIZE;//new x-coor w/ respect to
                                                    // grid

        int y_steps = (int) point.getY() / GraphEditor.GRID_SIZE;
        int new_y = y_steps * GraphEditor.GRID_SIZE;//new y-coor w/ respect to
                                                    // grid
        return new Point(new_x, new_y);
    }

    public void clearSelection()
    {
        selectedSet.clear();
    }

    public Graph getGraph()
    {
        return graph;
    }
}