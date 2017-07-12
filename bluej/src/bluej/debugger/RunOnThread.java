package bluej.debugger;

import bluej.Config;
import bluej.pkgmgr.Project;

/**
 * An enum indicating which thread methods should be invoked on.
 */
public enum RunOnThread
{
    DEFAULT, FX, SWING;

    // Easiest to do this in toString, even if it looks weird, because JavaFX ComboBox uses
    // toString to display, and it's awkward to make it use an outside method:
    @Override
    public String toString()
    {
        switch (this)
        {
            case FX:
                return Config.getString("prefmgr.misc.run.fx");
            case SWING:
                return Config.getString("prefmgr.misc.run.swing");
            default:
                return Config.getString("prefmgr.misc.run.default");
        }
    }

    // Like valueOf but returns DEFAULT if item not found
    public static RunOnThread load(String name)
    {
        try
        {
            return valueOf(name);
        } catch (Exception e)
        {
            return DEFAULT;
        }
    }
}
