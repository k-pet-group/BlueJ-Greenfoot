package bluej.parser;

import java.util.List;

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.JavaType;

/**
 * An entity representing either a package or a class (but not a value).
 * 
 * @author Davin McCall
 */
abstract class PackageOrClass extends JavaEntity
{
    /**
     * Returns a subentity which is either a package or class entity.
     * This is the same as getSubentity, but cannot yield a value.
     */ 
    abstract PackageOrClass getPackageOrClassMember(String name) throws SemanticException;
}

/**
 * An entity representing a class or generic type.
 * 
 * @author Davin McCall
 */
abstract class ClassEntity extends PackageOrClass
{
    // getType won't throw SemanticException
    abstract JavaType getType();
    
    abstract GenTypeClass getClassType();
    
    /**
     * Set the type parameters of this entity. If the entity is not a class, this
     * throws a SemanticException. The return is a duplicate of this entity with
     * the type parameters set as specified.
     * 
     * @param tparams   A list of GenTypeParameterizable type parameters
     * @throws SemanticException
     */
    abstract ClassEntity setTypeParams(List tparams) throws SemanticException;
    
    /**
     * Get the accessible static member class with the given name, declared in the
     * class represented by this entity.
     * 
     * @param name  The name of the inner class to retrieve
     * @return      The specified class, as an entity
     * @throws SemanticException   if the specified class does not exist or is not
     *                             accessible
     */
    abstract ClassEntity getStaticMemberClass(String name) throws SemanticException;
    
    /**
     * Get the accessible static field with the given name, declared in the class
     * represented by this entity.
     * 
     * @param name  The name of the field to retrieve
     * @return  The specified field (as an entity)
     * @throws SemanticException  if the field does not exist or is not accessible
     */
    abstract JavaEntity getStaticField(String name) throws SemanticException;
    
    /**
     * Return a list (possibly empty) of static methods declared in this
     * class with the given name.
     * 
     * @param name  The name of the methods to retrieve
     * @return  A list of java.lang.reflect.Method
     */
    abstract List getStaticMethods(String name);
}
