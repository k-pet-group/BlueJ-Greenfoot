// Copyright (c) 2000 BlueJ Group, Monash University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html
// Any queries should be directed to Michael Kolling: mik@mip.sdu.dk

package bluej.editor.moe;

/**
 * MoeSyntaxView.java - adapted from
 * SyntaxView.java - jEdit's own Swing view implementation
 * to add Syntax highlighting to the BlueJ programming environment.
 */

import javax.swing.text.*;

import java.awt.*;
import bluej.Config;
import bluej.prefmgr.PrefMgr;
import org.gjt.sp.jedit.syntax.*;

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
 * @version $Id: MoeSyntaxView.java 2717 2004-07-02 09:14:57Z mik $
 */

public class MoeSyntaxView extends BlueJSyntaxView
{
    // Attributes for lines and document
    public static final String BREAKPOINT = "break";
    public static final String STEPMARK = "step";

    static final Image breakImage =
        Config.getImageAsIcon("image.editor.breakmark").getImage();
    static final Image stepImage =
        Config.getImageAsIcon("image.editor.stepmark").getImage();
    static final Image breakStepImage =
        Config.getImageAsIcon("image.editor.breakstepmark").getImage();

    /**
     * Creates a new MoeSyntaxView for painting the specified element.
     * @param elem The element
     */
    public MoeSyntaxView(Element elem)
    {
        super(elem);
    }

    /**
     * Draw a line for the moe editor.
	 */
	public void paintTaggedLine(Segment line, int lineIndex, Graphics g, int x, int y, 
            SyntaxDocument document, TokenMarker tokenMarker, Color def, Element lineElement) 
    {
		if(PrefMgr.getFlag(PrefMgr.LINENUMBERS))
		    drawLineNumber(g, lineIndex+1, x, y);
   
		// draw breakpoint and/or step image
   
		if (Boolean.TRUE.equals
		    (lineElement.getAttributes().getAttribute(BREAKPOINT))) {
		    if (Boolean.TRUE.equals
		        (lineElement.getAttributes().getAttribute(STEPMARK))) {
		        g.drawImage(breakStepImage, x-1,
		                    y+3-breakStepImage.getHeight(null), null);
		    }
		    else {  // break only
		        g.drawImage(breakImage, x-1,
		                    y+3-breakImage.getHeight(null), null);
		    }
		}
		else if (Boolean.TRUE.equals
		    (lineElement.getAttributes().getAttribute(STEPMARK))) {
		    g.drawImage(stepImage, x-1, y+3-stepImage.getHeight(null),
		                null);
		}

		paintSyntaxLine(line, lineIndex, x+BREAKPOINT_OFFSET, y, g, 
		                document, tokenMarker, def);
	}

   /**
    * redefined paint method to paint breakpoint area
    *
    */
    public void paint(Graphics g, Shape allocation)
    {
        // if uncompiled, fill the tag line with grey
        Rectangle bounds = allocation.getBounds();        
        if(Boolean.FALSE.equals(getDocument().getProperty(MoeEditor.COMPILED))) {
            g.setColor(Color.lightGray);
            g.fillRect(0, 0, bounds.x + TAG_WIDTH,
                       bounds.y + bounds.height);
        }

        // paint the lines
        super.paint(g, allocation);

        // paint the tag separator line
        g.setColor(Color.black);
        g.drawLine(bounds.x + TAG_WIDTH, 0,
                   bounds.x + TAG_WIDTH, bounds.y + bounds.height);
    }

}
