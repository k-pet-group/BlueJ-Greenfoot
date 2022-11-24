/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2019  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser.entity;

import bluej.debugger.gentype.Reflective;

/**
 * An EntityResolver, broadly speaking, resolves identifiers into packages, classes,
 * fields and variables. A resolver can be specific to a context; for instance a
 * resolver inside a package normally sees classes in that package without requiring
 * qualification. 
 * 
 * @author Davin McCall
 */
public interface EntityResolver
{
    /**
     * Resolve a package or class. If a class with the given name exists in the resolver's scope,
     * it is returned; otherwise a package is returned.
     * 
     * @param name  The package or class name. This must be an unqualified name.
     * @param querySource  The source of the query (a fully qualified class name,
     *            as would be returned by Class.getName()).
     */
    public PackageOrClass resolvePackageOrClass(String name, Reflective querySource);
    
    /**
     * Resolve a class from its fully-qualified name. The supplied name should
     * be the same as would be returned by Class.getName() for the required type.
     */
    public TypeEntity resolveQualifiedClass(String name);
    
    /**
     * Resolve a value. If a local variable or field with the given name exists in the resolver's
     * scope (or if there is a static import of that name), it is returned; otherwise the effect is as if resolvePackageOrClass was called.
     * 
     * <p>To resolve the final value entity, call resolveAsValue() on the returned entity.
     * 
     * @param name The name of the entity to access
     * @param querySource The source of the query (a fully qualified class name,
     *            as would be returned by Class.getName()).
     */
    public JavaEntity getValueEntity(String name, Reflective querySource);
}
