/**
 ** @version $Id: Vertex.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** General graph vertices
 **/

package bluej.graph;

import java.awt.Graphics;
import java.awt.event.MouseEvent;

public abstract class Vertex
{
    public int x, y;		// position
    public int width, height;	// size

    public Vertex(int x, int y, int width, int height)
    {
	this.x = x;
	this.y = y;
	this.width = width;
	this.height = height;
    }
	
    public void setPos(int x, int y)
    {
	this.x = x;
	this.y = y;
    }
	
    public void setSize(int width, int height)
    {
	this.width = width;
	this.height = height;
    }

    public abstract void draw(Graphics g);

    public void mousePressed(MouseEvent evt, int x, int y, GraphEditor editor) {}

    public void mouseReleased(MouseEvent evt, int x, int y, GraphEditor editor) {}

    public void mouseDragged(MouseEvent evt, int x, int y, GraphEditor editor) {}

    public void mouseMoved(MouseEvent evt, int x, int y, GraphEditor editor) {}

    public void doubleClick(MouseEvent evt, int x, int y, GraphEditor editor) {}
    public void singleClick(MouseEvent evt, int x, int y, GraphEditor editor) {}

    public void popupMenu(MouseEvent evt, int x, int y, GraphEditor editor) {}
}
