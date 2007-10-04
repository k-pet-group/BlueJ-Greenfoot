package greenfoot.actions;

import bluej.Config;
import bluej.prefmgr.PrefMgrDialog;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

/**
 * @author Michael Kolling
 * @version $Id: PreferencesAction.java 5284 2007-10-04 04:09:40Z bquig $
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
        super(Config.getString("greenfoot.preferences"));
    }

    public void actionPerformed(ActionEvent e)
    {
        //PrefMgrDialog.showDialog();
        // this does currently not work. The preferences manager runs usually on the BlueJ VM,
        // and we are here on the Greenfoot VM...
        // TODO: class PrefMgr properly
    }
}