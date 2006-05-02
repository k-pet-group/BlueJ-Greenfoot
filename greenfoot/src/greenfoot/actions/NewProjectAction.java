package greenfoot.actions;

import greenfoot.core.GreenfootMain;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: NewProjectAction.java 4062 2006-05-02 09:38:55Z mik $
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
        GreenfootMain.getInstance().newProject();
    }
}