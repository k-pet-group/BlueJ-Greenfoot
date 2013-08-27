/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2013  Michael Kolling and John Rosenberg 
 
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
package bluej.debugmgr.texteval;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.text.Element;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;

import bluej.Config;
import bluej.editor.moe.BlueJSyntaxView;
import bluej.editor.moe.MoeSyntaxDocument;

/**
 * Syntax colouring for the codepad.
 *
 * @author Bruce Quig
 * @author Michael Kolling
 */
public class TextEvalSyntaxView extends BlueJSyntaxView
{
    public static final short TAG_WIDTH = 14;
    protected static final int BREAKPOINT_OFFSET = TAG_WIDTH + 2;
    protected static final int LEFT_MARGIN = BREAKPOINT_OFFSET + 5;

    // Attributes for lines and document
    public static final String OUTPUT = "output";
    public static final String ERROR = "error";
    public static final String CONTINUE = "continue";
    public static final String OBJECT = "object-ref";

    static final Image promptImage =
        Config.getImageAsIcon("image.eval.prompt").getImage();
    static final Image continueImage =
        Config.getImageAsIcon("image.eval.continue").getImage();
    static final Image objectImage =
        Config.getImageAsIcon("image.eval.object").getImage();

    static final Color outputColor = new Color(0, 120, 0);
    static final Color errorColor = new Color(200, 0, 20);
    
    /**
     * Creates a new TextEvalSyntaxView for painting the specified element.
     * @param elem The element
     */
    public TextEvalSyntaxView(Element elem)
    {
        super(elem, LEFT_MARGIN);
    }

    /**
     * Draw a line for the text eval area.
     */
    public void paintTaggedLine(Segment lineText, int lineIndex, Graphics g, int x, int y, 
            MoeSyntaxDocument document, Color def, Element line, TabExpander tx) 
    {
        if(hasTag(line, OUTPUT)) {
            g.setColor(outputColor);
            Utilities.drawTabbedText(lineText, x, y, g, tx, 0);
        }
        else if(hasTag(line, ERROR)) {
            g.setColor(errorColor);
            Utilities.drawTabbedText(lineText, x, y, g, tx, 0);
        }
        else if(hasObject(line, OBJECT)) {
            g.drawImage(objectImage, x-1-LEFT_MARGIN, y+3-objectImage.getHeight(null), null);
            g.setColor(outputColor);
            Utilities.drawTabbedText(lineText, x, y, g, tx, 0);
        }
        else if(hasTag(line, CONTINUE)) {
            g.drawImage(continueImage, x-1-LEFT_MARGIN, y+3-continueImage.getHeight(null), null);
            paintSyntaxLine(lineText, lineIndex, x, y, g, 
                    document, def, tx);   
        }
        else {
            g.drawImage(promptImage, x-1-LEFT_MARGIN, y+3-promptImage.getHeight(null), null);
            paintSyntaxLine(lineText, lineIndex, x, y, g, 
                    document, def, tx);   
        }
    }
    
    /**
     * Check whether a given line is tagged with a given tag.
     * @param line The line to check
     * @param tag  The name of the tag
     * @return     True, if the tag is set
     */
    protected final boolean hasObject(Element line, String tag)
    {
        return line.getAttributes().getAttribute(tag) != null;
    }
    
    /**
     * redefined paint method to paint breakpoint area
     */
    public void paint(Graphics g, Shape allocation)
    {
        Rectangle bounds = allocation.getBounds();

        // paint the lines
        super.paint(g, allocation);

        // paint the tag separator line
        g.setColor(Color.lightGray);
        g.drawLine(bounds.x + TAG_WIDTH, 0,
                bounds.x + TAG_WIDTH, bounds.y + bounds.height);
    }
}
