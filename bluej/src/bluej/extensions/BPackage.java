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
 * The BlueJ proxy Package object. This represents an open package, and functions relating
 * to that package.
 *
 * @author Clive Miller
 * @version $Id: BPackage.java 1631 2003-02-25 11:33:16Z damiano $
 *
 * @see bluej.extensions.BProject#getCurrentPackage()
 * @see bluej.extensions.BProject#getPackage(java.lang.String)
 * @see bluej.extensions.BProject#getPackages()
 */
public class BPackage
{
    private final Package pkg;
    private final PkgMgrFrame pmf;


    public BPackage (Package i_pkg)
    {
        pkg = i_pkg;
        pmf = PkgMgrFrame.findFrame (i_pkg);
    }

    
    BPackage (Package i_pkg, PkgMgrFrame i_pmf )
    {
        pkg = i_pkg;
        pmf = i_pmf;
    }



    Package getRealPackage()
    {
        return pkg;
    }
  
    /**
     * Gets the name of the package. This might
     * well be an empty String.
     * @return fully-qualified name of the package, or
     * an empty string if the package is the 'default' package
     * or <code>null</code> if this Package is an empty frame.
     */
    public String getName()
    {
        if (pkg == null) return null;
        return pkg.getQualifiedName();
    }
    
    /**
     * Gets a handle on the package's frame, in case you 
     * need to do modal dialogues etc.
     * @return package Frame.
     */
    public Frame getFrame()
    {
        return pmf;
    }

    /**
     * Determines whether this is an empty package frame.
     * If it is, some of the other functions will not be possible!
     * @return <code>true</code> if this there is no package open
     */
    public boolean isEmptyFrame()
    {
        return (pkg == null);
    }
    
    /**
     * Gets a class proxy object relating to a particular class in this package.
     * <p>If this is an empty package, no action will be taken and <code>null</code> will be returned.
     * @param name the simple name of the required class. For example, <CODE>Person</CODE>.
     * @return the proxy class object, or <CODE>null</CODE> if no such class exists.
     */
    public BClass getClass (String name)
    {
        if (isEmptyFrame()) return null;
        Target ct = pkg.getTarget (name);
        if (ct == null || !(ct instanceof ClassTarget)) return null;
        return new BClass (this, (ClassTarget)ct);
    }
    
    /**
     * Gets a list of the class names contained within this package.
     * <p>If this is an empty package, no action will be taken and <code>null</code> will be returned.
     * @return an array containing the simple names of all the classes in the package
     */
    public BClass[] getClasses()
    {
        if (isEmptyFrame()) return null;
        List names = pkg.getAllClassnames();
        BClass[] classes = new BClass [names.size()];
        for (ListIterator li=names.listIterator(); li.hasNext();) {
            int i=li.nextIndex();
            String name = (String)li.next();
            classes [i] = getClass (name);
        }
        return classes;
    }
    
    /**
     * Get an object proxy object for an object shown on the Object Bench.
     * @param name the name of the object
     * @return the proxy object, or <CODE>null</CODE> if no such object exists.
     */
    public BObject getObject (String name)
    {
        ObjectWrapper[] objects = pmf.getObjectBench().getWrappers();
        for (int i=0; i<objects.length; i++) {
            ObjectWrapper wrapper = (ObjectWrapper)objects[i];
            if (wrapper.getName().equals (name)) return new BObject (this, wrapper, name);
        }
        return null;
    }    

    /**
     * Gets a list of the object names on the object bench of this project.
     * @return an array containing the names of all the objects on the object bench
     */
    public BObject[] getObjects()
    {
        ObjectWrapper[] objectWrappers = pmf.getObjectBench().getWrappers();
        BObject[] objects = new BObject [objectWrappers.length];
        for (int i=0; i<objectWrappers.length; i++) {
            ObjectWrapper wrapper = (ObjectWrapper)objectWrappers[i];
            objects[i] = new BObject (this, wrapper, wrapper.getName());
        }
        return objects;
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
        return new BClass (this, cl);
    }

    /**
     * Compile classes
     * @param forceAll if <code>true</code> rebuilds the entire project, otherwise
     * only compiles currently uncompiled classes
     */
    public void compile (boolean forceAll)
    {
        if (pkg == null) return;
        if (forceAll) pkg.rebuild(); 
        else pkg.compile();
    }
    
    /**
     * Reloads the entire package
     */
    public void reload()
    {
        if (pkg == null) return;
        pkg.reload();
    }
    
    /**
     * Gets the package's project
     * <p>If this is an empty package, no action will be taken and <code>null</code> will be returned.
     * @return the project that this package belongs to
     */
    public BProject getProject()
    {
        if (pkg == null) return null;
        return new BProject (pkg.getProject());
    }
}