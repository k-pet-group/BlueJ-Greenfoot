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
 * NOTE on class Names: Most of the time we would like the qualified form of the class name
 * however there are cases when we need the short form, it seems reasonable to store the
 * long form and derive the short one.
 * 
 */

import bluej.pkgmgr.Project;
import bluej.pkgmgr.Package;

import java.io.File;
import bluej.pkgmgr.target.*;
import bluej.views.*;

 
class Identifier 
  {
  private File   projectId;
  private String packageId;
  private String qualifiedClassName;
  private String objectId;
  
  public Identifier(Project bleujProject)
    {
    projectId = bleujProject.getProjectDir();
    }

  public Identifier (Project bluejProject, Package bluejPackage )
    {
    projectId = bluejProject.getProjectDir();
    packageId = bluejPackage.getQualifiedName();
    }

  public Identifier (Project bluejProject, Package bluejPackage, String aQualifiedClassName )
    {
    projectId = bluejProject.getProjectDir();
    packageId = bluejPackage.getQualifiedName();
    qualifiedClassName = aQualifiedClassName;
    }

  /**
   * Returns the blueJProject by checking its existence
   */
  public Project getBluejProject ()
    {
    return Project.getProject(projectId);      
    }

  /**
   * Returns the inner bluej package given the current identifier.
   */
  public Package getBluejPackage ()
    {
    Project bluejProject = getBluejProject();
    Package bluejPkg = bluejProject.getPackage(packageId);
    return  bluejPkg;
    }

  /**
   * Returns the Java class that is associated with this name in this package
   */
  public Class getJavaClass ()
    {
    Project bluejPrj = getBluejProject();
    return bluejPrj.loadClass(qualifiedClassName);
    }

  /**
   * Returns the class target of this java class by checking its existence
   */
  ClassTarget getClassTarget ()
    {
    Package bluejPkg = getBluejPackage();

    String className = qualifiedClassName;
    int dotpos = qualifiedClassName.lastIndexOf(".");
    if ( dotpos > 0 ) className = qualifiedClassName.substring(dotpos+1);
    Target aTarget = bluejPkg.getTarget(className);
    
    if ( aTarget instanceof ClassTarget ) return (ClassTarget)aTarget;

    return null;
    }

  /**
   * Returns the view associated with this Class
   */
  View getBluejView ()
    {
    Class aClass = getJavaClass();

    return View.getView(aClass);
    }
}