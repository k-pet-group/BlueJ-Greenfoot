/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012,2013,2014,2015,2016,2017  Michael Kolling and John Rosenberg
 
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

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Segment;

import bluej.editor.moe.BlueJSyntaxView.ParagraphAttribute;
import bluej.editor.moe.BlueJSyntaxView.ScopeInfo;
import bluej.editor.moe.Token.TokenType;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;
import com.google.common.collect.ImmutableSet;
import javafx.beans.binding.BooleanExpression;
import org.fxmisc.richtext.model.*;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import org.reactfx.Subscription;
import org.reactfx.collection.LiveList;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Config;
import bluej.parser.entity.EntityResolver;
import bluej.parser.nodes.NodeStructureListener;
import bluej.parser.nodes.NodeTree;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import bluej.utility.Debug;


/**
 * An implementation of PlainDocument, with an optional added parser to provide
 * syntax highlighting, scope highlighting, and other advanced functionality.
 *
 * @author Bruce Quig
 * @author Jo Wood (Modified to allow user-defined colours, March 2001)
 */
@OnThread(Tag.FXPlatform)
public class MoeSyntaxDocument
{
    public static final String MOE_FIND_RESULT = "moe-find-result";
    public static final String MOE_BRACKET_HIGHLIGHT = "moe-bracket-highlight";

    /**
     * A RichTextFX document can have paragraph styles, and text-segment styles.
     *
     * We use RichTextFX such that one RichTextFX paragraph = one line of Java source code.
     * Our paragraph styles are thus line styles, and we use this to store information
     * about the scope boxes that need painting on the background for scope highlighting.
     *
     * Our text-segment styles are a set of style-classes to apply to a text-segment.
     * This is usually a single token-style (our tokens never overlap), and possibly
     * the error state class and/or search highlight class.
     */
    private final SimpleEditableStyledDocument<ScopeInfo, ImmutableSet<String>> document;
    
    /** Maximum amount of document to reparse in one hit (advisory) */
    private final static int MAX_PARSE_PIECE = 8000;
    private final int tabSize;

    private ParsedCUNode parsedNode;
    private EntityResolver parentResolver;
    private NodeTree<ReparseRecord> reparseRecordTree;

    /**
     * We want to avoid repaint flicker by letting the user see partly-updated
     * backgrounds when processing the reparse queue.  So we store new backgrounds
     * in this map until we're ready to show all new backgrounds, which typically
     * happens when the reparse queue is empty (and is done by the applyPendingScopeBackgrounds()
     * method)
     */
    private final Map<Integer, ScopeInfo> pendingScopeBackgrounds = new HashMap<>();
    private boolean applyingScopeBackgrounds = false;

    // Can be null if we are not being used for an editor pane:
    private final BlueJSyntaxView syntaxView;
    private boolean hasFindHighlights = false;
    // null means not cached, non-null means cached
    private String cachedContent = null;
    // null means not cached, non-null means cached
    // lineStarts[0] is 0, lineStarts[1] is the index of beginning of the next line (i.e. just after \n), and so on.
    private int[] lineStarts = null;
    // package-visible:
    boolean notYetShown = true;

    /**
     * Is this pane an off-screen item just for printing?
     */
    private boolean thisDocIsForPrinting = false;


    public Position createPosition(int initialPos)
    {
        return new Position(initialPos);
    }

    public void markFindResult(int start, int end)
    {
        hasFindHighlights = true;
        document.setStyleSpans(start, document.getStyleSpans(start, end).mapStyles(ss -> {
            // Add special/non-special and remove the converse:
            return Utility.setAdd(ss, MOE_FIND_RESULT);

        }));
    }

    public void removeSearchHighlights()
    {
        if (hasFindHighlights)
        {
            removeStyleThroughout(MOE_FIND_RESULT);
            hasFindHighlights = false;
        }
    }

    public void removeStyleThroughout(String spanStyle)
    {
        // Goes paragraph by paragraph, and only alters the style if necessary.
        LiveList<Paragraph<ScopeInfo, StyledText<ImmutableSet<String>>, ImmutableSet<String>>> paragraphs = document.getParagraphs();
        for (int i = 0; i < paragraphs.size(); i++)
        {
            Paragraph<ScopeInfo, StyledText<ImmutableSet<String>>, ImmutableSet<String>> para = paragraphs.get(i);
            StyleSpans<ImmutableSet<String>> styleSpans = para.getStyleSpans();
            boolean present = styleSpans.stream().anyMatch(s -> s.getStyle().contains(spanStyle));
            if (present)
            {
                document.setStyleSpans(i, 0, styleSpans.mapStyles(ss -> Utility.setMinus(ss, spanStyle)));
            }
        }
    }

    public void addStyle(int start, int end, String style)
    {
        document.setStyleSpans(start, document.getStyleSpans(start, end).mapStyles(ss -> Utility.setAdd(ss, style)));
    }

    /*
     * We'll keep track of recent events, to aid in hunting down bugs in the event
     * that we get an unexpected exception. 
     */
    
    private static int EDIT_INSERT = 0;
    private static int EDIT_DELETE = 1;

    public void copyFrom(MoeSyntaxDocument from)
    {
        document.replace(0, document.getLength(), from.document);
    }

    public TwoDimensional.Position offsetToPosition(int startOffset)
    {
        if (lineStarts == null)
        {
            return document.offsetToPosition(startOffset, Bias.Forward);
        }
        else
        {
            // No need to check first line:
            int line = 1;
            for (; line < lineStarts.length; line++)
            {
                if (startOffset < lineStarts[line])
                    break;

            }
            line -= 1; // We went just past it, so now go back one
            int lineFinal = line;
            int column = startOffset - lineStarts[lineFinal];
            return new TwoDimensional.Position()
            {
                @Override
                public TwoDimensional getTargetObject()
                {
                    return document;
                }

                @Override
                public int getMajor()
                {
                    return lineFinal;
                }

                @Override
                public int getMinor()
                {
                    return column;
                }

                @Override
                public boolean sameAs(TwoDimensional.Position other)
                {
                    return getTargetObject() == other.getTargetObject() && getMajor() == other.getMajor() && getMinor() == other.getMinor();
                }

                @Override
                public TwoDimensional.Position clamp()
                {
                    return this;
                }

                @Override
                public TwoDimensional.Position offsetBy(int offset, Bias bias)
                {
                    // Just fall back to document, don't think we call this anyway:
                    return document.offsetToPosition(startOffset + offset, Bias.Forward);
                }

                @Override
                public int toOffset()
                {
                    return startOffset;
                }
            };
        }
    }

    private int getAbsolutePosition(int lineIndex, int columnIndex)
    {
        if (lineStarts == null || lineIndex >= lineStarts.length) // Second part shouldn't happen, but just in case
        {
            return document.getAbsolutePosition(lineIndex, columnIndex);
        }
        else
        {
            return lineStarts[lineIndex] + columnIndex;
        }
    }

    /**
     * Marks this document as an off-screen copy for printing.  Note: you must call this before
     * altering the content of the document, or else the run-later we are suppressing with this flag
     * will already have been run.
     */
    public void markAsForPrinting()
    {
        thisDocIsForPrinting = true;
    }

    public boolean isPrinting()
    {
        return thisDocIsForPrinting;
    }

    @OnThread(Tag.Any)
    private static class EditEvent
    {
        int type; //  edit type - INSERT or DELETE
        int offset;
        int length;
    }
    
    private List<EditEvent> recentEdits = new LinkedList<EditEvent>();
    
    private void recordEvent(DocumentEvent event)
    {
        int type;
        if (event.getType() == DocumentEvent.EventType.INSERT) {
            type = EDIT_INSERT;
        }
        else if (event.getType() == DocumentEvent.EventType.REMOVE) {
            type = EDIT_DELETE;
        }
        else {
            return;
        }
        
        EditEvent eevent = new EditEvent();
        eevent.type = type;
        eevent.offset = event.getOffset();
        eevent.length = event.getLength();
        recentEdits.add(eevent);
        
        if (recentEdits.size() > 10) {
            recentEdits.remove(0);
        }
    }

    // Have to pass construction function because "this" isn't
    // available to other constructor callers:
    private MoeSyntaxDocument(Function<MoeSyntaxDocument, BlueJSyntaxView> makeSyntaxView)
    {
        // defaults to 4 if cannot read property
        tabSize = Config.getPropInteger("bluej.editor.tabsize", 4);
        document = new SimpleEditableStyledDocument<>(null, ImmutableSet.of());
        this.syntaxView = makeSyntaxView.apply(this);

        document.plainChanges().subscribe(c -> {
            invalidateCache();
            // Must fire remove before insert:
            if (!c.getRemoved().isEmpty())
            {
                fireRemoveUpdate(c.getPosition(), c.getRemovalEnd() - c.getPosition());
            }
            if (!c.getInserted().isEmpty())
            {
                fireInsertUpdate(c.getPosition(), c.getInsertionEnd() - c.getPosition());
            }
            // Don't attempt a run-later when printing as we'll be on a different thread, and we don't
            // print the scopes anyway:
            if (!thisDocIsForPrinting)
            {
                // Apply backgrounds from simple update, as it may not even
                // trigger a reparse.  This must be done later, after the document has finished
                // doing all the updates to the content, before we can mess with paragraph styles:
                JavaFXUtil.runAfterCurrent(() -> {
                    invalidateCache();
                    applyPendingScopeBackgrounds();
                });
            }
        });
    }

    private void invalidateCache()
    {
        cachedContent = null;
        lineStarts = null;
    }

    private void cacheContent()
    {
        if (cachedContent == null)
            cachedContent = document.getText();
        if (lineStarts == null)
        {
            lineStarts = new int[document.getParagraphs().size()];
            lineStarts[0] = 0;
            int curLine = 0;
            for (int character = 0; character < cachedContent.length(); character++)
            {
                if (cachedContent.charAt(character) == '\n')
                {
                    curLine += 1;
                    lineStarts[curLine] = character + 1;
                }
            }
        }
    }

    public MoeSyntaxDocument()
    {
        this(d -> null);
    }
    
    /**
     * Create an empty MoeSyntaxDocument.
     */
    @OnThread(Tag.FXPlatform)
    public MoeSyntaxDocument(ScopeColors scopeColors)
    {
        this(d -> new BlueJSyntaxView(d, scopeColors));
    }
    
    /**
     * Create an empty MoeSyntaxDocument, which uses the given entity resolver
     * to resolve symbols.
     */
    @OnThread(Tag.FXPlatform)
    public MoeSyntaxDocument(EntityResolver parentResolver, ScopeColors scopeColors)
    {
        this(scopeColors);
        // parsedNode = new ParsedCUNode(this);
        this.parentResolver = parentResolver;
        if (parentResolver != null) {
            reparseRecordTree = new NodeTree<ReparseRecord>();
        }
    }

    /**
     * Create an empty MoeSyntaxDocument, which uses the given entity resolver
     * to resolve symbols.
     */
    @OnThread(Tag.FXPlatform)
    public MoeSyntaxDocument(EntityResolver parentResolver)
    {
        this();
        // parsedNode = new ParsedCUNode(this);
        this.parentResolver = parentResolver;
        if (parentResolver != null) {
            reparseRecordTree = new NodeTree<ReparseRecord>();
        }
    }

    /**
     * Access the parsed node structure of this document.
     */
    public ParsedCUNode getParser()
    {
        flushReparseQueue();
        return parsedNode;
    }
    
    /**
     * Get the current parsed node structure of the document, without processing any
     * pending re-parse operations first.
     */
    public ParsedCUNode getParsedNode()
    {
        return parsedNode;
    }
    
    /**
     * Enable the parser. This should be called after loading a document.
     * @param force  whether to force-enable the parser. If false, the parser will only
     *                be enabled if an entity resolver is available.
     */
    @OnThread(Tag.FXPlatform)
    public void enableParser(boolean force)
    {
        if (parentResolver != null || force) {
            parsedNode = new ParsedCUNode(this);
            parsedNode.setParentResolver(parentResolver);
            reparseRecordTree = new NodeTree<ReparseRecord>();
            parsedNode.textInserted(this, 0, 0, getLength(), new NodeStructureListener() {
                public void nodeRemoved(NodeAndPosition<ParsedNode> node) { }
                public void nodeChangedLength(NodeAndPosition<ParsedNode> node,
                        int oldPos, int oldSize) { }
            });
        }
    }

    /**
     * Run an item from the re-parse queue, if there are any. Return true if
     * a queued re-parse was processed or false if the queue was empty.
     */
    public boolean pollReparseQueue()
    {
        boolean wasParsed = pollReparseQueue(MAX_PARSE_PIECE);
        // If queue is empty, apply backgrounds:
        if (!wasParsed)
        {
            applyPendingScopeBackgrounds();
        }
        return wasParsed;
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

                ParsedNode pn = parsedNode;
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
                    MoeSyntaxEvent mse = new MoeSyntaxEvent(this, -1, -1, false, false);
                    pn.reparse(this, ppos, pos, maxParse, mse);
                    // Dump tree (for debugging):
                    //Debug.message("Dumping tree:");
                    //dumpTree(parsedNode.getChildren(0), "");

                    fireChangedUpdate(mse);
                    return true;
                }
            }
            return false;
        }
        catch (RuntimeException e) {
            
            Debug.message("Exception during incremental parsing. Recent edits:");
            for (EditEvent event : recentEdits) {
                String eventStr = event.type == EDIT_INSERT ? "insert " : "delete ";
                eventStr += "offset=" + event.offset + " length=" + event.length;
                Debug.message(eventStr);
            }

            Debug.message("--- Source code ---");
            Debug.message(getText(0, getLength()));
            Debug.message("--- Source ends ---");
            
            throw e;
        }
    }

    private void dumpTree(Iterator<NodeAndPosition<ParsedNode>> iterator, String indent)
    {
        for (NodeAndPosition<ParsedNode> nap2 : (Iterable<NodeAndPosition<ParsedNode>>)(() -> iterator))
        {
            Debug.message(indent + "Node: " + nap2.getPosition() + " -> " + nap2.getEnd());
            dumpTree(nap2.getNode().getChildren(nap2.getPosition()), indent + "  ");
        }
    }

    /**
     * Sets the step-line (paused line in debugger) indicator on that line,
     * and clears it from all other lines.
     *
     * Line number starts at one.
     */
    public void showStepLine(int lineNumber)
    {
        for (int i = 0; i < document.getParagraphs().size(); i++)
        {
            // Line numbers start at 1:
            setParagraphAttributesForLineNumber(i + 1, Collections.singletonMap(ParagraphAttribute.STEP_MARK, i + 1 == lineNumber));
        }
    }

    void fireChangedUpdate(MoeSyntaxEvent mse)
    {
        if (syntaxView != null)
            syntaxView.updateDamage(mse);
        if (mse == null)
        {
            // Width change, so apply new backgrounds:
            applyPendingScopeBackgrounds();
        }
    }

    void recalculateAllScopes()
    {
        if (notYetShown)
            return;

        syntaxView.resetColors();
        recalculateScopesForLinesInRange(0, document.getParagraphs().size() - 1);
        applyPendingScopeBackgrounds();
    }

    void recalculateScopesForLinesInRange(int firstLineIncl, int lastLineIncl)
    {
        if (syntaxView == null)
            return;
        cacheContent();
        List<ScopeInfo> paragraphScopeInfo = syntaxView.recalculateScopes(firstLineIncl, lastLineIncl);
        if (paragraphScopeInfo.isEmpty())
            return; // Not initialised yet
        for (int i = 0; i < paragraphScopeInfo.size(); i++)
        {
            pendingScopeBackgrounds.put(i + firstLineIncl, paragraphScopeInfo.get(i));
        }
    }

    // Called if the reparse queue is empty:
    private void applyPendingScopeBackgrounds()
    {
        // Prevent re-entry, which can it seems can occur when applying
        // token highlight styles:
        if (applyingScopeBackgrounds)
            return;
        // No point doing scopes (in fact, not possible) if there's no editor involved:
        if (syntaxView == null || syntaxView.editorPane == null)
            return;
        applyingScopeBackgrounds = true;
        // Setting paragraph styles can cause RichTextFX to scroll the window, which we
        // don't want.  So we save and restore the scroll Y:
        double scrollY = syntaxView.editorPane.getEstimatedScrollY();

        // Take a copy of backgrounds to avoid concurrent modification:
        Set<Entry<Integer, ScopeInfo>> pendingBackgrounds = new HashMap<>(pendingScopeBackgrounds).entrySet();
        pendingScopeBackgrounds.clear();

        for (Entry<Integer, ScopeInfo> pending : pendingBackgrounds)
        {
            if (pending.getKey() >= document.getParagraphs().size())
                continue; // Line doesn't exist any more

            ScopeInfo old = document.getParagraphStyle(pending.getKey());
            ScopeInfo newStyle = pending.getValue();
            if (((old == null) != (newStyle == null)) || !old.equals(newStyle))
            {
                setParagraphStyle(pending.getKey(), newStyle);

            }

            StyleSpans<ImmutableSet<String>> styleSpans = syntaxView.getTokenStylesFor(pending.getKey(), this);
            // Applying style spans is expensive, so don't do it if they're already correct:
            if (styleSpans != null && !styleSpans.equals(document.getStyleSpans(pending.getKey())))
            {
                document.setStyleSpans(pending.getKey(), 0, document.getStyleSpans(pending.getKey()).overlay(styleSpans, MoeSyntaxDocument::setTokenStyles));
            }
        }

        // The setParagraphStyle causes a layout-request with request-follow-caret.  We have to
        // purge that layout request by executing it, before we restore the scroll Y:
        syntaxView.editorPane.layout();
        syntaxView.editorPane.setEstimatedScrollY(scrollY);
        // Setting the estimated scroll Y requests a layout but does not perform it.  This seemed
        // to lead to occasional scroll jumps, I think involving delayed layout passes.  So although
        // it is getting silly, we enforce another layout to *actually* set the scroll position:
        syntaxView.editorPane.layout();

        applyingScopeBackgrounds = false;
    }

    /**
     * Removes any existing token styles from allStyles, then adds them in from newTokenStyle.
     */
    private static ImmutableSet<String> setTokenStyles(ImmutableSet<String> allStyles, ImmutableSet<String> newTokenStyle)
    {
        return Utility.setUnion(Utility.setMinus(allStyles, TokenType.allCSSClasses()), newTokenStyle);
    }

    private void setParagraphStyle(int i, ScopeInfo newStyle)
    {
        document.setParagraphStyle(i, newStyle);
    }

    /**
     * Process all of the re-parse queue.
     */
    public void flushReparseQueue()
    {
        while (pollReparseQueue(getLength())) ;
        // Queue now empty, so flush backgrounds:
        applyPendingScopeBackgrounds();
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
     * Mark a portion of the document as having been parsed. This removes any
     * scheduled re-parses as appropriate and repaints the appropriate area.
     */
    public void markSectionParsed(int pos, int size)
    {
        repaintLines(pos, size);

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
    
    /**
     * Inform any listeners that a parse error has occurred.
     * 
     * @param position   The position of the parse error
     * @param size       The size of the erroneous portion
     * @param message    The error message
     */
    public void parseError(int position, int size, String message)
    {
    }

    /**
     *
     * @param lineNumber Line number (starts at 1)
     * @param alterAttr The attributes to alter (mapped to true means add, mapped to false means remove, not present means don't alter)
     */
    public void setParagraphAttributesForLineNumber(int lineNumber, Map<ParagraphAttribute, Boolean> alterAttr)
    {
        if (syntaxView == null)
            return;
        Map<Integer, EnumSet<ParagraphAttribute>> changedLines = syntaxView.setParagraphAttributes(lineNumber, alterAttr);
        updateScopesAfterParaAttrChange(changedLines);
    }

    private void updateScopesAfterParaAttrChange(Map<Integer, EnumSet<ParagraphAttribute>> changedLines)
    {
        for (Entry<Integer, EnumSet<ParagraphAttribute>> changedLine : changedLines.entrySet())
        {
            if (changedLine.getKey() - 1 < document.getParagraphs().size())
            {
                // Paragraph numbering starts at zero, but line numbers start at 1 so adjust:
                ScopeInfo prevStyle = document.getParagraphStyle(changedLine.getKey() - 1);
                if (prevStyle != null)
                {
                    setParagraphStyle(changedLine.getKey() - 1, prevStyle.withAttributes(changedLine.getValue()));
                }
            }
        }
    }

    /**
     * Sets attributes for all paragraphs.
     *
     * @param alterAttr the attributes to set the value for (other attributes will be unaffected)
     */
    public void setParagraphAttributes(Map<ParagraphAttribute, Boolean> alterAttr)
    {
        if (syntaxView == null)
            return;
        Map<Integer, EnumSet<ParagraphAttribute>> changedLines = syntaxView.setParagraphAttributes(alterAttr);
        updateScopesAfterParaAttrChange(changedLines);
    }
    
    /**
     * Identify the token types and positions in a line. This is used for syntax colouring.
     * @param line  The line number (0 based).
     */
    public Token getTokensForLine(int line)
    {
        Element lineEl = getDefaultRootElement().getElement(line);
        int pos = lineEl.getStartOffset();
        int length = lineEl.getEndOffset() - pos - 1;
        return parsedNode.getMarkTokensFor(pos, length, 0, this);
    }

    /* 
     * If text was inserted, the reparse-record tree needs to be updated.
     */
    protected void fireInsertUpdate(int offset, int length)
    {
        if (syntaxView != null)
        {
            syntaxView.setDuringUpdate(true);
        }
        if (reparseRecordTree != null) {
            NodeAndPosition<ReparseRecord> napRr = reparseRecordTree.findNodeAtOrAfter(offset);
            if (napRr != null) {
                if (napRr.getPosition() <= offset) {
                    napRr.getNode().resize(napRr.getSize() + length);
                }
                else {
                    napRr.getNode().slide(length);
                }
            }
        }

        MoeSyntaxEvent mse = new MoeSyntaxEvent(this, offset, length, true, false);
        if (parsedNode != null) {
            parsedNode.textInserted(this, 0, offset, length, new NodeStructureListener()
            {
                @Override
                public void nodeRemoved(NodeAndPosition<ParsedNode> node)
                {

                }

                @Override
                public void nodeChangedLength(NodeAndPosition<ParsedNode> node, int oldPos, int oldSize)
                {

                }
            });
        }
        fireChangedUpdate(mse);
        int startLine = document.offsetToPosition(offset, Bias.Forward).getMajor();
        int endLine = document.offsetToPosition(offset + length, Bias.Forward).getMajor();
        recalculateScopesForLinesInRange(startLine, endLine);
        if (syntaxView != null)
        {
            syntaxView.setDuringUpdate(false);
        }
    }
    
    
    /* 
     * If part of the document was removed, the reparse-record tree needs to be updated.
     */
    protected void fireRemoveUpdate(int offset, int length)
    {
        if (syntaxView != null)
        {
            syntaxView.setDuringUpdate(true);
        }
        NodeAndPosition<ReparseRecord> napRr = (reparseRecordTree != null) ?
                reparseRecordTree.findNodeAtOrAfter(offset) :
                    null;
        int rpos = offset;
        int rlen = length;
        if (napRr != null && napRr.getEnd() == rpos) {
            // Boundary condition
            napRr = napRr.nextSibling();
        }
        while (napRr != null && rlen > 0) {
            if (napRr.getPosition() < rpos) {
                if (napRr.getEnd() >= rpos + rlen) {
                    // remove middle
                    napRr.getNode().resize(napRr.getSize() - rlen);
                    break;
                }
                else {
                    // remove end and continue
                    int reduction = napRr.getEnd() - rpos;
                    napRr.getNode().resize(napRr.getSize() - reduction);
                    rlen -= reduction;
                    napRr = napRr.nextSibling();
                    continue;
                }
            }
            else if (napRr.getPosition() == rpos) {
                if (napRr.getEnd() > rpos + rlen) {
                    // remove beginning
                    napRr.getNode().resize(napRr.getSize() - rlen);
                    break;
                }
                else {
                    // remove whole node
                    napRr.getNode().remove();
                    napRr = reparseRecordTree.findNodeAtOrAfter(offset);
                    continue;
                }
            }
            else {
                // napRr position is greater than delete position
                if (napRr.getPosition() >= (rpos + rlen)) {
                    napRr.slide(-rlen);
                    break;
                }
                else if (napRr.getEnd() <= (rpos + rlen)) {
                    // whole node to be removed
                    NodeAndPosition<ReparseRecord> nextRr = napRr.nextSibling();
                    napRr.getNode().remove();
                    napRr = nextRr;
                    continue;
                }
                else {
                    // only a portion to be removed
                    int ramount = (rpos + rlen) - napRr.getPosition();
                    napRr.slideStart(ramount);
                    napRr.slide(-rlen);
                    break;
                }
            }
        }

        MoeSyntaxEvent mse = new MoeSyntaxEvent(this, offset, length, false, true);
        if (parsedNode != null) {
            parsedNode.textRemoved(this, 0, offset, length, new NodeStructureListener()
            {
                @Override
                public void nodeRemoved(NodeAndPosition<ParsedNode> node)
                {

                }

                @Override
                public void nodeChangedLength(NodeAndPosition<ParsedNode> node, int oldPos, int oldSize)
                {

                }
            });
        }
        fireChangedUpdate(mse);
        int line = document.offsetToPosition(offset, Bias.Forward).getMajor();
        recalculateScopesForLinesInRange(line, line);
        if (syntaxView != null)
        {
            syntaxView.setDuringUpdate(false);
        }
    }

    /**
     * Gets the RichTextFX document wrapped by this class.
     * The styles are deliberately wildcards as the actual types are
     * an implementation detail private to this class.
     * @return
     */
    public SimpleEditableStyledDocument<?, ?> getDocument()
    {
        return document;
    }

    /**
     * Placed in this class so we don't have to expose document's
     * inner types.
     * @return Creates a new MoeEditorPane for this document
     */
    @OnThread(Tag.FXPlatform)
    public MoeEditorPane makeEditorPane(MoeEditor editor, BooleanExpression compiledStatus)
    {
        return new MoeEditorPane(editor, document, syntaxView, compiledStatus);
    }

    /**
     * Notify that a certain area of the document needs repainting.
     */
    public void repaintLines(int offset, int length)
    {
        int startLine = offsetToPosition(offset).getMajor();
        int endLine = offsetToPosition(offset + length).getMajor();
        recalculateScopesForLinesInRange(startLine, endLine);
    }

    public int getLength()
    {
        return document.getLength();
    }

    public String getText(int start, int length)
    {
        if (cachedContent == null)
        {
            return document.getText(start, start + length);
        }
        else
        {
            return cachedContent.substring(start, start + length);
        }
    }

    public void getText(int startOffset, int length, Segment segment)
    {
        String s = getText(startOffset, length);
        segment.array = s.toCharArray();
        segment.offset = 0;
        segment.count = s.length();
    }

    public void insertString(int start, String text)
    {
        replace(start, 0, text);
    }

    public void replace(int start, int length, String text)
    {
        document.replace(start, start + length, ReadOnlyStyledDocument.fromString(text, null, ImmutableSet.of(), StyledText.textOps()));
    }

    public void remove(int start, int length)
    {
        if (length != 0)
            document.replace(start, start + length, new SimpleEditableStyledDocument<>(null, ImmutableSet.of()));
    }


    /**
     * First line is one
     */
    public EnumSet<ParagraphAttribute> getParagraphAttributes(int lineNo)
    {
        if (syntaxView != null)
            return syntaxView.getParagraphAttributes(lineNo);
        else
            return EnumSet.noneOf(ParagraphAttribute.class);
    }

    @OnThread(Tag.FXPlatform)
    public static interface Element
    {
        public Element getElement(int index);
        public int getStartOffset();
        public int getEndOffset();
        public int getElementIndex(int offset);
        public int getElementCount();
    }

    public Element getDefaultRootElement()
    {
        // This is a different kind of element, which is only there to return a wrapper for the paragraphs:
        return new Element()
        {
            @Override
            public Element getElement(int index)
            {
                if (index >= (lineStarts != null ? lineStarts.length : document.getParagraphs().size()))
                    return null;

                boolean lastPara = lineStarts != null ? (index == lineStarts.length - 1) : (index == document.getParagraphs().size() - 1);
                int paraLength;
                if (lineStarts == null)
                {
                    paraLength = document.getParagraph(index).length() + (lastPara ? 0 : 1 /* newline */);
                }
                else
                {
                    paraLength = lastPara ? (document.getLength() - lineStarts[index]) : lineStarts[index + 1] - lineStarts[index];
                }
                int pos = getAbsolutePosition(index, 0);
                return new Element()
                {
                    @Override
                    public Element getElement(int index)
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
                return offsetToPosition(offset).getMajor();
            }

            @Override
            public int getElementCount()
            {
                if (lineStarts != null)
                    return lineStarts.length;
                else
                    return document.getParagraphs().size();
            }
        };
    }

    @OnThread(Tag.Any)
    public class Position
    {
        private final Subscription subscription;
        private int position;

        public Position(int initial)
        {
            this.position = initial;
            subscription = document.plainChanges().subscribe(this::changed);
        }

        private void changed(PlainTextChange c)
        {
            if (c.getPosition() <= position)
            {
                // Insertion was before us, we need to adjust

                // First, adjust for removal.
                // If we are in removed section, go to start of removal, otherwise subtract length of removed section
                position -= Math.min(c.getRemovalEnd() - c.getPosition(), position - c.getPosition());
                // Now adjust for insertion
                position += (c.getInsertionEnd() - c.getPosition());
            }
            // Otherwise it's after us; we can ignore it
        }

        public void dispose()
        {
            subscription.unsubscribe();
        }

        public int getOffset()
        {
            return position;
        }
    }
}
