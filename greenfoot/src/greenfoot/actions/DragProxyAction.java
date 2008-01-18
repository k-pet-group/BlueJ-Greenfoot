package greenfoot.actions;

import greenfoot.GreenfootImage;
import greenfoot.core.ObjectDragProxy;
import greenfoot.core.WorldHandler;
import greenfoot.gui.DragGlassPane;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 * An action that creates an ObjectDragProxy when performed and initiates a drag
 * with that ObjectDragProxy.
 * 
 * @author Poul Henriksen
 * 
 */
public class DragProxyAction extends AbstractAction
{

    private GreenfootImage dragImage;
    private Action dropAction;

    public DragProxyAction(GreenfootImage dragImage, Action dropAction)
    {
        super((String)dropAction.getValue(Action.NAME));
        this.dragImage = dragImage;
        this.dropAction = dropAction;
    }

    public void actionPerformed(ActionEvent e)
    {
        ObjectDragProxy object = new ObjectDragProxy(dragImage, dropAction);
        WorldHandler.getInstance().getInputManager().objectAdded();
        DragGlassPane.getInstance().startDrag(object, null, null, true);
    }

}
