package bluej.pkgmgr.actions;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * "New 'Uses' relationship" command. User chooses two classes and a "uses"
 * relationship is created between them on the graph.
 * 
 * @author Davin McCall
 * @version $Id: NewUsesAction.java 2505 2004-04-21 01:50:28Z davmac $  
 */

final public class NewUsesAction extends PkgMgrAction {
    
    static private NewUsesAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public NewUsesAction getInstance()
    {
        if(instance == null)
            instance = new NewUsesAction();
        return instance;
    }
    
    private NewUsesAction()
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
