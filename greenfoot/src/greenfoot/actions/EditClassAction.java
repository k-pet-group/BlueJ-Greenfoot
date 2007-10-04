package greenfoot.actions;

import bluej.Config;
import greenfoot.core.GClass;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;

import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: EditClassAction.java 5284 2007-10-04 04:09:40Z bquig $
 */
public class EditClassAction extends AbstractAction
{
	private ClassBrowser classBrowser;
	
    public EditClassAction(ClassBrowser classBrowser)
    {
        super(Config.getString("edit.class"));
        this.classBrowser = classBrowser;
    }
    
    /**
     * Edits the currently selected class. If no class is selected it does
     * nothing.
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
    	ClassView selectedView = (ClassView) classBrowser.getSelectionManager().getSelected();
    	GClass selectedClass = selectedView.getGClass();
    	
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