/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014  Michael Kolling and John Rosenberg 

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
package bluej.utility;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A multi-line Label-like AWT component.
 * 
 * @author Justin Tan
 */
@OnThread(Tag.Swing)
public class MultiLineLabel extends JPanel
{
    protected int fontAttributes = Font.PLAIN;
    protected float alignment;
    protected Color col = null;
    protected int spacing = 0;

    /**
     ** Constructor - make a multiline label
     **/
    public MultiLineLabel(String text, float alignment)
    {
        this.alignment = alignment;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        if(text != null) {
            setText(text);
        }
    }

    /**
     ** Constructor, defaults to centered text
     **/
    public MultiLineLabel(String text)
    {
        this(text, LEFT_ALIGNMENT);
    }

    /**
     * Constructor, empty with the given alignment
     */
    public MultiLineLabel(float alignment)
    {
        this(null, alignment);
    }

    /**
     * Constructor, empty with the given alignment and line spacing
     */
    public MultiLineLabel(float alignment, int spacing)
    {
        this(null, alignment);
        this.spacing = spacing;
    }

    /**
     ** Constructor - make an empty multiline label
     **/
    public MultiLineLabel()
    {
        this(null, LEFT_ALIGNMENT);
    }

    public void setText(String text)
    {
        // clear the existing lines from the panel
        removeAll();
        addText(text);
    }

    public void addText(String text)
    {
        addText(text, 12);
    }

    public void addText(String text, int size)
    {
        if(spacing > 0) {
            add(Box.createVerticalStrut(spacing));
        }

        String strs[] = Utility.splitLines(text);
        JLabel l;
        Font font = new Font("SansSerif", fontAttributes, size);

        for (int i = 0; strs != null && i < strs.length; i++) {
            l = new JLabel(strs[i]);
            l.setFont(font);
            l.setAlignmentX(alignment);

            if (col != null) {
                l.setForeground(col);
            }

            add(l);
        }   
    }

    public void addText(String text, boolean bold, boolean italic)
    {
        int oldAttributes = fontAttributes;
        setBold(bold);
        setItalic(italic);
        addText(text);
        fontAttributes = oldAttributes;
    }

    @Override
    public void setForeground(Color col)
    {
        this.col = col;    
        Component[] components = this.getComponents();
        for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            component.setForeground(col);
        }
    }

    public void setItalic(boolean italic)
    {
        if(italic) {
            fontAttributes |= Font.ITALIC;
        }
        else {
            fontAttributes &= ~Font.ITALIC;
        }
    }

    public void setBold(boolean bold)
    {
        if(bold) {
            fontAttributes |= Font.BOLD;
        }
        else {
            fontAttributes &= ~Font.BOLD;
        }
    }
}
