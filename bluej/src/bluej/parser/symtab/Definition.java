
package bluej.parser.symtab;

/*******************************************************************************
 * This abstract class represents a symbol definition in a Java source file.
 * All symbols used in our Java symbol table stem from this definition.
 ******************************************************************************/
abstract class Definition implements Reportable
{ 
    //==========================================================================
    //==  Class Variables
    //==========================================================================
    
    /** A file location where the item was defined */
    private Occurrence definition;
    
    /** The scope that contains this symbol */
    private ScopedDef parentScope;
    
    /** A list of references to this symbol */
    private JavaVector references;
    
    /** The name of the symbol */
    private String name;


    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /** Constructor for the base of a symbol definition */
    Definition(String name,              // the symbol name
               Occurrence occ,           // the location of its definition
               ScopedDef parentScope) {  // scope containing the def
        this.definition  = occ;
        this.parentScope = parentScope;
        this.name        = name;
        
        // create a new vector to keep track of references to this symbol
        this.references  = new JavaVector();
    }   


    /** Add a location of a reference to the symbol to our reference list */
    void addReference(Occurrence occ) {
        references.addElement(occ);
    }   


    /** Get a String representation of the location where this symbol
     *  was defined
     */
    String getDef() {
        if (definition != null)
            return definition.getLocation();
        else
            return "";
    }   


    /** Get the basic name of the symbol */
    String getName() {
        if (name == null)
            return "~NO NAME~";
        else
            return name;
    }   


    /** Get the information about where the symbol was defined */
    Occurrence getOccurrence() {
        return definition;
    }


    /** Get the symbol that contains the definition of this symbol */
    ScopedDef getParentScope() {
        return parentScope;
    }


    /** Get the fully-qualified name of the symbol
     *  Keep building the name by recursively calling the parentScope's
     *    getQualifiedName() method...
     */
    String getQualifiedName() {
        String nameToUse = name;
        if (name == null)
            nameToUse = "~NO NAME~";

        if (getParentScope() != null &&
             !getParentScope().isDefaultOrBaseScope())
            return getParentScope().getQualifiedName() + "." + nameToUse;
        else
            return nameToUse;
    }   


    /** Determine if this symbol represents a class that is a superclass of
     *  another symbol.  For most symbols, this is false (because most symbols
     *  are not classes...).
     *  This method will be overridden for classes and interfaces.
     */
    boolean isSuperClassOf(Definition def) {
        return false;
    }   


    /** The "default" lookup routine.  This is used to search for a name within
     *  the scope of another symbol.  This version of the lookup method is a
     *  convenience that just passes -1 as the parameter count (meaning
     *  look the name up as a non-method symbol
     */
    Definition lookup(String name) {
        return lookup(name, -1);
    }   


    /** Lookup a method in our scope.  Because this is only a valid
     *  operation for scoped definitions, we default this to throw
     *  an exception that states so
     */
    Definition lookup(String name, int numParams) {
        throw new IllegalArgumentException("Can't lookup in a "+getClass());
    }   


    /** Collect information from this component. */
    public void getInfo(ClassInfo info, SymbolTable symbolTable) {}


    /** This method resolves any references to other symbols.
     *  At this level there is nothing to resolve, so do nothing.
     */
    void resolveTypes(SymbolTable symbolTable) {
    }   


    /** Set a reference to the symbol that syntactically
     *  contains this symbol.
     */
    void setParentScope(ScopedDef parentScope) {
        this.parentScope = parentScope;
    }   


    /** return a String representation of this class for printing
     *  Note that this version of toString() is used by nearly all of
     *    the subclasses of Definition.
     */
    public String toString() {
        return getClass().getName() + " [" + getQualifiedName() + "]";
    }   
}
