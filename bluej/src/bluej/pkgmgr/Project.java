package bluej.pkgmgr;

import bluej.utility.Debug;
import bluej.utility.DialogManager;

import java.util.*;
import java.io.File;
import java.io.IOException;


/**
 * A BlueJ Project.
 *
 * @version 
 *
 * @author Michael Kolling
 * @author Axel Schmolitzky
 */

public class Project {

    // static fields

    // collection of all open projects. the canonical name of the project
    // directory is used as the key.
    private static Map projects = new Hashtable();

    /**
     * Start everything off. This is used to open the projects
     * specified on the command line when starting BlueJ.
     */
    public static void openProjects(String[] args)
    {
        if(args.length == 0) {
            // No arguments, so start an empty package manager window
            openProject("");
        }
        else {
            for(int i = 0; i < args.length; i++)
                openProject(args[i]);
        }
    }

    /**
     * Open a new BlueJ project.
     */
    public static Project openProject(String projectPath)
    {
        File projectDir = new File(projectPath);
        Debug.message("project dir: '" + projectDir + "'");

        // check whether it exists
        String canonicalPath = projectDir.getPath();
        Debug.message("canonical path: '" + projectDir + "'");
        Project proj = (Project)projects.get(canonicalPath);
        if(proj == null) {
            proj = new Project(projectDir);
            projects.put(canonicalPath, proj);
        }
        return proj;
    }

    /**
     * Remove a project from the collection of currently open projects.
     */
    public static void closeProject(Project project)
    {
        projects.remove(project.getProjectDir().getPath());
    }


    public static void createNewProject(String projectName)
    {
        if (projectName != null) {

	    // check whether name is already in use
	    File dir = new File(projectName);
	    if(dir.exists()) {
		DialogManager.showError(null, "directory-exists");
		return;
	    }

	    Package newPkg;
            // Note: newPkg is not kept. It is a temporary object that gets
            // discarded. The information is read into "pkg" (through "load")


            Debug.reportError("Create new project - broken");
            /*

              // Open here if current window is empty
              if (pkg.getDirName() == noTitle || pkg.getDirName() == null) {
              newPkg = new Package(pkg.getProject(), newname, this);
              newPkg.save();
              doOpenPackage(newname);
              }
              else {
              // Otherwise open it in a new window
              newPkg = new Package(pkg.getProject(), newname, null);
              newPkg.save();
              PkgFrame frame = PkgMgrFrame.createFrame(null);
              frame.doOpenPackage(newname);
              frame.setVisible(true);
              }
              }
              */
        }
    }

    // instance fields

    private File projectDir;            // the path of the project directory
    private Collection packages;        // collection of open packages in 
    //  this project

    /* ------------------- end of field declarations ------------------- */

    /**
     * Create a new project. "projectDir" may be null to create a new project
     * with an empty package frame.
     */
    private Project(File projectDir)
    {
        this.projectDir = projectDir;
        packages = new ArrayList();

        // automatically open unnamed package

        PkgMgrFrame frame = PkgMgrFrame.createFrame(this, projectDir);
        frame.setVisible(true);
        packages.add(frame.getPackage());
    }

    /**
     *
     */
    public String getProjectName()
    {
        return projectDir.getName();
    }

    /**
     *
     */
    public File getProjectDir()
    {
        return projectDir;
    }



    /*
      private static Package getPackage(String pkgname)
      {
      return (Package)packages.get(pkgname);
      }

      public static Package openPackage(String pkgname)
      {
      return openPackage(null, pkgname);
      }

      public static Package openPackage(String baseDir, String pkgname)
      {
      // Check whether it's already open
      Package pkg = getPackage(pkgname);

      if(pkg == null) { // if not, then search the library path to open it
      String pkgdir = pkgname.replace('.', File.separatorChar);

      if(baseDir != null) {
      String fulldir = baseDir + File.separator + pkgdir;
      String pkgfile = fulldir + File.separator + Package.pkgfileName;

      if(new File(pkgfile).exists())
      return PkgMgrFrame.createFrame(fulldir).getPackage();
      }
      }

      return pkg;
      }
      */

}
