package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * User chooses "save project as". This allows saving the project under a
 * different name, to make a backup etc.
 * 
 * @author Davin McCall
 * @version $Id: SaveProjectAsAction.java 4905 2007-03-29 06:06:30Z davmac $
 */
final public class SaveProjectAsAction extends PkgMgrAction
{
    public SaveProjectAsAction()
    {
        super("menu.package.saveAs");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.getProject().saveAs(pmf);
    }
}
