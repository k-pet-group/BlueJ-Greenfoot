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
    private PkgMgrFrame pkgMgr;

    public ExecControlButtonModel(PkgMgrFrame manager)
    {
	super();
	pkgMgr = manager;
    }

    public boolean isSelected()
    {
	return ((pkgMgr.getExecControls() != null) && 
		(pkgMgr.getExecControls().isShowing()));
    }

    public void setSelected(boolean b)
    {
	super.setSelected(b);
	pkgMgr.showHideExecControls(b, true);
    }

}
