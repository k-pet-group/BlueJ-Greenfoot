package greenfoot.platforms.standalone;

import greenfoot.World;
import greenfoot.core.WorldHandler;
import greenfoot.gui.classbrowser.SelectionManager;
import greenfoot.platforms.WorldHandlerDelegate;

import java.awt.Component;
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
    
    public void attachProject(Object project)
    {
        //Not used in standalone
    }

    public void dragFinished(Object o)
    {
        worldHandler.finishDrag(o);
    }

    public Component getWorldTitle()
    {
        // Not used in standalone
        return null;
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

    public void reset()
    {
        // Not used in standalone
    }

    public void setQuickAddActive(boolean b)
    {
        // Not used in standalone
    }

    public void setSelectionManager(SelectionManager selectionManager)
    {
        // Not used in standalone
    }

    public void setWorld(World world, World world2)
    {
        // Not used in standalone
    }

    public void setWorldHandler(WorldHandler handler)
    {
        this.worldHandler = handler;
    }

}
