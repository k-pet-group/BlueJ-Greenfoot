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
    //public void setLength(int length) { len = length; }
    public File getFile() { return file; }
    
    /**
     * The idea is that to assist in adding type parameters where applicable
     * this Selection might represent more than one token. Tokens to be added 
     * need to be on the same line (at the moment) or are ignored. Any gaps between
     * the existing token and one added are assumed to be whitespace. 
     *
     * @param token JavaToken to be added. Must not be null and should be on same 
     * line as existing Selection.
     * 
     */
    public void addToken(JavaToken token)
    {
        //check if on the same line
        if(line == token.getLine()){
            StringBuffer buf = new StringBuffer(origText);
            int endOfExisting = column + len;
            int gap = token.getColumn() - endOfExisting;
            // if the added token is not already part of the Selection 
            // add whitespace up to the beginning of new token
            if(gap > 0){
                for (int i = 0; i < gap; i++){
                    buf.append(' ');
                }
                buf.append(token.getText());               
            }
            // Token is a part of the Selection already, replace that 
            // part of the selection. This may occur if you add a 
            // number of tokens in a non-sequential order. 
            else {
                String text = token.getText();
                buf.replace(token.getColumn() - column, token.getColumn() - column + text.length(), text);
            }
            origText = buf.toString();
            len = origText.length();
        }
    }

    /** return a string representation of the occurrence */
    public String getLocation() {
        return "[" + file + ":" + line + ":" + column + ":" + len + "]";
    }

    /** return a string representation of the occurrence */
    public String toString() {
        return "Selection " + getLocation();
    }
}
