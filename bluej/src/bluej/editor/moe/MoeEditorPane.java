// Copyright (c) 2000 BlueJ Group, Monash University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@mip.sdu.dk

package bluej.editor.moe;

import java.awt.*;
import javax.swing.*;

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
    }
    
    /**
     * Adjust this pane's preferred size to add the tag area.
     */
    public Dimension getPreferredSize() 
    {
        Dimension d = super.getPreferredSize();
        d.width += BlueJSyntaxView.TAG_WIDTH + 8;  // bit of empty space looks nice
        return d;
    }

    /**
     * Make sure, when we are scrolling to follow the caret,
     * that we can see the tag area as well.
     */
    public void scrollRectToVisible(Rectangle rect)
    {
        super.scrollRectToVisible(new Rectangle(rect.x - (BlueJSyntaxView.TAG_WIDTH + 4), rect.y,
                                                rect.width + BlueJSyntaxView.TAG_WIDTH + 4, rect.height));
    }
}
