package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Preferences" command. Displays a dialog box in which user can set various
 * preferences as to how BlueJ should behave.
 * 
 * @author Davin McCall
 * @version $Id: PreferencesAction.java 2505 2004-04-21 01:50:28Z davmac $
 */

final public class PreferencesAction extends PkgMgrAction {
    
    static private PreferencesAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public PreferencesAction getInstance()
    {
        if(instance == null)
            instance = new PreferencesAction();
        return instance;
    }
    
    private PreferencesAction()
    {
        super("menu.tools.preferences");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.showPreferences();
    }
}
