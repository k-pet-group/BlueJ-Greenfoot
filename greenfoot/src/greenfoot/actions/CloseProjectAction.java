package greenfoot.actions;

import greenfoot.core.GreenfootMain;
import greenfoot.gui.GreenfootFrame;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import bluej.Config;

/**
 * @author Poul Henriksen
 * @version $Id$
 */
public class CloseProjectAction extends AbstractAction
{
	private GreenfootFrame gfFrame;
    
    public CloseProjectAction(GreenfootFrame gfFrame)
    {
        super(Config.getString("project.close"));
        this.gfFrame = gfFrame;
        setEnabled(false);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        GreenfootMain.closeProject(gfFrame, false);
    }
}