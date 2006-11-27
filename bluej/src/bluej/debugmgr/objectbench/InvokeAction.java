package bluej.debugmgr.objectbench;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import bluej.views.MethodView;

/**
 * Simple action representing an interactive method invocation.
 *  
 * @author Davin McCall
 * @version $Id: InvokeAction.java 4708 2006-11-27 00:47:57Z bquig $
 */
public class InvokeAction extends AbstractAction
{
    MethodView methodView;
    InvokeListener invokeListener;
    
    /**
     * Constructor for an InvokeAction.
     * 
     * @param methodView   The method to be invoked
     * @param il           The listener to be notified
     * @param desc         The method description (as appearing on menu)
     */
    public InvokeAction(MethodView methodView, InvokeListener il, String desc)
    {
        super(desc);
        this.methodView = methodView;
        this.invokeListener = il;
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
        invokeListener.executeMethod(methodView);
    }
}
