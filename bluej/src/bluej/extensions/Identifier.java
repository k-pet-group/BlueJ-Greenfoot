package bluej.extensions;

/**
 * This class is used internally by the extensions and should not be visible to the
 * extension writer.
 * 
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 */

/* The problem I am tring to solve is to have a uniform and simple way to deal with
 * objects validity. An extension may be holding BProject  but this BProject may not be valid
 * since the gui has closed it. Or may be holding aBPackage and this not being valid
 * The same apply to a BClass or a BOBject.
 * 
 * To solve it I need to store the ID of the above objects and check if it is still valid
 * before doing anything.
 * Again, the problem is that for a BClass I need not only to check if the Class is valid
 * but also the Project and the Package !
 * So the ID if a BClass is really all of the above...
 * 
 * Then, the solution is to put all that is needed in here and have this class only deal with 
 * checking the mess of it...
 * 
 */

import bluej.pkgmgr.Project;
import bluej.pkgmgr.Package;

import java.io.File;

 
class Identifier 
  {
  private final File   projectId;
  private final String packageId;
  private final String classId;
  private final String objectId;
  
  public Identifier(Project bleujProject)
    {
    projectId = bleujProject.getProjectDir();
    packageId = null;
    classId   = null;
    objectId  = null;
    }

  public Identifier (Project bluejProject, Package bluejPackage )
    {
    projectId = bluejProject.getProjectDir();
    packageId = bluejPackage.getQualifiedName();
    classId   = null;
    objectId  = null;
    }

  public File getProjectId()
    {
    return projectId;
    }
    
  public Project getBluejProject ()
    {
    return Project.getProject(projectId);      
    }

  /**
   * Returns the inner bluej package given the current identifier.
   */
  public Package getBluejPackage ()
    {
    Project thisProject = getBluejProject();
    Package thisPackage = thisProject.getPackage(packageId);
    return  thisPackage;
    }
}