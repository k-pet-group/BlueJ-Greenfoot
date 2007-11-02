package greenfoot.mouse;


import greenfoot.Actor;
import greenfoot.MouseInfo;
import greenfoot.MouseInfoVisitor;


/**
 * Class to hold data collected from the mouse events. Using MouseInfo is
 * not enough, since a mouse info object doesn't contain all the info we
 * need (whether it was a drag, move, etc)
 * @author Poul Henriksen
 * 
 */
class MouseEventData {
    private MouseInfo mouseInfo;
    private boolean mousePressed;
    private boolean mouseClicked;
    private boolean mouseDragged;
    private boolean mouseDragEnded;
    private boolean mouseMoved;
    

    private void init()
    {
        if(mouseInfo == null) {
            mouseInfo = new MouseInfo();
        }
    }
    
    public MouseInfo getMouseInfo()
    {
        return mouseInfo;
    }

    public boolean isMousePressed()
    {
        return mousePressed;
    }

    public void mousePressed(int x, int y, int button, Actor actor)
    {
        init();
        this.mousePressed = true;
        MouseInfoVisitor.setButton(mouseInfo, button);
        MouseInfoVisitor.setLoc(mouseInfo, x, y);
        MouseInfoVisitor.setActor(mouseInfo, actor);
    }

    public boolean isMouseClicked()
    {
        return mouseClicked;
    }

    public void mouseClicked(int x, int y, int button, Actor actor)
    {
        init();
        this.mouseClicked = true;
        MouseInfoVisitor.setButton(mouseInfo, button);
        MouseInfoVisitor.setLoc(mouseInfo, x, y);
        MouseInfoVisitor.setActor(mouseInfo, actor);
    }

    public boolean isMouseDragged()
    {
        return mouseDragged;
    }

    public void mouseDragged(int x, int y, int button, Actor actor)
    {
        init();
        this.mouseDragged = true;
        MouseInfoVisitor.setButton(mouseInfo, button);
        MouseInfoVisitor.setLoc(mouseInfo, x, y);
        MouseInfoVisitor.setActor(mouseInfo, actor);
    }

    public boolean isMouseDragEnded()
    {
        return mouseDragEnded;
    }

    public void mouseDragEnded(int x, int y, int button, Actor actor)
    {
        init();
        this.mouseDragEnded = true;
        MouseInfoVisitor.setButton(mouseInfo, button);
        MouseInfoVisitor.setLoc(mouseInfo, x, y);
        MouseInfoVisitor.setActor(mouseInfo, actor);
    }

    public boolean isMouseMoved()
    {
        return mouseMoved;
    }

    public void mouseMoved(int x, int y, int button, Actor actor)
    {
        init();
        this.mouseMoved = true;
        MouseInfoVisitor.setButton(mouseInfo, button);
        MouseInfoVisitor.setLoc(mouseInfo, x, y);
        MouseInfoVisitor.setActor(mouseInfo, actor);
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
 
}