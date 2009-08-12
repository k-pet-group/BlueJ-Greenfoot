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

/**
 * MoeSyntaxView.java - adapted from
 * SyntaxView.java - jEdit's own Swing view implementation
 * to add Syntax highlighting to the BlueJ programming environment.
 */

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.LinkedList;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Segment;

import bluej.Config;
import bluej.parser.ParsedNode;
import bluej.parser.NodeTree.NodeAndPosition;
import bluej.prefmgr.PrefMgr;

/**
 * A Swing view implementation that colorizes lines of a
 * SyntaxDocument using a TokenMarker.
 *
 * This class should not be used directly; a SyntaxEditorKit
 * should be used instead.
 *
 * @author Slava Pestov
 * @author Bruce Quig
 * @author Michael Kolling
 *
 * @version $Id: MoeSyntaxView.java 6506 2009-08-12 05:39:15Z davmac $
 */

public class MoeSyntaxView extends BlueJSyntaxView
{
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
        super(elem);
    }

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
        
        // draw left margin
        Color oldColor = g.getColor();
        g.setColor(Color.GRAY);
        FontMetrics fm = g.getFontMetrics();
        int y1 = y - fm.getAscent() - fm.getLeading();
        int y2 = y + fm.getDescent();
        g.drawLine(x + LEFT_MARGIN - 1, y1, x + LEFT_MARGIN - 1, y2);
        g.setColor(oldColor);
    }
    
    /**
     * Pain the scope markers for the given line.
     */
    protected void paintScopeMarkers(Graphics g, MoeSyntaxDocument document, int x, int y, Element line)
    {
        ParsedNode rootNode = document.getParser();
        
        int pos = line.getEndOffset();
        
        List<NodeAndPosition> scopeStack = new LinkedList<NodeAndPosition>();
        rootNode.getNodeStack(scopeStack, pos, 0);
        
        List<NodeAndPosition> scopeStackAlt = new LinkedList<NodeAndPosition>();
        rootNode.getNodeStack(scopeStackAlt, line.getStartOffset(), 0);
        
        if (scopeStackAlt.size() > scopeStack.size()) {
            scopeStack = scopeStackAlt;
        }
        
        for (NodeAndPosition nap: scopeStack) {
            if (nap.getNode().isContainer()) {
                int indent = getIndentFor(document, nap);
                FontMetrics fm = g.getFontMetrics();
                int ypos = y - fm.getAscent() - fm.getLeading();
                int ypos2 = y + fm.getDescent();
                int char_width = fm.charWidth(' ');
                int xpos = x + LEFT_MARGIN + char_width * indent - 2;
                g.drawLine(xpos, ypos, xpos, ypos2);
            }
        }
    }
    
    /**
     * Get the scope indent for a given node.
     */
    private int getIndentFor(MoeSyntaxDocument document, NodeAndPosition nap)
    {
        // For now, just return indent of first line
        int nodePos = nap.getPosition();
        int firstLineIndex = document.getDefaultRootElement().getElementIndex(nodePos);
        Element firstLine = document.getDefaultRootElement().getElement(firstLineIndex);
        
        try {
            String text = document.getText(firstLine.getStartOffset(), firstLine.getEndOffset() - firstLine.getStartOffset());
            int indent = 0;
            int lpos = 0;
            while (lpos < text.length()) {
                int thechar = text.charAt(lpos++);
                if (thechar == ' ') {
                    indent++;
                }
                else if (thechar == '\t') {
                    indent += getTabSize();
                }
                else {
                    break;
                }
            }
            return indent;
        } catch (BadLocationException e) {
            return 0;
        }
    }
    
	public void paintTaggedLine(Segment lineText, int lineIndex, Graphics g, int x, int y, 
            MoeSyntaxDocument document, Color def, Element line) 
    {
	    paintLineMarkers(lineIndex, g, x, y, document, line);
	    paintScopeMarkers(g, document, x, y, line);

        //if(tokenMarker == null) {
        //    Utilities.drawTabbedText(lineText, x+BREAKPOINT_OFFSET, y, g, this, 0);            
        //}
	    int linePos = document.getDefaultRootElement().getElement(lineIndex).getStartOffset();
	    paintSyntaxLine(lineText, lineIndex, x + LEFT_MARGIN, y, g, document, def);
    }
	
   /**
    * redefined paint method to paint breakpoint area
    *
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

        // paint the lines
        super.paint(g, allocation);

        // paint the tag separator line
        g.setColor(Color.black);
        g.drawLine(bounds.x + TAG_WIDTH, 0,
                   bounds.x + TAG_WIDTH, bounds.y + bounds.height);
    }

}