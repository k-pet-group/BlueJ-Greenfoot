package bluej.graph;

import java.awt.Point;
import java.util.*;


/**
 * GraphElementManager holds a list of selected graphElements.
 * @author fisker
 *
 */
class GraphElementManager 
{
    private GraphEditor graphEditor;
    private List graphElements = new LinkedList();
    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    
    
    /**
     * 
     * @param graphEditor
     */
    public GraphElementManager(GraphEditor graphEditor)
    {
        this.graphEditor = graphEditor;
    }

    /**
     * Add an unselected selectable graphElement to the GraphElementManager and 
     * set it's 'selected' flag true.
     * If the graphElement is not implementing Selectable or is already 
     * selected, nothing happens.  
     * @param graphElement a GraphElement implementing Selectable which 
     * returns false if it's 'isSelected' method is called.
     */
    public void add(GraphElement graphElement){
        if (graphElement instanceof Selectable && !((Selectable)graphElement).isSelected()){
            ((Selectable)graphElement).setSelected(true);
            graphElements.add(graphElement);
        }
        if ( graphElement instanceof Vertex){
            Vertex vertex = (Vertex) graphElement;
            if (vertex.getX() < minX){
                minX = vertex.getX();
            }
            if (vertex.getY() < minY){
                minY = vertex.getY();
            }
        }
    }
    
    /**
     * Move all the elements from another graphElementManager to this one.
     * @param graphElementManager the other graphElementManager
     */
    public void moveAll(GraphElementManager graphElementManager){
        GraphElement graphElement;
        for(Iterator i=graphElementManager.graphElements.iterator();i.hasNext();){
            graphElement = (GraphElement) i.next();
            i.remove();
            graphElements.add(graphElement);
        }
        findMin();
    }
    
    /**
     * Remove the graphElement and set it's 'selected' flag false.
     * @param graphElement
     */
    public void remove(GraphElement graphElement){
        if (graphElement!=null && graphElement instanceof Selectable){
            ((Selectable)graphElement).setSelected(false);
        }
        graphElements.remove(graphElement);
        findMin();
    }
    
    /**
     * Remove all the graphElements from the list. 
     * Set each removed grahpElement 'selected' flag to false.
     * Does NOT selfuse remove method.
     */
    public void clear(){
        GraphElement graphElement;
        for(Iterator i=graphElements.iterator();i.hasNext();){
            graphElement = (GraphElement) i.next();
            if(graphElement instanceof Selectable){
                ((Selectable)graphElement).setSelected(false);
            } 
            i.remove();
        }
        minX = Integer.MAX_VALUE;
        minY = Integer.MAX_VALUE;
    }
    
    public Iterator iterator(){
        return graphElements.iterator();
    }
    
    
    /**
     * Get the number of graphElements in this graphElementManager
     * @return the number of elements
     */
    public int getSize(){
        return graphElements.size();
    }
    
    public Point getMinPosition(){
        return new Point(minX, minY);
    }
    
    private void findMin(){
        GraphElement graphElement;
        Vertex vertex;
        
        for(Iterator i=graphElements.iterator();i.hasNext();){
            graphElement = (GraphElement) i.next();
            if(graphElement instanceof Vertex){
                vertex = (Vertex) graphElement;
                if (vertex.getX() < minX){
                    minX = vertex.getX();
                }
                if (vertex.getY() < minY){
                    minY = vertex.getY();
                }
            } 
        }
    }
}
