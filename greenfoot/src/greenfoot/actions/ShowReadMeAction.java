package greenfoot.actions;

import greenfoot.core.GreenfootMain;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Action that compiles all classes that needs compilation.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ShowReadMeAction.java 4279 2006-05-16 11:20:51Z davmac $
 */
public class ShowReadMeAction extends AbstractAction
{
    private static ShowReadMeAction instance = new ShowReadMeAction();
    
    /**
     * Singleton factory method for action.
     */
    public static ShowReadMeAction getInstance()
    {
        return instance;
    }

    
    private ShowReadMeAction()
    {
        super("Project Information");
        setEnabled(false);
    }

    /**
     * Compiles all classes.
     *  
     */
    public void actionPerformed(ActionEvent e)
    {
        GreenfootMain.getInstance().getProject().openReadme();
    }
}