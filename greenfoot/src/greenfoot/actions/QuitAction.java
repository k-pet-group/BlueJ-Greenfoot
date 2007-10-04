package greenfoot.actions;

import bluej.Config;
import greenfoot.core.GreenfootMain;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * An action to Quit (close all Greenfoot projects and exit the application)
 * 
 * @author Davin McCall
 * @version $Id: QuitAction.java 5284 2007-10-04 04:09:40Z bquig $
 */
public class QuitAction extends AbstractAction
{
    private static QuitAction instance;
    
    public static QuitAction getInstance()
    {
        if (instance == null) {
            instance = new QuitAction();
        }
        return instance;
    }
    
    private QuitAction()
    {
        super(Config.getString("greenfoot.quit"));
    }
    
    public void actionPerformed(ActionEvent e)
    {
        GreenfootMain.closeAll();
    }
}
