package bluej.debugger;

import bluej.utility.Debug;

import java.util.List;

/**
 ** 
 ** 
 **
 ** @author Michael Kolling
 **/

public final class ExceptionDescription
{
    private String className;
    private String text;
    private List stack;

    public ExceptionDescription(String className, String text, 
                                List stack)
    {
	this.className = className;
	this.text = text;
        this.stack = stack;
    }

    public ExceptionDescription(String text)
    {
	this.className = null;
	this.text = text;
        this.stack = null;
    }

    /**
     * Return the name of the exception class.
     */
    public String getClassName()
    {
	return className;
    }

    /**
     * Return the text of the exception.
     */    
    public String getText()
    {
	return text;
    }

    /**
     * Return the file the exception was thrown from.
     */    
//     public String getSourceFile()
//     {
// 	return sourceFile;
//     }

//     /**
//      * Return the line number in the source file where this exception was
//      * thrown.
//      */
//     public int getLineNumber()
//     {
// 	return lineNumber;
//     }

    /**
     * Return the stack (a list of SourceLocation objects) for the exception
     * location. Element 0 in the list is the current frame, higher numbers
     * are caller frames.
     */
    public List getStack()
    {
	return stack;
    }

    public String toString()
    {
	return className + ": " + text;
    }

}
