package bluej.extensions;

import bluej.pkgmgr.Project;
import bluej.pkgmgr.Package;

import java.io.File;
import java.util.List;
import java.util.ListIterator;

/**
 * A wrapper for a Project open by BlueJ.
 *
 * @version $Id: BProject.java 1838 2003-04-11 13:16:46Z damiano $
 */

/*
 * Author Clive Mille, Univeristy of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 */

public class BProject
{
    private final Object projectKey;
  
    /**
     * Constructor for a Bproject.
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
     * The underlyng mechanism is the same as the previous one. This is for type checking
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
     * This object may not be valid since what it represent has been modified or deleted
     * from the main BlueJ graphical user interface.
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
    public File getDir()
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return null;

        return thisProject.getProjectDir();
    }
    
    /**
     * Requests to save all open files of this project. 
     */
    public void save()
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return;

        thisProject.saveAll();
    }
    
    /**
     * Saves any open files, then closes all frames relating to this project.
     */
    public void close()
    {
        Project thisProject = Project.getProject(projectKey);
        if ( thisProject == null ) return;

        thisProject.saveAll();
        Project.closeProject (thisProject);
    }
    
    
    /**
     * Get a package belonging to this project.
     * It can return null if this project is invalid.
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