package bluej.parser;

/**
 * A line/column location in a source file.
 *
 * Note that all line/column numbers start counting from
 * 1 (not 0).
 *
 * @author  Andrew Patterson
 * @version $Id: SourceLocation.java 3573 2005-09-19 02:21:52Z davmac $
 */
public class SourceLocation
{
    private int line;
    private int column;
    
    public SourceLocation(int line, int column)
    {
        if (line < 1 || column < 1)
            throw new IllegalArgumentException("line/column numbers must be greater than 0");

        this.line = line;
        this.column = column;
    }

    /**
     * Gets the line number of this location
     */
    public int getLine()
    {
        return line;
    }

    /**
     * gets the column where this node reside
     * @return <code>int</code>
     */
    public int getColumn()
    {
        return column;
    }

    public String toString()
    {   
        return "<" + line + "," + column + ">";
    }
}
