package bluej.parser;

/**
 * A line/column location in a source file.
 *
 * Note that all line/column numbers start counting from
 * 1 (not 0).
 *
 * @author  Andrew Patterson
 * @version $Id: SourceLocation.java 2252 2003-11-04 12:50:05Z ajp $
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
     * Sets the line number of this location
     */
    public void setLine(int line)
    {
        if (line < 1)
            throw new IllegalArgumentException("line number must be greater than 0");

        this.line = line;
    }

    /**
     * Gets the line number of this location
     */
    public int getLine()
    {
        return line;
    }

    /**
     * Sets the column of this location
     * @param column
     */
    public void setColumn(int column)
    {
        if (column < 1)
            throw new IllegalArgumentException("column number must be greater than 0");

        this.column = column;
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
