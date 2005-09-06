package greenfoot.actions;

import greenfoot.core.Greenfoot;
import greenfoot.core.Simulation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Action that compiles all classes that needs compilation.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: CompileAllAction.java 3551 2005-09-06 09:31:41Z polle $
 */
public class CompileAllAction extends AbstractAction
{

    /**
     * Compiles the currently selected class. If no class is selected it does
     * nothing.
     *  
     */
    public void actionPerformed(ActionEvent e)
    {
        Simulation.getInstance().setPaused(true);
        Greenfoot.getInstance().compileAll();
    }

    public CompileAllAction(String name)
    {
        super(name);
    }

}