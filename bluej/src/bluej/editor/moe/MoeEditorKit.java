// Copyright (c) 2000 BlueJ Group, Monash University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@monash.edu.au

package bluej.editor.moe;

import java.awt.*;
import javax.swing.text.*;
import javax.swing.JEditorPane;

import bluej.utility.Debug;

/**
* This class managers the editing of text in Moe. It gets all the basic
* functionality from StyledEditorKit, and adds some more.
*
* @author  Michael Kolling
*/

public class MoeEditorKit extends StyledEditorKit {


    /**
     * Redefinition of the inherited model to return a factory which
     * gives out my modified views.
     *
     * @return the factory
     */
    public ViewFactory getViewFactory() {
        return moeFactory;
    }

    private static final ViewFactory moeFactory = new MoeViewFactory();


    // ---- Moe's ViewFactory implementation ---------------------

    static class MoeViewFactory implements ViewFactory {

        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new LabelView(elem);
                } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                    return new MoeParagraphView(elem);
                } else if (kind.equals(AbstractDocument.SectionElementName)) {
                    return new MoeBoxView(elem, View.Y_AXIS);
                } else if (kind.equals(StyleConstants.ComponentElementName)) {
                    return new ComponentView(elem);
                } else if (kind.equals(StyleConstants.IconElementName)) {
                    return new IconView(elem);
                }
            }

            // default to text display
            return new LabelView(elem);
        }

    }
}

