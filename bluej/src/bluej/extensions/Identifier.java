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
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;

import java.io.File;
import bluej.pkgmgr.target.*;
import bluej.views.*;
import bluej.utility.*;

 
class Identifier 
  {
  private File   projectId;
  private String packageId;
  private String qualifiedClassName;
  
  Identifier(Project bleujProject)
    {
    projectId = bleujProject.getProjectDir();
    }

  Identifier (Project bluejProject, Package bluejPackage )
    {
    projectId = bluejProject.getProjectDir();
    packageId = bluejPackage.getQualifiedName();
    }

  Identifier (Project bluejProject, Package bluejPackage, String aQualifiedClassName )
    {
    projectId = bluejProject.getProjectDir();
    packageId = bluejPackage.getQualifiedName();
    qualifiedClassName = aQualifiedClassName;
    }

  /**
   * Returns the blueJProject and also checks its existence
   */
  Project getBluejProject () throws ProjectNotOpenException
    {
    Project aProject = Project.getProject(projectId);

    if ( aProject == null ) throw new ProjectNotOpenException ("Project "+projectId+" is closed");

    return aProject;
    }

  /**
   * Returns the inner bluej package given the current identifier.
   */
  Package getBluejPackage () throws ProjectNotOpenException, PackageNotFoundException
    {
    Project bluejProject = getBluejProject();

    Package bluejPkg = bluejProject.getPackage(packageId);  // ok 311003 Damiano
    if ( bluejPkg == null ) throw new PackageNotFoundException ("Package '"+packageId+"' is deleted");
    
    return  bluejPkg;
    }


  /**
   * Returns the current class target given the current identifier.
   */
  Target getCurrentBluejTarget () throws ProjectNotOpenException, PackageNotFoundException
    {
    Package bluejPkg = getBluejPackage();

    Target aTarget = bluejPkg.getSelectedTarget();
    if ( aTarget == null ) return null;

    return aTarget;
    }




  /**
   * Returns the Frame associated with this Package.
   * The nice thing about this one is that it WILL open a frame if it was not already open.
   * This gets rid of one possible exception regarding a packageFrame not open...
   */
  PkgMgrFrame getPackageFrame ()
    throws ProjectNotOpenException, PackageNotFoundException
    {
    Package thisPkg = getBluejPackage ();

    PkgMgrFrame pmf = PkgMgrFrame.findFrame(thisPkg);
    // If we already have a frame for this package, return it
    if ( pmf != null ) return pmf;

    PkgMgrFrame recentFrame = PkgMgrFrame.getMostRecent();
    if (recentFrame != null && recentFrame.isEmptyFrame() )
      {
      // If, by chance, the current fram is an empty one, use it !
      recentFrame.openPackage(thisPkg);
      return recentFrame;
      }

    // No empty fram I can use, I need to create a new one
    pmf = PkgMgrFrame.createFrame(thisPkg);
    // Yes, recent frame may teoretically be null.
    if ( recentFrame != null ) DialogManager.tileWindow(pmf, recentFrame);

    pmf.show();
    return pmf;
    }

  /**
   * Returns the Java class that is associated with this name in this package
   */
  Class getJavaClass () throws ProjectNotOpenException, ClassNotFoundException
    {
    Project bluejPrj = getBluejProject();

    Class aClass = bluejPrj.loadClass(qualifiedClassName);
    if ( aClass == null ) throw new ClassNotFoundException ("Class "+qualifiedClassName+" Not Found");

    return aClass;
    }

  /**
   * Returns the class target of this java class by checking its existence
   */
  ClassTarget getClassTarget ()
    throws ProjectNotOpenException, PackageNotFoundException
    {
    Package bluejPkg = getBluejPackage();

    String className = qualifiedClassName;
    int dotpos = qualifiedClassName.lastIndexOf(".");
    if ( dotpos > 0 ) className = qualifiedClassName.substring(dotpos+1);
    Target aTarget = bluejPkg.getTarget(className);

    if ( aTarget == null ) 
      throw new NullPointerException ("Class "+qualifiedClassName+" has NO ClassTarget");
    
    if ( ! (aTarget instanceof ClassTarget) ) 
      throw new NullPointerException ("Class "+qualifiedClassName+" is not of type ClassTarget");

    return (ClassTarget)aTarget;
    }


  /**
   * Returns the view associated with this Class
   */
  View getBluejView () 
    throws ProjectNotOpenException, ClassNotFoundException
    {
    Class aClass = getJavaClass();

    // View.getView does not fail, if the class does not exist it will be created.
    return View.getView(aClass);
    }
}