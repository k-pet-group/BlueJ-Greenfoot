
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
    private File file; 
    
    /** The line number containing the occurrence */
    private int line; 


    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /** Constructor to define a new occurrence */
    Occurrence(File file, int line) {
        this.file = file;
        this.line = line;
    }   


    /** return a string representation of the occurrence */
    public String getLocation() {
        return "[" + file + ":" + line + "]";
    }


    /**   */
    public void getInfo(ClassInfo info, SymbolTable symbolTable) {
    }   


    /** return a string representation of the occurrence */
    public String toString() {
        return "Occurrence [" + file + "," + line + "]";
    }
}
