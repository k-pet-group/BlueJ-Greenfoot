
package bluej.parser.symtab;

import java.util.Enumeration;
import bluej.parser.JavaToken;

/*******************************************************************************
 * An abstract class representing a symbol that provides a scope that 
 *  contains other symbols.
 ******************************************************************************/
abstract class ScopedDef extends Definition       
{ 
    //==========================================================================
    //==  Class Variables
    //==========================================================================
    
    /** A table of symbols "owned" by this symbol */
    private JavaHashtable elements;
    
    /** A list of yet-to-be-resolved references */
    private JavaVector unresolvedStuff;

    /** Is this scope one of the following:
     *  <dl>
     *  <dt>Base scope
     *    <dd>the scope that contains primitive types
     *  <dt>Default package
     *    <dd>where Java classes reside that are not explicitly in another scope.
     *  </dl>
     */
    private boolean iAmDefaultOrBaseScope = false;


    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /** Constructor to create the base part of a scoped definition */
    ScopedDef(String name,               // the scoped name
              Occurrence occ,            // where it's defined
              ScopedDef parentScope) {   // scope containing the def
        super(name, occ, parentScope);
        
        // Create a new hashtable for fast element lookup
        elements = new JavaHashtable();
    }   


    /** Add a symbol to our scope */
    void add(Definition def) {
        // Check to see if we already have a definition
        Definition oldDef = (Definition)elements.get(def.getName());
        
        // If so, we'll create a MultiDef to hold them
        if (oldDef != null) {
            // If the symbol there so far was not a MultiDef
            if (!(oldDef instanceof MultiDef)) {
                // remove the old definition
                elements.remove(oldDef);
                
                // create a new MultiDef
                MultiDef newMulti = new MultiDef(def.getName(), oldDef);
                
                // add the old symbol to the MultiDef
                newMulti.addDef(oldDef);
                oldDef = newMulti;
                
                // add the MultiDef back into the scope
                elements.put(def.getName(), oldDef);
            }
            
            // We now have a multidef, so add the new symbol to it
            ((MultiDef)oldDef).addDef(def);
        }   
        
        // Otherwise, just add the new symbol to the scope
        else {
            elements.put(def.getName(), def);
            def.setParentScope(this);
        }   
    }   


    /** Add a token to the list of unresolved references */
    void addUnresolved(JavaToken t) {
        // be lazy in our creation of the reference vector
        //   (many definitions might not contain refs to other symbols)
        if (unresolvedStuff == null)
            unresolvedStuff = new JavaVector();
        unresolvedStuff.addElement(t);
    }   


    /** Return whether or not this scope actually contains any elements */
    boolean hasElements() {
        return !elements.isEmpty();
    }   


    /** Return if this is a base or default scope.  This is used when printing
     *    information to the report so we won't prefix elements in these
     *    scopes.
     */
    boolean isDefaultOrBaseScope() {
        return iAmDefaultOrBaseScope;
    }   


    /** Lookup a method in the scope
     *  This is usually just a hashtable lookup, but if the element returned
     *  is a MultiDef, we need to ask it to find the best match
     */
    Definition lookup(String name, int numParams) {
        // Try to find the name in our scope
        Definition d = (Definition)elements.get(name);
        
        // if we found multiple defs of the same name, ask the multidef
        //  to do the resolution for us.
        if (d instanceof MultiDef)
            return d.lookup(name, numParams);
        
        // if we got a method back, check to see that the params apply
        else if (d instanceof MethodDef)
            if (((MethodDef)d).getParamCount() == numParams)
                return d;
            else
                return null;
        else
            return d;
    }   


    /** Collect information about all the elements */
    void getElementInfo(ClassInfo info, SymbolTable symbolTable) {
        Enumeration e = elements.elements();
        while(e.hasMoreElements()) {
            Definition d = (Definition)e.nextElement();
            d.getInfo(info, symbolTable);
        }
    }   


    /** Get info about elements in this scope */
    public void getInfo(ClassInfo info, SymbolTable symbolTable) {
        symbolTable.pushScope(this);

        // for method: get info about local var's
        getElementInfo(info, symbolTable);

        // for method: get info things referenced in statements
        if (unresolvedStuff != null)		// search refs to other syms
            unresolvedStuff.getUnresolvedInfo(info, symbolTable);

        symbolTable.popScope();
    }


    /** Resolve referenced names */
    void resolveTypes(SymbolTable symbolTable) {
        symbolTable.pushScope(this);        // push the current scope
        elements.resolveTypes(symbolTable); // resolve elements in this scope
        if (unresolvedStuff != null) {      // resolve refs to other syms
            unresolvedStuff.resolveRefs(symbolTable);
            unresolvedStuff = null;
        }   
        symbolTable.popScope();            // pop back out of the scope
        super.resolveTypes(symbolTable);  // let superclass resolve if needed
    }


    /** Indicate that this scope is a base scope or default package */
    void setDefaultOrBaseScope(boolean val) {
        iAmDefaultOrBaseScope = val;
    }   
}
