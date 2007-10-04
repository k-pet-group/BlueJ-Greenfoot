package greenfoot.actions;

import bluej.Config;
import greenfoot.core.GClass;
import greenfoot.core.Simulation;
import greenfoot.gui.GreenfootFrame;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: CompileClassAction.java 5284 2007-10-04 04:09:40Z bquig $
 */
public class CompileClassAction extends ClassAction
{
    public CompileClassAction(GreenfootFrame gfFrame)
    {
        super(Config.getString("compile.class"), gfFrame);
    }

    /**
     * Compiles the currently selected class. If no class is selected it does
     * nothing.
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
    	GClass selectedClass = getSelectedClassView().getGClass();
    	
        Simulation.getInstance().setPaused(true);
        try {
            if (selectedClass != null) {
                selectedClass.compile(false);
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
        catch (CompilationNotStartedException e1) {
            e1.printStackTrace();
        }
    }
}