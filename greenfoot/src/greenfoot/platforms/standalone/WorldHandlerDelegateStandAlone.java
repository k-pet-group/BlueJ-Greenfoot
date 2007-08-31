package greenfoot.platforms.standalone;

import greenfoot.World;
import greenfoot.core.WorldHandler;
import greenfoot.export.GreenfootScenarioViewer;
import greenfoot.platforms.WorldHandlerDelegate;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;


/**
 * Implementation for running scenarios in a standalone application or applet.
 * 
 * @author Poul Henriksen
 *
 */
public class WorldHandlerDelegateStandAlone implements WorldHandlerDelegate
{    
    private WorldHandler worldHandler;
    private GreenfootScenarioViewer viewer;
    
    public WorldHandlerDelegateStandAlone (GreenfootScenarioViewer viewer) 
    {
        this.viewer = viewer;
    }
    
    public void dragFinished(Object o)
    {
        worldHandler.finishDrag(o);
    }

    public void keyReleased(KeyEvent e)
    {
        // Not used in standalone
    }

    public boolean maybeShowPopup(MouseEvent e)
    {
        // Not used in standalone
        return false;
    }

    public void mouseClicked(MouseEvent e)
    {
        // Not used in standalone
    }

    public void processKeyEvent(KeyEvent e)
    {
        // Not used in standalone
    }

    public void setQuickAddActive(boolean b)
    {
        // Not used in standalone
    }

    public void setWorld(final World oldWorld, final World newWorld)
    {
        ActorDelegateStandAlone.initWorld(newWorld);
    }

    public void setWorldHandler(WorldHandler handler)
    {
        this.worldHandler = handler;
    }

    public void instantiateNewWorld()
    {
        viewer.instantiateNewWorld();
    }

    public Class getLastWorldClass()
    {
        // Not used in standalone
        return null;
    }

}
