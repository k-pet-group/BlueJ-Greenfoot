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
 * @version $Id: GraphEditor.java 2205 2003-10-07 09:35:40Z fisker $
 */
public class GraphEditor extends JComponent
    implements MouseListener, MouseMotionListener, KeyListener
{
    protected static final Color background = Config.getItemColour("colour.graph.background");
    final static Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
    final static Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
    final static Cursor arrowCursor = new Cursor(Cursor.SE_RESIZE_CURSOR);
    private Graph graph;
    private GraphElement activeGraphElement;
    private boolean readOnly = false;
    private Marquee marquee;
    // Contains the elements that has been selected
    private GraphElementManager graphElementManager;
    private int x,y; //coordinates for the last left clicked position
    
    
    public GraphEditor(Graph graph)
    {
        this.graph = graph;
        activeGraphElement = null;
        graphElementManager = new GraphElementManager(this);
        addMouseMotionListener(this);
        marquee = new Marquee(graph,this);
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
        marquee.draw(g);
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

    private boolean isMultiselectionKeyDown(MouseEvent evt){
        int mKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        int modifiers = evt.getModifiersEx();
        /*System.out.println("mKey=" + mKey + " modifiers=" + modifiers + 
                           " CTRL_MASK=" + Event.CTRL_MASK + 
                           " CTRL_DOWN_MASK="+ MouseEvent.CTRL_DOWN_MASK);
                           */
        return evt.isShiftDown();
    }

	// ---- KeyListener interface ----
	
    public void keyPressed(KeyEvent evt)
    {
        
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
            else {
                activeGraphElement.singleClick(evt, evt.getX(), evt.getY(), this);
            }
            if (!isMultiselectionKeyDown(evt)) {
                graphElementManager.clear();
                graphElementManager.add(activeGraphElement);
            }
            
        }
    }
    

    public void mousePressed(MouseEvent evt)
    {
        x = evt.getX();
        y = evt.getY();
        
        
        marquee.start(x, y); 
        activeGraphElement = findGraphElement(x, y);

        if (activeGraphElement == null) {
            if(!isMultiselectionKeyDown(evt)) {
                //the background was clicked and multiselectionKey wasn't down
                graphElementManager.clear();
            }
        }
        else {
            if(isMultiselectionKeyDown(evt)) {
                //a class was clicked, while multiselectionKey was down.
                if(((Selectable)activeGraphElement).isSelected()) {
                    // the clicked class was selected
                    graphElementManager.remove(activeGraphElement);
                }
                else {
                    //the clicked class wasn't selected
                    graphElementManager.add(activeGraphElement);
                }
            }
            else {
                //a class was clicked,while multiselection was up.
                if (!((Selectable)activeGraphElement).isSelected()) {
                    //the class was wasn't selected
                    graphElementManager.clear();
                    graphElementManager.add(activeGraphElement);
                }
            }
            
        }
        
      
        // Signal the graphElement that it was pressed
        if((activeGraphElement != null) && !evt.isPopupTrigger() &&
            ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
                graphElementManager.mousePressed(evt);
        }
        //if the graphElement is selectable and it got clicked on a handle,
        //then it is resizing.
        if(activeGraphElement instanceof Selectable) {
            Selectable selectable = (Selectable) activeGraphElement;
            selectable.setResizing(selectable.isHandle(x, y));
        }
    }
    

    public void mouseReleased(MouseEvent evt)
    {
        marquee.stop();
        graphElementManager.moveAll(marquee.getGraphElementManger());
        
        if (activeGraphElement != null) {
            ((Selectable) activeGraphElement).setResizing(false);
        }
        if(activeGraphElement != null && ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
            graphElementManager.mouseReleased(evt);
        }
    }

    // ---- end of MouseListener interface ----

    // ---- MouseMotionListener interface: ----
	
    public void mouseDragged(MouseEvent evt)
    {
        if (readOnly) return;
        
        if(!evt.isPopupTrigger()&&
        ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
            if(activeGraphElement == null)
            {
                marquee.move(evt.getX(), evt.getY());          
            }
            else {
                graphElementManager.mouseDragged(evt);
            }
        }
    }
    
   

    public void mouseMoved(MouseEvent evt)
    {
        int x = evt.getX();
        int y = evt.getY();
        GraphElement ge = findGraphElement(x,y);
        if(ge != null) {
            //make the mousecursor a hand
            if(ge instanceof Selectable) {
			    setCursor(handCursor);
                //are the mouse over a resizeHandle
                if (((Selectable) ge).isHandle(x,y)) {
                    setCursor(arrowCursor);
                }
			} 
        }
        else {
            //make the mousecursor normal
            setCursor(defaultCursor);
        }
        graphElementManager.mouseMoved(evt);
    }

    // ---- end of MouseMotionListener interface ----



    protected void processMouseEvent(MouseEvent evt)
    {
        super.processMouseEvent(evt);

        if(evt.isPopupTrigger() && activeGraphElement != null) {
            graphElementManager.clear();
            graphElementManager.add(activeGraphElement);
            activeGraphElement.popupMenu(x, y, this);
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
