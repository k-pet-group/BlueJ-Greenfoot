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
package bluej.editor.moe;
 
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.Segment;

public class NaviviewView extends BlueJSyntaxView
{
    private static final boolean SCOPE_HIGHLIGHTING = true;
    private static final boolean HIGHLIGHT_METHODS_ONLY = true;
    private static final boolean SYNTAX_COLOURING = false;
    
    public NaviviewView(Element elem)
    {
        super(elem, 0);
    }
    
    @Override
    protected void paintTaggedLine(Segment line, int lineIndex, Graphics g,
            int x, int y, MoeSyntaxDocument document, Color def,
            Element lineElement)
    {
        if (SYNTAX_COLOURING) {
            super.paintTaggedLine(line, lineIndex, g, x, y, document, def, lineElement);
            super.paintTaggedLine(line, lineIndex, g, x+1, y, document, def, lineElement);
        }
        else {
            paintPlainLine(lineIndex, g, x, y);
            paintPlainLine(lineIndex, g, x+1, y);
        }
    }
    
    @Override
    public void paint(Graphics g, Shape a)
    {
        Rectangle bounds = a.getBounds();
        Rectangle clip = g.getClipBounds();
        
        if (SCOPE_HIGHLIGHTING) {
            // Scope highlighting
            int spos = viewToModel(bounds.x, clip.y, a, new Position.Bias[1]);
            int epos = viewToModel(bounds.x, clip.y + clip.height - 1, a, new Position.Bias[1]);

            Element map = getElement();
            int firstLine = map.getElementIndex(spos);
            int lastLine = map.getElementIndex(epos);
            paintScopeMarkers(g, (MoeSyntaxDocument) getDocument(), a,
                    firstLine, lastLine, HIGHLIGHT_METHODS_ONLY, true);
        }

        super.paint(g, a);
    }
}
