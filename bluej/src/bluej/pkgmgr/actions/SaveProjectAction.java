package bluej.pkgmgr.actions;

import java.awt.event.KeyEvent;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * User chooses "save project". Save all files in the project.
 * 
 * @author Davin McCall
 * @version $Id: SaveProjectAction.java 2505 2004-04-21 01:50:28Z davmac $
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
        super("menu.package.save", KeyEvent.VK_S);
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.getProject().saveAll();
    }
}
