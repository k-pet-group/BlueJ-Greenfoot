package greenfoot.actions;

import greenfoot.core.GreenfootMain;
import greenfoot.core.Simulation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Action that compiles all classes that needs compilation.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: CompileAllAction.java 4062 2006-05-02 09:38:55Z mik $
 */
public class CompileAllAction extends AbstractAction
{
    private static CompileAllAction instance = new CompileAllAction();
    
    /**
     * Singleton factory method for action.
     */
    public static CompileAllAction getInstance()
    {
        return instance;
    }

    
    private CompileAllAction()
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
        GreenfootMain.getInstance().compileAll();
    }
}