/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2014  Michael Kolling and John Rosenberg 

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

import bluej.editor.moe.MoeSyntaxEvent.NodeChangeRecord;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import bluej.prefmgr.PrefMgr;
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
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;
import javax.swing.text.ViewFactory;

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
public abstract class BlueJSyntaxView extends MoePlainView
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
    private boolean initialised = false;

    private Map<ParsedNode,Integer> nodeIndents = new HashMap<ParsedNode,Integer>();


    /**
     * Creates a new BlueJSyntaxView.
     * @param elem The element
     */
    public BlueJSyntaxView(Element elem, int leftMargin)
    {
        super(elem, leftMargin);
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

    /*
     * Paints the specified line. This is called by the paint() method from PlainView.
     *
     * @param lineIndex The line number (0 based).
     * @param g The graphics context
     * @param x The x co-ordinate where the line should be painted
     * @param y The y co-ordinate (baseline) where the line should be painted
     */
    @Override
    protected void drawLine(int lineIndex, Graphics g, int x, int y)
    {
        if(!initialised) {
            initialise(g);
        }

        MoeSyntaxDocument document = (MoeSyntaxDocument)getDocument();

        Color def = MoeSyntaxDocument.getDefaultColor();
        TabExpander tx = new MoeTabExpander(tabSize, x);

        try {
            Element lineElement = getElement().getElement(lineIndex);
            int start = lineElement.getStartOffset();
            int end = lineElement.getEndOffset();

            document.getText(start, end - (start + 1), line);
            g.setColor(def);

            paintTaggedLine(line, lineIndex, g, x, y, document, def, lineElement, tx);
        }
        catch (BadLocationException bl) {
            // shouldn't happen
            throw new RuntimeException(bl);
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
            MoeSyntaxDocument document, Color def, Element lineElement, TabExpander tx)
    {
        paintSyntaxLine(line, lineIndex, x, y, g, document, def, tx);
    }

    /**
     * Paints a line with syntax highlighting,
     * @param x  The x co-ordinate of the line, where the text is to begin (i.e. the margin area is
     *           to the left of this point)
     */
    protected final void paintSyntaxLine(Segment line, int lineIndex, int x, int y,
            Graphics g, MoeSyntaxDocument document, 
            Color def, TabExpander tx)
    {
        Color[] colors = MoeSyntaxDocument.getColors();
        Token tokens = document.getTokensForLine(lineIndex);
        int offset = 0;
        for(;;) {
            byte id = tokens.id;
            if(id == Token.END)
                break;

            int length = tokens.length;
            Color color;
            if (id == Token.NULL || id >= colors.length) {
                color = def;
            }
            else {
                color = colors[id];
            }
            g.setColor(color);
            line.count = length;
            
            x = Utilities.drawTabbedText(line,x,y,g,tx,offset);
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
        //optimization for the raspberry pi.
        if (strength == 0) {
            return;
        }
        
        Element map = document.getDefaultRootElement();
        ParsedNode rootNode = document.getParsedNode();

        int aboveLine = firstLine - 1;
        List<NodeAndPosition<ParsedNode>> prevScopeStack = new LinkedList<NodeAndPosition<ParsedNode>>();
        int curLine = firstLine;

        try {
            ThreeLines lines = new ThreeLines();
            lines.aboveLineSeg = new Segment();
            lines.thisLineSeg = new Segment();
            lines.belowLineSeg = new Segment();

            lines.aboveLineEl = null;
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

            lines.thisLineEl = map.getElement(firstLine);
            document.getText(lines.thisLineEl.getStartOffset(),
                    lines.thisLineEl.getEndOffset() - lines.thisLineEl.getStartOffset(),
                    lines.thisLineSeg);

            getScopeStackAfter(rootNode, 0, lines.thisLineEl.getStartOffset(), prevScopeStack);

            while (curLine <= lastLine) {

                if (prevScopeStack.isEmpty()) {
                    break;
                }

                drawScopes(a, g, document, lines, prevScopeStack, small, onlyMethods, 0);

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
     * 
     * @param a              the shape to render into
     * @param g              the graphics context to render to
     * @param document       the document
     * @param lines          the previous, current and next lines (segments and elements)
     * @param prevScopeStack the stack of nodes (from outermost to innermost) at the beginning of the current line
     */
    private void drawScopes(Shape a, Graphics g, MoeSyntaxDocument document, ThreeLines lines,
            List<NodeAndPosition<ParsedNode>> prevScopeStack, boolean small,
            boolean onlyMethods, int nodeDepth)
    throws BadLocationException
    {
        Rectangle lbounds = modelToView(lines.thisLineEl.getStartOffset(), a,
                Position.Bias.Forward).getBounds();
        int ypos = lbounds.y;
        int ypos2 = ypos + lbounds.height;

        int rightMargin = small ? 0 : 20;
        int fullWidth = a.getBounds().width + a.getBounds().x;

        ListIterator<NodeAndPosition<ParsedNode>> li = prevScopeStack.listIterator();
        //Color lastLineColor = C3;

        NodeAndPosition<ParsedNode> parent = null;

        DrawInfo drawInfo = new DrawInfo();
        drawInfo.g = g;
        drawInfo.lines = lines;
        drawInfo.small = small;
        drawInfo.ypos = ypos;
        drawInfo.ypos2 = ypos2;

        while (li.hasNext()) {
            NodeAndPosition<ParsedNode> nap = li.next();
            int napPos = nap.getPosition();
            int napEnd = nap.getEnd();

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
            if (xpos != - 1 && xpos <= a.getBounds().x + a.getBounds().width) {
                boolean starts = nodeSkipsStart(nap, lines.aboveLineEl, lines.aboveLineSeg);
                boolean ends = nodeSkipsEnd(napPos, napEnd, lines.belowLineEl, lines.belowLineSeg);
                int rbound = getNodeRBound(a, nap, fullWidth - rightMargin, nodeDepth,
                        lines.thisLineEl, lines.thisLineSeg);

                drawInfo.node = nap.getNode();
                drawInfo.starts = starts;
                drawInfo.ends = ends;
                Color[] colors = colorsForNode(drawInfo.node);
                drawInfo.color1 = colors[0];
                drawInfo.color2 = colors[1];

                drawScopeLeft(drawInfo, xpos, rbound);
                drawScopeRight(drawInfo, rbound);
            }
            nodeDepth++;

            //lastNodePos = nap;
        }

        // Move along.
        li = prevScopeStack.listIterator(prevScopeStack.size());
        NodeAndPosition<ParsedNode> nap = li.previous(); // last node
        int napPos = nap.getPosition();
        int napEnd = napPos + nap.getSize();

        // For nodes which end on this line:
        while (napEnd <= lines.thisLineEl.getEndOffset()) {
            // Node ends this line
            li.remove(); nodeDepth--;

            if (! li.hasPrevious()) return;
            NodeAndPosition<ParsedNode> napParent = li.previous();
            li.next();

            NodeAndPosition<ParsedNode> nextNap = nap.nextSibling();
            napPos = napParent.getPosition();
            napEnd = napPos + napParent.getSize();
            nap = napParent;

            while (nextNap != null) {
                li.add(nextNap);
                li.previous(); li.next();  // so remove works
                nodeDepth++;
                napPos = nextNap.getPosition();
                napEnd = napPos + nextNap.getSize();
                if (! nodeSkipsStart(nextNap, lines.thisLineEl, lines.thisLineSeg)) {
                    if (drawNode(drawInfo, nextNap, napParent, onlyMethods)) {
                        // Draw it
                        int xpos = getNodeIndent(a, document, nextNap, lines.thisLineEl,
                                lines.thisLineSeg);
                        int rbound = getNodeRBound(a, nextNap, fullWidth - rightMargin, nodeDepth,
                                lines.thisLineEl, lines.thisLineSeg);
                        drawInfo.node = nextNap.getNode();
                        Color [] colors = colorsForNode(drawInfo.node);
                        drawInfo.color1 = colors[0];
                        drawInfo.color2 = colors[1];
                        drawInfo.starts = nodeSkipsStart(nextNap, lines.aboveLineEl,
                                lines.aboveLineSeg);
                        drawInfo.ends = nodeSkipsEnd(napPos, napEnd, lines.belowLineEl,
                                lines.belowLineSeg);

                        if (xpos != -1 && xpos <= a.getBounds().x + a.getBounds().width) {
                            drawScopeLeft(drawInfo, xpos, rbound);
                            drawScopeRight(drawInfo, rbound);
                        }
                    }
                }

                nap = nextNap;
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
    private boolean drawNode(DrawInfo info, NodeAndPosition<ParsedNode> nap, NodeAndPosition<ParsedNode> parent, boolean onlyMethods)
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

        if (nodeSkipsStart(nap, info.lines.thisLineEl, info.lines.thisLineSeg)) {
            return false; // just white space on this line
        }

        return !nodeSkipsEnd(napPos, napEnd, info.lines.thisLineEl, info.lines.thisLineSeg);
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
            if (node.getNodeType() == ParsedNode.NODETYPE_SELECTION
                    || node.getNodeType() == ParsedNode.NODETYPE_NONE) {
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
     * Find the rightmost bound of a node on a particular line.
     * 
     * @param a       The view allocation
     * @param napEnd  The end of the node (position in the document just beyond the node)
     * @param fullWidth  The full width to draw to (for the outermost mode)
     * @param nodeDepth  The node depth
     * @param lineEl   line element of the line to find the bound for
     * @param lineSeg  Segment containing text of the current line
     */
    private int getNodeRBound(Shape a, NodeAndPosition<ParsedNode> nap, int fullWidth, int nodeDepth,
            Element lineEl, Segment lineSeg) throws BadLocationException
    {
        int napEnd = nap.getEnd();
        int rbound = fullWidth - nodeDepth * RIGHT_SCOPE_MARGIN;
        if (lineEl == null || napEnd >= lineEl.getEndOffset()) {
            return rbound;
        }
        if (napEnd < lineEl.getStartOffset()) {
            return rbound;
        }
        
        // If there is some text between the node end and the end of the line, we want to clip the
        // node short so that the text does not appear to be part of the node.
        int nwsb = findNonWhitespaceComment(nap, lineEl, lineSeg, napEnd - lineEl.getStartOffset());
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
    private boolean nodeSkipsStart(NodeAndPosition<ParsedNode> nap, Element lineEl, Segment segment)
    {
        if (lineEl == null) {
            return true;
        }
        
        int napPos = nap.getPosition();
        int napEnd = nap.getEnd();
        if (napPos > lineEl.getStartOffset() && napEnd > lineEl.getEndOffset()) {
            // The node officially starts on this line, but might have no text on this
            // line. In that case, we probably want to move its start down to the next line.
            if (napPos >= lineEl.getEndOffset()) {
                return true;
            }
            int nws = findNonWhitespaceComment(nap, lineEl, segment, napPos - lineEl.getStartOffset());
            if (nws == -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether a node which overlaps a line of the document actually finishes on the
     * previous line, by way of not having any actual text on this line. Return true if
     * so.
     */
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
     * If the node isn't present on the line, returns Integer.MAX_VALUE. A cached value
     * is used if available.
     */
    private int getNodeIndent(Shape a, MoeSyntaxDocument doc, NodeAndPosition<ParsedNode> nap, Element lineEl,
            Segment segment)
        throws BadLocationException
    {

        if (lineEl == null) {
            return Integer.MAX_VALUE;
        }

        int napPos = nap.getPosition();
        int napEnd = nap.getEnd();
        
        if (napPos >= lineEl.getEndOffset()) {
            return Integer.MAX_VALUE;
        }

        if (napEnd <= lineEl.getStartOffset()) {
            return Integer.MAX_VALUE;
        }

        if (nodeSkipsStart(nap, lineEl, segment)
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
            // In this case, we'll stretch the border to the regular indent only if
            // we can do it without hitting non-whitespace (which must belong to another node).
            int nws = findNonWhitespaceBwards(segment, napPos - lineEl.getStartOffset() - 1, 0);
            if (nws != -1) {
                Rectangle lbounds = modelToView(lineEl.getStartOffset() + nws + 1, a,
                        Position.Bias.Forward).getBounds();
                xpos = Math.max(xpos, lbounds.x);
            }
        }

        return xpos;
    }

    /**
     * Calculate the indent for a node.
     */
    private int getNodeIndent(Shape a, MoeSyntaxDocument doc, NodeAndPosition<ParsedNode> nap)
    {
        
        try {
            int indent = Integer.MAX_VALUE;

            int curpos = nap.getPosition();
            int napEnd = nap.getEnd();

            Element map = doc.getDefaultRootElement();
            Stack<NodeAndPosition<ParsedNode>> scopeStack = new Stack<NodeAndPosition<ParsedNode>>();
            scopeStack.add(nap);

            outer:
            while (curpos < napEnd) {
                // Remove any nodes from the scope stack who we have now skipped over
                NodeAndPosition<ParsedNode> top = scopeStack.get(scopeStack.size() - 1);
                while (top.getEnd() <= curpos) {
                    scopeStack.remove(scopeStack.size() - 1);
                    top = scopeStack.get(scopeStack.size() - 1);
                }
                
                // Re-build the scope stack and skip inner nodes.
                // Note, we find nodes at curpos + 1 to avoid nodes which *end* here.
                NodeAndPosition<ParsedNode> nextChild = top.getNode().findNodeAt(curpos + 1, top.getPosition());
                while (nextChild != null) {
                    if (nextChild.getNode().isInner()) {
                        curpos = nextChild.getEnd();
                        continue outer;
                    }
                    scopeStack.add(nextChild);
                    top = nextChild;
                    nextChild = top.getNode().findNodeAt(curpos + 1, top.getPosition());
                }
                
                // Ok, we've skipped inner nodes
                int line = map.getElementIndex(curpos);
                Element lineEl = map.getElement(line);
                Segment segment = new Segment();
                doc.getText(lineEl.getStartOffset(), lineEl.getEndOffset() - lineEl.getStartOffset(), segment);

                int lineOffset = curpos - lineEl.getStartOffset();

                int nws;
                if (lineEl.getStartOffset() < nap.getPosition() && nap.getNode().isInner()) {
                    // The node is an inner node starting on this line
                    nws = findNonWhitespaceComment(nap, lineEl, segment, lineOffset);
                } else {
                    nws = findNonWhitespace(segment, lineOffset);
                }

                if (nws == lineOffset) {
                    // Ok, at this position we have non-white space and are not in an inner
                    Rectangle cbounds = modelToView(curpos, a, Position.Bias.Forward).getBounds();
                    indent = Math.min(indent, cbounds.x);
                    curpos = lineEl.getEndOffset();
                }
                else if (nws == -1) {
                    curpos = lineEl.getEndOffset();
                }
                else {
                    // We need to check for inner nodes at the adjusted position
                    curpos += nws - lineOffset;
                }
            }

            return indent == Integer.MAX_VALUE ? -1 : indent;
        }
        catch (BadLocationException ble) {
            return -1;
        }
    }
    
    private int[] reassessIndentsAdd(Shape a, int dmgStart, int dmgEnd)
    {
        MoeSyntaxDocument doc = (MoeSyntaxDocument) getDocument();
        ParsedCUNode pcuNode = doc.getParsedNode();
        if (pcuNode == null) {
            return new int[] {dmgStart, dmgEnd};
        }
        
        Element map = doc.getDefaultRootElement();
        int ls = map.getElementIndex(dmgStart);
        int le = map.getElementIndex(dmgEnd);
        Segment segment = new Segment();
        
        try {
            int [] dmgRange = new int[2];
            dmgRange[0] = dmgStart;
            dmgRange[1] = dmgEnd;

            int i = ls;
            List<NodeAndPosition<ParsedNode>> scopeStack = new LinkedList<NodeAndPosition<ParsedNode>>();
            int lineEndPos = map.getElement(le).getEndOffset();
            Element lineEl = map.getElement(ls);
            NodeAndPosition<ParsedNode> top =
                pcuNode.findNodeAtOrAfter(lineEl.getStartOffset(), 0);
            while (top != null && top.getEnd() == lineEl.getStartOffset()) {
                top = top.nextSibling();
            }
            
            if (top == null) {
                // No nodes at all.
                return dmgRange;
            }
            if (top.getPosition() >= lineEl.getEndOffset()) {
                // The first node we found begins on a line following the additions.
                i = map.getElementIndex(top.getPosition());
                if (i > le) {
                    return dmgRange;
                }
            }
            
            scopeStack.add(top);
            NodeAndPosition<ParsedNode> nap = top.getNode().findNodeAtOrAfter(lineEl.getStartOffset() + 1,
                    top.getPosition());
            while (nap != null) {
                scopeStack.add(nap);
                nap = nap.getNode().findNodeAtOrAfter(lineEl.getStartOffset() + 1, nap.getPosition());                
            }
            
            outer:
            while (true) {
                // Skip to the next line which has text on it
                doc.getText(lineEl.getStartOffset(), lineEl.getEndOffset() - lineEl.getStartOffset(), segment);
                int nws = findNonWhitespace(segment, 0);
                while (nws == -1) {
                    if (++i > le) {
                        break outer;
                    }
                    lineEl = map.getElement(i);
                    doc.getText(lineEl.getStartOffset(),
                            lineEl.getEndOffset() - lineEl.getStartOffset(), segment);
                    nws = findNonWhitespace(segment, 0);
                }

                // Remove from the stack nodes which we've gone past
                int curpos = lineEl.getStartOffset() + nws;
                ListIterator<NodeAndPosition<ParsedNode>> j = scopeStack.listIterator(scopeStack.size());
                NodeAndPosition<ParsedNode> topNap = null;
                do {
                    nap = j.previous();
                    if (nap.getEnd() > curpos) {
                        break;
                    }
                    topNap = nap;
                    j.remove();
                } while (j.hasPrevious());

                if (topNap != null) {
                    // Rebuild the scope stack
                    do {
                        topNap = topNap.nextSibling();
                    } while (topNap != null && topNap.getEnd() <= curpos);
                    while (topNap != null && topNap.getPosition() < lineEndPos) {
                        scopeStack.add(topNap);
                        topNap = topNap.getNode().findNodeAtOrAfter(curpos + 1, topNap.getPosition());
                    }
                }
                
                if (scopeStack.isEmpty()) {
                    break;
                }
                
                // At this point:
                // - curpos is the position of the first non-whitespace on the current line (it may be
                //   prior to damageStart, but in that case it will be on the same line)
                // - i is the current line index
                // - lineEl is the current line element
                // - segment contains the text of the current line
                // - scopeStack contains a stack of elements which overlap or follow curpos, and
                //   which start on or before the current line.

                // Calculate/store indent
                Rectangle cbounds = modelToView(lineEl.getStartOffset() + nws, a, Position.Bias.Forward).getBounds();
                int indent = cbounds.x;
                for (j = scopeStack.listIterator(scopeStack.size()); j.hasPrevious(); ) {
                    NodeAndPosition<ParsedNode> next = j.previous();
                    if (next.getPosition() <= curpos) {
                        // Node is present on this line (begins before curpos)
                        updateNodeIndent(next, indent, nodeIndents.get(next.getNode()), dmgRange);
                    }
                    else if (next.getPosition() < lineEl.getEndOffset()) {
                        // Node starts on this line, after curpos.
                        nws = findNonWhitespace(segment, next.getPosition() - lineEl.getStartOffset());
                        Integer oindent = nodeIndents.get(next.getNode());
                        if (oindent != null && nws != -1) {
                            cbounds = modelToView(lineEl.getStartOffset() + nws, a, Position.Bias.Forward).getBounds();
                            indent = cbounds.x;
                            updateNodeIndent(next, indent, oindent, dmgRange);
                        }
                    }
                    else {
                        // Node isn't on this line.
                        continue;
                    }
                    
                    // Inner nodes are skipped during indent calculation
                    if (next.getNode().isInner()) {
                        break;
                    }
                }

                // Process subsequent nodes which are also on this line
                j = scopeStack.listIterator(scopeStack.size());
                while (j.hasPrevious()) {
                    nap = j.previous();
                    if (nap.getEnd() > lineEl.getEndOffset()) {
                        break;
                    }
                    // Node ends this line and may have siblings
                    nap = nap.nextSibling();
                    j.remove();
                    if (nap != null) {
                        do {
                            scopeStack.add(nap);
                            if (nap.getPosition() < lineEl.getEndOffset()) {
                                int spos = nap.getPosition() - lineEl.getStartOffset();
                                nws = findNonWhitespace(segment, spos);
                                Integer oindent = nodeIndents.get(nap.getNode());
                                if (oindent != null && nws != -1) {
                                    cbounds = modelToView(lineEl.getStartOffset() + nws, a, Position.Bias.Forward).getBounds();
                                    indent = cbounds.x;
                                    updateNodeIndent(nap, indent, oindent, dmgRange);
                                }
                            }
                            nap = nap.getNode().findNodeAtOrAfter(nap.getPosition(), nap.getPosition());
                        }
                        while (nap != null);
                        j = scopeStack.listIterator(scopeStack.size());
                    }
                }

                // Move on to the next line
                if (++i > le) {
                    break;
                }
                lineEl = map.getElement(i);
            }
            
            return dmgRange;
        }
        catch (BadLocationException ble) {
            throw new RuntimeException(ble);
        }
    }

    private int[] reassessIndentsRemove(Shape a, int dmgPoint, boolean multiLine)
    {
        MoeSyntaxDocument doc = (MoeSyntaxDocument) getDocument();
        ParsedCUNode pcuNode = doc.getParsedNode();
        
        int [] dmgRange = new int[2];
        dmgRange[0] = dmgPoint;
        dmgRange[1] = dmgPoint;
        
        if (pcuNode == null) {
            return dmgRange;
        }
        
        Element map = doc.getDefaultRootElement();
        int ls = map.getElementIndex(dmgPoint);
        Element lineEl = map.getElement(ls);

        NodeAndPosition<ParsedNode> top =
            pcuNode.findNodeAtOrAfter(lineEl.getStartOffset(), 0);
        while (top != null && top.getEnd() == lineEl.getStartOffset()) {
            top = top.nextSibling();
        }
        
        if (top == null) {
            // No nodes at all.
            return dmgRange;
        }

        if (top.getPosition() >= lineEl.getEndOffset()) {
            // The first node we found is on the next line.
            return dmgRange;
        }
        
        try {
            // At this point lineEl/segment are the line containing the deletion point. Some lines beyond
            // this point may have been removed (if multiLine true).
            Segment segment = new Segment();
            doc.getText(lineEl.getStartOffset(),
                    lineEl.getEndOffset() - lineEl.getStartOffset(), segment);
            
            // All nodes for this line with a cached indent greater than or equal to the damage point
            // indent should have their indents re-assessed: If the indent of the node on this line is
            // lower than (or the same as) the cached indent, it becomes the new cached indent; otherwise
            // the cached indent must be discarded.
            // Except: if the node does not span the damage point, its cached indent need not be discarded,
            //   since in that case the node indent cannot have increased.

            List<NodeAndPosition<ParsedNode>> rscopeStack = new LinkedList<NodeAndPosition<ParsedNode>>();
            getScopeStackAfter(doc.getParsedNode(), 0, dmgPoint, rscopeStack);
            rscopeStack.remove(0); // remove the root node

            boolean doContinue = true;

            Rectangle cbounds = modelToView(dmgPoint, a, Position.Bias.Forward).getBounds();
            int dpI = cbounds.x; // damage point indent

            while (doContinue && ! rscopeStack.isEmpty()) {
                NodeAndPosition<ParsedNode> rtop = rscopeStack.remove(rscopeStack.size() - 1);
                while (rtop != null && rtop.getPosition() < lineEl.getEndOffset()) {
                    if (rtop.getPosition() <= dmgPoint && rtop.getEnd() >= lineEl.getEndOffset()) {
                        // Content of inner nodes can't affect containing nodes:
                        doContinue &= ! rtop.getNode().isInner();
                    }

                    Integer cachedIndent = nodeIndents.get(rtop.getNode());
                    if (cachedIndent == null) {
                        rtop = rtop.nextSibling();
                        continue;
                    }

                    // If the cached indent is smaller than the damage point indent, then it
                    // is still valid - unless this is a multiple line remove.
                    if (!multiLine && cachedIndent < dpI) {
                        rtop = rtop.nextSibling();
                        continue;
                    }

                    if (nodeSkipsStart(rtop, lineEl, segment)) {
                        if (rtop.getPosition() <= dmgPoint) {
                            // The remove may have made this line empty
                            nodeIndents.remove(rtop.getNode());
                            dmgRange[0] = Math.min(dmgRange[0], rtop.getPosition());
                            dmgRange[1] = Math.max(dmgRange[1], rtop.getEnd());
                        }
                        break; // no more siblings can be on this line
                    }

                    int nwsP = Math.max(lineEl.getStartOffset(), rtop.getPosition());
                    int nws = findNonWhitespace(segment, nwsP - lineEl.getStartOffset());
                    if (nws == -1 || nws + lineEl.getStartOffset() >= rtop.getEnd()) {
                        // Two separate cases which we can handle in the same manner.
                        if (rtop.getPosition() <= dmgPoint) {
                            // The remove may have made this line empty
                            nodeIndents.remove(rtop.getNode());
                            dmgRange[0] = Math.min(dmgRange[0], rtop.getPosition());
                            dmgRange[1] = Math.max(dmgRange[1], rtop.getEnd());
                        }

                        rtop = rtop.nextSibling();
                        continue;
                    }

                    cbounds = modelToView(nws + lineEl.getStartOffset(), a, Position.Bias.Forward).getBounds();
                    int newIndent = cbounds.x;

                    if (newIndent < cachedIndent) {
                        nodeIndents.put(rtop.getNode(), newIndent);
                        dmgRange[0] = Math.min(dmgRange[0], rtop.getPosition());
                        dmgRange[1] = Math.max(dmgRange[1], rtop.getEnd());
                    }
                    else if (newIndent > cachedIndent) {
                        if (rtop.getPosition() <= dmgPoint) {
                            nodeIndents.remove(rtop.getNode());
                            dmgRange[0] = Math.min(dmgRange[0], rtop.getPosition());
                            dmgRange[1] = Math.max(dmgRange[1], rtop.getEnd());
                        }
                    }

                    rtop = rtop.nextSibling();
                }
            }
            
            return dmgRange;
        }
        catch (BadLocationException ble) {
            throw new RuntimeException(ble);
        }
    }
    
    /**
     * Update an existing indent, in the case where we have found a line where the indent
     * may now be smaller due to an edit.
     * @param nap    The node whose cached indent value is to be updated
     * @param indent   The indent, on some line
     * @param oindent  The old indent value (may be null)
     * @param dmgRange  The range of positions which must be repainted. This is updated by
     *                  if necessary.
     */
    private void updateNodeIndent(NodeAndPosition<ParsedNode> nap, int indent, Integer oindent, int [] dmgRange)
    {
        int dmgStart = dmgRange[0];
        int dmgEnd = dmgRange[1];
        
        if (oindent != null) {
            int noindent = oindent;
            if (indent < noindent) {
                nodeIndents.put(nap.getNode(), indent);
            }
            else if (indent != noindent) {
                nodeIndents.remove(nap.getNode());
            }
            if (indent != noindent) {
                dmgStart = Math.min(dmgStart, nap.getPosition());
                dmgEnd = Math.max(dmgEnd, nap.getEnd());
                dmgRange[0] = dmgStart;
                dmgRange[1] = dmgEnd;
            }
        }
    }
    
    /**
     * Get a stack of ParsedNodes which overlap or follow a particular document position. The stack shall
     * contain the outermost node (at the bottom of the stack) through to the innermost node which overlaps
     * (but does not end at) or which is the node first following the specified position.
     * 
     * @param root     The root node
     * @param rootPos  The position of the root node
     * @param position The position for which to build the scope stack
     * @param list     The list into which to store the stack. Items are added to the end of the list.
     */
    private void getScopeStackAfter(ParsedNode root, int rootPos, int position, List<NodeAndPosition<ParsedNode>> list)
    {
        // Note we add 1 to the given position to skip nodes which actually end at the position,
        // or which are zero size.
        list.add(new NodeAndPosition<ParsedNode>(root, 0, root.getSize()));
        int curpos = rootPos;
        NodeAndPosition<ParsedNode> nap = root.findNodeAtOrAfter(position + 1, curpos);
        while (nap != null) {
            list.add(nap);
            curpos = nap.getPosition();
            nap = nap.getNode().findNodeAtOrAfter(position + 1, curpos);
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
     * Search for a non-whitespace character, starting from the given offset in the segment; treat
     * single-line comments as whitespace. Returns -1 if the line consists only of whitespace.
     */
    private int findNonWhitespaceComment(NodeAndPosition<ParsedNode> nap, Element lineEl, Segment segment, int startPos)
    {
        int nws = findNonWhitespace(segment, startPos);
        if (nws != -1) {
            int pos = nws + lineEl.getStartOffset();
            
            if (nap.getEnd() > pos) {
                NodeAndPosition<ParsedNode> inNap = nap.getNode().findNodeAt(pos, nap.getPosition());
                if (inNap != null && inNap.getNode().getNodeType() == ParsedNode.NODETYPE_COMMENT
                        && inNap.getPosition() == pos && inNap.getEnd() == lineEl.getEndOffset() - 1) {
                    return -1;
                }
            }
            else {
                NodeAndPosition<ParsedNode> nnap = nap.nextSibling();
                if (nnap != null && nnap.getNode().getNodeType() == ParsedNode.NODETYPE_COMMENT
                        && nnap.getPosition() == pos && nnap.getEnd() == lineEl.getEndOffset() - 1) {
                    return -1;
                }
            }
        }
        return nws;
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
        return endPos - 1;
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

    /*
     * Need to override this method to handle node updates. If a node indentation changes,
     * the whole node needs to be repainted.
     */
    @Override
    protected void updateDamage(DocumentEvent changes, Shape a, ViewFactory f)
    {
        if (a == null) {
            // We have no shape. One cause might be that the editor is not visible.
            nodeIndents.clear();
            return;
        }
        
        MoeSyntaxDocument document = (MoeSyntaxDocument) getDocument();
        
        int damageStart = document.getLength();
        int damageEnd = 0;

        if (changes instanceof MoeSyntaxEvent) {
            MoeSyntaxEvent mse = (MoeSyntaxEvent) changes;
            for (NodeAndPosition<ParsedNode> node : mse.getRemovedNodes()) {
                nodeRemoved(node.getNode());
                damageStart = Math.min(damageStart, node.getPosition());
                damageEnd = Math.max(damageEnd, node.getEnd());
                NodeAndPosition<ParsedNode> nap = node;
                
                int [] r = clearNap(nap, document, damageStart, damageEnd);
                damageStart = r[0];
                damageEnd = r[1];
            }
            
            for (NodeChangeRecord record : mse.getChangedNodes()) {
                NodeAndPosition<ParsedNode> nap = record.nap;
                nodeIndents.remove(nap.getNode());
                damageStart = Math.min(damageStart, nap.getPosition());
                damageStart = Math.min(damageStart, record.originalPos);
                damageEnd = Math.max(damageEnd, nap.getEnd());
                damageEnd = Math.max(damageEnd,record.originalPos + record.originalSize);
                
                int [] r = clearNap(nap, document, damageStart, damageEnd);
                damageStart = r[0];
                damageEnd = r[1];
            }
        }

        Component host = getContainer();
        if (host == null) {
            return;
        }
        Element map = getElement();

        if (changes.getType() == EventType.INSERT) {
            damageStart = Math.min(damageStart, changes.getOffset());
            damageEnd = Math.max(damageEnd, changes.getOffset() + changes.getLength());
            int [] r = reassessIndentsAdd(a, damageStart, damageEnd);
            damageStart = r[0];
            damageEnd = r[1];
        }
        else if (changes.getType() == EventType.REMOVE) {
            damageStart = Math.min(damageStart, changes.getOffset());
            ElementChange ec = changes.getChange(document.getDefaultRootElement());
            boolean multiLine = ec != null;
            int [] r = reassessIndentsRemove(a, damageStart, multiLine);
            damageStart = r[0];
            damageEnd = r[1];
        }
        
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

    /**
     * Clear a node's cached indent information. If the node is an inner node this
     * also clears parent nodes as appropriate.
     */
    private int[] clearNap(NodeAndPosition<ParsedNode> nap, MoeSyntaxDocument document,
            int damageStart, int damageEnd)
    {
        if (nap.getNode().isInner()) {

            List<NodeAndPosition<ParsedNode>> list = new LinkedList<NodeAndPosition<ParsedNode>>();
            NodeAndPosition<ParsedNode> top;
            top = new NodeAndPosition<ParsedNode>(document.getParsedNode(), 0, document.getLength());
            while (top != null && top.getNode() != nap.getNode()) {
                if (top.getNode().isInner()) {
                    list.clear();
                }
                list.add(top);
                top = top.getNode().findNodeAt(nap.getEnd(), top.getPosition());
            }

            for (NodeAndPosition<ParsedNode> cnap : list)
            {
                damageStart = Math.min(damageStart, cnap.getPosition());
                damageEnd = Math.max(damageEnd, cnap.getEnd());
                nodeIndents.remove(cnap.getNode());
            }
        }
        
        return new int[] {damageStart, damageEnd};
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
     * given strength value. The higher the strength value, the less the colour
     * is faded.
     */
    public static Color getReducedColor(int r, int g, int b, int strength)
    {
        Color bg = MoeSyntaxDocument.getBackgroundColor();
        double factor = strength / (float) ScopeHighlightingPrefDisplay.MAX;
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
     *  Return the pink wash colour
     *  modified to become less strong (until white) based on the 'strength' value
     */
    private static Color getPinkWash()
    {
        return getReducedColor(PINK_BASE);
    }
}
