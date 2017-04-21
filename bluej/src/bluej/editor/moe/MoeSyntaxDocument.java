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

import java.awt.Color;
import java.util.*;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Segment;

import bluej.editor.moe.BlueJSyntaxView.ParagraphAttribute;
import bluej.editor.moe.BlueJSyntaxView.ScopeInfo;
import javafx.beans.binding.BooleanExpression;
import org.fxmisc.richtext.model.*;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import org.reactfx.Subscription;
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
import bluej.utility.PersistentMarkDocument;


/**
 * An implementation of PlainDocument, with an optional added parser to provide
 * syntax highlighting, scope highlighting, and other advanced functionality.
 *
 * @author Bruce Quig
 * @author Jo Wood (Modified to allow user-defined colours, March 2001)
 */
public class MoeSyntaxDocument
{
    private final SimpleEditableStyledDocument<ScopeInfo, Integer> document;

    @OnThread(value = Tag.Any, requireSynchronized = true)
    private static Color[] colors = null;

    @OnThread(value = Tag.Any, requireSynchronized = true)
    private static Color defaultColour = null;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private static Color backgroundColour = null;
    
    /** Maximum amount of document to reparse in one hit (advisory) */
    private final static int MAX_PARSE_PIECE = 8000;
    private final int tabSize;

    private ParsedCUNode parsedNode;
    private EntityResolver parentResolver;
    private NodeTree<ReparseRecord> reparseRecordTree;
    
    private MoeDocumentListener listener;
    
    /** Tasks scheduled for when we are not locked */ 
    private Runnable[] scheduledUpdates;
    protected boolean inNotification = false;
    protected boolean runningScheduledUpdates = false;
    private final BlueJSyntaxView syntaxView;

    public Position createPosition(int initialPos)
    {
        return new Position(initialPos);
    }

    private class PendingError
    {
        int position;
        int size;
        String errCode;
        
        PendingError(int position, int size, String errCode)
        {
            this.position = position;
            this.size = size;
            this.errCode = errCode;
        }
    }
    
    /** A list of parse errors which have been detected but not yet indicated to the listener. */
    private List<PendingError> pendingErrors = new LinkedList<PendingError>();
    
    /*
     * We'll keep track of recent events, to aid in hunting down bugs in the event
     * that we get an unexpected exception. 
     */
    
    private static int EDIT_INSERT = 0;
    private static int EDIT_DELETE = 1;
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
    
    /**
     * Create an empty MoeSyntaxDocument.
     */
    public MoeSyntaxDocument()
    {
        getUserColors();
        // defaults to 4 if cannot read property
        tabSize = Config.getPropInteger("bluej.editor.tabsize", 4);
        document = new SimpleEditableStyledDocument<>(null, 0);
        syntaxView = new BlueJSyntaxView(this);

        document.plainChanges().subscribe(c -> {
            // Must fire remove before insert:
            if (!c.getRemoved().isEmpty())
            {
                fireRemoveUpdate(c.getPosition(), c.getRemovalEnd() - c.getPosition());
            }
            if (!c.getInserted().isEmpty())
            {
                fireInsertUpdate(c.getPosition(), c.getInsertionEnd() - c.getPosition());
            }
        });
    }
    
    /**
     * Create an empty MoeSyntaxDocument, which uses the given entity resolver
     * to resolve symbols.
     */
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
     * Create an empty MoeSyntaxDocument, which uses the given entity resolver to
     * resolve symbols, and which sends parser events to the specified listener.
     */
    public MoeSyntaxDocument(EntityResolver parentResolver, MoeDocumentListener listener)
    {
        this(parentResolver);
        this.listener = listener;
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
        return pollReparseQueue(MAX_PARSE_PIECE);
    }
    
    /**
     * Run an item from the re-parse queue, if there are any, and attempt to
     * parse the specified amount of document (approximately). Return true if
     * a queued re-parse was processed or false if the queue was empty.
     */
    public boolean pollReparseQueue(int maxParse)
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

                    Debug.message("Reparsing: " + ppos + " " + pos);
                    MoeSyntaxEvent mse = new MoeSyntaxEvent(this);
                    pn.reparse(this, ppos, pos, maxParse, mse);
                    // Dump tree:
                    Debug.message("Dumping tree:");
                    dumpTree(parsedNode.getChildren(0), "");

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
            ScopeInfo style = document.getParagraphStyle(i);
            boolean newStepLine = i == (lineNumber - 1);
            if (style != null && style.isStepLine() != newStepLine)
            {
                setParagraphStyle(i, style.withStepLine(newStepLine));
            }
        }

    }

    void fireChangedUpdate(MoeSyntaxEvent mse)
    {
        syntaxView.updateDamage(mse);
    }

    void recalculateAllScopes()
    {
        recalculateScopesForLinesInRange(0, document.getParagraphs().size() - 1);
    }

    void recalculateScopesForLinesInRange(int firstLineIncl, int lastLineIncl)
    {
        List<ScopeInfo> paragraphScopeInfo = syntaxView.recalculateScopes(this, firstLineIncl, lastLineIncl);
        if (paragraphScopeInfo.isEmpty())
            return; // Not initialised yet
        for (int i = firstLineIncl; i < lastLineIncl; i++)
        {
            ScopeInfo old = document.getParagraphStyle(i);
            ScopeInfo newStyle = paragraphScopeInfo.get(i - firstLineIncl);
            if (((old == null) != (newStyle == null)) || !old.equals(newStyle))
            {
                setParagraphStyle(i, newStyle);

            }

            StyleSpans<Integer> styleSpans = syntaxView.getTokenStylesFor(i, this);
            if (styleSpans != null)
                document.setStyleSpans(i, 0, styleSpans);
        }
    }

    private void setParagraphStyle(int i, ScopeInfo newStyle)
    {
        //MOEFX Stop working around RichTextFX bug post 0.7 release
        //document.setParagraphStyle(i, paragraphScopeInfo.get(i));
        Paragraph<ScopeInfo, StyledText<Integer>, Integer> oldPara = document.getParagraph(i);
        int startPos = document.getAbsolutePosition(i, 0);
        int endPos = (i == document.getParagraphs().size() - 1) ? document.getLength() : (document.getAbsolutePosition(i + 1, 0) - 1);
        document.replace(startPos, endPos, ReadOnlyStyledDocument.fromString(oldPara.getText(), newStyle, 0, StyledText.textOps()));
    }

    /**
     * Process all of the re-parse queue.
     */
    public void flushReparseQueue()
    {
        while (pollReparseQueue(getLength())) ;
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
        
        // We must first report the range reparsed, and then report and parse errors in the range
        // to the listener.
        if (listener != null) {
            listener.reparsingRange(pos, size);
            Iterator<PendingError> i = pendingErrors.iterator();
            while (i.hasNext()) {
                PendingError pe = i.next();
                if (pe.position >= pos && pe.position <= pos + size) {
                    listener.parseError(pe.position, pe.size, pe.errCode);
                    i.remove();
                }
            }
        }
        
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
        if (listener != null) {
            pendingErrors.add(new PendingError(position, size, message));
        }
    }
    
    /**
     * Sets attributes for a particular paragraph.
     *
     * @param offset the offset into the document which is contained by the paragraph to change.
     * @param alterAttr the attributes to set the value for (other attributes will be unaffected)
     */
    public void setParagraphAttributes(int offset, Map<ParagraphAttribute, Boolean> alterAttr)
    {
        syntaxView.setParagraphAttributes(document.offsetToPosition(offset, Bias.Forward).getMajor() + 1, alterAttr);
    }
    
    /**
     * Get the default colour for MoeSyntaxDocuments.
     */
    public static synchronized Color getDefaultColor()
    {
        return defaultColour;
    }
    
    /**
     * Get the background colour for MoeSyntaxDocuments.
     */
    @OnThread(Tag.Any)
    public static synchronized Color getBackgroundColor()
    {
        return backgroundColour;
    }
    
    /**
     * Get an array of colours as specified in the configuration file for different
     * token types. The indexes for each token type are defined in the Token class.
     */
    @OnThread(Tag.Any)
    public static Color[] getColors()
    {
        return getUserColors();
    }
    
    /**
     * Allows user-defined colours to be set for syntax highlighting. The file
     * containing the colour values is 'lib/moe.defs'. If this file is
     * not found, or not all colours are defined, the BlueJ default colours are
     * used.
     * 
     * @author This method was added by Jo Wood (jwo@soi.city.ac.uk), 9th March,
     *         2001.
     */
    @OnThread(Tag.Any)
    private static synchronized Color[] getUserColors()
    { 
        if(colors == null) {
            // Replace with user-defined colours.
            int    colorInt;
                        
            // First determine default colour and background colour
            colorInt = getPropHexInt("other", 0x000000);
            defaultColour = new Color(colorInt);
            
            colorInt = getPropHexInt("background", 0x000000);
            backgroundColour = new Color(colorInt);

            // Build colour table.     
            colors = new Color[Token.ID_COUNT];

            // Comments.
            colorInt = getPropHexInt("comment", 0x1a1a80);
            colors[Token.COMMENT1] = new Color(colorInt);    

            // Javadoc comments.
            colorInt = getPropHexInt("javadoc", 0x1a1a80);
            colors[Token.COMMENT2] = new Color(colorInt);

            // Stand-out comments (/*#).
            colorInt = getPropHexInt("stand-out", 0xee00bb);
            colors[Token.COMMENT3] = new Color(colorInt);

            // Java keywords.
            colorInt = getPropHexInt("keyword1", 0x660033);
            colors[Token.KEYWORD1] = new Color(colorInt);

            // Class-based keywords.
            colorInt = getPropHexInt("keyword2", 0xcc8033);
            colors[Token.KEYWORD2] = new Color(colorInt);

            // Other Java keywords (true, false, this, super).
            colorInt = getPropHexInt("keyword3", 0x006699);
            colors[Token.KEYWORD3] = new Color(colorInt);

            // Primitives.
            colorInt = getPropHexInt("primitive", 0xcc0000);
            colors[Token.PRIMITIVE] = new Color(colorInt);

            // String literals.
            colorInt = getPropHexInt("string", 0x339933);
            colors[Token.LITERAL1] = new Color(colorInt);

            // Labels
            colorInt = getPropHexInt("label", 0x999999);
            colors[Token.LABEL] = new Color(colorInt);
            
            // Invalid (eg unclosed string literal)
            colorInt = getPropHexInt("invalid", 0xff3300);
            colors[Token.INVALID] = new Color(colorInt);
            
            // Operator is not produced by token marker
            colors[Token.OPERATOR] = new Color(0xcc9900);
        }
        return colors;
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
    
    /**
     * Schedule a document update to be run at a suitable time (after all current
     * document notifications have been dispatched to listeners). This can be
     * used to avoid locking issues (AbstractDocument does not allow document
     * modifications to be made from within listener callbacks).
     */
    public void scheduleUpdate(Runnable r)
    {
        if (! inNotification) {
            // If we're not actually in the listener notification, just run the update immediately:
            r.run();
            return;
        }
        
        if (scheduledUpdates == null) {
            scheduledUpdates = new Runnable[1];
        }
        else {
            Runnable[] newScheduledTasks = new Runnable[scheduledUpdates.length + 1];
            System.arraycopy(scheduledUpdates, 0, newScheduledTasks, 0, scheduledUpdates.length);
            scheduledUpdates = newScheduledTasks;
        }
        
        scheduledUpdates[scheduledUpdates.length - 1] = r;        
    }

    /**
     * Run any scheduled document updates. This is called from update handlers.
     */
    private void runScheduledUpdates()
    {
        // Sometimes a callback wants to modify the document. AbstractDocument doesn't allow that;
        // we have 'scheduled updates' to work around the problem.
        if (scheduledUpdates != null && ! runningScheduledUpdates) {
            // Mark the queue as running, to avoid running it twice:
            runningScheduledUpdates = true;
            for (int i = 0; i < scheduledUpdates.length; i++) {
                // Note the callback may schedule further updates!
                // They will be appended to the array, and so will be
                // processed after any updates that are already pending.
                scheduledUpdates[i].run();
            }
            scheduledUpdates = null;
            runningScheduledUpdates = false;
        }
    }
    
    /**
     * Check if scheduled updates are being run presently. This might be used as a cue to
     * recognize that document updates do not need processing in the normal fashion,
     * because they have been generated automatically rather than being due to user input. 
     * 
     * @return  true if scheduled updates are currently being run
     */
    public boolean isRunningScheduledUpdates()
    {
        return runningScheduledUpdates;
    }
    
    /**
     * Get an integer value from a property whose value is hex-encoded.
     * @param propName  The name of the property
     * @param def       The default value if the property is undefined or
     *                  not parseable as a hexadecimal
     * @return  The value
     */
    @OnThread(Tag.Any)
    private static int getPropHexInt(String propName, int def)
    {
        String strVal = Config.getPropString(propName, null, Config.moeUserProps);
        try {
            return Integer.parseInt(strVal, 16);
        }
        catch (NumberFormatException nfe) {
            return def;
        }
    }
    
    /* 
     * If text was inserted, the reparse-record tree needs to be updated.
     */
    protected void fireInsertUpdate(int offset, int length)
    {
        inNotification = true;
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

        //MOEFX: Why was this here?
        //MoeSyntaxEvent mse = new MoeSyntaxEvent(this, e);
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
        //MOEFX: sort out undo
        //recordEvent(e);
        // MOEFX
        //super.fireInsertUpdate(mse);
        int startLine = document.offsetToPosition(offset, Bias.Forward).getMajor();
        int endLine = document.offsetToPosition(offset + length, Bias.Forward).getMajor();
        recalculateScopesForLinesInRange(startLine, endLine);
        inNotification = false;

        runScheduledUpdates();
    }
    
    
    /* 
     * If part of the document was removed, the reparse-record tree needs to be updated.
     */
    protected void fireRemoveUpdate(int offset, int length)
    {
        inNotification = true;
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

        //MOEFX: Why was this here?
        //MoeSyntaxEvent mse = new MoeSyntaxEvent(this, e);
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
        //MOEFX: Sort out undo
        //recordEvent(e);
        //MOEFX
        //super.fireRemoveUpdate(mse);
        int line = document.offsetToPosition(offset, Bias.Forward).getMajor();
        recalculateScopesForLinesInRange(line, line);
        inNotification = false;
        
        runScheduledUpdates();
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
    public MoeEditorPane makeEditorPane(MoeEditor editor, BooleanExpression compiledStatus)
    {
        return new MoeEditorPane(editor, document, syntaxView, compiledStatus);
    }

    /**
     * Notify that a certain area of the document needs repainting.
     */
    public void repaintLines(int offset, int length)
    {
        int startLine = document.offsetToPosition(offset, Bias.Forward).getMajor();
        int endLine = document.offsetToPosition(offset + length, Bias.Forward).getMajor();
        recalculateScopesForLinesInRange(startLine, endLine);
    }

    public int getLength()
    {
        return document.getLength();
    }

    public String getText(int start, int length)
    {
        return document.getText(start, start + length);
    }

    public void getText(int startOffset, int length, Segment segment)
    {
        String s = getText(startOffset, length);
        segment.array = s.toCharArray();
        segment.offset = 0;
        segment.count = s.length();
    }

    public void insertString(int start, String src, Object attrSet)
    {
        document.replace(start, start, ReadOnlyStyledDocument.fromString(src, null, 0, StyledText.textOps()));
    }

    public void replace(int start, int length, String text)
    {
        document.replace(start, start + length, ReadOnlyStyledDocument.fromString(text, null, 0, StyledText.textOps()));
    }

    public void remove(int start, int length)
    {
        document.replace(start, start + length, new SimpleEditableStyledDocument<>(null, 0));
    }


    /**
     * First line is one
     */
    public EnumSet<ParagraphAttribute> getParagraphAttributes(int lineNo)
    {
        return syntaxView.getParagraphAttributes(lineNo);
    }

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
        return new Element()
        {
            @Override
            public Element getElement(int index)
            {
                Paragraph<ScopeInfo, StyledText<Integer>, Integer> p = document.getParagraph(index);
                int pos = document.getAbsolutePosition(index, 0);
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
                        return pos + p.length() + (index == document.getParagraphs().size() - 1 ? 0 : 1 /* newline */);
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
                return document.offsetToPosition(offset, Bias.Forward).getMajor();
            }

            @Override
            public int getElementCount()
            {
                return document.getParagraphs().size();
            }
        };
    }

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
