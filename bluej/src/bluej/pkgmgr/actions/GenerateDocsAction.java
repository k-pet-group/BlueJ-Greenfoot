package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Project Documentation" command. Generate the documentation for all classes
 * using javadoc. Attempt to display the generated documentation using a web
 * browser.
 * 
 * @author Davin McCall
 * @version $Id: GenerateDocsAction.java 2571 2004-06-03 13:35:37Z fisker $
 */
final public class GenerateDocsAction extends PkgMgrAction {
    
    static private GenerateDocsAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public GenerateDocsAction getInstance()
    {
        if(instance == null)
            instance = new GenerateDocsAction();
        return instance;
    }
    
    private GenerateDocsAction()
    {
        super("menu.tools.generateDoc");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.generateProjectDocumentation();
    }
}
