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
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.Position.Bias;

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
 * @version $Id: MoeSyntaxView.java 6530 2009-08-15 03:50:28Z davmac $
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
//        Color oldColor = g.getColor();
//        g.setColor(Color.GRAY);
//        FontMetrics fm = g.getFontMetrics();
//        int y1 = y - fm.getAscent() - fm.getLeading();
//        int y2 = y + fm.getDescent();
//        g.drawLine(x + LEFT_MARGIN - 1, y1, x + LEFT_MARGIN - 1, y2);
//        g.setColor(oldColor);
    }
    
    /**
     * Pain the scope markers for the given line.
     */
    protected void paintScopeMarkers(Graphics g, MoeSyntaxDocument document, int x, int y, Element line)
    {
        ParsedNode rootNode = document.getParser();
        
        int pos = line.getEndOffset() - 1;
        
        List<NodeAndPosition> scopeStack = new LinkedList<NodeAndPosition>();
        rootNode.getNodeStack(scopeStack, pos, 0);
        
        List<NodeAndPosition> scopeStackAlt = new LinkedList<NodeAndPosition>();
        rootNode.getNodeStack(scopeStackAlt, line.getStartOffset(), 0);
        
        if (scopeStackAlt.size() > scopeStack.size()) {
            scopeStack = scopeStackAlt;
        }
        
        Color c1 = new Color(210, 230, 210); // green border (container)
        Color c2 = new Color(250, 255, 250); // green wash
        Color c3 = new Color(230, 240, 230); // green border (inner).
        Color c4 = new Color(255, 255, 255); // white wash
        
        boolean container = false;
        Color origColor = g.getColor();
        
        Rectangle bounds = g.getClipBounds();
        
        int lastIndent = 0;
        for (NodeAndPosition nap: scopeStack) {
            int ypos = y;
            int ypos2 = y + metrics.getHeight();
            if (nap.getNode().isContainer()) {
                container = true;
                int indent = getIndentFor(document, nap);
                if (indent == -1) {
                    indent = lastIndent + getTabSize();
                }
                lastIndent = indent;
                FontMetrics fm = g.getFontMetrics();
                int char_width = fm.charWidth(' ');
                int xpos = x + char_width * indent - 2;
                g.setColor(c1);
                g.drawLine(xpos, ypos, xpos, ypos2);
                g.setColor(c2);
                g.fillRect(xpos + 1, ypos, bounds.x + bounds.width - (xpos+1), ypos2 - ypos);
            }
            else if (container) {
                container = false;
                if (nap.getNode().isInner()) {
                    int indent = getIndentFor(document, nap);
                    if (indent == -1) {
                        indent = lastIndent + getTabSize();
                    }
                    lastIndent = indent;
                    
                    int lineoffset = line.getStartOffset();
                    int lineend = line.getEndOffset();
                    if (nap.getPosition() < lineend && nap.getPosition() > lineoffset) {
                        // The node starts on this line. Often the first line will be empty, in which
                        // case we want to ignore it.
                        Segment lineText = new Segment();
                        try {
                            document.getText(nap.getPosition(), lineend - nap.getPosition(), lineText);
                            if (isAllWhitespace(lineText)) {
                                break;
                            }
                        } catch (BadLocationException e) {}
                    }
                    
                    int nodeEnd = nap.getPosition() + nap.getSize();
                    if (nodeEnd > lineoffset && nodeEnd <= lineend) {
                        // likewise, node ends on this line
                        Segment lineText = new Segment();
                        try {
                            document.getText(lineoffset, nodeEnd - lineoffset, lineText);
                            if (isAllWhitespace(lineText)) {
                                break;
                            }
                        } catch (BadLocationException e) {}
                    }
                    
                    FontMetrics fm = g.getFontMetrics();
                    int char_width = fm.charWidth(' ');
                    int xpos = x + char_width * indent - 5;
                    g.setColor(c3);
                    g.drawLine(xpos, ypos, xpos, ypos2);
                    g.setColor(c4);
                    g.fillRect(xpos + 1, ypos, bounds.x + bounds.width - (xpos+1), ypos2 - ypos);
                }
            }
        }
        g.setColor(origColor);
    }
    
    private boolean isAllWhitespace(Segment segment)
    {
        int endpos = segment.offset + segment.count;
        for (int i = segment.offset; i < endpos; i++) {
            char c = segment.array[i];
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get the scope indent for a given node.
     */
    private int getIndentFor(MoeSyntaxDocument document, NodeAndPosition nap)
    {
        return nap.getNode().getLeftmostIndent(document, nap.getPosition());
    }
    
    public void paintTaggedLine(Segment lineText, int lineIndex, Graphics g, int x, int y, 
            MoeSyntaxDocument document, Color def, Element line) 
    {
        paintLineMarkers(lineIndex, g, x, y, document, line);

        //if(tokenMarker == null) {
        //    Utilities.drawTabbedText(lineText, x+BREAKPOINT_OFFSET, y, g, this, 0);            
        //}
        
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
        
        Rectangle clip = g.getClipBounds();
        
        int spos = viewToModel(bounds.x, clip.y, allocation, new Position.Bias[1]);
        int epos = viewToModel(bounds.x, clip.y + clip.height - 1, allocation, new Position.Bias[1]);
        
        Element map = getElement();
        for (int i = spos; i <= epos; ) {
            int lineIndex = map.getElementIndex(i);
            Element line = map.getElement(lineIndex);
            try {
                Shape lshape = modelToView(line.getStartOffset(), allocation, Bias.Forward);
                paintScopeMarkers(g, (MoeSyntaxDocument) map.getDocument(), lshape.getBounds().x, lshape.getBounds().y, line);
            } catch (BadLocationException e) {
            }
            i = line.getEndOffset();
        }
        
        // Left margin
        g.setColor(new Color(240, 240, 240));
        g.drawLine(bounds.x + LEFT_MARGIN - 1, clip.y, bounds.x + LEFT_MARGIN - 1, clip.y + clip.height);
        
        // paint the lines
        super.paint(g, allocation);

        // paint the tag separator line
        g.setColor(Color.black);
        g.drawLine(bounds.x + TAG_WIDTH, 0,
                   bounds.x + TAG_WIDTH, bounds.y + bounds.height);
    }

}
