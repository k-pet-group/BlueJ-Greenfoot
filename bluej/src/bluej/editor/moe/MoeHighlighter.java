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
package bluej.editor.moe;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.Position;
import javax.swing.text.View;

/**
 * A custom highlighter which handles some things much better than the DefaultHighlighter.
 * 
 * @author Davin McCall
 */
public class MoeHighlighter extends LayeredHighlighter
{
    // Concerns: highlight is specified as between two document positions...
    //   ... but: painter draws slightly outside the range of those positions
    //   ... only the painter itself knows its true extent
    
    private JTextComponent component;
    
    private List<StandardMoeHighlight> highlights = new LinkedList<StandardMoeHighlight>();
    private LinkedList<MoeHighlight> layeredHighlights = new LinkedList<MoeHighlight>();
    
    private abstract static class MoeHighlight
    {
        protected Position startPos;
        protected Position endPos;
        
        public MoeHighlight(Position startPos, Position endPos)
        {
            this.startPos = startPos;
            this.endPos = endPos;
        }
        
        //@Override
        public int getStartOffset()
        {
            return startPos.getOffset();
        }
        
        //@Override
        public int getEndOffset()
        {
            return endPos.getOffset();
        }
        
        /**
         * Issue a repaint to the component, in order that this highlight be repainted.
         */
        public void repaintHighlight(JTextComponent component)
        {
            int p0 = startPos.getOffset();
            int p1 = endPos.getOffset();
            // Must take extra area into account
            // component.getUI().damageRange(component, p0, p1, Bias.Forward, Bias.Backward);
            
            // TextUI interface is pretty bad. We can't find out what the paint region between two positions
            // is - we only have the option to issue a repaint (via damageRange()). So we make some assumptions
            // about the UI implementation here.
            
            Rectangle r = component.getBounds();
            Insets insets = component.getInsets();
            r.x = insets.left;
            r.y = insets.top;
            r.width -= insets.left + insets.right;
            r.height -= insets.top + insets.bottom;
            
            View rootView = component.getUI().getRootView(component);
            try {
                Rectangle region = rootView.modelToView(p0, Position.Bias.Forward,
                        p1, Position.Bias.Backward, r).getBounds();
                component.repaint(region);
            }
            catch (BadLocationException ble) {
                throw new RuntimeException(ble);
            }
        }
        
        /**
         * Paint this highlight as a layered highlight
         * @param g  The graphics context to draw in
         * @param p0   The start of the damaged region (first damaged character)
         * @param p1   The end of the damaged region  (one beyond last damaged character)
         * @param viewBounds  The bounds of the view containing the highlight
         * @param editor   The component
         * @param view    The view
         */
        public abstract void paint(Graphics g, int p0, int p1, Shape viewBounds, JTextComponent editor, View view);
    }
    
    /**
     * A highlight handling layered highlights.
     */
    private static class StandardMoeHighlight extends MoeHighlight
    {
        private HighlightPainter painter;

        public StandardMoeHighlight(Position startPos, Position endPos, HighlightPainter painter)
        {
            super(startPos, endPos);
            this.painter = painter;
        }
        
        public HighlightPainter getPainter()
        {
            return painter;
        }

        public void paint(Graphics g, int p0, int p1, Shape viewBounds, JTextComponent editor, View view)
        {
            int start = startPos.getOffset();
            int end = endPos.getOffset();

            if ((p0 < start && p1 > start) ||
                    (p0 >= start && p0 < end)) {
                p0 = Math.max(p0, start);
                p1 = Math.min(p1, end);
                ((LayerPainter) painter).paintLayer(g, p0, p1, viewBounds, editor, view);
            }
        }
    }
    
    /**
     * A highlight handling advanced highlights.
     */
    private static class AdvancedMoeHighlight extends MoeHighlight
    {
        private AdvancedHighlightPainter painter;
        
        public AdvancedMoeHighlight(Position startPos, Position endPos, AdvancedHighlightPainter painter)
        {
            super(startPos, endPos);
            this.painter = painter;
        }
        
        @Override
        public void repaintHighlight(JTextComponent component)
        {
            int p0 = startPos.getOffset();
            int p1 = endPos.getOffset();
            Rectangle r = component.getBounds();
            Insets insets = component.getInsets();
            r.x = insets.left;
            r.y = insets.top;
            r.width -= insets.left + insets.right;
            r.height -= insets.top + insets.bottom;
            
            View rootView = component.getUI().getRootView(component);
            painter.issueRepaint(p0, p1, r, component, rootView);
        }
        
        @Override
        public void paint(Graphics g, int p0, int p1, Shape viewBounds,
                JTextComponent editor, View view)
        {
            painter.paint(g, startPos.getOffset(), endPos.getOffset(), viewBounds, editor, view);
        }
    }
    
    @Override
    public Object addHighlight(int p0, int p1, HighlightPainter p)
            throws BadLocationException
    {
        Document doc = component.getDocument();
        Position pos0 = doc.createPosition(p0);
        Position pos1 = doc.createPosition(p1);
        StandardMoeHighlight highlight = new StandardMoeHighlight(pos0, pos1, p);
        if (p instanceof LayerPainter) {
            layeredHighlights.add(highlight);
        }
        else {
            highlights.add(highlight);
        }
        highlight.repaintHighlight(component);
        return highlight;
    }
    
    public Object addHighlight(int p0, int p1, AdvancedHighlightPainter p)
            throws BadLocationException
    {
        Document doc = component.getDocument();
        Position pos0 = doc.createPosition(p0);
        Position pos1 = doc.createPosition(p1);
        AdvancedMoeHighlight highlight = new AdvancedMoeHighlight(pos0, pos1, p);
        layeredHighlights.add(highlight);
        highlight.repaintHighlight(component);
        return highlight;
    }
    
    @Override
    public void changeHighlight(Object tag, int p0, int p1)
            throws BadLocationException
    {
        MoeHighlight highlight = (MoeHighlight) tag;
        Document doc = component.getDocument();
        highlight.repaintHighlight(component); // repaint the old area
        highlight.startPos = doc.createPosition(p0);
        highlight.endPos = doc.createPosition(p1);
        highlight.repaintHighlight(component); // repaint the new area
    }
    
    @Override
    public void install(JTextComponent c)
    {
        removeAllHighlights();
        component = c;
    }

    @Override
    public void deinstall(JTextComponent c)
    {
        component = null;
    }
    
    @Override
    public Highlight[] getHighlights()
    {
        return highlights.toArray(new Highlight[highlights.size()]);
    }
    
    @Override
    public void paint(Graphics g)
    {
        Rectangle r = component.getBounds();
        Insets insets = component.getInsets();
        r.x = insets.left;
        r.y = insets.top;
        r.width -= insets.left + insets.right;
        r.height -= insets.top + insets.bottom;
        
        for (StandardMoeHighlight highlight : highlights) {
            highlight.getPainter().paint(g, highlight.getStartOffset(), highlight.getEndOffset(), r, component);
        }
    }
    
    @Override
    public void paintLayeredHighlights(Graphics g, int p0, int p1,
            Shape viewBounds, JTextComponent editor, View view)
    {
        int viewStart = view.getStartOffset();
        int viewEnd = view.getEndOffset();

        Iterator<MoeHighlight> i = layeredHighlights.descendingIterator();
        for ( ; i.hasNext(); ) {
            MoeHighlight highlight = i.next();
            if (highlight.startPos.getOffset() < viewEnd && highlight.endPos.getOffset() > viewStart) {
                highlight.paint(g, p0, p1, viewBounds, editor, view);
            }
        }
    }
    
    @Override
    public void removeAllHighlights()
    {
        layeredHighlights.clear();
        highlights.clear();
        if (component != null) {
            component.repaint();
        }
    }
    
    @Override
    public void removeHighlight(Object tag)
    {
        MoeHighlight mh = (MoeHighlight) tag;
        mh.repaintHighlight(component);
        highlights.remove(tag);
        layeredHighlights.remove(tag);
    }
}
