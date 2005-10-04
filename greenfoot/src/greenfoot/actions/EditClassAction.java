package greenfoot.actions;

import greenfoot.core.GClass;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: EditClassAction.java 3643 2005-10-04 12:37:52Z polle $
 */
public class EditClassAction extends ClassAction
{

    public EditClassAction(String name)
    {
        super(name);
    }
    
    public EditClassAction(String name, GClass cls)
    {
        super(name);
        selectedClass = cls;
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