package bluej.graph;

import java.awt.Point;
import java.util.*;

/**
 * GraphElementManager holds a list of selected graphElements.
 * 
 * @author fisker
 *  
 */
public class GraphElementSet
{
    private List graphElements = new ArrayList();

    /**
     * 
     * @param graphEditor
     */
    public GraphElementSet()
    {}

    /**
     * Add an unselected selectable graphElement to the GraphElementManager and
     * set it's 'selected' flag true. If the graphElement is not implementing
     * Selectable or is already selected, nothing happens.
     * 
     * @param graphElement
     *            a GraphElement implementing Selectable which returns false if
     *            it's 'isSelected' method is called.
     */
    public void add(SelectableGraphElement element)
    {
        if (!element.isSelected()) {
            element.setSelected(true);
            graphElements.add(element);
        }
    }

    /**
     * Move all the elements from another graphElementManager to this one.
     * 
     * @param graphElementManager
     *            the other graphElementManager
     */
    public void moveAll(GraphElementSet graphElementManager)
    {
        GraphElement graphElement;
        for (Iterator i = graphElementManager.graphElements.iterator(); i.hasNext();) {
            graphElement = (GraphElement) i.next();
            i.remove();
            graphElements.add(graphElement);
        }
    }

    /**
     * Remove the graphElement and set it's 'selected' flag false.
     * 
     * @param graphElement
     */
    public void remove(SelectableGraphElement element)
    {
        if (element != null) {
            element.setSelected(false);
        }
        graphElements.remove(element);
    }

    /**
     * Remove all the graphElements from the list. Set each removed grahpElement
     * 'selected' flag to false. Does NOT selfuse remove method.
     */
    public void clear()
    {
        for (Iterator i = graphElements.iterator(); i.hasNext();) {
            SelectableGraphElement element = (SelectableGraphElement) i.next();
            element.setSelected(false);
            i.remove();
        }
    }

    public Iterator iterator()
    {
        return graphElements.iterator();
    }

    /**
     * Get the number of graphElements in this graphElementManager
     * 
     * @return the number of elements
     */
    public int getSize()
    {
        return graphElements.size();
    }

    public Point getMinGhostPosition()
    {
        GraphElement graphElement;
        Moveable moveable;
        int minGhostX = Integer.MAX_VALUE;
        int minGhostY = Integer.MAX_VALUE;

        for (Iterator i = graphElements.iterator(); i.hasNext();) {
            graphElement = (GraphElement) i.next();
            if (graphElement instanceof Moveable) {
                moveable = (Moveable) graphElement;
                if (moveable.getGhostX() < minGhostX) {
                    minGhostX = moveable.getGhostX();
                }
                if (moveable.getGhostY() < minGhostY) {
                    minGhostY = moveable.getGhostY();
                }
            }
        }
        return new Point(minGhostX, minGhostY);
    }
}