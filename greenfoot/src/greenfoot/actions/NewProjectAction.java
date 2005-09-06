package greenfoot.actions;

import greenfoot.core.Greenfoot;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: NewProjectAction.java 3551 2005-09-06 09:31:41Z polle $
 */
public class NewProjectAction extends AbstractAction
{

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
        Greenfoot.getInstance().newProject();
    }

    public NewProjectAction(String name)
    {
        super(name);
    }
}