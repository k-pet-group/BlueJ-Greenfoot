package greenfoot.actions;

import greenfoot.gui.GreenfootFrame;

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
    private GreenfootFrame gfFrame;
    
    public SaveProjectAction(GreenfootFrame gfFrame)
    {
        super("Save");
        this.gfFrame = gfFrame;
        setEnabled(false);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        try {
            gfFrame.getProject().save();
        }
        catch (ProjectNotOpenException e1) {
            Debug.reportError("Could not save scenario because it is not open.");
        }
        catch (RemoteException e1) {
            Debug.reportError("Could not save scenario because of a remote exception.");
            e1.printStackTrace();
        }
    }
}