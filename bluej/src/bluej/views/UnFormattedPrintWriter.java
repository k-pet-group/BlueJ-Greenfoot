package bluej.views;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 ** @version $Id: UnFormattedPrintWriter.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 **
 ** UnFormattedPrintWriter - ignore any formatting in a FormattedPrintWriter
 **/
public class UnFormattedPrintWriter extends FormattedPrintWriter
{
	public UnFormattedPrintWriter(Writer out)
	{
		super(out);
	}
	
	public UnFormattedPrintWriter(Writer out, boolean autoFlush)
	{
		super(out, autoFlush);
	}
	
	public UnFormattedPrintWriter(OutputStream out)
	{
		super(out);
	}
	
	public UnFormattedPrintWriter(OutputStream out, boolean autoFlush)
	{
		super(out, autoFlush);
	}
	
	protected void startBold() {}
	protected void endBold() {}
	protected void startItalic() {}
	protected void endItalic() {}
}
