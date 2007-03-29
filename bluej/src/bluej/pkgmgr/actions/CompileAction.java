package bluej.pkgmgr.actions;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Compile" command. Compiles all class files in the project which need to
 * be compiled.
 * 
 * @author Davin McCall
 * @version $Id: CompileAction.java 4905 2007-03-29 06:06:30Z davmac $
 */
final public class CompileAction extends PkgMgrAction
{
    public CompileAction()
    {
        super("menu.tools.compile");
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.compile"));
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.getPackage().compile();
    }
}
