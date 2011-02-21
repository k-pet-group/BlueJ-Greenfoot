/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.editor.moe;

import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import bluej.parser.entity.EntityResolver;

/**
 * An implementation of <code>EditorKit</code> used for syntax coloring.
 * This is an adaptation of the SyntaxEditorKit class from JEdit for BlueJ.
 * 
 * @author Bruce Quig
 * @author Michael Kolling
 */
public class MoeSyntaxEditorKit extends DefaultEditorKit
        implements ViewFactory
{
    private boolean isTextEval;
    private EntityResolver projectResolver;
    private MoeDocumentListener documentListener;

    /**
     * Create a moe editor kit. There are two modes in which this can operate:
     * as an editor kit for the standard editor (textEval == false) or as an
     * editor kit for the text evaluation area (textEval == true).
     * 
     * @param textEval  Indicate whether to operate for the text eval area
     */
    public MoeSyntaxEditorKit(boolean textEval, EntityResolver projectResolver)
    {
        super();
        isTextEval = textEval;
        this.projectResolver = projectResolver;
    }
    
    /**
     * Create a Moe editor kit, for documents which will resolve external references
     * using the given resolver, and send parse events to the specified listener.
     */
    public MoeSyntaxEditorKit(EntityResolver projectResolver, MoeDocumentListener documentListener)
    {
        super();
        isTextEval = false;
        this.projectResolver = projectResolver;
        this.documentListener = documentListener;
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
        if(isTextEval) {
            return new bluej.debugmgr.texteval.TextEvalSyntaxView(elem);
        }
        else {
            return new MoeSyntaxView(elem);
        }
    }

    /**
     * Creates a new instance of the default document for this
     * editor kit. This returns a new instance of
     * <code>DefaultSyntaxDocument</code>.
     * 
     */
    public Document createDefaultDocument()
    {
        return new MoeSyntaxDocument(projectResolver, documentListener);
    }
}
