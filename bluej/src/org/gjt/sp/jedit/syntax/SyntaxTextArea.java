/*
 * SyntaxTextArea.java - Enhanced text component
 * Copyright (C) 1998, 1999 Slava Pestov
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA.
 */

package org.gjt.sp.jedit.syntax;

import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

/**
 * An enhanced <code>JEditorPane</code> useful in text editors. It has
 * the following advantages over the standard Swing text components:
 * <ul>
 * <li>Uses the <code>SyntaxEditorKit</code> by default
 * <li>Implements line highlighting, where the line the caret is on
 * has a different background color
 * <li>Implements bracket highlighting, where if the caret is on a
 * bracket, the matching one is highlighted.
 * <li>Has an optional block caret
 * <li>Implements overwrite mode that is toggled by pressing the
 * Insert key
 * </ul>
 *
 * @author Slava Pestov
 * @version $Id: SyntaxTextArea.java 342 2000-01-12 03:18:00Z bruce $
 * @see org.gjt.sp.jedit.syntax.SyntaxEditorKit
 */
public class SyntaxTextArea extends JEditorPane
{
	/**
	 * The default editor kit for this text component.
	 */
	public static final EditorKit EDITOR_KIT = new SyntaxEditorKit();

	/**
	 * Creates a new SyntaxTextArea component.
	 */
	public SyntaxTextArea()
	{
		setCaret(new SyntaxCaret());
		
		setBorder(null);
		setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

		lineHighlightColor = new Color(0xe0e0e0);
		bracketHighlightColor = new Color(0x00ff00);
		lineSegment = new Segment();
		addCaretListener(new CaretHandler());

		if(!keymapInitialized)
		{
			Keymap map = JTextComponent.getKeymap(JTextComponent
				.DEFAULT_KEYMAP);
			map.setDefaultAction(new SyntaxTextArea
				.DefaultKeyTypedAction());
			map.addActionForKeyStroke(KeyStroke.getKeyStroke(
				KeyEvent.VK_INSERT,0),new SyntaxTextArea
				.InsertKeyAction());

			keymapInitialized = true;
		}
	}

	/**
	 * Returns the default editor kit for this text component.
	 */
	public EditorKit createDefaultEditorKit()
	{
		return EDITOR_KIT;
	}

	/**
	 * Sets the currently highlighted line.
	 * @param lineStart The start offset of the line in the document
	 * @param lineEnd The end offset of the line in the document
	 */
	public void setHighlightedLine(int lineStart, int lineEnd)
	{
		if(lineHighlightTag == null)
			return;

		try
		{
			getHighlighter().changeHighlight(lineHighlightTag,
				lineStart,lineEnd);
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
		}
	}

	/**
	 * Sets the line highlight color.
	 * @param color The line highlight color
	 */
	public void setLineHighlightColor(Color color)
	{
		lineHighlightColor = color;
	}

	/**
	 * Returns the line highlight color.
	 */
	public Color getLineHighlightColor()
	{
		return lineHighlightColor;
	}

	/**
	 * Sets the current line highlighting feature.
	 * @param lineHighlight True if the current line should be
	 * highlighted, false otherwise.
	 */
	public void setLineHighlight(boolean lineHighlight)
	{
		if(lineHighlightTag != null)
		{
			if(lineHighlight)
				return;
			else
			{
				getHighlighter().removeHighlight(lineHighlightTag);
				lineHighlightTag = null;
			}
		}
		else if(lineHighlight)
		{
			try
			{
				lineHighlightTag = getHighlighter().addHighlight(
					0,0,new CurrentLineHighlighter());
			}
			catch(BadLocationException bl)
			{
				bl.printStackTrace();
			}
		}
	}

	/**
	 * Returns true if current line highlighting is enabled, false
	 * otherwise.
	 */
	public boolean getLineHighlight()
	{
		return (lineHighlightTag != null);
	}

	/**
	 * Sets the highlighted bracket.
	 * @param bracketPos The offset of the bracket in the document
	 */
	public void setHighlightedBracket(int bracketPos)
	{
		if(bracketHighlightTag == null)
			return;

		if(bracketPos == lastBracket)
			return;

		lastBracket = bracketPos;

		try
		{
			if(bracketPos == -1)
				getHighlighter().changeHighlight(
					bracketHighlightTag,0,0);
			else
				getHighlighter().changeHighlight(
					bracketHighlightTag,bracketPos,
					bracketPos+1);
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
		}
	}

	/**
	 * Sets the bracket highlight color.
	 * @param color The bracket highlight color
	 */
	public void setBracketHighlightColor(Color color)
	{
		bracketHighlightColor = color;
	}

	/**
	 * Returns the bracket highlight color.
	 */
	public Color getBracketHighlightColor()
	{
		return bracketHighlightColor;
	}

	/**
	 * Sets the bracket highlighting feature.
	 * @param bracketHighlight True if matching brackets should be
	 * highlighted, false otherwise.
	 */
	public void setBracketHighlight(boolean bracketHighlight)
	{
		if(bracketHighlightTag != null)
		{
			if(bracketHighlight)
				return;
			else
			{
				getHighlighter().removeHighlight(bracketHighlightTag);
				bracketHighlightTag = null;
			}
		}
		else if(bracketHighlight)
		{
			try
			{
				bracketHighlightTag = getHighlighter()
					.addHighlight(0,0,new BracketHighlighter());
			}
			catch(BadLocationException bl)
			{
				bl.printStackTrace();
			}
		}
	}

	/**
	 * Returns true if bracket highlighting is enabled, false otherwise.
	 */
	public boolean getBracketHighlight()
	{
		return (bracketHighlightTag != null);
	}

	/**
	 * Sets the number of lines from the top or bottom of the
	 * text area from which autoscroll begins.
	 * @param lines The number of lines
	 */
	public void setElectricBorders(int lines)
	{
		electricLines = lines;
	}

	/**
	 * Returns the number of lines from the top or bottom of the
	 * text area from which autoscrolling begins.
	 */
	public int getElectricBorders()
	{
		return electricLines;
	}

	/**
	 * Sets the block caret.
	 * @param block True if a block caret should be drawn, false
	 * otherwise
	 */
	public void setBlockCaret(boolean block)
	{
		this.block = block;
	}

	/**
	 * Returns true if a block caret is enabled, false otherwise.
	 */
	public boolean getBlockCaret()
	{
		return block;
	}

	/**
	 * Sets the overwrite mode.
	 * @param overwrite True if newly inserted characters should
	 * overwrite existing ones, false if they should be inserted.
	 */
	public void setOverwrite(boolean overwrite)
	{
		this.overwrite = overwrite;
	}

	/**
	 * Returns true if overwrite mode is enabled, false otherwise.
	 */
	public boolean getOverwrite(boolean overwrite)
	{
		return overwrite;
	}

	/**
	 * Sets the overwrite mode flag to the opposite of it's
	 * current value.
	 */
	public void toggleOverwrite()
	{
		overwrite = !overwrite;
	}

	/**
	 * Updates the bracket and line highlighters.
	 */
	public void updateHighlighters()
	{
		Document doc = getDocument();
		int dot = getCaretPosition();
		Element map = doc.getDefaultRootElement();
		int lineNo = map.getElementIndex(dot);

		Element lineElement = map.getElement(lineNo);
		int start = lineElement.getStartOffset();
		int end = lineElement.getEndOffset();

		if(getSelectionStart() == getSelectionEnd())
		{
			if(lineNo != lastLine)
			{
				setHighlightedLine(start,end);
				lastLine = lineNo;
			}
		}
		
		try
		{
			if(dot != 0)
			{
				dot--;
				doc.getText(dot,1,lineSegment);
				char bracket = lineSegment.array
					[lineSegment.offset];
				int otherBracket;
				switch(bracket)
				{
				case '(':
					otherBracket = SyntaxUtilities.locateBracketForward(
						doc,dot,'(',')');
					break;
				case ')':
					otherBracket = SyntaxUtilities.locateBracketBackward(
						doc,dot,'(',')');
					break;
				case '[':
					otherBracket = SyntaxUtilities.locateBracketForward(
						doc,dot,'[',']');
					break;
				case ']':
					otherBracket = SyntaxUtilities.locateBracketBackward(
						doc,dot,'[',']');
					break;
				case '{':
					otherBracket = SyntaxUtilities.locateBracketForward(
						doc,dot,'{','}');
					break;
				case '}':
					otherBracket = SyntaxUtilities.locateBracketBackward(
						doc,dot,'{','}');
					break;
				default:
					otherBracket = -1;
					break;
				}
				setHighlightedBracket(otherBracket);
			}
			else
				setHighlightedBracket(-1);
		}
		catch(BadLocationException bl)
		{
			//bl.printStackTrace();
		}
	}

	/**
	 * Sets the document edited by this text area. This method
	 * makes sure that it implements the <code>SyntaxDocument</code>
	 * interface.
	 * @param doc The document
	 * @see org.gjt.sp.jedit.syntax.SyntaxDocument
	 */
	public void setDocument(Document doc)
	{
		if(doc instanceof SyntaxDocument)
			super.setDocument(doc);
		else
			throw new IllegalArgumentException("Document is not"
				+ " an instance of SyntaxDocument");
	}

	/**
	 * Sets the content of this text component. This implementation
	 * doesn't create a new document (therefore the token marker, etc
	 * is preserved) unlike the default <code>JEditorPane</code>
	 * implementation.
	 */
	public void setText(String text)
	{
		Document doc = getDocument();
		try
		{
			doc.remove(0,doc.getLength());
			doc.insertString(0,text,null);
		}
		catch(BadLocationException bl)
		{
			throw new InternalError("setText() fuckup");
		}
	}

	// delegates to SyntaxDocument

	/**
	 * Returns the text area's document, typecast to a
	 * <code>SyntaxDocument</code>.
	 * @see org.gjt.sp.jedit.syntax.SyntaxDocument
	 */
	public SyntaxDocument getSyntaxDocument()
	{
		return (SyntaxDocument)getDocument();
	}

	/**
	 * Returns the token marker that is to be used to split lines
	 * of this document up into tokens. May return null if this
	 * document is not to be colorized. This simply delegates to
	 * the text area's document.
	 */
	public TokenMarker getTokenMarker()
	{
		return getSyntaxDocument().getTokenMarker();
	}

	/**
	 * Sets the token marker that is to be used to split lines of
	 * this document up into tokens. May throw an exception if
	 * this is not supported for this type of document. This simply
	 * delegates to the text area's document.
	 * @param tm The new token marker
	 */
	public void setTokenMarker(TokenMarker tm)
	{
		getSyntaxDocument().setTokenMarker(tm);
	}

	/**
	 * Returns the color array that maps token identifiers to
	 * <code>java.awt.Color</code> objects. Each index in the
	 * array is a token type. This simply delegates to the
	 * text area's document.
	 */
	public Color[] getColors()
	{
		return getSyntaxDocument().getColors();
	}

	/**
	 * Sets the color array that maps token identifiers to
	 * <code>java.awt.Color</code> ojects. May throw an exception
	 * if this is not supported for this type of document. This
	 * simply delegates to the text area's document.
	 * @param colors The new color list
	 */
	public void setColors(Color[] colors)
	{
		getSyntaxDocument().setColors(colors);
	}

	/**
	 * Reparses the document, by passing all lines to the token
	 * marker. This should be called after the document is first
	 * loaded. This simply delegates to the text area's document.
	 */
	public void tokenizeLines()
	{
		getSyntaxDocument().tokenizeLines();
	}

	/**
	 * Reparses the document, by passing the specified lines to the
	 * token marker. This should be called after a large quantity of
	 * text is first inserted. This simply delegates to the text
	 * area's document.
	 * @param start The first line to parse
	 * @param len The number of lines, after the first one to parse
	 */
	public void tokenizeLines(int start, int len)
	{
		getSyntaxDocument().tokenizeLines(start,len);
	}

	/**
	 * This method is public because of a Java language limitation.
	 */
	public void doElectricScroll(Rectangle rect)
	{
		SwingUtilities.invokeLater(new SyntaxSafeScroller(rect));
	}

	// private members
	private Color lineHighlightColor;
	private Object lineHighlightTag;
	private Color bracketHighlightColor;
	private Object bracketHighlightTag;
	private int electricLines;
	private boolean block;
	private boolean overwrite;
	private Segment lineSegment;
	private int lastLine = -1;
	private int lastBracket = -1;

	private static boolean keymapInitialized;

	private void _replaceSelection(String content)
	{
		if(!overwrite || getSelectionStart() != getSelectionEnd())
		{
			replaceSelection(content);
			return;
		}

		int caret = getCaretPosition();
		Document doc = getDocument();
		Element map = doc.getDefaultRootElement();
		Element line = map.getElement(map.getElementIndex(caret));

		if(line.getEndOffset() - caret <= content.length())
		{
			replaceSelection(content);
			return;
		}

		try
		{
			doc.remove(caret,content.length());
			doc.insertString(caret,content,null);
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
		}
	}
		
	static class DefaultKeyTypedAction extends TextAction
	{
		public DefaultKeyTypedAction()
		{
			super("syntax-default-key-typed-action");
		}

		public void actionPerformed(ActionEvent evt)
		{
			JTextComponent comp = getTextComponent(evt);
			String content = evt.getActionCommand();
			int modifiers = evt.getModifiers();

			if(content != null && content.length() != 0
				&& (modifiers & ActionEvent.ALT_MASK) == 0)
			{
				char c = content.charAt(0);
				if ((c >= 0x20) && (c != 0x7F))
				{
					if(comp instanceof SyntaxTextArea)
						((SyntaxTextArea)comp)._replaceSelection(content);
					else
						comp.replaceSelection(content);
				}
			}
		}
	}

	static class InsertKeyAction extends TextAction
	{
		public InsertKeyAction()
		{
			super("insert-key");
		}

		public void actionPerformed(ActionEvent evt)
		{
			JComponent comp = getTextComponent(evt);
			if(comp instanceof SyntaxTextArea)
			{
				((SyntaxTextArea)comp).toggleOverwrite();
				comp.repaint(); // to repaint caret
			}
		}
	}

	class SyntaxCaret extends DefaultCaret
	{
		public void focusGained(FocusEvent evt)
		{
			/* we do this even if the text area is read only,
			 * otherwise stuff like line and bracket highlighting
			 * will look weird without a caret */
			SyntaxCaret.this.setVisible(true);
		}

		public void adjustVisibility(Rectangle rect)
		{
			doElectricScroll(rect);
		}

		public void damage(Rectangle r)
		{
			if(r != null)
			{
				x = r.x;
				y = r.y;
				height = r.height + 2;
				SyntaxCaret.this.repaint();
			}
		}
				
		public void paint(Graphics g)
		{
			if(getDot() != getMark() ||
				!SyntaxCaret.this.isVisible())
				return;
			try
			{
				int dot = getDot();
				Rectangle r = modelToView(dot);
				width = g.getFontMetrics().charWidth('m');
				r.width = (overwrite || block) ? width - 1 : 0;
				width += 2;

				if(overwrite)
				{
					r.y += r.height - 1;
					r.height = 1;
				}

				g.setColor(getCaretColor());
				g.drawRect(r.x,r.y,r.width,r.height - 1);
			}
			catch(BadLocationException bl)
			{
				System.out.println("Caret fuckup:");
				bl.printStackTrace();
			}
		}
	}

	class SyntaxSafeScroller implements Runnable
	{
		public Rectangle rect;

		public SyntaxSafeScroller(Rectangle rect)
		{
			this.rect = rect;
		}

		public void run()
		{
			int height = getFontMetrics(
				getFont()).getHeight();
			int y = Math.max(0,rect.y - height * electricLines);
			int lines = height * electricLines * 2;
			if(y + lines + rect.height <= getHeight())
			{
				rect.y = y;
				rect.height += lines;
			}
			scrollRectToVisible(rect);
		}
	}
			
	class CurrentLineHighlighter implements Highlighter.HighlightPainter
	{
		public void paint(Graphics g, int p0, int p1, Shape bounds,
			JTextComponent textComponent)
		{
			if(lineHighlightTag == null || getSelectionStart()
				!= getSelectionEnd())
				return;
			FontMetrics metrics = g.getFontMetrics();
			Document doc = getDocument();
			int lineNo = doc.getDefaultRootElement()
				.getElementIndex(p0);
			Rectangle rect = (Rectangle)bounds;
			int height = metrics.getHeight();
			int x = rect.x;
			int y = rect.y + height * lineNo;
			g.setColor(lineHighlightColor);
			g.fillRect(x,y,rect.width,height);
		}
	}

	class BracketHighlighter implements Highlighter.HighlightPainter
	{
		public void paint(Graphics g, int p0, int p1, Shape bounds,
			JTextComponent textComponent)
		{
			if(getSelectionStart() != getSelectionEnd()
				|| bracketHighlightTag == null)
				return;
			if(p0 == p1)
				return;
			Rectangle bracket;
			Document doc = getDocument();
			FontMetrics metrics = g.getFontMetrics();
			try
			{
				bracket = modelToView(p0);
				doc.getText(p0,1,lineSegment);
				bracket.width += metrics.charWidth(lineSegment
					.array[lineSegment.offset]);
			}
			catch(BadLocationException bl)
			{
				bl.printStackTrace();
				return;
			}
			g.setColor(bracketHighlightColor);
			g.fillRect(bracket.x,bracket.y,
				bracket.width,bracket.height);
		}
	}

	class CaretHandler implements CaretListener
	{
		public void caretUpdate(CaretEvent evt)
		{
			updateHighlighters();
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.1  2000/01/12 03:17:59  bruce
 *
 * Addition of Syntax Colour Highlighting Package to CVS tree.  This is LGPL code used in the Moe Editor to provide syntax highlighting.
 *
 * Revision 1.28  1999/06/05 00:22:58  sp
 * LGPL'd syntax package
 *
 * Revision 1.27  1999/05/22 08:33:53  sp
 * FAQ updates, mode selection tweak, patch mode update, javadoc updates, JDK 1.1.8 fix
 *
 * Revision 1.26  1999/05/15 00:29:19  sp
 * Prev error bug fix, doc updates, tips updates
 *
 * Revision 1.25  1999/05/11 09:05:10  sp
 * New version1.6.html file, some other stuff perhaps
 *
 * Revision 1.24  1999/05/07 06:15:43  sp
 * Resource loading update, fix for abstract Plugin classes in JARs
 *
 * Revision 1.23  1999/05/06 05:16:17  sp
 * Syntax text are compile fix, FAQ updated
 *
 * Revision 1.22  1999/05/03 08:28:14  sp
 * Documentation updates, key binding editor, syntax text area bug fix
 *
 * Revision 1.21  1999/05/03 04:28:01  sp
 * Syntax colorizing bug fixing, console bug fix for Swing 1.1.1
 *
 * Revision 1.20  1999/05/02 00:07:21  sp
 * Syntax system tweaks, console bugfix for Swing 1.1.1
 *
 * Revision 1.19  1999/04/28 04:10:40  sp
 * Overwrite/overstrike mode
 *
 * Revision 1.18  1999/04/22 05:31:17  sp
 * Documentation updates, minor SyntaxTextArea update
 *
 * Revision 1.17  1999/04/19 05:38:20  sp
 * Syntax API changes
 *
 */
