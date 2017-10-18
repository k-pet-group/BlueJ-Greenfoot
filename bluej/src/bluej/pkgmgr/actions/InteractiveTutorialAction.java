package bluej.pkgmgr.actions;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import org.apache.commons.vfs2.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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
            DialogManager.showErrorTextFX(pmf.getFXWindow(), e.getLocalizedMessage());
        }
    }
}
