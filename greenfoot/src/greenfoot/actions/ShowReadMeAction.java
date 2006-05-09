package greenfoot.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

/**
 * Action that compiles all classes that needs compilation.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ShowReadMeAction.java 4142 2006-05-09 09:39:27Z mik $
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
    }

    /**
     * Compiles all classes.
     *  
     */
    public void actionPerformed(ActionEvent e)
    {
        JOptionPane.showMessageDialog(null, "Not Yet Implemented - sorry.");
    }
}