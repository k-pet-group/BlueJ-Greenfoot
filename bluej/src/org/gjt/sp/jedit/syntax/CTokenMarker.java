/*
 * CTokenMarker.java - C token marker
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
 * C token marker.
 *
 * @author Slava Pestov
 * @version $Id: CTokenMarker.java 1307 2002-08-15 09:44:22Z mik $
 */
public class CTokenMarker extends TokenMarker
{
	public CTokenMarker()
	{
		this(true,getKeywords());
	}

	public CTokenMarker(boolean cpp, KeywordMap keywords)
	{
		this.cpp = cpp;
		this.keywords = keywords;
	}

	public byte markTokensImpl(byte token, Segment line, int lineIndex)
	{
		char[] array = line.array;
		int offset = line.offset;
		int lastOffset = offset;
		int lastKeyword = offset;
		int length = line.count + offset;
		boolean backslash = false;
loop:		for(int i = offset; i < length; i++)
		{
			int i1 = (i+1);

			char c = array[i];
			switch(c)
			{
			case '\\':
				backslash = !backslash;
				break;
			case '*':
				if((token == Token.COMMENT1 || token == Token.COMMENT2 || token == Token.COMMENT3)
					&& length - i > 1)
				{
					backslash = false;
					if(length - i > 1 && array[i1] == '/')
					{
						i++;
						addToken((i+1) - lastOffset,token);
						token = Token.NULL;
						lastOffset = i+1;
						lastKeyword = lastOffset;
					}
				}
				break;
			case '#':
				backslash = false;
				if(cpp && token == Token.NULL)
				{
					token = Token.KEYWORD2;
					addToken(i - lastOffset,Token.NULL);
					addToken(length - i,Token.KEYWORD2);
					lastOffset = length;
					break loop;
				}
				break;
			case '/':
				backslash = false;
				if(token == Token.NULL && length - i > 1)
				{
					switch(array[i1])
					{
					case '*':
						addToken(i - lastOffset,token);
						lastOffset = i;
						lastKeyword = lastOffset;
						if(length - i > 2 && array[i+2] == '*')
							token = Token.COMMENT2;
						else if(length - i > 2 && array[i+2] == '#')
							token = Token.COMMENT3;
						else
							token = Token.COMMENT1;
						break;
                    case '/':
						addToken(i - lastOffset,token);
						addToken(length - i,Token.COMMENT1);
						lastOffset = length;
						lastKeyword = lastOffset;
						break loop;
					}
				}
				break;
			case '"':
				if(backslash)
					backslash = false;
				else if(token == Token.NULL)
				{
					token = Token.LITERAL1;
					addToken(i - lastOffset,Token.NULL);
					lastOffset = i;
					lastKeyword = lastOffset;
				}
				else if(token == Token.LITERAL1)
				{
					token = Token.NULL;
					addToken(i1 - lastOffset,Token.LITERAL1);
					lastOffset = i1;
					lastKeyword = lastOffset;
				}
				break;
			case '\'':
				if(backslash)
					backslash = false;
				else if(token == Token.NULL)
				{
					token = Token.LITERAL2;
					addToken(i - lastOffset,Token.NULL);
					lastOffset = i;
					lastKeyword = lastOffset;
				}
				else if(token == Token.LITERAL2)
				{
					token = Token.NULL;
					addToken(i1 - lastOffset,Token.LITERAL1);
					lastOffset = i1;
					lastKeyword = lastOffset;
				}
				break;
			case ':':
				if(token == Token.NULL && lastKeyword == offset)
				{
					backslash = false;
					addToken(i1 - lastOffset,Token.LABEL);
					lastOffset = i1;
					lastKeyword = lastOffset;
					break;
				}
			default:
				backslash = false;
				if(token == Token.NULL && c != '_' &&
					!Character.isLetter(c))
				{
					int len = i - lastKeyword;
					byte id = keywords.lookup(line,lastKeyword,len);
					if(id != Token.NULL)
					{
						if(lastKeyword != lastOffset)
							addToken(lastKeyword - lastOffset,Token.NULL);
						addToken(len,id);
						lastOffset = i;
					}
					lastKeyword = i1;
				}
				break;
			}
		}
		if(token == Token.NULL)
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
				token = Token.NULL;
			}
			else
				addToken(length - lastOffset,token);
		}
		if(token == Token.KEYWORD2 && !backslash)
			token = Token.NULL;
		return token;
	}

	public static KeywordMap getKeywords()
	{
		if(cKeywords == null)
		{
			cKeywords = new KeywordMap(false);
			cKeywords.add("char",Token.KEYWORD3);
			cKeywords.add("double",Token.KEYWORD3);
			cKeywords.add("enum",Token.KEYWORD3);
			cKeywords.add("float",Token.KEYWORD3);
			cKeywords.add("int",Token.KEYWORD3);
			cKeywords.add("long",Token.KEYWORD3);
			cKeywords.add("short",Token.KEYWORD3);
			cKeywords.add("signed",Token.KEYWORD3);
			cKeywords.add("struct",Token.KEYWORD3);
			cKeywords.add("typedef",Token.KEYWORD3);
			cKeywords.add("union",Token.KEYWORD3);
			cKeywords.add("unsigned",Token.KEYWORD3);
			cKeywords.add("void",Token.KEYWORD3);
			cKeywords.add("auto",Token.KEYWORD1);
			cKeywords.add("const",Token.KEYWORD1);
			cKeywords.add("extern",Token.KEYWORD1);
			cKeywords.add("register",Token.KEYWORD1);
			cKeywords.add("static",Token.KEYWORD1);
			cKeywords.add("volatile",Token.KEYWORD1);
			cKeywords.add("break",Token.KEYWORD1);
			cKeywords.add("case",Token.KEYWORD1);
			cKeywords.add("continue",Token.KEYWORD1);
			cKeywords.add("default",Token.KEYWORD1);
			cKeywords.add("do",Token.KEYWORD1);
			cKeywords.add("else",Token.KEYWORD1);
			cKeywords.add("for",Token.KEYWORD1);
			cKeywords.add("goto",Token.KEYWORD1);
			cKeywords.add("if",Token.KEYWORD1);
			cKeywords.add("return",Token.KEYWORD1);
			cKeywords.add("sizeof",Token.KEYWORD1);
			cKeywords.add("switch",Token.KEYWORD1);
			cKeywords.add("while",Token.KEYWORD1);
			cKeywords.add("asm",Token.KEYWORD2);
			cKeywords.add("asmlinkage",Token.KEYWORD2);
			cKeywords.add("far",Token.KEYWORD2);
			cKeywords.add("huge",Token.KEYWORD2);
			cKeywords.add("inline",Token.KEYWORD2);
			cKeywords.add("near",Token.KEYWORD2);
			cKeywords.add("pascal",Token.KEYWORD2);
			cKeywords.add("true",Token.LITERAL2);
			cKeywords.add("false",Token.LITERAL2);
			cKeywords.add("NULL",Token.LITERAL2);
		}
		return cKeywords;
	}

	// private members
	private static KeywordMap cKeywords;

	private boolean cpp;
	private KeywordMap keywords;
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.2  2002/08/15 09:44:21  mik
 * added new syntax colouring catergory: stand-out comment (/*#).
 *
 * Revision 1.1  2000/01/12 03:17:58  bruce
 *
 * Addition of Syntax Colour Highlighting Package to CVS tree.  This is LGPL code used in the Moe Editor to provide syntax highlighting.
 *
 * Revision 1.30  1999/06/05 00:22:58  sp
 * LGPL'd syntax package
 *
 * Revision 1.29  1999/06/03 08:24:13  sp
 * Fixing broken CVS
 *
 * Revision 1.30  1999/05/31 08:11:10  sp
 * Syntax coloring updates, expand abbrev bug fix
 *
 * Revision 1.29  1999/05/31 04:38:51  sp
 * Syntax optimizations, HyperSearch for Selection added (Mike Dillon)
 *
 * Revision 1.28  1999/05/29 03:46:53  sp
 * CTokenMarker bug fix, new splash screen
 *
 * Revision 1.27  1999/05/14 04:56:15  sp
 * Docs updated, default: fix in C/C++/Java mode, full path in title bar toggle
 *
 * Revision 1.26  1999/05/11 09:05:10  sp
 * New version1.6.html file, some other stuff perhaps
 *
 * Revision 1.25  1999/04/22 06:03:26  sp
 * Syntax colorizing change
 *
 * Revision 1.24  1999/04/19 05:38:20  sp
 * Syntax API changes
 *
 * Revision 1.23  1999/03/13 08:50:39  sp
 * Syntax colorizing updates and cleanups, general code reorganizations
 *
 * Revision 1.22  1999/03/13 00:09:07  sp
 * Console updates, uncomment removed cos it's too buggy, cvs log tags added
 *
 * Revision 1.21  1999/03/12 23:51:00  sp
 * Console updates, uncomment removed cos it's too buggy, cvs log tags added
 *
 */
