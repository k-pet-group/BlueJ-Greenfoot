package bluej.graph;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Package;

import java.util.Enumeration;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 ** @version $Id: GraphEditor.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** Canvas to allow editing of general graphs
 **/

public class GraphEditor extends JComponent 

    implements MouseListener, MouseMotionListener
{
    static final int DEFAULT_WIDTH = 400;
    static final int DEFAULT_HEIGHT = 400;
    static final long DBL_CLICK_TIME = 300;		// milliseconds
    static final Color background = Config.getItemColour("colour.background");
    static final Color realBackground = Config.getItemColour("colour.graph.background");
    private Graph graph;
    PkgMgrFrame frame;
    Vertex activeVertex;
    boolean motionListening;

    private boolean readOnly = false;
	
    public GraphEditor(Graph graph, PkgMgrFrame frame)
    {
	setGraph(graph);
	this.frame = frame;
	addMouseListener(this);
	motionListening = false;

	setBackground(background);

	setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
	
    /**
     * Return the PkgMgrFrame containing this editor.
     */
    public PkgMgrFrame getFrame() {
	return this.frame;
    }
	
    public void setGraph(Graph graph)
    {
	this.graph = graph;
	this.graph.setEditor(this);
	activeVertex = null;
    }
	
    public Dimension getPreferredSize()
    {
	return graph.getMinimumSize();
    }

    public Dimension getMinimumSize()
    {
	return graph.getMinimumSize();
    }
	
    public void update(Graphics g)
    {
	paint(g);
    }
	
    public void paint(Graphics g)
    {
	if(!(g instanceof PrintGraphics)) {
	    Dimension d = getSize();
	    g.setColor(realBackground);
	    g.fillRect(0, 0, d.width, d.height);
	}
		
	graph.draw(g);
    }

    public void mousePressed(MouseEvent evt)
    {
	if (frame != null)
	    frame.clearStatus();

	int x = evt.getX();
	int y = evt.getY();

	activeVertex = null;

	// Try to find a vertex containing the point
	for(Enumeration e = graph.getVertices(); e.hasMoreElements(); ) {
	    Vertex v = (Vertex)e.nextElement();

	    if((v.x <= x) && (x < v.x + v.width) && (v.y <= y) && (y < v.y + v.height)) {
		activeVertex = v;
		break;
	    }
	}

	graph.setActiveVertex(activeVertex);
		
	if((activeVertex != null) && !isPopupEvent(evt)) {
	    activeVertex.mousePressed(evt, x, y, this);
	    if (!motionListening) {
		addMouseMotionListener(this);
		motionListening = true;
	    }
	}
    }

    public void mouseReleased(MouseEvent evt)
    {
	if(activeVertex != null) {
	    activeVertex.mouseReleased(evt, evt.getX(), evt.getY(), this);
	    if ((frame.getPackage().getState() != Package.S_CHOOSE_USES_TO) &&
		(frame.getPackage().getState() != Package.S_CHOOSE_EXT_TO)) {
		// if we're not choosing anymore, remove listener
		removeMouseMotionListener(this);
		motionListening = false;
	    }
	}
	else {
	    if (motionListening) {
		removeMouseMotionListener(this);
		motionListening = false;
		frame.getPackage().setState(Package.S_IDLE);
		repaint();
	    }
	}
    }
	
    public void mouseClicked(MouseEvent evt)
    {
	if(activeVertex != null) {
	    if(evt.getClickCount() > 1)
		activeVertex.doubleClick(evt, evt.getX(), evt.getY(), this);
	    else
		activeVertex.singleClick(evt, evt.getX(), evt.getY(), this);
		    
	}
    }
	
    public void mouseEntered(MouseEvent evt) {}
    public void mouseExited(MouseEvent evt) {}
	
    public void mouseDragged(MouseEvent evt) { 
	if (readOnly)
	    return;
		
	if(activeVertex != null)
	    activeVertex.mouseDragged(evt, evt.getX(), evt.getY(), this);
    }
	
    public void mouseMoved(MouseEvent evt) 
    {
	if(activeVertex != null)
	    activeVertex.mouseMoved(evt, evt.getX(), evt.getY(), this);
    }

    protected void processMouseEvent(MouseEvent evt)
    {
	super.processMouseEvent(evt);
		
	if (isPopupEvent(evt))
	    if((activeVertex != null))
		activeVertex.popupMenu(evt, evt.getX(), evt.getY(), this);
    }
	
    private boolean isPopupEvent(MouseEvent evt)
    {
	return evt.isPopupTrigger()
	    || ((evt.getID() == MouseEvent.MOUSE_PRESSED) && evt.isControlDown());
    }
	
    public void setReadOnly(boolean state) {
	readOnly = state;
    }
}
