package bluej.groupwork.ui;

import bluej.Config;
import bluej.groupwork.TeamStatusInfo;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

/**
 * Class to display files to be committed in a list for the CommitCommentsFrame
 * or UpdateFilesFrame
 * 
 * @author Bruce Quig
 * @author Davin McCall
 * @version $Id: FileRenderer.java 5074 2007-05-31 04:59:43Z davmac $
 */
public class FileRenderer extends DefaultListCellRenderer
{
    private Project project;
    
    public FileRenderer(Project proj)
    {
        project = proj;
    }
        
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
        String status = value.toString();
        if(value instanceof TeamStatusInfo) {
            TeamStatusInfo info = (TeamStatusInfo)value;
            boolean isPkgFile = info.getFile().getName().equals(Package.pkgfileName);
            
            if (isPkgFile) {
                status = Config.getString("team.commit.layout") + " " + project.getPackageForFile(info.getFile());
            }
            
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
       
        JLabel label = new JLabel(status);
        return label;
    }
   
}
