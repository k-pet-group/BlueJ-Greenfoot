package greenfoot.actions;

import bluej.utility.Debug;
import greenfoot.core.GreenfootMain;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;

/**
 * @author Bruce Quig
 * @version $Id: OpenProjectAction.java 4062 2006-05-02 09:38:55Z mik $
 */
public class OpenRecentProjectAction extends AbstractAction
{
    private static OpenRecentProjectAction instance = new OpenRecentProjectAction();
    
    /**
     * Singleton factory method for action.
     * @return singleton instance of the action for this VM
     */
    public static OpenRecentProjectAction getInstance()
    {
        return instance;
    }
    
    
    private OpenRecentProjectAction()
    {
        super("Open recent...");
    }
    
    public void actionPerformed(final ActionEvent e)
    {
        Object obj = e.getSource();
        if(obj instanceof JMenuItem){
            final JMenuItem item = (JMenuItem)obj;
            Thread t = new Thread(){
                public void run() {
                    try {
                        GreenfootMain.getInstance().openProject(item.getText());
                    }
                    catch(RemoteException ex){
                        ex.printStackTrace();
                    }
                }
            };
            t.start();
        }
    }
}