package bluej.parser.symtab;

import bluej.parser.JavaToken;
import java.io.File;

/*******************************************************************************
 * An occurrence of an indentifier in a file 
 ******************************************************************************/
public class Selection extends Occurrence
{ 
    private int len;

    //==========================================================================
    //==  Methods
    //==========================================================================
    

    /** Constructor to define a new selection */
    public Selection(File file, int line, int column, int len)
    {
        super(file, line, column);

        this.len = len;
    }   

    public Selection(JavaToken tok)
    {
        super(tok.getFile(), tok.getLine(), tok.getColumn());
        this.len = tok.getText().length();
    }
    
    public Selection(Occurrence o, int len)
    {
        super(o.file, o.line, o.column);
        
        this.len = len;
    }

    public int getLine() { return line; }
    public int getColumn() { return column; }
    public int getLength() { return len; }
    
    /** return a string representation of the occurrence */
    public String getLocation() {
        return "[" + file + ":" + line + ":" + column + ":" + len + "]";
    }

    /** return a string representation of the occurrence */
    public String toString() {
        return "Selection " + getLocation();
    }
}
