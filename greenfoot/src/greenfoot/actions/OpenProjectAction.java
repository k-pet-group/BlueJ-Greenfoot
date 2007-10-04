package greenfoot.actions;

import bluej.Config;
import greenfoot.core.GreenfootMain;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: OpenProjectAction.java 5284 2007-10-04 04:09:40Z bquig $
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
        super(Config.getString("open.project"));
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