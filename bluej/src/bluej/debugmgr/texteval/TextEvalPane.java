package bluej.debugmgr.texteval;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JEditorPane;

import bluej.editor.moe.BlueJSyntaxView;

/**
 * A modified editor pane for the text evaluation area.
 * The standard JEditorPane is adjusted to take the tag line to the left into
 * account in size computations.
 * 
 * @author Michael Kolling
 * @version $Id: TextEvalPane.java 2760 2004-07-08 09:39:51Z mik $
 */
public class TextEvalPane extends JEditorPane {

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
        rect.x -= BlueJSyntaxView.TAG_WIDTH + 4;
        rect.width += BlueJSyntaxView.TAG_WIDTH + 4;
        super.scrollRectToVisible(rect);
    }
}
