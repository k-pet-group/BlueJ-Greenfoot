// Copyright (c) 2000, 2005 BlueJ Group, Deakin University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@bluej.org

package bluej.editor.moe;

import javax.swing.text.*;

/**
 * An implementation of <code>EditorKit</code> used for syntax coloring.
 * This is an adaptation of the SyntaxEditorKit class from JEdit for BlueJ.
 * 
 * @author Bruce Quig
 * @author Michael Kolling
 *
 * 
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
     * 
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
     * 
     */
    public Document createDefaultDocument()
    {
        return new MoeSyntaxDocument();
    }
}
