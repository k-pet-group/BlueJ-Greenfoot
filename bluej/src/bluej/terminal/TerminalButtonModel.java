package bluej.terminal;

import javax.swing.JToggleButton;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * ButtonModel for the "Show Debugger" checkBoxItem in the menu.
 * This model takes care that the right things happen when the checkbox
 * is shown or changed.
 *
 * @author Michael Kolling
 */
public class TerminalButtonModel extends JToggleButton.ToggleButtonModel
{
    private PkgMgrFrame pmf;
    
    public TerminalButtonModel(PkgMgrFrame pmf)
    {
        super();
        this.pmf = pmf;
    }

    public boolean isSelected()
    {
        if (pmf.isEmptyFrame()) {
            // if no project is open, we default to off
            return false;
        }
        else if (!pmf.getProject().hasTerminal()) {
            return false;
        }
        else {
            // otherwise, ask the Terminal if it is visible
            return pmf.getProject().getTerminal().isVisible();
        }
    }

    public void setSelected(boolean b)
    {
        if (!pmf.isEmptyFrame()) {
            super.setSelected(b);
            pmf.getProject().getTerminal().showHide(b);
        }
    }
}
