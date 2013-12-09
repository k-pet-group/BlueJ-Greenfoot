/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2013  Michael Kolling and John Rosenberg 
 
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

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;

import javax.swing.JEditorPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import bluej.pkgmgr.Project;
import bluej.utility.Debug;

/**
 * A customised text area for use in the BlueJ text terminal.
 *
 * @author  Michael Kolling
 */
public final class TermTextArea extends JEditorPane implements MouseMotionListener, MouseListener
{
    private static final int BUFFER_LINES = 48;

    private boolean unlimitedBuffer = false;

    private InputBuffer buffer;
    private Terminal terminal;
    
    private int preferredRows;
    private int preferredColumns;

    /**
     * Create a new text area with given size.
     */
    public TermTextArea(int rows, int columns, InputBuffer buffer, final Project proj, Terminal terminal, final boolean isStderr)
    {
        preferredRows = rows;
        preferredColumns = columns;
        resetPreferredSize();
        this.buffer = buffer;
        this.terminal = terminal;
        setEditorKit(new DefaultEditorKit() {
            
            @Override
            public Document createDefaultDocument()
            {
                return new TerminalDocument(proj, isStderr /* Highlight stack traces if stderr */);
            }
            
            @Override
            public ViewFactory getViewFactory()
            {
                return new ViewFactory() {
                    @Override
                    public View create(Element elem)
                    {
                        return new TerminalView(elem, isStderr);
                    }
                };
            } 
        });
        if (isStderr) /* Only need mouse listener if we're highlighting stack traces */
        {
            addMouseMotionListener(this);
            addMouseListener(this);
        }
        
        setForeground(TerminalView.getDefaultColor(isStderr));
    }
    
    @Override
    public void setFont(Font font)
    {
        super.setFont(font);
        resetPreferredSize();
    }
    
    /**
     * Reset the preferred size according to font metrics and desired rows/columns
     */
    private void resetPreferredSize()
    {
        FontMetrics metrics = getFontMetrics(getFont());
        int mwidth = metrics.charWidth('m');
        int mheight = metrics.getHeight();
        setPreferredSize(new Dimension(mwidth * preferredColumns, mheight * preferredRows));
    }

    public void setUnlimitedBuffering(boolean arg)
    {
        unlimitedBuffer = arg;
    }

    /**
     * Append some text to the terminal text area.
     */
    public void append(String s)
    {
        append(s, null);
    }
    
    /**
     * Append some text to the terminal text area.
     */
    public void append(String s, AttributeSet attribs)
    {
        int length = getDocument().getLength();
        try {
            getDocument().insertString(length, s, attribs);
        }
        catch (BadLocationException ble) {
            throw new RuntimeException(ble);
        }

        if(!unlimitedBuffer) {             // possibly remove top line
            int lines = getLineCount();
            if(lines > BUFFER_LINES) {
                int linePos = getLineStartOffset(lines-BUFFER_LINES);
                replaceRange(null, 0, linePos);
            }
        }
    }
    
    /**
     * Append a line of text from a method call recording.
     */
    public void appendMethodCall(String s)
    {
        int length = getDocument().getLength();
        int lineCount = getLineCount();
        if (length != getLineStartOffset(lineCount - 1)) {
            // Start a new line for the method call details
            append("\n");
            lineCount++;
        }
        
        append(s, null);
        
        TerminalDocument doc = (TerminalDocument) getDocument();
        doc.markLineAsMethodOutput(lineCount - 1);
    }
    
    /**
     * Get the number of lines in the text area
     */
    public int getLineCount()
    {
        return getDocument().getDefaultRootElement().getElementCount();
    }
    
    /**
     * Get the start offset of a particular line (line 0 is the first line).
     */
    public int getLineStartOffset(int line)
    {
        return getDocument().getDefaultRootElement().getElement(line).getStartOffset();
    }
    
    /**
     * Replace the text in some portion of the document with the given string.
     */
    public void replaceRange(String s, int startPos, int endPos)
    {
        try {
            getDocument().remove(startPos, endPos - startPos);
            if (s != null) {
                getDocument().insertString(startPos, s, null);
            }
        }
        catch (BadLocationException ble) {
            throw new RuntimeException(ble);
        }
    }

    /*
     * Overrides the default method to stop it append to the JTextArea straight
     * away, instead we add the resultant string to our pasteBuffer for use
     * elsewhere
     * @see Terminal.keyTyped(KeyEvent event)
     */
    @Override
    public void paste()
    {
        if (! terminal.checkActive()) {
            return;
        }
        
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            String result = null;
            try {
                result = (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException ex) {
                Debug.message(ex.getMessage());
            } catch (IOException ex) {
                Debug.message(ex.getMessage());
            }
            
            if (result != null) {
                for (char ch : result.toCharArray()) {
                    if (buffer.putChar(ch)) {
                        terminal.writeToTerminal(String.valueOf(ch));
                    }
                }
            }
        } else {
            // if it isn't a string, let the usual paste method handle it.
            super.paste();
        }
    }
    
    @Override
    public void mouseMoved(MouseEvent e)
    {
        TermTextArea editor = TermTextArea.this;
        Point pt = new Point(e.getX(), e.getY());
        int pos = editor.getUI().viewToModel(editor, pt);
        
        Document doc = getDocument();
        int elementIndex = doc.getDefaultRootElement().getElementIndex(pos);
        Element el = doc.getDefaultRootElement().getElement(elementIndex);
        
        Cursor cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        
        AttributeSet attrs = el.getAttributes();
        if (attrs != null && attrs.getAttribute(TerminalView.SOURCE_LOCATION) != null) {
            ExceptionSourceLocation sel = (ExceptionSourceLocation) attrs.getAttribute(TerminalView.SOURCE_LOCATION);
            if (pos >= sel.getStart() && pos < sel.getEnd())
            {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            }
        }
        
        editor.setCursor(cursor);
    }
    
    @Override
    public void mouseClicked(MouseEvent e)
    {
        TermTextArea editor = TermTextArea.this;
        Point pt = new Point(e.getX(), e.getY());
        int pos = editor.getUI().viewToModel(editor, pt);
        
        Document doc = getDocument();
        int elementIndex = doc.getDefaultRootElement().getElementIndex(pos);
        Element el = doc.getDefaultRootElement().getElement(elementIndex);
        
        AttributeSet attrs = el.getAttributes();
        if (attrs != null && attrs.getAttribute(TerminalView.SOURCE_LOCATION) != null) {
            ExceptionSourceLocation sel = (ExceptionSourceLocation) attrs.getAttribute(TerminalView.SOURCE_LOCATION);
            if (pos >= sel.getStart() && pos < sel.getEnd())
            {
                sel.showInEditor();
            }
        }
    }
    
    // Un-needed stubs:
    @Override public void mouseDragged(MouseEvent e) { }
    @Override public void mousePressed(MouseEvent e) { }
    @Override public void mouseReleased(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }
}
