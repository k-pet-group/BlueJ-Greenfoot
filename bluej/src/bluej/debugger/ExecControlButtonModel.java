/**
 ** ButtonModel for the "Show Execution Controls" checkBoxItem in the menu.
 ** This model takes care that the right things happen when the checkbox
 ** is shown or changed.
 **
 ** @author Michael Kolling
 **/

package bluej.debugger;

import javax.swing.JToggleButton;

public class ExecControlButtonModel extends JToggleButton.ToggleButtonModel
{
    public ExecControlButtonModel()
    {
        super();
    }

    public boolean isSelected()
    {
        return true; //ExecControls.execControlsShown();
    }

    public void setSelected(boolean b)
    {
        super.setSelected(b);
//        ExecControls.showHide(b, true, null);
    }
}
