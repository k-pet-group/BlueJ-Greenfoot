package bluej.graph;

import java.awt.*;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import bluej.pkgmgr.target.*;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.Package;


public class GraphElementController
{
    GraphEditor graphEditor;
    private static int dragStartX; 
    private static int dragStartY;
    public static DependentTarget dependTarget;
    public static int dependencyArrowX;
    public static int dependencyArrowY;
    
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
            handleMousePressed(evt, graphElement, graphEditor);
        }
    }
    
  
    public void mouseDragged(MouseEvent evt){
        GraphElement graphElement = null;
        int deltaX = evt.getX() - dragStartX;
        int deltaY = evt.getY() - dragStartY;
        
        boolean isMoveAllowedX = true;
        boolean isMoveAllowedY = true;
        
        int tPointX = 0;
        int tPointY = 0;
        for(Iterator i=graphEditor.getGraphElementManager().iterator(); i.hasNext();){
            graphElement = (GraphElement) i.next();
            if (graphElement instanceof Vertex){
                Vertex vertex = (Vertex) graphElement;
                if (vertex.getX() + deltaX < 0){
                    isMoveAllowedX = false;
                }                
                if (vertex.getY() + deltaY < 0){
                    isMoveAllowedY = false;
                }
            }
        }
        
        if(!isMoveAllowedX){
            tPointX = - evt.getX() + dragStartX - graphEditor.getGraphElementManager().getMinPosition().x;
        }
        
        if(!isMoveAllowedY){
            tPointY = -evt.getY() + dragStartY - graphEditor.getGraphElementManager().getMinPosition().y;
        }
        evt.translatePoint(tPointX, tPointY);
        for(Iterator i=graphEditor.getGraphElementManager().iterator(); i.hasNext();graphElement = (GraphElement) i.next()){
            handleMouseDragged(evt, graphElement, graphEditor);
        }
    }
    
    
    
    
    /**
     * Invoke 'mouseMoved' on all the GraphElements in the list
     * @param evt the mouseEvent
     */
    public void mouseMoved(MouseEvent evt){
        /*GraphElement graphElement;
        for(Iterator i=graphEditor.getGraphElementManager().iterator(); i.hasNext();){
            graphElement = (GraphElement) i.next();
           
        }*/
    }
    
    

    /**
     * Invoke 'mouseReleased' on all the GraphElements in the list
     * @param evt the mouseEvent
     */
    public void mouseReleased(MouseEvent evt){
        GraphElement graphElement;
       
        for(Iterator i=graphEditor.getGraphElementManager().iterator(); i.hasNext();){
            graphElement = (GraphElement) i.next();
            handleMouseReleased(evt, graphElement, graphEditor);
        }
    }
    
    private void handleMousePressed(MouseEvent evt, GraphElement graphElement, GraphEditor graphEditor){
        if(graphElement instanceof Target){
            Target target = (Target)graphElement;
            
            if(target.getPackage().getState() != Package.S_IDLE) {
                target.getPackage().targetSelected(target);
            }
            target.oldRect = new Rectangle(target.getX(), target.getY(), 
                    target.getWidth(), target.getHeight() );
        }
        if(graphElement instanceof ClassTarget){
            dependencyArrowX = evt.getX();
            dependencyArrowY = evt.getY();
        }
        
    }
    
    private void handleMouseDragged(MouseEvent evt, GraphElement graphElement, 
            						GraphEditor graphEditor){
        boolean isClassTarget = graphElement instanceof ClassTarget;
        if(isClassTarget && isStateDrawingDependency((ClassTarget)graphElement)){
            ClassTarget classTarget = (ClassTarget) graphElement;
            dependTarget = classTarget;
            // Draw a line from this Target to the current Cursor position
            dependencyArrowX = evt.getX();
            dependencyArrowY = evt.getY();
        }else if (graphElement instanceof Target){
            Target target = (Target) graphElement;
            handleMouseDraggedTarget(evt, target);
        }
        graphEditor.repaint();
    }
    
    
    /**
     * @param evt
     * @param classTarget
     */
    private void handleMouseDraggedTarget(MouseEvent evt, Target target) {
        int deltaX = evt.getX() - dragStartX;
        int deltaY = evt.getY() - dragStartY;
        Point p = GraphEditor.snapToGrid( new Point(deltaX, deltaY) );
        deltaX = (int) p.getX();
        deltaY = (int) p.getY();
        target.setIsMoving(!target.isResizing()); // if this class is clicked and dragged
        						  // and isn't resizing, it must be moving.
        //TODO I don't like "if this class is clicked and dragged and isn't resizing, it must be moving.
        if (target.isMoving()) {	        
            target.ghostX = target.getX() + deltaX;
            target.ghostY = target.getY() + deltaY;
        }
        else if(target.isResizable()) {// Then we're resizing
            int origWidth = (int) target.oldRect.getWidth();
            int origHeight = (int) target.oldRect.getHeight();
            target.setSize(origWidth + deltaX, origHeight + deltaY);
        }
    }
    
    
    
    private void handleMouseReleased(MouseEvent evt, GraphElement graphElement, GraphEditor graphEditor){
        if(graphElement instanceof Target){
            Target target = (Target)graphElement;
            
            if (target.isMoving()) {
                target.setPos(target.getGhostX(), target.getGhostY());
                target.endMove();
            }
            
            Rectangle newRect = new Rectangle(target.getX(), target.getY(), 
                    						  target.getWidth(),
                    						  target.getHeight());  
            
            if(!newRect.equals(target.oldRect)) {
                graphEditor.revalidate();
                graphEditor.repaint();
                
            }
        }
        if(graphElement instanceof ClassTarget){
            ClassTarget classTarget = (ClassTarget) graphElement;
            classTarget.handleMoveAndResizing();
            if (isStateDrawingDependency(classTarget)){
                if (!classTarget.contains(evt.getX(), evt.getY())) {
                    handleNewDependencies(evt, classTarget);
                    dependencyArrowX = 0;
                    dependencyArrowY = 0;
    		        graphEditor.repaint();
                } 
                classTarget.getPackage().setState(Package.S_IDLE); 
            }
            //nobody is drawing dependencies now
            dependTarget = null;
        }
    }
    
    /**
     * If the user release the mouse over a target, it may mean that a new
     * dependency should be created.
     * @param evt
     */
    public void handleNewDependencies(MouseEvent evt, ClassTarget classTarget) {
        //are we adding a dependency arrow
        if (isStateDrawingDependency(classTarget)) {
            // What target is this pointing at now?
            Target overClass = null;
            for(Iterator it = classTarget.getPackage().getVertices(); overClass == null && it.hasNext(); ) {
                Target v = (Target)it.next();

                if (v.contains(evt.getX(), evt.getY())){
                    overClass = v;
                }
            }
            if (overClass != null && overClass != classTarget) {
                classTarget.getPackage().targetSelected(overClass);
            }
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
    
    public boolean isStateDrawingDependency(Target target) {
        return (target.getPackage().getState() == Package.S_CHOOSE_USES_TO) ||
        	   (target.getPackage().getState() == Package.S_CHOOSE_EXT_TO);
    }
    
}
