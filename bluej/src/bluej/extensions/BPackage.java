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
 * @version $Id: BPackage.java 1818 2003-04-10 13:31:55Z fisker $
 */
public class BPackage
{
    // Now this can be private, leave it private, of course. Damiano
    private final Package bluej_pkg;

    /**
     * NOT to be used by Extension writer.
     * BPackages can be obtained from BProject or directly from BlueJ
     */
    public BPackage (Package i_pkg)
    {
        // Unfortunately it must be bublic since it is called by the extension manager
        bluej_pkg = i_pkg;
    }

    /**
     * Return the package's project.
     * If this is an invalid package, no action will be taken and <code>null</code> will be returned.
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
     * This may be needed for modal dialogues etc.
     */
    public Frame getFrame()
    {
       PkgMgrFrame pmf = PkgMgrFrame.findFrame (bluej_pkg);
       return pmf;
    }

    /**
     * Determines whether this is a valid package.
     * Return true if it is, false othervise.
     */
    public boolean isValid()
    {
        if ( bluej_pkg == null) return false;
        // TODO: Possible other checks here
        return (true);
    }
    
    /**
     * Return a class proxy object relating to a particular class in this package.
     * If this is an empty package, no action will be taken and <code>null</code> will be returned.
     * 
     * @param name the simple name of the required class. For example, <CODE>Person</CODE>.
     * @return the proxy class object, or <CODE>null</CODE> if no such class exists.
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
     * 
     * @param forceAll if <code>true</code> rebuilds the entire project.
     */
    public void compile (boolean forceAll)
    {
        if (bluej_pkg == null) return;

        if (forceAll) bluej_pkg.rebuild(); 
        else bluej_pkg.compile();
    }
    
    /**
     * Reloads the entire package
     */
    public void reload()
    {
        if (bluej_pkg == null) return;
        bluej_pkg.reload();
    }


   

    
    

    
    
}