package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Compile selected" command. Compiles the selected classes.
 * 
 * @author Davin McCall
 * @version $Id: CompileSelectedAction.java 2571 2004-06-03 13:35:37Z fisker $
 */
final public class CompileSelectedAction extends PkgMgrAction {
    
    static private CompileSelectedAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public CompileSelectedAction getInstance()
    {
        if(instance == null)
            instance = new CompileSelectedAction();
        return instance;
    }
    
    private CompileSelectedAction()
    {
        super("menu.tools.compileSelected");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.compileSelected();
    }
}
