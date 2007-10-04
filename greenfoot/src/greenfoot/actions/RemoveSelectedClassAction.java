package greenfoot.actions;

import bluej.Config;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.classbrowser.ClassView;

import java.awt.event.ActionEvent;

/**
 * An action to remove the currently selected class.
 * 
 * @author davmac
 * @version $Id: RemoveSelectedClassAction.java 5284 2007-10-04 04:09:40Z bquig $
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
        super(Config.getString("remove.selected"), gfFrame);
        setEnabled(false);
    }
    
    public void actionPerformed(ActionEvent e)
    {
    	ClassView selectedClassView = getSelectedClassView();
        selectedClassView.remove();
    }
}
