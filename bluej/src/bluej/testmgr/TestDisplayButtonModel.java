package bluej.testmgr;

import javax.swing.JToggleButton.ToggleButtonModel;

/**
 * ButtonModel for the "Show Test Runner" checkBoxItem in the menu.
 * This model takes care that the right things happen when the checkbox
 * is shown or changed.
 *
 * @author  Michael Kolling
 * @version $Id: TestDisplayButtonModel.java 2746 2004-07-06 21:32:45Z mik $
 */
public class TestDisplayButtonModel extends ToggleButtonModel
{
    long timeStamp = 0;  // for bug fix

    public boolean isSelected()
    {
        return TestDisplayFrame.isFrameShown();
    }

    public void setSelected(boolean b)
    {
        super.setSelected(b);
        TestDisplayFrame.getTestDisplay().showTestDisplay(b);
    }
}

