/*
* MoeSyntaxDocument.java - inherits from
* DefaultSyntaxDocument.java - Simple implementation of SyntaxDocument
* Copyright (C) 1999 Slava Pestov
* modified by Bruce Quig to add Syntax highlighting to the BlueJ
* programming environment.
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA.
*/
package bluej.editor.moe;

import javax.swing.text.*;

import java.awt.Color;
import bluej.Config;

import org.gjt.sp.jedit.syntax.*;

// For configuration file reading.
import java.util.Properties;

/**
 * A simple implementation of <code>SyntaxDocument</code> that 
 * inherits from DefaultSyntaxDocument. It takes
 * care of inserting and deleting lines from the token marker's state.
 * It adds the ability to handle paragraph attributes on a per line basis.
 *
 * @author Bruce Quig
 * @author Jo Wood (Modified to allow user-defined colours, March 2001)
 *
 */
public class MoeSyntaxDocument extends DefaultSyntaxDocument
{
    public static final String OUTPUT = "output";
    public static final String ERROR = "error";

	private static Color[] colors = null;
    
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
     * Allows user-defined colours to be set for synax highlighting. The file 
     * containing the colour values is should be 'lib/moe.defs'.
     * If this file is not found, or not all colours are defined, the BlueJ
     * default colours are used.
     * @author This method was added by Jo Wood (jwo@soi.city.ac.uk), 9th March, 2001.
     */
    private static Color[] getUserColors()
    { 
        if(colors == null) {
            	Properties editorProps = Config.moe_user_props;
            	
            	// Build colour table.	   
            	colors = new Color[Token.ID_COUNT];
            	
            	// Replace with user-defined colours.
            	String colorStr;
            	int    colorInt;
            	
            	// Comments.
            	colorStr = editorProps.getProperty("comment","1a1a80");
            	try    {
            		colorInt = Integer.parseInt(colorStr,16);
            	}
            	catch (NumberFormatException e)    {
            		colorInt = 0x1a1a80;
            	}
            	colors[Token.COMMENT1] = new Color(colorInt);    
            	
            	// Javadoc comments.
            	colorStr = editorProps.getProperty("javadoc","1a1a80");
            	try {
            		colorInt = Integer.parseInt(colorStr,16);
            	}
            	catch (NumberFormatException e)    {
            		colorInt = 0x1a1a80;
            	}
            	colors[Token.COMMENT2] = new Color(colorInt);
            	
            	// Stand-out comments (/*#).
            	colorStr = editorProps.getProperty("stand-out","ee00bb");
            	try {
            		colorInt = Integer.parseInt(colorStr,16);
            	}
            	catch (NumberFormatException e)    {
            		colorInt = 0xee00bb;
            	}
            	colors[Token.COMMENT3] = new Color(colorInt);
            	
            	// Java keywords.    
            	colorStr = editorProps.getProperty("keyword1","660033");
            	try    {
            		colorInt = Integer.parseInt(colorStr,16);
            	}
            	catch (NumberFormatException e) {
            		colorInt = 0x660033;
            	}
            	colors[Token.KEYWORD1] = new Color(colorInt);
            	
            	// Class-based keywords.
            	colorStr = editorProps.getProperty("keyword2","cc8033");
            	try    {
            		colorInt = Integer.parseInt(colorStr,16);
            	}
            	catch (NumberFormatException e)    {
            		colorInt = 0xcc8033;
            	}
            	colors[Token.KEYWORD2] = new Color(colorInt);
            	
            	// Other Java keywords (true, false, this, super).
            	colorStr = editorProps.getProperty("keyword3","006699");
            	try    {
            		colorInt = Integer.parseInt(colorStr,16);
            	}
            	catch (NumberFormatException e)    {
            		colorInt = 0x006699;
            	}
            	colors[Token.KEYWORD3] = new Color(colorInt);
            	
            	// Primitives.
            	colorStr = editorProps.getProperty("primitive","cc0000");
            	try    {
            		colorInt = Integer.parseInt(colorStr,16);
            	}
            	catch (NumberFormatException e)    {
            		colorInt = 0xcc0000;
            	}
            	colors[Token.PRIMITIVE] = new Color(colorInt);
            	
            	// String literals.
            	colorStr = editorProps.getProperty("string","339933");
            	try    {
            		colorInt = Integer.parseInt(colorStr,16);
            	}
            	catch (NumberFormatException e)    {
            		colorInt = 0x339933;
            	}
            	colors[Token.LITERAL1] = new Color(colorInt);
            	
            	// Leave remaining tokens as default.
            	colors[Token.LABEL]    = new Color(0x990000);
            	colors[Token.OPERATOR] = new Color(0xcc9900);
            	colors[Token.INVALID]  = new Color(0xff3300);
        }
        return colors;
    }
}
