package bluej.graph;

import java.awt.event.KeyEvent;
import java.util.Iterator;

/**
 * @author fisker
 * 
 *  
 */
public class TraverseStragegyImpl
    implements TraverseStragegy
{

    private double calcDistance(Vertex vertex1, Vertex vertex2)
    {
        if (vertex1 == null || vertex2 == null) {
            return Double.POSITIVE_INFINITY;
        }
        int x1 = vertex1.getX() + vertex1.getWidth() / 2;
        int y1 = vertex1.getY() + vertex1.getHeight() / 2;
        int x2 = vertex2.getX() + vertex2.getWidth() / 2;
        int y2 = vertex2.getY() + vertex2.getHeight() / 2;
        double d = Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2));
        return d;
    }

    public Vertex findNextVertex(Graph graph, Vertex currentVertex, int key)
    {
        int currentVertexCenterX = currentVertex.getX() + currentVertex.getWidth() / 2;
        int currentVertexCenterY = currentVertex.getY() + currentVertex.getHeight() / 2;
        int x;
        int y;
        Vertex v = null;
        double closest = Double.POSITIVE_INFINITY;
        double currentDistance;
        Vertex closestVertex = null;
        boolean left, right, up, down, notSelf, inRightRegion;
        for (Iterator i = graph.getVertices(); i.hasNext();) {
            v = (Vertex) i.next();
            x = v.getX() + v.getWidth() / 2 - currentVertexCenterX;
            y = v.getY() + v.getHeight() / 2 - currentVertexCenterY;
            left = key == KeyEvent.VK_LEFT && y >= x && y <= -x;
            right = key == KeyEvent.VK_RIGHT && y <= x && y >= -x;
            up = key == KeyEvent.VK_UP && y <= x && y <= -x;
            down = key == KeyEvent.VK_DOWN && y >= x && y >= -x;
            notSelf = currentVertex != v;
            inRightRegion = (left || right || up || down) && notSelf;

            if (inRightRegion) {
                if (closestVertex == null) {
                    closestVertex = v;
                    closest = calcDistance(v, currentVertex);
                }
                if (closest > (currentDistance = calcDistance(v, currentVertex))) {
                    closest = currentDistance;
                    closestVertex = v;
                }

            }

        }
        if (closestVertex == null) {
            closestVertex = currentVertex;
        }
        return closestVertex;
    }
}