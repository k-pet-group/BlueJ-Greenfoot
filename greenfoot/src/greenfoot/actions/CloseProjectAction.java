package greenfoot.actions;

import greenfoot.core.GreenfootMain;
import greenfoot.gui.GreenfootFrame;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * @author Poul Henriksen
 * @version $Id$
 */
public class CloseProjectAction extends AbstractAction
{
	private GreenfootFrame gfFrame;
    
    public CloseProjectAction(GreenfootFrame gfFrame)
    {
        super("Close");
        this.gfFrame = gfFrame;
        setEnabled(false);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        GreenfootMain.closeProject(gfFrame, false);
    }
}