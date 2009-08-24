package bluej.parser;

/**
 * A node representing a parsed method or constructor.
 * 
 * @author Davin McCall
 */
public class MethodNode extends ParentParsedNode
{
    public MethodNode(ParsedNode parent)
    {
        super(parent);
    }
    
    @Override
    public boolean isContainer()
    {
        return true;
    }
}
