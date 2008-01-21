package greenfoot.mouse;


import greenfoot.Actor;
import greenfoot.MouseInfo;
import greenfoot.MouseInfoVisitor;
import greenfoot.World;


/**
 * Class to hold data collected from the mouse events. Using MouseInfo is
 * not enough, since a mouse info object doesn't contain all the info we
 * need (whether it was a drag, move, etc)
 * @author Poul Henriksen
 * 
 */
class MouseEventData {
    private MouseInfo mouseInfo;
    
    // We need to hold the data for each individual action that might have
    // happened, because what to report depends on what we are interested in,
    // with relation to the actor, world or globally.
    private MouseInfo mouseDragEndedInfo;
    private MouseInfo mouseClickedInfo;
    private MouseInfo mousePressedInfo;
    private MouseInfo mouseDraggedInfo;
    private MouseInfo mouseMovedInfo;    

    private void init()
    {
        mousePressedInfo = null;
        mouseClickedInfo = null;
        mouseDraggedInfo = null;
        mouseDragEndedInfo = null;
        mouseMovedInfo = null;
    }
    
    public MouseInfo getMouseInfo()
    {
        return mouseInfo;
    }

    public boolean isMousePressed(Object obj)
    {
        return checkObject(obj, mousePressedInfo);
    }

    public void mousePressed(int x, int y, int button, Actor actor)
    {
        init();
        mousePressedInfo = new MouseInfo();
        mouseInfo = mousePressedInfo;  
        MouseInfoVisitor.setButton(mouseInfo, button);
        MouseInfoVisitor.setLoc(mouseInfo, x, y);
        MouseInfoVisitor.setActor(mouseInfo, actor);
    }

    public boolean isMouseClicked(Object obj)
    { 
        // if the mouse was pressed outside the object we are looking for, it
        // can't be clicked on that object
        if(obj != null && (isMousePressed(null) && !isMousePressed(obj))) {
            return false;
        }
        return checkObject(obj, mouseClickedInfo);
    }
    
    public void mouseClicked(int x, int y, int button, Actor actor)
    {
        MouseInfo tempPressedInfo = mousePressedInfo;        
        init();       
        mousePressedInfo = tempPressedInfo;
        
        mouseClickedInfo = new MouseInfo();
        mouseInfo = mouseClickedInfo;
        MouseInfoVisitor.setButton(mouseInfo, button);
        MouseInfoVisitor.setLoc(mouseInfo, x, y);
        MouseInfoVisitor.setActor(mouseInfo, actor);
    }

    public boolean isMouseDragged(Object obj)
    {
        return checkObject(obj, mouseDraggedInfo);
    }

    public void mouseDragged(int x, int y, int button, Actor actor)
    {
        init();
        mouseDraggedInfo = new MouseInfo();
        mouseInfo = mouseDraggedInfo;
        MouseInfoVisitor.setButton(mouseInfo, button);
        MouseInfoVisitor.setLoc(mouseInfo, x, y);
        MouseInfoVisitor.setActor(mouseInfo, actor);
    }

    public boolean isMouseDragEnded(Object obj)
    {
        return checkObject(obj, mouseDragEndedInfo);
    }

    public void mouseDragEnded(int x, int y, int button, Actor actor)
    {
        MouseInfo tempPressedInfo = mousePressedInfo;
        MouseInfo tempClickedInfo = mouseClickedInfo;
        init();
        mousePressedInfo = tempPressedInfo;
        mouseClickedInfo = tempClickedInfo;
        mouseDragEndedInfo = new MouseInfo();
        mouseInfo = mouseDragEndedInfo;
        MouseInfoVisitor.setButton(mouseInfo, button);
        MouseInfoVisitor.setLoc(mouseInfo, x, y);
        MouseInfoVisitor.setActor(mouseInfo, actor);
    }

    public boolean isMouseMoved(Object obj)
    {
        return checkObject(obj, mouseMovedInfo);
    }

    public void mouseMoved(int x, int y, int button, Actor actor)
    {
        init();
        mouseMovedInfo = new MouseInfo();
        mouseInfo = mouseMovedInfo;
        MouseInfoVisitor.setButton(mouseInfo, button);
        MouseInfoVisitor.setLoc(mouseInfo, x, y);
        MouseInfoVisitor.setActor(mouseInfo, actor);
    }

    public int getX()
    {
        return mouseInfo.getX();
    }

    public int getY()
    {
        return mouseInfo.getY();
    }

    public Actor getActor()
    {
        if(mouseInfo == null) {
            return null;
        }
        return mouseInfo.getActor();
    }

    public int getButton()
    {
        if(mouseInfo == null) {
            return 0;
        }
        return mouseInfo.getButton();
    }
    
    /**
     * Check whether the given object can be considered to match the MouseInfo.
     * 
     * @param obj The query
     * @param info To check against
     * @return
     */
    private boolean checkObject(Object obj, MouseInfo info)
    {
        if(info == null) {
            return false;
        }
        Actor actor = info.getActor();
        return obj == null || (obj instanceof World && actor == null) || actor == obj;
    }

    
    public String toString()
    {
        String s = "MouseEventData ";
        if(mouseInfo != null) {
            s += mouseInfo.toString();
        }
        if(mousePressedInfo != null) {
            s += " pressed";
        }
        if(mouseClickedInfo != null) {
            s += " clicked";
        }
        if(mouseDraggedInfo != null) {
            s += " dragged";
        }
        if(mouseDragEndedInfo != null) {
            s += " dragEnded";
        }
        if(mouseMovedInfo != null) {
            s += " moved";
        }
        return s;
    }
 
}