/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009, 2013, 2014  Michael Kolling and John Rosenberg 
 
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

import javax.swing.*;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.prefmgr.PrefMgr;

/**
 * A customised text area for use in the BlueJ Java text evaluation.
 *
 * @author  Michael Kolling
 */
public final class TextEvalArea extends JScrollPane
    implements KeyListener, FocusListener
{
    private static final Color TRANSPARANT = new Color(0f, 0f, 0f, 0.0f);
    private static final Color selectionColour = Config.getSelectionColour();

    private TextEvalPane text;
    
    private boolean frameEmpty;
    
    /**
     * Create a new text area with given size.
     */
    public TextEvalArea(PkgMgrFrame frame, Font font)
    {
        createComponent(frame, font);
        frameEmpty = frame.isEmptyFrame();
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
     * Clear all text in this text area, and remove
     * all local variables.
     */
    public void clear()
    {
        text.clear();
        text.clearVars();
    }
    
    /**
     * Reset the font size according to preferences.
     */
    public void resetFontSize()
    {
        int fontsize = getFontSize();
        if (fontsize != 0) {
            Font codepadFont = text.getFont();
            if (codepadFont.getSize() != fontsize) {
                text.setFont(codepadFont.deriveFont((float) fontsize));
            }
        }
    }
    
    /**
     * Get the font size according to preferences. Might return 0.
     */
    private int getFontSize()
    {
        int fontsize = Config.getPropInteger("bluej.codepad.fontsize", 0);
        if (fontsize == 0) {
            // If not set specifically for codepad, use the editor font size
            fontsize = PrefMgr.getEditorFontSize();
        }
        return fontsize;
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
     * Workaround for JDK 1.4 bug: backspace and tab keys are still handled 
     * internally even when replaced in the keymap. So we explicitly remove them 
     * here. This method (and the whole keylistener interface) can be removed
     * when we don't support 1.4 anymore. (Fixed in JDK 5.0.)
     */
    public void keyTyped(KeyEvent e)
    {
        char ch = e.getKeyChar();
        final char DEL = 127;
        if(ch == '\b' || ch == '\t' || ch == DEL) {
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
        int fontSize = getFontSize();
        if (fontSize != 0) {
            font = font.deriveFont((float) fontSize);
        }
        text.setFont(font);
        text.setSelectionColor(selectionColour);
        if (!Config.isRaspberryPi()) text.setOpaque(false);
        
        if (!Config.isRaspberryPi()) setBackground(TRANSPARANT);
        if (!Config.isRaspberryPi()) viewport.setBackground(TRANSPARANT);
        //To get fill working properly under Nimbus L&F, set background to transparent, too:
        if (!Config.isRaspberryPi()) text.setBackground(TRANSPARANT);

        setViewportView(text);
        updateBackground(frame.isEmptyFrame());

        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        setPreferredSize(new Dimension(300,100));
    }
    
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        
        if (g instanceof Graphics2D && false == frameEmpty) {
            Graphics2D g2d = (Graphics2D)g;
            
            int w = getWidth();
            int h = getHeight();
            
            Color color1 = new Color(235, 230, 200);
            Color color2 = new Color(209, 203, 179);
            if (!Config.isRaspberryPi()){
                GradientPaint gp = new GradientPaint(
                    w/4, 0, color1,
                    w, h, color2);
                g2d.setPaint(gp);
            }else{
                g2d.setPaint(new Color(222, 216, 189));
            }

            
            // We don't draw the outermost pixel, so that when the border is empty, it
            // shows the gradient from the window beneath (grey)
            // rather than our gradient (beige) outside the grey bevel border
            g2d.fillRect(1, 1, w-2, h-2);
        }
    }

    public void updateBackground(boolean frameEmpty)
    {
        this.frameEmpty = frameEmpty;
        
        getViewport().setOpaque(frameEmpty);
        if (!Config.isRaspberryPi()) setOpaque(frameEmpty);
        
    }

    public JComponent getFocusableComponent()
    {
        return text;
    }
}
