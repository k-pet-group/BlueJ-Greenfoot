package bluej.pkgmgr.actions;

import javax.swing.ButtonModel;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * User wants to see the text evaluation component. Show it.
 */

final public class ShowTextEvalAction extends PkgMgrAction
{
    public ShowTextEvalAction()
    {
        super("menu.view.showTextEval");
    }
    
    public ButtonModel getToggleModel(PkgMgrFrame pmf)
    {
        return new bluej.debugmgr.texteval.TextEvalButtonModel(pmf);
    }
}
