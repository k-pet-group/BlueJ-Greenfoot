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

/**
 * An implementation of <code>EditorKit</code> used for syntax coloring.
 * This is an adaptation of the SyntaxEditorKit class from JEdit for BlueJ.
 * 
 * @author Bruce Quig
 * @author Michael Kolling
 *
 * @see org.gjt.sp.jedit.syntax.SyntaxView
 */
public class MoeSyntaxEditorKit extends DefaultEditorKit
        implements ViewFactory
{
    private boolean isTextEval;

    /**
     * Create a moe editor kit. There are two modes in which this can operate:
     * as an editor kit for the standard editor (textEval == false) or as an
     * editor kit for the text evaluation area (textEval == true).
     * 
     * @param textEval  Indicate whether to operate for the text eval area
     */
    public MoeSyntaxEditorKit(boolean textEval)
    {
        super();
        isTextEval = textEval;
    }
    
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
        if(isTextEval)
            return new bluej.debugmgr.texteval.TextEvalSyntaxView(elem);
        else
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
