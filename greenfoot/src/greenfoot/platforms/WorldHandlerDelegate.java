package greenfoot.platforms;

import greenfoot.World;
import greenfoot.core.WorldHandler;
import greenfoot.gui.classbrowser.SelectionManager;

import java.awt.Component;
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

    void setSelectionManager(SelectionManager selectionManager);

    void mouseClicked(MouseEvent e);

    void setQuickAddActive(boolean b);

    void processKeyEvent(KeyEvent e);

    void keyReleased(KeyEvent e);

    void attachProject(Object project);

    void setWorld(World world, World world2);

    Component getWorldTitle();

    void dragFinished(Object o);

    void reset();

    void setWorldHandler(WorldHandler handler);
}
