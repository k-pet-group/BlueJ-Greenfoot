package greenfoot.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Action to display a copyright notice.
 *
 * @author mik
 */
public class ShowCopyrightAction extends AbstractAction
{
    private JFrame frame;
    
    /** 
     *  Creates a new instance of ShowCopyrightAction 
     */
    public ShowCopyrightAction(JFrame frame) 
    {
        super("Copyright...");
        this.frame = frame;
    }

    /**
     * The action was fired...
     */
    public void actionPerformed(ActionEvent e)
    {
            JOptionPane.showMessageDialog(frame, new String[]{
                "Greenfoot \u00a9 2005-2006 Michael K\u00F6lling, Poul Henriksen.", " ",
                "Greenfoot is available 'as is' free of charge for use and non-commercial", 
                "redistribution. Disassembly of the system is prohibited.",
                "This software may not be sold for profit or included in other", 
                "packages which are sold for profit without athorisation."}, 
                "Copyright, License and Redistribution", JOptionPane.INFORMATION_MESSAGE);
    }
}
