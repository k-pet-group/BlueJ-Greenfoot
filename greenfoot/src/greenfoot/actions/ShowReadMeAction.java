package greenfoot.actions;

import bluej.Config;
import greenfoot.gui.GreenfootFrame;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Action that compiles all classes that needs compilation.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ShowReadMeAction.java 5284 2007-10-04 04:09:40Z bquig $
 */
public class ShowReadMeAction extends AbstractAction
{
	private GreenfootFrame gfFrame;
    
    public ShowReadMeAction(GreenfootFrame gfFrame)
    {
        super(Config.getString("show.readme"));
    	this.gfFrame = gfFrame;
        setEnabled(false);
    }

    /**
     * Compiles all classes.
     *  
     */
    public void actionPerformed(ActionEvent e)
    {
        gfFrame.getProject().openReadme();
    }
}