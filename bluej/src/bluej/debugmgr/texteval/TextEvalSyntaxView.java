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
 * @version $Id: TextEvalSyntaxView.java 2729 2004-07-04 18:50:27Z mik $
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
