/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
// Copyright (c) 2000 BlueJ Group, Monash University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html
// Any queries should be directed to Michael Kolling: mik@mip.sdu.dk

package bluej.debugmgr.texteval;

/**
 * MoeSyntaxView.java - adapted from
 * SyntaxView.java - jEdit's own Swing view implementation
 * to add Syntax highlighting to the BlueJ programming environment.
 */

import javax.swing.text.*;

import java.awt.*;

import bluej.Config;
import bluej.editor.moe.BlueJSyntaxView;

import org.syntax.jedit.*;
import org.syntax.jedit.tokenmarker.*;

/**
 * A Swing view implementation that colorizes lines of a
 * SyntaxDocument using a TokenMarker.
 *
 * This class should not be used directly; a SyntaxEditorKit
 * should be used instead.
 *
 * @author Slava Pestov
 * @author Bruce Quig
 * @author Michael Kolling
 *
 * @version $Id: TextEvalSyntaxView.java 6163 2009-02-19 18:09:55Z polle $
 */

public class TextEvalSyntaxView extends BlueJSyntaxView
{
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
        super(elem);
    }

 	/**
     * Draw a line for the text eval area.
	 */
	public void paintTaggedLine(Segment lineText, int lineIndex, Graphics g, int x, int y, 
            SyntaxDocument document, TokenMarker tokenMarker, Color def, Element line) 
    {
		if(hasTag(line, OUTPUT)) {
		    g.setColor(outputColor);
		    Utilities.drawTabbedText(lineText, x+BREAKPOINT_OFFSET, y, g, this, 0);
		}
		else if(hasTag(line, ERROR)) {
		    g.setColor(errorColor);
		    Utilities.drawTabbedText(lineText, x+BREAKPOINT_OFFSET, y, g, this, 0);
		}
        else if(hasObject(line, OBJECT)) {
            g.drawImage(objectImage, x-1, y+3-objectImage.getHeight(null), null);
            g.setColor(outputColor);
            Utilities.drawTabbedText(lineText, x+BREAKPOINT_OFFSET, y, g, this, 0);
        }
        else if(hasTag(line, CONTINUE)) {
            g.drawImage(continueImage, x-1, y+3-continueImage.getHeight(null), null);
            paintSyntaxLine(lineText, lineIndex, x+BREAKPOINT_OFFSET, y, g, 
                    document, tokenMarker, def);   
        }
		else {
            g.drawImage(promptImage, x-1, y+3-promptImage.getHeight(null), null);
		    paintSyntaxLine(lineText, lineIndex, x+BREAKPOINT_OFFSET, y, g, 
		            document, tokenMarker, def);   
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
    *
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
