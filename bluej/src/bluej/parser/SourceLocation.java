package bluej.parser;

/**
 * @author ajp
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SourceLocation
{
    private int line;
    private int column;
    
    public SourceLocation(int line, int column)
    {
        this.line = line;
        this.column = column;
    }

    /**
     * Sets the line number of this location
     */
    public void setLine(int line)
    {
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
