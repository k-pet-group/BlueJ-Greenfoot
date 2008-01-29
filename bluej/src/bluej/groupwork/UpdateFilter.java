package bluej.groupwork;

/**
 * Class to filter TeamStatusInfo objects to calculate those classes that will 
 * be changed when we next update. 
 *
 * @author Davin McCall
 * @version $Id: UpdateFilter.java 5501 2008-01-29 04:59:32Z davmac $
 */
public class UpdateFilter
{
    /**
     * Filter to identify which files in a repository will be altered at 
     * the next update.
     */
    public boolean accept(TeamStatusInfo statusInfo)
    {
        boolean isDir = statusInfo.getFile().isDirectory();
        int stat = statusInfo.getStatus();
        
        if (stat == TeamStatusInfo.STATUS_NEEDSCHECKOUT) {
            return true;
        }
        if (stat == TeamStatusInfo.STATUS_NEEDSMERGE) {
            return ! isDir;
        }
        if (stat == TeamStatusInfo.STATUS_NEEDSUPDATE) {
            return ! isDir;
        }
        if (stat == TeamStatusInfo.STATUS_REMOVED) {
            return true;
        }
        if (stat == TeamStatusInfo.STATUS_CONFLICT_LDRM) {
            // Locally deleted, remotely modified. Update pulls the repository version
            return true;
        }
        if (stat == TeamStatusInfo.STATUS_CONFLICT_LMRD) {
            // Update will succeed if forced (for bluej.pkg files)
            return true;
        }
    
        return false;
    }
    
    /**
     * For layout files, checks whether the file should be updated unconditionally.
     */
    public boolean updateAlways(TeamStatusInfo statusInfo)
    {
        if (statusInfo.getStatus() == TeamStatusInfo.STATUS_NEEDSCHECKOUT) {
            return true;
        }
        if (statusInfo.getStatus() == TeamStatusInfo.STATUS_REMOVED) {
            return true;
        }
        if (statusInfo.getStatus() == TeamStatusInfo.STATUS_CONFLICT_LMRD) {
            return true;
        }
        return false;
    }
}
