package bluej.graph;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Package;

import java.util.Enumeration;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Canvas to allow editing of general graphs
 *
 * @author  Michael Cahill
 * @version $Id: GraphEditor.java 519 2000-05-31 04:05:07Z ajp $
 */
public class GraphEditor extends JComponent
    implements MouseListener, MouseMotionListener
{
    static final long DBL_CLICK_TIME = 300;		// milliseconds
    static final Color background = Config.getItemColour("colour.background");
    static final Color realBackground = Config.getItemColour("colour.graph.background");
    private Graph graph;
    private Vertex activeVertex;
    private boolean motionListening;

    private boolean readOnly = false;

    public GraphEditor(Graph graph)
    {
        setGraph(graph);
        addMouseListener(this);
        motionListening = false;

        setBackground(background);
    }

    public void setGraph(Graph graph)
    {
        this.graph = graph;
        this.graph.editor = this;
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

    public void paint(Graphics g)
    {
        if(!(g instanceof PrintGraphics)) {
            Dimension d = getSize();
            g.setColor(realBackground);
            g.fillRect(0, 0, d.width, d.height);
        }

        graph.draw(g);
    }

    public Graphics2D getGraphics2D()
    {
        return (Graphics2D) super.getGraphics();
    }

    public void mousePressed(MouseEvent evt)
    {
//XXX	if (frame != null)
//	    frame.clearStatus();

        int x = evt.getX();
        int y = evt.getY();

        activeVertex = null;

        // Try to find a vertex containing the point
        // Rather than breaking when we find the vertex we keep searching
        // which will therefore find the LAST vertex containing the point
        // This turns out to be the vertex which is rendered at the front
        for(Enumeration e = graph.getVertices(); e.hasMoreElements(); ) {
            Vertex v = (Vertex)e.nextElement();

            if((v.x <= x) && (x < v.x + v.width) && (v.y <= y) && (y < v.y + v.height)) {
                activeVertex = v;
            }
        }

        graph.setActiveVertex(activeVertex);

        if((activeVertex != null) && !isPopupEvent(evt) &&
            ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
            activeVertex.mousePressed(evt, x, y, this);
            if (!motionListening) {
                startMotionListening();
            }
        }
    }

    public void startMotionListening()
    {
		addMouseMotionListener(this);
		motionListening = true;
    }

    public void stopMotionListening()
    {
        // if we're not choosing anymore, remove listener
        removeMouseMotionListener(this);
        motionListening = false;
    }

    public void mouseReleased(MouseEvent evt)
    {
        if(activeVertex != null && ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
            activeVertex.mouseReleased(evt, evt.getX(), evt.getY(), this);

/* XXX            if ((frame.getPackage().getState() != Package.S_CHOOSE_USES_TO) &&
                (frame.getPackage().getState() != Package.S_CHOOSE_EXT_TO)) { */

            if (motionListening) {
                stopMotionListening();
/*XXX                frame.getPackage().setState(Package.S_IDLE);
                repaint(); */
            }
        }
    }

    public void mouseClicked(MouseEvent evt)
    {
        if(activeVertex != null) {
            if(evt.getClickCount() > 1 && ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
                activeVertex.doubleClick(evt, evt.getX(), evt.getY(), this);
            }
            else
                activeVertex.singleClick(evt, evt.getX(), evt.getY(), this);
        }
    }

    public void mouseEntered(MouseEvent evt) {}
    public void mouseExited(MouseEvent evt) {}

    public void mouseDragged(MouseEvent evt)
    {
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

    public void setReadOnly(boolean state)
    {
        readOnly = state;
    }
}
