package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * User chooses "save project". Save all files in the project.
 * 
 * @author Davin McCall
 * @version $Id: SaveProjectAction.java 2571 2004-06-03 13:35:37Z fisker $
 */
final public class SaveProjectAction extends PkgMgrAction {
    
    static private SaveProjectAction instance = null;
    
    static public SaveProjectAction getInstance()
    {
        if(instance == null)
            instance = new SaveProjectAction();
        return instance;
    }
    
    private SaveProjectAction()
    {
        super("menu.package.save");
        
        
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.getProject().saveAll();
    }
}
