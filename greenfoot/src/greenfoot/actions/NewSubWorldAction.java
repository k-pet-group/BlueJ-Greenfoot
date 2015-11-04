package greenfoot.actions;

import bluej.extensions.SourceType;
import greenfoot.gui.GreenfootFrame;
import greenfoot.record.InteractionListener;

import java.awt.event.ActionEvent;

/**
 * Action that creates a new class as a subclass of an World class
 */
public class NewSubWorldAction extends NewSubclassAction
{
    private final GreenfootFrame gfFrame;
    private final boolean wizard;
    SourceType sourceType;

    /**
     * Creates a new subclass of the World class
     * 
     * @param gfFrame 
     *            Greenfoot main frame
     * 
     * @param wizard Whether this action is caused by the new scenario wizard.
     * 
     * @param interactionListener
     *            The listener to be notified of interactions (instance creation, method calls)
     *            which occur on the new class.
     */
    public NewSubWorldAction(GreenfootFrame gfFrame, boolean wizard, SourceType sourceType, InteractionListener interactionListener)
    {
        super();
        this.gfFrame = gfFrame;
        this.wizard = wizard;
        this.sourceType = sourceType;
        this.interactionListener = interactionListener;
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        this.classBrowser = gfFrame.getClassBrowser();
        this.superclass = classBrowser.getWorldClassView();
        if (wizard) {
            createClassSilently("MyWorld", sourceType);
        }
        else {
            createImageClass((String) getValue(NAME), null, null);
        }
    }
}