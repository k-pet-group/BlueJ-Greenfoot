package bluej.extensions;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.target.*;
import bluej.pkgmgr.target.Target;
import bluej.debugger.ObjectWrapper;



import java.util.List; 
import java.util.ListIterator;
import java.awt.Frame;



/**
 * A wrapper for a single package of a BlueJ project.
 * This represents an open package, and functions relating to that package.
 *
 * @version $Id: BPackage.java 1954 2003-05-15 06:06:01Z ajp $
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
     BPackage (Package aBlueJpkg)
    {
        // Unfortunately it must be bublic since it is called by the extension manager
        bluej_pkg = aBlueJpkg;
    }

    /**
     * Determines whether this is a valid package.
     * This object may not be valid since what it represents may have been modified or deleted
     * from the main BlueJ graphical user interface.
     * Returns true if it is still valid, false otherwise.
     */
    public boolean isValid()
    {
        if ( bluej_pkg == null) return false;
        // TODO: Possible other checks here
        return (true);
    }


    /**
     * Returns the package's project.
     * It will return null if this is an invalid package.
     */
    public BProject getProject()
    {
        if (bluej_pkg == null) return null;
        return new BProject (bluej_pkg.getProject());
    }



    /**
     * Returns the name of the package. 
     * Returns an empty string if no package name has been set.
     * Returns a null string if it is an invalid package.
     */
    public String getName()
    {
        if ( ! isValid() ) return null;
        return bluej_pkg.getQualifiedName();
    }
    
    /**
     * Returns the package frame.
     * This can be used (e.g.) as the "parent" frame for positioning modal dialogues.
     * Returns null if this is not a valid package.
     */
    public Frame getFrame()
    {
       PkgMgrFrame pmf = PkgMgrFrame.findFrame (bluej_pkg);
       return pmf;
    }

    
    /**
     * Returns the class with the given name in this package.
     * Returns null if this is an invalid package.
     * Returns null if the class name does not exist.
     * 
     * @param name the simple name of the required class.
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
     * Returns an array containing all the classes in this package.
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
     * Returns a wrapper for the object with the given name on BlueJ's object bench.
     * @param name the name of the object as shown on the object bench
     * @return the object, or null if no such object exists.
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
     * Returns an array of all the Objects on the object bench.
     * The array will be empty if no objects are on the bench.
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
     * Compile this package.
     * If forceAll is true it will compile all files otherwise it will compile
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
     * This is used (e.g.) when a new <code>.java</code> file has been added to the package.
     */
    public void reload()
    {
        if (bluej_pkg == null) return;
        bluej_pkg.reload();
    }


}
