package bluej.pkgmgr.actions;

import javax.swing.ButtonModel;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Action to toggle display of debugger. This action provides a ButtonModel
 * so that it can be tied to a check-box.
 * 
 * @author Davin McCall
 * @version $Id: ShowDebuggerAction.java 4905 2007-03-29 06:06:30Z davmac $
 */
final public class ShowDebuggerAction extends PkgMgrAction
{
    public ShowDebuggerAction()
    {
        super("menu.view.showExecControls");
    }
            
    public ButtonModel getToggleModel(PkgMgrFrame pmf)
    {
        return new bluej.debugmgr.ExecControlButtonModel(pmf);
    }
}
