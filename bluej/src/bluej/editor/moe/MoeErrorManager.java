/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011,2013  Michael Kolling and John Rosenberg 

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

import javax.swing.JEditorPane;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;

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
    
    private MoeEditor editor;
    
    private Object errorHighlightTag = null;
    
    /** A timer used to delay the appearance of parse errors until the user is idle */
    //private Timer timer;
    
    /** Parse errors that are currently being displayed */
//    private NodeTree<ParseErrorNode> parseErrors = new NodeTree<ParseErrorNode>();
    /** Parse errors that haven't yet been displayed */
//    private NodeTree<ParseErrorNode> pendingErrors = new NodeTree<ParseErrorNode>();
    
    /**
     * Construct a new MoeErrorManager to manage error display for the specified editor instance.
     * The new manager should be set as the document listener so that it receives notification
     * of parser errors as they occur.
     */
    public MoeErrorManager(MoeEditor editor)
    {
        this.editor = editor;
    }
    
    /**
     * Add a compiler error highlight.
     * @param startPos  The document position where the error highlight should begin
     * @param endPos    The document position where the error highlight should end
     */
    public void addErrorHighlight(int startPos, int endPos)
    {
        JEditorPane sourcePane = editor.getSourcePane();
        try {
            MoeHighlighter highlighter = (MoeHighlighter) sourcePane.getHighlighter();
            AdvancedHighlightPainter painter = new MoeBorderHighlighterPainter(Color.RED,
                    ERROR_HIGHLIGHT_GRADIENT1, ERROR_HIGHLIGHT_GRADIENT2,
                    ERROR_HIGHLIGHT_SELECTED1, ERROR_HIGHLIGHT_SELECTED2);
            errorHighlightTag = highlighter.addHighlight(startPos, endPos, painter);
        }
        catch (BadLocationException ble) {
            throw new RuntimeException(ble);
        }
    }
    
    /**
     * Remove any existing compiler error highlight.
     */
    public void removeErrorHighlight()
    {
        if (errorHighlightTag != null) {
            JEditorPane sourcePane = editor.getSourcePane();
            sourcePane.getHighlighter().removeHighlight(errorHighlightTag);
            errorHighlightTag = null;
        }
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
    }
    
    /**
     * Get the error code (or message) at a particular document position.
     */
    public String getErrorAtPosition(int pos)
    {
//        NodeAndPosition<ParseErrorNode> nap = parseErrors.findNode(pos);
//        if (nap != null) {
//            return nap.getNode().getErrCode();
//        }
        return null;
    }
    
    @Override
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
    public void reparsingRange(int position, int size)
    {
//        // Remove any parse error highlights in the reparsed range
//        int endPos = position + size;
//        
//        clearReparsedRange(parseErrors, position, endPos);
//        clearReparsedRange(pendingErrors, position, endPos);
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
