package greenfoot.actions;

import greenfoot.core.Simulation;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: CompileClassAction.java 3890 2006-03-27 16:04:42Z mik $
 */
public class CompileClassAction extends ClassAction
{
    private static CompileClassAction instance = new CompileClassAction();
    
    /**
     * Singleton factory method for action.
     */
    public static CompileClassAction getInstance()
    {
        return instance;
    }

    
    private CompileClassAction()
    {
        super("Compile");
    }

    /**
     * Compiles the currently selected class. If no class is selected it does
     * nothing.
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
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