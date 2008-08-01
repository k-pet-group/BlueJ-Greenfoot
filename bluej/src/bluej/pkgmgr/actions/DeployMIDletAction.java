package bluej.pkgmgr.actions;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * Deploys the MIDlet suite contained in the Java ME project.
 * 
 * @author Cecilia Vargas
 */
final public class DeployMIDletAction extends PkgMgrAction
{
    public DeployMIDletAction()
    {
        super( "menu.package.deploy.MIDlet" );
        putValue( SHORT_DESCRIPTION, Config.getString( "tooltip.deployMIDlet" ) );
    }
    
    public void actionPerformed( PkgMgrFrame pmf )
    {
        pmf.menuCall();
        pmf.doDeployMIDlet();
    }
}
