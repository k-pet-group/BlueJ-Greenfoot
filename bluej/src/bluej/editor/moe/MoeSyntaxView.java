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
import javax.swing.*;
import java.awt.*;
import java.util.*;

import bluej.utility.*;
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
 * @version $Id: MoeSyntaxView.java 1040 2001-12-10 16:35:56Z mik $
 */

public class MoeSyntaxView extends PlainView
{
    // private members
    private Segment line;

    private Font defaultFont;
    // protected FontMetrics metrics;  is inherited from PlainView
    private Font lineNumberFont;
    private Font smallLineNumberFont;
    FontMetrics lineNumberMetrics;
    private boolean initialised = false;

    static final Image breakImage =
        Config.getImageAsIcon("image.breakmark").getImage();
    static final Image stepImage =
        Config.getImageAsIcon("image.stepmark").getImage();
    static final Image breakStepImage =
        Config.getImageAsIcon("image.breakstepmark").getImage();
    static final int BREAKPOINT_OFFSET = MoeEditor.TAG_WIDTH + 2;

    /**
     * Creates a new <code>MoeSyntaxView</code> for painting the specified
     * element.
     * @param elem The element
     */
    public MoeSyntaxView(Element elem)
    {
        super(elem);
        line = new Segment();
    }

    /**
     * Paints the specified line.
     *
     * This method performs the following:
     *
     *  - Gets the token marker and color table from the current document,
     *    typecast to a SyntaxDocument.
     *  - Tokenizes the required line by calling the
     *    markTokens() method of the token marker.
     *  - Paints each token, obtaining the color by looking up the
     *    the Token.id value in the color table.
     *
     * If either the document doesn't implement
     * SyntaxDocument, or if the returned token marker is
     * null, the line will be painted with no colorization.
     *
     * Currently, we assume that the whole document uses the same font.
     * To support font changes, some of the code from "initilise" needs
     * to be here to be done repeatedly for each line.
     *
     * @param lineIndex The line number
     * @param g The graphics context
     * @param x The x co-ordinate where the line should be painted
     * @param y The y co-ordinate where the line should be painted
     */
    public void drawLine(int lineIndex, Graphics g, int x, int y)
    {
        if(!initialised)
            initialise(g);

        SyntaxDocument document = (SyntaxDocument)getDocument();
        TokenMarker tokenMarker = document.getTokenMarker();

        Color def = getDefaultColor();

        try {
            Element lineElement = getElement().getElement(lineIndex);
            int start = lineElement.getStartOffset();
            int end = lineElement.getEndOffset();

            document.getText(start,end - (start + 1), line);

            g.setColor(def);
            if(PrefMgr.displayLineNumbers())
                drawLineNumber(g, lineIndex+1, x, y);

            // draw breakpoint and/or step image

            if (Boolean.TRUE.equals
                (lineElement.getAttributes().getAttribute(
                                                MoeEditor.BREAKPOINT))) {
                if (Boolean.TRUE.equals
                    (lineElement.getAttributes().getAttribute(
                                                MoeEditor.STEPMARK))) {
                    g.drawImage(breakStepImage, x-1,
                                y+3-breakStepImage.getHeight(null), null);
                }
                else {  // break only
                    g.drawImage(breakImage, x-1,
                                y+3-breakImage.getHeight(null), null);
                }
            }
            else if (Boolean.TRUE.equals
                (lineElement.getAttributes().getAttribute(
                                                MoeEditor.STEPMARK))) {
                g.drawImage(stepImage, x-1, y+3-stepImage.getHeight(null),
                            null);
            }

            // if no tokenMarker just paint as plain text
            if(tokenMarker == null) {
                Utilities.drawTabbedText(line, x+BREAKPOINT_OFFSET, y, g, this,
                                         0);
            }
            else {
                paintSyntaxLine(line, lineIndex, x+BREAKPOINT_OFFSET, y, g, 
                                document, tokenMarker, def);
            }
        }
        catch(BadLocationException bl) {
            // shouldn't happen
            bl.printStackTrace();
        }
    }

    /**
     * Draw the line number in front of the line
     */
    private void drawLineNumber(Graphics g, int lineNumber, int x, int y)
    {
        String number = Integer.toString(lineNumber);
        int stringWidth = lineNumberMetrics.stringWidth(number);
        int xoffset = BREAKPOINT_OFFSET - stringWidth - 3;

        if(xoffset < -2) {
            g.setFont(smallLineNumberFont);
            g.drawString(number, x-3, y);
        }
        else {
            g.setFont(lineNumberFont);
            g.drawString(number, x + xoffset, y);
        }
        g.setFont(defaultFont);
    }

    /**
     * Initialise some fields after we get a graphics context for the first time
     */
    private void initialise(Graphics g)
    {
        defaultFont = g.getFont();
        lineNumberFont = defaultFont.deriveFont(9.0f);
        smallLineNumberFont = defaultFont.deriveFont(7.0f);
        Component c = getContainer();
        lineNumberMetrics = c.getFontMetrics(lineNumberFont);
        initialised = true;
    }

    /**
     * Return default foreground colour
     */
    protected Color getDefaultColor()
    {
        return getContainer().getForeground();
    }

    /**
     * paints a line with syntax highlighting,
     * redefined from DefaultSyntaxDocument.
     *
     */
    private void paintSyntaxLine(Segment line, int lineIndex, int x, int y,
                                 Graphics g, SyntaxDocument document, 
                                 TokenMarker tokenMarker, Color def)
    {
        Color[] colors = document.getColors();
        Token tokens = tokenMarker.markTokens(line, lineIndex);
        int offset = 0;
        for(;;) {
            byte id = tokens.id;
            if(id == Token.END)
                break;

            int length = tokens.length;
            Color color;
            if(id == Token.NULL)
                color = def;
            else
                color = colors[id];
            g.setColor(color == null ? def : color);

            line.count = length;
            x = Utilities.drawTabbedText(line,x,y,g,this,offset);
            line.offset += length;
            offset += length;

            tokens = tokens.next;
        }
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
            g.fillRect(0, 0, bounds.x + MoeEditor.TAG_WIDTH,
                       bounds.y + bounds.height);
        }

        // paint the lines
        super.paint(g, allocation);

        // paint the tag separator line

        g.setColor(Color.black);
        g.drawLine(bounds.x + MoeEditor.TAG_WIDTH, 0,
                   bounds.x + MoeEditor.TAG_WIDTH, bounds.y + bounds.height);
    }


    /**
     * Provides a mapping from the document model coordinate space
     * to the coordinate space of the view mapped to it.  This is a
     * redefined method from PlainView that adds an offset for the
     * view to allow for a breakpoint area in the associated editor.
     *
     * @param pos the position to convert >= 0
     * @param a the allocated region to render into
     * @return the bounding box of the given position
     * @exception BadLocationException  if the given position does not
     *   represent a valid location in the associated document
     * @see View#modelToView
     */
    public Shape modelToView(int pos, Shape a, Position.Bias b)
         throws BadLocationException
    {
        // line coordinates
        Document doc = getDocument();
        Element map = getElement();
        int lineIndex = map.getElementIndex(pos);
        Rectangle lineArea = lineToRect(a, lineIndex);

        // determine span from the start of the line
        int tabBase = lineArea.x + MoeEditor.TAG_WIDTH + 2;

        Element line = map.getElement(lineIndex);
        int p0 = line.getStartOffset();
        Segment buffer = getLineBuffer();
        doc.getText(p0, pos - p0, buffer);
        int xOffs = Utilities.getTabbedTextWidth(buffer, metrics, tabBase, this, p0);

        // fill in the results and return, include breakpoint area offset
        lineArea.x += xOffs + (MoeEditor.TAG_WIDTH + 2);
        lineArea.width = 1;
        lineArea.height = metrics.getHeight();
        return lineArea;
    }


  /**
     * Provides a mapping from the view coordinate space to the logical
     * coordinate space of the model.
     *
     * @param fx the X coordinate >= 0
     * @param fy the Y coordinate >= 0
     * @param a the allocated region to render into
     * @return the location within the model that best represents the
     *  given point in the view >= 0
     * @see View#viewToModel
     */
    public int viewToModel(float fx, float fy, Shape a, Position.Bias[] bias) {
	// PENDING(prinz) properly calculate bias
	bias[0] = Position.Bias.Forward;

        Rectangle alloc = a.getBounds();
        Document doc = getDocument();
        int x = (int) fx;
        int y = (int) fy;
        if (y < alloc.y) {
            // above the area covered by this icon, so the the position
            // is assumed to be the start of the coverage for this view.
            return getStartOffset();
        } else if (y > alloc.y + alloc.height) {
            // below the area covered by this icon, so the the position
            // is assumed to be the end of the coverage for this view.
            return getEndOffset() - 1;
        } else {
            // positioned within the coverage of this view vertically,
            // so we figure out which line the point corresponds to.
            // if the line is greater than the number of lines contained, then
            // simply use the last line as it represents the last possible place
            // we can position to.
            Element map = doc.getDefaultRootElement();
            int lineIndex = Math.abs((y - alloc.y) / metrics.getHeight() );
            if (lineIndex >= map.getElementCount()) {
                return getEndOffset() - 1;
            }
            Element line = map.getElement(lineIndex);
            if (x < alloc.x) {
                // point is to the left of the line
                return line.getStartOffset();
            } else if (x > alloc.x + alloc.width) {
                // point is to the right of the line
                return line.getEndOffset() - 1;
            } else {
                // Determine the offset into the text
                try {
                    Segment buffer = getLineBuffer();
                    int p0 = line.getStartOffset();
                    int p1 = line.getEndOffset() - 1;
                    doc.getText(p0, p1 - p0, buffer);
                    // add Moe breakpoint offset area width
                    int tabBase = alloc.x + MoeEditor.TAG_WIDTH + 2;
                    int offs = p0 + Utilities.getTabbedTextOffset(buffer, metrics,
                                                                  tabBase, x, this, p0);
                    return offs;
                } catch (BadLocationException e) {
                    // should not happen
                    return -1;
                }
            }
        }
    }


    // --- TabExpander interface methods -----------------------------------

    /**
     * Returns the next tab stop position after a given reference position.
     * This implementation does not support things like centering so it
     * ignores the tabOffset argument.
     *
     * @param x the current position >= 0
     * @param tabOffset the position within the text stream
     *   that the tab occurred at >= 0.
     * @return the tab stop, measured in points >= 0
     */
    public float nextTabStop(float x, int tabOffset) {
        // calculate tabsize using fontwidth and tab spaces
        int tabSize = getTabSize() * metrics.charWidth('m');
        if (tabSize == 0) {
            return x;
        }
        int tabStopNumber = (int)((x - BREAKPOINT_OFFSET) / tabSize) + 1;
        return (tabStopNumber * tabSize) + BREAKPOINT_OFFSET + 2;
    }


   /**
    * redefined from PlainView private method to allow for redefinition of
    * modelToView method
    */
    public Rectangle lineToRect(Shape a, int line) {
        Rectangle r = null;
        if (metrics != null) {
            Rectangle alloc = a.getBounds();
            r = new Rectangle(alloc.x, alloc.y + (line * metrics.getHeight()),
                              alloc.width, metrics.getHeight());
        }
        return r;
    }

}
