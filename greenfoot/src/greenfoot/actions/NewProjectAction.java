package greenfoot.actions;

import bluej.Config;
import greenfoot.core.GreenfootMain;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: NewProjectAction.java 5284 2007-10-04 04:09:40Z bquig $
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
        super(Config.getString("new.project"));
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