package bluej.extensions;

import bluej.pkgmgr.Project;
import bluej.pkgmgr.Package;

import java.io.File;
import java.util.List;
import java.util.ListIterator;

/**
 * A wrapper for a Project open by BlueJ.
 *
 * @version $Id: BProject.java 1726 2003-03-24 13:33:06Z damiano $
 */

public class BProject
{
    private final Object projectKey;
  
    /**
     * NOT to be used by Extension writer.
     * Get BProject from BPackage.
     */
    BProject (File projectDir)
    {
      /* NOTE: As a reference I store ONLY the key of the project and EVERY time I want to 
       * get information I will retrieve the LIVE Project.
       * I do this to try to have a reasonably syncronized view between the BleuJ and this
       * Othervise I will be holding a Project object that is NO longer active !
       */
      projectKey = projectDir;
    }

    /**
     * Not for public use.
     * NOTA: The underlyng mechanism is the same as the previous one. This is for type checking
     */
    BProject (Project bluejProject)
    {
        if ( bluejProject == null ) 
            projectKey = null;
        else
            projectKey = bluejProject.getProjectDir();
    }


    /**
     * Test if this project still a valid one.
     * @return true if this project is valid and active, false othervise.
     */
    public boolean isValid ()
    {
        Project thisProject = Project.getProject(projectKey);
        return thisProject != null;      
    }
        
    /**
     * Return the name of this project. 
     * This is what is displayed in the title bar of the frame after 'BlueJ'.
     * It can return null if this project has been invalidated.
     */
    public String getName()
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return null;
        
        return thisProject.getProjectName();
    }
    
    /**
     * Return the current directory of this project. 
     * Can return null if project is invalid.
     */
    public File getProjectDir()
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return null;

        return thisProject.getProjectDir();
    }
    
    /**
     * Requests BlueJ to save all open files of this project. 
     */
    public void saveProject()
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return;

        try {
            thisProject.saveAll();
        } catch (NullPointerException ex) {
        }
    }
    
    /**
     * Saves any open files, then closes all frames relating to this project.
     */
    public void closeProject()
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return;

        thisProject.saveAll();
        Project.closeProject (thisProject);
    }
    
    
    /**
     * Get a package belonging to this Project.
     * 
     * @param the fully-qualified name of the package
     * @return the requested package, or null if it wasn't found
     */
    public BPackage getPackage (String name)
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return null;

        Package pkg = thisProject.getPackage (name);
        if ( pkg == null ) return null;
        
        return new BPackage (pkg);
    }
    
    /**
     * Return all packages in this project.
     * If for some reason none exists, or the project is invalid, an empty array is returned.
     */
    public BPackage[] getPackages()
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return new BPackage[0];

        List names = thisProject.getPackageNames();
        BPackage[] packages = new BPackage [names.size()];
        for (ListIterator li=names.listIterator(); li.hasNext();) {
            int i=li.nextIndex();
            String name = (String)li.next();
            packages [i] = getPackage (name);
        }
        return packages;
    }
}