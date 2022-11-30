/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2012,2018  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.MouseInfo;
import greenfoot.gui.input.mouse.MouseEventData;

import java.awt.event.MouseEvent;

import javafx.scene.input.MouseButton;
import threadchecker.OnThread;
import threadchecker.Tag;

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
 * Priorities with highest priority first:
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
 * 
 * @author Poul Henriksen
 */
public class MousePollingManager
{
    /*
     * Methods in this class are called from two threads: the simulation thread and the
     * GUI even thread. Some methods are called by one and some by the other; the GUI produces
     * the data while the simulation consumes it.
     * 
     * A small number of fields can be accessed be either thread and so access to them must
     * be synchronized. Other fields are accessed by only one thread or the other.
     */
    
    /**
     * The current mouse data This will be the mouse info returned for the rest
     * of this act loop.
     * 
     * <p>Accessed only from the simulation thread.
     */
    private MouseEventData currentData = new MouseEventData();

    /**
     * The future mouse data is build up from mouse events happening from the
     * first time the user requested mouse info in this act loop, until the user
     * requests again in the next act-loop, at which point the future will
     * become the current.
     * 
     * <p>Access to this field must be synchronized.
     */
    private MouseEventData futureData = new MouseEventData();
    
    /**
     * Used to collect data if we already have a highest priority dragEnded
     * collected. We need this in order to collect data for a potential new
     * dragEnd since we want to report the latest dragEnd in case there are more
     * than one.
     * 
     * <p>Access to this field must be synchronized.
     */
    private MouseEventData potentialNewDragData = new MouseEventData();
    
    /**
     * Locates the actors in the world (read only field, requires no synchronization).
     */
    private WorldLocator locator;

    /**
     * Keeps track of where a drag started. This should never be explicitly set
     * to null, because it might result in exceptions when doing undefined
     * things like dragging with two buttons down at the same time.
     * 
     * <p>Accessed only from the GUI thread.
     */
    private MouseEventData dragStartData = new MouseEventData();

    /**
     * Track whether the mouse is currently being dragged.
     * 
     * <p>Accessed only from the GUI thread.
     */
    private boolean isDragging;

    /**
     * Whether we have received more mouse data since we last gave data to the simulation.
     * 
     * <p>Access to this field must be synchronized.
     */
    private boolean gotNewEvent;
    private boolean gotNewDragStartEvent;
    

    /**
     * Creates a new mouse manager. The mouse manager should be notified
     * whenever a new act round starts by calling {@link #newActStarted()}.
     * 
     * @param locator
     *            Used to locate things (actors and coordinates) within the
     *            World. May be null, but in that case the locator must be set
     *            later.
     */
    @OnThread(Tag.Any)
    public MousePollingManager(WorldLocator locator) 
    {
        this.locator = locator;
    }
    
    /**
     * Set the locator to be used by this mouse polling manager.
     */
    public void setWorldLocator(WorldLocator locator)
    {
        this.locator = locator;
    }
    
    /**
     * This method should be called when a new act-loop is started.
     */
    @OnThread(Tag.Simulation)
    public synchronized void newActStarted()
    {
        // The current data was already polled, or we have a new event since;
        // use futureData as our current data. (If there's been no event, i.e. if
        // gotNewEvent is false, futureData will contain no events).
        if (gotNewEvent) {
            MouseEventData newData = new MouseEventData();
            futureData.setActors(locator);
            currentData = futureData;
            futureData = newData;
            potentialNewDragData = new MouseEventData();

            if (gotNewDragStartEvent)
            {
                dragStartData.setActors(locator);
                // We need to also set the actor on the drag-ended if it both
                // started this frame (true if gotNewDragStartEvent was true) and
                // ended this frame (check inside setDragStartActor):
                currentData.setDragStartActor(dragStartData);
                gotNewDragStartEvent = false;
            }
            
            // Indicate that we have processed all current events.
            gotNewEvent = false;
        }
        else {
            currentData.init();
        }
    }

    /**
     * This method should be called every time we receive a mouse event. It is
     * used to keep track of whether any events have been occurring in this
     * frame.
     * 
     * <p>This must be called from a synchronized context.
     */
    private void registerEventRecieved()
    {
        gotNewEvent = true;
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
    @OnThread(Tag.Simulation)
    public boolean isMousePressed(Object obj)
    {
        return currentData.isMousePressedOn(obj);
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
    @OnThread(Tag.Simulation)
    public boolean isMouseClicked(Object obj)
    {
        return currentData.isMouseClickedOn(obj);
    }

    /**
     * Whether the mouse is being dragged on the given object. The mouse is
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
    @OnThread(Tag.Simulation)
    public boolean isMouseDragged(Object obj)
    {
        return currentData.isMouseDraggedOn(obj);
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
    @OnThread(Tag.Simulation)
    public boolean isMouseDragEnded(Object obj)
    {
        return currentData.isMouseDragEndedOn(obj);
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
    @OnThread(Tag.Simulation)
    public boolean isMouseMoved(Object obj)
    {
        return currentData.isMouseMovedOn(obj);
    }

    /**
     * Gets the mouse info with information about the current state of the
     * mouse. Within the same act-loop it will always return exactly the same
     * MouseInfo object with exactly the same contents.
     * 
     * @return The info about the current state of the mouse; Null if the mouse is outside
     *         the world boundaries (unless being dragged).
     */
    @OnThread(Tag.Simulation)
    public MouseInfo getMouseInfo()
    {
        return currentData.getMouseInfo();
    }   

    /**
     * The mouse got clicked at the given world location
     * @param x The pixel location in the world (not cells)
     * @param y The pixel location in the world (not cells)
     * @param button The button reported by the original event.
     * @param clickCount The click count recorded by the original event.
     */
    @OnThread(Tag.Any)
    public void mouseClicked(int x, int y, MouseButton button, int clickCount)
    {
        if (locator == null)
        {
            return;
        }
        
        synchronized (this)
        {
            MouseEventData mouseData = futureData;
            // In case we already have a dragEnded and we get another
            // dragEnded, we need to start collection data for that.            
            if (futureData.isMouseDragEnded())
            {
                mouseData = potentialNewDragData;
            }
            if (!PriorityManager.isHigherPriority(MouseEvent.MOUSE_CLICKED, mouseData))
            {
                return;
            }
            registerEventRecieved();
            int tx = locator.getTranslatedX(x);
            int ty = locator.getTranslatedY(y);
            
            mouseData.mouseClicked(tx, ty, x, y, getButton(button), clickCount);
            isDragging = false;
        }
    }

    /**
     * Translates a JavaFX button to 1/2/3 as used by the Greenfoot API for left/middle/right.
     */
    private int getButton(MouseButton button)
    {
        switch (button)
        {
            case PRIMARY:
                return 1;
            case MIDDLE:
                return 2;
            case SECONDARY:
                return 3;
            default:
                return 0;
        }
    }

    /**
     * The mouse left the world area.
     */
    @OnThread(Tag.Any)
    public synchronized void mouseExited()
    {
        futureData.mouseExited();
        registerEventRecieved();
    }

    /**
     * The mouse got pressed on the given world location
     * @param x The pixel location in the world (not cells)
     * @param y The pixel location in the world (not cells)
     * @param button The button reported by the original event.
     */
    @OnThread(Tag.Any)
    public void mousePressed(int x, int y, MouseButton button)
    {
        if (locator == null)
        {
            return;
        }
        
        synchronized(this)
        {
            MouseEventData mouseData = futureData;
            // In case we already have a dragEnded and we get another
            // dragEnded, we need to start collection data for that.
            if (futureData.isMouseDragEnded())
            {
                mouseData = potentialNewDragData;
            }
        
            // This might be the beginning of a drag so we store it
            dragStartData = new MouseEventData();
            int tx = locator.getTranslatedX(x);
            int ty = locator.getTranslatedY(y);
            dragStartData.mousePressed(tx, ty, x, y, getButton(button));
            gotNewDragStartEvent = true;

            // We only really want to register this event as a press if there is no higher priorities
            if (!PriorityManager.isHigherPriority(MouseEvent.MOUSE_PRESSED, mouseData))
            {
                return;
            }
            registerEventRecieved();
            mouseData.mousePressed(tx, ty, x, y, getButton(button));
            isDragging = false;
        }
    }

    /**
     * The mouse got released at the given world location
     * @param x The pixel location in the world (not cells)
     * @param y The pixel location in the world (not cells)
     * @param button The button reported by the original event.
     */
    @OnThread(Tag.Any)
    public void mouseReleased(int x, int y, MouseButton button)
    {
        if (locator == null)
        {
            return;
        }
        
        synchronized(this)
        {
            // This might be the end of a drag
            if(isDragging)
            {
                // In case we already have a dragEnded and we get another
                // dragEnded, should use the new one
                if (futureData.isMouseDragEnded())
                {
                    futureData = potentialNewDragData;
                }
                
                if (!PriorityManager.isHigherPriority(MouseEvent.MOUSE_RELEASED, futureData))
                {
                    return;
                }
                registerEventRecieved();
                int tx = locator.getTranslatedX(x);
                int ty = locator.getTranslatedY(y);

                futureData.mouseClicked(tx, ty, x, y, getButton(button), 1);
                
                futureData.mouseDragEnded(tx, ty, x, y, getButton(button), dragStartData);
                isDragging = false;
                potentialNewDragData = new MouseEventData();
            }
        }
    }

    /**
     * The mouse got dragged to the given world location
     * @param x The pixel location in the world (not cells)
     * @param y The pixel location in the world (not cells)
     * @param button The button reported by the original event.
     */
    @OnThread(Tag.Any)
    public void mouseDragged(int x, int y, MouseButton button)
    {
        if (locator == null)
        {
            return;
        }
        
        synchronized(this)
        {
            isDragging = true;
            
            if (!PriorityManager.isHigherPriority(MouseEvent.MOUSE_DRAGGED, futureData))
            {
                return;
            }
            registerEventRecieved();
            
            // Find and store the actor that relates to this drag.
            int tx = locator.getTranslatedX(x);
            int ty = locator.getTranslatedY(y);
            futureData.mouseDragged(tx, ty, x, y, dragStartData.getButton(), dragStartData.getActor());
        }
    }

    /**
     * The mouse got moved to the given world location
     * @param x The pixel location in the world (not cells)
     * @param y The pixel location in the world (not cells)
     */
    @OnThread(Tag.Any)
    public void mouseMoved(int x, int y)
    {
        if (locator == null)
        {
            // Not fully initialised yet, so no need to handle event:
            return;
        }
        
        synchronized(this)
        {
            if (!PriorityManager.isHigherPriority(MouseEvent.MOUSE_MOVED, futureData))
            {
                return;
            }
            registerEventRecieved();
            int tx = locator.getTranslatedX(x);
            int ty = locator.getTranslatedY(y);
            futureData.mouseMoved(tx, ty, x, y);
            isDragging = false;
        }
    }

    /**
     * Called when the world starts running, to discard any
     * old mouse data that may have been accumulated while paused.
     */
    public void startedRunning()
    {
        futureData = new MouseEventData();
    }
}

