package bluej.pkgmgr.actions;

import java.awt.event.KeyEvent;

import javax.swing.ButtonModel;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Action to toggle display of debugger. This action provides a ButtonModel
 * so that it can be tied to a check-box.
 * 
 * @author Davin McCall
 * @version $Id: ShowDebuggerAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class ShowDebuggerAction extends PkgMgrAction
{
    
    static private ShowDebuggerAction instance = null;

    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public ShowDebuggerAction getInstance()
    {
        if(instance == null)
            instance = new ShowDebuggerAction();
        return instance;
    }
    
    private ShowDebuggerAction()
    {
        super("menu.view.showExecControls", KeyEvent.VK_D);
    }
            
    public ButtonModel getToggleModel(PkgMgrFrame pmf)
    {
        return new bluej.debugmgr.ExecControlButtonModel(pmf);
    }
}
