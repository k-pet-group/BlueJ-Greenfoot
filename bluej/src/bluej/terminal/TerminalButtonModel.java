/**
 ** ButtonModel for the "Show Terminal" checkBoxItem in the menu.
 ** This model takes care that the right things happen when the checkbox
 ** is shown or changed.
 **
 ** @author Michael Kolling
 **/

package bluej.terminal;

import javax.swing.JToggleButton;

public class TerminalButtonModel extends JToggleButton.ToggleButtonModel
{
    public boolean isSelected()
    {
        return Terminal.getTerminal().isShown();
    }

    public void setSelected(boolean b)
    {
        super.setSelected(b);
        Terminal.getTerminal().showTerminal(b);

    }

}
