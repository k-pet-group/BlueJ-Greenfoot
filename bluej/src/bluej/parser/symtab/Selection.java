package bluej.parser.symtab;

import bluej.parser.JavaToken;
import java.io.File;

/*******************************************************************************
 * An occurrence of an indentifier in a file
 ******************************************************************************/
public class Selection extends Occurrence
{
    private int len;
    private String origText;

    //==========================================================================
    //==  Methods
    //==========================================================================


    /** Constructor to define an empty selection */
    public Selection(File f, int line, int column)
    {
        super(f, line, column);

        this.len = 0;
        this.origText = "";
    }

    /**
     * Constructor to define a selection representing a token
     *  in the source file
     */
    public Selection(JavaToken tok)
    {
        super(tok.getFile(), tok.getLine(), tok.getColumn());

        this.len = tok.getText().length();
        this.origText = new String(tok.getText());
    }

/*    public Selection(Occurrence o, int len)
    {
        super(o.file, o.line, o.column);

        this.len = len;
    } */

    public int getLine() { return line; }
    public int getColumn() { return column; }
    public int getLength() { return len; }
    public String getText() { return origText; }

    /** return a string representation of the occurrence */
    public String getLocation() {
        return "[" + file + ":" + line + ":" + column + ":" + len + "]";
    }

    /** return a string representation of the occurrence */
    public String toString() {
        return "Selection " + getLocation();
    }
}
