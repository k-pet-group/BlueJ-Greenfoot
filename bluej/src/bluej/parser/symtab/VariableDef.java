
package bluej.parser.symtab;

/*******************************************************************************
 * Definition of a variable in a source file.
 *  This can be member data in class, 
 *  a local variable or a method parameter.
 ******************************************************************************/
class VariableDef extends Definition implements TypedDef
{ 
    //==========================================================================
    //==  Class Variables
    //==========================================================================
    
    /** The type of the variable */
    private Definition type = null;


    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /** Constructor to create a new variable symbol */
    VariableDef(String name,               // the variable's name
                Occurrence occ,            // where it was defined
                ClassDef type,             // the type of the variable
                ScopedDef parentScope) {   // which scope owns it
        super(name, occ, parentScope);
        this.type = type;
    }   


    /** get the type of the variable */
    public Definition getType() {
        return type;
    }   


    /** Collect information about this variable */
    public void getInfo(ClassInfo info, SymbolTable symbolTable) {
	info.addUsed(type.getQualifiedName());
    }   


    /** Resolve referenced symbols used by this variable */
    void resolveTypes(SymbolTable symbolTable) {

	//System.out.println("   using class (var): " + type.getName());

        if (type != null && type instanceof DummyClass) {
            // resolve the type of the variable
            Definition newType = (Definition)symbolTable.lookupDummy(type);
            if (newType != null) {
                newType.addReference(type.getOccurrence());
                type = newType;
            }
        }   
        super.resolveTypes(symbolTable);
    }
}
