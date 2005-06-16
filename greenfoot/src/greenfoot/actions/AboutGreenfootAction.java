package greenfoot.actions;

import greenfoot.gui.AboutGreenfoot;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JFrame;

import bluej.Boot;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id$
 */
public class AboutGreenfootAction extends AbstractAction
{

    private AboutGreenfoot aboutGreenfoot;
    private JFrame parent;

    public AboutGreenfootAction(String name, JFrame parent)
    {
        super(name);
        this.parent = parent;
    }

    public void actionPerformed(ActionEvent e)
    {
        if (aboutGreenfoot == null) {
            aboutGreenfoot = new AboutGreenfoot(parent, Boot.GREENFOOT_VERSION);
        }
        aboutGreenfoot.setVisible(true);
    }
}