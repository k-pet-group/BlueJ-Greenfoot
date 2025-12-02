package bluej.parser.psi.visitor;

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LineColPos;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.psi.SourceInput;
import org.jetbrains.kotlin.com.intellij.psi.PsiElementVisitor;

public interface PsiVisitor {
    public void setTokenBase(LocatableToken currentToken);

    public void setTokenBase(LineColPos position);

    public LocatableToken getTokenBase();

    public void setEmitRangeStart(LocatableToken currentToken);

    public void setEmitRangeEnd(LocatableToken currentToken);

    default void setEmitRange(SourceInput.Range range) {
        range.start().ifPresent(lineColPos ->
            setEmitRangeStart(new LocatableToken(
                JavaTokenTypes.LITERAL_void,
                "",
                lineColPos,
                lineColPos
            ))
        );


        range.end().ifPresent(lineColPos ->
            setEmitRangeEnd(new LocatableToken(
                    JavaTokenTypes.LITERAL_void,
                    "",
                    lineColPos,
                    lineColPos
            ))
        );


    }

    public int getPsiStartOffset();

    public PsiElementVisitor asVisitor();
}
