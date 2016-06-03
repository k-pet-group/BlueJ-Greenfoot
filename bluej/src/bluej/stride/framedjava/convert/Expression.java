package bluej.stride.framedjava.convert;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import bluej.parser.lexer.JavaLexer;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.stride.framedjava.ast.CallExpressionSlotFragment;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.OptionalExpressionSlotFragment;
import bluej.stride.framedjava.ast.SuperThisParamsExpressionFragment;
import bluej.stride.framedjava.convert.ConversionWarning.UnsupportedFeature;
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
    private final List<Integer> incDec;
    private final Consumer<ConversionWarning> addWarning;

    Expression(String src, List<Integer> incDec, Consumer<ConversionWarning> addWarning)
    {
        this.stride = uniformSpacing(src, true);
        this.java = uniformSpacing(src, false);
        this.incDec = new ArrayList<>(incDec);
        this.addWarning = addWarning;
    }

    Expression(List<Expression> expressions, String join, Consumer<ConversionWarning> addWarning)
    {
        this.stride = expressions.stream().map(e -> e.stride).collect(Collectors.joining(join));
        this.java = expressions.stream().map(e -> e.java).collect(Collectors.joining(join));
        this.incDec = new ArrayList<>();
        expressions.forEach(e -> this.incDec.addAll(e.incDec));
        this.addWarning = addWarning;
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
        warnIncDec();
        return new FilledExpressionSlotFragment(stride, java);
    }
    
    OptionalExpressionSlotFragment toOptional()
    {
        warnIncDec();
        return new OptionalExpressionSlotFragment(stride, java);
    }

    SuperThisParamsExpressionFragment toSuperThis()
    {
        warnIncDec();
        return new SuperThisParamsExpressionFragment(stride, java);
    }

    public CodeElement toStatement()
    {
        boolean startInc = java.startsWith("++ ");
        boolean startDec = java.startsWith("-- ");
        boolean endInc = java.endsWith(" ++");
        boolean endDec = java.endsWith(" --");
        if (startInc || startDec)
        {
            incDec.remove((Integer)(startInc ? JavaTokenTypes.INC : JavaTokenTypes.DEC));
            warnIncDec();
            return new AssignElement(null, new FilledExpressionSlotFragment(stride.substring(3), java.substring(3)), new FilledExpressionSlotFragment(stride.substring(3) + " " + stride.substring(0, 1) + " 1", java.substring(3) + " " + java.substring(0, 1) + " 1"), true);
        }
        else if (endInc || endDec)
        {
            incDec.remove((Integer)(endInc ? JavaTokenTypes.INC : JavaTokenTypes.DEC));
            warnIncDec();
            String choppedStride = stride.substring(0, stride.length() - 3);
            String choppedJava = java.substring(0, java.length() - 3);
            return new AssignElement(null, new FilledExpressionSlotFragment(choppedStride, choppedJava), new FilledExpressionSlotFragment(choppedStride + " " + stride.substring(stride.length() - 1, stride.length()) + " 1", choppedJava + " " + java.substring(java.length() - 1, java.length()) + " 1"), true);
        }
        else
        {
            warnIncDec();
            return new CallElement(null, new CallExpressionSlotFragment(stride, java), true);
        }
    }

    private void warnIncDec()
    {
        if (!incDec.isEmpty())
            addWarning.accept(new UnsupportedFeature("++/-- in expression"));
    }

    @Override
    public String toString()
    {
        // If we need to display to the user, display the original Java:
        return java;
    }
}
