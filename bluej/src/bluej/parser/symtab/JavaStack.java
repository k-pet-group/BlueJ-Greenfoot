
package bluej.parser.symtab;

/*******************************************************************************
 * An extended Stack class to provide simple lookup and type
 *   resolution methods
 ******************************************************************************/
class JavaStack extends java.util.Stack {
    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /** A constructor for the Java stack */
    JavaStack() {
    }   


    /** Find the class definition in the stack closest to the top */
    Definition findTopmostClass() {
        // unfortunately, the enumeration of a stack returns it
        // in the reverse order in which we want to traverse it...
        // So we must walk manually...
        for(int i = size()-1; i > -1; i--)
            if ((elementAt(i)) instanceof ClassDef)
                return (Definition)elementAt(i);
        return null;
    }


    /** a wrapper to lookup a non-method -- calls the real lookup 
     *  method passing -1 for num parameters (meaning no parameters)
     */
    Definition lookup(String name) {
        return lookup(name, -1);
    }   


    /** Lookup a method in the stack */
    Definition lookup(String name, int numParams) {
        // unfortunately, the enumeration of a stack returns it
        // in the reverse order in which we want to traverse it...
        // So we must walk manually...
        for(int i = size()-1; i > -1; i--) {
            Definition d = ((Definition)elementAt(i)).lookup(name, numParams);
            if (d != null) return d;
        }
        return null;
    }   
}
