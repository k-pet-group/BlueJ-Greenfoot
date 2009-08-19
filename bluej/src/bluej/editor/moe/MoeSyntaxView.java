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
 * MoeSyntaxView.java - originally adapted from
 * SyntaxView.java - jEdit's own Swing view implementation -
 * to add Syntax highlighting to the BlueJ programming environment.
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.Segment;

import bluej.Config;
import bluej.parser.ParsedNode;
import bluej.parser.NodeTree.NodeAndPosition;
import bluej.prefmgr.PrefMgr;

/**
 * A Swing view implementation that colorizes lines of a
 * SyntaxDocument.
 *
 * This class should not be used directly; a SyntaxEditorKit
 * should be used instead.
 *
 * @author Slava Pestov
 * @author Bruce Quig
 * @author Michael Kolling
 * @author Davin McCall
 *
 * @version $Id: MoeSyntaxView.java 6539 2009-08-19 06:16:20Z davmac $
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
    }
    
    protected void paintScopeMarkers(Graphics g, MoeSyntaxDocument document, Shape a, int firstLine, int lastLine)
    {
        // document.getDefaultRootElement().getElementCount();
        Element map = document.getDefaultRootElement();
        ParsedNode rootNode = document.getParser();
        Rectangle bounds = g.getClipBounds();
        int char_width = metrics.charWidth('m');

        Color c1 = new Color(210, 230, 210); // green border (container)
        //Color c2 = new Color(250, 255, 250); // green wash
        Color c2 = new Color(220, 255, 220);
        Color c3 = new Color(230, 240, 230); // green border (inner).
        Color c4 = new Color(255, 255, 255); // white wash
        
        int aboveLine = firstLine - 1;
        List<NodeAndPosition> prevScopeStack = new LinkedList<NodeAndPosition>();
        int curLine = firstLine;
        
        try {
            Element aboveLineEl = null;
            Element thisLineEl = map.getElement(firstLine);
            if (aboveLine >= 0) {
                aboveLineEl = map.getElement(aboveLine);
            }
            Segment aboveLineSeg = new Segment();
            Segment thisLineSeg = new Segment();
            
            if (aboveLine >= 0) {
                document.getText(aboveLineEl.getStartOffset(),
                        aboveLineEl.getEndOffset() - aboveLineEl.getStartOffset(),
                        aboveLineSeg);
            }
            document.getText(thisLineEl.getStartOffset(),
                    thisLineEl.getEndOffset() - thisLineEl.getStartOffset(),
                    thisLineSeg);

            //rootNode.getNodeStack(prevScopeStack, thisLineEl.getStartOffset(), 0);
            getScopeStackAt(rootNode, thisLineEl.getStartOffset(), prevScopeStack);

            while (curLine <= lastLine) {
                
                if (prevScopeStack.size() == 0) {
                    break;
                }
                
                Rectangle lbounds = modelToView(thisLineEl.getStartOffset(), a, Position.Bias.Forward).getBounds();
                int ypos = lbounds.y;
                int ypos2 = ypos + lbounds.height;

                ListIterator<NodeAndPosition> li = prevScopeStack.listIterator();
                while (li.hasNext()) {
                    NodeAndPosition nap = li.next();
                    int napEnd = nap.getPosition() + nap.getSize();
                    
                    if (aboveLineEl != null && nap.getPosition() >= aboveLineEl.getStartOffset()) {
                        // officially starts on the previous line but...
                        int n = findNonWhitespace(aboveLineSeg, nap.getPosition() - aboveLineEl.getStartOffset());
                        if (n == -1) {
                            // effectively starts on this line!
                            // TODO
                        }
                        else if (n + aboveLineEl.getStartOffset() < napEnd) {
                            // It starts on the previous line, but continues onto this line.
                            // However the start on the line above might be to the right of the normal
                            // indent, meaning we need to draw some border here.
                            // TODO
                        }
                    }

                    //int n = findNonWhitespace(thisLineSeg, 0) + thisLineEl.getStartOffset();
                    int endX = bounds.x + bounds.width;

                    if (nap.getPosition() >= thisLineEl.getEndOffset()) {
                        // The node isn't even on this line, go to the next line
                        break;
                    }

                    if (! nap.getNode().isContainer() && ! nap.getNode().isInner()) {
                        continue; // no need to draw the node at all.
                    }
                    
                    if (napEnd < thisLineEl.getEndOffset()) {
                        // The node ends on this line. Just whitespace?
                        int nws = findNonWhitespace(thisLineSeg, 0);
                        if (nws == -1 || thisLineEl.getStartOffset() + nws >= napEnd) {
                            break;
                        }
                        
                        // The node ends on this line, so don't extend it too far
                        nws = findNonWhitespace(thisLineSeg,
                                nap.getPosition() + nap.getSize() - thisLineEl.getStartOffset());
                        if (nws != -1) {
                            Rectangle rr = modelToView(thisLineEl.getStartOffset() + nws,
                                    a, Position.Bias.Forward).getBounds();
                            endX = rr.x + rr.width;
                        }
                    }
                    
                    // Ok, it includes text on this line. Or maybe just whitespace?
                    int nws = 0;
                    boolean startsThisLine = (nap.getPosition() >= thisLineEl.getStartOffset());
                    // TODO also starts this line if it really starts on the previous line,
                    //      but that only has whitespace
                    if (startsThisLine) {
                        nws = findNonWhitespace(thisLineSeg, nap.getPosition() - thisLineEl.getStartOffset());
                        if (nws == -1) {
                            continue; // just white space on this line
                        }
                        
                        // if the node begins further right of the indent, we might still colour
                        // from the indent - only if that section is just whitespace though.
                        int ls = thisLineEl.getStartOffset();
                        //int limit = viewToModel(fx, fy, a, bias)
                        
                        int nwsb = findNonWhitespaceBwards(thisLineSeg,
                                nap.getPosition() - ls,
                                0);
                        nws = Math.max(nws, nwsb);
                    }

                    int indent = nap.getNode().getLeftmostIndent(document, nap.getPosition());

                    if (nap.getNode().isContainer()) {
                        int xpos = lbounds.x + char_width * indent - 2;
                        if (nws != 0) {
                            xpos = Math.max(xpos, modelToView(thisLineEl.getStartOffset() + nws, a, Position.Bias.Forward).getBounds().x - 2);
                        }
                        g.setColor(c1);
                        g.drawLine(xpos, ypos, xpos, ypos2);
                        g.setColor(c2);
                        g.fillRect(xpos + 1, ypos, endX - (xpos+1), ypos2 - ypos);
                    }
                    else if (nap.getNode().isInner()) {
                        int xpos = lbounds.x + indent * char_width;
                        if (nws != 0) {
                            xpos = Math.max(xpos, modelToView(thisLineEl.getStartOffset() + nws, a, Position.Bias.Forward).getBounds().x);
                        }
                        xpos -= 5;
                        g.setColor(c3);
                        g.drawLine(xpos, ypos, xpos, ypos2);
                        g.setColor(c4);
                        g.fillRect(xpos + 1, ypos, endX - (xpos+1), ypos2 - ypos);
                    }
                }

                // Move along
                li = prevScopeStack.listIterator(prevScopeStack.size());
                NodeAndPosition nap = li.previous();
                if (nap.getPosition() + nap.getSize() <= thisLineEl.getEndOffset()) {
                    li.remove();
                    int napEnd = nap.getPosition() + nap.getSize();
                    while (li.hasPrevious()) {
                        NodeAndPosition napParent = li.previous();
                        li.next();
                        nap = napParent.getNode().findNodeAtOrAfter(napEnd, napParent.getPosition());
                        while (nap != null) {
                            //napEnd = nap.getPosition() + nap.getSize();
                            if (nap.getPosition() < thisLineEl.getEndOffset()) {
                                // TODO
                                // draw it
                            }
                            li.add(nap);
                            nap = nap.getNode().findNodeAtOrAfter(napEnd, nap.getPosition());
                        }
                        if (napParent.getPosition() + napParent.getSize() >= thisLineEl.getEndOffset()) {
                            break;
                        }
                        nap = li.previous();
                        napEnd = nap.getPosition() + nap.getSize();
                        li.remove();
                    }
                }
                
                // Next line
                curLine++;
                if (curLine <= lastLine) {
                    aboveLineEl = thisLineEl;
                    thisLineEl = map.getElement(curLine);
                    Segment oldAbove = aboveLineSeg;
                    aboveLineSeg = thisLineSeg;
                    thisLineSeg = oldAbove; // recycle the object
                    document.getText(thisLineEl.getStartOffset(),
                            thisLineEl.getEndOffset() - thisLineEl.getStartOffset(),
                            thisLineSeg);
                }
            }
        }
        catch (BadLocationException ble) {}
    }
    
    private void getScopeStackAt(ParsedNode root, int position, List<NodeAndPosition> list)
    {
        list.add(new NodeAndPosition(root, 0, root.getSize()));
        int curpos = 0;
        NodeAndPosition nap = root.findNodeAtOrAfter(position, curpos);
        while (nap != null) {
            list.add(nap);
            curpos = nap.getPosition();
            nap = nap.getNode().findNodeAtOrAfter(position, curpos);
        }
    }
    
    private int findNonWhitespace(Segment segment, int startPos)
    {
        int endpos = segment.offset + segment.count;
        for (int i = segment.offset + startPos; i < endpos; i++) {
            char c = segment.array[i];
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return i - segment.offset;
            }
        }
        return -1;
    }
    
    private int findNonWhitespaceBwards(Segment segment, int startPos, int endPos)
    {
        int lastP = segment.offset + endPos;
        int i;
        for (i = segment.offset + startPos; i > lastP; i--) {
            char c = segment.array[i];
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                break;
            }
        }
        return i - segment.offset;    }
    
    public void paintTaggedLine(Segment lineText, int lineIndex, Graphics g, int x, int y, 
            MoeSyntaxDocument document, Color def, Element line) 
    {
        paintLineMarkers(lineIndex, g, x, y, document, line);
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
        int firstLine = map.getElementIndex(spos);
        int lastLine = map.getElementIndex(epos);
        paintScopeMarkers(g, (MoeSyntaxDocument) getDocument(), allocation, firstLine, lastLine);
        
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
