package bluej.compiler;

/**
 ** @version $Id: CompilerMessageError.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** CompilerMessageError - thrown when there is a compiler error.
 **/

public class CompilerMessageError extends Error
{
	String filename;
	int lineNo;
	String message;
	
	public CompilerMessageError(String filename, int lineNo, String message)
	{
		super(filename + ":" + lineNo + ":" + message);
		
		this.filename = filename;
		this.lineNo = lineNo;
		this.message = message;
	}
	
	public String getFilename()
	{
		return filename;
	}
	
	public int getLineNo()
	{
		return lineNo;
	}
	
	public String getMessage()
	{
		return message;
	}
}
