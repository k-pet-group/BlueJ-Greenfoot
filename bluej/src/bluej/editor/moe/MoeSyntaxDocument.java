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
// Copyright (c) 2000, 2005 BlueJ Group, Deakin University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@bluej.org


package bluej.editor.moe;

import javax.swing.text.*;

import java.awt.Color;
import bluej.Config;

import org.syntax.jedit.*;
import org.syntax.jedit.tokenmarker.*;


/**
 * A simple implementation of <code>SyntaxDocument</code> that 
 * inherits from SyntaxDocument. It takes
 * care of inserting and deleting lines from the token marker's state.
 * It adds the ability to handle paragraph attributes on a per line basis.
 *
 * @author Bruce Quig
 * @author Jo Wood (Modified to allow user-defined colours, March 2001)
 *
 */
public class MoeSyntaxDocument extends SyntaxDocument
{
    public static final String OUTPUT = "output";
    public static final String ERROR = "error";

	private static Color[] colors = null;
	
	private static Color defaultColour = null;
    private static Color backgroundColour = null;
	
    public MoeSyntaxDocument()
    {
        super(getUserColors());
         // defaults to 4 if cannot read property
         int tabSize = Config.getPropInteger("bluej.editor.tabsize", 4);
         putProperty(tabSizeAttribute, new Integer(tabSize));
    }

    /**
     * Sets attributes for a paragraph.  This method was added to 
     * provide the ability to replicate DefaultStyledDocument's ability to 
     * set each lines attributes easily.
     * This is an added method for the BlueJ adaption of jedit's Syntax
     * package   
     *
     * @param offset the offset into the paragraph >= 0
     * @param length the number of characters affected >= 0
     * @param s the attributes
     * @param replace whether to replace existing attributes, or merge them
     */
    public void setParagraphAttributes(int offset, AttributeSet s)
    {
        // modified version of method from DefaultStyleDocument
        try {
            writeLock();
            
            Element paragraph = getParagraphElement(offset);
            MutableAttributeSet attr = 
                    (MutableAttributeSet) paragraph.getAttributes();
            attr.addAttributes(s);
        } finally {
            writeUnlock();
        }
    }
    
    /**
     * Get the default colour for MoeSyntaxDocuments.
     */
    public static Color getDefaultColor()
    {
        return defaultColour;
    }
    
    /**
     * Get the background colour for MoeSyntaxDocuments.
     */
    public static Color getBackgroundColor()
    {
        return backgroundColour;
    }
    
    /**
     * Allows user-defined colours to be set for synax highlighting. The file
     * containing the colour values is 'lib/moe.defs'. If this file is
     * not found, or not all colours are defined, the BlueJ default colours are
     * used.
     * 
     * @author This method was added by Jo Wood (jwo@soi.city.ac.uk), 9th March,
     *         2001.
     */
    private static Color[] getUserColors()
    { 
        if(colors == null) {
            // Replace with user-defined colours.
            int    colorInt;
                        
            // First determine default colour and background colour
            colorInt = getPropHexInt("other", 0x000000);
            defaultColour = new Color(colorInt);
            
            colorInt = getPropHexInt("background", 0x000000);
            backgroundColour = new Color(colorInt);

            // Build colour table.	   
            colors = new Color[Token.ID_COUNT];

            // Comments.
            colorInt = getPropHexInt("comment", 0x1a1a80);
            colors[Token.COMMENT1] = new Color(colorInt);    

            // Javadoc comments.
            colorInt = getPropHexInt("javadoc", 0x1a1a80);
            colors[Token.COMMENT2] = new Color(colorInt);

            // Stand-out comments (/*#).
            colorInt = getPropHexInt("stand-out", 0xee00bb);
            colors[Token.COMMENT3] = new Color(colorInt);

            // Java keywords.
            colorInt = getPropHexInt("keyword1", 0x660033);
            colors[Token.KEYWORD1] = new Color(colorInt);

            // Class-based keywords.
            colorInt = getPropHexInt("keyword2", 0xcc8033);
            colors[Token.KEYWORD2] = new Color(colorInt);

            // Other Java keywords (true, false, this, super).
            colorInt = getPropHexInt("keyword3", 0x006699);
            colors[Token.KEYWORD3] = new Color(colorInt);

            // Primitives.
            colorInt = getPropHexInt("primitive", 0xcc0000);
            colors[Token.PRIMITIVE] = new Color(colorInt);

            // String literals.
            colorInt = getPropHexInt("string", 0x339933);
            colors[Token.LITERAL1] = new Color(colorInt);

            // Labels
            colorInt = getPropHexInt("label", 0x999999);
            colors[Token.LABEL] = new Color(colorInt);
            
            // Invalid (eg unclosed string literal)
            colorInt = getPropHexInt("invalid", 0xff3300);
            colors[Token.INVALID] = new Color(colorInt);
            
            // Operator is not produced by token marker
            colors[Token.OPERATOR] = new Color(0xcc9900);
        }
        return colors;
    }
    
    /**
     * Get an integer value from a property whose value is hex-encoded.
     * @param propName  The name of the property
     * @param def       The default value if the property is undefined or
     *                  not parseable as a hexadecimal
     * @return  The value
     */
    private static int getPropHexInt(String propName, int def)
    {
        String strVal = Config.getPropString(propName, null, Config.moeUserProps);
        try {
            return Integer.parseInt(strVal, 16);
        }
        catch (NumberFormatException nfe) {
            return def;
        }
    }
}
