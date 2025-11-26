/*
 This file is part of the BlueJ program. 
 Copyright (C) 2024  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser.psi.visitor;

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.lexer.LineColPos;
import bluej.parser.psi.JavaParserCallbacksAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;

import java.util.List;

/**
 * PSI visitor for traversing method and constructor bodies.
 * 
 * <p>This visitor is specialized for statement and expression parsing within executable contexts.
 * It handles method bodies, constructor bodies, init blocks, and creates nested visitors for lambda expressions.</p>
 * 
 * <h2>Delegation Pattern</h2>
 * <p>This visitor is instantiated by {@link FileVisitor} when entering method/constructor bodies.</p>
 * 
 * <h2>Key Responsibilities</h2>
 * <ul>
 *   <li><b>Statement Parsing:</b> if/else, loops, switch/when, try-catch, return, throw, break, continue</li>
 *   <li><b>Expression Parsing:</b> literals, operators, method calls, member access, array access</li>
 *   <li><b>Local Variables:</b> KtProperty in method context (val/var local variables)</li>
 *   <li><b>Lambda Bodies:</b> Creates nested MethodBodyVisitor for lambda traversal</li>
 * </ul>
 * 
 * <h2>Critical: KtProperty Context Handling</h2>
 * <p><b>In MethodBodyVisitor:</b> KtProperty represents LOCAL VARIABLES (not class fields).</p>
 * 
 * @see FileVisitor Parent visitor that delegates to this visitor
 */
public class MethodBodyVisitor extends BaseVisitor {
    
    /**
     * Creates a new method body visitor.
     * 
     * @param callbacks The callback adapter for parser integration (must not be null)
     */
    public MethodBodyVisitor(JavaParserCallbacksAdapter callbacks) {
        super(callbacks);
    }
    private List<LocatableToken> extractTypeTokens(KtTypeReference typeRef) {
        if (typeRef == null) {
            return List.of();
        }
        
        String typeText = typeRef.getText();
        if (typeText == null || typeText.isEmpty()) {
            return List.of();
        }
        
        var tokenType = switch (typeText) {
            case "Byte" -> JavaTokenTypes.LITERAL_byte;
            case "Short" -> JavaTokenTypes.LITERAL_short;
            case "Int" -> JavaTokenTypes.LITERAL_int;
            case "Long" -> JavaTokenTypes.LITERAL_long;
            case "Float" -> JavaTokenTypes.LITERAL_float;
            case "Double" -> JavaTokenTypes.LITERAL_double;
            case "Boolean" -> JavaTokenTypes.LITERAL_boolean;
            case "Char" -> JavaTokenTypes.LITERAL_char;
            default -> JavaTokenTypes.IDENT;
        };
        
        LocatableToken typeToken = createToken(typeRef, tokenType);
        return List.of(typeToken);
    }
    
    private PsiElement findChildByText(PsiElement parent, String text) {
        if (parent == null || text == null) {
            return null;
        }
        
        for (PsiElement child : parent.getChildren()) {
            if (text.equals(child.getText())) {
                return child;
            }
        }
        return null;
    }
    
    private PsiElement findArrowElement(KtWhenEntry entry) {
        for (PsiElement child : entry.getChildren()) {
            if (child.getText().equals("->")) {
                return child;
            }
        }
        return null;
    }
    
    // ==================== Property as Local Variable ====================
    
    /**
     * Visits a property declaration AS A LOCAL VARIABLE.
     *
     * <p><b>CRITICAL DIFFERENCE:</b> In MethodBodyVisitor context, KtProperty represents
     * local variables (val/var declarations in method bodies), NOT class-level fields.</p>
     *
     * <p>This contrasts with FileVisitor where the same visitProperty() method handles
     * class-level properties as fields.</p>
     */
    @Override
    public void visitProperty(@NotNull KtProperty property) {
        LocatableToken propertyToken = createToken( property.getValOrVarKeyword(), property.isVar() ? JavaTokenTypes.LITERAL_var : JavaTokenTypes.LITERAL_val);

        callbacks.gotDeclBegin(propertyToken);
        callbacks.beginVariableDecl(propertyToken);

//        LocatableToken typeToken = processPropertyType(property);
//
//        if (typeToken != null) {
//            List<LocatableToken> typeTokens = List.of(typeToken);
//            callbacks.gotTypeSpec(typeTokens);
//        }

        callbacks.gotTypeSpec(processPropertyType(property));

        PsiElement nameIdentifier = property.getNameIdentifier();
        LocatableToken nameToken = createToken(nameIdentifier, JavaTokenTypes.IDENT);
        boolean hasInitializer = property.hasInitializer();

        callbacks.gotVariableDecl(propertyToken, nameToken, hasInitializer);
//        callbacks.modifiersConsumed();

        if (hasInitializer) {
            KtExpression initializer = property.getInitializer();
            initializer.accept(this);
        }

//        LocatableToken propertyEndToken = createToken(property.getLastChild());
        LocatableToken propertyEndToken = getLastToken();

        callbacks.endVariable(propertyEndToken, true);
        callbacks.endVariableDecls(propertyEndToken, true);
        
//        // In method body context, KtProperty = local variable
//        // TODO: Implement local variable callback sequence
//        // For now, delegate to super to avoid breaking existing functionality
//        super.visitProperty(property);
    }
    
    // ==================== Statement Visitor Methods ====================
    
    /**
     * Visits a block expression ({ statements }).
     */
    @Override
    public void visitBlockExpression(@NotNull KtBlockExpression block) {
        if (block == null) {
            return;
        }
        
        boolean isMethodBody = block.getParent() instanceof KtFunction;
        
        PsiElement lBrace = block.getLBrace();
        if (lBrace != null && !isMethodBody) {
            LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
            callbacks.beginStmtblockBody(lBraceToken);
        }
        
        List<KtExpression> statements = block.getStatements();
        for (KtExpression statement : statements) {
            callbacks.beginElement(createToken(statement.getFirstChild()));
            statement.accept(this);
//            callbacks.endElement(createToken(statement.getLastChild()), true);
            callbacks.endElement(this.getLastToken(), true);
        }
        
        PsiElement rBrace = block.getRBrace();
        if (rBrace != null && !isMethodBody) {
            LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
            callbacks.endStmtblockBody(rBraceToken, true);
        }
    }
    
    /**
     * Visits an if expression (if/else statement or expression).
     */
    @Override
    public void visitIfExpression(@NotNull KtIfExpression ifExpr) {
        if (ifExpr == null) {
            return;
        }
        
        PsiElement ifKeyword = ifExpr.getIfKeyword();
        if (ifKeyword != null) {
            LocatableToken ifToken = createToken(ifKeyword, JavaTokenTypes.LITERAL_if);
            callbacks.beginIfStmt(ifToken);
        }
        
        KtExpression condition = ifExpr.getCondition();
        if (condition != null) {
            condition.accept(this);
        }
        
        KtExpression thenBranch = ifExpr.getThen();
        if (thenBranch != null) {
            LocatableToken thenToken = createToken(thenBranch);
            callbacks.beginIfCondBlock(thenToken);
            thenBranch.accept(this);
            callbacks.endIfCondBlock(thenToken, true);
        }
        
        KtExpression elseBranch = ifExpr.getElse();
        if (elseBranch != null) {
            PsiElement elseKeyword = ifExpr.getElseKeyword();
            
            if (elseBranch instanceof KtIfExpression) {
                if (elseKeyword != null) {
                    LocatableToken elseToken = createToken(elseKeyword, JavaTokenTypes.LITERAL_else);
                    callbacks.gotElseIf(elseToken);
                }
                elseBranch.accept(this);
            } else {
                LocatableToken elseToken = createToken(elseBranch);
                callbacks.beginIfCondBlock(elseToken);
                elseBranch.accept(this);
                callbacks.endIfCondBlock(elseToken, true);
            }
        }
        
        LocatableToken endToken = createToken(ifExpr.getLastChild());
        callbacks.endIfStmt(endToken, true);
    }
    
    /**
     * Visits a return expression.
     */
    @Override
    public void visitReturnExpression(@NotNull KtReturnExpression returnExpr) {
        if (returnExpr == null) {
            return;
        }
        
        KtExpression returnValue = returnExpr.getReturnedExpression();
        boolean hasValue = returnValue != null;
        
        callbacks.gotReturnStatement(hasValue);
        
        if (hasValue) {
            returnValue.accept(this);
        }
    }
    
    /**
     * Visits a throw expression.
     */
    @Override
    public void visitThrowExpression(@NotNull KtThrowExpression throwExpr) {
        if (throwExpr == null) {
            return;
        }
        
        PsiElement throwKeyword = throwExpr.getFirstChild();
        if (throwKeyword != null) {
            LocatableToken throwToken = createToken(throwKeyword, JavaTokenTypes.LITERAL_throw);
            callbacks.gotThrow(throwToken);
        }
        
        KtExpression thrownExpr = throwExpr.getThrownExpression();
        if (thrownExpr != null) {
            thrownExpr.accept(this);
        }
    }
    
    /**
     * Visits a break expression.
     */
    @Override
    public void visitBreakExpression(@NotNull KtBreakExpression breakExpr) {
        if (breakExpr == null) {
            return;
        }
        
        PsiElement breakKeyword = breakExpr.getFirstChild();
        LocatableToken keywordToken = null;
        if (breakKeyword != null) {
            keywordToken = createToken(breakKeyword, JavaTokenTypes.LITERAL_break);
        }
        
        LocatableToken labelToken = null;
        String labelName = breakExpr.getLabelName();
        if (labelName != null) {
            labelToken = createTokenWithText(breakExpr, labelName, JavaTokenTypes.IDENT);
        }
        
        callbacks.gotBreakContinue(keywordToken, labelToken);
    }
    
    /**
     * Visits a continue expression.
     */
    @Override
    public void visitContinueExpression(@NotNull KtContinueExpression continueExpr) {
        if (continueExpr == null) {
            return;
        }
        
        PsiElement continueKeyword = continueExpr.getFirstChild();
        LocatableToken keywordToken = null;
        if (continueKeyword != null) {
            keywordToken = createToken(continueKeyword, JavaTokenTypes.LITERAL_continue);
        }
        
        LocatableToken labelToken = null;
        String labelName = continueExpr.getLabelName();
        if (labelName != null) {
            labelToken = createTokenWithText(continueExpr, labelName, JavaTokenTypes.IDENT);
        }
        
        callbacks.gotBreakContinue(keywordToken, labelToken);
    }
    
    /**
     * Visits a for loop expression (for-in loop).
     */
    @Override
    public void visitForExpression(@NotNull KtForExpression forExpr) {
        if (forExpr == null) {
            return;
        }
        
        PsiElement forKeyword = forExpr.getFirstChild();
        LocatableToken forToken = createToken(forKeyword != null ? forKeyword : forExpr, JavaTokenTypes.LITERAL_for);
        callbacks.beginForLoop(forToken);
        
        KtParameter loopParam = forExpr.getLoopParameter();
        if (loopParam != null) {
            callbacks.beginForInitDecl(forToken);
            
            KtTypeReference paramType = loopParam.getTypeReference();
            if (paramType != null) {
                List<LocatableToken> typeTokens = extractTypeTokens(paramType);
                callbacks.gotTypeSpec(typeTokens);
            }
            
            PsiElement nameIdentifier = loopParam.getNameIdentifier();
            LocatableToken idToken;
            if (nameIdentifier != null) {
                idToken = createToken(nameIdentifier, JavaTokenTypes.IDENT);
                callbacks.gotForInit(forToken, idToken);
            } else {
                idToken = createToken(loopParam, JavaTokenTypes.IDENT);
            }
            callbacks.endForInit(idToken, true);
            
            callbacks.endForInitDecls(forToken, true);
            callbacks.modifiersConsumed();
        }
        
        callbacks.determinedForLoop(true, false);
        
        KtExpression body = forExpr.getBody();
//        if (body != null) {
//            LocatableToken openToken = createToken(body.getFirstChild());
//            callbacks.beginForLoopBody(openToken);
//
//            body.accept(this);
//
//            LocatableToken closeToken = createToken(body.getLastChild());
//            callbacks.endForLoopBody(closeToken, true);
//        }

        if (body != null) {
            KtBlockExpression bracedBody = (KtBlockExpression) body;
            // Extract separate opening and closing brace elements
            PsiElement lBrace = bracedBody.getLBrace();
            PsiElement rBrace = bracedBody.getRBrace();

            if (lBrace != null) {
                // Create separate tokens for opening and closing braces
                LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);

                callbacks.beginForLoopBody(lBraceToken);

                body.accept(this);

                if (rBrace != null) {
                    LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);

                    // 8. End type body with separate closing brace token
                    callbacks.endForLoopBody(rBraceToken, true);

//                    finalToken = rBraceToken;
                }
                else {
                    callbacks.endDecl(getLastToken());
                    return;
                }
            }
        }

        callbacks.endForLoop(this.getLastToken(), true);

//        LocatableToken endToken = createToken(forExpr.getLastChild());
//        callbacks.endForLoop(endToken, true);
    }
    
    /**
     * Visits a while loop expression.
     */
    @Override
    public void visitWhileExpression(@NotNull KtWhileExpression whileExpr) {
        if (whileExpr == null) {
            return;
        }
        
        PsiElement whileKeyword = whileExpr.getFirstChild();
        LocatableToken whileToken = createToken(whileKeyword != null ? whileKeyword : whileExpr, JavaTokenTypes.LITERAL_while);
        callbacks.beginWhileLoop(whileToken);
        
        KtExpression condition = whileExpr.getCondition();
        if (condition != null) {
            condition.accept(this);
        }
        
        KtExpression body = whileExpr.getBody();
        if (body != null) {
            LocatableToken openToken = createToken(body.getFirstChild());

            // NOTE: this actually behaves differently whether that is a brace or not
            callbacks.beginWhileLoopBody(openToken);

            body.accept(this);

            LocatableToken closeToken = createToken(body.getLastChild());

            callbacks.endWhileLoopBody(closeToken, true);
        }

        callbacks.endWhileLoop(this.getLastToken(), true);
        
//        LocatableToken endToken = createToken(whileExpr.getLastChild());
//        callbacks.endWhileLoop(endToken, true);
    }
    
    /**
     * Visits a do-while loop expression.
     */
    @Override
    public void visitDoWhileExpression(@NotNull KtDoWhileExpression doWhileExpr) {
        if (doWhileExpr == null) {
            return;
        }
        
        PsiElement doKeyword = doWhileExpr.getFirstChild();
        LocatableToken doToken = createToken(doKeyword != null ? doKeyword : doWhileExpr, JavaTokenTypes.LITERAL_do);
        callbacks.beginDoWhile(doToken);
        
        KtExpression body = doWhileExpr.getBody();
        if (body != null) {
            LocatableToken bodyToken = createToken(body);
            callbacks.beginDoWhileBody(bodyToken);
            body.accept(this);
            callbacks.endDoWhileBody(bodyToken, true);
        }
        
        KtExpression condition = doWhileExpr.getCondition();
        if (condition != null) {
            condition.accept(this);
        }
        
        LocatableToken endToken = createToken(doWhileExpr.getLastChild());
        callbacks.endDoWhile(endToken, true);
    }
    
    /**
     * Visits a when expression (Kotlin's switch).
     */
    @Override
    public void visitWhenExpression(@NotNull KtWhenExpression whenExpr) {
        if (whenExpr == null) {
            return;
        }
        
        PsiElement whenKeyword = whenExpr.getFirstChild();
        LocatableToken whenToken = createToken(whenKeyword != null ? whenKeyword : whenExpr, JavaTokenTypes.LITERAL_switch);
        callbacks.beginSwitchStmt(whenToken, false);
        
        KtExpression subject = whenExpr.getSubjectExpression();
        if (subject != null) {
            subject.accept(this);
        }
        
        LocatableToken lBraceToken = createToken(whenExpr, JavaTokenTypes.LCURLY);
        callbacks.beginSwitchBlock(lBraceToken);
        
        for (KtWhenEntry entry : whenExpr.getEntries()) {
            if (entry.isElse()) {
                callbacks.gotSwitchDefault();
            } else {
                LocatableToken entryToken = createToken(entry);
                callbacks.beginSwitchCase(entryToken);
                
                for (KtWhenCondition condition : entry.getConditions()) {
                    if (condition instanceof KtWhenConditionWithExpression) {
                        KtExpression condExpr = ((KtWhenConditionWithExpression) condition).getExpression();
                        if (condExpr != null) {
                            condExpr.accept(this);
                        }
                    } else if (condition instanceof KtWhenConditionInRange) {
                        KtExpression rangeExpr = ((KtWhenConditionInRange) condition).getRangeExpression();
                        if (rangeExpr != null) {
                            rangeExpr.accept(this);
                        }
                    } else if (condition instanceof KtWhenConditionIsPattern) {
                        KtTypeReference typeRef = ((KtWhenConditionIsPattern) condition).getTypeReference();
                        if (typeRef != null) {
                            List<LocatableToken> typeTokens = extractTypeTokens(typeRef);
                            callbacks.gotTypeSpec(typeTokens);
                        }
                    }
                }
                
                PsiElement arrow = findArrowElement(entry);
                LocatableToken arrowToken = createToken(arrow != null ? arrow : entry, JavaTokenTypes.LAMBDA);
                callbacks.gotSwitchCaseType(arrowToken, true);
            }
            
            KtExpression entryExpr = entry.getExpression();
            if (entryExpr != null) {
                entryExpr.accept(this);
            }
            
            if (!entry.isElse()) {
                LocatableToken endToken = createToken(entry.getLastChild());
                callbacks.endSwitchCase(endToken, true);
            }
        }
        
        LocatableToken rBraceToken = createToken(whenExpr.getLastChild(), JavaTokenTypes.RCURLY);
        callbacks.endSwitchBlock(rBraceToken);
        callbacks.endSwitchStmt(rBraceToken, true);
    }
    
    /**
     * Visits a try expression (try-catch-finally).
     */
    @Override
    public void visitTryExpression(@NotNull KtTryExpression tryExpr) {
        if (tryExpr == null) {
            return;
        }
        
        PsiElement tryKeyword = tryExpr.getTryKeyword();
        LocatableToken tryToken = createToken(tryKeyword != null ? tryKeyword : tryExpr, JavaTokenTypes.LITERAL_try);
        callbacks.beginTryCatchStmt(tryToken, false);
        
        KtBlockExpression tryBlock = tryExpr.getTryBlock();
        if (tryBlock != null) {
            PsiElement lBrace = tryBlock.getLBrace();
            if (lBrace != null) {
                LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
                callbacks.beginTryBlock(lBraceToken);
            }
            
            List<KtExpression> statements = tryBlock.getStatements();
            for (KtExpression statement : statements) {
                callbacks.beginElement(createToken(statement));
                statement.accept(this);
                callbacks.endElement(createToken(statement), true);
            }
            
            PsiElement rBrace = tryBlock.getRBrace();
            if (rBrace != null) {
                LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
                callbacks.endTryBlock(rBraceToken, true);
            }
        }
        
        for (KtCatchClause catchClause : tryExpr.getCatchClauses()) {
            visitCatchClause(catchClause);
        }
        
        KtFinallySection finallySection = tryExpr.getFinallyBlock();
        if (finallySection != null) {
            PsiElement finallyKeyword = findChildByText(finallySection, "finally");
            if (finallyKeyword != null) {
                LocatableToken finallyToken = createToken(finallyKeyword, JavaTokenTypes.LITERAL_finally);
                callbacks.gotCatchFinally(finallyToken);
            }
            
            KtBlockExpression finallyBlock = finallySection.getFinalExpression();
            if (finallyBlock != null) {
                finallyBlock.accept(this);
            }
        }
        
        LocatableToken endToken = createToken(tryExpr.getLastChild());
        callbacks.endTryCatchStmt(endToken, true);
    }
    
    private void visitCatchClause(KtCatchClause catchClause) {
        if (catchClause == null) {
            return;
        }
        
        PsiElement catchKeyword = findChildByText(catchClause, "catch");
        if (catchKeyword != null) {
            LocatableToken catchToken = createToken(catchKeyword, JavaTokenTypes.LITERAL_catch);
            callbacks.gotCatchFinally(catchToken);
        }
        
        KtParameter parameter = catchClause.getCatchParameter();
        if (parameter != null) {
            KtTypeReference typeRef = parameter.getTypeReference();
            if (typeRef != null) {
                List<LocatableToken> typeTokens = extractTypeTokens(typeRef);
                callbacks.gotTypeSpec(typeTokens);
            }
            
            PsiElement nameIdentifier = parameter.getNameIdentifier();
            if (nameIdentifier != null) {
                LocatableToken varToken = createToken(nameIdentifier, JavaTokenTypes.IDENT);
                callbacks.gotCatchVarName(varToken);
            }
        }
        
        KtExpression catchBody = catchClause.getCatchBody();
        if (catchBody != null) {
            if (catchBody instanceof KtBlockExpression) {
                KtBlockExpression block = (KtBlockExpression) catchBody;
                List<KtExpression> statements = block.getStatements();
                for (KtExpression statement : statements) {
                    callbacks.beginElement(createToken(statement));
                    statement.accept(this);
                    callbacks.endElement(createToken(statement), true);
                }
            } else {
                catchBody.accept(this);
            }
        }
    }
    
    // ==================== Expression Visitor Methods ====================
    
    /**
     * Visits a constant expression (literal).
     */
    @Override
    public void visitConstantExpression(@NotNull KtConstantExpression expr) {
        if (expr == null) {
            return;
        }
        
        LocatableToken token = createToken(expr);
        callbacks.beginExpression(token, false);
        callbacks.gotLiteral(token);
        callbacks.endExpression(token, false);
    }
    
    /**
     * Visits a simple name expression (identifier reference).
     */
    @Override
    public void visitSimpleNameExpression(@NotNull KtSimpleNameExpression expr) {
        if (expr == null) {
            return;
        }
        
        LocatableToken token = createToken(expr);
        callbacks.beginExpression(token, false);
        callbacks.gotIdentifier(token);
        callbacks.endExpression(token, false);
    }
    
    /**
     * Visits a binary expression (binary operator).
     */
    @Override
    public void visitBinaryExpression(@NotNull KtBinaryExpression expr) {
        if (expr == null) {
            return;
        }
        
        LocatableToken token = createToken(expr);
        callbacks.beginExpression(token, false);
        
        KtExpression left = expr.getLeft();
        if (left != null) {
            left.accept(this);
        }
        
        PsiElement operationRef = expr.getOperationReference();
        if (operationRef != null) {
            LocatableToken opToken = createToken(operationRef);
            callbacks.gotBinaryOperator(opToken);
        }
        
        KtExpression right = expr.getRight();
        if (right != null) {
            right.accept(this);
        }
        
        callbacks.endExpression(token, false);
    }
    
    /**
     * Visits a unary expression (prefix or postfix operator).
     */
    @Override
    public void visitUnaryExpression(@NotNull KtUnaryExpression expr) {
        if (expr == null) {
            return;
        }
        
        LocatableToken token = createToken(expr);
        callbacks.beginExpression(token, false);
        
        PsiElement operationRef = expr.getOperationReference();
        if (operationRef != null) {
            LocatableToken opToken = createToken(operationRef);
            
            if (expr instanceof KtPrefixExpression) {
                callbacks.gotUnaryOperator(opToken);
            } else if (expr instanceof KtPostfixExpression) {
                callbacks.gotPostOperator(opToken);
            }
        }
        
        KtExpression baseExpr = expr.getBaseExpression();
        if (baseExpr != null) {
            baseExpr.accept(this);
        }
        
        callbacks.endExpression(token, false);
    }
    
    @Override
    public void visitPrefixExpression(@NotNull KtPrefixExpression expr) {
        visitUnaryExpression(expr);
    }
    
    @Override
    public void visitPostfixExpression(@NotNull KtPostfixExpression expr) {
        visitUnaryExpression(expr);
    }
    
    /**
     * Visits a call expression (method or constructor call).
     */
    @Override
    public void visitCallExpression(@NotNull KtCallExpression expr) {
        if (expr == null) {
            return;
        }
        
        LocatableToken endToken = null;
        
        KtExpression calleeExpr = expr.getCalleeExpression();
        if (calleeExpr != null && !(calleeExpr instanceof KtDotQualifiedExpression)) {
            LocatableToken nameToken = createToken(calleeExpr);
            callbacks.beginExpression(nameToken, false);
            callbacks.gotMethodCall(nameToken);
            endToken = nameToken;
        }
        
        LocatableToken argBeginToken = null;
        LocatableToken argEndToken = null;
        
        KtValueArgumentList argList = expr.getValueArgumentList();
        List<KtLambdaArgument> lambdaArgs = expr.getLambdaArguments();
        if (argList != null) {
            PsiElement lParen = argList.getLeftParenthesis();
            if (lParen != null) {
                LocatableToken lParenToken = createToken(lParen, JavaTokenTypes.LPAREN);
                callbacks.beginArgumentList(lParenToken);
                argBeginToken = lParenToken;
            }
            
            for (KtValueArgument arg : argList.getArguments()) {
                KtExpression argExpr = arg.getArgumentExpression();
                if (argExpr != null) {
                    argExpr.accept(this);
                }
            }
            
            PsiElement rParen = argList.getRightParenthesis();
            if (rParen != null) {
                LocatableToken rParenToken = createToken(rParen, JavaTokenTypes.RPAREN);
                endToken = rParenToken;
                argEndToken = rParenToken;
            }
        }
        
        if (!lambdaArgs.isEmpty()) {
            KtLambdaArgument arg = lambdaArgs.getFirst();
            KtLambdaExpression blockLambda = arg.getLambdaExpression();
            
            if (argBeginToken == null) {
                PsiElement lBrace = blockLambda.getLeftCurlyBrace().getPsi();
                LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LBRACK);
                callbacks.beginArgumentList(lBraceToken);
            }
            
            blockLambda.accept(this);
            
            PsiElement rBrace = blockLambda.getRightCurlyBrace().getPsi();
            LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RBRACK);
            
            endToken = rBraceToken;
            argEndToken = rBraceToken;
        }
        
        if (argEndToken != null) {
            callbacks.endArgumentList(argEndToken);
        }
        
        if (endToken != null) {
            callbacks.endExpression(endToken, false);
        }
    }
    
    /**
     * Visits a qualified expression (member access).
     */
    @Override
    public void visitQualifiedExpression(@NotNull KtQualifiedExpression expr) {
        if (expr == null) {
            return;
        }
        
        LocatableToken token = createToken(expr.getFirstChild());
        callbacks.beginExpression(token, false);
        
        KtExpression receiver = expr.getReceiverExpression();
        if (receiver != null) {
            receiver.accept(this);
        }
        
        PsiElement operationSign = expr.getOperationTokenNode().getPsi();
        if (operationSign != null) {
            KtExpression selector = expr.getSelectorExpression();

            if (selector instanceof KtCallExpression call) {
                LocatableToken methodName = createToken(call.getCalleeExpression(), JavaTokenTypes.IDENT);

                // TODO: second parameter is generic type arguments, if any
                callbacks.gotMemberCall(methodName, List.of());

                selector.accept(this);
            }
            else {
                LocatableToken fieldName = createToken(selector, JavaTokenTypes.IDENT);

                callbacks.gotMemberAccess(fieldName);
            }
        }
//        if (operationSign != null) {
//            // TODO: is that dot fucking right here?
//            LocatableToken memberToken = createToken(operationSign, JavaTokenTypes.DOT);
//            callbacks.gotMemberAccess(memberToken);
//        }
//
//        KtExpression selector = expr.getSelectorExpression();
//        if (selector != null) {
//            selector.accept(this);
//        }
        
        callbacks.endExpression(getLastToken(), false);
    }
    
    @Override
    public void visitDotQualifiedExpression(@NotNull KtDotQualifiedExpression expr) {
        visitQualifiedExpression(expr);
    }
    
    @Override
    public void visitSafeQualifiedExpression(@NotNull KtSafeQualifiedExpression expr) {
        visitQualifiedExpression(expr);
    }
    
    /**
     * Visits an array access expression (subscript operator).
     */
    @Override
    public void visitArrayAccessExpression(@NotNull KtArrayAccessExpression expr) {
        if (expr == null) {
            return;
        }
        
        LocatableToken token = createToken(expr);
        callbacks.beginExpression(token, false);
        
        KtExpression arrayExpr = expr.getArrayExpression();
        if (arrayExpr != null) {
            arrayExpr.accept(this);
        }
        
        for (KtExpression indexExpr : expr.getIndexExpressions()) {
            indexExpr.accept(this);
        }
        
        callbacks.gotArrayElementAccess();
        callbacks.endExpression(token, false);
    }
    
    /**
     * Visits a lambda expression - creates nested MethodBodyVisitor for lambda body.
     */
    @Override
    public void visitLambdaExpression(@NotNull KtLambdaExpression expr) {
        if (expr == null) {
            return;
        }
        
        LocatableToken token = createToken(expr);
        
        KtFunctionLiteral literal = expr.getFunctionLiteral();
        if (literal != null) {
            PsiElement lBrace = literal.getLBrace();
            LocatableToken lBraceToken = lBrace != null ? createToken(lBrace, JavaTokenTypes.LCURLY) : token;
            callbacks.beginExpression(lBraceToken, true);
            callbacks.beginLambdaBody(true, lBraceToken);
            
            KtParameterList paramList = literal.getValueParameterList();
            if (paramList != null) {
                for (KtParameter param : paramList.getParameters()) {
                    callbacks.gotLambdaFormalParam();
                    
                    PsiElement nameId = param.getNameIdentifier();
                    if (nameId != null) {
                        LocatableToken nameToken = createToken(nameId, JavaTokenTypes.IDENT);
                        callbacks.gotLambdaFormalName(nameToken);
                    }
                    
                    KtTypeReference typeRef = param.getTypeReference();
                    if (typeRef != null) {
                        List<LocatableToken> typeTokens = extractTypeTokens(typeRef);
                        callbacks.gotLambdaFormalType(typeTokens);
                    }
                }
            }
            
            // Create nested MethodBodyVisitor for lambda body
            KtBlockExpression bodyBlock = literal.getBodyExpression();
            if (bodyBlock != null) {
                for (KtExpression statement : bodyBlock.getStatements()) {
                    statement.accept(this);
                }
            }
            
            PsiElement rBrace = literal.getRBrace();
            LocatableToken rBraceToken = rBrace != null ? createToken(rBrace, JavaTokenTypes.RCURLY) : token;
            callbacks.endLambdaBody(rBraceToken);
            callbacks.endExpression(rBraceToken, false);
        }
    }
    
    /**
     * Visits a string template expression.
     */
    @Override
    public void visitStringTemplateExpression(@NotNull KtStringTemplateExpression expr) {
        if (expr == null) {
            return;
        }
        
        LocatableToken token = createToken(expr);
        callbacks.beginExpression(token, false);
        callbacks.gotLiteral(token);
        callbacks.endExpression(token, false);
    }
    
    /**
     * Visits a this expression.
     */
    @Override
    public void visitThisExpression(@NotNull KtThisExpression expr) {
        if (expr == null) {
            return;
        }
        
        LocatableToken token = createToken(expr);
        callbacks.beginExpression(token, false);
        callbacks.gotLiteral(token);
        callbacks.endExpression(token, false);
    }
    
    /**
     * Visits a super expression.
     */
    @Override
    public void visitSuperExpression(@NotNull KtSuperExpression expr) {
        if (expr == null) {
            return;
        }
        
        LocatableToken token = createToken(expr);
        callbacks.beginExpression(token, false);
        callbacks.gotLiteral(token);
        callbacks.endExpression(token, false);
    }
    
    /**
     * Visits a binary expression with type RHS (type operations: as, is).
     */
    @Override
    public void visitBinaryWithTypeRHSExpression(@NotNull KtBinaryExpressionWithTypeRHS expr) {
        if (expr == null) {
            return;
        }
        
        LocatableToken token = createToken(expr);
        callbacks.beginExpression(token, false);
        
        KtExpression left = expr.getLeft();
        if (left != null) {
            left.accept(this);
        }
        
        PsiElement operationRef = expr.getOperationReference();
        KtTypeReference typeRef = expr.getRight();
        
        if (operationRef != null && typeRef != null) {
            String opText = operationRef.getText();
            List<LocatableToken> typeTokens = extractTypeTokens(typeRef);
            
            if ("as".equals(opText) || "as?".equals(opText)) {
                callbacks.gotTypeCast(typeTokens);
            } else if ("is".equals(opText) || "!is".equals(opText)) {
                LocatableToken opToken = createToken(operationRef, JavaTokenTypes.LITERAL_instanceof);
                callbacks.gotInstanceOfOperator(opToken);
                callbacks.gotTypeSpec(typeTokens);
            }
        }
        
        callbacks.endExpression(token, false);
    }
    
    /**
     * Visits an object literal expression (anonymous object).
     */
    @Override
    public void visitObjectLiteralExpression(@NotNull KtObjectLiteralExpression expr) {
        if (expr == null) {
            return;
        }
        
        KtObjectDeclaration objDecl = expr.getObjectDeclaration();
        if (objDecl != null) {
            KtClassBody body = objDecl.getBody();
            if (body != null) {
                PsiElement lBrace = body.getLBrace();
                if (lBrace != null) {
                    LocatableToken lBraceToken = createToken(lBrace, JavaTokenTypes.LCURLY);
                    callbacks.beginAnonClassBody(lBraceToken, false);
                }
                
                for (KtDeclaration decl : body.getDeclarations()) {
                    decl.accept(this);
                }
                
                PsiElement rBrace = body.getRBrace();
                if (rBrace != null) {
                    LocatableToken rBraceToken = createToken(rBrace, JavaTokenTypes.RCURLY);
                    callbacks.endAnonClassBody(rBraceToken, true);
                }
            }
        }
    }
    
    /**
     * Visits a parenthesized expression.
     */
    @Override
    public void visitParenthesizedExpression(@NotNull KtParenthesizedExpression expr) {
        if (expr == null) {
            return;
        }
        
        KtExpression innerExpr = expr.getExpression();
        if (innerExpr != null) {
            innerExpr.accept(this);
        }
    }
}