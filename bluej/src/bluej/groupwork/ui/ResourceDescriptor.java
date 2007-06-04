package bluej.groupwork.ui;

import bluej.Config;
import bluej.groupwork.TeamStatusInfo;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;

/**
 * Class to determine team resource descriptions for use in dialogs
 * 
 * @author Bruce Quig
 * @version $Id: ResourceDescriptor.java 5082 2007-06-04 04:29:25Z bquig $
 */
public class ResourceDescriptor
{
           
    public static String getResource(Project project, Object value, boolean annotate)
    {
        String status = value.toString();
        if(value instanceof TeamStatusInfo) {
            TeamStatusInfo info = (TeamStatusInfo)value;
            boolean isPkgFile = info.getFile().getName().equals(Package.pkgfileName);

            if (isPkgFile) {
                  status = Config.getString("team.commit.layout") + " " + project.getPackageForFile(info.getFile());
            }
            if(annotate) {
                // file has been deleted
                if(info.getStatus() == TeamStatusInfo.STATUS_DELETED) {
                    status += " (" + Config.getString("team.status.delete") + ")";
                }
                else if (info.getStatus() == TeamStatusInfo.STATUS_NEEDSADD) {
                    status += " (" + Config.getString("team.status.add") + ")";
                }
                else if (info.getStatus() == TeamStatusInfo.STATUS_NEEDSCHECKOUT) {
                    status += " (" + Config.getString("team.status.new") + ")";
                }
                else if (info.getStatus() == TeamStatusInfo.STATUS_REMOVED) {
                    status += " (" + Config.getString("team.status.removed") + ")";
                }
                else if (info.getStatus() == TeamStatusInfo.STATUS_NEEDSMERGE) {
                    if (! isPkgFile) {
                        status += " (" + Config.getString("team.status.needsmerge") + ")";
                    }
                }
            }
        }
        
        return status;
    }
   
}
