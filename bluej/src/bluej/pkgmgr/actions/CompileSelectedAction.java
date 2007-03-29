package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Compile selected" command. Compiles the selected classes.
 * 
 * @author Davin McCall
 * @version $Id: CompileSelectedAction.java 4905 2007-03-29 06:06:30Z davmac $
 */
final public class CompileSelectedAction extends PkgMgrAction
{
    public CompileSelectedAction()
    {
        super("menu.tools.compileSelected");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.compileSelected();
    }
}
