package bluej.debugger.gentype;

import java.util.Iterator;
import java.util.List;

/**
 * A "reflective" is an object representing a java type. This interface
 * provides methods to, for instance, find the superclass/superinterfaces,
 * determine the generic type parameters, etc.
 *  
 * @author Davin McCall
 * @version $Id: Reflective.java 3075 2004-11-09 00:10:18Z davmac $
 */
public abstract class Reflective {

    /**
     * Get the name of the class or interface represented by the reflective.
     * The name is such that it can be passed to ClassLoader's loadClass
     * method.
     * 
     * @return The fully qualified class/interface name.
     */
    public abstract String getName();
    
    /**
     * Get the formal type parameters of the class/interface this reflective
     * represents.
     * @return  The parameters as a List of GenTypeDeclTpar
     */
    public abstract List getTypeParams();
    
    /**
     * Get the supertypes of this reflective, as a list of reflectives. 
     * @return A List of Reflectives
     */
    public abstract List getSuperTypesR();
    
    /**
     * Get the supertypes of this reflective, as a list of GenTypes. The type
     * parameter names will refer to the type parameters in the parent type.
     * @return A List of GenTypeClass.
     */
    public abstract List getSuperTypes();
    
    /**
     * Get a reflective which represents an array, whose element type is
     * represented by this reflective.
     * 
     * @return A reflective representing an array
     */
    public abstract Reflective getArrayOf();
    
    /**
     * Return true if a variable of the reference type reflected by this
     * reflective can be assigned a value of the type represented by the given
     * reflective.
     * 
     * @param r  The other reflective
     * @return   True if the other reflective type is assignable to this type
     */
    public abstract boolean isAssignableFrom(Reflective r);
    
    /**
     * Get a supertype (as a GenTypeClass) by name. The default implementation
     * uses getSuperTypes() and searches the resulting list.
     * 
     * @param rawName   the name of the supertype to find
     * @return          the supertype as a GenTypeClass
     */
    public GenTypeClass superTypeByName(String rawName)
    {
        List superTypes = getSuperTypes();
        Iterator i = superTypes.iterator();
        while( i.hasNext() ) {
            GenTypeClass next = (GenTypeClass)i.next();
            if( next.rawName().equals(rawName) )
                return next;
        }
        return null;
    }
}
