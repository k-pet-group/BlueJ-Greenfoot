/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2014,2015,2016,2017,2018,2019  Michael Kolling and John Rosenberg

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
package bluej.editor.flow;

import bluej.Config;
import bluej.editor.flow.JavaSyntaxView.ScopeInfo.SingleNestedScope;
import bluej.editor.flow.LineDisplay.LineDisplayListener;
import bluej.editor.flow.TextLine.StyledSegment;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.editor.moe.MoeSyntaxEvent;
import bluej.editor.moe.MoeSyntaxEvent.NodeChangeRecord;
import bluej.editor.moe.ReparseRecord;
import bluej.editor.moe.ScopeColors;
import bluej.editor.moe.Token;
import bluej.editor.moe.Token.TokenType;
import bluej.parser.nodes.NodeStructureListener;
import bluej.parser.nodes.NodeTree;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.ReparseableDocument;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.JavaFXUtil;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.Reader;
import java.io.StringReader;
import java.util.*;

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
@OnThread(Tag.FXPlatform)
public class JavaSyntaxView implements ReparseableDocument, LineDisplayListener
{
    /** Maximum amount of document to reparse in one hit (advisory) */
    private final static int MAX_PARSE_PIECE = 8000;
    
    /** (NaviView) Paint method inner scope? if false, whole method will be highlighted as a single block */
    private static final boolean PAINT_METHOD_INNER = false;

    private static final int LEFT_INNER_SCOPE_MARGIN = 5;
    private static final int LEFT_OUTER_SCOPE_MARGIN = 2;
    private static final int RIGHT_SCOPE_MARGIN = 4;
    private static final int CURVED_CORNER_SIZE = 4;
    private static final int PARAGRAPH_MARGIN = 0; //24;
    
    // See comments in getImageFor for more info.
    // 1 means draw edge, 2 means draw filling
    @OnThread(Tag.FX)
    private static final int[][] CORNER_TEMPLATE = new int[][] {
            {0, 0, 1, 1},
            {0, 1, 2, 2},
            {1, 2, 2, 2},
            {1, 2, 2, 2}
    };
    private final Document document;
    private ParsedCUNode rootNode;
    private NodeTree<ReparseRecord> reparseRecordTree;
    private final ScopeColors scopeColors;
    private final BooleanExpression syntaxHighlighting;
    private int imageCacheLineHeight;
    private ReadOnlyDoubleProperty widthProperty; // width of editor view
    private FlowEditorPane editorPane;

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


    // Each item in the list maps the list index (as number of spaces) to indent amount
    private final List<Double> cachedSpaceSizes = new ArrayList<>();
    private FlowReparseRunner reparseRunner;

    public static enum ParagraphAttribute
    {
        STEP_MARK("bj-step-mark"), BREAKPOINT("bj-breakpoint"), ERROR("bj-error");

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

    /**
     * Cached indents for ParsedNode items.  Maps a node to an indent (in pixels)
     * When this is zero, it means it is not yet fully valid as the editor has not
     * yet appeared on screen.
     */
    private final Map<ParsedNode,Integer> nodeIndents = new HashMap<ParsedNode,Integer>();

    /**
     * We want to avoid repaint flicker by letting the user see partly-updated
     * backgrounds when processing the reparse queue.  So we store new backgrounds
     * in this map until we're ready to show all new backgrounds, which typically
     * happens when the reparse queue is empty (and is done by the applyPendingScopeBackgrounds()
     * method)
     */
    private final Map<Integer, ScopeInfo> pendingScopeBackgrounds = new HashMap<>();
    
    private final Multimap<Integer, Node> scopeBackgrounds = Multimaps.newMultimap(new HashMap<>(), ArrayList::new); 

    /**
      * Are we in the middle of an update which comes from the RichTextFX stream of changes?
      * If so, we must not ask for character bounds because the offset calculations
      * are all wrong, and a layout may be forced resulting in inconsistent state in
      * RichTextFX (and an exception being thrown: catching the exception is not enough
      * to avoid the incorrect state).
    */
    private boolean duringUpdate;

    /**
     * Creates a new BlueJSyntaxView.
     */
    public JavaSyntaxView(FlowEditorPane editorPane, ScopeColors scopeColors)
    {
        this.document = editorPane.getDocument();
        this.editorPane = editorPane;
        this.syntaxHighlighting = PrefMgr.flagProperty(PrefMgr.HIGHLIGHTING);
        this.scopeColors = scopeColors;
        resetColors();
        editorPane.addLineDisplayListener(this);
        editorPane.setLineStyler(this::getTokenStylesFor);
        JavaFXUtil.addChangeListenerPlatform(PrefMgr.getScopeHighlightStrength(), str -> {
            resetColors();
            recalculateAllScopes();
        });
        JavaFXUtil.addChangeListenerPlatform(syntaxHighlighting, syn -> {
            recalculateAllScopes();
        });
        JavaFXUtil.addChangeListenerPlatform(PrefMgr.getEditorFontSize(), sz -> {
            nodeIndents.clear();
            cachedSpaceSizes.clear();
            recalculateAllScopes();
        });
        // We use class color as a proxy for listening to all colors:
        JavaFXUtil.addChangeListenerPlatform(scopeColors.scopeClassColorProperty(), str -> {
            // If printing, don't run later as we're not on the main thread.
            // Instead, the printing code will trigger the necessary recalculation.
            //if (!document.isPrinting())
            {
                // runLater to make sure all colours have been set:
                JavaFXUtil.runAfterCurrent(() ->
                {
                    resetColors();
                    recalculateAllScopes();
                });
            }
        });
        
        JavaFXUtil.addChangeListenerPlatform(editorPane.widthProperty(), w -> JavaFXUtil.runAfter(Duration.millis(500), () -> {
            recalculateAllScopes();
            applyPendingScopeBackgrounds();
        }));
        JavaFXUtil.addChangeListenerPlatform(editorPane.heightProperty(), h -> JavaFXUtil.runAfter(Duration.millis(500), () -> {
            recalculateAllScopes();
            applyPendingScopeBackgrounds();
        }));
    }

    /**
     * Enable the parser. This should be called after loading a document.
     * @param force  whether to force-enable the parser. If false, the parser will only
     *                be enabled if an entity resolver is available.
     */
    @OnThread(Tag.FXPlatform)
    public void enableParser(boolean force)
    {
        if (rootNode == null)
        {
            rootNode = new ParsedCUNode();
            reparseRecordTree = new NodeTree<ReparseRecord>();
            //if (parentResolver != null || force) {
            //rootNode.setParentResolver(parentResolver);
            rootNode.textInserted(this, 0, 0, document.getLength(),
                    new MoeSyntaxEvent(0, document.getLength(), true, false));
            // We can discard the MoeSyntaxEvent: the reparse will update scopes/syntax
            //}
            document.addListener((start, end, repl) -> {
                NodeStructureListener blankListener = new NodeStructureListener()
                {
                    @Override
                    public void nodeRemoved(NodeAndPosition<ParsedNode> node)
                    {

                    }

                    @Override
                    public void nodeChangedLength(NodeAndPosition<ParsedNode> node, int oldPos, int oldSize)
                    {

                    }
                };
                if (start != end)
                {
                    rootNode.textRemoved(this, 0, start, end - start, blankListener);
                }
                if (repl != 0)
                {
                    rootNode.textInserted(this, 0, start, repl, blankListener);
                }
                scheduleReparseRunner();
            });
            
            scheduleReparseRunner();
        }
    }

    void recalculateAllScopes()
    {
        scopeBackgrounds.clear();
        recalculateScopes(pendingScopeBackgrounds, 0, document.getLineCount() - 1);
    }

    /**
     * Gets the syntax token styles for a given line of code.
     *
     * Returns null if there are no styles to apply (e.g. on a blank line or one with only whitespace).
     */
    private final List<StyledSegment> getTokenStylesFor(int lineIndex, CharSequence lineContent)
    {
        // Simple implementation if syntax highlighting is off:
        if (!syntaxHighlighting.get() || rootNode == null)
            return Collections.singletonList(new StyledSegment(Collections.emptyList(), lineContent.toString()));

        ArrayList<StyledSegment> lineStyle = new ArrayList<>();
        int curPosInLine = 0;
        Token nextToken = rootNode.getMarkTokensFor(document.getLineStart(lineIndex), lineContent.length(), 0, this); 
        while (nextToken.id != TokenType.END)
        {
            String tokenContent = lineContent.subSequence(curPosInLine, curPosInLine + nextToken.length).toString();
            List<String> tokenStyle = Collections.singletonList(nextToken.id.getCSSClass());
            lineStyle.add(new StyledSegment(tokenStyle, tokenContent));
            curPosInLine += nextToken.length;
            nextToken = nextToken.next;
        }
        // Very important to add a blank item if the line is blank, otherwise the line will get collapsed
        // in the display:
        if (lineStyle.isEmpty())
        {
            lineStyle.add(new StyledSegment(Collections.emptyList(), ""));
        }
        
        return lineStyle;
    }

    /**
     * Recalculate scope margins in the given line range. All line numbers are 0-based.
     * 
     * @param pendingScopes  map to store updated scope margin information.
     * @param firstLineIncl  the first line in the range to update (inclusive).
     * @param lastLineIncl   the last line in the range to update (inclusive).
     */
    private void recalculateScopes(Map<Integer, ScopeInfo> pendingScopes, int firstLineIncl, int lastLineIncl)
    {
        // editorPane is null during testing -- just skip updating the scopes in that case:
        if (editorPane == null)
            return;
        
        recalcScopeMarkers(pendingScopes,
                (int)editorPane.getWidth() - MarginAndTextLine.MARGIN_WIDTH,
                //(widthProperty == null || widthProperty.get() == 0) ? 200 :
                        //((int)widthProperty.get() - PARAGRAPH_MARGIN),
                firstLineIncl, lastLineIncl);
    }

    /*
    public void setEditorPane(MoeEditorPane editorPane)
    {
        this.editorPane = editorPane;
        this.widthProperty = editorPane.widthProperty();
        JavaFXUtil.addChangeListenerPlatform(widthProperty, w -> {
            document.fireChangedUpdate(null);
        });
        JavaFXUtil.addChangeListenerPlatform(editorPane.showLineNumbersProperty(), showLineNumbers -> {
            // By re-setting the paragraph graphic factory, force all visible lines to be re-drawn. Note that
            // this effectively creates a new function object from the method handle each time it is called, which
            // is why it works, but it's not clear if this behaviour is formally guaranteed.
            editorPane.setParagraphGraphicFactory(this::getParagraphicGraphic);
        });
    }
    */

    /**
     * A container for three line segments and elements: the previous (or above) line, the
     * current line, and the next (or below) line.
     */
    private class ThreeLines
    {
        Element aboveLineEl;
        Element thisLineEl;
        Element belowLineEl;
    }

    // The document should not change content during the lifetime of this object
    @OnThread(Tag.FXPlatform)
    private class Element
    {
        private final int lineIndex;
        private String cachedContent;

        private Element(int lineIndex)
        {
            this.lineIndex = lineIndex;
        }

        public int getStartOffset()
        {
            return document.getLineStart(lineIndex);
        }
        
        public int getEndOffset()
        {
            return lineIndex == document.getLineCount() - 1 ? document.getLength() : document.getLineStart(lineIndex + 1);
        }
        
        public String getText()
        {
            if (cachedContent == null)
            {
                cachedContent = document.getContent(getStartOffset(), getEndOffset());
            }
            return cachedContent;
        }
        //public int getElementIndex(int offset);
        //public int getElementCount();
    }

    /**
     * Re-calculate scope margins for the given lines, and add changed margin information to the given
     * map. Line numbers are 0-based.
     * 
     * @param pendingScopes  a map of (line number : scope information) for updated scope margins
     * @param fullWidth      the full width of the view, used for determining right margin
     * @param firstLine      the first line in the range to process (inclusive).
     * @param lastLine       the last line in the range to process (inclusive).
     */
    protected void recalcScopeMarkers(Map<Integer, ScopeInfo> pendingScopes, int fullWidth,
            int firstLine, int lastLine)
    {
        if (rootNode == null)
        {
            // Not initialised yet
            return;
        }

        int aboveLine = firstLine - 1;
        List<NodeAndPosition<ParsedNode>> prevScopeStack = new LinkedList<NodeAndPosition<ParsedNode>>();
        int curLine = firstLine;
        
        ThreeLines lines = new ThreeLines();

        lines.aboveLineEl = null;
        if (aboveLine >= 0) {
            lines.aboveLineEl = new Element(aboveLine);
        }
        lines.belowLineEl = null;
        if (firstLine + 1 < document.getLineCount()) {
            lines.belowLineEl = new Element(firstLine + 1);
        }

        lines.thisLineEl = new Element(firstLine);

        getScopeStackAfter(rootNode, 0, lines.thisLineEl.getStartOffset(), prevScopeStack);

        while (curLine <= lastLine) {

            // curLine is zero-based, but getParagraphAttributes is one-based:
            ScopeInfo scope = new ScopeInfo(getParagraphAttributes(curLine + 1));
            
            if (prevScopeStack.isEmpty()) {
                break;
            }

            drawScopes(fullWidth, scope, document, lines, prevScopeStack, 0);
            //if (! scope.equals(document.getDocument().getParagraphStyle(curLine)))
            {
                pendingScopes.put(curLine, scope);
            }
            //else
            {
                //pendingScopes.remove(curLine);
            }
            
            // Next line
            curLine++;
            if (curLine <= lastLine) {
                lines.aboveLineEl = lines.thisLineEl;
                lines.thisLineEl = lines.belowLineEl; 
                if (curLine + 1 < document.getLineCount()) {
                    lines.belowLineEl = new Element(curLine + 1);
                }
                else {
                    lines.belowLineEl = null;
                }
            }
        }
    }

    private class DrawInfo
    {
        final ScopeInfo scopes;
        ThreeLines lines;

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
    private void drawScopes(int fullWidth, ScopeInfo scopes, Document document, ThreeLines lines,
            List<NodeAndPosition<ParsedNode>> prevScopeStack, int nodeDepth)
    {
        int rightMargin = 10;

        ListIterator<NodeAndPosition<ParsedNode>> li = prevScopeStack.listIterator();

        DrawInfo drawInfo = new DrawInfo(scopes);
        drawInfo.lines = lines;

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

            if (! drawNode(drawInfo, nap)) {
                continue;
            }

            if (nodeSkipsEnd(napPos, napEnd, lines.thisLineEl)) {
                nodeDepth++;
                break;
            }

            // Draw the start node
            int xpos = getNodeIndent(nap, lines.thisLineEl);
            if (xpos != - 1 && xpos <= fullWidth) {
                boolean starts = nodeSkipsStart(nap, lines.aboveLineEl);
                boolean ends = nodeSkipsEnd(napPos, napEnd, lines.belowLineEl);
                int rbound = getNodeRBound(nap, fullWidth - rightMargin, nodeDepth,
                        lines.thisLineEl);

                drawInfo.node = nap.getNode();
                drawInfo.starts = starts;
                drawInfo.ends = ends;
                Color[] colors = colorsForNode(drawInfo.node);
                drawInfo.color1 = colors[0];
                drawInfo.color2 = colors[1];

                drawInfo.scopes.nestedScopes.add(calculatedNestedScope(drawInfo, xpos, rbound));
            }
            else if (xpos == -1)
            {
                // Mark as incomplete so we know to redraw later:
                drawInfo.scopes.incomplete = true;
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
            if (drawNode(drawInfo, nap)) {
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
                
                if (napPos < lines.thisLineEl.getEndOffset() && ! nodeSkipsStart(nextNap, lines.thisLineEl)) {
                    if (drawNode(drawInfo, nextNap)) {
                        // Draw it
                        nodeDepth++;
                        int xpos = getNodeIndent(nextNap, lines.thisLineEl);
                        int rbound = getNodeRBound(nextNap, fullWidth - rightMargin, nodeDepth,
                                lines.thisLineEl);
                        drawInfo.node = nextNap.getNode();
                        Color [] colors = colorsForNode(drawInfo.node);
                        drawInfo.color1 = colors[0];
                        drawInfo.color2 = colors[1];
                        drawInfo.starts = nodeSkipsStart(nextNap, lines.aboveLineEl);
                        drawInfo.ends = nodeSkipsEnd(napPos, napEnd, lines.belowLineEl);

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

    /**
     * Gets the left edge of the character at the given offset into the document, if we can calculate it.
     *
     * @param startOffset The offset into the document of the character we want the left edge for
     * @return If available, Optional.of(left-edge-X-in-pixels-in-local-coords).  If it is not available
     *         (which is very possible: *always* check for Optional.empty), then Optional.empty will be returned.
     */
    private OptionalInt getLeftEdge(int startOffset)
    {
        if (editorPane == null)
        {
            return OptionalInt.empty();
        }
        
        int column = document.getColumnFromPosition(startOffset);
        int line = document.getLineFromPosition(startOffset);
        String lineText = new Element(line).getText();
        boolean allSpaces = (column == 0) || lineText.substring(0, column).codePoints().allMatch(n -> n == ' ');

        if (!editorPane.isLineVisible(line) && (!allSpaces || cachedSpaceSizes.size() <= 4))
        {
            // If we are printing, we'll never be able to get the on-screen position
            // for our off-screen editor.  So we must make our best guess at positions
            // using measureString
            if (isPrinting())
            {
                TextField field = new TextField();
                field.styleProperty().bind(editorPane.styleProperty());
                // Have to put TextField into a Scene for CSS to take effect:
                @SuppressWarnings("unused")
                Scene s = new Scene(new BorderPane(field));
                field.applyCss();
                double singleSpaceWidth = JavaFXUtil.measureString(field, "          ", false, false) / 10.0;
                // I admit, I don't understand why we need the 1.05 fudge factor here,
                // but after an hour or two of fiddling, it's the only thing I've found
                // that makes the measureString backgrounds line-up with the editor pane text:
                int positionSpaceWidth = (int)(singleSpaceWidth * column * 1.05);
                return OptionalInt.of(positionSpaceWidth + PARAGRAPH_MARGIN);
            }
            else
            {
                return OptionalInt.empty();
            }
        }

        /*
         * So, if a character is on screen, it's trivial to calculate the indent in pixels, we just ask the
         * editor pane.  If the character is not on screen, the editor pane won't tell us the indent.  Which
         * would prevent us drawing the scope boxes correctly.  Consider this case:
         *                                <----- This is the top of the viewport
         * class A
         * {
         *     public void method foo()
         *     {
         *
         *                                <----- This is the bottom of the viewport
         *  } //line X
         * }
         *
         * Because we can't calculate the indent for line X, we would draw the scope box for the method with
         * its left-hand edge 4 spaces in, rather than 1 as it should be, and when we scrolled down, it would
         * either be wrong or we would need a redraw, which would look ugly.
         *
         * However, here's the trick.  If the line off-screen is *more* indented than any line on screen, it
         * won't affect our scope drawing (because we are looking for the left edge).  If the line off-screen
         * is indented *less* than any of the lines on screen then we can use the line on-screen with the highest
         * indent to calculate the scope that we need.  This is because if you've got a line with say 20 spaces,
         * and you need to know the indent for an 8-space line, then you can just calculate it by asking for
         * the 8th character position on the 20-space line.  So as long as we want an indent with less spaces
         * than the largest line on screen, we can calculate it.
         *
         * We store cached indent sizes (to cut down on continual recalculation) in the cachedSpaceSizes
         * array, where item at index 0 is the pixel indent for 0 spaces, item 1 is the pixel indent for 1 spaces, etc.
         * We are making the assumption that spaces are always the same sizes on each line, but I can't
         * think of any situation where that is not the case.  We clear the array when the font size changes.
         */

        if (allSpaces)
        {
            // All spaces, we can use/update cached space indents
            int numberOfSpaces = column;
            while (numberOfSpaces >= cachedSpaceSizes.size())
            {
                // We have more spaces than the cache; we must update it if we can
                Optional<Double> leftEdge = Optional.empty();
                if (!duringUpdate)
                {
                    leftEdge = editorPane.getLeftEdgeX(startOffset - numberOfSpaces + cachedSpaceSizes.size());
                }
                // If the character isn't on screen, we're not going to be able to calculate indent,
                // and we know we haven't got a cached indent, so give up:
                if (!leftEdge.isPresent())
                {
                    // If we've got a few spaces, we can make a reasonable estimate, on the basis
                    // that space characters are going to be the same width as each other.
                    if (cachedSpaceSizes.size() >= 4)
                    {
                        int highestSpaces = cachedSpaceSizes.size() - 1;
                        double highestWidth = cachedSpaceSizes.get(highestSpaces) - cachedSpaceSizes.get(0); 
                        return OptionalInt.of((int)(highestWidth / highestSpaces * numberOfSpaces
                                + cachedSpaceSizes.get(0)));
                    }
                    return OptionalInt.empty();
                }
                double indent = leftEdge.get();
                cachedSpaceSizes.add(indent);
            }
            return OptionalInt.of(cachedSpaceSizes.get(numberOfSpaces).intValue());
        }
        else
        {
            try
            {
                Optional<Double> leftEdge = Optional.empty();
                if (!duringUpdate)
                {
                    leftEdge = editorPane.getLeftEdgeX(startOffset);
                }

                if (leftEdge.isPresent())
                {
                    double indent = leftEdge.get();
                    return OptionalInt.of((int) indent);
                }
            }
            catch (IllegalArgumentException | IndexOutOfBoundsException e)
            {
                // These shouldn't occur but there have been some related bugs, and it is better to
                // catch the exception and leave the editor in a (somewhat) usable state. We'll log
                // the error, however:
                Debug.reportError(e);
            }

            // Not on screen, wider than any indent we have cached, nothing we can do:
            return OptionalInt.empty();
        }
    }

    /**
     * Check whether a node needs to be drawn.
     * @param info
     * @param node
     * @return
     */
    private boolean drawNode(DrawInfo info, NodeAndPosition<ParsedNode> nap)
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

        if (nodeSkipsStart(nap, info.lines.thisLineEl)) {
            return false; // just white space on this line
        }

        return !nodeSkipsEnd(napPos, napEnd, info.lines.thisLineEl);
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
     * Create a nested scope record based on the supplied information.
     */
    private ScopeInfo.SingleNestedScope calculatedNestedScope(DrawInfo info, int xpos, int rbound)
    {
        if (!false) {
            xpos -= info.node.isInner() ? LEFT_INNER_SCOPE_MARGIN : LEFT_OUTER_SCOPE_MARGIN;
        }

        return new ScopeInfo.SingleNestedScope(
                new LeftRight(xpos, rbound, info.starts, info.ends, info.color2, info.color1),
                getScopeMiddle(info, xpos, rbound));
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
            Element lineEl)
    {
        int napEnd = nap.getEnd();
        int rbound = fullWidth - nodeDepth * (false ? 0 : RIGHT_SCOPE_MARGIN);
        if (lineEl == null || napEnd >= lineEl.getEndOffset()) {
            return rbound;
        }
        if (napEnd < lineEl.getStartOffset()) {
            return rbound;
        }
        
        // If there is some text between the node end and the end of the line, we want to clip the
        // node short so that the text does not appear to be part of the node.
        int nwsb = findNonWhitespaceComment(nap, lineEl, napEnd - lineEl.getStartOffset());
        if (nwsb != -1) {
            OptionalInt eboundsX = getLeftEdge(napEnd);
            if (eboundsX.isPresent())
                return Math.min(rbound, eboundsX.getAsInt());
            else
                return rbound;
        }
        return rbound;
    }

    /**
     * Checks whether the given node should be skipped on the given line (because it
     * starts later). This takes into account that the node may "officially" start on the
     * line, but only have white space, in which case it can be moved down to the next line.
     */
    private boolean nodeSkipsStart(NodeAndPosition<ParsedNode> nap, Element lineEl)
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
            int nws = findNonWhitespaceComment(nap, lineEl, napPos - lineEl.getStartOffset());
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
    private boolean nodeSkipsEnd(int napPos, int napEnd, Element lineEl)
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
            int nws = findNonWhitespace(lineEl, 0);
            if (nws == -1 || lineEl.getStartOffset() + nws >= napEnd) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a node's indent amount (in component co-ordinate space, minus left margin) for a given line.
     * If the node isn't present on the line, returns Integer.MAX_VALUE. A cached value
     * is used if available.
     */
    private int getNodeIndent(NodeAndPosition<ParsedNode> nap, Element lineEl)
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

        if (nodeSkipsStart(nap, lineEl)
                || nodeSkipsEnd(napPos, napEnd, lineEl)) {
            return Integer.MAX_VALUE;
        }

        // int indent = nap.getNode().getLeftmostIndent(doc, 0, 0);
        Integer indent = nodeIndents.get(nap.getNode());
        // An indent value of zero is only given by getCharacterBoundsOnScreen when the editor
        // hasn't been shown yet, so we recalculate whenever we find that indent value in the
        // hope that the editor is now visible:
        if (indent == null) {
            // No point trying to re-calculate the indent if the line isn't on screen:
            if (editorPane != null && (editorPane.isLineVisible(document.getLineFromPosition(lineEl.getStartOffset())) || isPrinting()))
            {
                indent = getNodeIndent(nap);
                nodeIndents.put(nap.getNode(), indent);
            }
            else
            {
                indent = -1;
            }
        }

        int xpos = indent;

        // Corner case: node start position is on this line, and is greater than the node indent?
        if (napPos > lineEl.getStartOffset()) {
            // In this case, we'll stretch the border to the regular indent only if
            // we can do it without hitting non-whitespace (which must belong to another node).
            int nws = findNonWhitespaceBwards(lineEl, napPos - lineEl.getStartOffset() - 1, 0);
            if (nws != -1) {
                OptionalInt lboundsX = getLeftEdge(lineEl.getStartOffset() + nws + 1);
                if (lboundsX.isPresent())
                {
                    xpos = Math.max(xpos, lboundsX.getAsInt() - PARAGRAPH_MARGIN);
                }
            }
        }

        return xpos;
    }

    private boolean isPrinting()
    {
        return false;
    }

    /**
     * Calculate the indent for a node.
     */
    private int getNodeIndent(NodeAndPosition<ParsedNode> nap)
    {
        try {
            int indent = Integer.MAX_VALUE;

            int curpos = nap.getPosition();
            int napEnd = nap.getEnd();

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
                int line = document.getLineFromPosition(curpos);
                Element lineEl = new Element(line);

                int lineOffset = curpos - lineEl.getStartOffset();

                int nws;
                if (lineEl.getStartOffset() < nap.getPosition() && nap.getNode().isInner()) {
                    // The node is an inner node starting on this line
                    nws = findNonWhitespaceComment(nap, lineEl, lineOffset);
                } else {
                    nws = findNonWhitespace(lineEl, lineOffset);
                }

                if (nws == lineOffset) {
                    // Ok, at this position we have non-white space and are not in an inner
                    OptionalInt cboundsX = getLeftEdge(curpos);
                    if (cboundsX.isPresent())
                    {
                        indent = Math.min(indent, cboundsX.getAsInt() - PARAGRAPH_MARGIN);
                    }
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
        catch (IndexOutOfBoundsException e)
        {
            return -1;
        }
    }
    
    private int[] reassessIndentsAdd(int dmgStart, int dmgEnd)
    {
        ParsedCUNode pcuNode = rootNode;
        if (pcuNode == null) {
            return new int[] {dmgStart, dmgEnd};
        }
        
        int ls = document.getLineFromPosition(dmgStart);
        int le = document.getLineFromPosition(dmgEnd);
        
        try {
            int [] dmgRange = new int[2];
            dmgRange[0] = dmgStart;
            dmgRange[1] = dmgEnd;

            int i = ls;
            List<NodeAndPosition<ParsedNode>> scopeStack = new LinkedList<NodeAndPosition<ParsedNode>>();
            int lineEndPos = new Element(le).getEndOffset();
            Element lineEl = new Element(ls);
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
                i = document.getLineFromPosition(top.getPosition());
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
                int nws = findNonWhitespace(lineEl, 0);
                while (nws == -1) {
                    if (++i > le) {
                        break outer;
                    }
                    lineEl = new Element(i);
                    nws = findNonWhitespace(lineEl, 0);
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
                OptionalInt cboundsX = getLeftEdge(lineEl.getStartOffset() + nws);
                int indent = cboundsX.orElse(PARAGRAPH_MARGIN);
                for (j = scopeStack.listIterator(scopeStack.size()); j.hasPrevious(); ) {
                    NodeAndPosition<ParsedNode> next = j.previous();
                    if (next.getPosition() <= curpos) {
                        // Node is present on this line (begins before curpos)
                        updateNodeIndent(next, indent - PARAGRAPH_MARGIN, nodeIndents.get(next.getNode()), dmgRange);
                    }
                    else if (next.getPosition() < lineEl.getEndOffset()) {
                        // Node starts on this line, after curpos.
                        nws = findNonWhitespace(lineEl, next.getPosition() - lineEl.getStartOffset());
                        Integer oindent = nodeIndents.get(next.getNode());
                        if (oindent != null && nws != -1) {
                            cboundsX = getLeftEdge(lineEl.getStartOffset() + nws);
                            indent = cboundsX.orElse(PARAGRAPH_MARGIN);
                            updateNodeIndent(next, indent - PARAGRAPH_MARGIN, oindent, dmgRange);
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
                                nws = findNonWhitespace(lineEl, spos);
                                Integer oindent = nodeIndents.get(nap.getNode());
                                if (oindent != null && nws != -1) {
                                    cboundsX = getLeftEdge(lineEl.getStartOffset() + nws);
                                    indent = cboundsX.orElse(PARAGRAPH_MARGIN);
                                    updateNodeIndent(nap, indent - PARAGRAPH_MARGIN, oindent, dmgRange);
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
                lineEl = new Element(i);
            }
            
            return dmgRange;
        } finally {}
        //catch (BadLocationException ble) {
        //    throw new RuntimeException(ble);
        //}
    }

    private int[] reassessIndentsRemove(int dmgPoint, boolean multiLine)
    {
        ParsedCUNode pcuNode = rootNode;
        
        int [] dmgRange = new int[2];
        dmgRange[0] = dmgPoint;
        dmgRange[1] = dmgPoint;
        
        if (pcuNode == null) {
            return dmgRange;
        }
        
        int ls = document.getLineFromPosition(dmgPoint);
        Element lineEl = new Element(ls);

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
            
            // All nodes for this line with a cached indent greater than or equal to the damage point
            // indent should have their indents re-assessed: If the indent of the node on this line is
            // lower than (or the same as) the cached indent, it becomes the new cached indent; otherwise
            // the cached indent must be discarded.
            // Except: if the node does not span the damage point, its cached indent need not be discarded,
            //   since in that case the node indent cannot have increased.

            List<NodeAndPosition<ParsedNode>> rscopeStack = new LinkedList<NodeAndPosition<ParsedNode>>();
            getScopeStackAfter(rootNode, 0, dmgPoint, rscopeStack);
            rscopeStack.remove(0); // remove the root node

            boolean doContinue = true;

            OptionalInt cboundsX = getLeftEdge(dmgPoint);
            int dpI = cboundsX.orElse(PARAGRAPH_MARGIN) - PARAGRAPH_MARGIN; // damage point indent

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

                    if (nodeSkipsStart(rtop, lineEl)) {
                        if (rtop.getPosition() <= dmgPoint) {
                            // The remove may have made this line empty
                            nodeIndents.remove(rtop.getNode());
                            dmgRange[0] = Math.min(dmgRange[0], rtop.getPosition());
                            dmgRange[1] = Math.max(dmgRange[1], rtop.getEnd());
                        }
                        break; // no more siblings can be on this line
                    }

                    int nwsP = Math.max(lineEl.getStartOffset(), rtop.getPosition());
                    int nws = findNonWhitespace(lineEl, nwsP - lineEl.getStartOffset());
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
                    int newIndent = cboundsX.orElse(PARAGRAPH_MARGIN) - PARAGRAPH_MARGIN;

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
    private int findNonWhitespace(Element element, int startPos)
    {
        String text = element.getText();
        for (int i = startPos; i < text.length(); i++)
        {
            char c = text.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Search for a non-whitespace character, starting from the given offset in the segment; treat
     * single-line comments as whitespace. Returns -1 if the line consists only of whitespace.
     */
    private int findNonWhitespaceComment(NodeAndPosition<ParsedNode> nap, Element lineEl, int startPos)
    {
        int nws = findNonWhitespace(lineEl, startPos);
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
    private int findNonWhitespaceBwards(Element element, int startPos, int endPos)
    {
        String text = element.getText();
        for (int i = startPos; i > endPos; i--)
        {
            char c = text.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return i;
            }
        }
        return endPos - 1;
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
            recalculateAllScopes();
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

        
        if (changes.isInsert()) {
            damageStart = Math.min(damageStart, changes.getOffset());
            damageEnd = Math.max(damageEnd, changes.getOffset() + changes.getLength());
            int [] r = reassessIndentsAdd(damageStart, damageEnd);
            damageStart = r[0];
            damageEnd = r[1];
        }
        else if (changes.isRemove()) {
            damageStart = Math.min(damageStart, changes.getOffset());
            int [] r = reassessIndentsRemove(damageStart, true); //TODO we shouldn't always pass multiLine as true
            damageStart = r[0];
            damageEnd = r[1];
        }
        
        if (damageStart < damageEnd) {
            int line = document.getLineFromPosition(damageStart);
            int lastline = document.getLineFromPosition(damageEnd - 1);
            recalculateScopes(pendingScopeBackgrounds, line, lastline);
        }
    }

    /**
     * Clear a node's cached indent information. If the node is an inner node this
     * also clears parent nodes as appropriate.
     */
    private int[] clearNap(NodeAndPosition<ParsedNode> nap, Document document,
            int damageStart, int damageEnd)
    {
        if (nap.getNode().isInner()) {

            List<NodeAndPosition<ParsedNode>> list = new LinkedList<NodeAndPosition<ParsedNode>>();
            NodeAndPosition<ParsedNode> top;
            top = new NodeAndPosition<ParsedNode>(rootNode, 0, document.getLength());
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

    /*
    @OnThread(Tag.FXPlatform)
    public Node getParagraphicGraphic(int lineNumber)
    {
        // RichTextFX since version 0.9.0 started to generate -1 as a line number, when
        // constructing new lines before making them visible. Apparently this is not
        // considered a bug. Since there is no point doing anything in this case, we
        // just return immediately:
        if (lineNumber < 0)
        {
            return null;
        }
        
        // RichTextFX numbers from 0, but javac numbers from 1:
        lineNumber += 1;
        Label label = new Label("" + lineNumber);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label.setEllipsisString("\u2026");
        label.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
        JavaFXUtil.setPseudoclass("bj-odd", (lineNumber & 1) == 1, label);
        JavaFXUtil.addStyleClass(label, "moe-line-label");
        Node stepMarkIcon = makeStepMarkIcon();
        Node breakpointIcon = makeBreakpointIcon();
        label.setGraphic(new StackPane(breakpointIcon, stepMarkIcon));
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
                MoeEditor editor = editorPane.getEditor();
                // Shouldn't be null because that's only for off-screen copies
                // and we are in a click handler, but in case of future change:
                if (editor != null)
                {
                    editor.toggleBreakpoint(editorPane.getDocument().getAbsolutePosition(lineNumberFinal - 1, 0));
                }
            }
            e.consume();
        });

        EnumSet<ParagraphAttribute> attr = getParagraphAttributes(lineNumber);
        for (ParagraphAttribute possibleAttribute : ParagraphAttribute.values())
        {
            JavaFXUtil.setPseudoclass(possibleAttribute.getPseudoclass(), attr.contains(possibleAttribute), label);
        }
        stepMarkIcon.setVisible(attr.contains(ParagraphAttribute.STEP_MARK));
        breakpointIcon.setVisible(attr.contains(ParagraphAttribute.BREAKPOINT));
        if (stepMarkIcon.isVisible() || breakpointIcon.isVisible() ||
                (editorPane != null && !editorPane.isShowLineNumbers()))
        {
            label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }
        else
        {
            label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        }
        
        AnchorPane.setLeftAnchor(label, 0.0);
        AnchorPane.setRightAnchor(label, 3.0);
        AnchorPane.setTopAnchor(label, 0.0);
        AnchorPane.setBottomAnchor(label, 0.0);
        return new AnchorPane(label);
    }
    */

    // Red octagon with white STOP on it.  By doing it as a shape rather than
    // image file, we get it looking good on all HiDPI displays.
    private static Node makeBreakpointIcon()
    {
        Node icon = Config.makeStopIcon(false);
        JavaFXUtil.addStyleClass(icon, "moe-breakpoint-icon");
        return icon;
    }

    private Node makeStepMarkIcon()
    {
        Shape arrow = Config.makeArrowShape(false);
        JavaFXUtil.addStyleClass(arrow, "moe-step-mark-icon");
        return arrow;
    }

    /**
     * Sets attributes throughout the document.
     *
     * @param alterAttr Anything mapped to true will be added to all lines, anything mapped to false will be removed from all lines
     * @return The list of all line numbers where the attributes were changed
     */
    public Map<Integer, EnumSet<ParagraphAttribute>> setParagraphAttributes(Map<ParagraphAttribute, Boolean> alterAttr)
    {
        /*
        Map<Integer, EnumSet<ParagraphAttribute>> changed = new HashMap<>();
        for (int line = 1; line <= document.getLineCount(); line++)
        {
            changed.putAll(setParagraphAttributes(line, alterAttr));
        }
        return changed;
        */
        return Map.of();
    }


    /**
     * Sets attributes for a particular line number.
     *
     * @param lineNumber the line number for which to change the attributes (first line is 1)
     * @param alterAttr the attributes to set the value for (other attributes will be unaffected)
     * @return The list of all line numbers where the attributes were changed
     */
    /*
    public Map<Integer, EnumSet<ParagraphAttribute>> setParagraphAttributes(int lineNumber, Map<ParagraphAttribute, Boolean> alterAttr)
    {
        ScopeInfo paraStyle = editorPane.getParagraph(lineNumber - 1).getParagraphStyle();
        if (paraStyle == null) {
            paraStyle = new ScopeInfo(EnumSet.noneOf(ParagraphAttribute.class));
        }
        
        EnumSet<ParagraphAttribute> attr = EnumSet.copyOf(paraStyle.getAttributes());
        boolean changed = false;
        for (Entry<ParagraphAttribute, Boolean> alter : alterAttr.entrySet())
        {
            if (alter.getValue())
            {
                // Order matters; want to add/remove regardless of changed value:
                changed = attr.add(alter.getKey()) || changed;
            }
            else
            {
                // Order matters; want to add/remove regardless of changed value:
                changed = attr.remove(alter.getKey()) || changed;
            }
        }
        
        if (changed)
        {
            editorPane.setParagraphStyle(lineNumber - 1, paraStyle.withAttributes(attr));
            return Collections.singletonMap(lineNumber, EnumSet.copyOf(attr));
        }
        else
        {
            return Collections.emptyMap();
        }
    }
    */

    /**
     * Gets the paragraph attributes for a particular line.  If none found, returns the empty set.
     * The set returned should not be modified directly.
     *
     * @param lineNumber  The line number to retrieve attributes for (the first line is one rather than zero).
     */
    /*
    EnumSet<ParagraphAttribute> getParagraphAttributes(int lineNumber)
    {
        ScopeInfo scopeInfo = editorPane.getParagraph(lineNumber - 1).getParagraphStyle();
        if (scopeInfo == null)
        {
            return EnumSet.noneOf(ParagraphAttribute.class);
        }
        else
        {
            return scopeInfo.getAttributes();
        }
    }
    */
    EnumSet<ParagraphAttribute> getParagraphAttributes(int lineNumber)
    {
        return EnumSet.noneOf(ParagraphAttribute.class);
    }

    /**
     * Sets up the colors based on the strength value 
     * (from strongest (20) to white (0)
     */
    void resetColors()
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
    @OnThread(Tag.FX)
    public static class ScopeInfo
    {
        // For display purposes.  Step overrides breakpoint.
        public static enum Special
        {
            NONE, BREAKPOINT, STEP;
        }

        private final List<SingleNestedScope> nestedScopes = new ArrayList<>();
        private final EnumSet<ParagraphAttribute> attributes;
        // If a scope needs repainting later, we mark as incomplete:
        private boolean incomplete = false;

        public ScopeInfo(EnumSet<ParagraphAttribute> attributes)
        {
            this.attributes = EnumSet.copyOf(attributes);
        }

        public EnumSet<ParagraphAttribute> getAttributes()
        {
            return attributes;
        }

        public ScopeInfo withAttributes(EnumSet<ParagraphAttribute> attributes)
        {
            ScopeInfo scopeInfo = new ScopeInfo(attributes);
            scopeInfo.nestedScopes.addAll(nestedScopes);
            scopeInfo.incomplete = incomplete;
            return scopeInfo;
        }

        public boolean isIncomplete()
        {
            return incomplete;
        }


        public static class SingleNestedScope
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

            if (incomplete != scopeInfo.incomplete) return false;
            if (!attributes.equals(scopeInfo.attributes)) return false;
            return nestedScopes.equals(scopeInfo.nestedScopes);
        }

        @Override
        public int hashCode()
        {
            int result = nestedScopes.hashCode();
            result = 31 * result + attributes.hashCode();
            result += isIncomplete() ? 1 : 0;
            return result;
        }

        // Mainly for debugging
        @Override
        public String toString()
        {
            return "ScopeInfo{" +
                    "nestedScopes=" + nestedScopes +
                    ", attributes=" + attributes +
                    ", incomplete=" + incomplete +
                    '}';
        }
    }

    private class LeftRight
    {
        private final int lhs;
        private final int rhs;
        private final boolean starts;
        private final boolean ends;
        private final Color fillColor;
        private final Color edgeColor;

        public LeftRight(int lhs, int rhs, boolean starts, boolean ends, Color fillColor, Color edgeColor)
        {
            this.lhs = Math.max(0, lhs);
            this.rhs = rhs;
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
            result = 31 * result + (starts ? 1 : 0);
            result = 31 * result + (ends ? 1 : 0);
            result = 31 * result + fillColor.hashCode();
            result = 31 * result + edgeColor.hashCode();
            return result;
        }
    }

    /**
     * Schedule a reparse at a certain point within the document.
     * @param pos    The position to reparse at
     * @param size   The reparse size. This is a minimum, rather than a maximum; that is,
     *               the reparse when it occurs must parse at least this much.
     */
    public void scheduleReparse(int pos, int size)
    {
        NodeAndPosition<ReparseRecord> existing = reparseRecordTree.findNodeAtOrAfter(pos);
        if (existing != null) {
            if (existing.getPosition() > pos && existing.getPosition() <= (pos + size)) {
                existing.getNode().slideStart(pos - existing.getPosition());
                return;
            }
            else if (existing.getPosition() <= pos) {
                int nsize = (pos + size) - existing.getPosition();
                if (nsize > existing.getSize()) {
                    NodeAndPosition<ReparseRecord> next = existing.nextSibling();
                    while (next != null && next.getPosition() <= pos + size) {
                        nsize = Math.max(nsize, next.getEnd() - pos);
                        NodeAndPosition<ReparseRecord> nnext = next.nextSibling();
                        next.getNode().remove();
                        next = nnext;
                    }
                    existing.getNode().setSize(nsize);
                }
                return;
            }
        }

        ReparseRecord rr = new ReparseRecord();
        reparseRecordTree.insertNode(rr, pos, size);
    }

    /**
     * Process all of the re-parse queue.
     */
    public void flushReparseQueue()
    {
        while (pollReparseQueue(document.getLength())) ;
        // Queue now empty, so flush backgrounds:
        applyPendingScopeBackgrounds();
    }

    private void applyPendingScopeBackgrounds()
    {
        pendingScopeBackgrounds.forEach((line, info) -> {
            scopeBackgrounds.removeAll(line);
            Optional<double[]> possVertBounds = editorPane.getTopAndBottom(line);
            if (possVertBounds.isEmpty())
                return;
            for (SingleNestedScope nestedScope : info.nestedScopes)
            {
                // Draw outer:
                Region rectangle = new Region();
                rectangle.setManaged(false);
                rectangle.resizeRelocate(
                    nestedScope.leftRight.lhs, 0, nestedScope.leftRight.rhs - nestedScope.leftRight.lhs, possVertBounds.get()[1] - possVertBounds.get()[0]
                );
                CornerRadii radii = null;
                Insets bodyInsets = null;
                double singleRadius = 5.0;
                if (nestedScope.leftRight.starts && nestedScope.leftRight.ends)
                {
                    radii = new CornerRadii(singleRadius, false);
                    bodyInsets = new Insets(1);
                }
                else if (nestedScope.leftRight.starts)
                {
                    radii = new CornerRadii(singleRadius, singleRadius, 0.0, 0.0, false);
                    bodyInsets = new Insets(1, 1, 0, 1);
                }
                else if (nestedScope.leftRight.ends)
                {
                    radii = new CornerRadii(0.0, 0.0, singleRadius, singleRadius, false);
                    bodyInsets = new Insets(0, 1, 1, 1);
                }
                else
                {
                    bodyInsets = new Insets(0, 1, 0, 1);
                }
                rectangle.setBackground(new Background(
                    new BackgroundFill(nestedScope.leftRight.edgeColor, radii, null),
                    new BackgroundFill(nestedScope.leftRight.fillColor, radii, bodyInsets)
                ));
                //rectangle.setStroke(nestedScope.leftRight.edgeColor);
                //rectangle.setFill(nestedScope.leftRight.fillColor);
                scopeBackgrounds.put(line, rectangle);
                // Draw middle:
                /*
                rectangle = new Region();
                rectangle.setManaged(false);
                rectangle.resizeRelocate(
                        nestedScope.middle.lhs, possVertBounds.get()[0], nestedScope.middle.rhs - nestedScope.middle.lhs, possVertBounds.get()[1] - possVertBounds.get()[0]
                );
                rectangle.setBackground(new Background(new BackgroundFill(nestedScope.middle.bodyColor, null, null)));
                //rectangle.setStroke(nestedScope.leftRight.edgeColor);
                //rectangle.setFill(nestedScope.middle.bodyColor);
                scopeBackgrounds.put(line, rectangle);
                */
            }
        });
        pendingScopeBackgrounds.clear();

        editorPane.applyScopeBackgrounds(scopeBackgrounds);
    }

    /**
     * Run an item from the re-parse queue, if there are any. Return true if
     * a queued re-parse was processed or false if the queue was empty.
     */
    public boolean pollReparseQueue()
    {
        return pollReparseQueue(MAX_PARSE_PIECE);
    }

    /**
     * Run an item from the re-parse queue, if there are any, and attempt to
     * parse the specified amount of document (approximately). Return true if
     * a queued re-parse was processed or false if the queue was empty.
     */
    @OnThread(Tag.FXPlatform)
    private boolean pollReparseQueue(int maxParse)
    {
        try {
            if (reparseRecordTree == null) {
                return false;
            }

            NodeAndPosition<ReparseRecord> nap = reparseRecordTree.findNodeAtOrAfter(0);
            if (nap != null) {
                int pos = nap.getPosition();

                ParsedNode pn = rootNode;
                int ppos = 0;
                if (pn != null) {
                    // Find the ParsedNode to handle the reparse.
                    NodeAndPosition<ParsedNode> cn = pn.findNodeAt(pos, ppos);
                    while (cn != null && cn.getEnd() == pos) {
                        cn = cn.nextSibling();
                    }
                    while (cn != null && cn.getPosition() <= pos) {
                        ppos = cn.getPosition();
                        pn = cn.getNode();
                        cn = pn.findNodeAt(nap.getPosition(), ppos);
                        while (cn != null && cn.getEnd() == pos) {
                            cn = cn.nextSibling();
                        }
                    }

                    //Debug.message("Reparsing: " + ppos + " " + pos);
                    MoeSyntaxEvent mse = new MoeSyntaxEvent(-1, -1, false, false);
                    pn.reparse(this, ppos, pos, maxParse, mse);
                    // Dump tree (for debugging):
                    //Debug.message("Dumping tree:");
                    //dumpTree(parsedNode.getChildren(0), "");

                    updateDamage(mse);
                    return true;
                }
            }
            return false;
        }
        catch (RuntimeException e) {
            
            /*
            Debug.message("Exception during incremental parsing. Recent edits:");
            for (EditEvent event : recentEdits) {
                String eventStr = event.type == EDIT_INSERT ? "insert " : "delete ";
                eventStr += "offset=" + event.offset + " length=" + event.length;
                Debug.message(eventStr);
            }

            Debug.message("--- Source code ---");
            Debug.message(getText(0, getLength()));
            Debug.message("--- Source ends ---");
            */

            throw e;
        }
    }

    public MoeSyntaxDocument.Element getDefaultRootElement()
    {
        int[] lineStarts = new int[document.getLineCount()];
        for (int i = 0; i < lineStarts.length; i++)
        {
            lineStarts[i] = document.getLineStart(i);
        }
        
        // This is a different kind of element, which is only there to return a wrapper for the paragraphs:
        return new MoeSyntaxDocument.Element()
        {
            @Override
            public MoeSyntaxDocument.Element getElement(int index)
            {
                if (index >= lineStarts.length)
                    return null;

                boolean lastPara = index == lineStarts.length - 1;
                int paraLength;
                paraLength = lastPara ? (document.getLength() - lineStarts[index]) : lineStarts[index + 1] - lineStarts[index];
                int pos = lineStarts[index];
                return new MoeSyntaxDocument.Element()
                {
                    @Override
                    public MoeSyntaxDocument.Element getElement(int index)
                    {
                        return null;
                    }

                    @Override
                    public int getStartOffset()
                    {
                        return pos;
                    }

                    @Override
                    public int getEndOffset()
                    {
                        return pos + paraLength;
                    }

                    @Override
                    public int getElementIndex(int offset)
                    {
                        return -1;
                    }

                    @Override
                    public int getElementCount()
                    {
                        return 0;
                    }
                };
            }

            @Override
            public int getStartOffset()
            {
                return 0;
            }

            @Override
            public int getEndOffset()
            {
                return document.getLength();
            }

            @Override
            public int getElementIndex(int offset)
            {
                return document.getLineFromPosition(offset);
            }

            @Override
            public int getElementCount()
            {
                if (lineStarts != null)
                    return lineStarts.length;
                else
                    return document.getLineCount();
            }
        };
    }

    @Override
    public Reader makeReader(int startPos, int endPos)
    {
        // Simple but slow:
        return new StringReader(document.getFullContent().substring(startPos, endPos));
    }

    @Override
    public int getLength()
    {
        return document.getLength();
    }

    /**
     * Mark a portion of the document as having been parsed. This removes any
     * scheduled re-parses as appropriate and repaints the appropriate area.
     */
    public void markSectionParsed(int pos, int size)
    {
        repaintLines(pos, size, true);

        NodeAndPosition<ReparseRecord> existing = reparseRecordTree.findNodeAtOrAfter(pos);
        while (existing != null && existing.getPosition() <= pos) {
            NodeAndPosition<ReparseRecord> next = existing.nextSibling();
            // Remove from end, or a middle portion, or the whole node
            int rsize = existing.getEnd() - pos;
            rsize = Math.min(rsize, size);
            if (rsize == existing.getSize()) {
                existing.getNode().remove();
            }
            else if (existing.getPosition() == pos) {
                existing.slideStart(rsize);
                existing = next; break;
            }
            else {
                // the record begins before the point to be removed.
                int existingEnd = existing.getEnd();
                existing.setSize(pos - existing.getPosition());
                // Now we may have to insert a new node, if the middle portion
                // of the existing node was removed.
                if (existingEnd > pos + size) {
                    scheduleReparse(pos + size, existingEnd - (pos + size));
                    return;
                }
            }
            existing = next;
        }

        while (existing != null && existing.getPosition() < pos + size) {
            int rsize = pos + size - existing.getPosition();
            if (rsize < existing.getSize()) {
                existing.slideStart(rsize);
                return;
            }
            NodeAndPosition<ReparseRecord> next = existing.nextSibling();
            existing.getNode().remove();
            existing = next;
        }
    }

    private void repaintLines(int offset, int length, boolean restyle)
    {
        int startLine = document.getLineFromPosition(offset);
        int endLine = document.getLineFromPosition(offset + length);
        recalculateScopes(pendingScopeBackgrounds, startLine, endLine);
    }


    @Override
    public void lineVisibilityChanged(int fromLineIndexIncl, int toLineIndexIncl)
    {
        JavaFXUtil.runAfterNextLayout(editorPane.getScene(), () -> {
            scopeBackgrounds.clear();
            // We repaint all because the vertical positions will have changed:
            recalculateScopes(pendingScopeBackgrounds, 
                Math.min(fromLineIndexIncl, editorPane.getDocument().getLineCount() -1), 
                Math.min(toLineIndexIncl, editorPane.getDocument().getLineCount() - 1));
            applyPendingScopeBackgrounds();
        });
    }

    private void scheduleReparseRunner()
    {
        if (reparseRunner == null)
        {
            if (editorPane.getScene() == null)
            {
                JavaFXUtil.onceNotNull(editorPane.sceneProperty(), s -> scheduleReparseRunner());
            }
            else
            {
                reparseRunner = new FlowReparseRunner();
                // Wait until after layout to do a reparse (as that may involve asking for positions of 
                // characters on screen -- which will not give a valid answer until after the layout:

                JavaFXUtil.runAfterNextLayout(editorPane.getScene(), reparseRunner);
                editorPane.requestLayout();
            }
        }
    }

    public ParsedCUNode getRootNode()
    {
        return rootNode;
    }

    /**
     * Process the document re-parse queue.
     * 
     * <p>This is a Runnable which runs on the Swing/AWT event queue. It performs
     * a small amount of re-parsing before re-queing itself, which allows input
     * to be processed in the meantime.
     * 
     * @author Davin McCall
     */
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    private class FlowReparseRunner implements FXPlatformRunnable
    {
        private int procTime; //the time allowed for the incremental parsing before re-queueing
        
        public FlowReparseRunner()
        {
            this.procTime = 15;
        }
        
        public void run()
        {
            long begin = System.currentTimeMillis();
            if (document != null && pollReparseQueue()) {
                // Continue processing
                while (System.currentTimeMillis() - begin < this.procTime) {
                    if (! pollReparseQueue()) {
                        break;
                    }
                }
                JavaFXUtil.runPlatformLater(this);
            }
            else {
                // Mark that we are no longer scheduled.  Reapply backgrounds and syntax highlighting:
                applyPendingScopeBackgrounds();
                editorPane.repaint();
                reparseRunner = null;
            }
        }
    }
}
