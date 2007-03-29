package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Export to external package. Allow the user to export the project to a
 * directory structure or Jar file.
 * 
 * @author Davin McCall
 * @version $Id: ExportProjectAction.java 4905 2007-03-29 06:06:30Z davmac $
 */
final public class ExportProjectAction extends PkgMgrAction
{
    public ExportProjectAction()
    {
        super("menu.package.export");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doExport();
    }
}
