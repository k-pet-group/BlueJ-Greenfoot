
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

    /** The comment attached to this variable definition */
    private String comment;

    /** The type of the variable */
    private Definition type = null;

    /* Indicate if this was an array variable (>0) and if so, how many
       levels of nesting */
    private int arrayLevel = 0;
    
    /* need to track whther this represents a vararg parameter */
    private boolean isVarargs = false;

    //==========================================================================
    //==  Methods
    //==========================================================================


    /** Constructor to create a new variable symbol */
    VariableDef(String name,               // the variable's name
                Occurrence occ,            // where it was defined
                ClassDef type,             // the type of the variable
                boolean isVarargs,         // is it a vararg?
                int arrayLevel,            // level of array nesting
                ScopedDef parentScope) {   // which scope owns it
        super(name, occ, parentScope);
        this.type = type;
        this.isVarargs = isVarargs;
        this.arrayLevel = arrayLevel;
    }


    /** get the type of the variable */
    public Definition getType() {
        return type;
    }

    public int getArrayLevel() {
        return arrayLevel;
    }
    /** Collect information about this variable */
    public void getInfo(ClassInfo info, SymbolTable symbolTable)
    {
        if (getParentScope() instanceof ClassDef)
            info.addComment(getName(), comment, null);

        info.addUsed(type.getQualifiedName());
    }

    public void setComment(String comment) {
        this.comment = comment;
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
    
    
    /**
     * @return boolean representing whether it is a vararg parameter of a 
     * method (Java 1.5 feature)
     */
    public boolean isVarargs()
    {
        return this.isVarargs;
    }
    
}
