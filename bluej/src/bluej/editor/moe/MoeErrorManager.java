/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011,2013,2014,2015  Michael Kolling and John Rosenberg 

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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JEditorPane;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.parser.SourceLocation;

/**
 * Manages the display of parse and compiler errors for a MoeEditor instance.
 * 
 * @author Davin McCall
 */
public class MoeErrorManager implements MoeDocumentListener
{
    /** Parse error delay in milliseconds */
    //private static final int ERR_DISPLAY_DELAY = 1000;
    
    // Error highlight colors
    private static final Color ERROR_HIGHLIGHT_GRADIENT1 = new Color(240,128,128);
    private static final Color ERROR_HIGHLIGHT_GRADIENT2 = new Color(240,190,190);
    private static final Color ERROR_HIGHLIGHT_SELECTED1 = ERROR_HIGHLIGHT_GRADIENT1;
    private static final Color ERROR_HIGHLIGHT_SELECTED2 = ERROR_HIGHLIGHT_GRADIENT2;
    private final List<ErrorDetails> errorInfos = new ArrayList<>();
    private MoeEditor editor;
    private Consumer<Boolean> setNextErrorEnabled;
    /**
     * Construct a new MoeErrorManager to manage error display for the specified editor instance.
     * The new manager should be set as the document listener so that it receives notification
     * of parser errors as they occur.
     */
    public MoeErrorManager(MoeEditor editor, Consumer<Boolean> setNextErrorEnabled)
    {
        this.editor = editor;
        this.setNextErrorEnabled = setNextErrorEnabled;
    }
    
    /** A timer used to delay the appearance of parse errors until the user is idle */
    //private Timer timer;
    
    /** Parse errors that are currently being displayed */
//    private NodeTree<ParseErrorNode> parseErrors = new NodeTree<ParseErrorNode>();
    /** Parse errors that haven't yet been displayed */
//    private NodeTree<ParseErrorNode> pendingErrors = new NodeTree<ParseErrorNode>();
    
    /**
     * Add a compiler error highlight.
     * @param startPos  The document position where the error highlight should begin
     * @param endPos    The document position where the error highlight should end
     */
    public void addErrorHighlight(int startPos, int endPos, String message)
    {
        if (endPos < startPos)
            throw new IllegalArgumentException("Error ends before it begins: " + startPos + " to " + endPos);
        
        JEditorPane sourcePane = editor.getSourcePane();
        try {
            MoeHighlighter highlighter = (MoeHighlighter) sourcePane.getHighlighter();
            AdvancedHighlightPainter painter = new MoeSquigglyUnderlineHighlighterPainter(Color.RED, offs -> editor.getLineColumnFromOffset(offs).getLine());
            Object errorHighlightTag = highlighter.addHighlight(startPos, endPos, painter);
            errorInfos.add(new ErrorDetails(errorHighlightTag, startPos, endPos, message));
            setNextErrorEnabled.accept(true);
        }
        catch (BadLocationException ble) {
            throw new RuntimeException(ble);
        }
    }
    
    /**
     * Remove any existing compiler error highlight.
     */
    public void removeAllErrorHighlights()
    {
        JEditorPane sourcePane = editor.getSourcePane();
        for (ErrorDetails err : errorInfos)
        {
            sourcePane.getHighlighter().removeHighlight(err.highlightTag);
        }
        errorInfos.clear();
        setNextErrorEnabled.accept(false);
    }
    
    public int getNextErrorPos(int from)
    {
        int lowestDist = Integer.MIN_VALUE; // Negative means before the given position
        ErrorDetails next = null;
        
        for (ErrorDetails err : errorInfos)
        {
            // If error is before the given position, it will be a negative distance
            // If error is ahead, it will be a positive distance
            // If we are within the error, the position will also show up negative,
            // which means we will treat it as low priority, and advance to next error instead
            final int dist = err.startPos - from;
            
            if (next == null
                    // If the current best is before the position, ours is better if either
                    // it's after the position, or it's even further before
                    || (lowestDist <= 0 && (dist > 0 || dist <= lowestDist))
                    // If the current best is after the position, ours is better only if
                    // we are earlier
                    || (lowestDist > 0 && dist > 0 && dist <= lowestDist))
            {
                next = err;
                lowestDist = dist;
            }
        }
        if (next == null)
            return -1;
        else
            return next.startPos;
    }
    
    /**
     * Notify the error manager of an insert update to the document.
     */
    public void insertUpdate(DocumentEvent e)
    {
//        NodeAndPosition<ParseErrorNode> nap = parseErrors.findNodeAtOrAfter(e.getOffset());
//        if (nap != null) {
//            nap.slide(e.getLength());
//        }
//        
//        nap = pendingErrors.findNodeAtOrAfter(e.getOffset());
//        if (nap != null) {
//            nap.slide(e.getLength());
//        }
//        
//        if (timer != null) {
//            timer.restart();
//        }
        
        setNextErrorEnabled.accept(false);
    }
    
    /**
     * Notify the error manager of a remove update to the document.
     */
    public void removeUpdate(DocumentEvent e)
    {
//        NodeAndPosition<ParseErrorNode> nap = parseErrors.findNodeAtOrAfter(e.getOffset() + e.getLength());
//        if (nap != null) {
//            if (nap.getPosition() >= e.getOffset() + e.getLength()) {
//                nap.slide(-e.getLength());
//            }
//        }
//        
//        nap = pendingErrors.findNodeAtOrAfter(e.getOffset() + e.getLength());
//        if (nap != null) {
//            if (nap.getPosition() >= e.getOffset() + e.getLength()) {
//                nap.slide(-e.getLength());
//            }
//        }
//
//        if (timer != null) {
//            timer.restart();
//        }

        setNextErrorEnabled.accept(false);
    }
    
    /**
     * Get the error code (or message) at a particular document position.
     */
    public ErrorDetails getErrorAtPosition(int pos)
    {
        return errorInfos.stream()
                .filter(e -> e.containsPosition(pos))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    @OnThread(Tag.Any)
    public void parseError(int position, int size, String message)
    {
        // Don't add this error if it overlaps an existing error:
//        NodeAndPosition<ParseErrorNode> nap = parseErrors.findNodeAtOrAfter(position);
//        while (nap != null && nap.getEnd() == position && nap.getPosition() != position) {
//            nap = nap.nextSibling();
//        }
//        if (nap != null) {
//            if (nap.getEnd() <= position + size) {
//                return;
//            }
//        }
//
//        nap = pendingErrors.findNodeAtOrAfter(position);
//        while (nap != null && nap.getEnd() == position && nap.getPosition() != position) {
//            nap = nap.nextSibling();
//        }
//        if (nap != null) {
//            if (nap.getEnd() <= position + size) {
//                return;
//            }
//        }
//
//        pendingErrors.insertNode(new ParseErrorNode(null, message), position, size);
//
//        if (timer == null) {
//            timer = new Timer(ERR_DISPLAY_DELAY, new ActionListener() {
//                @Override
//                public void actionPerformed(ActionEvent e)
//                {
//                    timerExpiry();
//                }
//            });
//
//            timer.setCoalesce(true);
//            timer.setRepeats(false);
//            timer.start();
//        }
    }    
    
    @Override
    @OnThread(Tag.Any)
    public void reparsingRange(int position, int size)
    {
//        // Remove any parse error highlights in the reparsed range
//        int endPos = position + size;
//        
//        clearReparsedRange(parseErrors, position, endPos);
//        clearReparsedRange(pendingErrors, position, endPos);
    }
    
    /**
     * Returns null if no error on that line
     */
    public ErrorDetails getErrorOnLine(int lineIndex)
    {
        final int lineStart = editor.getOffsetFromLineColumn(new SourceLocation(lineIndex + 1, 1));
        if (lineIndex + 1 >= editor.numberOfLines())
        {
            return errorInfos.stream().filter(e -> e.endPos >= lineStart).findFirst().orElse(null);
        }
        else
        {
            int lineEnd = editor.getOffsetFromLineColumn(new SourceLocation(lineIndex + 2, 1));
            return errorInfos.stream().filter(e -> e.startPos <= lineEnd && e.endPos >= lineStart).findFirst().orElse(null);
        }
    }
    
    public static class ErrorDetails
    {
        public final int startPos;
        public final int endPos;
        public final String message;
        private final Object highlightTag;
        private ErrorDetails(Object highlightTag, int startPos, int endPos, String message)
        {
            this.highlightTag = highlightTag;
            this.startPos = startPos;
            this.endPos = endPos;
            this.message = message;
        }
        
        public boolean containsPosition(int pos)
        {
            return startPos <= pos && pos <= endPos;
        }
    }
    
//    private void clearReparsedRange(NodeTree<ParseErrorNode> tree, int position, int endPos)
//    {
//        NodeAndPosition<ParseErrorNode> nap = tree.findNodeAtOrAfter(position);
//        while (nap != null && nap.getPosition() <= endPos) {
//            ParseErrorNode pen = nap.getNode();
//            JEditorPane sourcePane = editor.getSourcePane();
//            Object highlightTag = pen.getHighlightTag();
//            if (highlightTag != null) {
//                sourcePane.getHighlighter().removeHighlight(highlightTag);
//            }
//            
//            NodeAndPosition<ParseErrorNode> nnap = nap.nextSibling();
//            pen.remove();
//            nap = nnap;
//        }
//    }
    
    /**
     * The timer expired... make pending errors visible
     */
//    private void timerExpiry()
//    {
//        NodeAndPosition<ParseErrorNode> nap = pendingErrors.findNodeAtOrAfter(0);
//        while (nap != null) {
//            ParseErrorNode pen = nap.getNode();
//            
//            int position = nap.getPosition();
//            int size = nap.getSize();
//
//            JEditorPane sourcePane = editor.getSourcePane();
//            int caretPos = sourcePane.getCaretPosition();
//            
//            try {
//                MoeHighlighter mhiliter = (MoeHighlighter) sourcePane.getHighlighter();
//                Object highlightTag = mhiliter.addHighlight(
//                        position, position + size,
//                        new MoeBorderHighlighterPainter(Color.RED, Color.RED, Color.PINK,
//                                Color.RED, Color.PINK)
//                );
//
//                parseErrors.insertNode(new ParseErrorNode(highlightTag, pen.getErrCode()), position, size);
//                
//                // Check if the error overlaps the caret currently
//                if (caretPos >= position && caretPos <= position + size) {
//                    if (! editor.isShowingInterface()) {
//                        editor.writeMessage(ParserMessageHandler.getMessageForCode(pen.getErrCode()));
//                    }
//                }
//            }
//            catch (BadLocationException ble) {
//                throw new RuntimeException(ble);
//            }
//            
//            nap = nap.nextSibling();
//        }
//        
//        pendingErrors.clear();
//        timer = null;
//    }
    
    
}
