/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2016  Poul Henriksen and Michael Kolling
 
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
package greenfoot;

/**
 * A representation of a Font. The Font can be used to write text on the screen.
 *
 * @author Fabio Heday
 */ 
public class Font
{

    private final java.awt.Font font;

    /**
     * Creates a Greenfoot font based on a java.awt.Font
     * @param font 
     */
    protected Font(java.awt.Font font)
    {
        this.font = font;
    }

    /**
     * Creates a font from  the specified name and size, bold, italic or regular.
     * @param name The font name
     * @param bold True if the font is meant to be bold
     * @param italic True if the font si meant to be italic
     * @param size The size of the font
     */
    public Font(String name, boolean bold, boolean italic, int size)
    {
        int style = java.awt.Font.PLAIN;
        if (bold) {
            style = java.awt.Font.BOLD;
        }
        if (italic) {
            style = style | java.awt.Font.ITALIC;
        }
        this.font = new java.awt.Font(name, style, size);
    }

    /**
     * Creates a font of a given size.
     * @param name The font name
     * @param size  The size of the font
     */
    public Font(String name, int size)
    {
        this.font = new java.awt.Font(name, java.awt.Font.PLAIN, size);
    }

    /**
     * Indicates whether or not this Font style is plain.
     * @return true if this font style is plain; false otherwise
     */
    public boolean isPlain()
    {
        return this.font.isPlain();
    }

    /**
     * Indicates whether or not this Font style is bold.
     * @return true if this font style is bold; false otherwise
     */
    public boolean isBold()
    {
        return this.font.isBold();
    }

    /**
     * Indicates whether or not this Font style is italic.
     * @return true if this font style is italic; false otherwise
     */
    public boolean isItalic()
    {
        return this.font.isItalic();
    }

    /**
     * Return the internal Font object representing the Greenfoot.Font.
     * @return the java.awt.Font object
     */
    protected java.awt.Font getFontObject()
    {
        return this.font;
    }

}
