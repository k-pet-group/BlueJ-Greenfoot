package bluej.debugmgr.texteval;

import java.awt.event.ActionEvent;
import javax.swing.*;

/**
 * Execute a java text statement.
 * 
 * @author Michael Kšlling
 * @version $Id: ExecuteCommandAction.java 2612 2004-06-14 20:36:28Z mik $
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
