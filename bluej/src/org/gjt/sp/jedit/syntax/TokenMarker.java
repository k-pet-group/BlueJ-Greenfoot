/*
 * TokenMarker.java - Generic token marker
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

import javax.swing.text.Segment;

/**
 * A token marker that splits lines of text into tokens. Each token carries
 * a length field and an indentification tag that can be mapped to a color
 * for painting that token.<p>
 *
 * For performance reasons, the linked list of tokens is reused after each
 * line is tokenized. Therefore, the return value of <code>markTokens</code>
 * should only be used for immediate painting. Notably, it cannot be
 * cached.
 *
 * @author Slava Pestov
 * @version $Id: TokenMarker.java 1819 2003-04-10 13:47:50Z fisker $
 *
 * @see org.gjt.sp.jedit.syntax.Token
 */
public abstract class TokenMarker
{
	/**
	 * A wrapper for the lower-level <code>markTokensImpl</code> method
	 * that is called to split a line up into tokens.
	 * @param line The line
	 * @param lineIndex The line number
	 */
	public Token markTokens(Segment line, int lineIndex)
	{
		lastToken = null;

		LineInfo info = lineInfo[lineIndex];
		byte oldToken = info.token;

		byte token = markTokensImpl((lineIndex == 0 ?
			Token.NULL : lineInfo[lineIndex -1].token),
			line,lineIndex);

		info.token = token;
		nextLineRequested = (oldToken != token);

		addToken(0,Token.END);

		return firstToken;
	}

	/**
	 * An abstract method that splits a line up into tokens. It
	 * should parse the line, and call <code>addToken()</code> to
	 * add syntax tokens to the token list. Then, it should return
	 * the initial token type for the next line.<p>
	 *
	 * For example if the current line contains the start of a 
	 * multiline comment that doesn't end on that line, this method
	 * should return the comment token type so that it continues on
	 * the next line.
	 *
	 * @param token The initial token type for this line
	 * @param line The line to be tokenized
	 * @param lineIndex The index of the line in the document,
	 * starting at 0
	 * @return The initial token type for the next line
	 */
	protected abstract byte markTokensImpl(byte token, Segment line,
		int lineIndex);

	/**
	 * Informs the token marker that lines have been inserted into
	 * the document. This inserts a gap in the <code>lineInfo</code>
	 * array.
	 * @param index The first line number
	 * @param lines The number of lines 
	 */
	public void insertLines(int index, int lines)
	{
		if(lines <= 0)
			return;
		length += lines;
		ensureCapacity(length);
		int len = index + lines;
		System.arraycopy(lineInfo,index,lineInfo,len,
			lineInfo.length - len);

		for(int i = index + lines - 1; i >= index; i--)
			lineInfo[i] = new LineInfo();
	}
	
	/**
	 * Informs the token marker that line have been deleted from
	 * the document. This removes the lines in question from the
	 * <code>lineInfo</code> array.
	 * @param index The first line number
	 * @param lines The number of lines
	 */
	public void deleteLines(int index, int lines)
	{
		if (lines <= 0)
			return;
		int len = index + lines;
		length -= lines;
		System.arraycopy(lineInfo,len,lineInfo,
			index,lineInfo.length - len);
	}

	/**
	 * Returns true if the next line should be repainted. This
	 * will return true after a line has been tokenized that starts
	 * a multiline token that continues onto the next line.
	 */
	public boolean isNextLineRequested()
	{
		return nextLineRequested;
	}

	// protected members

	/**
	 * The first token in the list. This should be used as the return
	 * value from <code>markTokens()</code>.
	 */
	protected Token firstToken;

	/**
	 * The last token in the list. New tokens are added here.
	 * This should be set to null before a new line is to be tokenized.
	 */
	protected Token lastToken;

	/**
	 * An array for storing information about lines. It is enlarged and
	 * shrunk automatically by the <code>insertLines()</code> and
	 * <code>deleteLines()</code> methods.
	 */
	protected LineInfo[] lineInfo;

	/**
	 * The length of the <code>lineInfo</code> array.
	 */
	protected int length;

	/**
	 * True if the next line should be painted.
	 */
	protected boolean nextLineRequested;

	/**
	 * Creates a new <code>TokenMarker</code>. This DOES NOT create
	 * a lineInfo array; an initial call to <code>insertLines()</code>
	 * does that.
	 */
	protected TokenMarker()
	{
	}

	/**
	 * Ensures that the <code>lineInfo</code> array can contain the
	 * specified index. This enlarges it if necessary. No action is
	 * taken if the array is large enough already.<p>
	 *
	 * It should be unnecessary to call this under normal
	 * circumstances; <code>insertLine()</code> should take care of
	 * enlarging the line info array automatically.
	 *
	 * @param index The array index
	 */
	protected void ensureCapacity(int index)
	{
		if(lineInfo == null)
			lineInfo = new LineInfo[index + 1];
		else if(lineInfo.length <= index)
		{
			LineInfo[] lineInfoN = new LineInfo[(index + 1) * 2];
			System.arraycopy(lineInfo,0,lineInfoN,0,
					 lineInfo.length);
			lineInfo = lineInfoN;
		}
	}

	/**
	 * Adds a token to the token list.
	 * @param length The length of the token
	 * @param id The id of the token
	 */
	protected void addToken(int length, byte id)
	{
		if(id >= Token.INTERNAL_FIRST && id <= Token.INTERNAL_LAST)
			throw new InternalError("Invalid id: " + id);

		if(firstToken == null)
		{
			firstToken = new Token(length,id);
			lastToken = firstToken;
		}
		else if(lastToken == null)
		{
			lastToken = firstToken;
			firstToken.length = length;
			firstToken.id = id;
		}
		else if(lastToken.next == null)
		{
			lastToken.next = new Token(length,id);
			lastToken = lastToken.next;
		}
		else
		{
			lastToken = lastToken.next;
			lastToken.length = length;
			lastToken.id = id;
		}
	}

	/**
	 * Inner class for storing information about tokenized lines.
	 */
	public class LineInfo
	{
		/**
		 * Creates a new LineInfo object with token = Token.NULL
		 * and obj = null.
		 */
		public LineInfo()
		{
		}

		/**
		 * Creates a new LineInfo object with the specified
		 * parameters.
		 */
		public LineInfo(byte token, Object obj)
		{
			this.token = token;
			this.obj = obj;
		}

		/**
		 * The id of the last token of the line.
		 */
		public byte token;

		/**
		 * This is for use by the token marker implementations
		 * themselves. It can be used to store anything that
		 * is an object and that needs to exist on a per-line
		 * basis.
		 */
		public Object obj;
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.2  2003/04/10 13:47:48  fisker
 * removed more unused imports
 *
 * Revision 1.1  2000/01/12 03:18:00  bruce
 *
 * Addition of Syntax Colour Highlighting Package to CVS tree.  This is LGPL code used in the Moe Editor to provide syntax highlighting.
 *
 * Revision 1.24  1999/06/05 00:22:58  sp
 * LGPL'd syntax package
 *
 * Revision 1.23  1999/05/03 08:28:14  sp
 * Documentation updates, key binding editor, syntax text area bug fix
 *
 * Revision 1.22  1999/05/03 04:28:01  sp
 * Syntax colorizing bug fixing, console bug fix for Swing 1.1.1
 *
 * Revision 1.21  1999/05/02 00:07:21  sp
 * Syntax system tweaks, console bugfix for Swing 1.1.1
 *
 * Revision 1.20  1999/05/01 00:55:11  sp
 * Option pane updates (new, easier API), syntax colorizing updates
 *
 * Revision 1.19  1999/04/30 23:20:38  sp
 * Improved colorization of multiline tokens
 *
 * Revision 1.18  1999/04/27 06:53:38  sp
 * JARClassLoader updates, shell script token marker update, token marker compiles
 * now
 *
 * Revision 1.17  1999/04/26 07:55:00  sp
 * Event multicaster tweak, console shows exit code of processes
 *
 * Revision 1.16  1999/04/23 22:37:55  sp
 * Tips updated, TokenMarker.LineInfo is public now
 *
 * Revision 1.15  1999/04/23 05:06:43  sp
 * TokenMarker.markTokens bug fix
 *
 * Revision 1.14  1999/04/23 05:02:25  sp
 * new LineInfo[] array in TokenMarker
 *
 * Revision 1.13  1999/04/19 05:38:20  sp
 * Syntax API changes
 *
 * Revision 1.12  1999/03/15 03:40:23  sp
 * Search and replace updates, TSQL mode/token marker updates
 *
 */
