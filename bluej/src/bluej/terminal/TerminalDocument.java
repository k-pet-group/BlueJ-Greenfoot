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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.GapContent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Position;
import javax.swing.text.Segment;

import bluej.pkgmgr.Project;
import bluej.pkgmgr.Package;
import bluej.utility.JavaNames;

/**
 * Document implementation for the terminal editor pane.
 * 
 * <p>This is mainly necessary to override PlainDocument's slightly brain-damaged
 * implementation of the insertUpdate() method, which can clear line attributes
 * unexpectedly (insertUpdate method).
 * 
 * <p>It also allows highlighting of exception stack traces
 * 
 * @author Davin McCall
 */
public class TerminalDocument extends AbstractDocument
{
    Element root;
    private boolean highlightSourceLinks;
    private Project project;
    
    public TerminalDocument(Project project, boolean highlightSourceLinks)
    {
        super(new GapContent());
        root = createDefaultRoot();
        this.project = project;
        this.highlightSourceLinks = highlightSourceLinks;
    }
    
    /**
     * Mark a line as displaying method output.
     * 
     * @param line  The line number (0..N)
     */
    public void markLineAsMethodOutput(int line)
    {
        writeLock();
        
        Element el = root.getElement(line);
        MutableAttributeSet attr = (MutableAttributeSet) el.getAttributes();
        attr.addAttribute(TerminalView.METHOD_RECORD, Boolean.valueOf(true));
        
        writeUnlock();
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
        lines[0] = new LeafElement(map, null, 0, 1);;
        map.replace(0, 0, lines);
        return map;
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
            firstAffected.setEndOffset(offset);
            nextLine.setStartOffset(offset);
            firstAffected = nextLine;
            nextLine = (LeafElement) lineMap.getElement(lindex + 1);
        }
        
        ArrayList<LeafElement> added = new ArrayList<LeafElement>();
        
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                // line break!
                int origEnd = firstAffected.getEndOffset();
                firstAffected.setEndOffset(i + offset + 1);
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
        
        if (highlightSourceLinks)
            scanForStackTrace();

        super.insertUpdate(chng, attr);
    }

    /**
     * Looks through the contents of the terminal for lines
     * that look like they are part of a stack trace.
     */
    private void scanForStackTrace()
    {
        try {
            String content = getText(0, getLength());
            
            Pattern p = java.util.regex.Pattern.compile("at (\\S+)\\((\\S+)\\.java:(\\d+)\\)");
            // Matches things like:
            // at greenfoot.localdebugger.LocalDebugger$QueuedExecution.run(LocalDebugger.java:267)
            //    ^--------------------group 1----------------------------^ ^--group 2--^      ^3^
            Matcher m = p.matcher(content);
            while (m.find())
            {
                int elementIndex = getDefaultRootElement().getElementIndex(m.start());
                Element el = getDefaultRootElement().getElement(elementIndex);
                MutableAttributeSet attr = (MutableAttributeSet) el.getAttributes();
                
                String fullyQualifiedMethodName = m.group(1);
                String javaFile = m.group(2);
                int lineNumber = Integer.parseInt(m.group(3));
                
                // The fully qualified method name will end in ".method", so we can
                // definitely remove that:
                
                String fullyQualifiedClassName = JavaNames.getPrefix(fullyQualifiedMethodName);
                // The class name may be an inner class, so we want to take the package:
                String packageName = JavaNames.getPrefix(fullyQualifiedClassName);
                
                //Find out if that file is available, and only link if it is:                
                Package pkg = project.getPackage(packageName);
                
                if (pkg != null && pkg.getAllClassnames().contains(javaFile))
                {
                    attr.addAttribute(TerminalView.SOURCE_LOCATION, new ExceptionSourceLocation(m.start(1), m.end(), pkg, javaFile, lineNumber));
                }
                else
                {
                    attr.addAttribute(TerminalView.FOREIGN_STACK_TRACE, Boolean.valueOf(true));
                }
            }
            
            //Also mark up native method lines in stack traces with a marker for font colour:
            
            p = java.util.regex.Pattern.compile("at \\S+\\(Native Method|Unknown Source\\)");
            // Matches things like:
            //  at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
            m = p.matcher(content);
            while (m.find())
            {
                int elementIndex = getDefaultRootElement().getElementIndex(m.start());
                Element el = getDefaultRootElement().getElement(elementIndex);
                MutableAttributeSet attr = (MutableAttributeSet) el.getAttributes();
                
                attr.addAttribute(TerminalView.FOREIGN_STACK_TRACE, Boolean.valueOf(true));
            }
        }
        catch (BadLocationException e) {
            e.printStackTrace();
        }
        catch (NumberFormatException e ) {
            //In case it looks like an exception but has a large line number:
            e.printStackTrace();
        }
    }

    @Override
    protected void removeUpdate(DefaultDocumentEvent chng)
    {
        BranchElement lineMap = (BranchElement) getDefaultRootElement();
        int offset = chng.getOffset();
        int length = chng.getLength();
        
        int index = lineMap.getElementIndex(offset);
        
        LeafElement first = (LeafElement) lineMap.getElement(index);
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
        
        first.end = last.end;
        lineMap.replace(index, lastIndex - index, new Element[0]);
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
