package bluej.debugger.gentype;

import java.util.Iterator;
import java.util.List;

/*
 * A "reflective" is an object representing a java type. This interface
 * provides methods to, for instance, find the superclass/superinterfaces,
 * determine the generic type parameters, etc.
 *  
 * @author Davin McCall
 * @version $Id: Reflective.java 2581 2004-06-10 01:09:01Z davmac $
 */
public abstract class Reflective {

    // Get the name of the class this reflective represents
    // (eg. "java.util.List").
    public abstract String getName();
    
    // Get the formal type parameters of the class this reflective represents
    // (ordered List of GenTypeDeclTpar)
    public abstract List getTypeParams();
    
    // Return a list of Reflective.
    public abstract List getSuperTypesR();
    
    // Return a list of GenType. The arguments to the base type are
    // expressed in terms of the parent type, for instance,
    // List<T> extends Collection<T>.
    public abstract List getSuperTypes();
    
    // Get the corresponding array reflective
    // public abstract Reflective getArray();
    
    // Get a supertype by its raw name. The default implementation just
    // searches the list returned by getSuperTypes().
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
