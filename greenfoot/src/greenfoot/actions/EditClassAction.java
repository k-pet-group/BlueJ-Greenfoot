package greenfoot.actions;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: EditClassAction.java 3124 2004-11-18 16:08:48Z polle $
 */
public class EditClassAction extends ClassAction
{

    public EditClassAction(String name)
    {
        super(name);
    }

    /**
     * Edits the currently selected class. If no class is selected it does
     * nothing.
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
        try {
            if (selectedClass != null) {
                selectedClass.edit();
            }

        }
        catch (RemoteException e1) {
            e1.printStackTrace();
        }
        catch (ProjectNotOpenException e1) {
            e1.printStackTrace();
        }
        catch (PackageNotFoundException e1) {
            e1.printStackTrace();
        }
    }

}