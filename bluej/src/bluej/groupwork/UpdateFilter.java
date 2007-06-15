package bluej.groupwork;

/**
 * Class to filter CVS StatusInformation to calculate thos classes that will 
 * be changed when we next update. 
 *
 * @author Davin McCall
 * @version $Id: UpdateFilter.java 5096 2007-06-15 05:04:26Z davmac $
 */
public class UpdateFilter
{
    /**
     * Filter to identify which files in a repository will be altered at 
     * the next update.
     */
    public boolean accept(TeamStatusInfo statusInfo)
    {
        if (statusInfo.getStatus() == TeamStatusInfo.STATUS_NEEDSCHECKOUT) {
            return true;
        }
        if (statusInfo.getStatus() == TeamStatusInfo.STATUS_NEEDSMERGE) {
            return true;
        }
        if (statusInfo.getStatus() == TeamStatusInfo.STATUS_NEEDSUPDATE) {
            return true;
        }
        if (statusInfo.getStatus() == TeamStatusInfo.STATUS_REMOVED) {
            return true;
        }
        if (statusInfo.getStatus() == TeamStatusInfo.STATUS_CONFLICT_LDRM) {
            // Locally deleted, remotely modified. Update pulls the repository version
            return true;
        }
    
        return false;
    }
}
