package bluej.groupwork;

import javax.swing.JToggleButton;

import bluej.pkgmgr.PkgMgrFrame;

/**
 *  ButtonModel for the "Show TeamControls" checkBoxItem in the menu.
 * This model takes care that the right things happen when the checkbox
 * is shown or changed.
 * 
 * @author fisker
 */
public class TeamControlsButtonModel extends JToggleButton.ToggleButtonModel
{
	private PkgMgrFrame pmf;
    
    public TeamControlsButtonModel(PkgMgrFrame pmf)
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
        else if (!pmf.getProject().hasTeamControls()) {
            return false;
        }
        else {
            // otherwise, ask the Terminal if it is visible
            return pmf.getProject().getTeamControls().isVisible();
        }
    }

    public void setSelected(boolean b)
    {
        if (!pmf.isEmptyFrame()) {
            super.setSelected(b);
            pmf.getProject().getTeamControls().showHide(b);
        }
    }
}
