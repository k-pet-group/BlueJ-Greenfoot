package bluej.pkgmgr.actions;

import javax.swing.ButtonModel;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Action to toggle display of terminal. This action provides a ButtonModel
 * which can be tied to a check-box.
 * 
 * @author Davin McCall
 * @version $Id: ShowTerminalAction.java 2571 2004-06-03 13:35:37Z fisker $
 */

final public class ShowTerminalAction extends PkgMgrAction
{
    
    static private ShowTerminalAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public ShowTerminalAction getInstance()
    {
        if(instance == null)
            instance = new ShowTerminalAction();
        return instance;
    }
    
    private ShowTerminalAction()
    {
        super("menu.view.showTerminal");
    }
    
    public ButtonModel getToggleModel(PkgMgrFrame pmf)
    {
        return new bluej.terminal.TerminalButtonModel(pmf);
    }
}
