package bluej.views;

import bluej.editor.Editor;
import bluej.utility.Utility;

import java.awt.Label;

/**
 ** @version $Id: HTMLPrintWriter.java 156 1999-07-06 14:37:16Z ajp $
 ** @author Andrew Patterson
 **
 ** HTMLPrintWriter - generate a crude HTML version of a FormattedPrintWriter
 **/
public class HTMLPrintWriter extends FormattedPrintWriter
{
	StringBuffer html;

	public HTMLPrintWriter()
	{
		// PrintWriter needs to be passed a valid outputstream
		// even if we are going to not actually print to it.
		// We pass it the standard System output stream
		super(System.out);
	}

	public String getHTML()
	{
		return html.toString();
	}

	protected void startBold()
	{
		html.append("<strong>");
	}
	
	protected void endBold()
	{
		html.append("</strong>");
	}
	
	protected void startItalic()
	{
		html.append("<i>");
	}

	protected void endItalic()
	{
		html.append("</i>");
	}

	protected void indentLine()
	{
		html.append(" &nbsp;&nbsp ");
	}

	public void println(String str)
	{
		html.append(str.trim() + "<BR>");
	}
}
