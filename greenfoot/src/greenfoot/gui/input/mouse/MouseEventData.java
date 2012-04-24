/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011,2012  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.gui.input.mouse;


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
class MouseEventData
{
    private MouseInfo mouseInfo;
    
    // We need to hold the data for each individual action that might have
    // happened, because what to report depends on what we are interested in,
    // with relation to the actor, world or globally.
    private MouseInfo mouseDragEndedInfo;
    private MouseInfo mouseClickedInfo;
    private MouseInfo mousePressedInfo;
    private MouseInfo mouseDraggedInfo;
    private MouseInfo mouseMovedInfo;

    public void init()
    {
        mousePressedInfo = null;
        mouseClickedInfo = null;
        mouseDraggedInfo = null;
        mouseDragEndedInfo = null;
        mouseMovedInfo = null;
        if (mouseInfo != null)
        {
            MouseInfo blankedMouseInfo = MouseInfoVisitor.newMouseInfo();
            // Only retain info on latest location, not clicks etc:
            MouseInfoVisitor.setLoc(blankedMouseInfo, mouseInfo.getX(), mouseInfo.getY());
            mouseInfo = blankedMouseInfo;
        }
        
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
        mousePressedInfo = MouseInfoVisitor.newMouseInfo();
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
    
    public void mouseClicked(int x, int y, int button, int clickCount, Actor actor)
    {
        MouseInfo tempPressedInfo = mousePressedInfo;        
        init();       
        mousePressedInfo = tempPressedInfo;
        
        mouseClickedInfo = MouseInfoVisitor.newMouseInfo();;
        mouseInfo = mouseClickedInfo;
        MouseInfoVisitor.setButton(mouseInfo, button);
        MouseInfoVisitor.setLoc(mouseInfo, x, y);
        MouseInfoVisitor.setActor(mouseInfo, actor);
        MouseInfoVisitor.setClickCount(mouseInfo, clickCount);
    }

    public boolean isMouseDragged(Object obj)
    {
        return checkObject(obj, mouseDraggedInfo);
    }

    public void mouseDragged(int x, int y, int button, Actor actor)
    {
        init();
        mouseDraggedInfo = MouseInfoVisitor.newMouseInfo();
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
        mouseDragEndedInfo = MouseInfoVisitor.newMouseInfo();;
        mouseInfo = mouseDragEndedInfo;
        MouseInfoVisitor.setButton(mouseInfo, button);
        MouseInfoVisitor.setLoc(mouseInfo, x, y);
        MouseInfoVisitor.setActor(mouseInfo, actor);
    }

    public void mouseExited()
    {
        mouseInfo = mouseDraggedInfo;
        mouseMovedInfo = null;
    }
    
    public boolean isMouseMoved(Object obj)
    {
        return checkObject(obj, mouseMovedInfo);
    }

    public void mouseMoved(int x, int y, int button, Actor actor)
    {
        init();
        mouseMovedInfo = MouseInfoVisitor.newMouseInfo();;
        mouseInfo = mouseMovedInfo;
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