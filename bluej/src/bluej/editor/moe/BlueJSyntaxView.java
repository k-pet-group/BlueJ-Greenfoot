/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
// Copyright (c) 2000, 2005 BlueJ Group, Deakin University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@bluej.org

package bluej.editor.moe;

/**
 * MoeSyntaxView.java - adapted from
 * SyntaxView.java - jEdit's own Swing view implementation
 * to add Syntax highlighting to the BlueJ programming environment.
 */

import javax.swing.text.*;

import java.awt.*;
import java.util.Map;

import org.syntax.jedit.tokenmarker.*;
import org.syntax.jedit.*;

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
 * @version $Id: BlueJSyntaxView.java 6215 2009-03-30 13:28:25Z polle $
 */

public abstract class BlueJSyntaxView extends PlainView
{
    /**  width of tag area for setting breakpoints */
    public static final short TAG_WIDTH = 14;
    protected static final int BREAKPOINT_OFFSET = TAG_WIDTH + 2;

    // private members
    private Segment line;

    private Font defaultFont;
    // protected FontMetrics metrics;  is inherited from PlainView
    private Font lineNumberFont;
    private Font smallLineNumberFont;
    FontMetrics lineNumberMetrics;
    private boolean initialised = false;
    
    /** System settings for graphics rendering (inc. font antialiasing etc.) */
    private Map desktopHints = null;
    
    /**
     * Creates a new BlueJSyntaxView.
     * @param elem The element
     */
    public BlueJSyntaxView(Element elem)
    {
        super(elem);
        line = new Segment();
    }

    @Override
    public void paint(Graphics g, Shape a)
    {
        if (desktopHints != null && g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.addRenderingHints(desktopHints); 
        }

        super.paint(g, a);
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
    protected void drawLine(int lineIndex, Graphics g, int x, int y)
    {
        if(!initialised) {
            initialise(g);
        }

        SyntaxDocument document = (SyntaxDocument)getDocument();
        TokenMarker tokenMarker = document.getTokenMarker();

        Color def = MoeSyntaxDocument.getDefaultColor();

        try {
            Element lineElement = getElement().getElement(lineIndex);
            int start = lineElement.getStartOffset();
            int end = lineElement.getEndOffset();

            document.getText(start, end - (start + 1), line);
            g.setColor(def);
            
            paintTaggedLine(line, lineIndex, g, x, y, document, tokenMarker, def, lineElement);
        }
        catch(BadLocationException bl) {
            // shouldn't happen
            bl.printStackTrace();
        }
    }

    /**
     * Draw a line for this view, including the tag mark.
	 */
	public abstract void paintTaggedLine(Segment line, int lineIndex, Graphics g, int x, int y, 
            SyntaxDocument document, TokenMarker tokenMarker, Color def, Element lineElement);

	/**
     * Draw the line number in front of the line
     */
    protected void drawLineNumber(Graphics g, int lineNumber, int x, int y)
    {
        g.setColor(Color.darkGray);

        String number = Integer.toString(lineNumber);
        int stringWidth = lineNumberMetrics.stringWidth(number);
        int xoffset = BREAKPOINT_OFFSET - stringWidth - 4;

        if(xoffset < -2)      // if it doesn't fit, shift one pixel over.
            xoffset++;

        if(xoffset < -2) {    // if it still doesn't fit...
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
     * paints a line with syntax highlighting,
     * redefined from DefaultSyntaxDocument.
     *
     */
    protected void paintSyntaxLine(Segment line, int lineIndex, int x, int y,
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
            else {
                // check we are within the array bounds
                // safeguard for updated syntax package
                if(id < colors.length)
                    color = colors[id];
                else color = def;
            }
            g.setColor(color == null ? def : color);

            line.count = length;
            x = Utilities.drawTabbedText(line,x,y,g,this,offset);
            line.offset += length;
            offset += length;

            tokens = tokens.next;
        }
    }

    
    /**
     * Check whether a given line is tagged with a given tag.
     * @param line The line to check
     * @param tag  The name of the tag
     * @return     True, if the tag is set
     */
    protected final boolean hasTag(Element line, String tag)
    {
        return Boolean.TRUE.equals(line.getAttributes().getAttribute(tag)); 
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
        
        // Use system settings for text rendering (Java 6 only)
        Toolkit tk = Toolkit.getDefaultToolkit(); 
        desktopHints = (Map) (tk.getDesktopProperty("awt.font.desktophints")); 
        if (desktopHints != null && g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.addRenderingHints(desktopHints); 
        }

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
        int tabBase = lineArea.x + TAG_WIDTH + 2;

        Element line = map.getElement(lineIndex);
        int p0 = line.getStartOffset();
        Segment buffer = getLineBuffer();
        doc.getText(p0, pos - p0, buffer);
        int xOffs = Utilities.getTabbedTextWidth(buffer, metrics, tabBase, this, p0);

        // fill in the results and return, include breakpoint area offset
        lineArea.x += xOffs + (TAG_WIDTH + 2);
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
    public int viewToModel(float fx, float fy, Shape a, Position.Bias[] bias) 
    {
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
                    int tabBase = alloc.x + TAG_WIDTH + 2;
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
