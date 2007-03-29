package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Project Documentation" command. Generate the documentation for all classes
 * using javadoc. Attempt to display the generated documentation using a web
 * browser.
 * 
 * @author Davin McCall
 * @version $Id: GenerateDocsAction.java 4905 2007-03-29 06:06:30Z davmac $
 */
final public class GenerateDocsAction extends PkgMgrAction
{
    public GenerateDocsAction()
    {
        super("menu.tools.generateDoc");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.generateProjectDocumentation();
    }
}
