// Copyright (c) 2000 BlueJ Group, Monash University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html
// Any queries should be directed to Michael Kolling mik@mip.sdu.dk

package bluej.editor.moe;

import bluej.Config;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;

/**
* This class manages the view of a paragraph in Moe. (A paragraph is one
* line in the source text.)
*
* @author  Michael Kolling
*/

public class MoeParagraphView extends ParagraphView  {

    static final Image breakImage = Config.getImageAsIcon("image.break").getImage();


    /**
     * Constructs a MoeParagraphView for the given element.
     *
     * @param elem the element that this view is responsible for
     */
    public MoeParagraphView(Element elem) {
        super(elem);
        setInsets((short)0, (short)(MoeEditor.TAG_WIDTH + 2),
                  (short)0, (short)0);
    }

    /**
     * Redefinition of setting of properties. Redefined to add some
     * space for the tag area.
     */
    protected void setPropertiesFromAttributes() {
        super.setPropertiesFromAttributes();
        setInsets((short)0, (short)(MoeEditor.TAG_WIDTH+2), (short)0, (short)0);
    }

    /**
     * Renders using the given rendering surface and area on that
     * surface.  Only the paiting of the breakpoint symbol is handled
     * here, the rest is handed on and handled by the superclass.
     *
     * @param g the rendering surface to use
     * @param a the allocated region to render into
     * @see View#paint
     */
    public void paint(Graphics g, Shape a) {
        // draw the text in the line
        super.paint(g, a);

        // draw the separator line to tag bar
        Rectangle bounds = a.getBounds();
        if (Boolean.TRUE.equals(
                                getElement().getAttributes().getAttribute(
                                                                          MoeEditor.BREAKPOINT))) {
            // draw break symbol
            g.drawImage(breakImage, bounds.x-1, bounds.y, null);
        }
    }

}

