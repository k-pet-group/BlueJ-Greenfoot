package bluej.groupwork;

import bluej.pkgmgr.Package;

/**
 * Class to filter CVS StatusInformation to calculate those classes that will 
 * be changed when we next commit. It should include files that are locally 
 * modified, remotely modified, locally deleted and remotely removed.
 *
 * @author bquig
 * @version $Id: CommitFilter.java 5648 2008-03-20 02:27:49Z davmac $
 */
public class CommitFilter
{
    /**
     * Filter to identify which files in a repository will be altered at 
     * the next commit.
     */
    public boolean accept(TeamStatusInfo statusInfo)
    {
        int stat = statusInfo.getStatus();
        
        if (stat == TeamStatusInfo.STATUS_DELETED) {
            return true;
        }
        if (stat == TeamStatusInfo.STATUS_NEEDSADD) {
            return true;
        }
        if (stat == TeamStatusInfo.STATUS_NEEDSCOMMIT) {
            return true;
        }
        
        if (statusInfo.getFile().getName().equals(Package.pkgfileName)) {
            boolean conflict = (stat == TeamStatusInfo.STATUS_CONFLICT_ADD);
            conflict |= (stat == TeamStatusInfo.STATUS_NEEDSMERGE);
            conflict |= (stat == TeamStatusInfo.STATUS_CONFLICT_LDRM);
            conflict |= (stat == TeamStatusInfo.STATUS_UNRESOLVED);
            if (conflict) {
                return true;
            }
        }

        return false;
    }
}
