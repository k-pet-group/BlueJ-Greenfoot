package bluej.editor;

import java.awt.TextArea;
import java.awt.Font;

/**
 ** @version $Id: SimpleText.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** Frame to edit a single file.
 **/

public class SimpleText extends TextArea
{
	static int DEFAULT_WIDTH = 80;
	static int DEFAULT_HEIGHT = 25;

	static Font editorFont = new Font("SansSerif", Font.PLAIN, 12);

	public SimpleText(String initial_text)
	{
		super(initial_text, DEFAULT_HEIGHT, DEFAULT_WIDTH);

		setFont(editorFont);
	}
}
