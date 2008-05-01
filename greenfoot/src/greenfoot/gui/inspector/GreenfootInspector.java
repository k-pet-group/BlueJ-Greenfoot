package greenfoot.gui.inspector;

import greenfoot.Actor;
import greenfoot.core.WorldHandler;
import greenfoot.gui.input.InputManager;
import greenfoot.localdebugger.LocalObject;
import bluej.debugmgr.inspector.Inspector;

/**
 * Contains methods used by the inspector in greenfoot.
 * 
 * @author Poul Henriksen
 */
public class GreenfootInspector
{
    /**
     * Whether the Get button should be enabled.
     * @return True if the selected object is an actor
     */
    static boolean isGetEnabled(Object selectedObject)
    {
        if (selectedObject != null && selectedObject instanceof LocalObject) {
            Object obj = ((LocalObject) selectedObject).getObject();
            if (obj != null && obj instanceof Actor) {
                return true;
            }
        }
        return false;
    }


    /**
     * The "Get" button was pressed. Start dragging the selected object.
     */
    static void doGet(Object selectedObject)
    {
        Object obj = ((LocalObject) selectedObject).getObject();
        InputManager inputManager = WorldHandler.getInstance().getInputManager();
        inputManager.objectAdded((Actor) obj);
    }
    
    /**
     * Should be called from event thread.
     */
    static void makeGreenfootTitle(Inspector insp)
    {
        String title = insp.getTitle();
        String newTitle = title.replaceAll("BlueJ", "Greenfoot");
        insp.setTitle(newTitle);
    }    
}
