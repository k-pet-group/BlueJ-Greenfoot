package bluej.debugger;

import bluej.utility.Debug;

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
    private String sourceFile;
    private int lineNumber;

    public ExceptionDescription(String className, String text, 
				String sourceFile, int lineNumber)
    {
	this.className = className;
	this.text = text;
	this.sourceFile = sourceFile;
	this.lineNumber = lineNumber;
    }

    public ExceptionDescription(String text)
    {
	this.className = null;
	this.text = text;
	this.sourceFile = null;
	this.lineNumber = 0;
    }

    public String getClassName()
    {
	return className;
    }

    public String getText()
    {
	return text;
    }

    public String getSourceFile()
    {
	return sourceFile;
    }

    public int getLineNumber()
    {
	return lineNumber;
    }

    public String toString()
    {
	return className + ": " + text + "\n  at " + sourceFile + 
	    ":" + lineNumber;
    }

    /**
     * After catching an exception, try to pick the text apart to find the
     * file and the line number. The message is in a format such as:
     * <exception description>
     *    at <class>.<method>(<filename>:<line>)
     *    at ...<stack trace>
     * We try to get the exception description, filename and line number,
     * and then use these to report the error properly.
     */
//      private void analyseExceptionText(String text)
//      {
//  	String msg;

//  	// We assume that the first line is the exception text
//  	int lineBreak = text.indexOf("\n");
//  	if(lineBreak == -1)
//  	    msg = text;
//  	else
//  	    msg = text.substring(0, text.indexOf("\n"));

//  	// now we search for text in the form "(___:__)"

//  	int startPos = text.indexOf('(');
//  	int colonPos = text.indexOf(':', startPos);
//    	int endPos = text.indexOf(')', startPos);
//  	colonPos = text.lastIndexOf(':', endPos);
//  	startPos = text.lastIndexOf('(', colonPos);
//    	if(startPos == -1 || endPos == -1 || colonPos == -1) {
//  	    // could not parse the message - use general report dialog
//    	    pkg.reportException(msg);
//    	    return;
//    	}

//  	String fileName = text.substring(startPos+1, colonPos);
//    	String line = text.substring(colonPos+1, endPos);
//    	int lineNum = Integer.parseInt(line);

//    	pkg.errorMessage(pkg.getFileName(fileName), lineNum, msg, false);
//      }


}
