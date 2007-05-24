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
 * Class to display files to be committed in a list for the CommitCommentsDialog
 * 
 * @author Bruce Quig
 * @author Davin McCall
 * @version $Id: CommitFileRenderer.java 5051 2007-05-24 03:35:15Z davmac $
 */
public class CommitFileRenderer extends DefaultListCellRenderer
{
    private Project project;
    
    public CommitFileRenderer(Project proj)
    {
        project = proj;
    }
        
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
        String status = value.toString();
        if(value instanceof TeamStatusInfo) {
            TeamStatusInfo info = (TeamStatusInfo)value;
            // file has been deleted
            if(info.getStatus() == TeamStatusInfo.STATUS_DELETED) {
                status += " (" + Config.getString("team.status.delete") + ")";
            }
            // bluej.pkg file description
            else if(info.getFile().getName().equals(Package.pkgfileName)) {
                status = Config.getString("team.commit.layout") + " " + project.getPackageForFile(info.getFile());
           }
        }
       
        JLabel label = new JLabel(status);
        return label;
    }
   
}
