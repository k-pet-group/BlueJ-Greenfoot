package greenfoot.actions;

import greenfoot.util.GreenfootUtil;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;

/**
 * Action to open the Greenfoot API documentation in an external web browser.
 *
 * @author Poul Henriksen
 */
public class ShowApiDocAction extends AbstractAction {
    

    public ShowApiDocAction(String name)
    {
        super(name);
    }

    public void actionPerformed(ActionEvent e)
    {
        try {
            GreenfootUtil.showApiDoc("index.html");
            // TODO: show status message: browser opened
        }
        catch (IOException e1) {
            // TODO: show status message: problem opening  
        }   
       
    }
}
