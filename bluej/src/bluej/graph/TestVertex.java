package bluej.graph;

import bluej.utility.Utility;
import java.awt.Graphics;

/**
 ** @version $Id: TestVertex.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 **
 ** Test graph vertices
 **/

public class TestVertex extends Vertex
{
	public TestVertex()
	{
		super(Utility.getRandom(0, 400), Utility.getRandom(0, 400), 40, 25);
	}

	public void draw(Graphics g)
	{
		g.drawRect(x - width / 2, y - height / 2, width, height);
	}
}
