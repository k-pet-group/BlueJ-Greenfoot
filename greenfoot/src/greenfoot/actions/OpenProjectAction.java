package greenfoot.actions;

import greenfoot.core.Greenfoot;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: OpenProjectAction.java 3551 2005-09-06 09:31:41Z polle $
 */
public class OpenProjectAction extends AbstractAction
{
    public void actionPerformed(ActionEvent e)
    {
        Thread t = new Thread() {
            public void run()
            {
                Greenfoot.getInstance().openProjectBrowser();
            }
        };
        t.start();
    }

    public OpenProjectAction(String name)
    {
        super(name);
    }

}