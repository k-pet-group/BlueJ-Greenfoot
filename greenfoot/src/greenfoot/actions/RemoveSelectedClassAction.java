package greenfoot.actions;

import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.classbrowser.ClassView;

import java.awt.event.ActionEvent;

import javax.swing.JFrame;

import bluej.Config;

/**
 * An action to remove the currently selected class.
 * 
 * @author davmac
 * @version $Id: RemoveSelectedClassAction.java 5507 2008-01-29 15:37:54Z polle $
 */
public class RemoveSelectedClassAction extends ClassAction
{
    private JFrame frame;

    /**
     * Construct a remove action to remove the currently selected class.
     * The constructed action should be set as selection listener for the
     * class browser.
     */
    public RemoveSelectedClassAction(GreenfootFrame gfFrame)
    {
        super(Config.getString("remove.selected"), gfFrame);
        setEnabled(false);
        this.frame = gfFrame;
    }
    
    public void actionPerformed(ActionEvent e)
    {
    	ClassView cls = getSelectedClassView();
    	
    	boolean confirmed = RemoveClassAction.confirmRemoveClass(cls, frame);
        if (confirmed) {
            cls.remove();
        }
    }

}
