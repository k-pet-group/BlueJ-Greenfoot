package bluej.views;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 ** @version $Id: FormattedPrintWriter.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 **
 ** FormattedPrintWriter - provides formatting on top of a PrintWriter
 **/
public abstract class FormattedPrintWriter extends PrintWriter
{
	public FormattedPrintWriter()
	{
		super((Writer)null);
	}
	
	public FormattedPrintWriter(Writer out)
	{
		super(out);
	}
	
	public FormattedPrintWriter(Writer out, boolean autoFlush)
	{
		super(out, autoFlush);
	}
	
	public FormattedPrintWriter(OutputStream out)
	{
		super(out);
	}
	
	public FormattedPrintWriter(OutputStream out, boolean autoFlush)
	{
		super(out, autoFlush);
	}
	
	boolean bold = false;
	public void setBold(boolean bold)
	{
		if(this.bold == bold)
			return;	// nothing to do

		if(bold)
			startBold();
		else
			endBold();
			
		this.bold = bold;
	}
	protected abstract void startBold();
	protected abstract void endBold();
	
	boolean italic = false;
	public void setItalic(boolean italic)
	{
		if(this.italic == italic)
			return;	// nothing to do

		if(italic)
			startItalic();
		else
			endItalic();
			
		this.italic = italic;
	}
	protected abstract void startItalic();
	protected abstract void endItalic();
}
