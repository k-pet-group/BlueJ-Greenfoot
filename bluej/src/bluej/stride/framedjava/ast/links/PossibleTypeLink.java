package bluej.stride.framedjava.ast.links;

import bluej.stride.framedjava.slots.UnderlineContainer;

/**
 * Created by neil on 29/06/2015.
 */
public class PossibleTypeLink extends PossibleLink
{
    private final String typeName;

    public PossibleTypeLink(String typeName, int startPosition, int endPosition, UnderlineContainer slot)
    {
        super(startPosition, endPosition, slot);
        this.typeName = typeName;
    }

    public String getTypeName()
    {
        return typeName;
    }
}
