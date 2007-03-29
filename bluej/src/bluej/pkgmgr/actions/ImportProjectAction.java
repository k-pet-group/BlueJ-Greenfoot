package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Import external project (directory structure / jar file).
 * 
 * @author Davin McCall
 * @version $Id: ImportProjectAction.java 4905 2007-03-29 06:06:30Z davmac $ 
 */
final public class ImportProjectAction extends PkgMgrAction
{
    public ImportProjectAction()
    {
        super("menu.package.import");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doImport();
    }
}
