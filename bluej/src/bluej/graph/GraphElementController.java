package bluej.graph;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.graphPainter.GraphPainterStdImpl;
import bluej.pkgmgr.target.*;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.Package;


public class GraphElementController
{
    GraphEditor graphEditor;
    Package pkg;
    private static int dragStartX; 
    private static int dragStartY;
    public static DependentTarget dependTarget;
    public static int dependencyArrowX;
    public static int dependencyArrowY;
    
    private Target currentTarget;
    private List dependencies;
    private Dependency currentDependency;
    private int currentDependencyIndex;
    
    private boolean isMoveAllowedX;
    private boolean isMoveAllowedY;
    int deltaX;
    int deltaY;
    
    private TraverseStragegy traverseStragegiImpl = new TraverseStragegyImpl();
    
    public GraphElementController(GraphEditor graphEditor, Package pkg){
        this.graphEditor = graphEditor;
        this.pkg = pkg;
    }
    
    public void setActiveGraphElement(GraphElement graphElement){
    	if (graphElement instanceof Target){
    		this.currentTarget = (Target) graphElement;
    	}
    }
    
    /**
     * Invoke 'mousePressed' on all the GraphElements in the list
     * @param evt the mouseEvent
     */
    public void mousePressed(MouseEvent evt){
        dragStartX = evt.getX();
        dragStartY = evt.getY();
        GraphElement graphElement;
        
        for(Iterator i = graphEditor.getGraphElementManager().iterator(); i.hasNext();){
            graphElement = (GraphElement) i.next();
            handleMousePressed(evt, graphElement, graphEditor);
        }
    }
    
    
    public void mouseDragged(MouseEvent evt)
    {
        GraphElement graphElement = null;
        deltaX = evt.getX() - dragStartX;
        deltaY = evt.getY() - dragStartY;
        Point p = GraphEditor.snapToGrid(new Point(deltaX, deltaY));
        deltaX = (int) p.getX();
        deltaY = (int) p.getY();
        
        isMoveAllowed(deltaX, deltaY);
        
        for(Iterator i=graphEditor.getGraphElementManager().iterator(); i.hasNext(); ) {
            graphElement = (GraphElement) i.next();
            handleMouseDragged(evt, graphElement, graphEditor);
        }
    }
    
    
    /**
     * Update the attributes isMoveAllowedX and isMoveAllowedY. isMoveAllowed
     * uses deltaX and deltaY, which is the move to be evaluated, and looks
     * at the selection and checks whether or not the move is still in the
     * diagram.
     * @param int deltaX, int deltaY
     * @return
     */
    private void isMoveAllowed(int deltaX, int deltaY) {
        GraphElement graphElement = null;
        
        isMoveAllowedX = true;
        isMoveAllowedY = true;
        
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
    /**
     * Takes care of dependency drawing.
     * @param evt
     * @param graphElement
     * @param graphEditor
     */
    private void handleMouseDragged(MouseEvent evt, GraphElement graphElement, 
            						GraphEditor graphEditor)
    {
        boolean isClassTarget = graphElement instanceof ClassTarget;
        if(isClassTarget && isStateDrawingDependency((ClassTarget)graphElement)) {
            ClassTarget classTarget = (ClassTarget) graphElement;
            dependTarget = classTarget;
            // Draw a line from this Target to the current Cursor position
            dependencyArrowX = evt.getX();
            dependencyArrowY = evt.getY();
        }
        else if (graphElement instanceof Target) {
            Target target = (Target) graphElement;
            handleMouseDraggedTarget(target);
        }
        graphEditor.repaint();
    }
    
    
    /**
     * Takes care of moveing and resizeing targets
     * @param evt
     * @param classTarget
     */
    private void handleMouseDraggedTarget(Target target) {
        Moveable moveableTarget;
        if (target instanceof Moveable){
            moveableTarget = (Moveable) target;
            if (moveableTarget.isMoveable()){
	            moveableTarget.setIsMoving(!target.isResizing()); // if this class is clicked and dragged
				  // and isn't resizing, it must be moving.
	            //TODO I don't like "if this class is clicked and dragged and isn't resizing, it must be moving.
	            if (moveableTarget.isMoving()&& isMoveAllowedX) {	        
	                moveableTarget.setGhostX( target.getX() + deltaX );
	            }
	            else{
	                int minGhostX = graphEditor.getGraphElementManager().getMinGhostPosition().x;
	                moveableTarget.setGhostX(moveableTarget.getGhostX()- minGhostX);
	                // int minGhostX = graphEditor.getGraphElementManager().getMinGhostPosition().x;
	                // int minGhostY = graphEditor.getGraphElementManager().getMinGhostPosition().y;
	            }
	            if (moveableTarget.isMoving() && isMoveAllowedY){
	                moveableTarget.setGhostY( target.getY() + deltaY );
	            }
	            else{
	                int minGhostY = graphEditor.getGraphElementManager().getMinGhostPosition().y;
	                moveableTarget.setGhostY(moveableTarget.getGhostY()- minGhostY);
	            }
            }
        
        	if(target.isResizable() && target.isResizing()) {// Then we're resizing
        	    int origWidth = (int) target.oldRect.getWidth();
        	    int origHeight = (int) target.oldRect.getHeight();
        	    target.setSize(origWidth + deltaX, origHeight + deltaY);
        	}
        }
    }
    
    
    
    private void handleMouseReleased(MouseEvent evt, GraphElement graphElement, GraphEditor graphEditor){
        if(graphElement instanceof Moveable){
            Target target = (Target)graphElement;
            Moveable moveable = (Moveable) graphElement;
            
            if (moveable.isMoving()) {
                target.setPos(moveable.getGhostX(), moveable.getGhostY());
                moveable.setIsMoving(false);
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
            classTarget.updateAssociatePosition();
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
    
    /**
     * Is the package to which target belongs drawing a dependency.
     * @param target
     * @return true if yes
     */
    public static boolean isStateDrawingDependency(Target target) {
        return (target.getPackage().getState() == Package.S_CHOOSE_USES_TO) ||
        	   (target.getPackage().getState() == Package.S_CHOOSE_EXT_TO);
    }

    
    private static boolean isArrowKey(KeyEvent evt)
    {
    	    return evt.getKeyCode() == KeyEvent.VK_UP ||
    	           evt.getKeyCode() == KeyEvent.VK_DOWN ||
    	           evt.getKeyCode() == KeyEvent.VK_LEFT ||
    	           evt.getKeyCode() == KeyEvent.VK_RIGHT;
    }
    
    
    /**
     * @param evt
     */
    public void keyPressed(KeyEvent evt) 
    {
        //init dependencies
        if (currentTarget instanceof DependentTarget){
            dependencies = ((DependentTarget)currentTarget).dependentsAsList();                 
        } else {
            dependencies = new LinkedList();//dummy empty list
        }

        if(isArrowKey(evt)) {
            if(evt.isControlDown()) {       //resizing
                resizeFreely(evt);
            }
            else if(evt.isShiftDown()) {    //moving targets
                moveTargets(evt);
            }
            else {                          //navigate the diagram
                navigate(evt);
            }
        }
        
        boolean isPlusOrMinus = evt.getKeyCode() == KeyEvent.VK_PLUS ||
                                evt.getKeyCode() == KeyEvent.VK_MINUS;  
        if (isPlusOrMinus){
            resizeWithFixedRatio(evt);
        }
       
        //dependency selection
        if(evt.getKeyCode() == KeyEvent.VK_PAGE_UP || 
           evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN){   
            selectDependency(evt);
        }
        
        //context menu
        if (evt.getKeyCode() == KeyEvent.VK_SPACE ||
            evt.getKeyCode()== KeyEvent.VK_ENTER){  
            showPopupMenu();
        }
        
        //Escape removes selections
        if (evt.getKeyCode() == KeyEvent.VK_ESCAPE){
            undoSelection();
        }
        
        evt.consume();
        graphEditor.repaint();
    }
    
    
    /**
	 * @param evt
	 */
	private void navigate(KeyEvent evt) 
    {
	    if(currentTarget == null) {
			currentTarget = (Target) graphEditor.findSingleVertex();
		}
		currentTarget = (Target) traverseStragegiImpl.findNextVertex(pkg, currentTarget, evt.getKeyCode());
		graphEditor.getGraphElementManager().clear();
		graphEditor.getGraphElementManager().add(currentTarget);
		
		currentDependencyIndex = 0;
		if(currentDependency != null) {
		    graphEditor.getGraphElementManager().remove(currentDependency);
		    currentDependency = null;
		}
	}
    
	
	/**
	 * @param evt
	 */
	private void moveTargets(KeyEvent evt) {
		if (currentTarget instanceof Moveable){
			Moveable moveableTarget = (Moveable) currentTarget;
		    if (moveableTarget.isMoveable()){
		        int deltaX = moveableTarget.getGhostX() - currentTarget.getX() - GraphEditor.GRID_SIZE;
		        int deltaY = moveableTarget.getGhostY() - currentTarget.getY() - GraphEditor.GRID_SIZE;
		        isMoveAllowed(deltaX, deltaY);
		        switch (evt.getKeyCode()){
		            case KeyEvent.VK_UP:{
		                if (isMoveAllowedY){
		                    moveableTarget.setGhostY(moveableTarget.getGhostY() - GraphEditor.GRID_SIZE);
		                    moveableTarget.setIsMoving(true);
		                }
		                break;
		            }
		            case KeyEvent.VK_DOWN:{
		                moveableTarget.setGhostY(moveableTarget.getGhostY() + GraphEditor.GRID_SIZE);
		                moveableTarget.setIsMoving(true);
		                break;
		            }
		            case KeyEvent.VK_LEFT:{
		                if (isMoveAllowedX){
		                    moveableTarget.setGhostX(moveableTarget.getGhostX() - GraphEditor.GRID_SIZE);
		                    moveableTarget.setIsMoving(true);
		                }
		                break;
		            }
		            case KeyEvent.VK_RIGHT:{
		                moveableTarget.setGhostX(moveableTarget.getGhostX() + GraphEditor.GRID_SIZE);
		                moveableTarget.setIsMoving(true);
		                break;
		            }
		        }
		        graphEditor.repaint();
		    }
		}
	}


	/**
	 * @param evt
	 */
	private void resizeWithFixedRatio(KeyEvent evt) {
		if (currentTarget != null && currentTarget.isResizable()){
			int delta = (evt.getKeyCode() == KeyEvent.VK_PLUS ? 10 : -10);
			currentTarget.setSize(currentTarget.getWidth() + delta,
								  currentTarget.getHeight() + delta);
		}
	}

	
	/**
	 * @param evt
	 */
	private void resizeFreely(KeyEvent evt) {
		if (currentTarget != null && currentTarget.isResizable()){
		    switch (evt.getKeyCode()){
		        case KeyEvent.VK_UP:{
		            currentTarget.setSize(currentTarget.getWidth(), currentTarget.getHeight() - 10);
		            break;
		        }
		        case KeyEvent.VK_DOWN:{
		            currentTarget.setSize(currentTarget.getWidth(), currentTarget.getHeight() + 10);
		            break;
		        }
		        case KeyEvent.VK_LEFT:{
		            currentTarget.setSize(currentTarget.getWidth() - 10, currentTarget.getHeight());
		            break;
		        }
		        case KeyEvent.VK_RIGHT:{
		            currentTarget.setSize(currentTarget.getWidth() + 10, currentTarget.getHeight());
		            break;
		        }
		    }
		}
	}
	
	
	/**
	 * @param evt
	 */
	private void selectDependency(KeyEvent evt) {
		if (currentTarget instanceof DependentTarget){
		    currentDependency = (Dependency) dependencies.get(currentDependencyIndex);
		    if(currentDependency != null) {
		        graphEditor.getGraphElementManager().remove(currentDependency);
		    }
		    currentDependencyIndex +=(evt.getKeyCode() == KeyEvent.VK_PAGE_UP ? 1 : -1);
		    currentDependencyIndex %= dependencies.size();
		    if (currentDependencyIndex < 0) {//% is not a real modulo
		        currentDependencyIndex = dependencies.size() - 1;
		    }
		    currentDependency = (Dependency) dependencies.get(currentDependencyIndex);
		    if(currentDependency != null){
		        graphEditor.getGraphElementManager().add(currentDependency);
		    }
		}
	}

	
	/**
	 * 
	 */
	private void showPopupMenu() {
		if (currentDependency != null){
		    int x,y;
		    x = currentDependency.getTo().getX();
		    y = currentDependency.getTo().getY();
		    Point p = ((GraphPainterStdImpl)GraphPainterStdImpl.getInstance()).getDependencyPainter(currentDependency).getPopupMenuPosition(currentDependency);
		    currentDependency.popupMenu(p.x, p.y, graphEditor);
		}
		else if(currentTarget != null) {
		    int x,y;
		    x = currentTarget.getX() + 10;
		    y = currentTarget.getY() + currentTarget.getHeight() - 10;
		    currentTarget.popupMenu(x, y, graphEditor);
		}
	}
	
	
	/**
	 * 
	 */
	private void undoSelection() {
		if (currentDependency != null){
		     graphEditor.getGraphElementManager().remove(currentDependency);
		     currentDependency = null;
		} else {
		    graphEditor.getGraphElementManager().clear();
		    currentTarget = null;
		}
	}

	
	/**
     * @param evt
     */
    public void keyReleased(KeyEvent evt) {
        if (currentTarget == null){
            return;
        }
        
        if(currentTarget instanceof Moveable){
            Moveable moveable = (Moveable) currentTarget;
        
	        if (!evt.isShiftDown() && moveable.isMoving()) {
	            currentTarget.setPos(moveable.getGhostX(), moveable.getGhostY());
		        
	        }
	        
	        if (currentTarget instanceof ClassTarget){
	            ((ClassTarget)currentTarget).updateAssociatePosition();
	        }
	        
        }
        graphEditor.revalidate();
        graphEditor.repaint();
 
        if (currentTarget instanceof DependentTarget){
            ((DependentTarget) currentTarget).handleMoveAndResizing();
        }
    }
    
}
