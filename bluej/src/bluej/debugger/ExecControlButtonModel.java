/**
 ** ButtonModel for the "Show Execution Controls" checkBoxItem in the menu.
 ** This model takes care that the right things happen when the checkbox
 ** is shown or changed.
 **
 ** @author Michael Kolling
 **/

package bluej.debugger;

import javax.swing.JToggleButton;

import bluej.pkgmgr.PkgMgrFrame;

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
    	else
        	return pmf.getProject().getExecControls().isVisible();
        //ExecControls.execControlsShown();
    }

    public void setSelected(boolean b)
    {
		if (!pmf.isEmptyFrame()) {
			super.setSelected(b);
			pmf.getProject().getExecControls().showHide(b, true, null);
		}
//        ExecControls.showHide(b, true, null);
    }
}
