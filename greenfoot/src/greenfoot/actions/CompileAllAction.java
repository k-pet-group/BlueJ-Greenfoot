package greenfoot.actions;

import greenfoot.Greenfoot;
import greenfoot.Simulation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Action that compiles all classes that needs compilation.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: CompileAllAction.java 3124 2004-11-18 16:08:48Z polle $
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