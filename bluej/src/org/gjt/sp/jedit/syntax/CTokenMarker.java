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
 * @version $Id: CTokenMarker.java 2618 2004-06-17 14:03:32Z mik $
 */
public class CTokenMarker extends TokenMarker
{
	public CTokenMarker(KeywordMap keywords)
	{
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
					token = Token.PRIMITIVE;
					addToken(i - lastOffset,Token.NULL);
					lastOffset = i;
					lastKeyword = lastOffset;
				}
				else if(token == Token.PRIMITIVE)
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
			if(token == Token.LITERAL1 || token == Token.PRIMITIVE)
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

	// private members
	private KeywordMap keywords;
}
