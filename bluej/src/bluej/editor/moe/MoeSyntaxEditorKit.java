/**
 * MoeSyntaxEditorKit.java - adapted from
 * SyntaxEditorKit.java - jEdit's own editor kit
 * to add Syntax highlighting to the BlueJ programming environment.
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

package bluej.editor.moe;

import javax.swing.text.*;
import javax.swing.*;

import org.gjt.sp.jedit.syntax.*;

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
 * @author Bruce Quig (BlueJ specific modifications)
 *
 * @see org.gjt.sp.jedit.syntax.SyntaxView
 */
public class MoeSyntaxEditorKit extends DefaultEditorKit 
        implements ViewFactory
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
     * @return a new MoeSyntaxView for an element
     * @see org.gjt.sp.jedit.syntax.SyntaxView
     */
    public View create(Element elem)
    {
        return new MoeSyntaxView(elem);
    }

    /**
     * Creates a new instance of the default document for this
     * editor kit. This returns a new instance of
     * <code>DefaultSyntaxDocument</code>.
     * @see org.gjt.sp.jedit.syntax.DefaultSyntaxDocument
     */
    public Document createDefaultDocument()
    {
        return new MoeSyntaxDocument();
    }
}
