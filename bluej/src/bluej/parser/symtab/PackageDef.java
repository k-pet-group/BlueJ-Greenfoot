
package bluej.parser.symtab;

/*******************************************************************************
 * Definition of a package. 
 ******************************************************************************/
class PackageDef extends ScopedDef       
{
    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /** Constructor to create a package object */
    PackageDef(String name,                // the name of the package
               Occurrence occ,             // where it was defined (NULL)
               ScopedDef parentScope) {    // which scope owns it
        super(name, occ, parentScope);
    }   


    /**  */
    public void getInfo(ClassInfo info, SymbolTable symbolTable) {
        if (hasElements())
            getElementInfo(info, symbolTable);
    }   
}
