package bluej.stride.framedjava.ast.links;

import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.slots.UnderlineContainer;

/**
 * Created by neil on 29/06/2015.
 */
public class PossibleVarLink extends PossibleLink
{
    private final String varName;
    private final CodeElement usePoint;

    public PossibleVarLink(String varName, CodeElement usePoint, int startPosition, int endPosition, UnderlineContainer slot)
    {
        super(startPosition, endPosition, slot);
        this.varName = varName;
        this.usePoint = usePoint;
    }

    public CodeElement getUsePoint()
    {
        return usePoint;
    }

    public String getVarName()
    {
        return varName;
    }
}
