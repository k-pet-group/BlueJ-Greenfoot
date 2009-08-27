package bluej.parser;

import bluej.parser.nodes.ParentParsedNode;
import bluej.parser.nodes.ParsedNode;

public class ParsedTypeNode extends ParentParsedNode
{
    public ParsedTypeNode(ParsedNode parent)
    {
        super(parent);
    }
    
    public boolean isContainer()
    {
        return true;
    }
}
