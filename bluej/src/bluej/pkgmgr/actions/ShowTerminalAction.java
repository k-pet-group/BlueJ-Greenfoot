package bluej.pkgmgr.actions;

import java.awt.event.KeyEvent;

import javax.swing.ButtonModel;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Action to toggle display of terminal. This action provides a ButtonModel
 * which can be tied to a check-box.
 * 
 * @author Davin McCall
 * @version $Id: ShowTerminalAction.java 2505 2004-04-21 01:50:28Z davmac $
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
        super("menu.view.showTerminal", KeyEvent.VK_T);
    }
    
    public ButtonModel getToggleModel(PkgMgrFrame pmf)
    {
        return new bluej.terminal.TerminalButtonModel(pmf);
    }
}
