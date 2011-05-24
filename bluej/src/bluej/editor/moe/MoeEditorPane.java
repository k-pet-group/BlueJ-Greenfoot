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
package bluej.editor.moe;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JEditorPane;
import javax.swing.border.EmptyBorder;

/**
 * MoeJEditorPane - a variation of JEditorPane for Moe. The preferred size
 * is adjusted to allow for the tag line.
 *
 * @author Michael Kolling
 */
public final class MoeEditorPane extends JEditorPane
{
    /**
     * Create an editor pane specifically for Moe.
     */
    public MoeEditorPane()
    {
        super();
        setBorder(new EmptyBorder(6, 6, 6, 0));
        setHighlighter(new MoeHighlighter());
    }
    
    /*
     * Adjust this pane's preferred size to add the tag area.
     */
    public Dimension getPreferredSize() 
    {
        Dimension d = super.getPreferredSize();
        d.width += MoeSyntaxView.TAG_WIDTH + 8;  // bit of empty space looks nice
        return d;
    }

    /*
     * Make sure, when we are scrolling to follow the caret,
     * that we can see the tag area as well.
     */
    public void scrollRectToVisible(Rectangle rect)
    {
        super.scrollRectToVisible(new Rectangle(rect.x - (MoeSyntaxView.TAG_WIDTH + 4), rect.y,
                                                rect.width + MoeSyntaxView.TAG_WIDTH + 4, rect.height));
    }
}
