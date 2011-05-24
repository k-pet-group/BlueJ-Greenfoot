/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011  Michael Kolling and John Rosenberg 
 
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

import java.awt.Graphics;
import java.awt.Shape;

import javax.swing.text.JTextComponent;
import javax.swing.text.View;

/**
 * An "advanced" highlight painter, usable with {@link MoeHighlighter}
 * 
 * @author Davin McCall
 */
public interface AdvancedHighlightPainter
{
    /**
     * Paint the highlight
     * 
     * @param g   The graphics context to paint to
     * @param p0   The position where the highlight starts
     * @param p1   The position where the highlight ends
     * @param viewBounds  The view bounds of the root view within the component
     * @param editor  The component on which the highlight is being painted
     * @param view  The root view
     */
    public void paint(Graphics g, int p0, int p1, Shape viewBounds, JTextComponent editor, View view);
    
    /**
     * Issue a repaint for the area covered by the highlight.
     * 
     * @param p0  The position where the highlight starts
     * @param p1  The position where the highlight ends
     * @param viewBounds  The view bounds of the root view within the component
     * @param editor      The component to repaint
     * @param rootViwe  The root view
     */
    public void issueRepaint(int p0, int p1, Shape viewBounds, JTextComponent editor, View rootView);
}
