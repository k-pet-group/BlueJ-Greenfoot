package greenfoot.actions;

import greenfoot.core.Greenfoot;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: OpenProjectAction.java 3879 2006-03-25 20:40:14Z mik $
 */
public class OpenProjectAction extends AbstractAction
{
    public OpenProjectAction()
    {
        super("Open...");
    }

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
}