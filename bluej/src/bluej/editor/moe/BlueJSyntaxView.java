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
package bluej.editor.moe;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainView;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.Utilities;
import javax.swing.text.ViewFactory;

import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

/**
 * A Swing view implementation that does syntax colouring and adds some utility.
 *
 * This class should not be used directly; a SyntaxEditorKit
 * should be used instead.
 *
 * @author Bruce Quig
 * @author Michael Kolling
 * @author Davin McCall
 */
public abstract class BlueJSyntaxView extends PlainView
{
    /** Paint method inner scope? if false, whole method will be highlighted as a single block (NaviView) */
    private static final boolean PAINT_METHOD_INNER = false;
    
    private static final int LEFT_INNER_SCOPE_MARGIN = 5;
    private static final int RIGHT_SCOPE_MARGIN = 4;
    
    // private members
    private Segment line;

    protected Font defaultFont;
    // protected FontMetrics metrics;  is inherited from PlainView
    private boolean initialised = false;
    
    /** System settings for graphics rendering (inc. font antialiasing etc.) */
    private Map<?,?> desktopHints = null;
    
    private int leftMargin = 0;
    
    /**
     * Creates a new BlueJSyntaxView.
     * @param elem The element
     */
    public BlueJSyntaxView(Element elem, int leftMargin)
    {
        super(elem);
        line = new Segment();
        this.leftMargin = leftMargin;
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
    
    /* Override default viewToModel translation to account for margin
     * @see javax.swing.text.PlainView#viewToModel(float, float, java.awt.Shape, javax.swing.text.Position.Bias[])
     */
    @Override
    public int viewToModel(float fx, float fy, Shape a, Position.Bias[] bias)
    {
        return super.viewToModel(fx - leftMargin, fy, a, bias);
    }
    
    /*
     * redefined from PlainView method to allow for redefinition of modelToView translation
     */
    protected Rectangle lineToRect(Shape a, int line)
    {
        Rectangle r = null;
        if (metrics != null) {
            Rectangle alloc = a.getBounds();
            r = new Rectangle(alloc.x + leftMargin, alloc.y + (line * metrics.getHeight()),
                    alloc.width - leftMargin, metrics.getHeight());
        }
        return r;
    }
    
    /*
     * Paints the specified line. This is called by the paint() method from PlainView.
     *
     * @param lineIndex The line number (0 based).
     * @param g The graphics context
     * @param x The x co-ordinate where the line should be painted
     * @param y The y co-ordinate where the line should be painted
     */
    protected void drawLine(int lineIndex, Graphics g, int x, int y)
    {
        if(!initialised) {
            initialise(g);
        }

        MoeSyntaxDocument document = (MoeSyntaxDocument)getDocument();

        Color def = MoeSyntaxDocument.getDefaultColor();

        try {
            Element lineElement = getElement().getElement(lineIndex);
            int start = lineElement.getStartOffset();
            int end = lineElement.getEndOffset();

            document.getText(start, end - (start + 1), line);
            g.setColor(def);
            
            paintTaggedLine(line, lineIndex, g, x, y, document, def, lineElement);
        }
        catch(BadLocationException bl) {
            // shouldn't happen
            bl.printStackTrace();
        }
    }
    
    /**
     * Paint a line of text, without syntax colouring. This is provided as a convenience for subclasses.
     */
    protected void paintPlainLine(int lineIndex, Graphics g, int x, int y)
    {
        super.drawLine(lineIndex, g, x, y);
    }

    /**
     * Draw a line for this view. Default implementation defers to paintSyntaxLine().
     * @param x  The x co-ordinate of the line, where the text is to begin (i.e. the margin area is
     *           to the left of this point)
     */
    protected void paintTaggedLine(Segment line, int lineIndex, Graphics g, int x, int y, 
            MoeSyntaxDocument document, Color def, Element lineElement)
    {
        paintSyntaxLine(line, lineIndex, x, y, g, document, def);
    }

    /**
     * Paints a line with syntax highlighting,
     * @param x  The x co-ordinate of the line, where the text is to begin (i.e. the margin area is
     *           to the left of this point)
     */
    protected final void paintSyntaxLine(Segment line, int lineIndex, int x, int y,
                                 Graphics g, MoeSyntaxDocument document, 
                                 Color def)
    {
        Color[] colors = document.getColors();
        Token tokens = document.getTokensForLine(lineIndex);
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

    protected final void paintScopeMarkers(Graphics g, MoeSyntaxDocument document, Shape a,
            int firstLine, int lastLine, boolean onlyMethods)
    {
        paintScopeMarkers(g, document, a, firstLine, lastLine, onlyMethods, false);
    }
    
    protected void paintScopeMarkers(Graphics g, MoeSyntaxDocument document, Shape a,
            int firstLine, int lastLine, boolean onlyMethods, boolean small)
    {
        Element map = document.getDefaultRootElement();
        ParsedNode rootNode = document.getParser();
        Rectangle clipBounds = g.getClipBounds();
        int char_width = metrics.charWidth('m');

        Color c1 = new Color(210, 230, 210); // green border (container)
        Color c2 = new Color(245, 253, 245); // green wash
        Color c3 = new Color(230, 240, 230); // green border (inner).
        Color c4 = new Color(255, 255, 255); // white wash
        
        Color m1 = new Color(230, 230, 210); // yellow border (methods)
        Color m2 = new Color(253, 253, 245); // yellow wash
        
        Color s1 = new Color(215, 215, 230); // blue border (selection)
        Color s2 = new Color(245, 245, 253); // blue wash
        
        Color i1 = new Color(230, 210, 230); // pink border (iteration)
        Color i2 = new Color(253, 245, 253); // pink wash
        
        int aboveLine = firstLine - 1;
        List<NodeAndPosition> prevScopeStack = new LinkedList<NodeAndPosition>();
        int curLine = firstLine;
        
        try {
            Segment aboveLineSeg = new Segment();
            Segment thisLineSeg = new Segment();
            Segment belowLineSeg = new Segment();
            
            Element aboveLineEl = null;
            Element thisLineEl = map.getElement(firstLine);
            if (aboveLine >= 0) {
                aboveLineEl = map.getElement(aboveLine);
                document.getText(aboveLineEl.getStartOffset(),
                        aboveLineEl.getEndOffset() - aboveLineEl.getStartOffset(),
                        aboveLineSeg);
            }
            Element belowLineEl = null;
            if (firstLine + 1 < map.getElementCount()) {
                belowLineEl = map.getElement(firstLine + 1);
                document.getText(belowLineEl.getStartOffset(),
                        belowLineEl.getEndOffset() - belowLineEl.getStartOffset(),
                        belowLineSeg);
            }
            
            document.getText(thisLineEl.getStartOffset(),
                    thisLineEl.getEndOffset() - thisLineEl.getStartOffset(),
                    thisLineSeg);

            //rootNode.getNodeStack(prevScopeStack, thisLineEl.getStartOffset(), 0);
            getScopeStackAt(rootNode, thisLineEl.getStartOffset(), prevScopeStack);

            while (curLine <= lastLine) {
                
                if (prevScopeStack.size() == 0) {
                    break;
                }
                
                Rectangle lbounds = modelToView(thisLineEl.getStartOffset(), a, Position.Bias.Forward).getBounds();
                int ypos = lbounds.y;
                int ypos2 = ypos + lbounds.height;

                ListIterator<NodeAndPosition> li = prevScopeStack.listIterator();
                int scopeCount = 0; // DAV remove this
                int rightMargin = small ? 0 : 7;
                boolean lastWasInner = true;
                Color lastLineColor = c3;
                while (li.hasNext()) {
                    NodeAndPosition nap = li.next();
                    int napPos = nap.getPosition();
                    int napEnd = napPos + nap.getSize();

                    if (napPos >= thisLineEl.getEndOffset()) {
                        // The node isn't even on this line, go to the next line
                        break;
                    }

                    if (! nap.getNode().isContainer() && ! nap.getNode().isInner()) {
                        continue; // no need to draw the node at all.
                    }
                    
                    if (onlyMethods && nap.getNode().getNodeType() != ParsedNode.NODETYPE_METHODDEF
                            && (! nap.getNode().isInner() || lastWasInner || !PAINT_METHOD_INNER)) {
                        continue;
                    }
                    lastWasInner = nap.getNode().isInner();

                    int endX = clipBounds.x + clipBounds.width; // X position at which we stop drawing
                    boolean startsThisLine = (napPos >= thisLineEl.getStartOffset());
                    
                    if (!startsThisLine && napPos >= aboveLineEl.getStartOffset()) {
                        // officially starts on the previous line but...
                        int n = findNonWhitespace(aboveLineSeg, nap.getPosition() - aboveLineEl.getStartOffset());
                        if (n == -1) {
                            // effectively starts on this line!
                            startsThisLine = true;
                            napPos = thisLineEl.getStartOffset();
                        }
                        else if (n + aboveLineEl.getStartOffset() < napEnd) {
                            // It starts on the previous line, but continues onto this line.
                            // However the start on the line above might be to the right of the normal
                            // indent, meaning we need to draw some border here.
                            // TODO
                        }
                    }
                    
                    boolean endsThisLine = false;
                    if (napEnd < thisLineEl.getEndOffset()) {
                        endsThisLine = true;
                        // The node ends on this line. Just whitespace?
                        int nws = findNonWhitespace(thisLineSeg, 0);
                        if (nws == -1 || thisLineEl.getStartOffset() + nws >= napEnd) {
                            break;
                        }
                        
                        // The node ends on this line, so don't extend it too far
                        nws = findNonWhitespace(thisLineSeg,
                                nap.getPosition() + nap.getSize() - thisLineEl.getStartOffset());
                        if (nws != -1) {
                            Rectangle rr = modelToView(thisLineEl.getStartOffset() + nws,
                                    a, Position.Bias.Forward).getBounds();
                            endX = Math.min(endX, rr.x + rr.width);
                        }
                    }
                    else if (napEnd < belowLineEl.getEndOffset()) {
                        // The node ends on the next line. Just whitespace there?
                        int nws = findNonWhitespace(belowLineSeg, 0);
                        if (nws == -1 || belowLineEl.getStartOffset() + nws >= napEnd) {
                            endsThisLine = true;
                        }
                    }
                    
                    // Ok, it includes text on this line. Or maybe just whitespace?
                    int nws = 0;
                    if (startsThisLine) { 
                        nws = findNonWhitespace(thisLineSeg, napPos - thisLineEl.getStartOffset());
                        if (nws == -1 && nap.getPosition() >= thisLineEl.getStartOffset()) {
                            // Note 2nd check above might seem redundant, but we are just checking
                            // if the node *really* starts this line (as opposed to starting with
                            // whitespace only on the line before).
                            continue; // just white space on this line
                        }
                        
                        // if the node begins further right of the indent, we might still colour
                        // from the indent - only if that section is just whitespace though.
                        int ls = thisLineEl.getStartOffset();
                        
                        if (napPos > ls) {
                            int nwsb = findNonWhitespaceBwards(thisLineSeg,
                                    napPos - ls - 1,
                                    -1) + 1;
                            nws = Math.min(nws, nwsb);
                        }
                        else {
                            nws = 0;
                        }
                    }

                    int indent = nap.getNode().getLeftmostIndent(document, nap.getPosition(), getTabSize());
                  
                    // The width of the editor area
                    int fullWidth = getContainer().getWidth();

                    // We might not want to render the right margin if it is out
                    // of the clip area, so we calculate how much of the margin
                    // we should not render.
                    //int rightMarginNotRendered = fullWidth - endX;
//                    int rightMarginNotRendered = clipBounds.x + clipBounds.width - endX;
                    //int rightMargin = scopeCount * RIGHT_SCOPE_MARGIN + 1  - rightMarginNotRendered;
//                    if (small) {
//                        rightMargin /= 10;
//                    }
//                    if (scopeCount != 0) rightMargin += 3;
//                    if(rightMargin < 0) {
//                        rightMargin = 0;
//                    }                   
                    int scopeRightX = fullWidth - rightMargin;
                    if (nap.getNode().isContainer()) {
                        Color color1 = c1;
                        Color color2 = c2;
                        if (nap.getNode().getNodeType() == ParsedNode.NODETYPE_METHODDEF) {
                            color1 = m1;
                            color2 = m2;
                        }
                        else if (nap.getNode().getNodeType() == ParsedNode.NODETYPE_ITERATION) {
                            color1 = i1;
                            color2 = i2;
                        }
                        else if (nap.getNode().getNodeType() == ParsedNode.NODETYPE_SELECTION) {
                            color1 = s1;
                            color2 = s2;
                        }
                        lastLineColor = color1;
                        
                        int xpos = lbounds.x + char_width * indent - 2;
                        if (nws != 0) {
                             xpos = Math.max(xpos, modelToView(thisLineEl.getStartOffset() + nws, a, Position.Bias.Forward).getBounds().x - 2);
                        }
                                                
                        if (startsThisLine || endsThisLine) {
                            int hoffs = small ? 0 : 4; // determines size of corner arcs
                            g.setColor(color2);
                            g.fillRect(xpos + hoffs, ypos, scopeRightX - xpos - hoffs, ypos2 - ypos);
                            
                            int edgeTop = ypos + (startsThisLine ? hoffs : 0);
                            int edgeBtm = ypos2 - 1 - (endsThisLine ? hoffs : 0);
                            g.fillRect(xpos + 1, edgeTop, scopeRightX - xpos - 1, edgeBtm - edgeTop);
                            
                            if(startsThisLine) {
                                // Top left corner
                                g.fillArc(xpos, ypos, hoffs * 2, hoffs * 2, 180, -90);
                                
                                // Top edge
                                g.setColor(color1);
                                g.drawArc(xpos, ypos, hoffs * 2, hoffs * 2, 180, -90);
                                g.drawLine(xpos + hoffs, ypos, scopeRightX, ypos);
                            }
                            if(endsThisLine) {
                                // Bottom left corner
                                g.setColor(color2);
                                g.fillArc(xpos, edgeBtm - hoffs, hoffs * 2, hoffs * 2, 180, 90);

                                // Bottom edge
                                g.setColor(color1);
                                g.drawArc(xpos, edgeBtm - hoffs, hoffs * 2, hoffs * 2, 180, 90);
                                g.drawLine(xpos + hoffs, ypos2 - 1, scopeRightX, ypos2 - 1);
                            }

                            // Left edge
                            g.drawLine(xpos, edgeTop, xpos, edgeBtm);
                        }
                        else {
                            g.setColor(color2);
                            g.fillRect(xpos + 1, ypos, scopeRightX - xpos - 1, ypos2 - ypos);
                            
                            g.setColor(color1);
                            
                            // Left edge
                            g.drawLine(xpos, ypos, xpos, ypos2);
                        }
                        
                        // Right edge
                        g.drawLine(scopeRightX, ypos, scopeRightX, ypos2);
                        rightMargin += RIGHT_SCOPE_MARGIN;
                    }
                    else if (nap.getNode().isInner()) {
                        int xpos = lbounds.x + indent * char_width;
                        if (nws != 0) {
                            xpos = Math.max(xpos, modelToView(thisLineEl.getStartOffset() + nws, a, Position.Bias.Forward).getBounds().x);
                        }
                        xpos -= LEFT_INNER_SCOPE_MARGIN;
                     
                        g.setColor(c4);
                        g.fillRect(xpos + 1, ypos, scopeRightX - (xpos+1), ypos2 - ypos);
                        
                        g.setColor(lastLineColor);
                        
                        if(startsThisLine) {
                            // Top edge
                            g.drawLine(xpos, ypos, scopeRightX, ypos);
                            
                        }
                        if(endsThisLine) {
                            // Bottom edge          
                            g.drawLine(xpos, ypos2 - 1, scopeRightX, ypos2 - 1);
                        }
       
                        // Left edge
                        g.drawLine(xpos, ypos, xpos, ypos2);
                        
                        // Right edge
                        g.drawLine(scopeRightX, ypos, scopeRightX, ypos2);                       
                    }                    
                    scopeCount++;
                }

                // Move along
                li = prevScopeStack.listIterator(prevScopeStack.size());
                NodeAndPosition nap = li.previous();
                if (nap.getPosition() + nap.getSize() <= thisLineEl.getEndOffset()) {
                    li.remove();
                    int napEnd = nap.getPosition() + nap.getSize();
                    while (li.hasPrevious()) {
                        NodeAndPosition napParent = li.previous();
                        li.next();
                        nap = napParent.getNode().findNodeAtOrAfter(napEnd, napParent.getPosition());
                        while (nap != null) {
                            //napEnd = nap.getPosition() + nap.getSize();
                            if (nap.getPosition() < thisLineEl.getEndOffset()) {
                                // TODO
                                // draw it
                            }
                            li.add(nap);
                            nap = nap.getNode().findNodeAtOrAfter(napEnd, nap.getPosition());
                        }
                        if (napParent.getPosition() + napParent.getSize() > thisLineEl.getEndOffset()) {
                            break;
                        }
                        nap = li.previous();
                        napEnd = nap.getPosition() + nap.getSize();
                        li.remove();
                    }
                }
                
                // Next line
                curLine++;
                if (curLine <= lastLine) {
                    aboveLineEl = thisLineEl;
                    thisLineEl = belowLineEl; 
                    if (curLine + 1 < map.getElementCount()) {
                        belowLineEl = map.getElement(curLine + 1);
                    }
                    Segment oldAbove = aboveLineSeg;
                    aboveLineSeg = thisLineSeg;
                    thisLineSeg = belowLineSeg;
                    belowLineSeg = oldAbove; // recycle the object
                    
                    document.getText(belowLineEl.getStartOffset(),
                            belowLineEl.getEndOffset() - belowLineEl.getStartOffset(),
                            belowLineSeg);
                }
            }
        }
        catch (BadLocationException ble) {}
    }

    private void getScopeStackAt(ParsedNode root, int position, List<NodeAndPosition> list)
    {
        list.add(new NodeAndPosition(root, 0, root.getSize()));
        int curpos = 0;
        NodeAndPosition nap = root.findNodeAtOrAfter(position, curpos);
        while (nap != null) {
            list.add(nap);
            curpos = nap.getPosition();
            nap = nap.getNode().findNodeAtOrAfter(position, curpos);
        }
    }

    private int findNonWhitespace(Segment segment, int startPos)
    {
        int endpos = segment.offset + segment.count;
        for (int i = segment.offset + startPos; i < endpos; i++) {
            char c = segment.array[i];
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return i - segment.offset;
            }
        }
        return -1;
    }
    
    private int findNonWhitespaceBwards(Segment segment, int startPos, int endPos)
    {
        int lastP = segment.offset + endPos;
        int i;
        for (i = segment.offset + startPos; i > lastP; i--) {
            char c = segment.array[i];
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                break;
            }
        }
        return i - segment.offset;
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
    protected void initialise(Graphics g)
    {
        defaultFont = g.getFont();
        
        // Use system settings for text rendering (Java 6 only)
        Toolkit tk = Toolkit.getDefaultToolkit(); 
        desktopHints = (Map<?,?>) (tk.getDesktopProperty("awt.font.desktophints")); 
        if (desktopHints != null && g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.addRenderingHints(desktopHints); 
        }

        initialised = true;
    }

    // --- TabExpander interface methods -----------------------------------

    /*
     * Returns the next tab stop position after a given reference position.
     * This implementation does not support things like centering so it
     * ignores the tabOffset argument.
     *
     * @param x the current position >= 0
     * @param tabOffset the position within the text stream
     *   that the tab occurred at >= 0.
     * @return the tab stop, measured in points >= 0
     */
    public float nextTabStop(float x, int tabOffset)
    {
        // calculate tabsize using fontwidth and tab spaces
        updateMetrics();
        int tabSize = getTabSize() * metrics.charWidth('m');
        if (tabSize == 0) {
            return x;
        }
        int tabStopNumber = (int)((x - leftMargin) / tabSize) + 1;
        return (tabStopNumber * tabSize) + leftMargin + 2;
    }
    
    /**
     * Need to override this method from PlainView because the PlainView version is buggy for
     * changes (which aren't inserts/removes) of multiple lines.
     */
    protected void updateDamage(DocumentEvent changes, Shape a, ViewFactory f)
    {
        Component host = getContainer();
        Element elem = getElement();
        DocumentEvent.ElementChange ec = changes.getChange(elem);
        
        Element[] added = (ec != null) ? ec.getChildrenAdded() : null;
        Element[] removed = (ec != null) ? ec.getChildrenRemoved() : null;
        if (((added != null) && (added.length > 0)) || 
            ((removed != null) && (removed.length > 0))) {
            // This case is handled Ok by the superclass.
            super.updateDamage(changes, a, f);
        } else {
            // This is the case we have to fix. The PlainView implementation only
            // repaints a single line; we need to repaint the whole range.
            super.updateDamage(changes, a, f);
            Element map = getElement();
            int choffset = changes.getOffset();
            int chlength = Math.max(changes.getLength(), 1);
            int line = map.getElementIndex(choffset);
            int lastline = map.getElementIndex(choffset + chlength - 1);
            damageLineRange(line, lastline, a, host);
        }
    }
}
