/*
 This file is part of the BlueJ program.
 Copyright (C) 2019  Michael Kolling and John Rosenberg

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

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class InteractiveTutorialAction extends PkgMgrAction
{
    public InteractiveTutorialAction(PkgMgrFrame pmf)
    {
        super(pmf, "menu.help.tutorial");
    }

    @Override
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        try
        {
            // We need to make a new temporary dir, copy the tutorial contents there
            // and open it as a project:
            File dir = Files.createTempDirectory("tutorial").toFile();
            dir.deleteOnExit();

            // Since dir will be something like "tutorial287498237498237", we make an inner dir with simpler name:
            dir = new File(dir, "tutorial");

            int result = FileUtility.copyDirectory(new File(Config.getBlueJLibDir(), "tutorial"), dir);

            switch (result)
            {
                case FileUtility.NO_ERROR:
                    // It worked, use this as the new project:
                    PkgMgrFrame.doOpen(dir, pmf);
                    break;

                case FileUtility.DEST_EXISTS_NOT_DIR:
                    DialogManager.showErrorFX(null, "directory-exists-file");
                    break;

                case FileUtility.DEST_EXISTS_NON_EMPTY:
                    DialogManager.showErrorFX(null, "directory-exists-non-empty");
                    break;

                case FileUtility.SRC_NOT_DIRECTORY:
                case FileUtility.COPY_ERROR:
                    DialogManager.showErrorFX(null, "cannot-save-project");
                    break;
            }

        }
        catch (IOException e)
        {
            DialogManager.showErrorTextFX(pmf.getWindow(), e.getLocalizedMessage());
        }
    }
}
