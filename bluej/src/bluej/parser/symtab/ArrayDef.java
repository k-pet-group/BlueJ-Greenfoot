
package bluej.parser.symtab;

/*******************************************************************************
 * Definition of an array type.  Note that this is not currently used in the
 * cross reference tool, but you would define something like this if you
 * wanted to make the tool complete.
 ******************************************************************************/
class ArrayDef extends Definition implements TypedDef
{ 
    //==========================================================================
    //==  Class Variables
    //==========================================================================
    
    /** The base type for the Array */
    private Definition type;


    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /** Constructor to create a new array type */
    ArrayDef(String name,               // the name of the symbol
             Occurrence occ,            // the location of its def
             ScopedDef parentScope) {   // scope containing the def
        super(name, occ, parentScope);
    }   


    /** return the base type of the array */
    public Definition getType() {
        return type;
    }   


    /** Collect information about the array */
    public void getInfo(ClassInfo info) {
        System.out.println("info missing from array: " + getQualifiedName());
    }


    /** Resolves references to other symbols used by this symbol */
    void resolveTypes() {
        // would need to lookup the base type in the symbol table
    }   


    /** Return a String representation of the class */
    public String toString() {
        return "ArrayDef [" + type.getQualifiedName() + "]";
    }   
}
