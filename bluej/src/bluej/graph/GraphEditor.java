package bluej.graph;

import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;

import javax.swing.JComponent;

import bluej.Config;

/**
 * Canvas to allow editing of general graphs
 *
 * @author  Michael Cahill
 * @version $Id: GraphEditor.java 2176 2003-09-05 10:23:47Z fisker $
 */
public class GraphEditor extends JComponent
    implements MouseListener, MouseMotionListener, KeyListener
{
    protected static final Color background = Config.getItemColour("colour.graph.background");

    private Graph graph;
    private GraphElement activeGraphElement;
    final static Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
    final static Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
    final static Cursor arrowCursor = new Cursor(Cursor.SE_RESIZE_CURSOR);
    private boolean readOnly = false;

    public GraphEditor(Graph graph)
    {
        this.graph = graph;
        activeGraphElement = null;
        addMouseMotionListener(this);
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
        if(!(g instanceof PrintGraphics)) {
            Dimension d = getSize();
            g.setColor(background);
            g.fillRect(0, 0, d.width, d.height);
        }

        graph.draw(g);
    }


    public Graphics2D getGraphics2D()
    {
        return (Graphics2D) super.getGraphics();
    }


	public boolean isFocusTraversable()
	{
		return false;
	}
    
    
    /**
     * Finds the Edge that covers the coordinate x,y.
     * If no edge is found, null is returned.
     * @param x the x coordinate
     * @param y the x coordinate
     * @return Edge
     */
    private Edge findEdge(int x, int y){
        GraphElement graphElement = null;
        for (Iterator it = graph.getEdges(); it.hasNext(); ){
            graphElement = (GraphElement)it.next();
            if(graphElement.contains(x,y)){
                return (Edge) graphElement;
            }
        }
        return null;
    }
    
    /**
     * Finds the Vertex that covers the coordinate x,y.
     * If no vertex is found, null is returned.
     * @param x the x coordinate
     * @param y the x coordinate
     * @return Vertex
     */
    private Vertex findVertex(int x, int y){
        GraphElement currentGraphElement = null;
        GraphElement topGraphElement = null;
        
        //Try to find a vertex containing the point
        // Rather than breaking when we find the vertex we keep searching
        // which will therefore find the LAST vertex containing the point
        // This turns out to be the vertex which is rendered at the front
        for (Iterator it = graph.getVertices(); it.hasNext();){
            currentGraphElement = (GraphElement)it.next();
            if(currentGraphElement.contains(x,y)){
                topGraphElement = currentGraphElement;
            }
        }
        return (Vertex) topGraphElement;
    }
    
    /**
     * Finds the graphElement that covers the coordinate x,y.
     * If no element is found, null is returned. If a Vertex and an Edge both
     * covers x,y the Vertex will be returned.
     * @param x the x coordinate
     * @param y the x coordinate
     * @return GraphElement 
     */
    private GraphElement findGraphElement(int x, int y){
        GraphElement graphElement = null;
        graphElement = findVertex(x, y);
        if (graphElement != null){
            return graphElement;
        }
        else
        {
            graphElement = findEdge(x,y);
        }
        return graphElement;
       
        
    }


	// ---- KeyListener interface ----
	
    public void keyPressed(KeyEvent evt)
    {
/**  kay activation of popup menu -no ready
    	char key = evt.getKeyChar();
    	switch(key) {
    	case 'm': 
            if(activeVertex != null)
                activeVertex.popupMenu(10, 10, this);
        }
 */
    }
    
    public void keyReleased(KeyEvent evt)
    {
    }
    
    public void keyTyped(KeyEvent evt)
    {
    }
    
    // ---- MouseListener interface ----
	
    public void mouseClicked(MouseEvent evt)
    {
        if(activeGraphElement != null) {
            if(evt.getClickCount() > 1 && ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
                activeGraphElement.doubleClick(evt, evt.getX(), evt.getY(), this);
            }
            else
            activeGraphElement.singleClick(evt, evt.getX(), evt.getY(), this);
        }
    }

    public void mousePressed(MouseEvent evt)
    {
        int x = evt.getX();
        int y = evt.getY();

        activeGraphElement = findGraphElement(x,y);
        graph.setActiveGraphElement(activeGraphElement);
        // Signal the graphElement that it was clicked
        if((activeGraphElement != null) && !evt.isPopupTrigger() &&
            ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
                activeGraphElement.mousePressed(evt, x, y, this);
        }
        //if the graphElement is selectable and it got clicked on a handle,
        //then it is resizing.
        if(activeGraphElement instanceof Selectable)
        {
            Selectable selectable = (Selectable) activeGraphElement;
            if ( selectable.isHandle(x,y) )
            {
               selectable.setResizing(true);
            }
            else
            {
                selectable.setResizing(false);
            }
        }
    }
    
    

    public void mouseReleased(MouseEvent evt)
    {
        if(activeGraphElement != null && ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
            activeGraphElement.mouseReleased(evt, evt.getX(), evt.getY(), this);
        }
    }

    // ---- end of MouseListener interface ----

    // ---- MouseMotionListener interface: ----
	
    public void mouseDragged(MouseEvent evt)
    {
        if (readOnly)
            return;

        if(activeGraphElement != null)
        activeGraphElement.mouseDragged(evt, evt.getX(), evt.getY(), this);
    }
    
   

    public void mouseMoved(MouseEvent evt)
    {
        int x = evt.getX();
        int y = evt.getY();
        GraphElement ge = findGraphElement(x,y);
        if(ge != null)
        {
            //make the mousecursor a hand
            if(ge instanceof Selectable)
			{
			    setCursor(handCursor);
                //are the mouse over a resizeHandle
                if (((Selectable) ge).isHandle(x,y)){
                    setCursor(arrowCursor);
                }
			} 
        }
        else
        {
            //make the mousecursor normal
            setCursor(defaultCursor);
        }
        if(activeGraphElement != null)
        {
            activeGraphElement.mouseMoved(evt, evt.getX(), evt.getY(), this);
        } 
    }

    // ---- end of MouseMotionListener interface ----



    protected void processMouseEvent(MouseEvent evt)
    {
        super.processMouseEvent(evt);

        if(evt.isPopupTrigger() && activeGraphElement != null)
        {
            activeGraphElement.popupMenu(evt.getX(), evt.getY(), this);
        }
    }

    public void setReadOnly(boolean state)
    {
        readOnly = state;
    }

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent e) {}
}
