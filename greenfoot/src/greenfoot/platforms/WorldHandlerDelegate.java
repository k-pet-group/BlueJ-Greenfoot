package greenfoot.platforms;

import greenfoot.World;
import greenfoot.core.WorldHandler;
import greenfoot.gui.input.InputManager;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;


/**
 * Interface to classes that contain specialized behaviour for the WorldHandler
 * depending on where and how the greenfoot project is running.
 * 
 * @author Poul Henriksen
 *
 */
public interface WorldHandlerDelegate
{
    /**
     * Show the popup menu if the mouseevent is a popup trigger.
     */
    boolean maybeShowPopup(MouseEvent e);

    void mouseClicked(MouseEvent e);

    void setQuickAddActive(boolean b);

    void processKeyEvent(KeyEvent e);

    void keyReleased(KeyEvent e);

    void setWorld(World oldWorld, World newWorld);

    void dragFinished(Object o);

    void setWorldHandler(WorldHandler handler);
    
    /**
     * Instantiate a new world and do any initialisation needed to activate that world.
     */
    void instantiateNewWorld();

    Class getLastWorldClass();

    InputManager getInputManager();

    void discardWorld(World world);
}
