package bluej.extensions;

import bluej.compiler.*;
import bluej.debugmgr.objectbench.*;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.*;
import bluej.utility.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;



/**
 * A wrapper for a single package of a BlueJ project.
 * This represents an open package, and functions relating to that package.
 *
 * @version $Id: BPackage.java 2266 2003-11-05 11:20:26Z damiano $
 */

/*
 * Author Clive Miller, University of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 */
 
public class BPackage
{
    private Identifier packageId;

    /**
     * Constructor for a BPackage.
     */
    BPackage (Identifier aPackageId)
    {
        packageId=aPackageId;
    }

    /**
     * Removes this package from BlueJ
     *
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public void remove() 
        throws ProjectNotOpenException, PackageNotFoundException
    {
        Package bluejPkg  = packageId.getBluejPackage();
        Package parentPkg = bluejPkg.getParent();

        PackageTarget pkgTarget=(PackageTarget)parentPkg.getTarget(bluejPkg.getBaseName());
        pkgTarget.removeImmediate();
    }

    /**
     * Returns the package's project.
     * @throws ProjectNotOpenException if the project has been closed by the user.
     */
    public BProject getProject() throws ProjectNotOpenException
    {
        Project bluejProject = packageId.getBluejProject();

        return new BProject (new Identifier(bluejProject));
    }



    /**
     * Returns the name of the package. 
     * Returns an empty string if no package name has been set.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public String getName() 
        throws ProjectNotOpenException, PackageNotFoundException
        {
        Package bluejPkg = packageId.getBluejPackage();

        return bluejPkg.getQualifiedName();
        }


    /**
     * Reloads the entire package.
     * This is used (e.g.) when a new <code>.java</code> file has been added to the package.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public void reload() 
        throws ProjectNotOpenException, PackageNotFoundException
        {
        Package bluejPkg = packageId.getBluejPackage();

        bluejPkg.reload();
        }

    /**
     * Creates a new Class with the given name.
     * The class name must not be a fully qualified name.
     */
    public BClass newClass ( String className )
        throws ProjectNotOpenException, PackageNotFoundException, MissingJavaFileException
        {
        Package bluejPkg = packageId.getBluejPackage();
        PkgMgrFrame bluejFrame = packageId.getPackageFrame();

        File classJavaFile = new File (bluejPkg.getPath(),className+".java");
        if ( ! classJavaFile.canWrite() ) 
            throw new MissingJavaFileException (classJavaFile.toString());
        
        bluejFrame.createNewClass(className,null);
        return getBClass ( className );
        }

    
    /**
     * Returns the package frame.
     * This can be used (e.g.) as the "parent" frame for positioning modal dialogues.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public Frame getFrame() 
        throws ProjectNotOpenException, PackageNotFoundException
        {
        return packageId.getPackageFrame();
        }

    
    /**
     * Returns the class with the given name in this package.
     * Returns null if the class name does not exist. Note the naming
     * inconsistency, which avoids a clash with
     * <code>java.lang.Object.getClass()</code>
     * 
     * @param name the simple name of the required class.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public BClass getBClass (String name)   
        throws ProjectNotOpenException, PackageNotFoundException
        {
        Project bluejPrj = packageId.getBluejProject();
        Package bluejPkg = packageId.getBluejPackage();

        Target aTarget = bluejPkg.getTarget (name);

        if ( aTarget == null ) return null;
        if ( !(aTarget instanceof ClassTarget)) return null;

        ClassTarget classTarget = (ClassTarget)aTarget;
        
        return new BClass (new Identifier (bluejPrj,bluejPkg, classTarget.getQualifiedName()));
        }
    
    /**
     * Returns an array containing all the classes in this package.
     * If there are no classes an empty array will be returned.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public BClass[] getClasses() 
        throws ProjectNotOpenException, PackageNotFoundException
        {
        Project bluejPrj = packageId.getBluejProject();
        Package bluejPkg = packageId.getBluejPackage();

        String pkgBasename = bluejPkg.getQualifiedName();
        if ( pkgBasename.length() > 1 ) pkgBasename = pkgBasename+".";
        
        List names = bluejPkg.getAllClassnames();
        
        BClass[] classes = new BClass [names.size()];
        for (ListIterator iter=names.listIterator(); iter.hasNext();) {
            int index=iter.nextIndex();
            String className = pkgBasename+(String)iter.next();
            classes [index] = new BClass (new Identifier (bluejPrj,bluejPkg,className));
            }
        return classes;
        }
    
    /**
     * Returns a wrapper for the object with the given name on BlueJ's object bench.
     * @param name the name of the object as shown on the object bench
     * @return the object, or null if no such object exists.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public BObject getObject (String instanceName) 
        throws ProjectNotOpenException, PackageNotFoundException
        {
        // The usual check to avoid silly stack trace
        if ( instanceName == null ) return null;

        Package bluejPkg = packageId.getBluejPackage();
        PkgMgrFrame pmf = packageId.getPackageFrame();
        
        ObjectWrapper[] objects = pmf.getObjectBench().getWrappers();
        for (int index=0; index<objects.length; index++) 
            {
            ObjectWrapper wrapper = objects[index];
            if (instanceName.equals(wrapper.getName())) return new BObject (wrapper);
            }
        return null;
    }    

    /**
     * Returns an array of all the Objects on the object bench.
     * The array will be empty if no objects are on the bench.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public BObject[] getObjects() 
        throws ProjectNotOpenException, PackageNotFoundException
        {
        Package bluejPkg = packageId.getBluejPackage();
        PkgMgrFrame pmf = packageId.getPackageFrame();
   
        ObjectWrapper[] objectWrappers = pmf.getObjectBench().getWrappers();
        BObject[] objects = new BObject [objectWrappers.length];
        for (int index=0; index<objectWrappers.length; index++) {
            ObjectWrapper wrapper = (ObjectWrapper)objectWrappers[index];
            objects[index] = new BObject (wrapper);
        }
        return objects;
    }
    

    /**
     * Compile all modified files of this package.
     * A single CompileEvent with all modified files listed will be generated.
     * @param  waitCompileEnd <code>true</code> waits for the compilation to be finished.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     * @throws CompilationNotStartedException if BlueJ is currently executing Java code.
     */
    public void compile ( boolean waitCompileEnd ) 
        throws ProjectNotOpenException, PackageNotFoundException, CompilationNotStartedException
        {
        Package bluejPkg = packageId.getBluejPackage();

        if ( ! bluejPkg.isDebuggerIdle() )
          throw new CompilationNotStartedException ("BlueJ is currently executing Java code");

        // Start compilation
        bluejPkg.compile();

        // if requested wait for the compilation to finish.
        if ( waitCompileEnd ) JobQueue.getJobQueue().waitForEmptyQueue();
        }
    

    /**
     * Compile all files of this package.
     * A single CompileEvent with all modified files listed will be generated.
     * @param  waitCompileEnd <code>true</code> waits for the compilation to be finished.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     * @throws CompilationNotStartedException if BlueJ is currently executing Java code.
     */
    public void compileAll ( boolean waitCompileEnd ) 
        throws ProjectNotOpenException, PackageNotFoundException, CompilationNotStartedException
        {
        Package bluejPkg = packageId.getBluejPackage();

        if ( ! bluejPkg.isDebuggerIdle() )
          throw new CompilationNotStartedException ("BlueJ is currently executing Java code");

        // Request for ALL files to be compiled
        bluejPkg.rebuild(); 

        // if requested wait for the compilation to finish.
        if ( waitCompileEnd ) JobQueue.getJobQueue().waitForEmptyQueue();
        }




    /** 
     * Returns the currently selected Class in a Package.
     * If no Class is being selected null is returned.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public BClass getCurrentClass ()
        throws ProjectNotOpenException, PackageNotFoundException
    {
        Target aTarget = packageId.getCurrentBluejTarget();
        // The above may return null if there is nothing selected.
        if ( aTarget == null ) return null;

        // Nothing to do if it is not a class target.
        if ( !(aTarget instanceof ClassTarget )) return null; 
        
        ClassTarget aClass = (ClassTarget)aTarget;
        String qualifiedClassName = aClass.getQualifiedName();
        Package attachedPkg = aClass.getPackage();
        Identifier anId = new Identifier (attachedPkg.getProject(),attachedPkg, qualifiedClassName);

        return new BClass(anId);
    }

    /** 
     * Returns the currently selected Object in the Object Bench.
     * If no Object is being selected null is returned.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public BObject getCurrentObject ()
        throws ProjectNotOpenException, PackageNotFoundException
    {
        PkgMgrFrame bluejFrame = packageId.getPackageFrame();

        ObjectBench aBench = bluejFrame.getObjectBench();
        // Should never happens really.
        if ( aBench == null ) return null;

        ObjectWrapper aWrapper = aBench.getSelectedObjectWrapper();
        // This can happen quite easly.
        if ( aWrapper == null ) return null;
        
        return new BObject(aWrapper);
    }



    /**
     * Returns the directory where this package is stored.
     * @throws ProjectNotOpenException if the project this package is part of has been closed by the user.
     * @throws PackageNotFoundException if the package has been deleted by the user.
     */
    public File getDir ()
        throws ProjectNotOpenException, PackageNotFoundException
    {
        Package aPkg = packageId.getBluejPackage();

        return aPkg.getPath();
    }

    /**
     * Returns a string representation of the Object
     */
    public String toString () 
      {
      try 
        {
        Package bluejPkg = packageId.getBluejPackage();
        return "BPackage: "+bluejPkg.getQualifiedName();
        }
      catch ( ExtensionException exc )
        {
        return "BPackage: INVALID";  
        }
      }

}
