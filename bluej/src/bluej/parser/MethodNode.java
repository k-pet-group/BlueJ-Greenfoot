package bluej.parser;

import bluej.parser.nodes.ParentParsedNode;
import bluej.parser.nodes.ParsedNode;

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
    
    @Override
    public int getNodeType()
    {
        return ParsedNode.NODETYPE_METHODDEF;
    }
}
