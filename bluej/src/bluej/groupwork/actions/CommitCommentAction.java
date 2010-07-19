/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
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
            CommitCommentsFrame dialog = project.getCommitCommentsDialog();
            
            dialog.reset();
            dialog.setVisible(true);
        }
    }
}