package bluej.extensions;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Target;
import bluej.pkgmgr.ClassTarget;
import bluej.debugger.ObjectWrapper;



import java.util.List; 
import java.util.ListIterator;
import java.awt.Frame;



/**
 * A wrapper for a Package in the BlueJ environment.
 * This represents an open package, and functions relating to that package.
 *
 * @version $Id: BPackage.java 1838 2003-04-11 13:16:46Z damiano $
 */

/*
 * Author Clive Miller, University of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 */
 
public class BPackage
{
    // Now this can be private, leave it private, of course. Damiano
    private final Package bluej_pkg;

    /**
     * Constructor for a BPackage.
     */
    public BPackage (Package aBlueJpkg)
    {
        // Unfortunately it must be bublic since it is called by the extension manager
        bluej_pkg = aBlueJpkg;
    }

    /**
     * Determines whether this is a valid package.
     * This object may not be valid since what it represent has been modified or deleted
     * from the main BlueJ graphical user interface.
     * Return true if it is still valid, false othervise.
     */
    public boolean isValid()
    {
        if ( bluej_pkg == null) return false;
        // TODO: Possible other checks here
        return (true);
    }


    /**
     * Return the package's project.
     * It will return null if this is an invalid package.
     */
    public BProject getProject()
    {
        if (bluej_pkg == null) return null;
        return new BProject (bluej_pkg.getProject());
    }



    /**
     * Return the name of the package. 
     * This might well be an empty String if no package name has been set.
     * It can be a null string if it is an invalid package.
     */
    public String getName()
    {
        if ( ! isValid() ) return null;
        return bluej_pkg.getQualifiedName();
    }
    
    /**
     * Return a handle on the package's frame.
     * This may be needed for modal dialogues.
     * It may return null if this is not a valid package.
     */
    public Frame getFrame()
    {
       PkgMgrFrame pmf = PkgMgrFrame.findFrame (bluej_pkg);
       return pmf;
    }

    
    /**
     * Return the class with the given name in this package.
     * It may return null if this is an invalid package.
     * It may return null if the class name does not exist.
     * 
     * @param name the simple name of the required class. For example, <CODE>Person</CODE>.
     */
    public BClass getBClass (String name)
    {
        if ( ! isValid() ) return null;

        Target classTarget = bluej_pkg.getTarget (name);

        if (classTarget == null ) return null;
        if ( !(classTarget instanceof ClassTarget)) return null;
        
        return new BClass (bluej_pkg, (ClassTarget)classTarget);
    }
    
    /**
     * Return an array containing all classes in this package.
     * If there are no classes or the package is invalid an empty array will be returned.
     */
    public BClass[] getBClasses()
    {
        if (! isValid()) return new BClass[0];
        
        List names = bluej_pkg.getAllClassnames();
        BClass[] classes = new BClass [names.size()];
        for (ListIterator iter=names.listIterator(); iter.hasNext();) {
            int index=iter.nextIndex();
            String name = (String)iter.next();
            classes [index] = getBClass (name);
        }
        return classes;
    }
    
    /**
     * Return an object shown on the Object Bench.
     * @param name the name of the object as shown on the object bench
     * @return the object, or <CODE>null</CODE> if no such object exists.
     */
    public BObject getObject (String instanceName)
    {
        // The usual check to avoid silly stack trace
        if ( instanceName == null ) return null;

        PkgMgrFrame pmf = PkgMgrFrame.findFrame (bluej_pkg);
        // The above may return null, unfortunately.
        if ( pmf == null ) return null;
        
        ObjectWrapper[] objects = pmf.getObjectBench().getWrappers();
        for (int index=0; index<objects.length; index++) 
            {
            ObjectWrapper wrapper = objects[index];
            if (instanceName.equals(wrapper.getName())) return new BObject (wrapper);
            }
        return null;
    }    

    /**
     * Return an array of all the Objects on the Object bench.
     * It can be an empty array if no objects are on the bench.
     */
    public BObject[] getObjects()
    {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame (bluej_pkg);
        if ( pmf == null ) return new BObject[0];
   
        ObjectWrapper[] objectWrappers = pmf.getObjectBench().getWrappers();
        BObject[] objects = new BObject [objectWrappers.length];
        for (int index=0; index<objectWrappers.length; index++) {
            ObjectWrapper wrapper = (ObjectWrapper)objectWrappers[index];
            objects[index] = new BObject (wrapper);
        }
        return objects;
    }
    

    /**
     * Compile this Package.
     * If forceAll is true it will compile all files othervise it will compile
     * just the ones that are modified.
     * @param forceAll if <code>true</code> compile all files.
     */
    public void compile (boolean forceAll)
    {
        if (bluej_pkg == null) return;

        if (forceAll) bluej_pkg.rebuild(); 
        else bluej_pkg.compile();
    }
    
    /**
     * Reloads the entire package.
     * This is usually needed when a new java file has been added to the package.
     */
    public void reload()
    {
        if (bluej_pkg == null) return;
        bluej_pkg.reload();
    }


}