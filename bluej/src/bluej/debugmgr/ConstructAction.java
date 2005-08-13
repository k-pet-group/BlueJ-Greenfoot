package bluej.debugmgr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import bluej.debugmgr.objectbench.InvokeListener;
import bluej.views.ConstructorView;

/**
 * Simple action to construct an object.
 * 
 * @author Davin McCall
 * @version $Id$
 */
public class ConstructAction extends AbstractAction
{
    private ConstructorView constructor;
    private InvokeListener invokeListener;
    
    public ConstructAction(ConstructorView cv, InvokeListener il, String desc)
    {
        super(desc);
        constructor = cv;
        invokeListener = il;
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
        invokeListener.callConstructor(constructor);
    }

}
