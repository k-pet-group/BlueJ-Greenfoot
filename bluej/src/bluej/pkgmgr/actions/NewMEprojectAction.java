package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Java ME Project action. User chooses "create new ME project". This prompts for a
 * project name, creates the directory, and displays the new project in a new window.
 * 
 * @author Cecilia Vargas (based on Davin McCall's NewProjectAction)
 * @version $Id: NewProjectAction.java 2505 2004-04-21 01:50:28Z davmac $
 */

final public class NewMEprojectAction extends PkgMgrAction {
    
    static private NewMEprojectAction instance = null;
    
    private NewMEprojectAction()  { super("menu.mepackage.new"); }
    
    /**
     * Factory method to retrieve an instance of the class as constructor is private.
     * @return an instance of the class.
     */
    static public NewMEprojectAction getInstance()
    {
        if(instance == null)
            instance = new NewMEprojectAction();
        return instance;
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doNewProject( true );  //pass true because we are creating an ME project
    }                        
}