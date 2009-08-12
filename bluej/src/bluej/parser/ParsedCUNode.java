package bluej.parser;

import java.io.Reader;

import javax.swing.text.Document;


/**
 * A parsed compilation unit node.
 * 
 * @author davmac
 */
public class ParsedCUNode extends ParentParsedNode
{
	//private JavaTokenMarker marker = new JavaTokenMarker();
	private Document document;
	
	public ParsedCUNode(Document document)
    {
        this.document = document;
    }
	
	/**
	 * Overridden getSize() which returns the document size.
	 * 
	 * @see bluej.parser.ParsedNode#getSize()
	 */
	public int getSize()
	{
	    return document.getLength();
	}
	
	public int lineColToPosition(int line, int col)
	{
	    return document.getDefaultRootElement().getElement(line - 1).getStartOffset() + col - 1;
	}
	
    /**
     * Reparse this node from the specified offset.
     */
    protected void reparseNode(Document document, int nodePos, int offset)
    {
        doReparse(document, 0, offset);
    }
    
	protected void doReparse(Document document, int nodePos, int pos)
	{
	    getNodeTree().clear();
	    Reader r = new DocumentReader(document);
	    EditorParser parser = new EditorParser(r);
	    parser.parseCU(this);
	}
}
