package greenfoot.mouse;

import greenfoot.Actor;
import greenfoot.MouseInfo;
import greenfoot.TestObject;
import greenfoot.World;
import greenfoot.mouse.MouseManager;
import greenfoot.platforms.GreenfootUtilDelegate;
import greenfoot.util.GreenfootUtil;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.swing.JPanel;

import junit.framework.TestCase;
/**
 * Tests of the implementation of the Greenfoot mouse support. 
 * TODO drags on boundaries
 * TODO things happening outside actors
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
                List<?> actors = world.getObjectsAt(e.getX(), e.getY(), TestObject.class);
                if(actors.isEmpty()) {
                    return null;
                }
                return (Actor) actors.get(0);
            }

            public int getTranslatedX(MouseEvent e)
            {
                return e.getX();
            }

            public int getTranslatedY(MouseEvent e)
            {
                return e.getY();
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

        assertTrue(mouseMan.isMouseDragged(actorAtClick));
        assertTrue(mouseMan.isMousePressed(actorAtClick));
        assertTrue(mouseMan.isMouseDragEnded(actorAtClick));
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
    
    public void testButton2() {
        
    }

    public void testButton3() {
        
    }
    
}
