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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;

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
import bluej.prefmgr.PrefMgr;

/**
 * A Swing view implementation that does syntax colouring and adds some utility.
 *
 * <p>A BlueJSyntaxView (or subclass) instance is normally created by an implementation of
 * the EditorKit interface.
 * 
 * <p>The original version of this class was based on SyntaxView from JEdit. Little of
 * that code remains.
 *
 * @author Slava Pestov
 * @author Bruce Quig
 * @author Michael Kolling
 * @author Davin McCall
 */
public abstract class BlueJSyntaxView extends PlainView
{
    /** (NaviView) Paint method inner scope? if false, whole method will be highlighted as a single block */
    private static final boolean PAINT_METHOD_INNER = false;

    private static final int LEFT_INNER_SCOPE_MARGIN = 5;
    private static final int LEFT_OUTER_SCOPE_MARGIN = 2;
    private static final int RIGHT_SCOPE_MARGIN = 4;
    private static int strength = PrefMgr.getScopeHighlightStrength();
    
    {
        MoeSyntaxDocument.getColors(); // initialize colors
        resetColors();
    }

    /* Scope painting colours */
    public static final Color GREEN_BASE = new Color(225, 248, 225);
    public static final Color BLUE_BASE = new Color(233, 233, 248);
    public static final Color YELLOW_BASE = new Color(250, 250, 180);
    public static final Color PINK_BASE = new Color(248, 233, 248);
    public static final Color GREEN_OUTER_BASE = new Color(188, 218, 188);
    public static final Color BLUE_OUTER_BASE = new Color(188, 188, 210);
    public static final Color YELLOW_OUTER_BASE = new Color(215, 215, 205);
    public static final Color PINK_OUTER_BASE = new Color(210, 177, 210);
    public static final Color GREEN_INNER_BASE = new Color(210, 230, 210);
    
    /* The following are initialized by resetColors() */
    private static Color C1; // green border (container)
    private static Color C2; // green wash
    private static Color C3; // green border (inner).

    private static Color M1; // yellow border (methods)
    private static Color M2; // yellow wash

    private static Color S1; // blue border (selection)
    private static Color S2; // blue wash

    private static Color I1; // pink border (iteration)
    private static Color I2; // pink wash


    /** System settings for graphics rendering (inc. font antialiasing etc.) */
    private static Map<?,?> desktopHints = null;

    // private members
    private Segment line;

    protected Font defaultFont;
    // protected FontMetrics metrics;  is inherited from PlainView
    private boolean initialised = false;

    private Map<ParsedNode,Integer> nodeIndents = new HashMap<ParsedNode,Integer>();

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
     * @param y The y co-ordinate (baseline) where the line should be painted
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

    /**
     * A container for three line segments and elements: the previous (or above) line, the
     * current line, and the next (or below) line.
     */
    private class ThreeLines
    {
        Segment aboveLineSeg;
        Segment thisLineSeg;
        Segment belowLineSeg;

        Element aboveLineEl;
        Element thisLineEl;
        Element belowLineEl;
    }

    protected void paintScopeMarkers(Graphics g, MoeSyntaxDocument document, Shape a,
            int firstLine, int lastLine, boolean onlyMethods, boolean small)
    {
        Element map = document.getDefaultRootElement();
        ParsedNode rootNode = document.getParser();
        Rectangle clipBounds = g.getClipBounds();
        if (clipBounds == null) {
            clipBounds = a.getBounds();
        }
        int char_width = metrics.charWidth('m');

        int aboveLine = firstLine - 1;
        List<NodeAndPosition> prevScopeStack = new LinkedList<NodeAndPosition>();
        int curLine = firstLine;

        try {
            ThreeLines lines = new ThreeLines();
            lines.aboveLineSeg = new Segment();
            lines.thisLineSeg = new Segment();
            lines.belowLineSeg = new Segment();

            lines.aboveLineEl = null;
            lines.thisLineEl = map.getElement(firstLine);
            if (aboveLine >= 0) {
                lines.aboveLineEl = map.getElement(aboveLine);
                document.getText(lines.aboveLineEl.getStartOffset(),
                        lines.aboveLineEl.getEndOffset() - lines.aboveLineEl.getStartOffset(),
                        lines.aboveLineSeg);
            }
            lines.belowLineEl = null;
            if (firstLine + 1 < map.getElementCount()) {
                lines.belowLineEl = map.getElement(firstLine + 1);
                document.getText(lines.belowLineEl.getStartOffset(),
                        lines.belowLineEl.getEndOffset() - lines.belowLineEl.getStartOffset(),
                        lines.belowLineSeg);
            }

            document.getText(lines.thisLineEl.getStartOffset(),
                    lines.thisLineEl.getEndOffset() - lines.thisLineEl.getStartOffset(),
                    lines.thisLineSeg);

            getScopeStackAfter(rootNode, 0, lines.thisLineEl.getStartOffset(), prevScopeStack);

            while (curLine <= lastLine) {

                if (prevScopeStack.size() == 0) {
                    break;
                }

                drawScopes(a, g, document, lines, char_width, prevScopeStack, small, onlyMethods, 0);

                // Next line
                curLine++;
                if (curLine <= lastLine) {
                    lines.aboveLineEl = lines.thisLineEl;
                    lines.thisLineEl = lines.belowLineEl; 
                    if (curLine + 1 < map.getElementCount()) {
                        lines.belowLineEl = map.getElement(curLine + 1);
                    }
                    else {
                        lines.belowLineEl = null;
                    }
                    Segment oldAbove = lines.aboveLineSeg;
                    lines.aboveLineSeg = lines.thisLineSeg;
                    lines.thisLineSeg = lines.belowLineSeg;
                    lines.belowLineSeg = oldAbove; // recycle the object

                    if (lines.belowLineEl != null) {
                        document.getText(lines.belowLineEl.getStartOffset(),
                                lines.belowLineEl.getEndOffset() - lines.belowLineEl.getStartOffset(),
                                lines.belowLineSeg);
                    }
                }
            }
        }
        catch (BadLocationException ble) {}
    }

    private class DrawInfo
    {
        Graphics g;
        ThreeLines lines;
        boolean small;

        ParsedNode node;
        int ypos;
        int ypos2;
        boolean starts;  // the node starts on the current line
        boolean ends;    // the node ends on the current line
        Color color1;    // Edge colour
        Color color2;    // Fill colour
    }

    /**
     * Draw the scope highlighting for one line of the document.
     */
    private void drawScopes(Shape a, Graphics g, MoeSyntaxDocument document, ThreeLines lines,
            int charWidth, List<NodeAndPosition> prevScopeStack, boolean small, boolean onlyMethods,
            int nodeDepth)
    throws BadLocationException
    {
        Rectangle clipBounds = g.getClipBounds();
        if (clipBounds == null) {
            clipBounds = a.getBounds();
        }

        Rectangle lbounds = modelToView(lines.thisLineEl.getStartOffset(), a,
                Position.Bias.Forward).getBounds();
        int ypos = lbounds.y;
        int ypos2 = ypos + lbounds.height;

        int rightMargin = small ? 0 : 20;
        int fullWidth = a.getBounds().width + a.getBounds().x;

        ListIterator<NodeAndPosition> li = prevScopeStack.listIterator();
        //Color lastLineColor = C3;

        NodeAndPosition parent = null;

        DrawInfo drawInfo = new DrawInfo();
        drawInfo.g = g;
        drawInfo.lines = lines;
        drawInfo.small = small;
        drawInfo.ypos = ypos;
        drawInfo.ypos2 = ypos2;

        while (li.hasNext()) {
            NodeAndPosition nap = li.next();
            int napPos = nap.getPosition();
            int napEnd = napPos + nap.getSize();

            if (napPos >= lines.thisLineEl.getEndOffset()) {
                // The node isn't even on this line, go to the next line
                break;
            }

            if (nodeSkipsEnd(napPos, napEnd, lines.thisLineEl, lines.thisLineSeg)) {
                break;
            }

            if (! drawNode(drawInfo, nap, parent, onlyMethods)) {
                parent = nap;
                continue;
            }

            // Draw the start node
            int xpos = getNodeIndent(a, document, nap, lines.thisLineEl,
                    lines.thisLineSeg);
            boolean starts = nodeSkipsStart(napPos, napEnd, lines.aboveLineEl, lines.aboveLineSeg);
            boolean ends = nodeSkipsEnd(napPos, napEnd, lines.belowLineEl, lines.belowLineSeg);
            int rbound = getNodeRBound(a, napEnd, fullWidth - rightMargin, nodeDepth,
                    lines.thisLineEl, lines.thisLineSeg);

            drawInfo.node = nap.getNode();
            drawInfo.starts = starts;
            drawInfo.ends = ends;
            Color [] colors = colorsForNode(drawInfo.node);
            drawInfo.color1 = colors[0];
            drawInfo.color2 = colors[1];

            if (xpos != - 1 && xpos <= a.getBounds().x + a.getBounds().width) {
                drawScopeLeft(drawInfo, xpos, rbound);
                drawScopeRight(drawInfo, rbound);
            }
            nodeDepth++;

            //lastNodePos = nap;
        }

        // Move along.
        li = prevScopeStack.listIterator(prevScopeStack.size());
        NodeAndPosition nap = li.previous(); // last node
        int napPos = nap.getPosition();
        int napEnd = napPos + nap.getSize();

        // For nodes which end on this line:
        while (napEnd <= lines.thisLineEl.getEndOffset()) {
            // Node ends this line
            li.remove(); nodeDepth--;

            if (! li.hasPrevious()) return;
            NodeAndPosition napParent = li.previous();
            li.next();

            NodeAndPosition nextNap = napParent.getNode().findNodeAtOrAfter(napEnd,
                    napParent.getPosition());
            napPos = napParent.getPosition();
            napEnd = napPos + napParent.getSize();

            while (nextNap != null) {
                li.add(nextNap);
                li.previous(); li.next();  // so remove works
                nodeDepth++;
                napPos = nextNap.getPosition();
                napEnd = napPos + nextNap.getSize();
                if (! nodeSkipsStart(napPos, napEnd, lines.thisLineEl, lines.thisLineSeg)) {
                    if (drawNode(drawInfo, nextNap, napParent, onlyMethods)) {
                        // Draw it
                        int xpos = getNodeIndent(a, document, nextNap, lines.thisLineEl,
                                lines.thisLineSeg);
                        int rbound = getNodeRBound(a, napEnd, fullWidth - rightMargin, nodeDepth,
                                lines.thisLineEl, lines.thisLineSeg);
                        drawInfo.node = nextNap.getNode();
                        Color [] colors = colorsForNode(drawInfo.node);
                        drawInfo.color1 = colors[0];
                        drawInfo.color2 = colors[1];
                        drawInfo.starts = nodeSkipsStart(napPos, napEnd, lines.aboveLineEl,
                                lines.aboveLineSeg);
                        drawInfo.ends = nodeSkipsEnd(napPos, napEnd, lines.belowLineEl,
                                lines.belowLineSeg);

                        if (xpos != -1 && xpos <= a.getBounds().x + a.getBounds().width) {
                            drawScopeLeft(drawInfo, xpos, rbound);
                            drawScopeRight(drawInfo, rbound);
                        }
                    }
                }

                nextNap = nextNap.getNode().findNodeAtOrAfter(napPos, napPos);
            }
        }
    }

    /**
     * Check whether a node needs to be drawn.
     * @param info
     * @param node
     * @return
     */
    private boolean drawNode(DrawInfo info, NodeAndPosition nap, NodeAndPosition parent, boolean onlyMethods)
    {
        int napPos = nap.getPosition();
        int napEnd = napPos + nap.getSize();

        if (napPos >= info.lines.thisLineEl.getEndOffset()) {
            // The node isn't even on this line, go to the next line
            return false;
        }

        if (! nap.getNode().isContainer() && ! nap.getNode().isInner()) {
            return false;
        }

        if (onlyMethods) {
            if (nap.getNode().getNodeType() == ParsedNode.NODETYPE_METHODDEF) {
                return true;
            }
            if (! PAINT_METHOD_INNER) {
                return false;
            }
            /*
            if (nap.getNode().isInner() && parent != null && parent.getNode().getNodeType()
                    == ParsedNode.NODETYPE_METHODDEF) {
                return true;
            }
            return false;
             */
        }

        if (nodeSkipsStart(napPos, napEnd, info.lines.thisLineEl, info.lines.thisLineSeg)) {
            return false; // just white space on this line
        }

        if (nodeSkipsEnd(napPos, napEnd, info.lines.thisLineEl, info.lines.thisLineSeg)) {
            return false;
        }

        return true;
    }

    /**
     * Get the scope highlighting colours for a given node.
     */
    private Color[] colorsForNode(ParsedNode node)
    {
        if (node.isInner()) {
            return new Color[] { C3, MoeSyntaxDocument.getBackgroundColor() };
        }
        else {
            if (node.getNodeType() == ParsedNode.NODETYPE_METHODDEF) {
                return new Color[] { M1, M2 };
            }
            if (node.getNodeType() == ParsedNode.NODETYPE_ITERATION) {
                return new Color[] { I1, I2 };
            }
            if (node.getNodeType() == ParsedNode.NODETYPE_SELECTION) {
                return new Color[] { S1, S2 };
            }
            return new Color[] { C1, C2 };
        }
    }

    /**
     * Draw the left edge of the scope, and the middle part up the given bound.
     */
    private void drawScopeLeft(DrawInfo info, int xpos, int rbound)
    {
        Graphics g = info.g;
        if (! info.small) {
            xpos -= info.node.isInner() ? LEFT_INNER_SCOPE_MARGIN : LEFT_OUTER_SCOPE_MARGIN;
        }

        // draw node start
        int hoffs = info.small ? 0 : 4; // determines size of corner arcs
        //g.setColor(info.color2);
        // g.fillRect(xpos + hoffs, info.ypos, endX - xpos - hoffs, ypos2 - ypos);

        int edgeTop = info.ypos + (info.starts ? hoffs : 0);
        int edgeBtm = info.ypos2 - (info.ends ? hoffs : 0);

        g.setColor(info.color2);
        g.fillRect(xpos, edgeTop, hoffs, edgeBtm - edgeTop);
        g.setColor(info.color1);
        g.drawLine(xpos, edgeTop, xpos, edgeBtm);


        if(info.starts) {
            // Top left corner
            g.setColor(info.color2);
            g.fillArc(xpos, info.ypos, hoffs * 2, hoffs * 2, 180, -90);

            // Top edge
            g.setColor(info.color1);
            g.drawArc(xpos, info.ypos, hoffs * 2, hoffs * 2, 180, -90);
        }
        if(info.ends) {
            // Bottom left corner
            g.setColor(info.color2);
            g.fillArc(xpos, edgeBtm - hoffs, hoffs * 2, hoffs * 2, 180, 90);

            // Bottom edge
            g.setColor(info.color1);
            g.drawArc(xpos, edgeBtm - hoffs, hoffs * 2, hoffs * 2, 180, 90);
            //g.drawLine(xpos + hoffs, ypos2 - 1, rbounds, ypos2 - 1);
        }

        drawScope(info, xpos + hoffs, rbound);
    }

    /**
     * Draw the right edge of a scope.
     */
    private void drawScopeRight(DrawInfo info, int xpos)
    {
        Graphics g = info.g;

        int hoffs = info.small ? 0 : 4; // determines size of corner arcs        
        int edgeTop = info.ypos + (info.starts ? hoffs : 0);
        int edgeBtm = info.ypos2 - (info.ends ? hoffs + 1 : 0);

        g.setColor(info.color2);
        g.fillRect(xpos, edgeTop, hoffs, edgeBtm - edgeTop);

        g.setColor(info.color1);
        g.drawLine(xpos + hoffs, edgeTop, xpos + hoffs, edgeBtm);

        if(info.starts) {
            // Top right corner
            g.setColor(info.color2);
            g.fillArc(xpos - hoffs, info.ypos, hoffs * 2, hoffs * 2, 0, 90);

            g.setColor(info.color1);
            g.drawArc(xpos - hoffs, info.ypos, hoffs * 2, hoffs * 2, 0, 90);
        }
        if(info.ends) {
            // Bottom right corner
            g.setColor(info.color2);
            g.fillArc(xpos - hoffs, edgeBtm - hoffs, hoffs * 2, hoffs * 2, 0, -90);

            g.setColor(info.color1);
            g.drawArc(xpos - hoffs, edgeBtm - hoffs, hoffs * 2, hoffs * 2, 0, -90);
        }
    }

    /**
     * Draw the center part of a scope (not the left or right edge, but the bit in between)
     * @param info  general drawing information
     * @param xpos  the leftmost x-coordinate to draw from
     * @param rbounds the rightmost x-coordinate to draw to
     */
    private void drawScope(DrawInfo info, int xpos, int rbounds)
    {
        Graphics g = info.g;
        Color color1 = info.color1;
        Color color2 = info.color2;
        boolean startsThisLine = info.starts;
        boolean endsThisLine = info.ends;
        int ypos = info.ypos;
        int ypos2 = info.ypos2;

        // draw node start
        g.setColor(color2);
        g.fillRect(xpos, ypos, rbounds - xpos, ypos2 - ypos);

        if(startsThisLine) {
            // Top edge
            g.setColor(color1);
            g.drawLine(xpos, ypos, rbounds, ypos);
        }
        if(endsThisLine) {
            // Bottom edge
            g.setColor(color1);
            g.drawLine(xpos, ypos2 - 1, rbounds, ypos2 - 1);
        }
    }

    /**
     * Find the rightmost bound of a node.
     */
    private int getNodeRBound(Shape a, int napEnd, int fullWidth, int nodeDepth,
            Element lineEl, Segment lineSeg) throws BadLocationException
    {
        int rbound = fullWidth - nodeDepth * RIGHT_SCOPE_MARGIN;
        if (lineEl == null || napEnd >= lineEl.getEndOffset()) {
            return rbound;
        }
        if (napEnd < lineEl.getStartOffset()) {
            return rbound;
        }
        int nwsb = findNonWhitespace(lineSeg, napEnd - lineEl.getStartOffset());
        if (nwsb != -1) {
            Rectangle ebounds = modelToView(napEnd, a, Position.Bias.Backward).getBounds();
            return Math.min(rbound, ebounds.x);
        }
        return rbound;
    }

    /**
     * Checks whether the given node should be skipped on the given line (because it
     * starts later). This takes into account that the node may "officially" start on the
     * line, but only have white space, in which case it can be moved down to the next line.
     */
    private boolean nodeSkipsStart(int napPos, int napEnd, Element lineEl, Segment segment)
    {
        if (lineEl == null) {
            return true;
        }
        if (napPos > lineEl.getStartOffset() && napEnd > lineEl.getEndOffset()) {
            // The node officially starts on this line, but might have no text on this
            // line. In that case, we probably want to move its start down to the next line.
            if (napPos >= lineEl.getEndOffset()) {
                return true;
            }
            int nws = findNonWhitespace(segment, napPos - lineEl.getStartOffset());
            if (nws == -1) {
                return true;
            }
        }
        return false;
    }

    private boolean nodeSkipsEnd(int napPos, int napEnd, Element lineEl, Segment segment)
    {
        if (lineEl == null) {
            return true;
        }
        if (napEnd < lineEl.getEndOffset() && napPos < lineEl.getStartOffset()) {
            // The node officially finishes on this line, but might have no text on
            // this line.
            if (napEnd <= lineEl.getStartOffset()) {
                return true;
            }
            if (napEnd >= lineEl.getEndOffset()) {
                return false;
            }
            int nws = findNonWhitespace(segment, 0);
            if (nws == -1 || lineEl.getStartOffset() + nws >= napEnd) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a node's indent amount (in component co-ordinate space) for a given line.
     * If the node isn't present on the line, returns Integer.MAX_VALUE.
     */
    private int getNodeIndent(Shape a, MoeSyntaxDocument doc, NodeAndPosition nap, Element lineEl,
            Segment segment)
        throws BadLocationException
    {
        int napPos = nap.getPosition();
        int napEnd = napPos + nap.getSize();

        if (lineEl == null) {
            return Integer.MAX_VALUE;
        }

        if (nap.getPosition() >= lineEl.getEndOffset()) {
            return Integer.MAX_VALUE;
        }

        if (nap.getPosition() + nap.getSize() <= lineEl.getStartOffset()) {
            return Integer.MAX_VALUE;
        }

        if (nodeSkipsStart(napPos, napEnd, lineEl, segment)
                || nodeSkipsEnd(napPos, napEnd, lineEl, segment)) {
            return Integer.MAX_VALUE;
        }

        // int indent = nap.getNode().getLeftmostIndent(doc, 0, 0);
        Integer indent = nodeIndents.get(nap.getNode());
        if (indent == null) {
            indent = getNodeIndent(a, doc, nap);
            nodeIndents.put(nap.getNode(), indent);
        }

        int xpos = indent;

        // Corner case: node start position is on this line, and is greater than the node indent?
        if (napPos > lineEl.getStartOffset()) {
            int nws = findNonWhitespaceBwards(segment, napPos - lineEl.getStartOffset() - 1, 0);
            if (nws != -1) {
                Rectangle lbounds = modelToView(lineEl.getStartOffset() + nws + 1, a,
                        Position.Bias.Forward).getBounds();
                xpos = Math.max(xpos, lbounds.x);
            }
        }

        return xpos;
    }

    int getNodeIndent(Shape a, MoeSyntaxDocument doc, NodeAndPosition nap)
    {
        try {
            int indent = Integer.MAX_VALUE;

            int curpos = nap.getPosition();
            int napEnd = curpos + nap.getSize();

            Element map = doc.getDefaultRootElement();
            Stack<NodeAndPosition> scopeStack = new Stack<NodeAndPosition>();
            getScopeStackAt(nap.getNode(), nap.getPosition(), 0, scopeStack);

            while (curpos < napEnd) {
                // First skip over inner nodes
                ListIterator<NodeAndPosition> i = scopeStack.listIterator();
                i.next();
                while (i.hasNext()) {
                    NodeAndPosition inner = i.next();
                    if (inner.getNode().isInner()) {
                        int skip = inner.getPosition() + inner.getSize();
                        i.remove();
                        while (i.hasNext()) {
                            i.next();
                            i.remove();
                        }
                        curpos = skip;
                        NodeAndPosition parent = i.previous();
                        int pindex = i.nextIndex();
                        getScopeStackAt(parent.getNode(), parent.getPosition(), curpos, scopeStack);
                        // Urgh. Java invalidates all iterators when the list is modified. Must recreate.
                        i = scopeStack.listIterator(pindex);
                    }
                }

                // Ok, we've skipped inner nodes
                int line = map.getElementIndex(curpos);
                Element lineEl = map.getElement(line);
                Segment segment = new Segment();
                doc.getText(curpos, lineEl.getEndOffset() - curpos, segment);
                int nws = findNonWhitespace(segment, 0);

                if (nws == 0) {
                    // Ok, at this position we have non-white space and are not in an inner
                    Rectangle cbounds = modelToView(curpos, a, Position.Bias.Forward).getBounds();
                    indent = Math.min(indent, cbounds.x);
                    curpos = lineEl.getEndOffset();
                }
                else if (nws == -1) {
                    curpos = lineEl.getEndOffset();
                }
                else {
                    curpos += nws;
                }
            }

            return indent == Integer.MAX_VALUE ? -1 : indent;
        }
        catch (BadLocationException ble) {
            return -1;
        }
    }

    private void getScopeStackAt(ParsedNode root, int rootPos, int position, List<NodeAndPosition> list)
    {
        list.add(new NodeAndPosition(root, 0, root.getSize()));
        int curpos = rootPos;
        NodeAndPosition nap = root.findNodeAtOrAfter(position, curpos);
        while (nap != null && nap.getPosition() <= position) {
            list.add(nap);
            curpos = nap.getPosition();
            nap = nap.getNode().findNodeAtOrAfter(position, curpos);
        }
    }

    private void getScopeStackAfter(ParsedNode root, int rootPos, int position, List<NodeAndPosition> list)
    {
        list.add(new NodeAndPosition(root, 0, root.getSize()));
        int curpos = rootPos;
        NodeAndPosition nap = root.findNodeAtOrAfter(position, curpos);
        while (nap != null) {
            list.add(nap);
            curpos = nap.getPosition();
            nap = nap.getNode().findNodeAtOrAfter(position, curpos);
        }
    }

    /**
     * Search for a non-whitespace character, starting from the given offset
     * (0 = start of the segment). Returns -1 if no such character can be found.
     */
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

    /**
     * Search backwards for a non-whitespace character. If no such character
     * is found, returns (endPos - 1).
     */
    private int findNonWhitespaceBwards(Segment segment, int startPos, int endPos)
    {
        int lastP = segment.offset + endPos;
        int i;
        for (i = segment.offset + startPos; i > lastP; i--) {
            char c = segment.array[i];
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return i - segment.offset;
            }
        }
        return i - segment.offset - 1;
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
        if (desktopHints == null) {
            Toolkit tk = Toolkit.getDefaultToolkit(); 
            desktopHints = (Map<?,?>) (tk.getDesktopProperty("awt.font.desktophints"));
        }
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
        int damageStart = getDocument().getLength();
        int damageEnd = 0;

        if (changes instanceof MoeSyntaxEvent) {
            MoeSyntaxEvent mse = (MoeSyntaxEvent) changes;
            for (NodeAndPosition node : mse.getRemovedNodes()) {
                nodeRemoved(node.getNode());
                damageStart = Math.min(damageStart, node.getPosition());
                damageEnd = Math.max(damageEnd, node.getEnd());
            }
            for (NodeAndPosition node : mse.getAddedNodes()) {
                damageStart = Math.min(damageStart, node.getPosition());
                damageEnd = Math.max(damageEnd, node.getEnd());
            }
        }

        Component host = getContainer();
        Element map = getElement();

        if (damageStart < damageEnd) {
            int line = map.getElementIndex(damageStart);
            int lastline = map.getElementIndex(damageEnd - 1);
            damageLineRange(line, lastline, a, host);
        }

        DocumentEvent.ElementChange ec = changes.getChange(map);
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
            int choffset = changes.getOffset();
            int chlength = Math.max(changes.getLength(), 1);
            int line = map.getElementIndex(choffset);
            int lastline = map.getElementIndex(choffset + chlength - 1);
            damageLineRange(line, lastline, a, host);
        }
    }

    private void nodeRemoved(ParsedNode node)
    {
        nodeIndents.remove(node);
    }

    public static void setHighlightStrength(int strength)
    {
        BlueJSyntaxView.strength = strength;
        resetColors();
    }

    /**
     * Sets up the colors based on the strength value 
     * (from strongest (20) to white (0)
     */
    private static void resetColors()
    {       
        C1 = getGreenContainerBorder();
        C2 = getGreenWash(); 
        C3 = getGreenBorder();
        M1 = getYellowBorder();
        M2 = getYellowWash();
        S1 = getBlueBorder();
        S2 = getBlueWash();
        I1 = getPinkBorder();
        I2 = getPinkWash();             
    }

    private static Color getReducedColor(Color c)
    {
        return getReducedColor(c.getRed(), c.getGreen(), c.getBlue(), strength);
    }
    
    /**
     * Get a colour which has been faded toward the background according to the
     * given strength value. The higher the strength value, the less the color
     * is faded.
     */
    public static Color getReducedColor(int r, int g, int b, int strength)
    {
        Color bg = MoeSyntaxDocument.getBackgroundColor();
        double factor = strength / 20.0;
        double other = 1 - factor;
        int nr = Math.min((int)(r * factor + bg.getRed() * other), 255);
        int ng = Math.min((int)(g * factor + bg.getGreen() * other), 255);
        int nb = Math.min((int)(b * factor + bg.getBlue() * other), 255);
        return new Color(nr, ng, nb);
    }
    
    /** 
     * Return the green wash color
     * modified to become less strong based on the 'strength' value
     */
    private static Color getGreenWash()
    {
        return getReducedColor(GREEN_BASE);
    }

    /** 
     * Return the green container border color
     * modified to become less strong based on the 'strength' value
     */
    private static Color getGreenContainerBorder()
    {
        return getReducedColor(GREEN_OUTER_BASE);
    }

    /** 
     * Return the green border color
     * modified to become less strong based on the 'strength' value
     */
    private static Color getGreenBorder()
    {
        return getReducedColor(GREEN_INNER_BASE);
    }

    /**
     * Return the yellow border color
     * modified to become less strong (until white) based on the 'strength' value
     */
    private static Color getYellowBorder()
    {
        return getReducedColor(YELLOW_OUTER_BASE);
    }
    
    /**
     * Return the yellow wash color
     * modified to become less strong (until white) based on the 'strength' value
     */
    private static Color getYellowWash()
    {
        return getReducedColor(YELLOW_BASE);
    }

    /**
     * Return the blue border (selection) color
     * modified to become less strong (until white) based on the 'strength' value
     */
    private static Color getBlueBorder()
    {
        return getReducedColor(BLUE_OUTER_BASE);
    }

    /**
     * Return the blue wash color
     * modified to become less strong (until white) based on the 'strength' value
     */
    private static Color getBlueWash()
    {
        return getReducedColor(BLUE_BASE);
    }

    /**
     *  Return the pink border (iteration) color
     *  modified to become less strong (until white) based on the 'strength' value
     */
    private static Color getPinkBorder()
    {
        return getReducedColor(PINK_OUTER_BASE);
    }

    /**
     *  Return the pink wash color
     *  modified to become less strong (until white) based on the 'strength' value
     */
    private static Color getPinkWash()
    {
        return getReducedColor(PINK_BASE);
    }
}
