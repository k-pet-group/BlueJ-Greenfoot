package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;


/**
 * Project action. User chooses "create new project". This prompts for a
 * choice of project name, creates the directory, and displays the new
 * project in a new window.
 * 
 * @author Davin McCall
 * @version $Id: NewProjectAction.java 2505 2004-04-21 01:50:28Z davmac $
 */

final public class NewProjectAction extends PkgMgrAction {
    
    static private NewProjectAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public NewProjectAction getInstance()
    {
        if(instance == null)
            instance = new NewProjectAction();
        return instance;
    }
    
    private NewProjectAction()
    {
        super("menu.package.new");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doNewProject();
    }                        
}
