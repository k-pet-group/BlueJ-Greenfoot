package bluej.parser.psi.visitor;

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.psi.JavaParserCallbacksAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.psi.PsiComment;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.kotlin.psi.KtNamedFunction;

public class FunctionVisitor extends BaseVisitor {

    boolean topLevelFunction = false;

    /**
     * Creates a new method body visitor.
     *
     * @param callbacks The callback adapter for parser integration (must not be null)
     */
    public FunctionVisitor(@NotNull JavaParserCallbacksAdapter callbacks) {
        super(callbacks);
    }

    public FunctionVisitor(@NotNull JavaParserCallbacksAdapter callbacks, boolean topLevelFunction) {
        super(callbacks);
        this.topLevelFunction = topLevelFunction;
    }

    /**
     * Visits a method declaration (member function) within a class.
     *
     * <p><b>Context-Aware:</b> ClassVisitor KNOWS this is a class method, so it uses
     * {@code callbacks.endMethodDecl()} rather than {@code callbacks.endDecl()}.</p>
     *
     * <h3>Callback Sequence</h3>
     * <ol>
     *   <li>{@code gotDeclBegin(token)} - Begin method declaration</li>
     *   <li>{@code gotModifier(token)} × n - Process modifiers</li>
     *   <li>{@code gotTypeSpec(tokens)} - Return type specification</li>
     *   <li>{@code modifiersConsumed()} - Modifiers processed</li>
     *   <li>Type parameters (if generic)</li>
     *   <li>{@code gotMethodDeclaration(nameToken, javadocToken)} - Method name</li>
     *   <li>Parameter processing</li>
     *   <li>{@code gotAllMethodParameters()} - Parameters complete</li>
     *   <li>Body delegation to {@link MethodBodyVisitor}</li>
     *   <li>{@code endMethodDecl(token, hasBody)} - End method (CLASS CONTEXT)</li>
     * </ol>
     *
     * @param function The method declaration PSI element
     */
    @Override
    public void visitNamedFunction(@NotNull KtNamedFunction function) {
        String functionName = function.getName();

        // 1. Extract signature data using BaseVisitor helper
        BaseVisitor.FunctionSignatureResult signature = extractFunctionSignature(function);

        // 2. Begin declaration - use first modifier or fun keyword
        PsiElement declElement = function.getFirstChild();
        while (declElement instanceof PsiComment || declElement instanceof PsiWhiteSpace) {
            declElement = declElement.getNextSibling();
        }
        LocatableToken declToken = createToken(declElement);
        callbacks.gotDeclBegin(declToken);

        // 3. Process modifiers
        BaseVisitor.ModifierSet modifiers = signature.modifiers();
        for (BaseVisitor.ModifierToken modToken : modifiers.modifierTokens()) {
            callbacks.gotModifier(modToken.token());
        }

        // 4. Return type
        if (signature.returnTypeTokens() != null) {
            callbacks.gotTypeSpec(signature.returnTypeTokens());
        } else {
            callbacks.gotTypeSpec(null);
        }

        // 5. Modifiers consumed
//        callbacks.modifiersConsumed();

        LocatableToken endToken = null;
        boolean didStartMethod = false;
        boolean includeToken = false;

        if (functionName != null) {
            // 6. Generic type parameters (if present)
            BaseVisitor.TypeParametersResult typeParams = signature.typeParameters();
            if (typeParams != null && typeParams.hasTypeParameters()) {
                callbacks.gotMethodTypeParamsBegin();
                for (BaseVisitor.TypeParameterData tp : typeParams.typeParameters()) {
                    if (tp.nameToken() != null) {
                        callbacks.gotTypeParam(tp.nameToken());
                    }
                    if (tp.boundTokens() != null) {
                        callbacks.gotTypeParamBound(tp.boundTokens());
                    }
                }
                callbacks.endMethodTypeParams();
            }

            // 7. Parameters - using extracted data
            BaseVisitor.FunctionParametersResult params = signature.parameters();
            if (params.hasParameterList() && params.leftParenToken() != null) {
                callbacks.skipToToken(params.leftParenToken());

                if (signature.nameToken() != null) {
                    didStartMethod = startedFunctionDeclaration(signature.nameToken());
                }

                // Process each parameter
                for (BaseVisitor.ParameterData param : params.parameters()) {
                    if (param.firstToken() != null) {
                        callbacks.beginFormalParameter(param.firstToken());
                    }

                    if (param.colonToken() != null) {
                        callbacks.skipToToken(param.colonToken());
                    }

                    callbacks.gotTypeSpec(param.typeTokens());

                    if (param.nameToken() != null) {
                        callbacks.gotMethodParameter(param.nameToken(), param.varargToken());
                    }
                }

                if (params.rightParenToken() != null) {
                    callbacks.skipToToken(params.rightParenToken());
                }

                if (didStartMethod) {
                    callbacks.gotAllMethodParameters();
                }
            }

            endToken = params.lastToken();
        }

        callbacks.modifiersConsumed();

        // 8. Process method body
        if (endToken != null && endToken.getType() == JavaTokenTypes.RPAREN) {
            BaseVisitor.FunctionBodyResult body = extractFunctionBody(function);

            if (body.hasBody()) {
                if (body.isBlockBody() && body.lBraceToken() != null) {
                    callbacks.beginMethodBody(body.lBraceToken());

                    // Delegate to MethodBodyVisitor
                    if (body.bodyExpression() != null) {
                        MethodBodyVisitor bodyVisitor = new MethodBodyVisitor(callbacks);
                        body.bodyExpression().accept(bodyVisitor);
                    }

                    if (body.rBraceToken() != null) {
                        callbacks.endMethodBody(body.rBraceToken(), true);
                        endToken = body.rBraceToken();
                        includeToken = true;
                    } else if (body.lastToken() != null) {
                        callbacks.endMethodBody(body.lastToken(), true);
                        endToken = body.lastToken();
                        includeToken = true;
                    }
                } else if (body.isExpressionBody() && body.bodyExpression() != null) {
                    // Expression body: fun method() = expr
                    MethodBodyVisitor bodyVisitor = new MethodBodyVisitor(callbacks);
                    body.bodyExpression().accept(bodyVisitor);
                    endToken = body.lastToken();
                    includeToken = true;
                }
            } else {
                endToken = getTokenStream().LA(1);
                includeToken = false;
            }
        } else {
            // Incomplete method - use last known token
            LocatableToken lastToken = getLastToken();
            if (didStartMethod) {
                callbacks.endMethodDecl(lastToken, true);
            } else {
                callbacks.endDecl(lastToken);
            }
            return;
        }

        // 9. End method declaration - CLASS CONTEXT: use endMethodDecl
        if (endToken == null) {
            endToken = getLastToken();
        }

        if (didStartMethod) {
            endedFunctionDeclaration(endToken, includeToken);
        } else {
            callbacks.endDecl(endToken);
        }
    }

    boolean startedFunctionDeclaration(LocatableToken token) {
//        if (this.topLevelFunction) {
//            callbacks.endMethodDecl(endToken, includeToken);
//        } else {
//            callbacks.endElement(endToken, includeToken);
//        }
        callbacks.gotMethodDeclaration(token, null);

        return callbacks.isInEmitRange(token);
    }

    void endedFunctionDeclaration(LocatableToken endToken, boolean includeToken) {
        if (false && this.topLevelFunction) {
            callbacks.endElement(endToken, includeToken);
        } else {
            callbacks.endMethodDecl(endToken, includeToken);
        }
    }
}
