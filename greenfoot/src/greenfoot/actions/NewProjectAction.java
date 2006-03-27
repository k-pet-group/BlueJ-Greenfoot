package greenfoot.actions;

import greenfoot.core.Greenfoot;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: NewProjectAction.java 3890 2006-03-27 16:04:42Z mik $
 */
public class NewProjectAction extends AbstractAction
{
    private static NewProjectAction instance = new NewProjectAction();
    
    /**
     * Singleton factory method for action.
     */
    public static NewProjectAction getInstance()
    {
        return instance;
    }

    
    private NewProjectAction()
    {
        super("New...");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
        Greenfoot.getInstance().newProject();
    }
}