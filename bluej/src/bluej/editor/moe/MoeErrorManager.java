/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011,2013,2014,2015,2016  Michael Kolling and John Rosenberg

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
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JEditorPane;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;

import bluej.editor.moe.BlueJSyntaxView.ParagraphAttribute;
import javafx.application.Platform;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.parser.SourceLocation;

/**
 * Manages the display of parse and compiler errors for a MoeEditor instance.
 * 
 * @author Davin McCall
 */
public class MoeErrorManager
{
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

    /**
     * Add a compiler error highlight.
     * @param startPos  The document position where the error highlight should begin
     * @param endPos    The document position where the error highlight should end
     */
    public void addErrorHighlight(int startPos, int endPos, String message, int identifier)
    {
        if (endPos < startPos)
            throw new IllegalArgumentException("Error ends before it begins: " + startPos + " to " + endPos);
        
        MoeEditorPane sourcePane = editor.getSourcePane();
        sourcePane.setStyleSpans(startPos, sourcePane.getStyleSpans(startPos, endPos).mapStyles(s -> {
            s = new ArrayList<>(s);
            s.add(MoeEditorPane.ERROR_CLASS);
            return s;
        }));
        editor.getSourceDocument().setParagraphAttributes(startPos, Collections.singletonMap(ParagraphAttribute.ERROR, true));
        errorInfos.add(new ErrorDetails(startPos, endPos, message, identifier));
        setNextErrorEnabled.accept(true);
        editor.updateHeaderHasErrors(true);
    }
    
    /**
     * Remove any existing compiler error highlight.
     */
    public void removeAllErrorHighlights()
    {
        MoeEditorPane sourcePane = editor.getSourcePane();
        for (ErrorDetails err : errorInfos)
        {
            sourcePane.setStyleSpans(err.startPos, sourcePane.getStyleSpans(err. startPos, err.endPos).mapStyles(s -> {
                s = new ArrayList<>(s);
                s.remove(MoeEditorPane.ERROR_CLASS);
                return s;
            }));
            editor.getSourceDocument().setParagraphAttributes(err.startPos, Collections.singletonMap(ParagraphAttribute.ERROR, false));
        }
        errorInfos.clear();
        setNextErrorEnabled.accept(false);
        editor.updateHeaderHasErrors(false);
    }
    
    // Returns null if there is no next error.
    public ErrorDetails getNextErrorPos(int from)
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
        return next;
    }
    
    /**
     * Notify the error manager of a change to the document.
     */
    public void documentContentChanged()
    {
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
    
    /**
     * Returns null if no error on that line
     */
    @OnThread(Tag.FXPlatform)
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

    public boolean hasErrorHighlights()
    {
        return !errorInfos.isEmpty();
    }

    public static class ErrorDetails
    {
        public final int startPos;
        public final int endPos;
        public final String message;
        public final int identifier;
        private ErrorDetails(int startPos, int endPos, String message, int identifier)
        {
            this.startPos = startPos;
            this.endPos = endPos;
            this.message = message;
            this.identifier = identifier;
        }
        
        public boolean containsPosition(int pos)
        {
            return startPos <= pos && pos <= endPos;
        }
    }

}
