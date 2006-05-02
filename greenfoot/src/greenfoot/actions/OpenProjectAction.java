package greenfoot.actions;

import greenfoot.core.GreenfootMain;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: OpenProjectAction.java 4062 2006-05-02 09:38:55Z mik $
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
                GreenfootMain.getInstance().openProjectBrowser();
            }
        };
        t.start();
    }
}