package bluej.parser.psi;

import bluej.parser.lexer.LocatableToken;

import java.util.List;

public interface JavaParserCallbacks {
    /**
     * Found a package X; statement
     *
     * @param token The "package" token
     */
    void beginPackageStatement(LocatableToken token);

    /**
     * We have the package name for this source, from a package statement at the top of the file.
     *
     * @param pkgTokens The tokens making up the package name (including the dots)
     */
    void gotPackage(List<LocatableToken> pkgTokens);

    /**
     * We've seen the semicolon at the end of a "package" statement.
     *
     * @param token The semicolon token
     */
    void gotPackageSemi(LocatableToken token);

    /**
     * Saw a modifier (public,private etc)
     */
    void gotModifier(LocatableToken token);

    /**
     * Modifiers were consumed. This is called after the entity to which the modifiers apply
     * has been identified (eg gotTypeDef() called)
     */
    void modifiersConsumed();

    /**
     * Beginning of some arbitrary grammatical element
     */
    void beginElement(LocatableToken token);

    /**
     * End of some arbitrary grammatical element.
     *
     * @param token    The end token
     * @param included True if the end token is part of the element; false if it is part of the next element.
     */
    void endElement(LocatableToken token, boolean included);

    /**
     * Got the beginning (opening brace) of a method or constructor body.
     */
    void beginMethodBody(LocatableToken token);

    /**
     * End of a method or constructor body reached.
     */
    void endMethodBody(LocatableToken token, boolean included);

    /**
     * End of a method or constructor declaration
     */
    void endMethodDecl(LocatableToken token, boolean included);

    /**
     * Reached a compilation unit state.
     * State 1 = package statement parsed. State 2 = one or more type definitions parsed
     */
    void reachedCUstate(int i);

    /**
     * Finished parsing a compilation unit.
     *
     * @param state Our last state: see reachedCUState for details
     */
    void finishedCU(int state);

    /**
     * We've seen the semicolon at the end of an "import" statement
     */
    void gotImportStmtSemi(LocatableToken token);

    void beginForLoop(LocatableToken token);

    void beginForLoopBody(LocatableToken token);

    void endForLoopBody(LocatableToken token, boolean included);

    void endForLoop(LocatableToken token, boolean included);

    void beginWhileLoop(LocatableToken token);

    void beginWhileLoopBody(LocatableToken token);

    void endWhileLoopBody(LocatableToken token, boolean included);

    void endWhileLoop(LocatableToken token, boolean included);

    void beginIfStmt(LocatableToken token);

    /**
     * Begin an "if" conditional block (the part that is executed conditionally)
     */
    void beginIfCondBlock(LocatableToken token);

    void endIfCondBlock(LocatableToken token, boolean included);

    void gotElseIf(LocatableToken token);

    void endIfStmt(LocatableToken token, boolean included);

    void beginSwitchStmt(LocatableToken token, boolean isSwitchExpression);

    void beginSwitchBlock(LocatableToken token);

    void endSwitchBlock(LocatableToken token);

    void endSwitchStmt(LocatableToken token, boolean included);

    void beginDoWhile(LocatableToken token);

    void beginDoWhileBody(LocatableToken token);

    void endDoWhileBody(LocatableToken token, boolean included);

    void endDoWhile(LocatableToken token, boolean included);

    void beginTryCatchStmt(LocatableToken token, boolean hasResource);

    void beginTryBlock(LocatableToken token);

    void endTryBlock(LocatableToken token, boolean included);

    void endTryCatchStmt(LocatableToken token, boolean included);

    void beginSynchronizedBlock(LocatableToken token);

    void endSynchronizedBlock(LocatableToken token, boolean included);

    /**
     * A list of a parameters to a method or constructor
     */
    void beginArgumentList(LocatableToken token);

    /**
     * An individual argument has ended
     */
    void endArgument();

    /**
     * The end of the argument list has been reached.
     */
    void endArgumentList(LocatableToken token);

    /**
     * got a "new ..." expression. Will be followed by a type spec (gotTypeSpec())
     * and possibly by array size declarations, then endExprNew()
     */
    void gotExprNew(LocatableToken token);

    void endExprNew(LocatableToken token, boolean included);

    void beginArrayInitList(LocatableToken token);

    void endArrayInitList(LocatableToken token);

    /**
     * An anonymous class body. Preceded by a type spec (see gotTypeSpec()) except in the case of an enum member body.
     */
    void beginAnonClassBody(LocatableToken token, boolean isEnumMember);

    void endAnonClassBody(LocatableToken token, boolean included);

    /**
     * Beginning of a statement block. This includes anonymous statement blocks, and static
     * initializer blocks
     */
    void beginStmtblockBody(LocatableToken token);

    void endStmtblockBody(LocatableToken token, boolean included);

    /**
     * Begin a (possibly static) initialisation block.
     *
     * @param first  The first token (should be either "static" or the "{")
     * @param lcurly The "{" token which opens the block body
     */
    void beginInitBlock(LocatableToken first, LocatableToken lcurly);

    /**
     * End of a (possibly static) initialisation block
     *
     * @param rcurly   The last token (should be "}")
     * @param included True if the last token is actually a "}"
     */
    void endInitBlock(LocatableToken rcurly, boolean included);

    /**
     * Begin the type definition body.
     */
    void beginTypeBody(LocatableToken leftCurlyToken);

    /**
     * End of type definition body. This should be a '}' unless an error occurred
     */
    void endTypeBody(LocatableToken endCurlyToken, boolean included);

    /**
     * Got the beginning of a declaration - either a type, a field/variable, or a
     * method constructor, or an initialisation block. This will be followed by one of:
     *
     * <ul>
     * <li>gotTypeDef(...) - if a type definition
     * <li>gotMethodDeclaration(...) - if a method declaration
     * <li>gotConstructorDecl(...) - if a constructor declaration
     * <li>beginInitBlock(...) - if an initialiser block
     * <li>beginFieldDeclarations(...) - if a field declaration
     * <li>beginVariableDecl(...) - if a variable declaration
     * <li>endDecl(...) - if not a valid declaration
     * </ul>
     */
    void gotDeclBegin(LocatableToken token);

    /**
     * End a declaration (unsuccessfully).
     */
    void endDecl(LocatableToken token);

    /**
     * Called when the current element is recognised as a type definition.
     *
     * @param tdType one of TYPEDEF_CLASS, _INTERFACE, _ANNOTATION or _ENUM
     */
    void gotTypeDef(LocatableToken firstToken, int tdType);

    /**
     * Called when we have the identifier token for a class/interface/enum definition
     */
    void gotTypeDefName(LocatableToken nameToken);

    /**
     * Called when we have seen the "extends" literal token
     */
    void beginTypeDefExtends(LocatableToken extendsToken);

    /**
     * Called after we have seen the last type in an "extends" type list
     */
    void endTypeDefExtends();

    /**
     * Called when we have seen the "implements" literal token
     */
    void beginTypeDefImplements(LocatableToken implementsToken);

    /**
     * Called after we have seen the last type in an "implements" type list
     */
    void endTypeDefImplements();

    /**
     * Called when we have seen the "permits" literal token
     */
    void beginTypeDefPermits(LocatableToken permitsToken);

    /**
     * Called after we have seen the last type in a "permits" type list
     */
    void endTypeDefPermits();

    void gotTypeDefEnd(LocatableToken token, boolean included);

    /**
     * Got a variable declaration, which might declare multiple variables. Each
     * variable will generate gotVariable() or gotSubsequentVar().
     *
     * @param first The first token in the declaration
     */
    void beginVariableDecl(LocatableToken first);

    /**
     * Got the (first) variable in a variable declaration.
     *
     * @param first   The first token in the declaration
     * @param idToken The token with the variable identifier
     * @param inited  Whether the variable is initialized as part of the declaration
     */
    void gotVariableDecl(LocatableToken first, LocatableToken idToken, boolean inited);

    void gotSubsequentVar(LocatableToken first, LocatableToken idToken, boolean inited);

    void endVariable(LocatableToken token, boolean included);

    void endVariableDecls(LocatableToken token, boolean included);

    void beginForInitDecl(LocatableToken first);

    void gotForInit(LocatableToken first, LocatableToken idToken);

    void gotSubsequentForInit(LocatableToken first, LocatableToken idToken, boolean initFollows);

    void endForInit(LocatableToken token, boolean included);

//    void endForInitDecl(LocatableToken token, boolean included);

    void endForInitDecls(LocatableToken token, boolean included);

    /**
     * Got a field declaration, which might declare multiple fields. Each field will generate
     * gotField() or gotSubsequentField().
     *
     * @param first The first token in the declaration
     *
     */
    void beginFieldDeclarations(LocatableToken first);

    /**
     * Got a field (inside a type definition).
     *
     * @param first                 The first token that forms part of the field declaration
     * @param idToken               The token with the name of the field.
     * @param initExpressionFollows
     */
    void gotField(LocatableToken first, LocatableToken idToken, boolean initExpressionFollows);

    void gotSubsequentField(LocatableToken first, LocatableToken idToken, boolean initFollows);

    /**
     * End a single field declaration (but not necessarily the field declaration statement)
     */
    void endField(LocatableToken token, boolean included);

    /**
     * End a field declaration statement
     */
    void endFieldDeclarations(LocatableToken token, boolean included);

    /**
     * We've seen a type specification or something that looks a lot like one.
     */
    void gotTypeSpec(List<LocatableToken> tokens);

    /**
     * Seen a type cast operator. The tokens list contains the type to which is cast.
     */
    void gotTypeCast(List<LocatableToken> tokens);

    /**
     * Saw the beginning of an expression
     */
    void beginExpression(LocatableToken token, boolean isLambdaBody);

    /**
     * Reached the end of an expression. The given token is the first one past the end.
     */
    void endExpression(LocatableToken token, boolean emptyExpression);

    /**
     * Saw a literal as part of an expression
     */
    void gotLiteral(LocatableToken token);

    /**
     * Saw a primitive type literal in an expression; usually occurs as "int.class"
     * or "int[].class" for example.
     *
     * @param token The primitive token
     */
    void gotPrimitiveTypeLiteral(LocatableToken token);

    /**
     * Saw an identifier as (part of) an expression
     */
    void gotIdentifier(LocatableToken token);

    /**
     * Got an identifier (possibly part of a compound identifier) immediately followed by
     * end of input stream.
     */
    void gotIdentifierEOF(LocatableToken token);

    void gotMemberAccessEOF(LocatableToken token);

    void gotCompoundIdent(LocatableToken token);

    void gotCompoundComponent(LocatableToken token);

    void completeCompoundValue(LocatableToken token);

    void completeCompoundValueEOF(LocatableToken token);

    void completeCompoundClass(LocatableToken token);

    void gotMemberAccess(LocatableToken token);

    /**
     * Saw a member method call (expr.methodName()), token is the method name; arguments to follow
     */
    void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs);

    /**
     * Saw a "naked" method call - "methodName(...)"
     */
    void gotMethodCall(LocatableToken token);

    /**
     * Saw a call to the constructor as this(...) or super(...)
     */
    void gotConstructorCall(LocatableToken token);

    /**
     * Saw a dot operator followed by end-of-file
     */
    void gotDotEOF(LocatableToken token);

    void gotStatementExpression();

    void gotClassLiteral(LocatableToken token);

    /**
     * Saw a binary operator as part of an expression
     */
    void gotBinaryOperator(LocatableToken token);

    void gotUnaryOperator(LocatableToken token);

    /**
     * Saw a "?" operator. This will be followed by the left-hand-side expression
     * (demarked by beginExpression() and endExpression(), then gotQuestionColon) followed by a continuation
     * of the current expression (for the right-hand-side).
     */
    void gotQuestionOperator(LocatableToken token);

    void gotQuestionColon(LocatableToken token);

    /**
     * Saw the "instanceof" operator. The type spec will follow.
     */
    void gotInstanceOfOperator(LocatableToken token);

    /**
     * Saw a var name following an "instanceof". Called after gotInstanceOfOperator and gotTypeSpec.
     */
    void gotInstanceOfVar(LocatableToken token);

    void gotArrayElementAccess();

    void gotImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken);

    void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken);

    /**
     * We've seen a constructor declaration. The token supplied is the constructor name.
     * The hiddenToken is the comment before the constructor.
     */
    void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken);

    /**
     * We've seen a constructor declaration. The token supplied is the start (can be e.g. `constructor` keyword or
     * an opening parent of argument list).
     * The hiddenToken is the comment before the constructor.
     */
    void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken, String name);

    /**
     * We've seen a method declaration; the token parameter is the method name;
     * the hiddenToken parameter is the comment before the method
     */
    void gotMethodDeclaration(LocatableToken token, LocatableToken hiddenToken);

    /**
     * We saw a method (or constructor) parameter. The given token specifies the parameter name.
     * The last type parsed by parseTypeSpec(boolean) is the parameter type, after any additonal
     * array declarators (see gotArrayDeclarator()) are applied.
     *
     * @param token         The token giving the parameter name
     * @param ellipsisToken The token, if any, with the ellipsis indicating a varargs parameter. May be null.
     */
    void gotMethodParameter(LocatableToken token, LocatableToken ellipsisToken);

    /**
     * Called when, after a parameter/field/variable name, array declarators "[]" are seen.
     * Will be called once for each set of "[]", immediately before gotField() or equivalent
     * is called.
     */
    void gotArrayDeclarator();

    /**
     * Called for the array components when we get "new xyz[]".
     */
    void gotNewArrayDeclarator(boolean withDimension);

    void gotAllMethodParameters();

    /**
     * Saw a type parameter for a class or method. If for a method, will be bracketed by
     * calls to {@code gotMethodTypeParamsBegin} and {@code endMethodTypeParams}
     *
     * @param idToken The token with the type parameter identifier
     */
    void gotTypeParam(LocatableToken idToken);

    void gotTypeParamBound(List<LocatableToken> tokens);

    void gotMethodTypeParamsBegin();

    void endMethodTypeParams();

    /**
     * Called by the lexer when it sees a comment.
     */
    void gotComment(LocatableToken token);

    void gotInnerType(LocatableToken start);

    void beginThrows(LocatableToken token);

    void endThrows();

    void gotTopLevelDecl(LocatableToken token);

    void beginSwitchCase(LocatableToken token);

    void gotSwitchCaseType(LocatableToken token, boolean isArrowSyntax);

    void endSwitchCase(LocatableToken token, boolean wasArrowSyntax);

    void gotSwitchDefault();

    void gotThrow(LocatableToken token);

    void gotBreakContinue(LocatableToken keywordToken, LocatableToken labelToken);

    void gotReturnStatement(boolean hasValue);

    void gotYieldStatement();

    void gotEmptyStatement();

    void gotCatchFinally(LocatableToken token);

    void gotMultiCatch(LocatableToken token);

    void gotCatchVarName(LocatableToken token);

    void gotAssert();

    void gotForTest(boolean isPresent);

    void gotForIncrement(boolean isPresent);

    void determinedForLoop(boolean forEachLoop, boolean initExpressionFollows);

    void gotAnnotation(List<LocatableToken> annName, boolean paramsFollow);

    /**
     * A lambda expression has been found and we are about to parse its body (the part after ->).
     * If lambdaIsBlock, a statement block body follows, otherwise an expression follows.
     */
    void beginLambdaBody(boolean lambdaIsBlock, LocatableToken openCurly);

    /**
     * The end of the lambda body has been reached (either block or expression)
     */
    void endLambdaBody(LocatableToken closeCurly);

    void gotPostOperator(LocatableToken token);

    void gotArrayTypeIdentifier(LocatableToken token);

    void gotParentIdentifier(LocatableToken token);

    /**
     * Called when we find a lambda formal parameter, i.e. declaration of a parameter
     * like (int x) ->
     */
    void gotLambdaFormalParam();

    /**
     * Called when we find a lambda formal parameter name, i.e. the "x" in the
     * declaration of a parameter like (int x)
     */
    void gotLambdaFormalName(LocatableToken name);

    /**
     * Called when we find a lambda formal parameter name, i.e. the "List&lt;Integer&gt;" in the
     * declaration of a parameter like (List&lt;Integer&gt; x)
     */
    void gotLambdaFormalType(List<LocatableToken> type);

    void beginFormalParameter(LocatableToken token);

    /**
     * Called at the beginning of the record parameters in a header, i.e. the opening parenthesis
     * in record Point(int x, int y)
     *
     * @param parenToken The opening-parenthesis token
     */
    void beginRecordParameters(LocatableToken parenToken);

    /**
     * Called when a record parameter has been encountered in a record header.
     *
     * @param first        The first token of that record parameter, either a modifier or the type.
     * @param idToken      The token for the identifier (name) of the parameter.
     * @param varargsToken The token for the varargs on the parameter, or null if this is not a varargs parameter.
     */
    void gotRecordParameter(LocatableToken first, LocatableToken idToken, LocatableToken varargsToken);

    /**
     * Called at the end of the record parameters in a header, i.e. the closing parenthesis
     * in record Point(int x, int y)
     *
     * @param closeParen The close-parenthesis token
     */
    void endRecordParameters(LocatableToken closeParen);

    /**
     * An error occurred during parsing. Override this method to control error behaviour.
     *
     * @param msg       A message describing the error
     * @param beginLine The line where the erroneous token begins
     * @param beginCol  The column where the erroneous token begins
     * @param endLine   The line where the erroneous token ends
     * @param endCol    The column where the erroneous token ends
     */
    void error(String msg, int beginLine, int beginCol, int endLine, int endCol);
}
