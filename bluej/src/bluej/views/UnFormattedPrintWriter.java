package bluej.views;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 ** @version $Id: UnFormattedPrintWriter.java 156 1999-07-06 14:37:16Z ajp $
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
	protected void indentLine() {}
}
