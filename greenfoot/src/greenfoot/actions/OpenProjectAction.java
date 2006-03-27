package greenfoot.actions;

import greenfoot.core.Greenfoot;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: OpenProjectAction.java 3890 2006-03-27 16:04:42Z mik $
 */
public class OpenProjectAction extends AbstractAction
{
    private static OpenProjectAction instance = new OpenProjectAction();
    
    /**
     * Singleton factory method for action.
     */
    public static OpenProjectAction getInstance()
    {
        return instance;
    }

    
    private OpenProjectAction()
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