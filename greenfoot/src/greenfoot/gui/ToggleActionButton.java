package greenfoot.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.JButton;

/**
 * A button that takes two Action's which is toggled between each time the
 * button is clicked.
 * 
 * @author Poul Henriksen
 * @version $Id: ToggleActionButton.java 3124 2004-11-18 16:08:48Z polle $
 */
public class ToggleActionButton extends JButton
{

    public ToggleActionButton(final Action initialAction, final Action otherAction)
    {
        super(initialAction);

        this.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                ToggleActionButton button = ToggleActionButton.this;
                if (button.getAction() == otherAction) {
                    button.setAction(initialAction);
                }
                else {
                    button.setAction(otherAction);
                }
            }
        });
    }
}