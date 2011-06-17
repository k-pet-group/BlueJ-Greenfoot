/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011  Michael Kolling and John Rosenberg 
 
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
package bluej.terminal;

import bluej.Config;
import java.awt.Color;
import java.awt.Graphics;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainView;
import javax.swing.text.Segment;
import javax.swing.text.Utilities;

/**
 * A View implementation for the terminal. Styles lines representing recorded method
 * calls (and their results) differently to regular text.
 * 
 * @author Davin McCall
 */
public class TerminalView extends PlainView
{
    public static final String METHOD_RECORD = "method-record";
    private static final Color METHOD_RECORD_COLOR = Config.ENV_COLOUR;
    
    public TerminalView(Element el)
    {
        super(el);
    }
    
    @Override
    protected int drawUnselectedText(Graphics g, int x, int y, int p0, int p1)
        throws BadLocationException
    {        
        Document doc = getDocument();
        int elementIndex = doc.getDefaultRootElement().getElementIndex(p0);
        Element el = doc.getDefaultRootElement().getElement(elementIndex);
        
        AttributeSet attrs = el.getAttributes();
        if (attrs != null && attrs.getAttribute(METHOD_RECORD) != null) {
            g.setColor(METHOD_RECORD_COLOR);
            Segment s = new Segment();
            doc.getText(p0, p1 - p0, s);
            return Utilities.drawTabbedText(s, x, y, g, this, p0);
        }
        else {
            return super.drawUnselectedText(g, x, y, p0, p1);
        }
    }
}
