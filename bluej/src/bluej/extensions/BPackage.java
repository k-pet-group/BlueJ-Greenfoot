package bluej.extensions;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Target;
import bluej.pkgmgr.ClassTarget;
import bluej.debugger.ObjectWrapper;

import java.util.List;
import java.util.ListIterator;
import java.awt.Component;
import java.awt.Frame;

/**
 * The BlueJ proxy Package object. 
 * This represents an open package, and functions relating to that package.
 * NOTA: The same reasoning apply here as for the BProject. I am storing as little as
 * possible and try to get all info dynamically. I need this to be in sync with BlueJ
 * This MAY NOT be ENOUGH so, check it with Andrew.
 *
 * @version $Id: BPackage.java 1647 2003-03-05 12:00:24Z damiano $
 */
public class BPackage
{
    // Now this can be private, leave it private, of course. Damiano
    private final Package bluej_pkg;

    /**
     * NOT to be used by the extensions writer. You can get packaged from Projects !
     * Unfortunately it must be bublic since it is called by the extension manager
     */
    public BPackage (Package i_pkg)
    {
        bluej_pkg = i_pkg;
    }

    /**
     * Gets the package's project
     * <p>If this is an empty package, no action will be taken and <code>null</code> will be returned.
     * @return the project that this package belongs to
     */
    public BProject getProject()
    {
        if (bluej_pkg == null) return null;
        return new BProject (bluej_pkg.getProject());
    }



    /**
     * Gets the name of the package. This might well be an empty String.
     *
     * @return fully-qualified name of the package, or
     * an empty string if the package is the 'default' package
     * or <code>null</code> if this Package is an empty frame.
     */
    public String getName()
    {
        if ( ! isValid() ) return null;
        return bluej_pkg.getQualifiedName();
    }
    
    /**
     * Gets a handle on the package's frame, in case you need to do modal dialogues etc.
     * @return package Frame. Can return null if none found
     */
    public Frame getFrame()
    {
       PkgMgrFrame pmf = PkgMgrFrame.findFrame (bluej_pkg);
       return pmf;
    }

    /**
     * Determines whether this is a valid BPackage.
     *
     * @return <code>true</code> if this there is no package open
     */
    public boolean isValid()
    {
        if ( bluej_pkg == null) return false;
        // TODO: Possible other checks here
        return (true);
    }
    
    /**
     * Gets a class proxy object relating to a particular class in this package.
     * <p>If this is an empty package, no action will be taken and <code>null</code> will be returned.
     * @param name the simple name of the required class. For example, <CODE>Person</CODE>.
     * @return the proxy class object, or <CODE>null</CODE> if no such class exists.
     */
    public BClass getClass (String name)
    {
        if ( ! isValid() ) return null;

        Target classTarget = bluej_pkg.getTarget (name);

        if (classTarget == null ) return null;
        if ( !(classTarget instanceof ClassTarget)) return null;
        
        return new BClass (bluej_pkg, (ClassTarget)classTarget);
    }
    
    /**
     * Gets a list of the class names contained within this package.
     *
     * @return an array containing all BClass of the BPackage
     */
    public BClass[] getClasses()
    {
        if (! isValid()) return new BClass[0];
        
        List names = bluej_pkg.getAllClassnames();
        BClass[] classes = new BClass [names.size()];
        for (ListIterator iter=names.listIterator(); iter.hasNext();) {
            int index=iter.nextIndex();
            String name = (String)iter.next();
            classes [index] = getClass (name);
        }
        return classes;
    }
    

    /**
     * Gets a System class. This can be manipulated in the same way as other BlueJ proxy classes.
     * @param name the fully-qualified name of the System class
     * @return the proxy class object
     */
    public BClass getSystemClass (String name)
    {
        Class cl = null;
        try {
            cl = ClassLoader.getSystemClassLoader().loadClass (name);
        } catch (ClassNotFoundException ex) {
            return null;
        }
        return new BClass (bluej_pkg, cl);
    }

    /**
     * Get an object proxy object for an object shown on the Object Bench.
     * @param name the name of the object
     * @return the proxy object, or <CODE>null</CODE> if no such object exists.
     */
    public BObject getObject (String name)
    {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame (bluej_pkg);
        if ( pmf == null ) return null;

        ObjectWrapper[] objects = pmf.getObjectBench().getWrappers();
        for (int i=0; i<objects.length; i++) {
            ObjectWrapper wrapper = objects[i];
            if (wrapper.getName().equals (name)) return new BObject (wrapper);
        }
        return null;
    }    

    /**
     * Gets a list of the object names on the object bench of this project.
     * @return an array containing the names of all the objects on the object bench
     */
    public BObject[] getObjects()
    {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame (bluej_pkg);
        if ( pmf == null ) return new BObject[0];
   
        ObjectWrapper[] objectWrappers = pmf.getObjectBench().getWrappers();
        BObject[] objects = new BObject [objectWrappers.length];
        for (int i=0; i<objectWrappers.length; i++) {
            ObjectWrapper wrapper = (ObjectWrapper)objectWrappers[i];
            objects[i] = new BObject (wrapper);
        }
        return objects;
    }
    

    /**
     * Compile classes
     * @param forceAll if <code>true</code> rebuilds the entire project, otherwise
     * only compiles currently uncompiled classes
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