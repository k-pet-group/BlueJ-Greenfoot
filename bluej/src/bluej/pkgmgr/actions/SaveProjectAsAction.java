/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2014  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr.actions;

import java.io.File;

import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;

/**
 * User chooses "save project as". This allows saving the project under a
 * different name, to make a backup etc.
 * 
 * @author Davin McCall
 */
final public class SaveProjectAsAction extends PkgMgrAction
{
    public SaveProjectAsAction()
    {
        super("menu.package.saveAs");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        saveAs(pmf, pmf.getProject());
    }
    
    public void saveAs(PkgMgrFrame frame, Project project)
    {
        // get a file name to save under
        File newName = FileUtility.getDirName(frame,
                Config.getString("pkgmgr.saveAs.title"),
                Config.getString("pkgmgr.saveAs.buttonLabel"), false, true);

        if (newName != null) {
            project.saveAll();

            int result = FileUtility.copyDirectory(project.getProjectDir(),
                    newName);

            switch (result) {
            case FileUtility.NO_ERROR:
                break;

            case FileUtility.DEST_EXISTS_NOT_DIR:
                DialogManager.showError(frame, "directory-exists-file");
                return;
            case FileUtility.DEST_EXISTS_NON_EMPTY:
                DialogManager.showError(frame, "directory-exists-non-empty");
                return;

            case FileUtility.SRC_NOT_DIRECTORY:
            case FileUtility.COPY_ERROR:
                DialogManager.showError(frame, "cannot-save-project");
                return;
            }

            PkgMgrFrame.closeProject(project);

            // open new project
            Project openProj = Project.openProject(newName.getAbsolutePath(), null);

            if (openProj != null) {
                Package pkg = openProj.getPackage("");
                PkgMgrFrame pmf = PkgMgrFrame.createFrame(pkg);
                pmf.setVisible(true);
            } else {
                Debug.message("Save as: could not open package under new name");
            }
        }
    }

}
