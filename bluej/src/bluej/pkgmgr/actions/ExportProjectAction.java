package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Export to external package. Allow the user to export the project to a
 * directory structure or Jar file.
 * 
 * @author Davin McCall
 * @version $Id: ExportProjectAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class ExportProjectAction extends PkgMgrAction {
    
    static private ExportProjectAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public ExportProjectAction getInstance()
    {
        if(instance == null)
            instance = new ExportProjectAction();
        return instance;
    }
        
    private ExportProjectAction()
    {
        super("menu.package.export");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doExport();
    }
}
