package bluej.parser.nodes;

public class ContainerNode extends ParentParsedNode
{
    public ContainerNode(ParsedNode parent)
    {
        super(parent);
    }
    
    @Override
    public boolean isContainer()
    {
        return true;
    }
}
