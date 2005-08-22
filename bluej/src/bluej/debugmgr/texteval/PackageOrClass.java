package bluej.debugmgr.texteval;

import java.util.List;

import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.texteval.TextParser.SemanticException;

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
    
    /**
     * Set the type parameters of this entity. If the entity is not a class, this
     * throws a SemanticException. The return is a duplicate of this entity with
     * the type parameters set as specified.
     * 
     * @param tparams   A list of GenTypeParameterizable type parameters
     * @throws SemanticException
     */
    abstract ClassEntity setTypeParams(List tparams) throws SemanticException;
    
    abstract ClassEntity getStaticMemberClass(String name) throws SemanticException;
    
    abstract JavaEntity getStaticField(String name) throws SemanticException;
}
