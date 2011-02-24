/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011  Michael Kolling and John Rosenberg 
 
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
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.Position;

import bluej.utility.Debug;


/**
 * A customised caret for Moe. It gets most of its behaviour from
 * Swing's "DefaultCaret" and adds some functionality.
 *
 * @author  Michael Kolling
 */
public class MoeCaret extends DefaultCaret  
{
    private static final Color bracketHighlightColour = new Color(230, 200, 200);
    
    private static final LayeredHighlighter.LayerPainter bracketPainter = 
        new BracketMatchPainter(bracketHighlightColour);
        
    private MoeEditor editor;

    private boolean persistentHighlight = false;
    
    // matching bracket highlight holder
    private Object matchingBracketHighlight;

    /**
     * Constructs a Moe Caret
     */
    public MoeCaret(MoeEditor editor) 
    {
        super();
        this.editor = editor;
        setBlinkRate(0);
    }

    /**
     * Redefinition of caret positioning (after mouse click). Here, we
     * first check whether the click was in the tag line. If it was, we
     * toggle the breakpoint, if not we just position the caret as usual.
     */
    protected void positionCaret(MouseEvent e) 
    {
        Point pt = new Point(e.getX(), e.getY());
        Position.Bias[] biasRet = new Position.Bias[1];
        int pos = getComponent().getUI().viewToModel(getComponent(), pt, biasRet);

        if (e.getX() > MoeSyntaxView.TAG_WIDTH) {
            super.positionCaret(e);
        }
        else {
            editor.toggleBreakpoint(pos);
        }
    }

    /**
     * Tries to move the position of the caret from
     * the coordinates of a mouse event, using viewToModel(). 
     * This will cause a selection if the dot and mark
     * are different.
     *
     * @param e the mouse event
     */
    protected void moveCaret(MouseEvent e) 
    {
        if (e.getX() > MoeSyntaxView.TAG_WIDTH) {
            super.moveCaret(e);
        }
    }
    
    /**
     * Set the dot and mark position
     */
    public void setDot(int pos)
    {
        persistentHighlight = false;
        super.setDot(pos);
    }
    
    /**
     * Set the dot position (leave the mark where it is).
     */
    public void moveDot(int pos)
    {
        persistentHighlight = false;
        super.moveDot(pos);
    }

    @Override
    protected void fireStateChanged()
    {
        // Note this is called when the caret is moved.
        super.fireStateChanged();
        editor.caretMoved();
    }
    
    /**
     * Target text component lost focus.
     */
    public void focusLost(FocusEvent e)
    {
        super.focusLost(e);
    }
    
    /**
     * Set the highlight (of the selection) as persistent - that is, it won't
     * become invisible if the component loses focus. This lasts until the
     * caret position is changed.
     */
    public void setPersistentHighlight()
    {
        setSelectionVisible(true);
        persistentHighlight = true;
    }
     
    /**
     * Paint matching bracket if caret is directly after a bracket.  
     */
    public void paintMatchingBracket()
    {
        int matchBracket = editor.getBracketMatch();
        // remove existing bracket if needed
        removeBracket();
        if(matchBracket != -1) {
            try {
                matchingBracketHighlight = getComponent().getHighlighter().addHighlight(matchBracket, matchBracket + 1, bracketPainter);
            }
            catch(BadLocationException ble) {
                Debug.reportError("bad location exception thrown");
                ble.printStackTrace();
            }
        }      
    }
    
    /**
     * remove the existing matching bracket if it exists
     */
    public void removeBracket()
    {
        if(matchingBracketHighlight != null) {
            getComponent().getHighlighter().removeHighlight(matchingBracketHighlight);
            matchingBracketHighlight = null;        
        }  
    }
    
    @Override
    public void setSelectionVisible(boolean vis)
    {
        if (vis || ! persistentHighlight) {
            super.setSelectionVisible(vis);
        }
    }
}
