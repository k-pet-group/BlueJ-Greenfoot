package greenfoot.actions;

import greenfoot.core.Greenfoot;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * @author Poul Henriksen
 * @version $Id$
 */
public class CloseProjectAction extends AbstractAction
{
    private static CloseProjectAction instance = new CloseProjectAction();
    
    /**
     * Singleton factory method for action.
     */
    public static CloseProjectAction getInstance()
    {
        return instance;
    }

    
    private CloseProjectAction()
    {
        super("Close");
    }
    
    public void actionPerformed(ActionEvent e)
    {
        Greenfoot.getInstance().closeThisInstance();
    }
}