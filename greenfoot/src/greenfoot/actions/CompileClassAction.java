package greenfoot.actions;

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
 * @version $Id: CompileClassAction.java 5154 2007-08-10 07:02:51Z davmac $
 */
public class CompileClassAction extends ClassAction
{
    public CompileClassAction(GreenfootFrame gfFrame)
    {
        super("Compile", gfFrame);
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