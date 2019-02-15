package bluej.parser.nodes;

import bluej.editor.moe.MoeSyntaxDocument;

import java.io.Reader;

public interface ReparseableDocument
{
    /**
     * Schedule a reparse at a certain point within the document.
     * @param pos    The position to reparse at
     * @param size   The reparse size. This is a minimum, rather than a maximum; that is,
     *               the reparse when it occurs must parse at least this much.
     */
    public void scheduleReparse(int pos, int size);

    public MoeSyntaxDocument.Element getDefaultRootElement();
    
    public int getLength();
    
    public Reader makeReader(int startPos, int endPos);

    /**
     * Mark a portion of the document as having been parsed. This removes any
     * scheduled re-parses as appropriate and repaints the appropriate area.
     */
    public void markSectionParsed(int pos, int size);
}
