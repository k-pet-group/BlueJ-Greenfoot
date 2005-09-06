package greenfoot.core;

import greenfoot.GreenfootImage;
import greenfoot.GreenfootObject;

import javax.swing.Action;

/**
 * This object is used when dragging greenfoot objects around. Because we do not
 * want objects ot be constructed until they are actually added into the world,
 * we need a temporary object to be dragged around , which represents the real
 * class that will be instantiated.
 * 
 * @author Poul Henriksen
 * 
 */
public class ObjectDragProxy extends GreenfootObject
{
    private Action realAction;

    public ObjectDragProxy(GreenfootImage dragImage, Action realAction)
    {
        setImage(dragImage);
        this.realAction = realAction;
    }

    /**
     * Create the real object
     * 
     */
    public void createRealObject()
    {
        realAction.actionPerformed(null);
    }

}
