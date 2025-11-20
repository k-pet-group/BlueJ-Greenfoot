package bluej.parser.psi.visitor;

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LineColPos;
import bluej.parser.lexer.LocatableToken;
import org.jetbrains.kotlin.com.intellij.psi.PsiElementVisitor;

public interface PsiVisitor {
    public void setTokenBase(LocatableToken currentToken);

    public void setTokenBase(LineColPos position);

    public LocatableToken getTokenBase();

    public void setEmitRangeStart(LocatableToken currentToken);

    public int getPsiStartOffset();

    public PsiElementVisitor asVisitor();
}
