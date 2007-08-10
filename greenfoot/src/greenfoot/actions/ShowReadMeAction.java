package greenfoot.actions;

import greenfoot.gui.GreenfootFrame;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Action that compiles all classes that needs compilation.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ShowReadMeAction.java 5154 2007-08-10 07:02:51Z davmac $
 */
public class ShowReadMeAction extends AbstractAction
{
	private GreenfootFrame gfFrame;
    
    public ShowReadMeAction(GreenfootFrame gfFrame)
    {
        super("Project Information");
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