// Copyright (c) 2000 BlueJ Group, Monash University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@monash.edu.au

package bluej.editor.moe;

import java.awt.*;              // MenuBar, MenuItem, Menu, Button, etc.
import javax.swing.*;		// all the GUI components

/**
 * MoeJEditorPane - a variation of JEditorPane for Moe. The preferred size
 * is adjusted to allow for the tag line.
 *
 * @author Michael Kolling
 */

public class MoeEditorPane extends JEditorPane
{
    public Dimension getPreferredSize() {
	Dimension d = super.getPreferredSize();
        d.width += MoeEditor.TAG_WIDTH + 8;  // bit of empty space looks nice
        return d;
    }

}
