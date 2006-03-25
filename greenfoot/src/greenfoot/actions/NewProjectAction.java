package greenfoot.actions;

import greenfoot.core.Greenfoot;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: NewProjectAction.java 3879 2006-03-25 20:40:14Z mik $
 */
public class NewProjectAction extends AbstractAction
{
    public NewProjectAction()
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