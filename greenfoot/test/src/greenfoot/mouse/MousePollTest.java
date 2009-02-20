/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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
package greenfoot.mouse;

import greenfoot.Actor;
import greenfoot.MouseInfo;
import greenfoot.TestObject;
import greenfoot.TestUtilDelegate;
import greenfoot.World;
import greenfoot.WorldCreator;
import greenfoot.WorldVisitor;
import greenfoot.gui.input.mouse.MousePollingManager;
import greenfoot.gui.input.mouse.WorldLocator;
import greenfoot.util.GreenfootUtil;

import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.JPanel;

import junit.framework.TestCase;
/**
 * Tests of the implementation of the Greenfoot mouse support. 
 * TODO drags on boundaries
 * TODO things happening outside actors
 * TODO buffering events until new ones arrive
 * @author Poul Henriksen
 *
 */
public class MousePollTest extends TestCase
{
    private MousePollingManager mouseMan; 
    
    /** Panel used to simulate events on */
    private JPanel panel;     

    private World world;
    private TestObject actorAtClick;

    private TestObject actorOutsideClick;
    
    @Override
    protected void setUp()
        throws Exception
    {
        GreenfootUtil.initialise(new TestUtilDelegate());
        
        //set up world with two actors
        world = WorldCreator.createWorld(200, 200, 1);
        actorAtClick = new TestObject(10,10);
        world.addObject(actorAtClick, 5,5);
        actorOutsideClick = new TestObject(10,10);
        world.addObject(actorOutsideClick, 50,50);
        mouseMan = new MousePollingManager(new WorldLocator(){
                public Actor getTopMostActorAt(MouseEvent e)
                { 
                    Collection<?> actors = WorldVisitor.getObjectsAtPixel(world, e.getX(), e.getY());
                    if(actors.isEmpty()) {
                        return null;
                    }
                    return (Actor) actors.iterator().next();
                }

                public int getTranslatedX(MouseEvent e)
                {
                    return WorldVisitor.toCellFloor(world, e.getX());
                }

                public int getTranslatedY(MouseEvent e)
                {
                    return WorldVisitor.toCellFloor(world, e.getY());
                }});
        panel = new JPanel();
        panel.addMouseListener(mouseMan);
        panel.addMouseMotionListener(mouseMan);  
        panel.setEnabled(true);
       
       
      
    }


    private void dispatch(MouseEvent e)
    {
        panel.dispatchEvent(e);
    }
    
    /**
     * Test that a click was on a specific actor and no other actors.
     */
    public void testSingleLeftClickOnActor() {
        mouseMan.newActStarted();
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);        
        dispatch(event);     
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);        
        dispatch(event);     
        event = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);        
        dispatch(event);             
        
        assertTrue(mouseMan.isMousePressed(actorAtClick));
        assertTrue(mouseMan.isMouseClicked(actorAtClick));
        
        assertFalse(mouseMan.isMouseClicked(actorOutsideClick));
        assertTrue(mouseMan.isMouseClicked(null));
        
        // Make sure it is still true after doing more queries
        assertTrue(mouseMan.isMouseClicked(actorAtClick)); 
        
        MouseInfo mouseInfo = mouseMan.getMouseInfo();
        assertTrue(mouseInfo.getButton() == 1);

        // After new act it should not be clicked because we already checked it.
        mouseMan.newActStarted();
        assertFalse(mouseMan.isMouseClicked(actorAtClick));
    }
    
    /**
     * Test that a mouse press was on a specific actor and no other actors.
     */
    public void testSingleLeftPressedOnActor() {
        mouseMan.newActStarted();
        MouseEvent pressedEvent = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);        
        dispatch(pressedEvent);     
        assertTrue(mouseMan.isMousePressed(actorAtClick));
        assertFalse(mouseMan.isMousePressed(actorOutsideClick));
        assertFalse(mouseMan.isMousePressed(world));
        assertTrue(mouseMan.isMousePressed(null));
        
        // Make sure it is still true after doing more queries
        assertTrue(mouseMan.isMousePressed(actorAtClick)); 
        
        MouseInfo mouseInfo = mouseMan.getMouseInfo();
        assertTrue(mouseInfo.getButton() == 1);      
        
        // After new act it should not be pressed because we already checked it.
        mouseMan.newActStarted();
        assertFalse(mouseMan.isMousePressed(actorAtClick));        
    }
    
    
    
    /**
     * Test mouse dragging within one act
     */
    public void testSimpleMouseDraggedOnActor() {
        mouseMan.newActStarted();
        
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);        
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1);          
        dispatch(event);         
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 7, 7, 1, false, MouseEvent.BUTTON1);        
        dispatch(event);          

        assertTrue(mouseMan.isMouseDragEnded(actorAtClick));
        assertFalse(mouseMan.isMouseDragged(actorAtClick));
        assertTrue(mouseMan.isMousePressed(actorAtClick));
        assertTrue(mouseMan.isMouseClicked(actorAtClick));
        
    }
    
    public void testLongMouseDraggedOnActor() 
    {
        mouseMan.newActStarted();
        
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);        
        dispatch(event);     
        assertTrue(mouseMan.isMousePressed(actorAtClick));
        
        /// New act round where nothing happens
        mouseMan.newActStarted();        
        assertFalse(mouseMan.isMousePressed(actorAtClick));
        assertNull(mouseMan.getMouseInfo());
        
        /// New act round where we drag a bit
        mouseMan.newActStarted();
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1);  
        dispatch(event);          
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 7, 7, 1, false, MouseEvent.BUTTON1);  
        dispatch(event);          
        assertTrue(mouseMan.isMouseDragged(actorAtClick));
        assertTrue(mouseMan.getMouseInfo().getX() == 7 && mouseMan.getMouseInfo().getY() == 7);
        
        /// New act round where we drag a bit more
        mouseMan.newActStarted();
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 8, 8, 1, false, MouseEvent.BUTTON1);  
        dispatch(event);          
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 9, 9, 1, false, MouseEvent.BUTTON1);  
        dispatch(event);          
        assertTrue(mouseMan.isMouseDragged(actorAtClick));
        assertTrue(mouseMan.getMouseInfo().getX() == 9 && mouseMan.getMouseInfo().getY() == 9);
        
        
        /// New act round where nothing happens
        mouseMan.newActStarted();
        assertFalse(mouseMan.isMousePressed(actorAtClick));
        assertFalse(mouseMan.isMouseDragged(actorAtClick));
        assertNull(mouseMan.getMouseInfo());
        
        // New act round where the drag is ended
        mouseMan.newActStarted();
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 14, 14, 1, false, MouseEvent.BUTTON1);        
        dispatch(event);             

        assertTrue(mouseMan.getMouseInfo().getX() == 14 && mouseMan.getMouseInfo().getY() == 14);
        assertTrue(mouseMan.isMouseDragEnded(actorAtClick));
        assertFalse(mouseMan.isMouseClicked(actorAtClick));
    }
    
    
    /**
     * Test mouse dragging across act boundaries
     */
    public void testLongMouseMovedOnActor() {
        mouseMan.newActStarted();
        
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);          
        dispatch(event);  
        
        assertTrue(mouseMan.isMouseMoved(actorAtClick));    
        assertTrue(mouseMan.isMouseMoved(null));   
        assertFalse(mouseMan.isMouseMoved(actorOutsideClick));  
        assertFalse(mouseMan.isMouseMoved(world));     
        
        mouseMan.newActStarted();
        assertFalse(mouseMan.isMouseMoved(actorAtClick));  
        mouseMan.newActStarted();
        event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1);        
        dispatch(event);             

        assertTrue(mouseMan.isMouseMoved(actorAtClick));
        
        assertFalse(mouseMan.isMouseClicked(actorAtClick));
        
    }

    /**
     * Tests behaviour when several buttons are pressed and dragged at the same
     * time. We just want to ensure that nothing crashes or produces exceptions.
     * Otherwise the behaviour is undefined.
     */
    public void testMultipleButtons()
    {
        Exception exception = null;
        try {
            mouseMan.newActStarted();

            MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 5, 5, 1,
                    false, MouseEvent.BUTTON1);
            dispatch(event);
            event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(), 0, 6, 6, 1, false,
                    MouseEvent.BUTTON1);
            dispatch(event);

            event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 5, 5, 1, false,
                    MouseEvent.BUTTON2);
            dispatch(event);

            event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(), 0, 6, 6, 1, false,
                    MouseEvent.BUTTON1);
            dispatch(event);
            event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(), 0, 6, 6, 1, false,
                    MouseEvent.BUTTON2);
            dispatch(event);

            event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, 7, 7, 1, false,
                    MouseEvent.BUTTON1);
            dispatch(event);
            event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(), 0, 6, 6, 1, false,
                    MouseEvent.BUTTON2);
            dispatch(event);
            mouseMan.isMouseClicked(null);
            mouseMan.isMousePressed(null);
            mouseMan.isMouseMoved(null);
            mouseMan.isMouseDragged(null);
            mouseMan.isMouseDragEnded(null);
            MouseInfo info = mouseMan.getMouseInfo();
            info.getActor();
            info.getX();
            info.getButton();
        }
        catch (Exception e) {
            exception = e;
        }
        assertNull(exception);
    }
    
    public void testButton2() {
        mouseMan.newActStarted();
        
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON2);        
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 6, 6, 0, false, MouseEvent.BUTTON2);          
        dispatch(event);           
        assertNotNull(mouseMan.getMouseInfo());
     //   assertFalse(mouseMan.isMouseDragged(actorAtClick));
     //   assertTrue(mouseMan.isMousePressed(actorAtClick));
            
        
        // act
        mouseMan.newActStarted();
        
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 5, 5, 0, false, MouseEvent.BUTTON2);          
        dispatch(event);  

        
        // act
        mouseMan.newActStarted();
        assertNotNull(mouseMan.getMouseInfo());
        

        mouseMan.newActStarted();
        
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 8, 8, 0, false, MouseEvent.BUTTON2);          
        dispatch(event);  

        assertNotNull(mouseMan.getMouseInfo());
        
        
        // act
        mouseMan.newActStarted();
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 7, 7, 1, false, MouseEvent.BUTTON2);        
        dispatch(event);
        assertTrue(mouseMan.isMouseDragEnded(actorAtClick));
    }

    public void testButton3() {
        
    }
    
    
    /**
     * Test a world that has a cell size bigger than 1x1 
     */
    public void testBigCellSize() 
    {

        final World world = WorldCreator.createWorld(20, 20, 10);
        final MousePollingManager mouseMan = new MousePollingManager(new WorldLocator(){
            public Actor getTopMostActorAt(MouseEvent e)
            { 
                Collection<?> actors = WorldVisitor.getObjectsAtPixel(world, e.getX(), e.getY());
                if(actors.isEmpty()) {
                    return null;
                }
                return (Actor) actors.iterator().next();
            }

            public int getTranslatedX(MouseEvent e)
            {
                return WorldVisitor.toCellFloor(world, e.getX());
            }

            public int getTranslatedY(MouseEvent e)
            {
                return WorldVisitor.toCellFloor(world, e.getY());
            }});
        panel.addMouseListener(mouseMan);
        panel.addMouseMotionListener(mouseMan);  
        panel.setEnabled(true);
        
        mouseMan.newActStarted();
        
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);          
        dispatch(event);  
        
        assertTrue(mouseMan.isMouseMoved(null));   
        
        mouseMan.newActStarted();
        event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 55, 75, 1, false, MouseEvent.BUTTON1);        
        dispatch(event);             

        assertTrue(mouseMan.isMouseMoved(null));
        
        MouseInfo info = mouseMan.getMouseInfo();
        assertEquals(5, info.getX());
        assertEquals(7, info.getY());
    }
    
    /**
     * Test priorities between different mouse events.
     * <p>
     * Priorities with highest priority first::
     * <ul>
     * <li> dragEnd </li>
     * <li> click </li>
     * <li> press </li>
     * <li> drag </li>
     * <li> move </li>
     * </ul>
     * 
     * In general only one event can happen in a frame, the only exception is
     * click and press which could happen in the same frame if a mouse is
     * clicked in one frame. <br>
     * If several of the same type of event happens, then the last one is used.
     * <p>
     */
    public void testDragEndPriorities()
    {
        mouseMan.newActStarted();
        
        // Test that dragEnd is highest priority
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 1, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 2, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 3, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);
        event = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED,  System.currentTimeMillis(), 0, 4, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 5, 2, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 6, 3, 1, false, MouseEvent.BUTTON1);
        dispatch(event);         
        // this one will trigger a dragEnd
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 7, 8, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 8, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 9, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 1, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);
        event = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED,  System.currentTimeMillis(), 0, 2, 4, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 3, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 4, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);

        MouseInfo info = mouseMan.getMouseInfo();
        assertEquals(7, info.getX());
        assertEquals(8, info.getY());
        assertTrue(mouseMan.isMouseDragEnded(actorAtClick));
        assertTrue(mouseMan.isMouseClicked(actorAtClick));
        assertFalse(mouseMan.isMouseMoved(actorAtClick));
        assertFalse(mouseMan.isMouseDragged(actorAtClick));
        assertTrue(mouseMan.isMousePressed(actorAtClick));
       
        mouseMan.newActStarted();
        // Test that the last dragEnd is reported when several occurs
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);         
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);         
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 3, 3, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 

        assertTrue(mouseMan.isMouseDragEnded(actorAtClick));
        info = mouseMan.getMouseInfo();
        assertEquals(3, info.getX());
        assertEquals(3, info.getY());
        assertFalse(mouseMan.isMouseMoved(actorAtClick));
        assertFalse(mouseMan.isMouseDragged(actorAtClick));
        assertTrue(mouseMan.isMousePressed(actorAtClick));
        assertTrue(mouseMan.isMouseClicked(actorAtClick));
    }
    
    public void testClickPriorities() 
    {
        mouseMan.newActStarted();
        
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);
        event = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED,  System.currentTimeMillis(), 0, 8, 8, 1, false, MouseEvent.BUTTON1);
        dispatch(event);                   
        event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        
        assertTrue(mouseMan.isMouseClicked(actorAtClick));
        MouseInfo info = mouseMan.getMouseInfo();
        assertEquals(8, info.getX());
        assertEquals(8, info.getY());
        assertFalse(mouseMan.isMouseMoved(actorAtClick));
        assertFalse(mouseMan.isMouseDragged(actorAtClick));
        assertTrue(mouseMan.isMousePressed(actorAtClick));
        assertFalse(mouseMan.isMouseDragEnded(actorAtClick));
       
        mouseMan.newActStarted();
        // Test that the last click is reported when several occurs
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 1, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);        
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 2, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED,  System.currentTimeMillis(), 0, 3, 7, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 4, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);        
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED,  System.currentTimeMillis(), 0, 6, 4, 1, false, MouseEvent.BUTTON1);
        dispatch(event);

        info = mouseMan.getMouseInfo();
        assertEquals(6, info.getX());
        assertEquals(4, info.getY());        
        assertTrue(mouseMan.isMouseClicked(actorAtClick));
        assertFalse(mouseMan.isMouseMoved(actorAtClick));
        assertFalse(mouseMan.isMouseDragged(actorAtClick));
        assertTrue(mouseMan.isMousePressed(actorAtClick));
        assertFalse(mouseMan.isMouseDragEnded(actorAtClick));       
    }
    
    public void testPressPriorities() 
    {
        mouseMan.newActStarted();
        
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);  
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 8, 8, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        
        assertTrue(mouseMan.isMousePressed(actorAtClick));
        MouseInfo info = mouseMan.getMouseInfo();
        assertEquals(8, info.getX());
        assertEquals(8, info.getY());
        assertFalse(mouseMan.isMouseMoved(actorAtClick));
        assertFalse(mouseMan.isMouseDragged(actorAtClick));
        assertFalse(mouseMan.isMouseClicked(actorAtClick));
        assertFalse(mouseMan.isMouseDragEnded(actorAtClick));
       
        mouseMan.newActStarted();
        // Test that the last press is reported when several occurs
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 1, 6, 1, false, MouseEvent.BUTTON1);
        dispatch(event);        
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 2, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 3, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 4, 3, 1, false, MouseEvent.BUTTON1);
        dispatch(event);        
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 

        info = mouseMan.getMouseInfo();
        assertEquals(4, info.getX());
        assertEquals(3, info.getY());       
        assertTrue(mouseMan.isMousePressed(actorAtClick));
        assertFalse(mouseMan.isMouseMoved(actorAtClick));
        assertFalse(mouseMan.isMouseDragged(actorAtClick));
        assertFalse(mouseMan.isMouseClicked(actorAtClick));
        assertFalse(mouseMan.isMouseDragEnded(actorAtClick));       
    }
    
    public void testOnlyKeepDataTillFirstClick() {
        mouseMan.newActStarted();
        
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);
        event = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);  

        mouseMan.newActStarted();
        mouseMan.newActStarted();
        
        assertTrue(mouseMan.isMouseClicked(actorAtClick));
        mouseMan.newActStarted();                
        assertFalse(mouseMan.isMouseClicked(actorAtClick));        
    }
    
    public void testMoveClickWithinActor() 
    {
        mouseMan.newActStarted();
        
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1);
        dispatch(event);
        assertTrue(mouseMan.isMouseDragEnded(actorAtClick));
        assertTrue(mouseMan.isMouseClicked(actorAtClick));    
        assertTrue(mouseMan.isMousePressed(actorAtClick));  
        
        
        mouseMan.newActStarted();
        // test that there is NO click when dragging outside actor bounds
        event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 26, 26, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 26, 26, 1, false, MouseEvent.BUTTON1);
        dispatch(event);
        assertTrue(mouseMan.isMouseDragEnded(actorAtClick));
        assertFalse(mouseMan.isMouseClicked(actorAtClick));    
        assertTrue(mouseMan.isMousePressed(actorAtClick));  
        

        mouseMan.newActStarted();
        // test that there is NO click and press when dragging from outside actor bounds into actor bounds
        event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 25, 25, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 25, 25, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1);
        dispatch(event);
        assertFalse(mouseMan.isMouseDragEnded(actorAtClick));
        assertFalse(mouseMan.isMouseClicked(actorAtClick));    
        assertFalse(mouseMan.isMousePressed(actorAtClick));          
    }
    

    public void testNullPressDragClickOnActorBounds() 
    {
        mouseMan.newActStarted();
              
        // test that even if moving out of actor, giving null as argument will still report all press, clicks and dragEnds
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 26, 26, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 26, 26, 1, false, MouseEvent.BUTTON1);
        dispatch(event);
        assertTrue(mouseMan.isMouseDragEnded(null));
        assertTrue(mouseMan.isMouseClicked(null));    
        assertTrue(mouseMan.isMousePressed(null));  

        mouseMan.newActStarted();
        event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 25, 25, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 25, 25, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1);
        dispatch(event);

        assertFalse(mouseMan.isMouseMoved(null));
        
        assertTrue(mouseMan.isMouseDragEnded(null));
        assertTrue(mouseMan.isMouseClicked(null));    
        assertTrue(mouseMan.isMousePressed(null));   
       
        assertTrue(mouseMan.isMousePressed(world));
        assertTrue(mouseMan.isMouseDragEnded(world));
        assertFalse(mouseMan.isMouseClicked(world));          

        assertFalse(mouseMan.isMousePressed(actorAtClick));
        assertFalse(mouseMan.isMouseDragEnded(actorAtClick));
        assertFalse(mouseMan.isMouseClicked(actorAtClick));    
        assertFalse(mouseMan.isMousePressed(actorAtClick)); 
        
    }
    
    public void testMultipleDragsInFrame() 
    {
        mouseMan.newActStarted();
              
        // test that even if moving out of actor, giving null as argument will still report all press, clicks and dragEnds
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 26, 26, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 26, 26, 1, false, MouseEvent.BUTTON1);
        dispatch(event);

        event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 25, 25, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 25, 25, 1, false, MouseEvent.BUTTON1);
        dispatch(event); 
        event = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED,  System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1);
        dispatch(event);    
        event = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,  System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1);
        dispatch(event);

        assertFalse(mouseMan.isMouseMoved(null));
        
        assertTrue(mouseMan.isMouseDragEnded(null));
        assertTrue(mouseMan.isMouseClicked(null));    
        assertTrue(mouseMan.isMousePressed(null));   
       
        assertTrue(mouseMan.isMousePressed(world));
        assertTrue(mouseMan.isMouseDragEnded(world));
        assertFalse(mouseMan.isMouseClicked(world));          

        assertFalse(mouseMan.isMousePressed(actorAtClick));
        assertFalse(mouseMan.isMouseDragEnded(actorAtClick));
        assertFalse(mouseMan.isMouseClicked(actorAtClick));    
        assertFalse(mouseMan.isMousePressed(actorAtClick)); 
        
    }
}
