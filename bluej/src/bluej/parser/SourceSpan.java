package bluej.parser;

/**
 * @author ajp
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SourceSpan
{
    private SourceLocation start;
    private SourceLocation end;
    
    public SourceSpan(SourceLocation start, SourceLocation end)
    {
        this.start = start;
        this.end = end;
    }
    
    /**
     * @param start
     * @param numChars
     */
    public SourceSpan(SourceLocation start, int numChars)
    {
        this.start = start;
        this.end = new SourceLocation(start.getLine(), start.getColumn() + numChars);
    }

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
