package greenfoot.actions;

import greenfoot.core.Greenfoot;
import greenfoot.core.Simulation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Action that compiles all classes that needs compilation.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: CompileAllAction.java 3879 2006-03-25 20:40:14Z mik $
 */
public class CompileAllAction extends AbstractAction
{

    public CompileAllAction()
    {
        super("Compile All");
    }

    /**
     * Compiles all classes.
     *  
     */
    public void actionPerformed(ActionEvent e)
    {
        Simulation.getInstance().setPaused(true);
        Greenfoot.getInstance().compileAll();
    }
}