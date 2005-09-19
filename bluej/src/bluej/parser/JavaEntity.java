package bluej.parser;

import bluej.debugger.gentype.JavaType;

/**
 * A general abstraction for handling entities which may have fields or
 * members - including packages, classes, and values.
 * 
 * @author Davin McCall
 * @version $Id$
 */
abstract class JavaEntity
{
    /**
     * Get the type of the entity. For a class entity, this is the class itself
     * (may be generic). For a value this is the value type.
     * 
     * Throws SemanticException if the entity doesn't have an
     * assosciated type (for instance, it represents a package)
     */ 
    abstract JavaType getType() throws SemanticException;
    
    /**
     * Get a sub-entity (member, field, whatever) by name.
     * @param name  The name of the subentity
     * @return  The subentity
     * @throws SemanticException  if the given subentity doesn't exist
     */
    abstract JavaEntity getSubentity(String name) throws SemanticException;
    
    abstract boolean isClass();
    
    abstract String getName();
}
