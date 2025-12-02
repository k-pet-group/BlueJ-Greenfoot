package bluej.parser.psi;

import bluej.parser.SourceParser;
import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.LocatableToken;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

/**
 * High-performance adapter for JavaParserCallbacks using typed method handles.
 * This adapter provides near-native performance by pre-binding method handles at construction time,
 * avoiding reflection overhead during parsing operations.
 */
public final class KotlinParserCallbacksAdapterImpl extends JavaParserCallbacksAdapterImpl implements JavaParserCallbacksAdapter {
    // Emit range filtering - callbacks only triggered if position >= start
    private int emitRangeStartLine = -1;
    private int emitRangeStartColumn = -1;
    private int emitRangeEndLine = 1;
    private int emitRangeEndColumn = 1;

    public KotlinParserCallbacksAdapterImpl(SourceParser target) {
        super(target);
    }

    // Package and imports
    @Override
    public void beginPackageStatement(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginPackageStatement(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotPackage(List<LocatableToken> pkgTokens) {
        try {
            if (isInEmitRange(pkgTokens)) {
                super.gotPackage(pkgTokens);
            }
            skipToLastToken(pkgTokens);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotPackageSemi(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotPackageSemi(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotImportStmtSemi(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotImportStmtSemi(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken) {
        try {
            if (isInEmitRange(semiColonToken)) {
                super.gotImport(tokens, isStatic, importToken, semiColonToken);
            }
            skipToToken(semiColonToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken) {
        try {
            if (isInEmitRange(semiColonToken)) {
                super.gotWildcardImport(tokens, isStatic, importToken, semiColonToken);
            }
            skipToToken(semiColonToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Modifiers and elements
    @Override
    public void gotModifier(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotModifier(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void modifiersConsumed() {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.modifiersConsumed();
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginElement(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginElement(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endElement(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endElement(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Method and constructor declarations
    @Override
    public void beginMethodBody(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginMethodBody(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endMethodBody(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endMethodBody(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endMethodDecl(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endMethodDecl(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken) {
        try {
            if (isInEmitRange(token)) {
                super.gotConstructorDecl(token, hiddenToken);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken, String name) {
        try {
            if (isInEmitRange(token)) {
                super.gotConstructorDecl(token, hiddenToken, name);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotMethodDeclaration(LocatableToken token, LocatableToken hiddenToken) {
        try {
            if (isInEmitRange(token)) {
                super.gotMethodDeclaration(token, hiddenToken);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotMethodParameter(LocatableToken token, LocatableToken ellipsisToken) {
        try {
            if (isInEmitRange(ellipsisToken != null ? ellipsisToken : token)) {
                super.gotMethodParameter(token, ellipsisToken);
            }
            skipToToken(ellipsisToken != null ? ellipsisToken : token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotArrayDeclarator() {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.gotArrayDeclarator();
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotAllMethodParameters() {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.gotAllMethodParameters();
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotMethodTypeParamsBegin() {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.gotMethodTypeParamsBegin();
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endMethodTypeParams() {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.endMethodTypeParams();
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginThrows(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginThrows(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endThrows() {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.endThrows();
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Compilation unit states
    @Override
    public void reachedCUstate(int i) {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.reachedCUstate(i);
            }
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void finishedCU(int state) {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.finishedCU(state);
            }
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Control flow - for loops
    @Override
    public void beginForLoop(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginForLoop(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginForLoopBody(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginForLoopBody(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endForLoopBody(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endForLoopBody(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endForLoop(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endForLoop(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotForTest(boolean isPresent) {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.gotForTest(isPresent);
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotForIncrement(boolean isPresent) {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.gotForIncrement(isPresent);
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void determinedForLoop(boolean forEachLoop, boolean initExpressionFollows) {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.determinedForLoop(forEachLoop, initExpressionFollows);
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Control flow - while loops
    @Override
    public void beginWhileLoop(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginWhileLoop(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginWhileLoopBody(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginWhileLoopBody(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endWhileLoopBody(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endWhileLoopBody(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endWhileLoop(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endWhileLoop(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Control flow - if statements
    @Override
    public void beginIfStmt(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginIfStmt(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginIfCondBlock(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginIfCondBlock(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endIfCondBlock(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endIfCondBlock(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotElseIf(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotElseIf(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endIfStmt(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endIfStmt(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Control flow - switch statements
    @Override
    public void beginSwitchStmt(LocatableToken token, boolean isSwitchExpression) {
        try {
            if (isInEmitRange(token)) {
                super.beginSwitchStmt(token, isSwitchExpression);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginSwitchBlock(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginSwitchBlock(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endSwitchBlock(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.endSwitchBlock(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endSwitchStmt(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endSwitchStmt(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginSwitchCase(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginSwitchCase(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotSwitchCaseType(LocatableToken token, boolean isArrowSyntax) {
        try {
            if (isInEmitRange(token)) {
                super.gotSwitchCaseType(token, isArrowSyntax);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endSwitchCase(LocatableToken token, boolean wasArrowSyntax) {
        try {
            if (isInEmitRange(token)) {
                super.endSwitchCase(token, wasArrowSyntax);
            }
            skipToToken(token, wasArrowSyntax);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotSwitchDefault() {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.gotSwitchDefault();
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Control flow - do-while loops
    @Override
    public void beginDoWhile(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginDoWhile(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginDoWhileBody(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginDoWhileBody(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endDoWhileBody(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endDoWhileBody(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endDoWhile(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endDoWhile(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Exception handling
    @Override
    public void beginTryCatchStmt(LocatableToken token, boolean hasResource) {
        try {
            if (isInEmitRange(token)) {
                super.beginTryCatchStmt(token, hasResource);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginTryBlock(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginTryBlock(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endTryBlock(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endTryBlock(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endTryCatchStmt(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endTryCatchStmt(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotCatchFinally(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotCatchFinally(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotMultiCatch(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotMultiCatch(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotCatchVarName(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotCatchVarName(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Synchronized blocks
    @Override
    public void beginSynchronizedBlock(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginSynchronizedBlock(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endSynchronizedBlock(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endSynchronizedBlock(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Arguments
    @Override
    public void beginArgumentList(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginArgumentList(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endArgument() {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.endArgument();
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endArgumentList(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.endArgumentList(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Expressions
    @Override
    public void gotExprNew(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotExprNew(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endExprNew(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endExprNew(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginArrayInitList(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginArrayInitList(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endArrayInitList(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.endArrayInitList(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginExpression(LocatableToken token, boolean isLambdaBody) {
        try {
            if (isInEmitRange(token)) {
                super.beginExpression(token, isLambdaBody);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endExpression(LocatableToken token, boolean emptyExpression) {
        try {
            if (isInEmitRange(token)) {
                super.endExpression(token, emptyExpression);
            }
            skipToToken(token, emptyExpression);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotLiteral(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotLiteral(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotPrimitiveTypeLiteral(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotPrimitiveTypeLiteral(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotIdentifier(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotIdentifier(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotIdentifierEOF(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotIdentifierEOF(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotMemberAccessEOF(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotMemberAccessEOF(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotCompoundIdent(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotCompoundIdent(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotCompoundComponent(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotCompoundComponent(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void completeCompoundValue(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.completeCompoundValue(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void completeCompoundValueEOF(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.completeCompoundValueEOF(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void completeCompoundClass(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.completeCompoundClass(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotMemberAccess(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotMemberAccess(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs) {
        try {
            if (isInEmitRange(token)) {
                super.gotMemberCall(token, typeArgs);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotMethodCall(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotMethodCall(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotConstructorCall(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotConstructorCall(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotDotEOF(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotDotEOF(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotStatementExpression() {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.gotStatementExpression();
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotClassLiteral(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotClassLiteral(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotBinaryOperator(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotBinaryOperator(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotUnaryOperator(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotUnaryOperator(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotQuestionOperator(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotQuestionOperator(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotQuestionColon(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotQuestionColon(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotInstanceOfOperator(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotInstanceOfOperator(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotInstanceOfVar(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotInstanceOfVar(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotArrayElementAccess() {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.gotArrayElementAccess();
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotPostOperator(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotPostOperator(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Anonymous class bodies
    @Override
    public void beginAnonClassBody(LocatableToken token, boolean isEnumMember) {
        try {
            if (isInEmitRange(token)) {
                super.beginAnonClassBody(token, isEnumMember);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endAnonClassBody(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endAnonClassBody(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Statement blocks
    @Override
    public void beginStmtblockBody(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginStmtblockBody(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endStmtblockBody(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endStmtblockBody(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Initializer blocks
    @Override
    public void beginInitBlock(LocatableToken first, LocatableToken lcurly) {
        try {
            if (isInEmitRange(lcurly)) {
                super.beginInitBlock(first, lcurly);
            }
            skipToToken(lcurly);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endInitBlock(LocatableToken rcurly, boolean included) {
        try {
            if (isInEmitRange(rcurly)) {
                super.endInitBlock(rcurly, included);
            }
            skipToToken(rcurly, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Type definitions
    @Override
    public void beginTypeBody(LocatableToken leftCurlyToken) {
        try {
            if (isInEmitRange(leftCurlyToken)) {
                super.beginTypeBody(leftCurlyToken);
            }
            skipToToken(leftCurlyToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endTypeBody(LocatableToken endCurlyToken, boolean included) {
        try {
            if (isInEmitRange(endCurlyToken)) {
                super.endTypeBody(endCurlyToken, included);
            }
            skipToToken(endCurlyToken, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotDeclBegin(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotDeclBegin(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endDecl(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.endDecl(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotTypeDef(LocatableToken firstToken, int tdType) {
        try {
            if (isInEmitRange(firstToken)) {
                super.gotTypeDef(firstToken, tdType);
            }
            skipToToken(firstToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotTypeDefName(LocatableToken nameToken) {
        try {
            if (isInEmitRange(nameToken)) {
                super.gotTypeDefName(nameToken);
            }
            skipToToken(nameToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginTypeDefExtends(LocatableToken extendsToken) {
        try {
            if (isInEmitRange(extendsToken)) {
                super.beginTypeDefExtends(extendsToken);
            }
            skipToToken(extendsToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endTypeDefExtends() {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.endTypeDefExtends();
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginTypeDefImplements(LocatableToken implementsToken) {
        try {
            if (isInEmitRange(implementsToken)) {
                super.beginTypeDefImplements(implementsToken);
            }
            skipToToken(implementsToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endTypeDefImplements() {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.endTypeDefImplements();
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginTypeDefPermits(LocatableToken permitsToken) {
        try {
            if (isInEmitRange(permitsToken)) {
                super.beginTypeDefPermits(permitsToken);
            }
            skipToToken(permitsToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endTypeDefPermits() {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.endTypeDefPermits();
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotTypeDefEnd(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.gotTypeDefEnd(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotInnerType(LocatableToken start) {
        try {
            if (isInEmitRange(start)) {
                super.gotInnerType(start);
            }
            skipToToken(start);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotTopLevelDecl(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotTopLevelDecl(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Variable declarations
    @Override
    public void beginVariableDecl(LocatableToken first) {
        try {
            if (isInEmitRange(first)) {
                super.beginVariableDecl(first);
            }
            skipToToken(first);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotVariableDecl(LocatableToken first, LocatableToken idToken, boolean inited) {
        try {
            if (isInEmitRange(idToken)) {
                super.gotVariableDecl(first, idToken, inited);
            }
            skipToToken(idToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotSubsequentVar(LocatableToken first, LocatableToken idToken, boolean inited) {
        try {
            if (isInEmitRange(idToken)) {
                super.gotSubsequentVar(first, idToken, inited);
            }
            skipToToken(idToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endVariable(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endVariable(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endVariableDecls(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endVariableDecls(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginForInitDecl(LocatableToken first) {
        try {
            if (isInEmitRange(first)) {
                super.beginForInitDecl(first);
            }
            skipToToken(first);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotForInit(LocatableToken first, LocatableToken idToken) {
        try {
            if (isInEmitRange(idToken)) {
                super.gotForInit(first, idToken);
            }
            skipToToken(idToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotSubsequentForInit(LocatableToken first, LocatableToken idToken, boolean initFollows) {
        try {
            if (isInEmitRange(idToken)) {
                super.gotSubsequentForInit(first, idToken, initFollows);
            }
            skipToToken(idToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endForInit(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endForInit(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

//    @Override
//    public void endForInitDecl(LocatableToken token, boolean included) {
//        try {
//            if (isInEmitRange(token)) {
//                super.endForInitDecl(token, included);
//            }
//            skipToToken(token, included);
//        } catch (Throwable t) {
//            throw sneakyThrow(t);
//        }
//    }

    @Override
    public void endForInitDecls(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endForInitDecls(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Field declarations
    @Override
    public void beginFieldDeclarations(LocatableToken first) {
        try {
            if (isInEmitRange(first)) {
                super.beginFieldDeclarations(first);
            }
            skipToToken(first);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotField(LocatableToken first, LocatableToken idToken, boolean initExpressionFollows) {
        try {
            if (isInEmitRange(idToken)) {
                super.gotField(first, idToken, initExpressionFollows);
            }
            skipToToken(idToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotSubsequentField(LocatableToken first, LocatableToken idToken, boolean initFollows) {
        try {
            if (isInEmitRange(idToken)) {
                super.gotSubsequentField(first, idToken, initFollows);
            }
            skipToToken(idToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endField(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endField(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endFieldDeclarations(LocatableToken token, boolean included) {
        try {
            if (isInEmitRange(token)) {
                super.endFieldDeclarations(token, included);
            }
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Type specifications
    @Override
    public void gotTypeSpec(List<LocatableToken> tokens) {
        try {
            if (isInEmitRange(tokens)) {
                super.gotTypeSpec(tokens);
            }
            skipToLastToken(tokens);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotTypeCast(List<LocatableToken> tokens) {
        try {
            if (isInEmitRange(tokens)) {
                super.gotTypeCast(tokens);
            }
            skipToLastToken(tokens);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotNewArrayDeclarator(boolean withDimension) {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.gotNewArrayDeclarator(withDimension);
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotTypeParam(LocatableToken idToken) {
        try {
            if (isInEmitRange(idToken)) {
                super.gotTypeParam(idToken);
            }
            skipToToken(idToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotTypeParamBound(List<LocatableToken> tokens) {
        try {
            if (isInEmitRange(tokens)) {
                super.gotTypeParamBound(tokens);
            }
            skipToLastToken(tokens);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Statements
    @Override
    public void gotThrow(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotThrow(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotBreakContinue(LocatableToken keywordToken, LocatableToken labelToken) {
        try {
            if (isInEmitRange(labelToken != null ? labelToken : keywordToken)) {
                super.gotBreakContinue(keywordToken, labelToken);
            }
            skipToToken(labelToken != null ? labelToken : keywordToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotReturnStatement(boolean hasValue) {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.gotReturnStatement(hasValue);
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotYieldStatement() {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.gotYieldStatement();
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotEmptyStatement() {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.gotEmptyStatement();
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotAssert() {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.gotAssert();
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Annotations
    @Override
    public void gotAnnotation(List<LocatableToken> annName, boolean paramsFollow) {
        try {
            if (isInEmitRange(annName)) {
                super.gotAnnotation(annName, paramsFollow);
            }
            skipToLastToken(annName);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Lambda expressions
    @Override
    public void beginLambdaBody(boolean lambdaIsBlock, LocatableToken openCurly) {
        try {
            if (isInEmitRange(openCurly)) {
                super.beginLambdaBody(lambdaIsBlock, openCurly);
            }
            skipToToken(openCurly);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endLambdaBody(LocatableToken closeCurly) {
        try {
            if (isInEmitRange(closeCurly)) {
                super.endLambdaBody(closeCurly);
            }
            skipToToken(closeCurly);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotLambdaFormalParam() {
        try {
            if (isInEmitRange((LocatableToken)null)) {
                super.gotLambdaFormalParam();
            }
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotLambdaFormalName(LocatableToken name) {
        try {
            if (isInEmitRange(name)) {
                super.gotLambdaFormalName(name);
            }
            skipToToken(name);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotLambdaFormalType(List<LocatableToken> type) {
        try {
            if (isInEmitRange(type)) {
                super.gotLambdaFormalType(type);
            }
            skipToLastToken(type);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Formal parameters
    @Override
    public void beginFormalParameter(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.beginFormalParameter(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotArrayTypeIdentifier(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotArrayTypeIdentifier(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotParentIdentifier(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotParentIdentifier(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Record declarations
    @Override
    public void beginRecordParameters(LocatableToken parenToken) {
        try {
            if (isInEmitRange(parenToken)) {
                super.beginRecordParameters(parenToken);
            }
            skipToToken(parenToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotRecordParameter(LocatableToken first, LocatableToken idToken, LocatableToken varargsToken) {
        try {
            if (isInEmitRange(varargsToken != null ? varargsToken : idToken)) {
                super.gotRecordParameter(first, idToken, varargsToken);
            }
            skipToToken(varargsToken != null ? varargsToken : idToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endRecordParameters(LocatableToken closeParen) {
        try {
            if (isInEmitRange(closeParen)) {
                super.endRecordParameters(closeParen);
            }
            skipToToken(closeParen);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Comments
    @Override
    public void gotComment(LocatableToken token) {
        try {
            if (isInEmitRange(token)) {
                super.gotComment(token);
            }
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Error handling
    @Override
    public void error(String msg, int beginLine, int beginCol, int endLine, int endCol) {
        try {
            if (isInEmitRange(beginLine, beginCol)) {
                super.error(msg, beginLine, beginCol, endLine, endCol);
            }
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    /**
     * Converts checked exceptions to unchecked ones without wrapping.
     * Preserves RuntimeException and Error as-is, wraps checked exceptions.
     */
    private static RuntimeException sneakyThrow(Throwable t) {
        if (t instanceof RuntimeException re) return re;
        if (t instanceof Error e) throw e;
        throw new RuntimeException(t);
    }

    // ***

    @Override
    public void setEmitRangeStart(int line, int column) {
        this.emitRangeStartLine = line;
        this.emitRangeStartColumn = column;
    }

    @Override
    public void clearEmitRangeStart() {
        this.emitRangeStartLine = -1;
        this.emitRangeStartColumn = -1;
    }

    @Override
    public void setEmitRangeEnd(int line, int column) {
        this.emitRangeEndLine = -line;
        this.emitRangeEndColumn = -column;
    }

    @Override
    public void clearEmitRangeEnd() {
        this.emitRangeEndLine = 1;
        this.emitRangeEndColumn = 1;
    }

    @Override
    public JavaTokenFilter getTokenStream() {
        return this.target.getTokenStream();
    }

    @Override
    public LocatableToken getLastToken() {
        return this.target.getLastToken();
    }

    @Override
    public void setLastToken(LocatableToken token) {
        this.target.setLastToken(token);
    }

    @Override
    public boolean isInEmitRange(int line, int column) {
        if (emitRangeStartLine > 0) {
            if (line < emitRangeStartLine || (line <= emitRangeStartLine && column < emitRangeStartColumn)) {
                return false;
            }
        }

        // end range is non-inclusive
        if (emitRangeEndLine < 0) {
            if (-line < emitRangeEndLine || (-line <= emitRangeEndLine && -column <= emitRangeEndColumn)) {
                return false;
            }
        }

        return true;
    }
}
