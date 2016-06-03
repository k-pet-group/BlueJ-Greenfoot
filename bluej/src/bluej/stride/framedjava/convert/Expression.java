package bluej.stride.framedjava.convert;

import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import bluej.parser.lexer.JavaLexer;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.stride.framedjava.ast.CallExpressionSlotFragment;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.OptionalExpressionSlotFragment;
import bluej.stride.framedjava.ast.SuperThisParamsExpressionFragment;
import bluej.stride.framedjava.elements.AssignElement;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.CodeElement;

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
        this(uniformSpacing(src, true), uniformSpacing(src, false));
    }

    Expression(List<Expression> expressions, String join)
    {
        this(expressions.stream().map(e -> e.stride).collect(Collectors.joining(join)),
            expressions.stream().map(e -> e.java).collect(Collectors.joining(join)));
    }

    static String uniformSpacing(String src, boolean replaceInstanceof)
    {
        // It is a bit inefficient to re-lex the string, but
        // it's easiest this way and conversion is not particularly time sensitive:
        JavaLexer lexer = new JavaLexer(new StringReader(src));
        StringBuilder r = new StringBuilder();
        while (true)
        {
            LocatableToken token = lexer.nextToken();
            if (token.getType() == JavaTokenTypes.EOF)
                return r.toString();
            if (r.length() != 0)
                r.append(" ");
            if (replaceInstanceof && token.getType() == JavaTokenTypes.LITERAL_instanceof)
                r.append("<:");
            else
                r.append(token.getText());
        }
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
        if (java.startsWith("++ ") || java.startsWith("-- "))
            return new AssignElement(null, new FilledExpressionSlotFragment(stride.substring(3), java.substring(3)), new FilledExpressionSlotFragment(stride.substring(3) + " " + stride.substring(0, 1) + " 1", java.substring(3) + " " + java.substring(0, 1) + " 1"), true);
        else if (java.endsWith(" ++") || java.endsWith(" --"))
        {
            String choppedStride = stride.substring(0, stride.length() - 3);
            String choppedJava = java.substring(0, java.length() - 3);
            return new AssignElement(null, new FilledExpressionSlotFragment(choppedStride, choppedJava), new FilledExpressionSlotFragment(choppedStride + " " + stride.substring(stride.length() - 1, stride.length()) + " 1", choppedJava + " " + java.substring(java.length() - 1, java.length()) + " 1"), true);
        }
        else
            return new CallElement(null, new CallExpressionSlotFragment(stride, java), true);
    }
}
