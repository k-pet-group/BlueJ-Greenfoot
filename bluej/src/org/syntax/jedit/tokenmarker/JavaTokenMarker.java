/*
 * JavaTokenMarker.java - Java token marker
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package org.syntax.jedit.tokenmarker;

import org.syntax.jedit.KeywordMap;

/**
 * Java token marker.
 *
 * @author Slava Pestov
 * @version $Id: JavaTokenMarker.java 3074 2004-11-08 04:24:58Z bquig $
 */
public class JavaTokenMarker extends CTokenMarker
{
	public JavaTokenMarker()
	{
		super(false,getKeywords());
	}

	public static KeywordMap getKeywords()
	{
        if(javaKeywords == null) {
            javaKeywords = new KeywordMap(false);
            javaKeywords.add("abstract",Token.KEYWORD1);
            javaKeywords.add("final",Token.KEYWORD1);
            javaKeywords.add("private",Token.KEYWORD1);
            javaKeywords.add("protected",Token.KEYWORD1);
            javaKeywords.add("public",Token.KEYWORD1);
            javaKeywords.add("static",Token.KEYWORD1);
            javaKeywords.add("synchronized",Token.KEYWORD1);
            javaKeywords.add("volatile",Token.KEYWORD1);
            javaKeywords.add("transient",Token.KEYWORD1);
            javaKeywords.add("break",Token.KEYWORD1);
            javaKeywords.add("case",Token.KEYWORD1);
            javaKeywords.add("continue",Token.KEYWORD1);
            javaKeywords.add("default",Token.KEYWORD1);
            javaKeywords.add("do",Token.KEYWORD1);
            javaKeywords.add("else",Token.KEYWORD1);
            javaKeywords.add("for",Token.KEYWORD1);
            javaKeywords.add("if",Token.KEYWORD1);
            javaKeywords.add("instanceof",Token.KEYWORD1);
            javaKeywords.add("new",Token.KEYWORD1);
            javaKeywords.add("return",Token.KEYWORD1);
            javaKeywords.add("switch",Token.KEYWORD1);
            javaKeywords.add("while",Token.KEYWORD1);
            javaKeywords.add("throw",Token.KEYWORD1);
            javaKeywords.add("try",Token.KEYWORD1);
            javaKeywords.add("catch",Token.KEYWORD1);
            javaKeywords.add("finally",Token.KEYWORD1);
            javaKeywords.add("throws",Token.KEYWORD1);
            javaKeywords.add("assert",Token.KEYWORD1);

            javaKeywords.add("package",Token.KEYWORD2);
            javaKeywords.add("import",Token.KEYWORD2);
            javaKeywords.add("class",Token.KEYWORD2);
            javaKeywords.add("interface",Token.KEYWORD2);
            javaKeywords.add("extends",Token.KEYWORD2);
            javaKeywords.add("implements",Token.KEYWORD2);

            javaKeywords.add("this",Token.KEYWORD3);
            javaKeywords.add("null",Token.KEYWORD3);
            javaKeywords.add("super",Token.KEYWORD3);
            javaKeywords.add("true",Token.KEYWORD3);
            javaKeywords.add("false",Token.KEYWORD3);

            javaKeywords.add("byte",Token.PRIMITIVE);
            javaKeywords.add("char",Token.PRIMITIVE);
            javaKeywords.add("short",Token.PRIMITIVE);
            javaKeywords.add("int",Token.PRIMITIVE);
            javaKeywords.add("long",Token.PRIMITIVE);
            javaKeywords.add("float",Token.PRIMITIVE);
            javaKeywords.add("double",Token.PRIMITIVE);
            javaKeywords.add("boolean",Token.PRIMITIVE);
            javaKeywords.add("void",Token.PRIMITIVE);
            javaKeywords.add("enum",Token.PRIMITIVE);
        }
        return javaKeywords;
	}

	// private members
	private static KeywordMap javaKeywords;
}
