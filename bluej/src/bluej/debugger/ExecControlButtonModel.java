package bluej.debugger;

import javax.swing.JToggleButton;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * ButtonModel for the "Show Execution Controls" checkBoxItem in the menu.
 * This model takes care that the right things happen when the checkbox
 * is shown or changed.
 *
 * @author Michael Kolling
 */
public class ExecControlButtonModel extends JToggleButton.ToggleButtonModel
{
	private PkgMgrFrame pmf;
	
    public ExecControlButtonModel(PkgMgrFrame pmf)
    {
        super();
        this.pmf = pmf;
    }

    public boolean isSelected()
    {
    	if (pmf.isEmptyFrame())
    		return false;
    	else if (!pmf.getProject().hasExecControls())
    		return false;
    	else
        	return pmf.getProject().getExecControls().isVisible();
    }

    public void setSelected(boolean b)
    {
		if (!pmf.isEmptyFrame()) {
			super.setSelected(b);
			pmf.getProject().getExecControls().showHide(b, true, null);
		}
    }
}
