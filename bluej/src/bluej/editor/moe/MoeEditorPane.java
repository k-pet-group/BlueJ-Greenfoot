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
        d.width += MoeEditor.TAG_WIDTH + 10;  // bit of empty space looks nice
        return d;
    }

}
