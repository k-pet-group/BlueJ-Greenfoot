package greenfoot.actions;

import greenfoot.core.GreenfootMain;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;

import bluej.extensions.ProjectNotOpenException;
import bluej.utility.Debug;

/**
 * @author Poul Henriksen
 * @version $Id$
 */
public class SaveProjectAction extends AbstractAction
{
    private static SaveProjectAction instance = new SaveProjectAction();
    
    /**
     * Singleton factory method for action.
     */
    public static SaveProjectAction getInstance()
    {
        return instance;
    }

    
    private SaveProjectAction()
    {
        super("Save");
    }
    
    public void actionPerformed(ActionEvent e)
    {
        try {
            GreenfootMain.getInstance().getProject().save();
        }
        catch (ProjectNotOpenException e1) {
            Debug.reportError("Could not save project because it is not open.");
        }
        catch (RemoteException e1) {
            Debug.reportError("Could not save project because of a remote exception.");
            e1.printStackTrace();
        }
    }
}