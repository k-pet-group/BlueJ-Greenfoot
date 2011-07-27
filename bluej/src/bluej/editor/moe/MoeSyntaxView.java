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
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;

import bluej.Config;
import bluej.prefmgr.PrefMgr;

/**
 * MoeSyntaxView is the main view for the Moe editor. It paints all aspects of the
 * document, performing syntax and scope colouring.
 *
 * @author Bruce Quig
 * @author Michael Kolling
 * @author Davin McCall
 */
public class MoeSyntaxView extends BlueJSyntaxView
{
    /**  width of tag area for setting breakpoints */
    public static final short TAG_WIDTH = 14;
    protected static final int BREAKPOINT_OFFSET = TAG_WIDTH + 2;
    protected static final int LEFT_MARGIN = BREAKPOINT_OFFSET + 8;
    
    private static boolean syntaxHighlighting = PrefMgr.getFlag(PrefMgr.HILIGHTING);
    
    protected Font lineNumberFont;
    protected Font smallLineNumberFont;
    protected FontMetrics lineNumberMetrics;
    
    // Attributes for lines and document
    public static final String BREAKPOINT = "break";
    public static final String STEPMARK = "step";
    
    static final Image breakImage =
        Config.getImageAsIcon("image.editor.breakmark").getImage();
    static final Image stepImage =
        Config.getImageAsIcon("image.editor.stepmark").getImage();
    static final Image breakStepImage =
        Config.getImageAsIcon("image.editor.breakstepmark").getImage();

    /**
     * Creates a new MoeSyntaxView for painting the specified element.
     * @param elem The element
     */
    public MoeSyntaxView(Element elem)
    {
        super(elem, LEFT_MARGIN);
    }

    /**
     * Reset the syntax highlighting status (on/off) according to preferences.
     */
    public static void resetSyntaxHighlighting()
    {
        syntaxHighlighting = PrefMgr.getFlag(PrefMgr.HILIGHTING);
    }
    
    @Override
    protected void initialise(Graphics g)
    {
        super.initialise(g);
        lineNumberFont = defaultFont.deriveFont(9.0f);
        smallLineNumberFont = defaultFont.deriveFont(7.0f);
        Component c = getContainer();
        lineNumberMetrics = c.getFontMetrics(lineNumberFont);
    }
    
    /**
     * Draw the line number in front of the line
     */
    protected void drawLineNumber(Graphics g, int lineNumber, int x, int y)
    {
        g.setColor(Color.darkGray);

        String number = Integer.toString(lineNumber);
        int stringWidth = lineNumberMetrics.stringWidth(number);
        int xoffset = BREAKPOINT_OFFSET - stringWidth - 4;

        if(xoffset < -2)      // if it doesn't fit, shift one pixel over.
            xoffset++;

        if(xoffset < -2) {    // if it still doesn't fit...
            g.setFont(smallLineNumberFont);
            g.drawString(number, x-3, y);
        }
        else {
            g.setFont(lineNumberFont);
            g.drawString(number, x + xoffset, y);
        }
        g.setFont(defaultFont);
    }
     
    /**
     * Paint the line markers such as breakpoint, step mark
     */
    protected void paintLineMarkers(int lineIndex, Graphics g, int x, int y,
            MoeSyntaxDocument document, Element line)
    {
        if(PrefMgr.getFlag(PrefMgr.LINENUMBERS))
            drawLineNumber(g, lineIndex+1, x, y);
   
        // draw breakpoint and/or step image
   
        if(hasTag(line, BREAKPOINT)) {
            if(hasTag(line, STEPMARK)) {
                g.drawImage(breakStepImage, x-1, y+3-breakStepImage.getHeight(null), 
                            null);
            }
            else {  // break only
                g.drawImage(breakImage, x-1, y+3-breakImage.getHeight(null), null);
            }
        }
        else if(hasTag(line, STEPMARK)) {
            g.drawImage(stepImage, x-1, y+3-stepImage.getHeight(null), null);
        }
    }

    /* When painting a line also paint the markers (breakpoint marks, step marks) etc.
     * @see bluej.editor.moe.BlueJSyntaxView#paintTaggedLine(javax.swing.text.Segment, int, java.awt.Graphics, int, int, bluej.editor.moe.MoeSyntaxDocument, java.awt.Color, javax.swing.text.Element)
     */
    @Override
    public void paintTaggedLine(Segment lineText, int lineIndex, Graphics g, int x, int y, 
            MoeSyntaxDocument document, Color def, Element line, TabExpander tx) 
    {
        paintLineMarkers(lineIndex, g, x - LEFT_MARGIN, y, document, line);
        if (document.getParsedNode() != null && syntaxHighlighting) {
            paintSyntaxLine(lineText, lineIndex, x, y, g, document, def, tx);
        }
        else {
            paintPlainLine(lineIndex, g, x, y);
        }
    }
        
   /*
    * redefined paint method to paint breakpoint area
    */
    public void paint(Graphics g, Shape allocation)
    {
        // if uncompiled, fill the tag line with grey
        Rectangle bounds = allocation.getBounds();        
        if(Boolean.FALSE.equals(getDocument().getProperty(MoeEditor.COMPILED))) {
            g.setColor(Color.lightGray);
            g.fillRect(0, 0, bounds.x + TAG_WIDTH,
                       bounds.y + bounds.height);
        }
        
        Rectangle clip = g.getClipBounds();
        
        // Left margin
        g.setColor(new Color(240, 240, 240));
        g.drawLine(bounds.x + LEFT_MARGIN - 1, clip.y, bounds.x + LEFT_MARGIN - 1, clip.y + clip.height);
        
        // Scope highlighting
        int spos = viewToModel(bounds.x, clip.y, allocation, new Position.Bias[1]);
        int epos = viewToModel(bounds.x, clip.y + clip.height - 1, allocation, new Position.Bias[1]);
        
        MoeSyntaxDocument document = (MoeSyntaxDocument)getDocument();
        if (document.getParsedNode() != null) {
            Element map = getElement();
            int firstLine = map.getElementIndex(spos);
            int lastLine = map.getElementIndex(epos);
            paintScopeMarkers(g, document, allocation, firstLine, lastLine, false);
        }
        
        // paint the lines
        super.paint(g, allocation);

        // paint the tag separator line
        g.setColor(Color.black);
        g.drawLine(bounds.x + TAG_WIDTH, 0,
                   bounds.x + TAG_WIDTH, bounds.y + bounds.height);
    }

}
