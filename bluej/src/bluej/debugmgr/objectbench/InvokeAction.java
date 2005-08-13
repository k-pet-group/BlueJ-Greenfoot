package bluej.debugmgr.objectbench;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import bluej.views.MethodView;

/**
 * Simple action representing an interactive method invocation.
 *  
 * @author Davin McCall
 * @version $Id: InvokeAction.java 3519 2005-08-13 18:01:44Z polle $
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
