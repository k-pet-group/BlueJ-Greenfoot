package greenfoot.actions;

import bluej.utility.Utility;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

/**
 * Action to open a URL in an external web browser.
 *
 * @author mik
 */
public class ShowWebsiteAction extends AbstractAction {
    
    private String url;
    
    /** Creates a new instance of ShowWebsiteAction */
    public ShowWebsiteAction(String name, String url) {
        super(name);
        this.url = url;
    }

    public void actionPerformed(ActionEvent e)
    {
        if (Utility.openWebBrowser(url)) {
            // TODO: show status message: browser opened
        }
        else {
            // TODO: show status message: problem opening            
        }      
    }
}
