package greenfoot.actions;

import greenfoot.Greenfoot;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: OpenProjectAction.java 3124 2004-11-18 16:08:48Z polle $
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