package bluej.views;

import bluej.editor.Editor;
import bluej.utility.Utility;

import java.awt.Label;

/**
 ** @version $Id: EditorPrintWriter.java 187 1999-07-17 02:32:38Z ajp $
 ** @author Michael Cahill
 **
 ** EditorPrintWriter - ignore any formatting in a FormattedPrintWriter
 **/
public class EditorPrintWriter extends FormattedPrintWriter
{
	private Editor editor;
	private boolean bold = false;
        private boolean italic = false;
	
	public EditorPrintWriter(Editor editor)
	{
		// PrintWriter needs to be passed a valid outputstream
		// even if we are going to not actually print to it.
		// We pass it the standard System output stream
		super(System.out);

		this.editor = editor;
	}
	
	protected void startBold()
	{
		bold = true;
	}
	
	protected void endBold()
	{
		bold = false;
	}
	
	protected void startItalic()
	{
		italic = true;
	}

	protected void endItalic()
	{
		italic = false;
	}

	protected void indentLine()
	{
		editor.insertText("    ", bold, italic);
	}
	
	public void println(String str)
	{
		editor.insertText(str + "\n", bold, italic);
	}
}
