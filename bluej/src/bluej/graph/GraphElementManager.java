package bluej.graph;

import java.awt.event.MouseEvent;
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
    }
    
    /**
     * Invoke 'mouseDragged' on all the GraphElements in the list
     * @param evt
     */
    public void mouseDragged(MouseEvent evt){
        GraphElement graphElement = null;
        int x = evt.getX();
        int y = evt.getY();
        for(Iterator i=graphElements.iterator(); i.hasNext();){
            graphElement = (GraphElement) i.next();
            graphElement.mouseDragged(evt, graphEditor);
        }
    }
    
    
    /**
     * Invoke 'mouseMoved' on all the GraphElements in the list
     * @param evt the mouseEvent
     */
    public void mouseMoved(MouseEvent evt){
        GraphElement graphElement;
        int x = evt.getX();
        int y = evt.getY();
        for(Iterator i=graphElements.iterator(); i.hasNext();){
            graphElement = (GraphElement) i.next();
            graphElement.mouseMoved(evt, graphEditor);
        }
    }
    
    /**
     * Invoke 'mousePressed' on all the GraphElements in the list
     * @param evt the mouseEvent
     */
    public void mousePressed(MouseEvent evt){
        GraphElement graphElement;
        int x = evt.getX();
        int y = evt.getY();
        for(Iterator i=graphElements.iterator(); i.hasNext();){
            graphElement = (GraphElement) i.next();
            graphElement.mousePressed(evt, graphEditor);
        }
    }

    /**
     * Invoke 'mouseReleased' on all the GraphElements in the list
     * @param evt the mouseEvent
     */
    public void mouseReleased(MouseEvent evt){
        GraphElement graphElement;
        int x = evt.getX();
        int y = evt.getY();
        for(Iterator i=graphElements.iterator(); i.hasNext();){
            graphElement = (GraphElement) i.next();
            graphElement.mouseReleased(evt, graphEditor);
        }
    }
    
    /**
     * Get the number of graphElements in this graphElementManager
     * @return the number of elements
     */
    public int getSize(){
        return graphElements.size();
    }
}
