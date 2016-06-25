package bluej.pkgmgr.actions;

import javax.swing.ButtonModel;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Created by neil on 25/06/2016.
 */
public abstract class PkgMgrToggleAction extends PkgMgrAction
{
    public PkgMgrToggleAction(PkgMgrFrame pmf, String s)
    {
        super(pmf, s);
    }

    public PkgMgrToggleAction(PkgMgrFrame pmf, String s, int keycode)
    {
        super(pmf, s, keycode);
    }

    public PkgMgrToggleAction(PkgMgrFrame pmf, String s, int keycode, int modifiers)
    {
        super(pmf, s, keycode, modifiers);
    }

    /**
     * Retrieve the "toggle model" if any of an action. An action only has a toggle
     * model if it has an assosciated boolean state which should be displayed as a check
     * box.
     *
     * By default there is no toggle model.
     *
     * @return the toggle model for this action (or null).
     */
    public abstract ButtonModel getToggleModel();

    @Override
    public void actionPerformed(PkgMgrFrame pmf)
    {
        // Do nothing by default
    }
}
