
package bluej.parser.symtab;

import java.io.File;

/*******************************************************************************
 * An occurrence of an indentifier in a file 
 ******************************************************************************/
class Occurrence implements Reportable     
{ 
    //==========================================================================
    //==  Class Variables
    //==========================================================================
    
    /** The file containing the occurrence */
    protected File file; 
    
    /** The line number containing the occurrence */
    protected int line;
    protected int column; 


    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /** Constructor to define a new occurrence */
    Occurrence(File file, int line, int column) {
        this.file = file;
        this.line = line;
        this.column = column;
    }   


    /** return a string representation of the occurrence */
    public String getLocation() {
        return "[" + file + ":" + line + ":" + column + "]";
    }


    /**   */
    public void getInfo(ClassInfo info, SymbolTable symbolTable) {
    }   


    /** return a string representation of the occurrence */
    public String toString() {
        return "Occurrence " + getLocation();
    }
}
