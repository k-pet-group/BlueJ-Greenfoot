package greenfoot.actions;

import greenfoot.gui.AboutGreenfootDialog;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import bluej.Boot;
import bluej.Config;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id$
 */
public class AboutGreenfootAction extends AbstractAction
{
    private static AboutGreenfootAction instance;
    
     /**
     * Singleton factory method for action.
     */
    public static AboutGreenfootAction getInstance(JFrame parent)
    {
        if(instance == null)
            instance = new AboutGreenfootAction(parent);
        return instance;
    }
    
    
    private AboutGreenfootDialog aboutGreenfoot;
    private JFrame parent;

    private AboutGreenfootAction(JFrame parent)
    {
        super(Config.getString("greenfoot.about"));
        this.parent = parent;
    }

    public void actionPerformed(ActionEvent e)
    {
        if (aboutGreenfoot == null) {
            aboutGreenfoot = new AboutGreenfootDialog(parent, Boot.GREENFOOT_VERSION);
        }
        aboutGreenfoot.setVisible(true);
    }
}