package bluej.testmgr;

import javax.swing.JToggleButton;

/**
 * ButtonModel for the "Show Test Runner" checkBoxItem in the menu.
 * This model takes care that the right things happen when the checkbox
 * is shown or changed.
 *
 * @author Michael Kolling
 */
public class TestDisplayButtonModel extends JToggleButton.ToggleButtonModel
{
    long timeStamp = 0;  // for bug fix

    public boolean isSelected()
    {
        return TestDisplayFrame.getTestDisplay().isShown();
    }

    public void setSelected(boolean b)
    {
        // this is a workaround for a MacOS/Java bug: JCheckBoxMenuItems in menus
        // fire twice when screenmenubar is true.
        // -- unused at the moment because we are not using the screen menu bar
        /*
        long now = System.currentTimeMillis();
        if (now - timeStamp > 1000) { // Filter redundant calls
            System.out.println("   executing");
            timeStamp = now;
            super.setSelected(b);
            Terminal.getTerminal().showTerminal(b);
        }
        */

        super.setSelected(b);
        TestDisplayFrame.getTestDisplay().showTestDisplay(b);
    }
}

