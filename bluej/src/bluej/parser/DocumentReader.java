/*
 This file is part of the BlueJ program. 
 Copyright (C) 2013  Michael Kolling and John Rosenberg 
 
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
package bluej.parser;

import java.io.Reader;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;

/**
 * An efficient reader which reads directly from the supplied Document.
 * 
 * @author Davin McCall
 */
public class DocumentReader extends Reader
{
    private Segment buffer;
    private Document document;
    private int bufpos;
    private int docPosition;
    private int docLength;
    
    /**
     * Construct a DocumentReader to read an entire document.
     */
    public DocumentReader(Document document)
    {
        this(document, 0);
    }
    
    /**
     * Construct a DocumentReader to read a document starting from the given position.
     */
    public DocumentReader(Document document, int position)
    {
        buffer = new Segment();
        buffer.setPartialReturn(true);
        this.document = document;
        docPosition = position;
        docLength = document.getLength();
        fillBuffer();
    }
    
    /**
     * Construct a new DocumentReader to read text between the two
     * given document positions.
     */
    public DocumentReader(Document document, int position, int endpos)
    {
        buffer = new Segment();
        buffer.setPartialReturn(true);
        this.document = document;
        docPosition = position;
        docLength = endpos;
        fillBuffer();
    }
    
    @Override
    public void close()
    {
        // Nothing to do
    }

    @Override
    public int read()
    {
        if (bufpos == buffer.getEndIndex()) {
            if (docPosition == docLength) {
                return -1;
            }
            fillBuffer();
        }
        
        return buffer.array[bufpos++];
    }
    
    @Override
    public int read(char[] cbuf, int off, int len)
    {
        int docAvail = Math.min(len, docLength - docPosition + buffer.getEndIndex() - bufpos);
        
        if (docAvail == 0) {
            return -1;
        }
        
        len = Math.min(len, docAvail);
        int remaining = len;
        
        while (remaining > 0) {
            int avail = Math.min(buffer.getEndIndex() - bufpos, remaining);
            if (avail == 0) {
                fillBuffer();
                avail = Math.min(buffer.getEndIndex() - bufpos, remaining);
            }
            System.arraycopy(buffer.array, bufpos, cbuf, off, avail);
            off += avail;
            bufpos += avail;
            remaining -= avail;
        }
        
        return len;
    }

    private void fillBuffer()
    {
        int docAvail = docLength - docPosition;
        try {
            document.getText(docPosition, docAvail, buffer);
            docPosition += (buffer.getEndIndex() - buffer.getBeginIndex());
            bufpos = buffer.getBeginIndex();
        }
        catch (BadLocationException e) {}
    }
}
