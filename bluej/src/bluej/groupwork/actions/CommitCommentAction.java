package bluej.groupwork.actions;

import bluej.Config;
import bluej.groupwork.ui.CommitCommentsFrame;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;



/**
 * Action to show the frame which allows commit comments to be entered.
 * The frame has a button to make the commit.
 * 
 * @author Kasper
 * @author Bruce Quig
 * @version $Id$
 */
public class CommitCommentAction extends TeamAction
{
    public CommitCommentAction()
    {
        super("team.commit", true);
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.commit"));
    }
    
   /* (non-Javadoc)
    * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
    public void actionPerformed(PkgMgrFrame pmf)
    {
        doCommitComment(pmf);
    }
    
    private void doCommitComment(PkgMgrFrame pmf)
    {
        if(!pmf.isEmptyFrame()) {
            Project project = pmf.getProject();
            // we want to save bluej.pkg files first
            project.saveAllGraphLayout();
            CommitCommentsFrame dialog = project.getCommitCommentsDialog();
            
            dialog.reset();
            dialog.setVisible(true);
        }
    }
}