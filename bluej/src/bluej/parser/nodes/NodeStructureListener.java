package bluej.parser.nodes;

import bluej.parser.nodes.NodeTree.NodeAndPosition;

public interface NodeStructureListener
{
    public void nodeRemoved(NodeAndPosition node);
    
    public void nodeAdded(NodeAndPosition node);
    
    //
    // public void nodeChangedLength();
}
