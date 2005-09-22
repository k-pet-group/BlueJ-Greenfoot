package bluej.parser.symtab;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Scope
{
    private Scope parentScope;
    
    private Set types;
    private Set variables;
    
    /**
     * Construct a new scope with the given parent. The parent may be null to indicate
     * that this scope has no parent scope.
     */
    public Scope(Scope parent)
    {
        parentScope = parent;
        types = new HashSet();
        variables = new HashSet();
    }
    
    /**
     * Add a variable name into the current scope.
     */
    public void addVariable(String name)
    {
        variables.add(name);
    }
    
    /**
     * Check to see whether a variable with the given name is defined in this scope
     * or a parent scope.
     */
    public boolean checkVariable(String name)
    {
        if (variables.contains(name))
            return true;
        
        if (parentScope == null)
            return false;
        
        return parentScope.checkVariable(name);
    }
    
    /**
     * Add a type into the current scope.
     */
    public void addType(String name)
    {
        types.add(name);
    }
    
    /**
     * Check to see whether a type (class or interface) with the given name is defined
     * in this scope or a parent scope. In general, a name should be resolved as a variable
     * first (checkVariable method) unless context excludes variables.
     */
    public boolean checkType(String name)
    {
        if (types.contains(name))
            return true;
        
        if (parentScope == null)
            return false;
        
        return parentScope.checkType(name);
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
    public void addMethod(String name, String tpars, String retType, List paramTypes, List paramNames, String comment)
    {
        // By default, don't do anything.
    }
}
