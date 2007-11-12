package greenfoot.mouse;

import greenfoot.Actor;
import greenfoot.MouseInfo;
import greenfoot.TestObject;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.mouse.MouseManager;
import greenfoot.platforms.GreenfootUtilDelegate;
import greenfoot.util.GreenfootUtil;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

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
    private MouseManager mouseMan; 
    
    /** Panel used to simulate events on */
    private JPanel panel;     

    private World world;
    private TestObject actorAtClick;

    private TestObject actorOutsideClick;
    
    @Override
    protected void setUp()
        throws Exception
    {
        GreenfootUtil.initialise(new GreenfootUtilDelegate( ) {
            public void createSkeleton(String className, String superClassName, File file, String templateFileName)
                throws IOException
            {
                return;
            }

            public ClassLoader getCurrentClassLoader()
            {
                return getClass().getClassLoader();
            }

            public String getGreenfootLogoPath()
            {
                String classes = getClass().getClassLoader().getResource(".").toString();
                File startingDir = null;
                try {
                    startingDir = (new File(new URI(classes)).getParentFile());
                }
                catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                while((startingDir != null) &&
                        !(new File(startingDir, "images").isDirectory())) {
                    startingDir = startingDir.getParentFile();
                }
                File imageFile = new File(startingDir, "images/greenfoot.png");
                return imageFile.toString();
            }

            public String getNewProjectName(Component parent)
            {
                // TODO Auto-generated method stub
                return null;
            }

            public File getScenarioFromFileBrowser(Component parent)
            {
                // TODO Auto-generated method stub
                return null;
            }});
        
        //set up world with two actors
        world = new World(200, 200, 1) {};
        actorAtClick = new TestObject(10,10);
        world.addObject(actorAtClick, 5,5);
        actorOutsideClick = new TestObject(10,10);
        world.addObject(actorOutsideClick, 50,50);
        mouseMan = new MouseManager(new WorldLocator(){
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

        // After new act it should still longer be clicked
        mouseMan.newActStarted();
        assertTrue(mouseMan.isMouseClicked(actorAtClick));
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
        
        // After new act it should still be pressed
        mouseMan.newActStarted();
        assertTrue(mouseMan.isMousePressed(actorAtClick));        
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
        assertFalse(mouseMan.isMousePressed(actorAtClick));
        assertFalse(mouseMan.isMouseClicked(actorAtClick));
        
    }
    
    public void testLongMouseDraggedOnActor() 
    {
        mouseMan.newActStarted();
        
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,  System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);        
        dispatch(event);     
        assertTrue(mouseMan.isMousePressed(actorAtClick));
        
        /// New act round where nothing happens
        mouseMan.newActStarted();        
        assertTrue(mouseMan.isMousePressed(actorAtClick));
        assertNotNull(mouseMan.getMouseInfo());
        
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
        assertTrue(mouseMan.isMouseDragged(actorAtClick));
        assertNotNull(mouseMan.getMouseInfo());
        
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
        assertTrue(mouseMan.isMouseMoved(actorAtClick));  
        mouseMan.newActStarted();
        event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,  System.currentTimeMillis(), 0, 6, 6, 1, false, MouseEvent.BUTTON1);        
        dispatch(event);             

        assertTrue(mouseMan.isMouseMoved(actorAtClick));
        
        assertFalse(mouseMan.isMouseClicked(actorAtClick));
        
    }

    /**
     * Tests behaviour when several buttons are pressed and dragged at the same
     * time. We just want to ensure that nothing crashes or produecs exceptions.
     * Otherwise the behaiour is undefined.
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
        
    }

    public void testButton3() {
        
    }
    
    
    /**
     * Test a world that has a cell size bigger than 1x1 
     */
    public void testBigCellSize() 
    {

        final World world = new World(20, 20, 10) {};
        final MouseManager mouseMan = new MouseManager(new WorldLocator(){
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
        assertFalse(mouseMan.isMouseClicked(actorAtClick));
        assertFalse(mouseMan.isMouseMoved(actorAtClick));
        assertFalse(mouseMan.isMouseDragged(actorAtClick));
        assertFalse(mouseMan.isMousePressed(actorAtClick));
       
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
        assertFalse(mouseMan.isMousePressed(actorAtClick));
        assertFalse(mouseMan.isMouseClicked(actorAtClick));
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
        assertFalse(mouseMan.isMousePressed(actorAtClick));
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
        assertFalse(mouseMan.isMousePressed(actorAtClick));
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
}
