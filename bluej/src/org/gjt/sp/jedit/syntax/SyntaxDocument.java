/*
 * SyntaxDocument.java - Interface all colorized documents must implement
 * Copyright (C) 1999 Slava Pestov
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

import javax.swing.text.Document;
import java.awt.Color;

/**
 * The interface a document must implement to be colorizable by the
 * <code>SyntaxEditorKit</code>. It defines two methods, one that returns
 * the <code>TokenMarker</code> that will split a line into a list of
 * tokens, and a method that returns a color array that maps identification
 * tags returned by the token marker into <code>Color</code> objects. The
 * possible token identifiers are defined as static fields in the
 * <code>Token</code> class.<p>
 *
 * @author Slava Pestov
 * @version $Id: SyntaxDocument.java 342 2000-01-12 03:18:00Z bruce $
 *
 * @see org.gjt.sp.jedit.syntax.DefaultSyntaxDocument
 * @see org.gjt.sp.jedit.syntax.SyntaxEditorKit
 * @see org.gjt.sp.jedit.syntax.TokenMarker
 * @see org.gjt.sp.jedit.syntax.Token
 */
public interface SyntaxDocument extends Document
{
	/**
	 * Returns the token marker that is to be used to split lines
	 * of this document up into tokens. May return null if this
	 * document is not to be colorized.
	 */
	public TokenMarker getTokenMarker();

	/**
	 * Sets the token marker that is to be used to split lines of
	 * this document up into tokens. May throw an exception if
	 * this is not supported for this type of document.
	 * @param tm The new token marker
	 */
	public void setTokenMarker(TokenMarker tm);

	/**
	 * Returns the color array that maps token identifiers to
	 * <code>java.awt.Color</code> objects. Each index in the
	 * array is a token type.
	 */
	public Color[] getColors();

	/**
	 * Sets the color array that maps token identifiers to
	 * <code>java.awt.Color</code> ojects. May throw an exception
	 * if this is not supported for this type of document.
	 * @param colors The new color list
	 */
	public void setColors(Color[] colors);

	/**
	 * Reparses the document, by passing all lines to the token
	 * marker. This should be called after the document is first
	 * loaded.
	 */
	public void tokenizeLines();

	/**
	 * Reparses the document, by passing the specified lines to the
	 * token marker. This should be called after a large quantity of
	 * text is first inserted.
	 * @param start The first line to parse
	 * @param len The number of lines, after the first one to parse
	 */
	public void tokenizeLines(int start, int len);
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.1  2000/01/12 03:17:59  bruce
 *
 * Addition of Syntax Colour Highlighting Package to CVS tree.  This is LGPL code used in the Moe Editor to provide syntax highlighting.
 *
 * Revision 1.6  1999/06/05 00:22:58  sp
 * LGPL'd syntax package
 *
 * Revision 1.5  1999/05/02 00:07:21  sp
 * Syntax system tweaks, console bugfix for Swing 1.1.1
 *
 * Revision 1.4  1999/04/19 05:38:20  sp
 * Syntax API changes
 *
 * Revision 1.3  1999/04/02 00:39:19  sp
 * Fixed console bug, syntax API changes, minor jEdit.java API change
 *
 * Revision 1.2  1999/03/22 04:20:01  sp
 * Syntax colorizing updates
 *
 * Revision 1.1  1999/03/13 09:11:46  sp
 * Syntax code updates, code cleanups
 *
 */
