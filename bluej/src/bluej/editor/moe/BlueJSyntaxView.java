/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2014,2015,2016,2017  Michael Kolling and John Rosenberg

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

import bluej.Config;
import bluej.editor.moe.MoeSyntaxDocument.Element;
import bluej.editor.moe.MoeSyntaxEvent.NodeChangeRecord;
import bluej.editor.moe.Token.TokenType;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import bluej.prefmgr.PrefMgr;
import bluej.utility.javafx.FXCache;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.JavaFXUtil;
import javafx.application.Platform;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.Map.Entry;
import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;

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
public class BlueJSyntaxView
{
    /** (NaviView) Paint method inner scope? if false, whole method will be highlighted as a single block */
    private static final boolean PAINT_METHOD_INNER = false;

    private static final int LEFT_INNER_SCOPE_MARGIN = 5;
    private static final int LEFT_OUTER_SCOPE_MARGIN = 2;
    private static final int RIGHT_SCOPE_MARGIN = 4;
    private static final int CURVED_CORNER_SIZE = 4;
    // See comments in getImageFor for more info.
    // 1 means draw edge, 2 means draw filling
    private static final int[][] CORNER_TEMPLATE = new int[][] {
            {0, 0, 1, 1},
            {0, 1, 2, 2},
            {1, 2, 2, 2},
            {1, 2, 2, 2}
    };
    private final MoeSyntaxDocument document;
    private final FXCache<ScopeInfo, Image> imageCache;
    private final ScopeColors scopeColors;
    private int imageCacheLineHeight;
    private ReadOnlyDoubleProperty widthProperty; // width of editor view
    private MoeEditorPane editorPane;

    /* Scope painting colours */
    /* The following are initialized by resetColors() */
    private Color BK; // background

    private Color C1; // green border (container)
    private Color C2; // green wash
    private Color C3; // green border (inner).

    private Color M1; // yellow border (methods)
    private Color M2; // yellow wash

    private Color S1; // blue border (selection)
    private Color S2; // blue wash

    private Color I1; // pink border (iteration)
    private Color I2; // pink wash

    public static enum ParagraphAttribute
    {
        STEP_MARK("bj-step-mark"), BREAKPOINT("bj-breakpoint"), ERROR("bj-error"), ERROR_ABOVE("bj-error-above"), ERROR_BELOW("bj-error-below");

        private final String pseudoClass;

        ParagraphAttribute(String pseudoClass)
        {
            this.pseudoClass = pseudoClass;
        }

        public String getPseudoclass()
        {
            return pseudoClass;
        }
    }

    // The line numbers in both maps start at one:
    private final Map<Integer, EnumSet<ParagraphAttribute>> paragraphAttributes = new HashMap<>();
    private final Map<Integer, FXPlatformConsumer<EnumSet<ParagraphAttribute>>> paragraphAttributeListeners = new HashMap<>();

    /**
     * Cached indents for ParsedNode items.  Maps a node to an indent (in pixels)
     * When this is zero, it means it is not yet fully valid as the editor has not
     * yet appeared on screen.
     */
    private final Map<ParsedNode,Integer> nodeIndents = new HashMap<ParsedNode,Integer>();

    /**
     * Creates a new BlueJSyntaxView.
     */
    public BlueJSyntaxView(MoeSyntaxDocument document, ScopeColors scopeColors)
    {
        this.document = document;
        this.imageCache = new FXCache<>(s -> drawImageFor(s, imageCacheLineHeight), 200);
        this.scopeColors = scopeColors;
        resetColors();
        JavaFXUtil.addChangeListenerPlatform(PrefMgr.getScopeHighlightStrength(), str -> {
            resetColors();
            imageCache.clear();
            document.recalculateAllScopes();
        });
        // We use class color as a proxy for listening to all colors:
        JavaFXUtil.addChangeListenerPlatform(scopeColors.scopeClassColorProperty(), str -> {
            // runLater to make sure all colours have been set:
            Platform.runLater(() ->
            {
                resetColors();
                imageCache.clear();
                document.recalculateAllScopes();
            });
        });
    }

    /**
     * Gets the syntax token styles for a given line of code.
     *
     * Returns null if there are no styles to apply (e.g. on a blank line or one with only whitespace).
     */
    protected final StyleSpans<List<String>> getTokenStylesFor(int lineIndex, MoeSyntaxDocument document)
    {
        StyleSpansBuilder<List<String>> lineStyle = new StyleSpansBuilder<>();
        Token tokens = document.getTokensForLine(lineIndex);
        boolean addedAny = false;
        for(;;) {
            TokenType id = tokens.id;
            if(id == TokenType.END)
                break;

            lineStyle.add(Collections.singletonList(id.getCSSClass()), tokens.length);
            addedAny = true;

            tokens = tokens.next;
        }
        if (addedAny)
            return lineStyle.create();
        else
            return null;
    }

    protected final void paintScopeMarkers(List<ScopeInfo> scopes, MoeSyntaxDocument document, int fullWidth,
            int firstLine, int lastLine, boolean onlyMethods)
    {
        paintScopeMarkers(scopes, document, fullWidth, firstLine, lastLine, onlyMethods, false);
    }

    public List<ScopeInfo> recalculateScopes(MoeSyntaxDocument moeSyntaxDocument, int firstLineIncl, int lastLineIncl)
    {
        List<ScopeInfo> scopes = new ArrayList<>();
        paintScopeMarkers(scopes, moeSyntaxDocument, widthProperty == null  || widthProperty.get() == 0 ? 200 : (int)widthProperty.get(), firstLineIncl, lastLineIncl, false);
        return scopes;
    }

    public Image getImageFor(ScopeInfo s, int lineHeight)
    {
        if (lineHeight == 0)
        {
            return new WritableImage(1, 1);
        }

        // Many of the images we use will be duplicated, e.g. for multiple lines in the same body of a block
        // So we keep them in a cache to save unnecessary effort drawing new line backgrounds:
        if (lineHeight != imageCacheLineHeight)
        {
            imageCache.clear();
            imageCacheLineHeight = lineHeight;
        }
        return imageCache.get(s);
    }

    private Image drawImageFor(ScopeInfo s, int lineHeight)
    {
        WritableImage image = new WritableImage(s.nestedScopes.stream().mapToInt(n -> n.leftRight.rhs + 1).max().orElse(1) + 1, lineHeight);

        for (ScopeInfo.SingleNestedScope singleNestedScope : s.nestedScopes)
        {
            LeftRight leftRight = singleNestedScope.leftRight;
            int sideTopMargin = leftRight.starts ? CURVED_CORNER_SIZE : 0;
            int sideBottomMargin = leftRight.ends ? CURVED_CORNER_SIZE : 0;
            fillRect(image.getPixelWriter(), leftRight.lhs, 0 + sideTopMargin, leftRight.padding, lineHeight - sideBottomMargin - sideTopMargin, leftRight.fillColor);
            for (int y = sideTopMargin; y < lineHeight - sideBottomMargin; y++)
            {
                image.getPixelWriter().setColor(leftRight.lhs, y, leftRight.edgeColor);
            }

            // I realise it seems crazy to be drawing the curved corners manually here.  But
            // the JavaFX PixelWriter class for writing directly to an image has no support for anything
            // better than setting individual pixels.  It would be possible to create a Canvas
            // and fill an arc and then pull a snapshot of the Canvas into an image, but getting right
            // things like HiDPI would be difficult.  So it is the simplest solution to just
            // draw a simple curved corner ourselves:

            if (leftRight.starts)
            {
                for (int x = 0; x < CURVED_CORNER_SIZE; x++)
                {
                    for (int y = 0; y < CURVED_CORNER_SIZE; y++)
                    {
                        if (CORNER_TEMPLATE[y][x] == 1)
                            image.getPixelWriter().setColor(leftRight.lhs + x, y, leftRight.edgeColor);
                        else if (CORNER_TEMPLATE[y][x] == 2)
                            image.getPixelWriter().setColor(leftRight.lhs + x, y, leftRight.fillColor);
                    }
                }
            }
            if (leftRight.ends)
            {
                for (int x = 0; x < CURVED_CORNER_SIZE; x++)
                {
                    for (int y = 0; y < CURVED_CORNER_SIZE; y++)
                    {
                        if (CORNER_TEMPLATE[y][x] == 1)
                            image.getPixelWriter().setColor(leftRight.lhs + x, lineHeight - 1 - y, leftRight.edgeColor);
                        else if (CORNER_TEMPLATE[y][x] == 2)
                            image.getPixelWriter().setColor(leftRight.lhs + x, lineHeight - 1 - y, leftRight.fillColor);
                    }
                }
            }


            Middle middle = singleNestedScope.middle;

            fillRect(image.getPixelWriter(), middle.lhs, 0, middle.rhs - middle.lhs, lineHeight, middle.bodyColor);

            if (middle.topColor != null)
            {
                for (int x = middle.lhs; x < middle.rhs; x++)
                {
                    image.getPixelWriter().setColor(x, 0, middle.topColor);
                }
            }

            if (middle.bottomColor != null)
            {
                for (int x = middle.lhs; x < middle.rhs; x++)
                {
                    image.getPixelWriter().setColor(x, lineHeight - 1, middle.bottomColor);
                }
            }


            // Right edge:
            fillRect(image.getPixelWriter(), leftRight.rhs - leftRight.padding, 0 + sideTopMargin, leftRight.padding, lineHeight - sideBottomMargin - sideTopMargin, leftRight.fillColor);
            for (int y = sideTopMargin; y < lineHeight - sideBottomMargin; y++)
            {
                image.getPixelWriter().setColor(leftRight.rhs, y, leftRight.edgeColor);
            }

            if (leftRight.starts && leftRight.rhs > CURVED_CORNER_SIZE)
            {
                for (int x = 0; x < CURVED_CORNER_SIZE; x++)
                {
                    for (int y = 0; y < CURVED_CORNER_SIZE; y++)
                    {
                        if (CORNER_TEMPLATE[y][x] == 1)
                            image.getPixelWriter().setColor(leftRight.rhs - x, y, leftRight.edgeColor);
                        else if (CORNER_TEMPLATE[y][x] == 2)
                            image.getPixelWriter().setColor(leftRight.rhs - x, y, leftRight.fillColor);
                    }
                }
            }
            if (leftRight.ends && leftRight.rhs > CURVED_CORNER_SIZE)
            {
                for (int x = 0; x < CURVED_CORNER_SIZE; x++)
                {
                    for (int y = 0; y < CURVED_CORNER_SIZE; y++)
                    {
                        if (CORNER_TEMPLATE[y][x] == 1)
                            image.getPixelWriter().setColor(leftRight.rhs - x, lineHeight - 1 - y, leftRight.edgeColor);
                        else if (CORNER_TEMPLATE[y][x] == 2)
                            image.getPixelWriter().setColor(leftRight.rhs - x, lineHeight - 1 - y, leftRight.fillColor);
                    }
                }
            }
        }

        if (s.isStepLine())
        {
            ScopeInfo.SingleNestedScope innermostScope = s.nestedScopes.get(s.nestedScopes.size() - 1);
            double leftSide = innermostScope.leftRight.lhs;
            double rightSide = innermostScope.leftRight.rhs;
            for (int x = 0; x < leftSide; x++)
            {
                image.getPixelWriter().setArgb(x, (int)(image.getHeight() / 2), 0xFF000000);
            }
            for (int x = (int)rightSide; x < image.getWidth(); x++)
            {
                image.getPixelWriter().setArgb(x, (int)(image.getHeight() / 2), 0xFF000000);
            }
        }

        return image;
    }

    private static void fillRect(PixelWriter pixelWriter, int x, int y, int w, int h, Color c)
    {
        // If we're trying to draw off the left-hand/top edge, just truncate the rectangles
        if (x < 0)
        {
            // If x is -4, we want to take +4 off the width:
            w -= -x;
            x = 0;
        }
        if (y < 0)
        {
            h -= -y;
            y = 0;
        }

        for (int i = 0; i < w; i++)
        {
            for (int j = 0; j < h; j++)
            {
                pixelWriter.setColor(x + i, y + j, c);
            }
        }
    }

    public void setEditorPane(MoeEditorPane editorPane)
    {
        this.editorPane = editorPane;
        this.widthProperty = editorPane.widthProperty();
        JavaFXUtil.addChangeListener(widthProperty, w -> {
            document.fireChangedUpdate(null);
        });
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

    protected void paintScopeMarkers(List<ScopeInfo> scopes, MoeSyntaxDocument document, int fullWidth,
            int firstLine, int lastLine, boolean onlyMethods, boolean small)
    {
        //optimization for the raspberry pi.
        //if (strength == 0) {
            //return;
        //}
        
        Element map = document.getDefaultRootElement();
        ParsedNode rootNode = document.getParsedNode();

        if (rootNode == null)
        {
            // Not initialised yet
            return;
        }

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

                ScopeInfo scope = new ScopeInfo(false);
                scopes.add(scope);
                drawScopes(fullWidth, scope, document, lines, prevScopeStack, small, onlyMethods, 0);

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
        final ScopeInfo scopes;
        ThreeLines lines;
        boolean small;

        ParsedNode node;
        boolean starts;  // the node starts on the current line
        boolean ends;    // the node ends on the current line
        Color color1;    // Edge colour
        Color color2;    // Fill colour

        private DrawInfo(ScopeInfo scopes)
        {
            this.scopes = scopes;
        }
    }

    /**
     * Draw the scope highlighting for one line of the document.
     * 
     * @param fullWidth      the width of the editor view
     * @param g              the graphics context to render to
     * @param document       the document
     * @param lines          the previous, current and next lines (segments and elements)
     * @param prevScopeStack the stack of nodes (from outermost to innermost) at the beginning of the current line
     */
    private void drawScopes(int fullWidth, ScopeInfo scopes, MoeSyntaxDocument document, ThreeLines lines,
            List<NodeAndPosition<ParsedNode>> prevScopeStack, boolean small,
            boolean onlyMethods, int nodeDepth)
    throws BadLocationException
    {
        int rightMargin = small ? 0 : 20;

        ListIterator<NodeAndPosition<ParsedNode>> li = prevScopeStack.listIterator();

        DrawInfo drawInfo = new DrawInfo(scopes);
        drawInfo.lines = lines;
        drawInfo.small = small;

        // Process the current scope stack. This contains all nodes that span the beginning of this line,
        // the foremost child and its foremost child and so on.
        while (li.hasNext()) {
            NodeAndPosition<ParsedNode> nap = li.next();
            int napPos = nap.getPosition();
            int napEnd = nap.getEnd();

            if (napPos >= lines.thisLineEl.getEndOffset()) {
                // The node isn't even on this line, go to the next line
                return;
            }

            if (! drawNode(drawInfo, nap, onlyMethods)) {
                continue;
            }

            if (nodeSkipsEnd(napPos, napEnd, lines.thisLineEl, lines.thisLineSeg)) {
                nodeDepth++;
                break;
            }

            // Draw the start node
            int xpos = getNodeIndent(document, nap, lines.thisLineEl,
                    lines.thisLineSeg);
            if (xpos != - 1 && xpos <= fullWidth) {
                boolean starts = nodeSkipsStart(nap, lines.aboveLineEl, lines.aboveLineSeg);
                boolean ends = nodeSkipsEnd(napPos, napEnd, lines.belowLineEl, lines.belowLineSeg);
                int rbound = getNodeRBound(nap, fullWidth - rightMargin, nodeDepth,
                        lines.thisLineEl, lines.thisLineSeg);

                drawInfo.node = nap.getNode();
                drawInfo.starts = starts;
                drawInfo.ends = ends;
                Color[] colors = colorsForNode(drawInfo.node);
                drawInfo.color1 = colors[0];
                drawInfo.color2 = colors[1];

                drawInfo.scopes.nestedScopes.add(calculatedNestedScope(drawInfo, xpos, rbound));
            }
            nodeDepth++;
        }

        // Move along.
        nodeDepth--;
        li = prevScopeStack.listIterator(prevScopeStack.size());
        NodeAndPosition<ParsedNode> nap = li.previous(); // last node
        int napPos = nap.getPosition();
        int napEnd = napPos + nap.getSize();

        // For nodes which end on this line, there may be subsequent nodes we
        // need to draw (and anyway we need to build the scope stack).
        while (napEnd <= lines.thisLineEl.getEndOffset()) {
            // Node ends this line
            li.remove();
            if (drawNode(drawInfo, nap, onlyMethods)) {
                nodeDepth--;
            }

            if (! li.hasPrevious()) return;
            NodeAndPosition<ParsedNode> napParent = li.previous();
            li.next();

            // There might be a sibling which has to be processed:
            NodeAndPosition<ParsedNode> nextNap = nap.nextSibling();
            napPos = napParent.getPosition();
            napEnd = napPos + napParent.getSize();
            nap = napParent;

            while (nextNap != null) {
                li.add(nextNap);
                li.previous(); li.next();  // so remove works
                napPos = nextNap.getPosition();
                napEnd = napPos + nextNap.getSize();
                
                if (napPos < lines.thisLineEl.getEndOffset() && ! nodeSkipsStart(nextNap, lines.thisLineEl, lines.thisLineSeg)) {
                    if (drawNode(drawInfo, nextNap, onlyMethods)) {
                        // Draw it
                        nodeDepth++;
                        int xpos = getNodeIndent(document, nextNap, lines.thisLineEl,
                                lines.thisLineSeg);
                        int rbound = getNodeRBound(nextNap, fullWidth - rightMargin, nodeDepth,
                                lines.thisLineEl, lines.thisLineSeg);
                        drawInfo.node = nextNap.getNode();
                        Color [] colors = colorsForNode(drawInfo.node);
                        drawInfo.color1 = colors[0];
                        drawInfo.color2 = colors[1];
                        drawInfo.starts = nodeSkipsStart(nextNap, lines.aboveLineEl,
                                lines.aboveLineSeg);
                        drawInfo.ends = nodeSkipsEnd(napPos, napEnd, lines.belowLineEl,
                                lines.belowLineSeg);

                        if (xpos != -1 && xpos <= fullWidth) {
                            drawInfo.scopes.nestedScopes.add(calculatedNestedScope(drawInfo, xpos, rbound));
                        }
                    }
                }
                
                nap = nextNap;
                nextNap = nextNap.getNode().findNodeAtOrAfter(napPos, napPos);
            }
        }
    }

    private int getLeftEdge(int startOffset)
    {
        if (editorPane == null)
            return 0;
        double x = editorPane.getCharacterBoundsOnScreen(startOffset, startOffset + 1).map(editorPane::screenToLocal).map(Bounds::getMinX).orElse(0.0);
        // Allow for the left-hand margin.  We don't let it go below zero
        // as zero is our special recalculate value meaning it's not initialised yet:
        x = Math.max(0, x - 24);
        return (int)x;
    }

    /**
     * Check whether a node needs to be drawn.
     * @param info
     * @param node
     * @return
     */
    private boolean drawNode(DrawInfo info, NodeAndPosition<ParsedNode> nap, boolean onlyMethods)
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
        }

        if (nodeSkipsStart(nap, info.lines.thisLineEl, info.lines.thisLineSeg)) {
            return false; // just white space on this line
        }

        return !nodeSkipsEnd(napPos, napEnd, info.lines.thisLineEl, info.lines.thisLineSeg);
    }

    private Color getBackgroundColor()
    {
        return BK;
    }

    /**
     * Get the scope highlighting colours for a given node.
     */
    private Color[] colorsForNode(ParsedNode node)
    {
        if (node.isInner()) {
            return new Color[] { C3, getBackgroundColor() };
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
    private ScopeInfo.SingleNestedScope calculatedNestedScope(DrawInfo info, int xpos, int rbound)
    {
        if (! info.small) {
            xpos -= info.node.isInner() ? LEFT_INNER_SCOPE_MARGIN : LEFT_OUTER_SCOPE_MARGIN;
        }

        // draw node start
        int hoffs = info.small ? 0 : CURVED_CORNER_SIZE; // determines size of corner arcs

        return new ScopeInfo.SingleNestedScope(
                new LeftRight(xpos, rbound, hoffs, info.starts, info.ends, info.color2, info.color1),
                getScopeMiddle(info, xpos + hoffs, rbound));
    }

    /**
     * Draw the center part of a scope (not the left or right edge, but the bit in between)
     * @param info  general drawing information
     * @param xpos  the leftmost x-coordinate to draw from
     * @param rbounds the rightmost x-coordinate to draw to
     */
    private Middle getScopeMiddle(DrawInfo info, int xpos, int rbounds)
    {
        Color color1 = info.color1;
        Color color2 = info.color2;
        boolean startsThisLine = info.starts;
        boolean endsThisLine = info.ends;

        Middle middle = new Middle(color2, xpos, rbounds - 1);
        if (startsThisLine)
        {
            middle.drawTop(color1);
        }
        if (endsThisLine)
        {
            middle.drawBottom(color1);
        }
        return middle;
    }

    /**
     * Find the rightmost bound of a node on a particular line.
     *
     * @param napEnd  The end of the node (position in the document just beyond the node)
     * @param fullWidth  The full width to draw to (for the outermost mode)
     * @param nodeDepth  The node depth
     * @param lineEl   line element of the line to find the bound for
     * @param lineSeg  Segment containing text of the current line
     */
    private int getNodeRBound(NodeAndPosition<ParsedNode> nap, int fullWidth, int nodeDepth,
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
            int eboundsX = getLeftEdge(napEnd);
            return Math.min(rbound, eboundsX);
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
    private int getNodeIndent(MoeSyntaxDocument doc, NodeAndPosition<ParsedNode> nap, Element lineEl,
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
        // An indent value of zero is only given by getCharacterBoundsOnScreen when the editor
        // hasn't been shown yet, so we recalculate whenever we find that indent value in the
        // hope that the editor is now visible:
        if (indent == null || indent == 0) {
            indent = getNodeIndent(doc, nap);
            nodeIndents.put(nap.getNode(), indent);
        }

        int xpos = indent;

        // Corner case: node start position is on this line, and is greater than the node indent?
        if (napPos > lineEl.getStartOffset()) {
            // In this case, we'll stretch the border to the regular indent only if
            // we can do it without hitting non-whitespace (which must belong to another node).
            int nws = findNonWhitespaceBwards(segment, napPos - lineEl.getStartOffset() - 1, 0);
            if (nws != -1) {
                int lboundsX = getLeftEdge(lineEl.getStartOffset() + nws + 1);
                xpos = Math.max(xpos, lboundsX);
            }
        }

        return xpos;
    }

    /**
     * Calculate the indent for a node.
     */
    private int getNodeIndent(MoeSyntaxDocument doc, NodeAndPosition<ParsedNode> nap)
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
                // Note, we find nodes at curpos + 1 to avoid nodes which *end* here, but we filter
                // out nodes which do not span curpos within the loop:
                NodeAndPosition<ParsedNode> nextChild = top.getNode().findNodeAt(curpos + 1, top.getPosition());
                while (nextChild != null) {
                    if (nextChild.getPosition() > curpos) break;
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
                    int cboundsX = getLeftEdge(curpos);
                    indent = Math.min(indent, cboundsX);
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
        } finally {}
        //catch (BadLocationException ble) {
        //    return -1;
        //}
    }
    
    private int[] reassessIndentsAdd(int dmgStart, int dmgEnd)
    {
        //MOEFX
        MoeSyntaxDocument doc = null;
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
                int cboundsX = getLeftEdge(lineEl.getStartOffset() + nws);
                int indent = cboundsX;
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
                            cboundsX = getLeftEdge(lineEl.getStartOffset() + nws);
                            indent = cboundsX;
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
                                    cboundsX = getLeftEdge(lineEl.getStartOffset() + nws);
                                    indent = cboundsX;
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
        } finally {}
        //catch (BadLocationException ble) {
        //    throw new RuntimeException(ble);
        //}
    }

    private int[] reassessIndentsRemove(int dmgPoint, boolean multiLine)
    {
        //MOEFX
        MoeSyntaxDocument doc = null;
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

            int cboundsX = getLeftEdge(dmgPoint);
            int dpI = cboundsX; // damage point indent

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

                    cboundsX = getLeftEdge(nws + lineEl.getStartOffset());
                    int newIndent = cboundsX;

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
        } finally {}
        //catch (BadLocationException ble) {
        //    throw new RuntimeException(ble);
        //}
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
        //MOEFX
        return false;
        //return Boolean.TRUE.equals(line.getAttributes().getAttribute(tag));
    }


    /*
     * Need to override this method to handle node updates. If a node indentation changes,
     * the whole node needs to be repainted.
     */
    protected void updateDamage(MoeSyntaxEvent changes)
    {
        if (changes == null) {
            // Width has changed, so do it all:
            nodeIndents.clear();
            document.recalculateAllScopes();
            return;
        }

        int damageStart = document.getLength();
        int damageEnd = 0;

        MoeSyntaxEvent mse = changes;
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


        Element map = document.getDefaultRootElement();
        //MOEFX work out where to move this to:
        /*
        if (changes.getType() == EventType.INSERT) {
            damageStart = Math.min(damageStart, changes.getOffset());
            damageEnd = Math.max(damageEnd, changes.getOffset() + changes.getLength());
            int [] r = reassessIndentsAdd(damageStart, damageEnd);
            damageStart = r[0];
            damageEnd = r[1];
        }
        else if (changes.getType() == EventType.REMOVE) {
            damageStart = Math.min(damageStart, changes.getOffset());
            int [] r = reassessIndentsRemove(damageStart, true); //TODO MOEFX changes.isMultilineChange()
            damageStart = r[0];
            damageEnd = r[1];
        }
        */
        
        if (damageStart < damageEnd) {
            int line = map.getElementIndex(damageStart);
            int lastline = map.getElementIndex(damageEnd - 1);
            document.recalculateScopesForLinesInRange(line, lastline);
        }

        /*MOEFX is this all handled ok by the code above?
        DocumentEvent.ElementChange ec = changes.getChange(map);
        Element[] added = (ec != null) ? ec.getChildrenAdded() : null;
        Element[] removed = (ec != null) ? ec.getChildrenRemoved() : null;
        if (((added != null) && (added.length > 0)) || 
                ((removed != null) && (removed.length > 0))) {
            // This case is handled Ok by the superclass.
            super.updateDamage(changes, a, f);
        } else*/ {
            // This is the case we have to fix. The PlainView implementation only
            // repaints a single line; we need to repaint the whole range.
            //super.updateDamage(changes);
            /*
            int choffset = changes.getOffset();
            int chlength = Math.max(changes.getLength(), 1);
            int line = map.getElementIndex(choffset);
            int lastline = map.getElementIndex(choffset + chlength - 1);
            damageLineRange(line, lastline);
            */
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

    @OnThread(Tag.FXPlatform)
    public Node getParagraphicGraphic(int lineNumber)
    {
        // RichTextFX numbers from 0, but javac numbers from 1:
        lineNumber += 1;
        Label label = new Label("" + lineNumber);
        JavaFXUtil.setPseudoclass("bj-odd", (lineNumber & 1) == 1, label);
        JavaFXUtil.addStyleClass(label, "moe-line-label");
        label.setOnContextMenuRequested(e -> {
            CheckMenuItem checkMenuItem = new CheckMenuItem(Config.getString("prefmgr.edit.displaylinenumbers"));
            checkMenuItem.setSelected(PrefMgr.getFlag(PrefMgr.LINENUMBERS));
            checkMenuItem.setOnAction(ev -> {
                PrefMgr.setFlag(PrefMgr.LINENUMBERS, checkMenuItem.isSelected());
            });
            ContextMenu menu = new ContextMenu(checkMenuItem);
            menu.show(label, e.getScreenX(), e.getScreenY());
        });
        int lineNumberFinal = lineNumber;
        label.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1 && e.getButton() == MouseButton.PRIMARY)
            {
                editorPane.getEditor().toggleBreakpoint(editorPane.getDocument().getAbsolutePosition(lineNumberFinal - 1, 0));
            }
            e.consume();
        });
        WeakReference<Label> weakLabel = new WeakReference<>(label);
        FXPlatformConsumer<EnumSet<ParagraphAttribute>> listener = attr -> {
            Label l = weakLabel.get();
            if (l != null)
            {
                for (ParagraphAttribute possibleAttribute : ParagraphAttribute.values())
                {
                    JavaFXUtil.setPseudoclass(possibleAttribute.getPseudoclass(), attr.contains(possibleAttribute), l);
                }
            }
            else
                paragraphAttributeListeners.remove(lineNumberFinal);
        };
        listener.accept(paragraphAttributes.getOrDefault(lineNumber, EnumSet.noneOf(ParagraphAttribute.class)));
        paragraphAttributeListeners.put(lineNumber, listener);
        return label;
    }


    /**
     * Sets attributes for a particular line number.
     *
     * @param offset the line number for which to change the attributes (first line is 1)
     * @param alterAttr the attributes to set the value for (other attributes will be unaffected)
     */
    public void setParagraphAttributes(int lineNumber, Map<ParagraphAttribute, Boolean> alterAttr)
    {
        EnumSet<ParagraphAttribute> attr = getParaAttr(lineNumber);
        for (Entry<ParagraphAttribute, Boolean> alter : alterAttr.entrySet())
        {
            if (alter.getValue())
            {
                attr.add(alter.getKey());
            }
            else
            {
                attr.remove(alter.getKey());
            }
        }
        if (alterAttr.containsKey(ParagraphAttribute.ERROR))
        {
            // Update above/below states by finding nearest error then pointing up
            // and down accordingly
            int totalLines = editorPane.getParagraphs().size();
            int[] errorLines = paragraphAttributes.entrySet().stream().filter(e -> e.getValue().contains(ParagraphAttribute.ERROR)).mapToInt(e -> e.getKey()).sorted().toArray();
            for (int i = 1; i <= totalLines;i++)
            {
                int nearest = Arrays.binarySearch(errorLines, i);
                if (nearest >= 0)
                {
                    // Actually an error; remove above/below designation:
                    getParaAttr(i).removeAll(Arrays.asList(ParagraphAttribute.ERROR_ABOVE, ParagraphAttribute.ERROR_BELOW));
                }
                else
                {
                    // Returns insertion point - 1 if not found:
                    nearest = -nearest - 1;
                    // Is nearest one above or below?
                    boolean nearestIsBelow = nearest <= 0 || (nearest < errorLines.length && (errorLines[nearest] - i) < (i - errorLines[nearest - 1]));
                    getParaAttr(i).remove(nearestIsBelow ? ParagraphAttribute.ERROR_ABOVE : ParagraphAttribute.ERROR_BELOW);
                    getParaAttr(i).add(nearestIsBelow ? ParagraphAttribute.ERROR_BELOW : ParagraphAttribute.ERROR_ABOVE);
                }
            }
        }


        FXPlatformConsumer<EnumSet<ParagraphAttribute>> listener = paragraphAttributeListeners.get(lineNumber);
        if (listener != null)
        {
            listener.accept(attr);
        }
    }

    /**
     * Gets the paragraph attributes for a particular line.  If none found, returns the empty set.
     * The set is the live set in the map, so updating it will affect the stored attributes for the line.
     */
    private EnumSet<ParagraphAttribute> getParaAttr(int lineNumber)
    {
        return paragraphAttributes.computeIfAbsent(lineNumber, k -> EnumSet.noneOf(ParagraphAttribute.class));
    }

    /**
     * First line is one
     */
    public EnumSet<ParagraphAttribute> getParagraphAttributes(int lineNo)
    {
        return paragraphAttributes.getOrDefault(lineNo, EnumSet.noneOf(ParagraphAttribute.class));
    }

    /**
     * Sets up the colors based on the strength value 
     * (from strongest (20) to white (0)
     */
    private void resetColors()
    {
        BK = scopeColors.scopeBackgroundColorProperty().get();
        C1 = getReducedColor(scopeColors.scopeClassOuterColorProperty());
        C2 = getReducedColor(scopeColors.scopeClassColorProperty());
        C3 = getReducedColor(scopeColors.scopeClassInnerColorProperty());
        M1 = getReducedColor(scopeColors.scopeMethodOuterColorProperty());
        M2 = getReducedColor(scopeColors.scopeMethodColorProperty());
        S1 = getReducedColor(scopeColors.scopeSelectionOuterColorProperty());
        S2 = getReducedColor(scopeColors.scopeSelectionColorProperty());
        I1 = getReducedColor(scopeColors.scopeIterationOuterColorProperty());
        I2 = getReducedColor(scopeColors.scopeIterationColorProperty());
    }

    private Color getReducedColor(ObjectExpression<Color> c)
    {
        return scopeColors.getReducedColor(c, PrefMgr.getScopeHighlightStrength()).getValue();
    }

    private static class Middle
    {
        private final Color bodyColor;
        private final int lhs;
        private final int rhs;
        private Color topColor; // null if no top
        private Color bottomColor; // null if no bottom

        public Middle(Color bodyColor, int lhs, int rhs)
        {
            this.bodyColor = bodyColor;
            this.lhs = Math.max(0, lhs);
            this.rhs = rhs;
        }

        public void drawTop(Color topColor)
        {
            this.topColor = topColor;
        }

        public void drawBottom(Color bottomColor)
        {
            this.bottomColor = bottomColor;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Middle middle = (Middle) o;

            if (lhs != middle.lhs) return false;
            if (rhs != middle.rhs) return false;
            if (!bodyColor.equals(middle.bodyColor)) return false;
            if (topColor != null ? !topColor.equals(middle.topColor) : middle.topColor != null) return false;
            return bottomColor != null ? bottomColor.equals(middle.bottomColor) : middle.bottomColor == null;
        }

        @Override
        public int hashCode()
        {
            int result = bodyColor.hashCode();
            result = 31 * result + lhs;
            result = 31 * result + rhs;
            result = 31 * result + (topColor != null ? topColor.hashCode() : 0);
            result = 31 * result + (bottomColor != null ? bottomColor.hashCode() : 0);
            return result;
        }
    }

    /**
     * This is one set of scopes for a single line in the document.  A set of scopes
     * is a list of nested scope boxes applicable to that line.  The first scope
     * is the outermost and last is innermost, so we render them in list order.
     */
    public static class ScopeInfo
    {
        private final List<SingleNestedScope> nestedScopes = new ArrayList<>();
        private final boolean isStepLine;

        public ScopeInfo(boolean isStepLine)
        {
            this.isStepLine = isStepLine;
        }

        public boolean isStepLine()
        {
            return isStepLine;
        }

        public ScopeInfo withStepLine(boolean newStepLine)
        {
            ScopeInfo scopeInfo = new ScopeInfo(newStepLine);
            scopeInfo.nestedScopes.addAll(nestedScopes);
            return scopeInfo;
        }

        private static class SingleNestedScope
        {
            private final LeftRight leftRight;
            private final Middle middle;
            // Both are immutable once passed, so we can cache the hashCode:
            private final int hashCode;

            public SingleNestedScope(LeftRight leftRight, Middle middle)
            {
                this.leftRight = leftRight;
                this.middle = middle;

                hashCode = leftRight.hashCode() * 31 + middle.hashCode();
            }

            @Override
            public boolean equals(Object o)
            {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                SingleNestedScope that = (SingleNestedScope) o;
                // We can use the cached hashCode as a quick shortcut:
                if (hashCode != that.hashCode) return false;

                if (!leftRight.equals(that.leftRight)) return false;
                return middle.equals(that.middle);
            }

            @Override
            public int hashCode()
            {
                return hashCode;
            }
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ScopeInfo scopeInfo = (ScopeInfo) o;

            if (isStepLine != scopeInfo.isStepLine) return false;
            return nestedScopes.equals(scopeInfo.nestedScopes);
        }

        @Override
        public int hashCode()
        {
            int result = nestedScopes.hashCode();
            result = 31 * result + (isStepLine ? 1 : 0);
            return result;
        }
    }

    private class LeftRight
    {
        private final int lhs;
        private final int rhs;
        private final int padding;
        private final boolean starts;
        private final boolean ends;
        private final Color fillColor;
        private final Color edgeColor;

        public LeftRight(int lhs, int rhs, int padding, boolean starts, boolean ends, Color fillColor, Color edgeColor)
        {
            this.lhs = Math.max(0, lhs);
            this.rhs = rhs;
            this.padding = padding;
            this.starts = starts;
            this.ends = ends;
            this.fillColor = fillColor;
            this.edgeColor = edgeColor;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LeftRight leftRight = (LeftRight) o;

            if (lhs != leftRight.lhs) return false;
            if (rhs != leftRight.rhs) return false;
            if (padding != leftRight.padding) return false;
            if (starts != leftRight.starts) return false;
            if (ends != leftRight.ends) return false;
            if (!fillColor.equals(leftRight.fillColor)) return false;
            return edgeColor.equals(leftRight.edgeColor);
        }

        @Override
        public int hashCode()
        {
            int result = lhs;
            result = 31 * result + rhs;
            result = 31 * result + padding;
            result = 31 * result + (starts ? 1 : 0);
            result = 31 * result + (ends ? 1 : 0);
            result = 31 * result + fillColor.hashCode();
            result = 31 * result + edgeColor.hashCode();
            return result;
        }
    }
}
