package bluej.pkgmgr.actions;

import java.awt.event.KeyEvent;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Project Documentation" command. Generate the documentation for all classes
 * using javadoc. Attempt to display the generated documentation using a web
 * browser.
 * 
 * @author Davin McCall
 * @version $Id: GenerateDocsAction.java 2505 2004-04-21 01:50:28Z davmac $
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
        super("menu.tools.generateDoc", KeyEvent.VK_J);
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.generateProjectDocumentation();
    }
}
