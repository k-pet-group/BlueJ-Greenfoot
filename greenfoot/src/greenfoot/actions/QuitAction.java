package greenfoot.actions;

import greenfoot.core.GreenfootMain;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * An action to Quit (close all Greenfoot projects and exit the application)
 * 
 * @author Davin McCall
 * @version $Id: QuitAction.java 4351 2006-06-12 04:31:35Z davmac $
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
        super("Quit");
    }
    
    public void actionPerformed(ActionEvent e)
    {
        GreenfootMain.closeAll();
    }
}
