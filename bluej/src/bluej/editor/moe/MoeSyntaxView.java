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

import org.gjt.sp.jedit.syntax.*;
import bluej.utility.*;
import bluej.Config;

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
 * @version $Id: MoeSyntaxView.java 375 2000-01-24 22:56:25Z mik $
 */

public class MoeSyntaxView extends PlainView
{
    // private members
    private Segment line;

    static final Image breakImage = 
        new ImageIcon(Config.getImageFilename("image.breakmark")).getImage();
    static final Image stepImage = 
        new ImageIcon(Config.getImageFilename("image.stepmark")).getImage();
    static final Image breakStepImage = 
        new ImageIcon(Config.getImageFilename("image.breakstepmark")).getImage();
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
     * @param lineIndex The line number
     * @param g The graphics context
     * @param x The x co-ordinate where the line should be painted
     * @param y The y co-ordinate where the line should be painted
     */
    public void drawLine(int lineIndex, Graphics g, int x, int y)
    {

        //need to add left indent
        // add breakpoint image

        // add breakpoint offset to x co-ordinate
        int offsetX = x  + BREAKPOINT_OFFSET;
        

        SyntaxDocument document = (SyntaxDocument)getDocument();
        TokenMarker tokenMarker = document.getTokenMarker();

        FontMetrics metrics = g.getFontMetrics();
        Color def = getDefaultColor();

        try {
            Element lineElement = getElement()
                .getElement(lineIndex);
            int start = lineElement.getStartOffset();
            int end = lineElement.getEndOffset();

            document.getText(start,end - (start + 1),line);

            // draw breakpoint and/or step image

            if (Boolean.TRUE.equals
                (lineElement.getAttributes().getAttribute(
                                                MoeEditor.BREAKPOINT))) {
                if (Boolean.TRUE.equals
                    (lineElement.getAttributes().getAttribute(
                                                MoeEditor.STEPMARK))) {
                    g.drawImage(breakStepImage, x-1, 
                                y-breakStepImage.getHeight(null), null);
                }
                else {  // break only
                    g.drawImage(breakImage, x-1, 
                                y-breakImage.getHeight(null), null);
                }
            }
            else if (Boolean.TRUE.equals
                (lineElement.getAttributes().getAttribute(
                                                MoeEditor.STEPMARK))) {
                g.drawImage(stepImage, x-1, y-stepImage.getHeight(null), 
                            null);
            }

            // if no tokenMarker just paint as plain text
            if(tokenMarker == null) {
                g.setColor(def);
                Utilities.drawTabbedText(line, offsetX, y, g, this, 0);
            }
            else {
                paintSyntaxLine(line, lineIndex, offsetX, y, g, document, 
                                tokenMarker, def);

                // following lines were in SyntaxView.  Unsure as to whether
                //  needed
                // if(tokenMarker.isNextLineRequested())
                // forceRepaint(metrics,x,y);
            }
        }
        catch(BadLocationException bl) {
            // shouldn't happen
            bl.printStackTrace();
        }
    }

    /**
     * returns default foreground colour
     * 
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
                                 Graphics g, SyntaxDocument document, TokenMarker tokenMarker,
                                 Color def)
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

    // redefinition of a method used in SyntaxView, unsure if needed anymore
//    /**
//     *
//     * Stupid hack that repaints from y to the end of the text component
//     *
//     */
//     private void forceRepaint(FontMetrics metrics, int x, int y)
//     {
//         Container host = getContainer();
//         Dimension size = host.getSize();
//         /**
//          * We repaint the next line only, instead of the
//          * entire viewscreen, since PlainView doesn't (yet)
//          * collapse multiple repaint requests.
//          */
//         host.repaint(x,y,size.width - x,metrics.getHeight()
//                      + metrics.getMaxAscent());
//     }


   /**
    * redefined paint method to paint breakpoint area
    *
    */
    public void paint(Graphics g, Shape allocation)
    {
        // paint the lines
        super.paint(g, allocation);

        Rectangle bounds = allocation.getBounds();

        // paint the tag line (possibly grey, always a separator line)

        if(Boolean.FALSE.equals(getDocument().getProperty(MoeEditor.COMPILED))) {
            g.setColor(Color.lightGray);
            g.fillRect(0, 0, bounds.x + MoeEditor.TAG_WIDTH, 
                       bounds.y + bounds.height);
        }
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
    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException 
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
        Segment buffer =  getLineBuffer(); 
        doc.getText(p0, pos - p0, buffer);
        int xOffs = Utilities.getTabbedTextWidth(buffer, metrics, tabBase, this, p0);

        // fill in the results and return, include breakpoint area offset
        lineArea.x += xOffs + (MoeEditor.TAG_WIDTH + 1); 
        lineArea.width = 1;
        lineArea.height = metrics.getHeight();
        return lineArea;
    }



  // --- TabExpander interface methods ------------------------------------------

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
        return x + tabSize;
    }
    


   /**
    * redefined from PlainView private method to allow for redefinition of 
    * modelToView method
    */
    private Rectangle lineToRect(Shape a, int line) {
        Rectangle r = null;
        if (metrics != null) {
            Rectangle alloc = a.getBounds();
            r = new Rectangle(alloc.x, alloc.y + (line * metrics.getHeight()),
                              alloc.width, metrics.getHeight());
        }
        return r;
    }

}

/*
* ChangeLog:
* $Log$
* Revision 1.2  2000/01/24 22:56:23  mik
*
* editor improvements:
*  - fixed problem with compile button disable inviews
*  - added step arrow
*
* Revision 1.1  2000/01/12 03:39:59  bruce
*
* New files added to provide Syntax highlighting.  Altered document type from Styled to Plain.
*
* Revision 1.23  1999/06/05 00:22:58  sp
* LGPL'd syntax package
*
* Revision 1.22  1999/05/28 02:00:25  sp
* SyntaxView bug fix, faq update, MiscUtilities.isURL() method added
*
* Revision 1.21  1999/05/02 00:07:21  sp
* Syntax system tweaks, console bugfix for Swing 1.1.1
*
* Revision 1.20  1999/05/01 02:21:12  sp
* 1.6pre4
*
* Revision 1.19  1999/05/01 00:55:11  sp
* Option pane updates (new, easier API), syntax colorizing updates
*
* Revision 1.18  1999/04/30 23:20:38  sp
* Improved colorization of multiline tokens
*
* Revision 1.17  1999/04/19 05:38:20  sp
* Syntax API changes
*
* Revision 1.16  1999/03/13 08:50:39  sp
* Syntax colorizing updates and cleanups, general code reorganizations
*
* Revision 1.15  1999/03/12 23:51:00  sp
* Console updates, uncomment removed cos it's too buggy, cvs log tags added
*
*/
