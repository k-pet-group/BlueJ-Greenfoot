
package bluej.parser.symtab;

import java.util.Enumeration;

/*******************************************************************************
 * Because methods can be overloaded and member data can have the same name
 *  as a method, we provide this dummy definition to hold a list of all
 *  definitions in a scope with the same name.
 ******************************************************************************/
class MultiDef extends Definition       
{ 
    //==========================================================================
    //==  Class Variables
    //==========================================================================
    
    /** A list of the various definitions for this symbol name */
    private JavaVector defs;


    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /** Constructor to create a new multidef object.
     *  This version takes its information from a symbol that
     *  it will be replacing.
     *  This is just a convenience form of the real constructor
     *  that takes a Definition as the base for the new MultiDef
     */
    MultiDef(String name,         // the name of the definition
             Definition oldDef) { // a standing def with its name
        this(name, oldDef.getOccurrence(),
              oldDef.getParentScope());
    }   


    /** Constructor to create a new multidef object */
    MultiDef(String name,                   // the name of the definition
             Occurrence occ,            // where it was defined
             ScopedDef parentScope) {   // the overall symbol table
        super(name, occ, parentScope);
        
        // Create the list to store the definitions
        defs = new JavaVector();
    }   


    /** Add a definition to the list of symbols with the same name */
    void addDef(Definition def) {
        defs.addElement(def);
    }   

    public void getInfo(ClassInfo info, SymbolTable symbolTable) {
        Enumeration e = defs.elements();
        while(e.hasMoreElements()) {
            Definition d = (Definition)e.nextElement();
            d.getInfo(info, symbolTable);
        }

    }

    /** Lookup a symbol in the list of symbols
     *  This is a rather lame approximation that just returns the first match
     *   based on number of parameters.  A real routine to perform this would
     *   use the best-fit parameter type matching algorithm described
     *   in the Java Language Specification
     */
    Definition lookup(String name,          // the name to locate
                                    int numParams) {        // number of params
        // note that the name isn't used...  all definitions contained
        //   by the MultiDef have the same name
                                    
        // walk through the list of symbols
        Enumeration e = defs.elements();
        while(e.hasMoreElements()) {
            Definition d = (Definition)e.nextElement();
                        
            // If the symbol is a method and it has the same number of
            //   parameters as what we are calling, assume it's the match
            if (d instanceof MethodDef) {
                if (((MethodDef)d).getParamCount() == numParams)
                    return d;
                }
                
            // otherwise, if it's not a method, AND we're not looking for
            //   a method, return the definition found.
            else if (numParams == -1)
                return d;
        }   

        // If we didn't find a match return null
        return null;
    }


    /** Resolve references to other symbols */
    void resolveTypes(SymbolTable symbolTable) {
        defs.resolveTypes(symbolTable);  // resolve all the definitions
        // DO NOT resolve anything else! (ie don't call super.resolveTypes() )
        //  this is just a placeholder for a group of symbols with the same name
    }
}
