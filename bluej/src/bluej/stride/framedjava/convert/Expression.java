package bluej.stride.framedjava.convert;

import java.util.List;
import java.util.stream.Collectors;

import bluej.stride.framedjava.ast.CallExpressionSlotFragment;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.OptionalExpressionSlotFragment;
import bluej.stride.framedjava.ast.SuperThisParamsExpressionFragment;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.CodeElement;
import static bluej.stride.framedjava.convert.JavaStrideParser.replaceInstanceof;
import static bluej.stride.framedjava.convert.JavaStrideParser.uniformSpacing;

/**
 * Created by neil on 03/06/2016.
 */
class Expression
{
    private final String stride;
    private final String java;

    private Expression(String stride, String java)
    {
        this.java = java;
        this.stride = stride;
    }
    
    Expression(String src)
    {
        this(replaceInstanceof(src), uniformSpacing(src));
    }

    Expression(List<Expression> expressions, String join)
    {
        this(expressions.stream().map(e -> e.stride).collect(Collectors.joining(join)),
            expressions.stream().map(e -> e.java).collect(Collectors.joining(join)));
    }

    FilledExpressionSlotFragment toFilled()
    {
        return new FilledExpressionSlotFragment(stride, java);
    }
    
    OptionalExpressionSlotFragment toOptional()
    {
        return new OptionalExpressionSlotFragment(stride, java);
    }

    SuperThisParamsExpressionFragment toSuperThis()
    {
        return new SuperThisParamsExpressionFragment(stride, java);
    }

    public CodeElement toStatement()
    {
        return new CallElement(null, new CallExpressionSlotFragment(stride, java), true);
    }
}
