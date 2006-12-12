package greenfoot.actions;

import greenfoot.core.GreenfootMain;
import greenfoot.core.Simulation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Action that compiles all classes that needs compilation.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: CompileAllAction.java 4763 2006-12-12 01:32:12Z davmac $
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
        setEnabled(false);
    }

    /**
     * Compiles all classes.
     *  
     */
    public void actionPerformed(ActionEvent e)
    {
        Simulation.getInstance().setPaused(true);
        GreenfootMain.getInstance().compileAll();
        
        // Disable the action until the compilation is finished, when it
        // will be re-enabled.
        setEnabled(false);
    }
}