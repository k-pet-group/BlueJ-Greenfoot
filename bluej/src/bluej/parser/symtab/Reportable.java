
package bluej.parser.symtab;

/*******************************************************************************
 * This interface is used as a handle to all classes that can be reported 
 ******************************************************************************/
interface Reportable {

    /** Collect information about an object */
    void getInfo(ClassInfo info, SymbolTable symbolTable);
}
