package bluej.pkgmgr.actions;

import javax.swing.ButtonModel;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Created by neil on 25/06/2016.
 */
public abstract class PkgMgrToggleAction extends PkgMgrAction
{
    private final ButtonModel toggleButtonModel;

    public PkgMgrToggleAction(PkgMgrFrame pmf, String s, ButtonModel toggleButtonModel)
    {
        super(pmf, s);
        this.toggleButtonModel = toggleButtonModel;
    }


    /**
     * Retrieve the "toggle model" of this action.
     */
    public final ButtonModel getToggleModel()
    {
        return toggleButtonModel;
    }

    @Override
    public final void actionPerformed(PkgMgrFrame pmf)
    {
        // Do nothing by default
    }
}
