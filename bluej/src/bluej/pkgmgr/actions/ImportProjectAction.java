package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Import external project (directory structure / jar file).
 * 
 * @author Davin McCall
 * @version $Id: ImportProjectAction.java 2505 2004-04-21 01:50:28Z davmac $ 
 */
final public class ImportProjectAction extends PkgMgrAction {
    
    static private ImportProjectAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public ImportProjectAction getInstance()
    {
        if(instance == null)
            instance = new ImportProjectAction();
        return instance;
    }
    
    private ImportProjectAction()
    {
        super("menu.package.import");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doImport();
    }
}
