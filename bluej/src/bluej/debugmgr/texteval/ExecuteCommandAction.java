package bluej.debugmgr.texteval;

import java.awt.event.ActionEvent;
import javax.swing.*;

/**
 * Execute a java text statement.
 * 
 * @author Michael Kolling
 * @version $Id: ExecuteCommandAction.java 2630 2004-06-19 14:26:37Z polle $
 */

final public class ExecuteCommandAction extends AbstractAction {

    private TextEvalArea text;
    
    /**
     */
    public ExecuteCommandAction(TextEvalArea text)
    {
        super("ExecuteCommand");
        this.text = text;
    }
    
    final public void actionPerformed(ActionEvent event)
    {
        text.executeCommand();
    }

}
