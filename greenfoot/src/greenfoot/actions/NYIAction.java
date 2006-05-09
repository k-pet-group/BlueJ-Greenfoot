package greenfoot.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * NYIAction: An action to display a NYI (Not Yet Implemented) dialogue.
 *
 * @author mik
 */
public class NYIAction extends AbstractAction
{
    private JFrame parent;

    /** 
     * Creates a new instance of NYIAction 
     */
    public NYIAction(String name, JFrame parent) 
    {
        super(name);
        setEnabled(false);
        this.parent = parent;
    }

    public void actionPerformed(ActionEvent e)
    {
        JOptionPane.showMessageDialog(parent, "Not Yet Implemented - sorry.");
    }
   
}
