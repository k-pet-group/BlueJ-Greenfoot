/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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
package greenfoot.actions;

import greenfoot.core.GProject;
import greenfoot.gui.GreenfootFrame;

import java.awt.event.ActionEvent;
import java.io.File;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;

import bluej.Config;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;

/**
 * An action to save a copy of a project into another location.
 * 
 * @author Davin McCall
 */
public class SaveCopyAction extends AbstractAction
{
    private GreenfootFrame gfFrame;
    
    public SaveCopyAction(GreenfootFrame gfFrame)
    {
        super(Config.getString("project.savecopy"));
        this.gfFrame = gfFrame;
        setEnabled(false);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        // get a file name to save under
        String newName = FileUtility.getFileName(gfFrame,
                Config.getString("project.savecopy.title"),
                Config.getString("pkgmgr.saveAs.buttonLabel"), true, null, true);

        if (newName != null) {
            GProject project = gfFrame.getProject();

            int result = FileUtility.COPY_ERROR;
            
            try {
                project.save();

                result = FileUtility.copyDirectory(project.getDir(),
                        new File(newName));
            }
            catch (RemoteException re) {
                re.printStackTrace();
            }
            catch (ProjectNotOpenException pnoe) {
                // can't happen
                pnoe.printStackTrace();
            }

            switch (result) {
                case FileUtility.NO_ERROR:
                    break;

                case FileUtility.DEST_EXISTS:
                    DialogManager.showError(gfFrame, "directory-exists");

                    return;

                case FileUtility.SRC_NOT_DIRECTORY:
                case FileUtility.COPY_ERROR:
                    DialogManager.showError(gfFrame, "cannot-copy-package");

                    return;
            }
        }
    }
}
