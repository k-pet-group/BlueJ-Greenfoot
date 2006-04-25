package greenfoot.actions;

import java.awt.event.ActionEvent;

/**
 * An action to remove the currently selected class.
 * 
 * @author davmac
 * @version $Id: RemoveSelectedClassAction.java 4017 2006-04-25 17:51:23Z davmac $
 */
public class RemoveSelectedClassAction extends ClassAction
{
    /**
     * Construct a remove action to remove the currently selected class.
     * The constructed action should be set as selection listener for the
     * class browser.
     */
    public RemoveSelectedClassAction()
    {
        super("Remove Class");
        setEnabled(false);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        selectedClassView.remove();
    }
}
