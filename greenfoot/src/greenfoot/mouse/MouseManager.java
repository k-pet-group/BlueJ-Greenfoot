package greenfoot.mouse;

import greenfoot.Actor;
import greenfoot.MouseInfo;
import greenfoot.World;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * There are two ways that the mouse can be handled in Greenfoot. One is the
 * built-in mouse support like it was in Greenfoot release 1.3.0 and earlier.
 * This still works the same way in newer Greenfoot versions as long as the
 * simulation is not running. When the simulation is running there is no default
 * mouse support for dragging objects already added into the world. When the
 * simulation is running the user classes have to poll for mouse information
 * through the greenfoot.Greenfoot class.
 * <p>
 * MouseEvents are collected in frames. A frame is defined as the interval
 * between the first poll of anything in the previous act-round to the first
 * poll in the current act-round.
 * <p>
 * When the first poll in an act loop happens, the future mouse data is copied
 * into current mouseInfo and the creation of a new future mouse data object is
 * started.
 * <p>
 * 
 * If several events happen in the same frame the events are prioritized like
 * this: <br>
 * Priorities with highest priority first::
 * <ul>
 * <li> dragEnd </li>
 * <li> click </li>
 * <li> press </li>
 * <li> drag </li>
 * <li> move </li>
 * </ul>
 * 
 * In general only one event can happen in a frame, the only exception is click
 * and press which could happen in the same frame if a mouse is clicked in one
 * frame. <br>
 * If several of the same type of event happens, then the last one is used.
 * <p>
 * If, for instance, two buttons are pressed at the same time, the behaviour is
 * undefined. Maybe we should define it so that button1 always have higher
 * priority than button2 and button2 always higher than button3. But not
 * necessarily documenting this to the user.
 * @author Poul Henriksen
 * 
 */
public class MouseManager implements MouseListener, MouseMotionListener
{

    /** Whether the user has requested any information about the mouse in this act loop. */
    private boolean polledInThisAct;
    
    /**
     * The current mouse data This will be the mouse info returned for the rest
     * of this act loop.
     */
    private MouseEventData currentData = new MouseEventData();

    /**
     * The future mouse data is build up from mouse events happening from the
     * first time the user requested mouse info in this act loop, until the user
     * requests again in the next act-loop, at which point the future will
     * become the current.
     */
    private MouseEventData futureData = new MouseEventData();

    /**
     * Locates the actors in the world
     */
    private WorldLocator locator;

    /**
     * Keeps track of where a drag started. This should never be explicitly set
     * to null, because it might result in exceptions when doing undefined
     * things like dragging with two buttons down at the same time.
     */
    private MouseEventData dragStartData;

    private boolean isDragging;

    private boolean gotNewEvent;
    

    /**
     * Creates a new mouse manager. The mouse manager should be notified whenever a new act round starts by calling {@link #newActStarted()}. 
     * @param locator Used to locate things (actors and coordinates) within the World.
     */
    public MouseManager(WorldLocator locator) 
    {
        this.locator = locator;
    }
    
    /**
     * This method should be called when a new act-loop is started.
     */
    public void newActStarted()
    {
        polledInThisAct = false;
    }

    /**
     * This method should be called every time we receive a mouse event. It is
     * used to keep track of whether any events have been occurring in this
     * frame.
     */
    private void registerEventRecieved()
    {
        gotNewEvent = true;
    }
    
    /**
     * If not already frozen, then freeze the current MouseEventData collected
     * in this act, so that the same will be returned for the rest of this
     * frame. This indicates the end of the previous frame and the beginning of
     * a new frame.
     */
    private void freezeMouseData()
    {
        if(polledInThisAct) {
            return;
        }
        
        synchronized(futureData) {
            if(! gotNewEvent) {
                //If we didn't get a new event we want to keep the current data.
                return;
            }
            MouseEventData newData = new MouseEventData();
            currentData = futureData;
            futureData = newData;
            
            // Indicate that we have processed all current events.
            gotNewEvent = false;
        }

        polledInThisAct = true;
        
        
    }

    // ************************************
    // Methods available to the user
    // ************************************

    /**
     * Whether the mouse had been pressed (changed from a non-pressed state to
     * being pressed) on the given object. If the parameter is an Actor the
     * method will only return true if the mouse has been pressed on the given
     * actor - if there are several actors at the same place, only the top most
     * actor will count. If the parameter is a World then true will be returned
     * only if the mouse was pressed outside the boundaries of all Actors. If
     * the parameter is null, then it will return true no matter where the mouse
     * was pressed as long as it is inside the world boundaries.
     * 
     * @param obj Typically one of Actor, World or null
     * @return True if the mouse has been pressed as explained above
     */
    public boolean isMousePressed(Object obj)
    {
        freezeMouseData();
        return checkObject(obj, currentData.getActor()) && currentData.isMousePressed();
    }

    /**
     * Whether the mouse had been clicked (pressed and released) on the given
     * object. If the parameter is an Actor the method will only return true if
     * the mouse has been clicked on the given actor - if there are several
     * actors at the same place, only the top most actor will count. If the
     * parameter is a World then true will be returned only if the mouse was
     * clicked outside the boundaries of all Actors. If the parameter is null,
     * then it will return true no matter where the mouse was clicked as long as
     * it is inside the world boundaries.
     * 
     * @param obj Typically one of Actor, World or null
     * @return True if the mouse has been clicked as explained above
     */
    public boolean isMouseClicked(Object obj)
    {
        freezeMouseData();
        return checkObject(obj, currentData.getActor()) && currentData.isMouseClicked();
    }

    /**
     * Whether the mouse had been dragged on the given object. The mouse is
     * considered to be dragged on an object, only if the drag started on that
     * object - even if the mouse has since been moved outside of that object.
     * <p>
     * If the parameter is an Actor the method will only return true if the drag
     * started on the given actor - if there are several actors at the same
     * place, only the top most actor will count. If the parameter is a World
     * then true will be returned only if the drag was started outside the
     * boundaries of all Actors. If the parameter is null, then it will return
     * true no matter where the drag was started as long as it is inside the
     * world boundaries.
     * 
     * @param obj Typically one of Actor, World or null
     * @return True if the mouse has been pressed as explained above
     */
    public boolean isMouseDragged(Object obj)
    {
        freezeMouseData();
        return checkObject(obj, currentData.getActor()) && currentData.isMouseDragged();
    }

    /**
     * A mouse drag has ended. This happens when the mouse has been dragged and
     * the mouse button released.
     * <p>
     * If the parameter is an Actor the method will only return true if the drag
     * started on the given actor - if there are several actors at the same
     * place, only the top most actor will count. If the parameter is a World
     * then true will be returned only if the drag was started outside the
     * boundaries of all Actors. If the parameter is null, then it will return
     * true no matter where the drag was started as long as it is inside the
     * world boundaries.
     * 
     * 
     * @param obj Typically one of Actor, World or null
     * @return True if the mouse has been pressed as explained above
     */
    public boolean isMouseDragEnded(Object obj)
    {
        freezeMouseData(); 
        return checkObject(obj, currentData.getActor()) && currentData.isMouseDragEnded();
    }

    /**
     * Whether the mouse had been moved on the given object. The mouse is
     * considered to be moved on an object, only if the mouse pointer is above that
     * object.
     * <p>
     * If the parameter is an Actor the method will only return true if the move
     * is on the given actor - if there are several actors at the same
     * place, only the top most actor will count. If the parameter is a World
     * then true will be returned only if the move is outside the
     * boundaries of all Actors. If the parameter is null, then it will return
     * true no matter where the drag was started as long as it is inside the
     * world boundaries.
     * 
     * @param obj Typically one of Actor, World or null
     * @return True if the mouse has been moved as explained above
     */
    public boolean isMouseMoved(Object obj)
    {
        freezeMouseData();
        return checkObject(obj, currentData.getActor()) && currentData.isMouseMoved();
    }

    /**
     * Gets the mouse info with information about the current state of the
     * mouse. Within the same act-loop it will always return exactly the same
     * MouseInfo object with exactly the same contents.
     * 
     * @return The info about the current state of the mouse. Null if nothing mouse related has happened in this act round.
     */
    public MouseInfo getMouseInfo()
    {
        freezeMouseData();
        return currentData.getMouseInfo();
    }   

    /**
     * Check whether the given object can be considered to match the actor.
     * 
     * @param obj The query
     * @param actor The actor - often the actor related to some mouseevent.
     * @return
     */
    private boolean checkObject(Object obj, Actor actor)
    {
        return obj == null || (obj instanceof World && actor == null) || actor == obj;
    }

    // ************************************
    // Implementations of listeners.
    // ************************************

    public void mouseClicked(MouseEvent e)
    {
        synchronized (futureData) {
            if(! PriorityManager.isHigherPriority(e, futureData)) return;
            registerEventRecieved();
            Actor actor = locator.getTopMostActorAt(e);
            int x = locator.getTranslatedX(e);
            int y = locator.getTranslatedY(e);
            int button = e.getButton();
            //in case we already had a mouse press associated with this click, we want to keep it.
            boolean isPressed = futureData.isMousePressed() && futureData.getX() == x && futureData.getY() == y && futureData.getButton() == button;
            futureData.mouseClicked(x, y, button, actor, isPressed);
            isDragging = false;
        }
    }


    public void mouseEntered(MouseEvent e)
    {
        synchronized(futureData) {
        }
    }

    public void mouseExited(MouseEvent e)
    {
        synchronized(futureData) {
        }
    }

    public void mousePressed(MouseEvent e)
    {
        synchronized(futureData) {
            // This might be the beginning of a drag so we store it
            dragStartData = new MouseEventData();
            Actor actor = locator.getTopMostActorAt(e);
            int x = locator.getTranslatedX(e);
            int y = locator.getTranslatedY(e);
            int button = e.getButton();
            dragStartData.mousePressed(x, y, button, actor);            

            // We only really want to register this event as a press if there is no higher priorities
            if(! PriorityManager.isHigherPriority(e, futureData)) return;
            registerEventRecieved();
            futureData.mousePressed(x, y, button, actor);
            isDragging = false;
        }
    }

    public void mouseReleased(MouseEvent e)
    {
        synchronized(futureData) {
            // This might be the end of a drag
            if(isDragging) {
                if(! PriorityManager.isHigherPriority(e, futureData)) return;
                registerEventRecieved();
                int x = locator.getTranslatedX(e);
                int y = locator.getTranslatedY(e);
                int button = e.getButton();
                futureData.mouseDragEnded(x, y, button, dragStartData.getActor());
                isDragging = false;
            }
        }
    }

    public void mouseDragged(MouseEvent e)
    {
        synchronized(futureData) {
            isDragging = true;
            
            if(! PriorityManager.isHigherPriority(e, futureData)) return;
            registerEventRecieved();
            
            // Find and store the actor that relates to this drag.
            int x = locator.getTranslatedX(e);
            int y = locator.getTranslatedY(e);
            int button = e.getButton();
            futureData.mouseDragged(x, y, button, dragStartData.getActor());
        }
    }

    public void mouseMoved(MouseEvent e)
    {
        synchronized(futureData) {
            if(! PriorityManager.isHigherPriority(e, futureData)) return;
            registerEventRecieved();
            Actor actor = locator.getTopMostActorAt(e);
            int x = locator.getTranslatedX(e);
            int y = locator.getTranslatedY(e);
            int button = e.getButton();
            futureData.mouseMoved(x, y, button, actor);
            isDragging = false;
        }
    }   
}

