package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * User chooses "save project". Save all files in the project.
 * 
 * @author Davin McCall
 * @version $Id: SaveProjectAction.java 4905 2007-03-29 06:06:30Z davmac $
 */
final public class SaveProjectAction extends PkgMgrAction
{
    public SaveProjectAction()
    {
        super("menu.package.save");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.getProject().saveAll();
    }
}
