package bluej.views;

import bluej.editor.Editor;
import bluej.utility.Utility;

import java.awt.Label;

/**
 ** @version $Id: EditorPrintWriter.java 36 1999-04-27 04:04:54Z mik $
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

	
	public void println(String str)
	{
		editor.insertText(str + "\n", bold, italic);
	}
}
