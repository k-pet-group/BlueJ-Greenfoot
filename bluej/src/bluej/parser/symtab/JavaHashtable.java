
package bluej.parser.symtab;

import java.util.Enumeration;

/*******************************************************************************
 * An extension of the java.util.Hashtable that is used to 
 * add some simple looup and type resolution
 ******************************************************************************/
class JavaHashtable extends java.util.Hashtable {
    //==========================================================================
    //==  Class Variables
    //==========================================================================
    
    /** prevent nested resolutions... */
    private boolean resolving=false;

    private static final int CLASS = 0;
    private static final int INTERFACE = 1;
    private static final int EITHER = 2;


    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /** Constructor to create a new java hash table */
    JavaHashtable() {
        super();
    }


    /** List the names of all elements in the hashtable */
    void list(IndentingPrintWriter out) {
        Enumeration e = elements();
        while(e.hasMoreElements())
            out.println(((Definition)e.nextElement()).getQualifiedName());
    }


    /** list the names of all elements in the hashtable, but first print
     *  a title for the section and indent the following lines
     */
    void listIndented(IndentingPrintWriter out, String title) {
        if (title != null)
            out.println(title);
        out.indent();
        list(out);
        out.dedent();
    }


    /** Resolve the types of dummy elements in the hash table */
    void resolveTypes(SymbolTable symbolTable) {
        if (!resolving) {
            resolving = true;
            // walk through each element in the hash table
            Enumeration e = elements();
            while(e.hasMoreElements()) {
                Definition d = (Definition)e.nextElement();
            
                // if the element is a Dummy class or dummy interface, we
                //   will replace it with the real definition
                if (d instanceof DummyClass) {
                    Definition newD;
                    
                    // get its package name and look up the class/interface
                    String pkg = ((DummyClass)d).getPackage();
                    newD = symbolTable.lookupDummy(d);
                
                    // if we found the class/interface, 
                    //    add a reference to it, and replace the current def
                    //    with the one we found
                    if (newD != null) {
                        newD.addReference(d.getOccurrence());
                        remove(d.getName());
                        put(d.getName(), newD);
                    }   
                }   
            
                // otherwise, ask it if it needs resolution
                else
                    d.resolveTypes(symbolTable);
            }   
        }   
    }
}
