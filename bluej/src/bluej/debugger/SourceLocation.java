package bluej.debugger;

/**
 * This class a location in some source code
 *
 * @author  Michael Kolling
 * @version $Id: SourceLocation.java 1818 2003-04-10 13:31:55Z fisker $
 */
public final class SourceLocation
{
    private String classname;
    private String filename;
    private String methodname;
    private int lineNumber;

    public SourceLocation(String classname, String filename, 
                          String methodname, int lineNumber)
    {
        this.classname = classname;
        this.filename = filename;
        this.methodname = methodname;
        this.lineNumber = lineNumber;
    }

    public String getClassName()
    {
        return classname;
    }

    public String getFileName()
    {
        return filename;
    }

    public String getMethodName()
    {
        return methodname;
    }

    public int getLineNumber()
    {
        return lineNumber;
    }
    
    /**
     * Return the location in the format "<class>.<method>"
     */
    public String toString()
    {
        return classname + "." + methodname;
    }
}
