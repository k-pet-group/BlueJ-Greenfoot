package bluej.pkgmgr.actions;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * "New 'Uses' relationship" command. User chooses two classes and a "uses"
 * relationship is created between them on the graph.
 * 
 * @author Davin McCall
 * @version $Id: NewUsesAction.java 4905 2007-03-29 06:06:30Z davmac $  
 */

final public class NewUsesAction extends PkgMgrAction
{
    public NewUsesAction()
    {
        super("menu.edit.newUses");
        putValue(SMALL_ICON, Config.getImageAsIcon("image.build.depends"));
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.newUses"));
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.clearStatus();
        pmf.doNewUses(); 
    }
}
