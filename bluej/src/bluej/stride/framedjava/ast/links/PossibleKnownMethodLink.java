package bluej.stride.framedjava.ast.links;

import bluej.stride.framedjava.slots.UnderlineContainer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by neil on 29/06/2015.
 */
public class PossibleKnownMethodLink extends PossibleLink
{
    private final String qualClassName;
    private final String methodName;
    private final List<String> paramTypes;

    public PossibleKnownMethodLink(String qualClassName, String methodName, List<String> paramTypes, int startPosition, int endPosition, UnderlineContainer slot)
    {
        super(startPosition, endPosition, slot);
        this.methodName = methodName;
        this.paramTypes = paramTypes;
        this.qualClassName = qualClassName;
    }

    public String getQualClassName()
    {
        return qualClassName;
    }

    public String getURLMethodSuffix()
    {
        return encodeSuffix(methodName, paramTypes);
    }

    public static String encodeSuffix(String methodName, List<String> paramTypes)
    {
        return "#" + methodName + "-" + paramTypes.stream().map(PossibleKnownMethodLink::chopAtOpenAngle).collect(Collectors.joining("-")) + "-";
    }

    private static String chopAtOpenAngle(String s)
    {
        int i = s.indexOf('<');
        if (i < 0)
            return s;
        else
            return s.substring(0, i);
    }

    public String getDisplayName()
    {
        return methodName + "(" + paramTypes.stream().collect(Collectors.joining(", ")) + ")";
    }
}
