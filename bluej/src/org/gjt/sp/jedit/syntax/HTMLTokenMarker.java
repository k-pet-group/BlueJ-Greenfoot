/*
 * HTMLTokenMarker.java - HTML token marker
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
 * HTML token marker.
 *
 * @author Slava Pestov
 * @version $Id: HTMLTokenMarker.java 342 2000-01-12 03:18:00Z bruce $
 */
public class HTMLTokenMarker extends TokenMarker
{
	public static final byte JAVASCRIPT = Token.INTERNAL_FIRST;

	public HTMLTokenMarker()
	{
		keywords = JavaScriptTokenMarker.getKeywords();
	}

	public byte markTokensImpl(byte token, Segment line, int lineIndex)
	{
		char[] array = line.array;
		boolean backslash = false;
		int offset = line.offset;
		int lastOffset = offset;
		int lastKeyword = offset;
		int length = line.count + offset;
loop:		for(int i = offset; i < length; i++)
		{
			int i1 = (i+1);

			switch(array[i])
			{
			case '\\':
				backslash = !backslash;
				break;
			case ';':
				if(token == Token.KEYWORD2)
				{
					token = Token.NULL;
					addToken(i1 - lastOffset,Token.KEYWORD2);
					lastOffset = i1;
					break;
				}
			case '*':
				if(token == Token.COMMENT2 && length - i > 1)
				{
					if(length - i > 1 && array[i1] == '/')
					{
						backslash = false;
						token = JAVASCRIPT;
						i++;
						addToken(i1 - lastOffset,Token.COMMENT2);
						lastOffset = i1;
						break;
					}
				}
			case ':':
				if(token == JAVASCRIPT && lastKeyword == offset)
				{
					backslash = false;
					addToken(i1 - lastOffset,Token.LABEL);
					lastOffset = i1;
					break;
				}
			case '.': case ',': case ' ': case '\t':
			case '(': case ')': case '[': case ']':
			case '{': case '}':
				backslash = false;
				if(token == JAVASCRIPT)
				{
					int len = i - lastKeyword;
					byte id = keywords.lookup(line,lastKeyword,len);
					if(id != Token.NULL)
					{
						if(lastKeyword != lastOffset)
							addToken(lastKeyword - lastOffset,
								Token.NULL);
						addToken(len,id);
						lastOffset = i;
					}
					lastKeyword = i1;
				}
				break;
			case '<':
				backslash = false;
				if(token == Token.NULL)
				{
					if(SyntaxUtilities.regionMatches(false,
						line,i,"<!--"))
						token = Token.COMMENT1;
					else
						token = Token.KEYWORD1;
					addToken(i - lastOffset,Token.NULL);
					lastOffset = i;
				}
				else if(token == JAVASCRIPT)
				{
					if(SyntaxUtilities.regionMatches(true,
						line,i,"</SCRIPT>"))
					{
						token = Token.KEYWORD1;
						addToken(i - lastOffset,Token.NULL);
						lastOffset = i;
					}
				}
				break;
			case '>':
				backslash = false;
				if(token == Token.KEYWORD1)
				{
					if(SyntaxUtilities.regionMatches(true,line,
						lastOffset,"<SCRIPT"))
						token = JAVASCRIPT;
					else
						token = Token.NULL;
					addToken(i1 - lastOffset,Token.KEYWORD1);
					lastOffset = i1;
				}
				else if(token == Token.COMMENT1)
				{
					if(SyntaxUtilities.regionMatches(false,line,
						i - 2,"-->"))
					{
						token = Token.NULL;
						addToken(i1 - lastOffset,
							 Token.COMMENT1);
						lastOffset = i1;
					}
				}
				break;
			case '&':
				backslash = false;
				if(token == Token.NULL)
				{
					token = Token.KEYWORD2;
					addToken(i - lastOffset,Token.NULL);
					lastOffset = i;
				}
				break;
			case '/':
				backslash = false;
				if(token == JAVASCRIPT && length - i > 1)
				{
					switch(array[i1])
					{
					case '*':
						token = Token.COMMENT2;
						addToken(i - lastOffset,Token.NULL);
						lastOffset = i;
						i++;
						break;
					case '/':
						addToken(i - lastOffset,Token.NULL);
						addToken(length - i,Token.COMMENT2);
						lastOffset = length;
						break loop;
					}
				}
				break;
			case '"':
				if(backslash)
					backslash = false;
				else if(token == JAVASCRIPT)
				{
					token = Token.LITERAL1;
					addToken(i - lastOffset,Token.NULL);
					lastOffset = i;
				}
				else if(token == Token.LITERAL1)
				{
					token = JAVASCRIPT;
					addToken(i1 - lastOffset,Token.LITERAL1);
					lastOffset = i1;
				}
				break;
			case '\'':
				if(backslash)
					backslash = false;
				else if(token == JAVASCRIPT)
				{
					token = Token.LITERAL2;
					addToken(i - lastOffset,Token.NULL);
					lastOffset = i;
				}
				else if(token == Token.LITERAL2)
				{
					token = JAVASCRIPT;
					addToken(i1 - lastOffset,Token.LITERAL1);
					lastOffset = i1;
				}
				break;
			default:
				backslash = false;
				break;
			}
		}
		if(token == JAVASCRIPT)
		{
			int len = length - lastKeyword;
			byte id = keywords.lookup(line,lastKeyword,len);
			if(id != Token.NULL)
			{
				if(lastKeyword != lastOffset)
					addToken(lastKeyword - lastOffset,Token.NULL);
				addToken(len,id);
				lastOffset = length;
			}
		}
		if(lastOffset != length)
		{
			if(token == Token.LITERAL1 || token == Token.LITERAL2)
			{
				addToken(length - lastOffset,Token.INVALID);
				token = JAVASCRIPT;
			}
			else if(token == Token.KEYWORD2)
			{
				addToken(length - lastOffset,Token.INVALID);
				token = Token.NULL;
			}
			else if(token == JAVASCRIPT)
				addToken(length - lastOffset,Token.NULL);
			else
				addToken(length - lastOffset,token);
		}
		return token;
	}

	// private members
	private KeywordMap keywords;
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
 * Revision 1.27  1999/06/03 08:24:13  sp
 * Fixing broken CVS
 *
 * Revision 1.28  1999/05/31 08:11:10  sp
 * Syntax coloring updates, expand abbrev bug fix
 *
 * Revision 1.27  1999/05/31 04:38:51  sp
 * Syntax optimizations, HyperSearch for Selection added (Mike Dillon)
 *
 * Revision 1.26  1999/05/14 04:56:15  sp
 * Docs updated, default: fix in C/C++/Java mode, full path in title bar toggle
 *
 * Revision 1.25  1999/05/11 09:05:10  sp
 * New version1.6.html file, some other stuff perhaps
 *
 * Revision 1.24  1999/05/03 04:28:01  sp
 * Syntax colorizing bug fixing, console bug fix for Swing 1.1.1
 *
 * Revision 1.23  1999/04/22 06:03:26  sp
 * Syntax colorizing change
 *
 * Revision 1.22  1999/04/19 05:38:20  sp
 * Syntax API changes
 *
 * Revision 1.21  1999/03/13 08:50:39  sp
 * Syntax colorizing updates and cleanups, general code reorganizations
 *
 * Revision 1.20  1999/03/12 23:51:00  sp
 * Console updates, uncomment removed cos it's too buggy, cvs log tags added
 *
 */
