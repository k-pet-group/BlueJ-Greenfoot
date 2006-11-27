package bluej.groupwork;

import bluej.pkgmgr.Package;
import bluej.utility.Debug;

/**
 * Class to filter CVS StatusInformation to calculate thos classes that will 
 * be changed when we next commit. It should include files that are locally 
 * modified, remotely modified, locally deleted and remotely removed.
 *
 * @author bquig
 * @version $Id: StatusFilter.java 4704 2006-11-27 00:07:19Z bquig $
 */
public class StatusFilter
{
       TeamSettingsController controller;
        
        public StatusFilter(TeamSettingsController tsc)
        {
            //Debug.message("Creating new status filter");
            controller = tsc;
        }
        
        
        /**
         * Filter to identify which files in a repository will be altered at 
         * the next commit.
         */
        public boolean accept(TeamStatusInfo statusInfo)
        {
                                                    
            if (statusInfo.getStatus() == TeamStatusInfo.STATUS_DELETED) {
                return true;
            }
            if (statusInfo.getStatus() == TeamStatusInfo.STATUS_NEEDSADD) {
                return true;
            }
            if (statusInfo.getStatus() == TeamStatusInfo.STATUS_NEEDSCOMMIT) {
                return true;
            }
             
          
            
            return false;
        }
}
