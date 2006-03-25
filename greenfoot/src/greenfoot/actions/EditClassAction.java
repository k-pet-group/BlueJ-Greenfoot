package greenfoot.actions;

import greenfoot.core.GClass;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: EditClassAction.java 3879 2006-03-25 20:40:14Z mik $
 */
public class EditClassAction extends ClassAction
{

    public EditClassAction()
    {
        super("Open editor");
    }
    
    public EditClassAction(GClass cls)
    {
        super("Open editor");
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