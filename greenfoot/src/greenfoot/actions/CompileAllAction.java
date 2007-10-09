package greenfoot.actions;

import bluej.Config;
import greenfoot.core.GProject;
import greenfoot.core.Simulation;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;

import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * Action that compiles all classes that needs compilation.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: CompileAllAction.java 5310 2007-10-09 13:13:16Z polle $
 */
public class CompileAllAction extends AbstractAction
{
	private GProject project;
    
    public CompileAllAction(GProject project)
    {
        super(Config.getString("compile.all"));
        setProject(project);
    }
    
    public void setProject(GProject project)
    {
    	this.project = project;
    	setEnabled(project != null);
    }

    /**
     * Compiles all classes.
     *  
     */
    public void actionPerformed(ActionEvent e)
    {
        try {
            int numOfClasses = project.getDefaultPackage().getClasses().length;
            // we only want to compile if there are classes in the project
            if(numOfClasses < 1) {
                return;
            }
            Simulation.getInstance().setPaused(true);
        	project.getDefaultPackage().compileAll(false);
        }
        catch (ProjectNotOpenException pnoe) {}
        catch (PackageNotFoundException pnfe) {}
        catch (RemoteException re) {
        	re.printStackTrace();
        }
        catch (CompilationNotStartedException cnse) {
        	cnse.printStackTrace();
        }
        
        // Disable the action until the compilation is finished, when it
        // will be re-enabled.
        setEnabled(false);
    }
}