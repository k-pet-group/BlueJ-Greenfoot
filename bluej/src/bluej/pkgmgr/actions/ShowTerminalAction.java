package bluej.pkgmgr.actions;

import javax.swing.ButtonModel;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Action to toggle display of terminal. This action provides a ButtonModel
 * which can be tied to a check-box.
 * 
 * @author Davin McCall
 * @version $Id: ShowTerminalAction.java 4905 2007-03-29 06:06:30Z davmac $
 */

final public class ShowTerminalAction extends PkgMgrAction
{
    public ShowTerminalAction()
    {
        super("menu.view.showTerminal");
    }
    
    public ButtonModel getToggleModel(PkgMgrFrame pmf)
    {
        return new bluej.terminal.TerminalButtonModel(pmf);
    }
}
