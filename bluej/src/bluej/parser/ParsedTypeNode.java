package bluej.parser;

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
