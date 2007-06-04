package bluej.groupwork.ui;

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
 * @version $Id: FileRenderer.java 5083 2007-06-04 04:30:31Z bquig $
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
        String status = ResourceDescriptor.getResource(project, value, true);       
        JLabel label = new JLabel(status);
        return label;
    }
   
}
