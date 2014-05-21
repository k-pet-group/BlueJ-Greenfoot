/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012,2013,2014  Michael Kolling and John Rosenberg 
 
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;

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
public class MoeSyntaxDocument extends PersistentMarkDocument
{
    private static Color[] colors = null;
    
    private static Color defaultColour = null;
    private static Color backgroundColour = null;
    
    /** Maximum amount of document to reparse in one hit (advisory) */
    private final static int MAX_PARSE_PIECE = 8000;
    
    private ParsedCUNode parsedNode;
    private EntityResolver parentResolver;
    private NodeTree<ReparseRecord> reparseRecordTree;
    
    private MoeDocumentListener listener;
    
    /** Tasks scheduled for when we are not locked */ 
    private Runnable[] scheduledUpdates;
    protected boolean inNotification = false;
    protected boolean runningScheduledUpdates = false;
    
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
        int tabSize = Config.getPropInteger("bluej.editor.tabsize", 4);
        putProperty(tabSizeAttribute, Integer.valueOf(tabSize));
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

                    MoeSyntaxEvent mse = new MoeSyntaxEvent(this);
                    pn.reparse(this, ppos, pos, maxParse, mse);
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
            
            try {
                Debug.message("--- Source code ---");
                Debug.message(getText(0, getLength()));
                Debug.message("--- Source ends ---");
            }
            catch (BadLocationException ble) { }
            
            throw e;
        }
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
     * Sets attributes for a paragraph.  This method was added to 
     * provide the ability to replicate DefaultStyledDocument's ability to 
     * set each lines attributes easily.
     * This is an added method for the BlueJ adaption of jedit's Syntax
     * package   
     *
     * @param offset the offset into the paragraph >= 0
     * @param length the number of characters affected >= 0
     * @param s the attributes
     * @param replace whether to replace existing attributes, or merge them
     */
    public void setParagraphAttributes(int offset, AttributeSet s)
    {
        // modified version of method from DefaultStyleDocument
        try {
            writeLock();
            
            Element paragraph = getParagraphElement(offset);
            MutableAttributeSet attr = 
                    (MutableAttributeSet) paragraph.getAttributes();
            attr.addAttributes(s);
        } finally {
            writeUnlock();
        }
    }
    
    /**
     * Get the default colour for MoeSyntaxDocuments.
     */
    public static Color getDefaultColor()
    {
        return defaultColour;
    }
    
    /**
     * Get the background colour for MoeSyntaxDocuments.
     */
    public static Color getBackgroundColor()
    {
        return backgroundColour;
    }
    
    /**
     * Get an array of colours as specified in the configuration file for different
     * token types. The indexes for each token type are defined in the Token class.
     */
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
    private static Color[] getUserColors()
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
     * Get an integer value from a property whose value is hex-encoded.
     * @param propName  The name of the property
     * @param def       The default value if the property is undefined or
     *                  not parseable as a hexadecimal
     * @return  The value
     */
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
    @Override
    protected void fireInsertUpdate(DocumentEvent e)
    {
        inNotification = true;
        if (reparseRecordTree != null) {
            NodeAndPosition<ReparseRecord> napRr = reparseRecordTree.findNodeAtOrAfter(e.getOffset());
            if (napRr != null) {
                if (napRr.getPosition() <= e.getOffset()) {
                    napRr.getNode().resize(napRr.getSize() + e.getLength());
                }
                else {
                    napRr.getNode().slide(e.getLength());
                }
            }
        }
        
        MoeSyntaxEvent mse = new MoeSyntaxEvent(this, e);
        if (parsedNode != null) {
            parsedNode.textInserted(this, 0, e.getOffset(), e.getLength(), mse);
        }
        recordEvent(e);
        super.fireInsertUpdate(mse);
        inNotification = false;
        
        // Sometimes a callback wants to modify the document. AbstractDocument doesn't allow that;
        // we have 'scheduled updates' to work around the problem.
        if (scheduledUpdates != null && ! runningScheduledUpdates) {
            // Mark the queue as running, to avoid running it twice:
            runningScheduledUpdates = true;
            for (int i = 0; i < scheduledUpdates.length; i++) {
                // Note the callback may schedule further updates!
                scheduledUpdates[i].run();
            }
            scheduledUpdates = null;
            runningScheduledUpdates = false;
        }
    }
    
    /* 
     * If part of the document was removed, the reparse-record tree needs to be updated.
     */
    @Override
    protected void fireRemoveUpdate(DocumentEvent e)
    {
        NodeAndPosition<ReparseRecord> napRr = (reparseRecordTree != null) ?
                reparseRecordTree.findNodeAtOrAfter(e.getOffset()) :
                    null;
        int rpos = e.getOffset();
        int rlen = e.getLength();
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
                    napRr = reparseRecordTree.findNodeAtOrAfter(e.getOffset());
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
        
        MoeSyntaxEvent mse = new MoeSyntaxEvent(this, e);
        if (parsedNode != null) {
            parsedNode.textRemoved(this, 0, e.getOffset(), e.getLength(), mse);
        }
        recordEvent(e);
        super.fireRemoveUpdate(mse);
    }
    
    /**
     * Notify that the whole document potentially needs repainting.
     */
    public void documentChanged()
    {
        repaintLines(0, getLength());
    }
    
    /**
     * Notify that a certain area of the document needs repainting.
     */
    public void repaintLines(int offset, int length)
    {
        fireChangedUpdate(new DefaultDocumentEvent(offset, length, EventType.CHANGE));
    }
}
