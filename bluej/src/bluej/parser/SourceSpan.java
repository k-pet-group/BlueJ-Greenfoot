package bluej.parser;

/**
 * A span between two line/column locations.
 *
 * @author  Andrew Patterson
 * @version $Id: SourceSpan.java 2252 2003-11-04 12:50:05Z ajp $
 */
public class SourceSpan
{
    private SourceLocation start;
    private SourceLocation end;
    
	/**
	 * @param start  the line/column location where the span starts
	 * @param end	 the line/column location where the span ends
	 */
    public SourceSpan(SourceLocation start, SourceLocation end)
    {
        this.start = start;
        this.end = end;
    }
    
    /**
     * @param start    the line/column location where the span starts
     * @param numChars the number of characters (assumes span is only on one line)
     */
    public SourceSpan(SourceLocation start, int numChars)
    {
        this.start = start;
        this.end = new SourceLocation(start.getLine(), start.getColumn() + numChars);
    }

    /**
     * Determine if a span crosses more that one line.
     * 
     * @return  true if the span is only on one line.
     */
    public boolean isOneLine()
    {
        return (start.getLine() == end.getLine() );
    }
    
    /**
     * @return
     */
    public SourceLocation getStartLocation()
    {
        return start;
    }

    public int getStartColumn()
    {
        return start.getColumn();
    }

    public int getStartLine()
    {
        return start.getLine();
    }

    public SourceLocation getEndLocation()
    {
        return end;
    }

    public int getEndColumn()
    {
        return end.getColumn();
    }

    public int getEndLine()
    {
        return end.getLine();
    }
    
    public String toString()
    {
        return start.toString() + "-" + end.toString();
    }
}
