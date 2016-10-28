/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016  Michael Kolling and John Rosenberg 
 
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
 */
public class FileRenderer extends DefaultListCellRenderer
{
    private Project project;
    private boolean remote;
    
    public FileRenderer(Project proj)
    {
        project = proj;
    }
    
    /**
     * Creates a fileRenderer for remote or local status.
     * @param proj project
     * @param remote the status we are taking into account.
     */
    public FileRenderer(Project proj, boolean remote)
    {
        project = proj;
        this.remote = remote;
    }
        
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
        String status;
        if (project.getTeamSettingsController().isDVCS()){
            status = ResourceDescriptor.getDCVSResource(project, value, true, remote);
        } else {
            status = ResourceDescriptor.getResource(project, value, true);
        }   
        JLabel label = new JLabel(status);
        return label;
    }
   
}
