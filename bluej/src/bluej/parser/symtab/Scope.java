/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.parser.symtab;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Scope
{
    private Scope parentScope;
    
    private Set<String> types;
    private Set<String> variables;
    private Set<String> varOrClassRefs;  // unresolved references to a class or variable
    private Set<String> classRefs; // unresolved references to a class
    
    /**
     * Construct a new scope with the given parent. The parent may be null to indicate
     * that this scope has no parent scope.
     */
    public Scope(Scope parent)
    {
        parentScope = parent;
        types = new HashSet<String>();
        variables = new HashSet<String>();
        varOrClassRefs = new HashSet<String>();
        classRefs = new HashSet<String>();
    }
    
    /**
     * Add a variable name into the current scope.
     */
    public void addVariable(String name)
    {
        variables.add(name);
        varOrClassRefs.remove(name);
    }
    
    /**
     * Add a reference to an entity which might be either a variable or a class.
     */
    public void addVarOrClassReference(String name)
    {
    	if (variables.contains(name) || types.contains(name)) {
    		return;
    	}
    	
    	varOrClassRefs.add(name);
    }
    
    /**
     * Add a type into the current scope. The subscope is the scope within the type
     * declaration.
     */
    public void addType(String name, Scope subscope)
    {
        types.add(name);
        classRefs.remove(name);
        varOrClassRefs.remove(name);
    }
        
    /**
     * Add a reference to a type.
     */
    public void addTypeReference(String name)
    {
    	if (! types.contains(name)) {
    		classRefs.add(name);
    	}
    }
    
    /**
     * Add a method into this scope.
     * @param name  The name of the method
     * @param tpars   The type parameters, in the form required by ClassInfo.addComment().
     *                May be null.
     * @param retType  The return type of this method (null for a constructor)
     * @param paramTypes  The parameter types (may be null if no parameters)
     * @param paramNames  The parameter names as declared in the method definition
     *                    (may be null)
     * @param comment   The attached javadoc comment, if any, or null.
     */
    public void addMethod(String name, String tpars, String retType, List<?> paramTypes, List<?> paramNames, String comment)
    {
        // By default, don't do anything.
    }
    
    /**
     * Close this scope, i.e. indicate that no new variables/types/methods will be added to it.
     */
    public void closeScope()
    {
    	for (String varRef : varOrClassRefs) {
    		parentScope.addVarOrClassReference(varRef);
    	}
    	for (String classRef : classRefs) {
    		parentScope.addTypeReference(classRef);
    	}
    }
    
    /**
     * Check whether the given type is defined in this scope.
     */
    public boolean hasType(String name)
    {
    	return types.contains(name);
    }
}
