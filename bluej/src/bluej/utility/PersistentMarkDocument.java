/*
 This file is part of the BlueJ program.
 Copyright (C) 2011,2013,2014  Michael Kolling and John Rosenberg

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
package bluej.utility;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.GapContent;
import javax.swing.text.PlainDocument;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * Document implementation for the terminal editor pane and codepad.
 * 
 * <p>This is mainly necessary to override PlainDocument's slightly brain-damaged
 * implementation of the insertUpdate() method, which can clear line attributes
 * unexpectedly (insertUpdate method).
 * 
 * <p>(Abstracted out from TerminalDocument class, August 2013).
 * 
 * @author Davin McCall
 */
public class PersistentMarkDocument extends AbstractDocument
{
    protected Element root;

    public static final String tabSizeAttribute = PlainDocument.tabSizeAttribute;
    
    public PersistentMarkDocument()
    {
        super(new GapContent());
        root = createDefaultRoot();
    }
    
    @Override
    public Element getDefaultRootElement()
    {
        return root;
    }
    
    @Override
    public Element getParagraphElement(int pos)
    {
        int index = root.getElementIndex(pos);
        return root.getElement(index);
    }
    
    protected AbstractElement createDefaultRoot()
    {
        BranchElement map = (BranchElement) createBranchElement(null, null);
        Element[] lines = new Element[1];
        lines[0] = new LeafElement(map, null, 0, 1);
        map.replace(0, 0, lines);
        return map;
    }
    
    /**
     * An undo/redo-able edit setting the start position of an element.
     */
    private class SetStartPos extends AbstractUndoableEdit
    {
        private LeafElement element;
        private Position oldStart;
        private Position newStart;
        
        public SetStartPos(LeafElement el, Position oldStart, Position newStart)
        {
            this.element = el;
            this.oldStart = oldStart;
            this.newStart = newStart;
        }
        
        @Override
        public void undo() throws CannotUndoException
        {
            super.undo();
            element.start = oldStart;
        }
        
        @Override
        public void redo() throws CannotRedoException
        {
            super.redo();
            element.start = newStart;
        }
    }

    /**
     * An undo/redo-able edit setting the end position of an element.
     */
    private class SetEndPos extends AbstractUndoableEdit
    {
        private LeafElement element;
        private Position oldEnd;
        private Position newEnd;
        
        public SetEndPos(LeafElement el, Position oldEnd, Position newEnd)
        {
            this.element = el;
            this.oldEnd = oldEnd;
            this.newEnd = newEnd;
        }
        
        @Override
        public void undo() throws CannotUndoException
        {
            super.undo();
            element.end = oldEnd;
        }
        
        @Override
        public void redo() throws CannotRedoException
        {
            super.redo();
            element.end = newEnd;
        }
    }
    
    @Override
    protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attr)
    {
        BranchElement lineMap = (BranchElement) getDefaultRootElement();
        int offset = chng.getOffset();
        int length = chng.getLength();
        
        Segment s = new Segment();
        try {
            getText(offset, length, s);
        }
        catch (BadLocationException ble) {
            throw new RuntimeException(ble);
        }
        
        int index = lineMap.getElementIndex(offset);
        LeafElement firstAffected = (LeafElement) lineMap.getElement(index);
        
        int lindex = lineMap.getElementIndex(offset + length);
        LeafElement nextLine = (LeafElement) lineMap.getElement(lindex);
        
        if (offset > 0 && (offset + length) == nextLine.getStartOffset()) {
            // Inserting at a position moves the position, unless the position is 0.
            // So inserting at the beginning of a line moves the line start position,
            // and the previous line end position, which need to be reset:
            Position origEndPos = firstAffected.end;
            firstAffected.setEndOffset(offset);
            chng.addEdit(new SetEndPos(firstAffected, origEndPos, firstAffected.end));
            nextLine.setStartOffset(offset);
            Position origStartPos = nextLine.start;
            chng.addEdit(new SetStartPos(nextLine, origStartPos, nextLine.start));
            
            firstAffected = nextLine;
            nextLine = (LeafElement) lineMap.getElement(lindex + 1);
        }
        
        ArrayList<LeafElement> added = new ArrayList<LeafElement>();
        
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                // line break!
                int origEnd = firstAffected.getEndOffset();
                Position origEndPos = firstAffected.end;
                firstAffected.setEndOffset(i + offset + 1);
                chng.addEdit(new SetEndPos(firstAffected, origEndPos, firstAffected.end));
                LeafElement newFirst = new LeafElement(root, attr, i + offset + 1, origEnd);
                added.add(newFirst);
                firstAffected = newFirst;
            }
        }
        
        if (! added.isEmpty()) {
            Element [] removed = new Element[0];
            Element [] addedArr = new Element[added.size()];
            added.toArray(addedArr);
            lineMap.replace(lindex + 1, 0, addedArr);
            ElementEdit ee = new ElementEdit(lineMap, lindex + 1, removed, addedArr);
            chng.addEdit(ee);
        }
        
        super.insertUpdate(chng, attr);
    }

    @Override
    protected void removeUpdate(DefaultDocumentEvent chng)
    {
        BranchElement lineMap = (BranchElement) getDefaultRootElement();
        int offset = chng.getOffset();
        int length = chng.getLength();
        
        int index = lineMap.getElementIndex(offset);
        
        LeafElement first = (LeafElement) lineMap.getElement(index);
        
        if (offset != 0 && first.getStartOffset() == offset) {
            // Removing from the start of a line has the effect that an undo will
            // insert at the *end of the previous line*, which moves the position of the end
            // and the start of this line. We need to add undo records to correct the line end
            // position and line start position.
            LeafElement prev = (LeafElement) lineMap.getElement(index - 1);
            try {
                // These positions are the same, but it's probably not wise to share Position
                // objects between line elements, so we'll create two:
                Position newEnd = createPosition(offset + length);
                Position newStart = createPosition(offset + length);
                chng.addEdit(new SetEndPos(prev, prev.end, newEnd));
                chng.addEdit(new SetStartPos(first, first.start, newStart));
            }
            catch (BadLocationException ble) {
                // shouldn't happen
                throw new RuntimeException(ble);
            }
        }
        
        if (first.getEndOffset() > (offset + length)) {
            return; // only removing part of a line
        }
        
        ArrayList<Element> removed = new ArrayList<Element>();
        
        int lastIndex = index + 1;
        LeafElement last = (LeafElement) lineMap.getElement(lastIndex);
        removed.add(last);
        while (last.getEndOffset() <= (offset + length)) {
            lastIndex++;
            last = (LeafElement) lineMap.getElement(lastIndex);
            removed.add(last);
        }
        
        // The first line now extends to the end of the last line, since all intermediate
        // line breaks were removed:
        Position origEnd = first.end;
        first.end = last.end;
        chng.addEdit(new SetEndPos(first, origEnd, first.end));
        lineMap.replace(index + 1, removed.size(), new Element[0]);
        
        Element[] removedArr = new Element[removed.size()];
        removed.toArray(removedArr);
        ElementEdit ee = new ElementEdit(lineMap, index + 1, removedArr, new Element[0]);
        chng.addEdit(ee);
        
        super.removeUpdate(chng);
    }
    
    /**
     * Special purposed leaf element which allows resetting the start and end
     * positions.
     */
    public class LeafElement extends AbstractElement
    {
        Position start;
        Position end;
        
        public LeafElement(Element parent, AttributeSet attrs, int startOffs, int endOffs)
        {
            super(parent, attrs);
            try {
                start = createPosition(startOffs);
                end = createPosition(endOffs);
            }
            catch (BadLocationException ble) {
                throw new RuntimeException(ble);
            }
        }
        
        @Override
        public int getStartOffset()
        {
            return start.getOffset();
        }
        
        @Override
        public int getEndOffset()
        {
            return end.getOffset();
        }
        
        public void setStartOffset(int offset)
        {
            try {
                start = createPosition(offset);
            }
            catch (BadLocationException ble) {
                throw new RuntimeException();
            }
        }

        public void setEndOffset(int offset)
        {
            try {
                end = createPosition(offset);
            }
            catch (BadLocationException ble) {
                throw new RuntimeException();
            }
        }
        
        @Override
        public int getElementCount()
        {
            return 0;
        }
        
        @Override
        public Element getElement(int index)
        {
            return null;
        }
        
        @Override
        public int getElementIndex(int offset)
        {
            return 0;
        }
        
        @Override
        public boolean isLeaf()
        {
            return true;
        }
        
        @SuppressWarnings("rawtypes")
        @Override
        public Enumeration children()
        {
            return new Vector().elements();
        }
        
        @Override
        public boolean getAllowsChildren()
        {
            return false;
        }
    }
    
}
