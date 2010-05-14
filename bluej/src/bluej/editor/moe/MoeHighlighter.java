/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010  Michael Kolling and John Rosenberg 
 
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
package bluej.editor.moe;

import java.awt.Color;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.JTextComponent;

import bluej.Config;

/**
 * The MoeHighlighter class provides the editor with the necessary 
 * HighlightPainters for highlighting founds and selects
 *
 * @author  Marion Zalk
 */
public class MoeHighlighter extends DefaultHighlighter {

    private static final Color borderColor = new Color(212, 172,45);

    protected HighlightPainter highlightPainter;
    protected HighlightPainter selectPainter;
    
    public MoeHighlighter(JTextComponent comp) 
    {
        super();
        highlightPainter=new MoeBorderHighlighterPainter(borderColor, Config.getHighlightColour(), Config.getHighlightColour2());
        selectPainter=new MoeBorderHighlighterPainter(borderColor, Config.getSelectionColour(), Config.getSelectionColour2());
        install(comp);
    }

}
