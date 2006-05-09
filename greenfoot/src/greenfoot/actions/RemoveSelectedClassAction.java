package greenfoot.actions;

import java.awt.event.ActionEvent;

/**
 * An action to remove the currently selected class.
 * 
 * @author davmac
 * @version $Id: RemoveSelectedClassAction.java 4154 2006-05-09 12:59:31Z mik $
 */
public class RemoveSelectedClassAction extends ClassAction
{
    private static RemoveSelectedClassAction instance = new RemoveSelectedClassAction();
    
    /**
     * Singleton factory method for action.
     */
    public static synchronized RemoveSelectedClassAction getInstance()
    {
        return instance;
    }

    
    /**
     * Construct a remove action to remove the currently selected class.
     * The constructed action should be set as selection listener for the
     * class browser.
     */
    private RemoveSelectedClassAction()
    {
        super("Remove Class");
        setEnabled(false);
    }
    

    public void actionPerformed(ActionEvent e)
    {
        selectedClassView.remove();
    }
}
