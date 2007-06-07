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
 * @version $Id: SaveProjectAsAction.java 5089 2007-06-07 02:19:17Z davmac $
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
        String newName = FileUtility.getFileName(frame,
                Config.getString("pkgmgr.saveAs.title"),
                Config.getString("pkgmgr.saveAs.buttonLabel"), true, null, true);

        if (newName != null) {
            project.saveAll();

            int result = FileUtility.copyDirectory(project.getProjectDir(),
                    new File(newName));

            switch (result) {
            case FileUtility.NO_ERROR:
                break;

            case FileUtility.DEST_EXISTS:
                DialogManager.showError(frame, "directory-exists");

                return;

            case FileUtility.SRC_NOT_DIRECTORY:
            case FileUtility.COPY_ERROR:
                DialogManager.showError(frame, "cannot-copy-package");

                return;
            }

            PkgMgrFrame.closeProject(project);

            // open new project
            Project openProj = Project.openProject(newName);

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
