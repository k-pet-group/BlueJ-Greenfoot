
package bluej.parser;


import java.io.File;

/*******************************************************************************
 * A simple token that is used to relay information from the scanner to
 * the parser.  We've extended it to save information about the file from
 * which the token was created, and the number of parameters (telling if the
 * symbol looked like a method invocation or some other symbol reference.)
 ******************************************************************************/
public class JavaToken extends antlr.CommonToken {
    //==========================================================================
    //==  Class Variables
    //==========================================================================
    
    /** A count of the parameters used to call a method.
     *  -1 means the symbol is not a method invocation
     */
    private int paramCount = -1;
    
    /** A reference to the File that was scanned to create this symbol */
    private File file = null;


    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /** get the File that contained the text scanned for this token */
    public File getFile() {
        return file;
    }       


    /** get the number of parameters for this token (if it represents a 
     *  method invocation 
     */
    public int getParamCount() {
        return paramCount;
    }


    /** Sets the file property of this token */
    public void setFile(File file) {
        this.file = file;
    }


    /** Sets the parameter count property of this token */
    public void setParamCount(int count) {
        paramCount = count;
    }
}
