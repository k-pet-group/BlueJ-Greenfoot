package bluej.pkgmgr.actions;


import bluej.pkgmgr.PkgMgrFrame;

/**
 * User chooses "open existing project". Allows for opening a blueJ project
 * into a new window or (if no project is currently open) into the current
 * window.
 */

final public class OpenProjectAction extends PkgMgrAction {
    
    static private OpenProjectAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public OpenProjectAction getInstance()
    {
        if(instance == null)
            instance = new OpenProjectAction();
        return instance;
    }
    
    private OpenProjectAction()
    {
        super("menu.package.open");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doOpen();
    }
}
