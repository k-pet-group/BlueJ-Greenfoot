package bluej.debugmgr.texteval;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * A customised text area for use in the BlueJ Java text evaluation.
 *
 * @author  Michael Kolling
 * @version $Id: TextEvalArea.java 2885 2004-08-17 10:37:37Z mik $
 */
public final class TextEvalArea extends JScrollPane
    implements KeyListener, FocusListener
{
    private static final Color selectionColour = Config.getSelectionColour();

    private TextEvalPane text;
    
    /**
     * Create a new text area with given size.
     */
    public TextEvalArea(PkgMgrFrame frame, Font font)
    {
        createComponent(frame, font);
    }

    /**
     * Request to get the keyboard focus into the text evaluation area.
     */
    public void requestFocus()
    {
        text.requestFocus();
    }

    /**
     * Sets whether or not this component is enabled.  
     */
    public void setEnabled(boolean enabled)
    {
        text.setEnabled(enabled);
    }

    /**
     * Clear all text in this text area.
     */
    public void clear()
    {
        text.clear();
    }

    // --- FocusListener interface ---
    
    /**
     * Note that the object bench got keyboard focus.
     */
    public void focusGained(FocusEvent e) 
    {
        if (!e.isTemporary()) {
            setBorder(Config.focusBorder);        
            repaint();
        }
    }

    
    /**
     * Note that the object bench lost keyboard focus.
     */
    public void focusLost(FocusEvent e) 
    {
        setBorder(Config.normalBorder);
        repaint();
    }

    // --- end of FocusListener interface ---


    //   --- KeyListener interface ---

    /**
     * Workaround for JDK 1.4 bug: backspace keys are still handled internally
     * even when replaced in the keymap. So we explicitly remove them here.
     * This method (and the whole keylistener interface) can be removed
     * when we don't support 1.4 anymore. (Fixed in JDK 5.0.)
     */
    public void keyTyped(KeyEvent e) {
        if(e.getKeyChar() == '\b') {
            e.consume();
        }
    }  

    public void keyPressed(KeyEvent e) {}  
    public void keyReleased(KeyEvent e) {}  

    //   --- end of KeyListener interface ---

    /**
     * Create the Swing component representing the text area.
     */
    private void createComponent(PkgMgrFrame frame, Font font)
    {
        text = new TextEvalPane(frame);
        text.setMargin(new Insets(2,2,2,2));

        text.addKeyListener(this);
        text.addFocusListener(this);
        text.setFont(font);
        text.setSelectionColor(selectionColour);

        setViewportView(text);

        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
        setPreferredSize(new Dimension(300,100));
    }
}
