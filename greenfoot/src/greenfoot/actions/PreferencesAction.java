package greenfoot.actions;

import bluej.prefmgr.PrefMgrDialog;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

/**
 * @author Michael Kolling
 * @version $Id: PreferencesAction.java 4963 2007-04-19 09:44:05Z mik $
 */
public class PreferencesAction extends AbstractAction
{
    private static PreferencesAction instance;
    
     /**
     * Singleton factory method for action.
     */
    public static PreferencesAction getInstance()
    {
        if(instance == null)
            instance = new PreferencesAction();
        return instance;
    }
    
    
    private PreferencesAction()
    {
        super("Preferences...");
    }

    public void actionPerformed(ActionEvent e)
    {
        //PrefMgrDialog.showDialog();
        // this does currently not work. The preferences manager runs usually on the BlueJ VM,
        // and we are here on the Greenfoot VM...
        // TODO: class PrefMgr properly
    }
}