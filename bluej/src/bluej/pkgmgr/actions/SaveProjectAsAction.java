package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * User chooses "save project as". This allows saving the project under a
 * different name, to make a backup etc.
 * 
 * @author Davin McCall
 * @version $Id: SaveProjectAsAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class SaveProjectAsAction extends PkgMgrAction {
    
    static private SaveProjectAsAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public SaveProjectAsAction getInstance()
    {
        if(instance == null)
            instance = new SaveProjectAsAction();
        return instance;
    }
    
    private SaveProjectAsAction()
    {
        super("menu.package.saveAs");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.getProject().saveAs(pmf);
    }
}
