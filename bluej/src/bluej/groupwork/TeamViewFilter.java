package bluej.groupwork;

import bluej.pkgmgr.BlueJPackageFile;

/**
 * Filter for filtering out certain elements from the views in the groupwork UI.
 * 
 * @author Poul Henriksen
 */
public class TeamViewFilter
{
    /**
     * Filter to identify which files will be shown in the groupwork UI.
     * <p>
     * This will filter out the old bluej package file (bluej.pkg) so that
     * Diagram Layout doesn't appear twice in the same view.
     * @return True if it should be accepted for viewing, false if not.
     */
    public boolean accept(TeamStatusInfo statusInfo)
    {
        if(BlueJPackageFile.isOldPackageFileName(statusInfo.getFile().getName())) {
            return false;
        }
        return true;
    }
}
