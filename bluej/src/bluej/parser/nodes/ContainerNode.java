package bluej.parser.nodes;

/**
 * A basic container node implementation. A container node contains some sort of inner
 * scope.
 * 
 * @author Davin McCall
 */
public class ContainerNode extends ParentParsedNode
{
    private int nodeType;
    
    public ContainerNode(ParsedNode parent, int nodeType)
    {
        super(parent);
        this.nodeType = nodeType;
    }
    
    @Override
    public int getNodeType()
    {
        return nodeType;
    }
    
    @Override
    public boolean isContainer()
    {
        return true;
    }
}
