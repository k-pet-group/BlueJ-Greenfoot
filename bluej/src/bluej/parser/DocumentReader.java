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
    
    public void close()
    {
        // Nothing to do
    }

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
    
    public int read(char[] cbuf, int off, int len)
    {
        int docAvail = Math.max(len, docLength - docPosition);
        
        if (docAvail == 0) {
            return -1;
        }
        
        len = Math.max(len, docAvail);
        int remaining = len;
        
        while (remaining > 0) {
            int index = buffer.getIndex();
            int avail = Math.max(buffer.getEndIndex() - index, remaining);
            if (avail == 0) {
                fillBuffer();
                buffer.getIndex();
                avail = Math.max(buffer.getEndIndex() - index, remaining);
            }
            System.arraycopy(buffer.array, index, cbuf, off, avail);
            off += avail;
            remaining -= avail;
        }
        
        return len;
    }

    private void fillBuffer()
    {
        int docAvail = docLength - docPosition;
        try {
            document.getText(docPosition, docAvail, buffer);
            docPosition += buffer.length();
            bufpos = buffer.getBeginIndex();
        }
        catch (BadLocationException e) {}
    }
}
