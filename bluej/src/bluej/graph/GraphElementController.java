package bluej.graph;

import java.awt.event.MouseEvent;
import java.util.Iterator;


public class GraphElementController
{
    GraphEditor graphEditor;
    private static int dragStartX; 
    private static int dragStartY;
    
    
    public GraphElementController(GraphEditor graphEditor){
        this.graphEditor = graphEditor;
    }
    
    /**
     * Invoke 'mousePressed' on all the GraphElements in the list
     * @param evt the mouseEvent
     */
    public void mousePressed(MouseEvent evt){
        dragStartX = evt.getX();
        dragStartY = evt.getY();
        GraphElement graphElement;
        
        for(Iterator i=graphEditor.getGraphElementManager().iterator(); i.hasNext();){
            graphElement = (GraphElement) i.next();
            graphElement.mousePressed(evt, graphEditor);
        }
    }
    
  
    public void mouseDragged(MouseEvent evt){
        GraphElement graphElement = null;
        int deltaX = evt.getX() - dragStartX;
        int deltaY = evt.getY() - dragStartY;
        
        
        boolean isMoveAllowed = true;
        
        for(Iterator i=graphEditor.getGraphElementManager().iterator(); i.hasNext();){
            graphElement = (GraphElement) i.next();
            if (graphElement instanceof Vertex){
                Vertex vertex = (Vertex) graphElement;
                if (vertex.getX() + deltaX < 0 || vertex.getY() + deltaY < 0){
                    isMoveAllowed = false;
                }
            }
        }
        
        for(Iterator i=graphEditor.getGraphElementManager().iterator(); i.hasNext() && isMoveAllowed;){
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
        for(Iterator i=graphEditor.getGraphElementManager().iterator(); i.hasNext();){
            graphElement = (GraphElement) i.next();
            graphElement.mouseMoved(evt, graphEditor);
        }
    }
    
    

    /**
     * Invoke 'mouseReleased' on all the GraphElements in the list
     * @param evt the mouseEvent
     */
    public void mouseReleased(MouseEvent evt){
        System.out.println("##");
        GraphElement graphElement;
       
        for(Iterator i=graphEditor.getGraphElementManager().iterator(); i.hasNext();){
            graphElement = (GraphElement) i.next();
            graphElement.mouseReleased(evt, graphEditor);
        }
    }
    

    
    public void mouseClicked(MouseEvent evt){
        
    }
    
    public void popupMenu(){
        
    }
    
    public void doubleClick(MouseEvent evt){
        
    }
    
    public void singleClick(MouseEvent evt){
        
    }
    
}
