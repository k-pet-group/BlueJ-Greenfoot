/*
 * CTokenMarker.java - C token marker
 * Copyright (C) 1998, 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package org.syntax.jedit.tokenmarker;

import org.syntax.jedit.*;
import javax.swing.text.Segment;

/**
 * C token marker.
 *
 * @author Slava Pestov
 * @version $Id: CTokenMarker.java 5365 2007-11-01 05:24:32Z davmac $
 */
public class CTokenMarker extends TokenMarker
{
    private static KeywordMap cKeywords;

    private boolean cpp;
    private KeywordMap keywords;
    
    /** Where the current token begins */
    private int lastOffset;
    
    /** Where the current keyword must begin (if we are processing one) */
    private int lastKeyword;

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
	    lastOffset = offset;
	    lastKeyword = offset;
	    int length = line.count + offset;
	    boolean backslash = false;

	    loop:
	    for(int i = offset; i < length; i++)
	    {
	        int i1 = (i+1);

	        char c = array[i];
	        if(c == '\\')
	        {
	            backslash = !backslash;
	            continue;
	        }

	        switch(token)
	        {
	            case Token.NULL:
	                switch(c)
	                {
	                    case '#':
	                        if(backslash) {
	                            backslash = false;
	                        }
	                        else if(cpp) {
	                            doKeyword(line,i);
	                            addToken(i - lastOffset,token);
	                            addToken(length - i,Token.KEYWORD2);
	                            lastOffset = lastKeyword = length;
	                            break loop;
	                        }
	                        break;
	                    case '"':
	                        doKeyword(line,i);
	                        if(backslash) {
	                            backslash = false;
	                        }
	                        else {
	                            addToken(i - lastOffset,token);
	                            token = Token.LITERAL1;
	                            lastOffset = lastKeyword = i;
	                        }
	                        break;
	                    case '\'':
	                        doKeyword(line,i);
	                        if(backslash) {
	                            backslash = false;
	                        }
	                        else {
	                            addToken(i - lastOffset, token);
	                            token = Token.LITERAL2;
	                            lastOffset = lastKeyword = i;
	                        }
	                        break;
	                    case ':':
	                        int labelOffset = lastKeyword;
	                        boolean gotKeyword = doKeyword(line,i);
	                        backslash = false;

	                        if (! gotKeyword && labelOffset > lastOffset) {
	                            addToken(labelOffset - lastOffset, token);
	                            lastOffset = labelOffset;
	                        }

	                        addToken(i1 - lastOffset,Token.LABEL);
	                        lastOffset = lastKeyword = i1;
	                        break;
	                    case '/':
	                        backslash = false;
	                        doKeyword(line,i);
	                        if(length - i > 1)
	                        {
	                            switch(array[i1])
	                            {
	                                case '*':
	                                    addToken(i - lastOffset,token);
	                                    lastOffset = lastKeyword = i;
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
	                                    lastOffset = lastKeyword = length;
	                                    break loop;
	                            }
	                        }
	                        break;
	                    default:
	                        backslash = false;
	                        if(!Character.isLetterOrDigit(c) && c != '_') {
	                            doKeyword(line,i);
	                        }
	                    break;
	                }
	                break;
	            case Token.COMMENT1:
	            case Token.COMMENT2:
	            case Token.COMMENT3:
	                backslash = false;
	                if(c == '*' && length - i > 1)
	                {
	                    if(array[i1] == '/')
	                    {
	                        i++;
	                        addToken((i+1) - lastOffset,token);
	                        token = Token.NULL;
	                        lastOffset = lastKeyword = i+1;
	                    }
	                }
	                break;
	            case Token.LITERAL1:
	                if(backslash)
	                    backslash = false;
	                else if(c == '"')
	                {
	                    addToken(i1 - lastOffset,token);
	                    token = Token.NULL;
	                    lastOffset = lastKeyword = i1;
	                }
	                break;
	            case Token.LITERAL2:
	                if(backslash)
	                    backslash = false;
	                else if(c == '\'')
	                {
	                    addToken(i1 - lastOffset,Token.LITERAL1);
	                    token = Token.NULL;
	                    lastOffset = lastKeyword = i1;
	                }
	                break;
	            default:
	                throw new InternalError("Invalid state: "
	                        + token);
	        }
	    }

	    if(token == Token.NULL) {
	        doKeyword(line,length);
	    }

	    switch(token)
	    {
	        case Token.LITERAL1:
	        case Token.LITERAL2:
	            addToken(length - lastOffset,Token.INVALID);
	            token = Token.NULL;
	            break;
	        case Token.KEYWORD2:
	            addToken(length - lastOffset,token);
	            if(!backslash)
	                token = Token.NULL;
	        default:
	            addToken(length - lastOffset,token);
	        break;
	    }

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

	/**
	 * Add a keyword token representing the keyword between lastKeyword and i,
	 * if there is one. If there is not a keyword mark the new lastKeyword position.
	 * 
	 * (something has been hit which isn't part of a keyword, so check if we have
	 * a keyword prior).
	 */
	private boolean doKeyword(Segment line, int i)
	{
		int i1 = i+1;

		int len = i - lastKeyword;
		byte id = keywords.lookup(line,lastKeyword,len);
		if(id != Token.NULL)
		{
			if(lastKeyword != lastOffset) {
				addToken(lastKeyword - lastOffset,Token.NULL);
			}
			addToken(len,id);
			lastOffset = i; // one past the end of the keyword
	        lastKeyword = i1;
			return true;
		}
		lastKeyword = i1;
		return false;
	}
}
