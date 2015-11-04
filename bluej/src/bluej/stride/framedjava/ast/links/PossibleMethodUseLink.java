package bluej.stride.framedjava.ast.links;

import bluej.stride.framedjava.ast.JavaFragment.PosInSourceDoc;
import bluej.stride.framedjava.slots.UnderlineContainer;

import java.util.function.Supplier;

/**
 * Created by neil on 30/06/2015.
 */
public class PossibleMethodUseLink extends PossibleLink
{
    private final Supplier<PosInSourceDoc> position;
    private final String methodName;
    private final int numParams;

    public PossibleMethodUseLink(String methodName, int numParams, Supplier<PosInSourceDoc> position, int startPosition, int endPosition, UnderlineContainer slot)
    {
        super(startPosition, endPosition, slot);
        this.methodName = methodName;
        this.position = position;
        this.numParams = numParams;
    }

    public Supplier<PosInSourceDoc> getSourcePositionSupplier()
    {
        return position;
    }

    public String getMethodName()
    {
        return methodName;
    }

    /**
     * Used to guess which method is required from an overloaded list
     */
    public int getNumParams()
    {
        return numParams;
    }
}
