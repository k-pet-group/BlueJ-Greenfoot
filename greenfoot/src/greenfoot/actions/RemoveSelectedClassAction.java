package greenfoot.actions;

import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.classbrowser.ClassView;

import java.awt.event.ActionEvent;

/**
 * An action to remove the currently selected class.
 * 
 * @author davmac
 * @version $Id: RemoveSelectedClassAction.java 5154 2007-08-10 07:02:51Z davmac $
 */
public class RemoveSelectedClassAction extends ClassAction
{
    /**
     * Construct a remove action to remove the currently selected class.
     * The constructed action should be set as selection listener for the
     * class browser.
     */
    public RemoveSelectedClassAction(GreenfootFrame gfFrame)
    {
        super("Remove Class", gfFrame);
        setEnabled(false);
    }
    
    public void actionPerformed(ActionEvent e)
    {
    	ClassView selectedClassView = getSelectedClassView();
        selectedClassView.remove();
    }
}
