package bluej.debugmgr.texteval;

import javax.swing.JToggleButton;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * ButtonModel for the "Show Text Evaluation" checkBoxItem in the menu.
 * This model takes care that the right things happen when the checkbox
 * is shown or changed.
 *
 * @author Michael Kolling
 */
public class TextEvalButtonModel extends JToggleButton.ToggleButtonModel
{
    private PkgMgrFrame pmf;
    
    public TextEvalButtonModel(PkgMgrFrame pmf)
    {
        super();
        this.pmf = pmf;
    }

    public boolean isSelected()
    {

        return pmf.isTextEvalVisible();
    }

    public void setSelected(boolean b)
    {
        pmf.showHideTextEval(b);
    }
}
