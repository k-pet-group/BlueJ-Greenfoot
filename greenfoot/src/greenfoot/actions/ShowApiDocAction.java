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
    private String page = "index.html";
    
    /**
     * Will show the index.html of the API Documentation
     * @param name
     */
    public ShowApiDocAction(String name)
    {
        super(name);
    }
    

    /**
     * Opens the given page of the Greenfoot API documentation in a web browser.
     * @param page name of the page relative to the root of the API doc.
     * @throws IOException If the greenfoot directory can not be read
     */
    public ShowApiDocAction(String name, String page)
    {
        super(name);
        this.page = page;
    }

    public void actionPerformed(ActionEvent e)
    {
        try {
            GreenfootUtil.showApiDoc(page);
            // TODO: show status message: browser opened
        }
        catch (IOException e1) {
            // TODO: show status message: problem opening  
        }   
       
    }
}
