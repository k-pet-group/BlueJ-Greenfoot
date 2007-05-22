package bluej.groupwork.actions;

import java.util.List;

import bluej.Config;
import bluej.groupwork.ui.UpdateFilesFrame;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;


/**
 * Action to show dialog for updating out-of-date files.
 * 
 * @author davmac
 * @version $Id: UpdateDialogAction.java 5048 2007-05-22 06:03:32Z davmac $
 */
public class UpdateDialogAction extends TeamAction
{
    private Project project;
    private boolean includeLayout;
    
    /** A list of packages whose bluej.pkg file has been removed */
    private List removedPackages;
    
    public UpdateDialogAction()
    {
        super("team.update", true);
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.update"));
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(PkgMgrFrame pmf)
    {
        project = pmf.getProject();
        if (project != null) {
            project.saveAllEditors();
            new UpdateFilesFrame(project).setVisible(true);
            // includeLayout = project.getTeamSettingsController().includeLayout();
        }
    }
}
