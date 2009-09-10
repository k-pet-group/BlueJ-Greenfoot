/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.debugmgr.texteval;

import java.awt.*;
import java.awt.event.*;
import javax.swing.text.*;

/**
 * A customised caret for Moe. It gets most of its bahaviour from
 * Swing's "DefaultCaret" and adds some functionality.
 *
 * @author  Michael Kolling
 */

public class TextEvalCaret extends DefaultCaret  
{
    /**
     * Constructs a Moe Caret
     */
    public TextEvalCaret()
    {
        super();
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

        if (e.getX() > TextEvalSyntaxView.TAG_WIDTH) {
            if (pos >= 0) {
                setDot(pos);
            }
        }
        else {
            ((TextEvalPane)getComponent()).tagAreaClick(pos, e.getClickCount());
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
        if (e.getX() > TextEvalSyntaxView.TAG_WIDTH) {
            Point pt = new Point(e.getX(), e.getY());
            Position.Bias[] biasRet = new Position.Bias[1];
            int pos = getComponent().getUI().viewToModel(getComponent(), pt, biasRet);
            if (pos >= 0) {
                moveDot(pos);
            }
        }
    }
}


