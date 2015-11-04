package greenfoot.actions;

import greenfoot.gui.GreenfootFrame;
import greenfoot.record.InteractionListener;

import java.awt.event.ActionEvent;


/**
 * Action that creates a new class as a subclass of an Actor class
 */
public class NewSubActorAction extends NewSubclassAction
{
    private final boolean wizard;
    private GreenfootFrame gfFrame;


    /**
     * Creates a new subclass of the Actor class
     * 
     * @param gfFrame 
     *            Greenfoot main frame
     * 
     * @param interactionListener
     *            The listener to be notified of interactions (instance creation, method calls)
     *            which occur on the new class.
     */
    public NewSubActorAction(GreenfootFrame gfFrame, boolean wizard, InteractionListener interactionListener)
    {
        super();
        this.gfFrame = gfFrame;
        this.wizard = wizard;
        this.interactionListener = interactionListener;
    }
    
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        this.classBrowser = gfFrame.getClassBrowser();
        this.superclass = classBrowser.getActorClassView();
        createImageClass((String)getValue(NAME), wizard ? "MyActor" : null, null);
    }
}