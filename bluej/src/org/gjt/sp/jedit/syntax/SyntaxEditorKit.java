/*
 * SyntaxEditorKit.java - jEdit's own editor kit
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

import javax.swing.text.*;

/**
 * An implementation of <code>EditorKit</code> used for syntax colorizing.
 * It implements a view factory that maps elements to syntax colorizing
 * views.<p>
 *
 * This editor kit can be plugged into text components to give them
 * colorization features. It can be used in other applications, not
 * just jEdit. The syntax colorizing package doesn't depend on any
 * jEdit classes.
 *
 * @author Slava Pestov
 * @version $Id: SyntaxEditorKit.java 2618 2004-06-17 14:03:32Z mik $
 *
 * @see org.gjt.sp.jedit.syntax.SyntaxView
 */
public class SyntaxEditorKit extends DefaultEditorKit implements ViewFactory
{
	/**
	 * Returns an instance of a view factory that can be used for
	 * creating views from elements. This implementation returns
	 * the current instance, because this class already implements
	 * <code>ViewFactory</code>.
	 */
	public ViewFactory getViewFactory()
	{
		return this;
	}

	/**
	 * Creates a view from an element that can be used for painting that
	 * element. This implementation returns a new <code>SyntaxView</code>
	 * instance.
	 * @param elem The element
	 * @see org.gjt.sp.jedit.syntax.SyntaxView
	 */
	public View create(Element elem)
	{
		return new SyntaxView(elem);
	}

	/**
	 * Creates a new instance of the default document for this
	 * editor kit. This returns a new instance of
	 * <code>DefaultSyntaxDocument</code>.
	 * @see org.gjt.sp.jedit.syntax.DefaultSyntaxDocument
	 */
	public Document createDefaultDocument()
	{
		return new DefaultSyntaxDocument(null);
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.3  2004/06/17 14:03:32  mik
 * next stage of text evaluator: does syntax colouring now,
can evaluate most expressions and statements
still work in progress
 *
 * Revision 1.2  2003/04/10 13:47:48  fisker
 * removed more unused imports
 *
 * Revision 1.1  2000/01/12 03:17:59  bruce
 *
 * Addition of Syntax Colour Highlighting Package to CVS tree.  This is LGPL code used in the Moe Editor to provide syntax highlighting.
 *
 * Revision 1.10  1999/06/05 00:22:58  sp
 * LGPL'd syntax package
 *
 * Revision 1.9  1999/05/02 00:07:21  sp
 * Syntax system tweaks, console bugfix for Swing 1.1.1
 *
 * Revision 1.8  1999/03/24 05:45:27  sp
 * Juha Lidfors' backup directory patch, removed debugging messages from various locations, documentation updates
 *
 * Revision 1.7  1999/03/13 09:11:46  sp
 * Syntax code updates, code cleanups
 *
 * Revision 1.6  1999/03/13 08:50:39  sp
 * Syntax colorizing updates and cleanups, general code reorganizations
 *
 * Revision 1.5  1999/03/12 23:51:00  sp
 * Console updates, uncomment removed cos it's too buggy, cvs log tags added
 *
 */
