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
 *
 * @param <Target> The concrete type implementing JavaParserCallbacks
 */
public final class JavaParserCallbacksAdapter implements JavaParserCallbacks {
    private final SourceParser target;
    
    // Method handles - organized by functional category
    // Package and imports
    private final MethodHandle mhBeginPackageStatement;
    private final MethodHandle mhGotPackage;
    private final MethodHandle mhGotPackageSemi;
    private final MethodHandle mhGotImportStmtSemi;
    private final MethodHandle mhGotImport;
    private final MethodHandle mhGotWildcardImport;
    
    // Modifiers and elements
    private final MethodHandle mhGotModifier;
    private final MethodHandle mhModifiersConsumed;
    private final MethodHandle mhBeginElement;
    private final MethodHandle mhEndElement;
    
    // Method and constructor declarations
    private final MethodHandle mhBeginMethodBody;
    private final MethodHandle mhEndMethodBody;
    private final MethodHandle mhEndMethodDecl;
    private final MethodHandle mhGotConstructorDecl;
    private final MethodHandle mhGotMethodDeclaration;
    private final MethodHandle mhGotMethodParameter;
    private final MethodHandle mhGotArrayDeclarator;
    private final MethodHandle mhGotAllMethodParameters;
    private final MethodHandle mhGotMethodTypeParamsBegin;
    private final MethodHandle mhEndMethodTypeParams;
    private final MethodHandle mhBeginThrows;
    private final MethodHandle mhEndThrows;
    
    // Compilation unit states
    private final MethodHandle mhReachedCUstate;
    private final MethodHandle mhFinishedCU;
    
    // Control flow - for loops
    private final MethodHandle mhBeginForLoop;
    private final MethodHandle mhBeginForLoopBody;
    private final MethodHandle mhEndForLoopBody;
    private final MethodHandle mhEndForLoop;
    private final MethodHandle mhGotForTest;
    private final MethodHandle mhGotForIncrement;
    private final MethodHandle mhDeterminedForLoop;
    
    // Control flow - while loops
    private final MethodHandle mhBeginWhileLoop;
    private final MethodHandle mhBeginWhileLoopBody;
    private final MethodHandle mhEndWhileLoopBody;
    private final MethodHandle mhEndWhileLoop;
    
    // Control flow - if statements
    private final MethodHandle mhBeginIfStmt;
    private final MethodHandle mhBeginIfCondBlock;
    private final MethodHandle mhEndIfCondBlock;
    private final MethodHandle mhGotElseIf;
    private final MethodHandle mhEndIfStmt;
    
    // Control flow - switch statements
    private final MethodHandle mhBeginSwitchStmt;
    private final MethodHandle mhBeginSwitchBlock;
    private final MethodHandle mhEndSwitchBlock;
    private final MethodHandle mhEndSwitchStmt;
    private final MethodHandle mhBeginSwitchCase;
    private final MethodHandle mhGotSwitchCaseType;
    private final MethodHandle mhEndSwitchCase;
    private final MethodHandle mhGotSwitchDefault;
    
    // Control flow - do-while loops
    private final MethodHandle mhBeginDoWhile;
    private final MethodHandle mhBeginDoWhileBody;
    private final MethodHandle mhEndDoWhileBody;
    private final MethodHandle mhEndDoWhile;
    
    // Exception handling
    private final MethodHandle mhBeginTryCatchSmt;
    private final MethodHandle mhBeginTryBlock;
    private final MethodHandle mhEndTryBlock;
    private final MethodHandle mhEndTryCatchStmt;
    private final MethodHandle mhGotCatchFinally;
    private final MethodHandle mhGotMultiCatch;
    private final MethodHandle mhGotCatchVarName;
    
    // Synchronized blocks
    private final MethodHandle mhBeginSynchronizedBlock;
    private final MethodHandle mhEndSynchronizedBlock;
    
    // Arguments
    private final MethodHandle mhBeginArgumentList;
    private final MethodHandle mhEndArgument;
    private final MethodHandle mhEndArgumentList;
    
    // Expressions
    private final MethodHandle mhGotExprNew;
    private final MethodHandle mhEndExprNew;
    private final MethodHandle mhBeginArrayInitList;
    private final MethodHandle mhEndArrayInitList;
    private final MethodHandle mhBeginExpression;
    private final MethodHandle mhEndExpression;
    private final MethodHandle mhGotLiteral;
    private final MethodHandle mhGotPrimitiveTypeLiteral;
    private final MethodHandle mhGotIdentifier;
    private final MethodHandle mhGotIdentifierEOF;
    private final MethodHandle mhGotMemberAccessEOF;
    private final MethodHandle mhGotCompoundIdent;
    private final MethodHandle mhGotCompoundComponent;
    private final MethodHandle mhCompleteCompoundValue;
    private final MethodHandle mhCompleteCompoundValueEOF;
    private final MethodHandle mhCompleteCompoundClass;
    private final MethodHandle mhGotMemberAccess;
    private final MethodHandle mhGotMemberCall;
    private final MethodHandle mhGotMethodCall;
    private final MethodHandle mhGotConstructorCall;
    private final MethodHandle mhGotDotEOF;
    private final MethodHandle mhGotStatementExpression;
    private final MethodHandle mhGotClassLiteral;
    private final MethodHandle mhGotBinaryOperator;
    private final MethodHandle mhGotUnaryOperator;
    private final MethodHandle mhGotQuestionOperator;
    private final MethodHandle mhGotQuestionColon;
    private final MethodHandle mhGotInstanceOfOperator;
    private final MethodHandle mhGotInstanceOfVar;
    private final MethodHandle mhGotArrayElementAccess;
    private final MethodHandle mhGotPostOperator;
    
    // Anonymous class bodies
    private final MethodHandle mhBeginAnonClassBody;
    private final MethodHandle mhEndAnonClassBody;
    
    // Statement blocks
    private final MethodHandle mhBeginStmtblockBody;
    private final MethodHandle mhEndStmtblockBody;
    
    // Initializer blocks
    private final MethodHandle mhBeginInitBlock;
    private final MethodHandle mhEndInitBlock;
    
    // Type definitions
    private final MethodHandle mhBeginTypeBody;
    private final MethodHandle mhEndTypeBody;
    private final MethodHandle mhGotDeclBegin;
    private final MethodHandle mhEndDecl;
    private final MethodHandle mhGotTypeDef;
    private final MethodHandle mhGotTypeDefName;
    private final MethodHandle mhBeginTypeDefExtends;
    private final MethodHandle mhEndTypeDefExtends;
    private final MethodHandle mhBeginTypeDefImplements;
    private final MethodHandle mhEndTypeDefImplements;
    private final MethodHandle mhBeginTypeDefPermits;
    private final MethodHandle mhEndTypeDefPermits;
    private final MethodHandle mhGotTypeDefEnd;
    private final MethodHandle mhGotInnerType;
    private final MethodHandle mhGotTopLevelDecl;
    
    // Variable declarations
    private final MethodHandle mhBeginVariableDecl;
    private final MethodHandle mhGotVariableDecl;
    private final MethodHandle mhGotSubsequentVar;
    private final MethodHandle mhEndVariable;
    private final MethodHandle mhEndVariableDecls;
    private final MethodHandle mhBeginForInitDecl;
    private final MethodHandle mhGotForInit;
    private final MethodHandle mhGotSubsequentForInit;
    private final MethodHandle mhEndForInit;
    private final MethodHandle mhEndForInitDecls;
    
    // Field declarations
    private final MethodHandle mhBeginFieldDeclarations;
    private final MethodHandle mhGotField;
    private final MethodHandle mhGotSubsequentField;
    private final MethodHandle mhEndField;
    private final MethodHandle mhEndFieldDeclarations;
    
    // Type specifications
    private final MethodHandle mhGotTypeSpec;
    private final MethodHandle mhGotTypeCast;
    private final MethodHandle mhGotNewArrayDeclarator;
    private final MethodHandle mhGotTypeParam;
    private final MethodHandle mhGotTypeParamBound;
    
    // Statements
    private final MethodHandle mhGotThrow;
    private final MethodHandle mhGotBreakContinue;
    private final MethodHandle mhGotReturnStatement;
    private final MethodHandle mhGotYieldStatement;
    private final MethodHandle mhGotEmptyStatement;
    private final MethodHandle mhGotAssert;
    
    // Annotations
    private final MethodHandle mhGotAnnotation;
    
    // Lambda expressions
    private final MethodHandle mhBeginLambdaBody;
    private final MethodHandle mhEndLambdaBody;
    private final MethodHandle mhGotLambdaFormalParam;
    private final MethodHandle mhGotLambdaFormalName;
    private final MethodHandle mhGotLambdaFormalType;
    
    // Formal parameters
    private final MethodHandle mhBeginFormalParameter;
    private final MethodHandle mhGotArrayTypeIdentifier;
    private final MethodHandle mhGotParentIdentifier;
    
    // Record declarations
    private final MethodHandle mhBeginRecordParameters;
    private final MethodHandle mhGotRecordParameter;
    private final MethodHandle mhEndRecordParameters;
    
    // Comments and errors
    private final MethodHandle mhGotComment;
    private final MethodHandle mhError;

    public JavaParserCallbacksAdapter(SourceParser target) {
        var targetClass = target.getClass();
        this.target = target;

        try {
            var lookup = MethodHandles.privateLookupIn(targetClass, MethodHandles.lookup());

            // Package and imports
            mhBeginPackageStatement = findAndBind(lookup, targetClass, "beginPackageStatement", void.class, LocatableToken.class);
            mhGotPackage = findAndBind(lookup, targetClass, "gotPackage", void.class, List.class);
            mhGotPackageSemi = findAndBind(lookup, targetClass, "gotPackageSemi", void.class, LocatableToken.class);
            mhGotImportStmtSemi = findAndBind(lookup, targetClass, "gotImportStmtSemi", void.class, LocatableToken.class);
            mhGotImport = findAndBind(lookup, targetClass, "gotImport", void.class, List.class, boolean.class, LocatableToken.class, LocatableToken.class);
            mhGotWildcardImport = findAndBind(lookup, targetClass, "gotWildcardImport", void.class, List.class, boolean.class, LocatableToken.class, LocatableToken.class);
            
            // Modifiers and elements
            mhGotModifier = findAndBind(lookup, targetClass, "gotModifier", void.class, LocatableToken.class);
            mhModifiersConsumed = findAndBind(lookup, targetClass, "modifiersConsumed", void.class);
            mhBeginElement = findAndBind(lookup, targetClass, "beginElement", void.class, LocatableToken.class);
            mhEndElement = findAndBind(lookup, targetClass, "endElement", void.class, LocatableToken.class, boolean.class);
            
            // Method and constructor declarations
            mhBeginMethodBody = findAndBind(lookup, targetClass, "beginMethodBody", void.class, LocatableToken.class);
            mhEndMethodBody = findAndBind(lookup, targetClass, "endMethodBody", void.class, LocatableToken.class, boolean.class);
            mhEndMethodDecl = findAndBind(lookup, targetClass, "endMethodDecl", void.class, LocatableToken.class, boolean.class);
            mhGotConstructorDecl = findAndBind(lookup, targetClass, "gotConstructorDecl", void.class, LocatableToken.class, LocatableToken.class);
            mhGotMethodDeclaration = findAndBind(lookup, targetClass, "gotMethodDeclaration", void.class, LocatableToken.class, LocatableToken.class);
            mhGotMethodParameter = findAndBind(lookup, targetClass, "gotMethodParameter", void.class, LocatableToken.class, LocatableToken.class);
            mhGotArrayDeclarator = findAndBind(lookup, targetClass, "gotArrayDeclarator", void.class);
            mhGotAllMethodParameters = findAndBind(lookup, targetClass, "gotAllMethodParameters", void.class);
            mhGotMethodTypeParamsBegin = findAndBind(lookup, targetClass, "gotMethodTypeParamsBegin", void.class);
            mhEndMethodTypeParams = findAndBind(lookup, targetClass, "endMethodTypeParams", void.class);
            mhBeginThrows = findAndBind(lookup, targetClass, "beginThrows", void.class, LocatableToken.class);
            mhEndThrows = findAndBind(lookup, targetClass, "endThrows", void.class);
            
            // Compilation unit states
            mhReachedCUstate = findAndBind(lookup, targetClass, "reachedCUstate", void.class, int.class);
            mhFinishedCU = findAndBind(lookup, targetClass, "finishedCU", void.class, int.class);
            
            // Control flow - for loops
            mhBeginForLoop = findAndBind(lookup, targetClass, "beginForLoop", void.class, LocatableToken.class);
            mhBeginForLoopBody = findAndBind(lookup, targetClass, "beginForLoopBody", void.class, LocatableToken.class);
            mhEndForLoopBody = findAndBind(lookup, targetClass, "endForLoopBody", void.class, LocatableToken.class, boolean.class);
            mhEndForLoop = findAndBind(lookup, targetClass, "endForLoop", void.class, LocatableToken.class, boolean.class);
            mhGotForTest = findAndBind(lookup, targetClass, "gotForTest", void.class, boolean.class);
            mhGotForIncrement = findAndBind(lookup, targetClass, "gotForIncrement", void.class, boolean.class);
            mhDeterminedForLoop = findAndBind(lookup, targetClass, "determinedForLoop", void.class, boolean.class, boolean.class);
            
            // Control flow - while loops
            mhBeginWhileLoop = findAndBind(lookup, targetClass, "beginWhileLoop", void.class, LocatableToken.class);
            mhBeginWhileLoopBody = findAndBind(lookup, targetClass, "beginWhileLoopBody", void.class, LocatableToken.class);
            mhEndWhileLoopBody = findAndBind(lookup, targetClass, "endWhileLoopBody", void.class, LocatableToken.class, boolean.class);
            mhEndWhileLoop = findAndBind(lookup, targetClass, "endWhileLoop", void.class, LocatableToken.class, boolean.class);
            
            // Control flow - if statements
            mhBeginIfStmt = findAndBind(lookup, targetClass, "beginIfStmt", void.class, LocatableToken.class);
            mhBeginIfCondBlock = findAndBind(lookup, targetClass, "beginIfCondBlock", void.class, LocatableToken.class);
            mhEndIfCondBlock = findAndBind(lookup, targetClass, "endIfCondBlock", void.class, LocatableToken.class, boolean.class);
            mhGotElseIf = findAndBind(lookup, targetClass, "gotElseIf", void.class, LocatableToken.class);
            mhEndIfStmt = findAndBind(lookup, targetClass, "endIfStmt", void.class, LocatableToken.class, boolean.class);
            
            // Control flow - switch statements
            mhBeginSwitchStmt = findAndBind(lookup, targetClass, "beginSwitchStmt", void.class, LocatableToken.class, boolean.class);
            mhBeginSwitchBlock = findAndBind(lookup, targetClass, "beginSwitchBlock", void.class, LocatableToken.class);
            mhEndSwitchBlock = findAndBind(lookup, targetClass, "endSwitchBlock", void.class, LocatableToken.class);
            mhEndSwitchStmt = findAndBind(lookup, targetClass, "endSwitchStmt", void.class, LocatableToken.class, boolean.class);
            mhBeginSwitchCase = findAndBind(lookup, targetClass, "beginSwitchCase", void.class, LocatableToken.class);
            mhGotSwitchCaseType = findAndBind(lookup, targetClass, "gotSwitchCaseType", void.class, LocatableToken.class, boolean.class);
            mhEndSwitchCase = findAndBind(lookup, targetClass, "endSwitchCase", void.class, LocatableToken.class, boolean.class);
            mhGotSwitchDefault = findAndBind(lookup, targetClass, "gotSwitchDefault", void.class);
            
            // Control flow - do-while loops
            mhBeginDoWhile = findAndBind(lookup, targetClass, "beginDoWhile", void.class, LocatableToken.class);
            mhBeginDoWhileBody = findAndBind(lookup, targetClass, "beginDoWhileBody", void.class, LocatableToken.class);
            mhEndDoWhileBody = findAndBind(lookup, targetClass, "endDoWhileBody", void.class, LocatableToken.class, boolean.class);
            mhEndDoWhile = findAndBind(lookup, targetClass, "endDoWhile", void.class, LocatableToken.class, boolean.class);
            
            // Exception handling
            mhBeginTryCatchSmt = findAndBind(lookup, targetClass, "beginTryCatchSmt", void.class, LocatableToken.class, boolean.class);
            mhBeginTryBlock = findAndBind(lookup, targetClass, "beginTryBlock", void.class, LocatableToken.class);
            mhEndTryBlock = findAndBind(lookup, targetClass, "endTryBlock", void.class, LocatableToken.class, boolean.class);
            mhEndTryCatchStmt = findAndBind(lookup, targetClass, "endTryCatchStmt", void.class, LocatableToken.class, boolean.class);
            mhGotCatchFinally = findAndBind(lookup, targetClass, "gotCatchFinally", void.class, LocatableToken.class);
            mhGotMultiCatch = findAndBind(lookup, targetClass, "gotMultiCatch", void.class, LocatableToken.class);
            mhGotCatchVarName = findAndBind(lookup, targetClass, "gotCatchVarName", void.class, LocatableToken.class);
            
            // Synchronized blocks
            mhBeginSynchronizedBlock = findAndBind(lookup, targetClass, "beginSynchronizedBlock", void.class, LocatableToken.class);
            mhEndSynchronizedBlock = findAndBind(lookup, targetClass, "endSynchronizedBlock", void.class, LocatableToken.class, boolean.class);
            
            // Arguments
            mhBeginArgumentList = findAndBind(lookup, targetClass, "beginArgumentList", void.class, LocatableToken.class);
            mhEndArgument = findAndBind(lookup, targetClass, "endArgument", void.class);
            mhEndArgumentList = findAndBind(lookup, targetClass, "endArgumentList", void.class, LocatableToken.class);
            
            // Expressions
            mhGotExprNew = findAndBind(lookup, targetClass, "gotExprNew", void.class, LocatableToken.class);
            mhEndExprNew = findAndBind(lookup, targetClass, "endExprNew", void.class, LocatableToken.class, boolean.class);
            mhBeginArrayInitList = findAndBind(lookup, targetClass, "beginArrayInitList", void.class, LocatableToken.class);
            mhEndArrayInitList = findAndBind(lookup, targetClass, "endArrayInitList", void.class, LocatableToken.class);
            mhBeginExpression = findAndBind(lookup, targetClass, "beginExpression", void.class, LocatableToken.class, boolean.class);
            mhEndExpression = findAndBind(lookup, targetClass, "endExpression", void.class, LocatableToken.class, boolean.class);
            mhGotLiteral = findAndBind(lookup, targetClass, "gotLiteral", void.class, LocatableToken.class);
            mhGotPrimitiveTypeLiteral = findAndBind(lookup, targetClass, "gotPrimitiveTypeLiteral", void.class, LocatableToken.class);
            mhGotIdentifier = findAndBind(lookup, targetClass, "gotIdentifier", void.class, LocatableToken.class);
            mhGotIdentifierEOF = findAndBind(lookup, targetClass, "gotIdentifierEOF", void.class, LocatableToken.class);
            mhGotMemberAccessEOF = findAndBind(lookup, targetClass, "gotMemberAccessEOF", void.class, LocatableToken.class);
            mhGotCompoundIdent = findAndBind(lookup, targetClass, "gotCompoundIdent", void.class, LocatableToken.class);
            mhGotCompoundComponent = findAndBind(lookup, targetClass, "gotCompoundComponent", void.class, LocatableToken.class);
            mhCompleteCompoundValue = findAndBind(lookup, targetClass, "completeCompoundValue", void.class, LocatableToken.class);
            mhCompleteCompoundValueEOF = findAndBind(lookup, targetClass, "completeCompoundValueEOF", void.class, LocatableToken.class);
            mhCompleteCompoundClass = findAndBind(lookup, targetClass, "completeCompoundClass", void.class, LocatableToken.class);
            mhGotMemberAccess = findAndBind(lookup, targetClass, "gotMemberAccess", void.class, LocatableToken.class);
            mhGotMemberCall = findAndBind(lookup, targetClass, "gotMemberCall", void.class, LocatableToken.class, List.class);
            mhGotMethodCall = findAndBind(lookup, targetClass, "gotMethodCall", void.class, LocatableToken.class);
            mhGotConstructorCall = findAndBind(lookup, targetClass, "gotConstructorCall", void.class, LocatableToken.class);
            mhGotDotEOF = findAndBind(lookup, targetClass, "gotDotEOF", void.class, LocatableToken.class);
            mhGotStatementExpression = findAndBind(lookup, targetClass, "gotStatementExpression", void.class);
            mhGotClassLiteral = findAndBind(lookup, targetClass, "gotClassLiteral", void.class, LocatableToken.class);
            mhGotBinaryOperator = findAndBind(lookup, targetClass, "gotBinaryOperator", void.class, LocatableToken.class);
            mhGotUnaryOperator = findAndBind(lookup, targetClass, "gotUnaryOperator", void.class, LocatableToken.class);
            mhGotQuestionOperator = findAndBind(lookup, targetClass, "gotQuestionOperator", void.class, LocatableToken.class);
            mhGotQuestionColon = findAndBind(lookup, targetClass, "gotQuestionColon", void.class, LocatableToken.class);
            mhGotInstanceOfOperator = findAndBind(lookup, targetClass, "gotInstanceOfOperator", void.class, LocatableToken.class);
            mhGotInstanceOfVar = findAndBind(lookup, targetClass, "gotInstanceOfVar", void.class, LocatableToken.class);
            mhGotArrayElementAccess = findAndBind(lookup, targetClass, "gotArrayElementAccess", void.class);
            mhGotPostOperator = findAndBind(lookup, targetClass, "gotPostOperator", void.class, LocatableToken.class);
            
            // Anonymous class bodies
            mhBeginAnonClassBody = findAndBind(lookup, targetClass, "beginAnonClassBody", void.class, LocatableToken.class, boolean.class);
            mhEndAnonClassBody = findAndBind(lookup, targetClass, "endAnonClassBody", void.class, LocatableToken.class, boolean.class);
            
            // Statement blocks
            mhBeginStmtblockBody = findAndBind(lookup, targetClass, "beginStmtblockBody", void.class, LocatableToken.class);
            mhEndStmtblockBody = findAndBind(lookup, targetClass, "endStmtblockBody", void.class, LocatableToken.class, boolean.class);
            
            // Initializer blocks
            mhBeginInitBlock = findAndBind(lookup, targetClass, "beginInitBlock", void.class, LocatableToken.class, LocatableToken.class);
            mhEndInitBlock = findAndBind(lookup, targetClass, "endInitBlock", void.class, LocatableToken.class, boolean.class);
            
            // Type definitions
            mhBeginTypeBody = findAndBind(lookup, targetClass, "beginTypeBody", void.class, LocatableToken.class);
            mhEndTypeBody = findAndBind(lookup, targetClass, "endTypeBody", void.class, LocatableToken.class, boolean.class);
            mhGotDeclBegin = findAndBind(lookup, targetClass, "gotDeclBegin", void.class, LocatableToken.class);
            mhEndDecl = findAndBind(lookup, targetClass, "endDecl", void.class, LocatableToken.class);
            mhGotTypeDef = findAndBind(lookup, targetClass, "gotTypeDef", void.class, LocatableToken.class, int.class);
            mhGotTypeDefName = findAndBind(lookup, targetClass, "gotTypeDefName", void.class, LocatableToken.class);
            mhBeginTypeDefExtends = findAndBind(lookup, targetClass, "beginTypeDefExtends", void.class, LocatableToken.class);
            mhEndTypeDefExtends = findAndBind(lookup, targetClass, "endTypeDefExtends", void.class);
            mhBeginTypeDefImplements = findAndBind(lookup, targetClass, "beginTypeDefImplements", void.class, LocatableToken.class);
            mhEndTypeDefImplements = findAndBind(lookup, targetClass, "endTypeDefImplements", void.class);
            mhBeginTypeDefPermits = findAndBind(lookup, targetClass, "beginTypeDefPermits", void.class, LocatableToken.class);
            mhEndTypeDefPermits = findAndBind(lookup, targetClass, "endTypeDefPermits", void.class);
            mhGotTypeDefEnd = findAndBind(lookup, targetClass, "gotTypeDefEnd", void.class, LocatableToken.class, boolean.class);
            mhGotInnerType = findAndBind(lookup, targetClass, "gotInnerType", void.class, LocatableToken.class);
            mhGotTopLevelDecl = findAndBind(lookup, targetClass, "gotTopLevelDecl", void.class, LocatableToken.class);
            
            // Variable declarations
            mhBeginVariableDecl = findAndBind(lookup, targetClass, "beginVariableDecl", void.class, LocatableToken.class);
            mhGotVariableDecl = findAndBind(lookup, targetClass, "gotVariableDecl", void.class, LocatableToken.class, LocatableToken.class, boolean.class);
            mhGotSubsequentVar = findAndBind(lookup, targetClass, "gotSubsequentVar", void.class, LocatableToken.class, LocatableToken.class, boolean.class);
            mhEndVariable = findAndBind(lookup, targetClass, "endVariable", void.class, LocatableToken.class, boolean.class);
            mhEndVariableDecls = findAndBind(lookup, targetClass, "endVariableDecls", void.class, LocatableToken.class, boolean.class);
            mhBeginForInitDecl = findAndBind(lookup, targetClass, "beginForInitDecl", void.class, LocatableToken.class);
            mhGotForInit = findAndBind(lookup, targetClass, "gotForInit", void.class, LocatableToken.class, LocatableToken.class);
            mhGotSubsequentForInit = findAndBind(lookup, targetClass, "gotSubsequentForInit", void.class, LocatableToken.class, LocatableToken.class, boolean.class);
            mhEndForInit = findAndBind(lookup, targetClass, "endForInit", void.class, LocatableToken.class, boolean.class);
            mhEndForInitDecls = findAndBind(lookup, targetClass, "endForInitDecls", void.class, LocatableToken.class, boolean.class);
            
            // Field declarations
            mhBeginFieldDeclarations = findAndBind(lookup, targetClass, "beginFieldDeclarations", void.class, LocatableToken.class);
            mhGotField = findAndBind(lookup, targetClass, "gotField", void.class, LocatableToken.class, LocatableToken.class, boolean.class);
            mhGotSubsequentField = findAndBind(lookup, targetClass, "gotSubsequentField", void.class, LocatableToken.class, LocatableToken.class, boolean.class);
            mhEndField = findAndBind(lookup, targetClass, "endField", void.class, LocatableToken.class, boolean.class);
            mhEndFieldDeclarations = findAndBind(lookup, targetClass, "endFieldDeclarations", void.class, LocatableToken.class, boolean.class);
            
            // Type specifications
            mhGotTypeSpec = findAndBind(lookup, targetClass, "gotTypeSpec", void.class, List.class);
            mhGotTypeCast = findAndBind(lookup, targetClass, "gotTypeCast", void.class, List.class);
            mhGotNewArrayDeclarator = findAndBind(lookup, targetClass, "gotNewArrayDeclarator", void.class, boolean.class);
            mhGotTypeParam = findAndBind(lookup, targetClass, "gotTypeParam", void.class, LocatableToken.class);
            mhGotTypeParamBound = findAndBind(lookup, targetClass, "gotTypeParamBound", void.class, List.class);
            
            // Statements
            mhGotThrow = findAndBind(lookup, targetClass, "gotThrow", void.class, LocatableToken.class);
            mhGotBreakContinue = findAndBind(lookup, targetClass, "gotBreakContinue", void.class, LocatableToken.class, LocatableToken.class);
            mhGotReturnStatement = findAndBind(lookup, targetClass, "gotReturnStatement", void.class, boolean.class);
            mhGotYieldStatement = findAndBind(lookup, targetClass, "gotYieldStatement", void.class);
            mhGotEmptyStatement = findAndBind(lookup, targetClass, "gotEmptyStatement", void.class);
            mhGotAssert = findAndBind(lookup, targetClass, "gotAssert", void.class);
            
            // Annotations
            mhGotAnnotation = findAndBind(lookup, targetClass, "gotAnnotation", void.class, List.class, boolean.class);
            
            // Lambda expressions
            mhBeginLambdaBody = findAndBind(lookup, targetClass, "beginLambdaBody", void.class, boolean.class, LocatableToken.class);
            mhEndLambdaBody = findAndBind(lookup, targetClass, "endLambdaBody", void.class, LocatableToken.class);
            mhGotLambdaFormalParam = findAndBind(lookup, targetClass, "gotLambdaFormalParam", void.class);
            mhGotLambdaFormalName = findAndBind(lookup, targetClass, "gotLambdaFormalName", void.class, LocatableToken.class);
            mhGotLambdaFormalType = findAndBind(lookup, targetClass, "gotLambdaFormalType", void.class, List.class);
            
            // Formal parameters
            mhBeginFormalParameter = findAndBind(lookup, targetClass, "beginFormalParameter", void.class, LocatableToken.class);
            mhGotArrayTypeIdentifier = findAndBind(lookup, targetClass, "gotArrayTypeIdentifier", void.class, LocatableToken.class);
            mhGotParentIdentifier = findAndBind(lookup, targetClass, "gotParentIdentifier", void.class, LocatableToken.class);
            
            // Record declarations
            mhBeginRecordParameters = findAndBind(lookup, targetClass, "beginRecordParameters", void.class, LocatableToken.class);
            mhGotRecordParameter = findAndBind(lookup, targetClass, "gotRecordParameter", void.class, LocatableToken.class, LocatableToken.class, LocatableToken.class);
            mhEndRecordParameters = findAndBind(lookup, targetClass, "endRecordParameters", void.class, LocatableToken.class);
            
            // Comments and errors
            mhGotComment = findAndBind(lookup, targetClass, "gotComment", void.class, LocatableToken.class);
            mhError = findAndBind(lookup, targetClass, "error", void.class, String.class, int.class, int.class, int.class, int.class);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to initialize JavaParserCallbacksAdapter for " + targetClass.getName(), e);
        }
    }

    /**
     * Helper method to find a virtual method, bind it to the target, and convert to the appropriate type.
     */
    private MethodHandle findAndBind(MethodHandles.Lookup lookup, Class<?> targetClass, String methodName, 
                                      Class<?> returnType, Class<?>... paramTypes) throws ReflectiveOperationException {
        return lookup
                .findVirtual(targetClass, methodName, MethodType.methodType(returnType, paramTypes))
                .bindTo(target)
                .asType(MethodType.methodType(returnType, paramTypes));
    }

    // Package and imports
    @Override
    public void beginPackageStatement(LocatableToken token) {
        try {
            mhBeginPackageStatement.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotPackage(List<LocatableToken> pkgTokens) {
        try {
            mhGotPackage.invokeExact(pkgTokens);
            skipToLastToken(pkgTokens);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotPackageSemi(LocatableToken token) {
        try {
            mhGotPackageSemi.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotImportStmtSemi(LocatableToken token) {
        try {
            mhGotImportStmtSemi.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken) {
        try {
            mhGotImport.invokeExact(tokens, isStatic, importToken, semiColonToken);
            skipToToken(semiColonToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken) {
        try {
            mhGotWildcardImport.invokeExact(tokens, isStatic, importToken, semiColonToken);
            skipToToken(semiColonToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Modifiers and elements
    @Override
    public void gotModifier(LocatableToken token) {
        try {
            mhGotModifier.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void modifiersConsumed() {
        try {
            mhModifiersConsumed.invokeExact();
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginElement(LocatableToken token) {
        try {
            mhBeginElement.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endElement(LocatableToken token, boolean included) {
        try {
            mhEndElement.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Method and constructor declarations
    @Override
    public void beginMethodBody(LocatableToken token) {
        try {
            mhBeginMethodBody.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endMethodBody(LocatableToken token, boolean included) {
        try {
            mhEndMethodBody.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endMethodDecl(LocatableToken token, boolean included) {
        try {
            mhEndMethodDecl.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken) {
        try {
            mhGotConstructorDecl.invokeExact(token, hiddenToken);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotMethodDeclaration(LocatableToken token, LocatableToken hiddenToken) {
        try {
            mhGotMethodDeclaration.invokeExact(token, hiddenToken);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotMethodParameter(LocatableToken token, LocatableToken ellipsisToken) {
        try {
            mhGotMethodParameter.invokeExact(token, ellipsisToken);
            skipToToken(ellipsisToken != null ? ellipsisToken : token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotArrayDeclarator() {
        try {
            mhGotArrayDeclarator.invokeExact();
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotAllMethodParameters() {
        try {
            mhGotAllMethodParameters.invokeExact();
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotMethodTypeParamsBegin() {
        try {
            mhGotMethodTypeParamsBegin.invokeExact();
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endMethodTypeParams() {
        try {
            mhEndMethodTypeParams.invokeExact();
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginThrows(LocatableToken token) {
        try {
            mhBeginThrows.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endThrows() {
        try {
            mhEndThrows.invokeExact();
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Compilation unit states
    @Override
    public void reachedCUstate(int i) {
        try {
            mhReachedCUstate.invokeExact(i);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void finishedCU(int state) {
        try {
            mhFinishedCU.invokeExact(state);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Control flow - for loops
    @Override
    public void beginForLoop(LocatableToken token) {
        try {
            mhBeginForLoop.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginForLoopBody(LocatableToken token) {
        try {
            mhBeginForLoopBody.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endForLoopBody(LocatableToken token, boolean included) {
        try {
            mhEndForLoopBody.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endForLoop(LocatableToken token, boolean included) {
        try {
            mhEndForLoop.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotForTest(boolean isPresent) {
        try {
            mhGotForTest.invokeExact(isPresent);
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotForIncrement(boolean isPresent) {
        try {
            mhGotForIncrement.invokeExact(isPresent);
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void determinedForLoop(boolean forEachLoop, boolean initExpressionFollows) {
        try {
            mhDeterminedForLoop.invokeExact(forEachLoop, initExpressionFollows);
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Control flow - while loops
    @Override
    public void beginWhileLoop(LocatableToken token) {
        try {
            mhBeginWhileLoop.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginWhileLoopBody(LocatableToken token) {
        try {
            mhBeginWhileLoopBody.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endWhileLoopBody(LocatableToken token, boolean included) {
        try {
            mhEndWhileLoopBody.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endWhileLoop(LocatableToken token, boolean included) {
        try {
            mhEndWhileLoop.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Control flow - if statements
    @Override
    public void beginIfStmt(LocatableToken token) {
        try {
            mhBeginIfStmt.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginIfCondBlock(LocatableToken token) {
        try {
            mhBeginIfCondBlock.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endIfCondBlock(LocatableToken token, boolean included) {
        try {
            mhEndIfCondBlock.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotElseIf(LocatableToken token) {
        try {
            mhGotElseIf.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endIfStmt(LocatableToken token, boolean included) {
        try {
            mhEndIfStmt.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Control flow - switch statements
    @Override
    public void beginSwitchStmt(LocatableToken token, boolean isSwitchExpression) {
        try {
            mhBeginSwitchStmt.invokeExact(token, isSwitchExpression);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginSwitchBlock(LocatableToken token) {
        try {
            mhBeginSwitchBlock.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endSwitchBlock(LocatableToken token) {
        try {
            mhEndSwitchBlock.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endSwitchStmt(LocatableToken token, boolean included) {
        try {
            mhEndSwitchStmt.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginSwitchCase(LocatableToken token) {
        try {
            mhBeginSwitchCase.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotSwitchCaseType(LocatableToken token, boolean isArrowSyntax) {
        try {
            mhGotSwitchCaseType.invokeExact(token, isArrowSyntax);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endSwitchCase(LocatableToken token, boolean wasArrowSyntax) {
        try {
            mhEndSwitchCase.invokeExact(token, wasArrowSyntax);
            skipToToken(token, wasArrowSyntax);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotSwitchDefault() {
        try {
            mhGotSwitchDefault.invokeExact();
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Control flow - do-while loops
    @Override
    public void beginDoWhile(LocatableToken token) {
        try {
            mhBeginDoWhile.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginDoWhileBody(LocatableToken token) {
        try {
            mhBeginDoWhileBody.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endDoWhileBody(LocatableToken token, boolean included) {
        try {
            mhEndDoWhileBody.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endDoWhile(LocatableToken token, boolean included) {
        try {
            mhEndDoWhile.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Exception handling
    @Override
    public void beginTryCatchSmt(LocatableToken token, boolean hasResource) {
        try {
            mhBeginTryCatchSmt.invokeExact(token, hasResource);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginTryBlock(LocatableToken token) {
        try {
            mhBeginTryBlock.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endTryBlock(LocatableToken token, boolean included) {
        try {
            mhEndTryBlock.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endTryCatchStmt(LocatableToken token, boolean included) {
        try {
            mhEndTryCatchStmt.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotCatchFinally(LocatableToken token) {
        try {
            mhGotCatchFinally.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotMultiCatch(LocatableToken token) {
        try {
            mhGotMultiCatch.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotCatchVarName(LocatableToken token) {
        try {
            mhGotCatchVarName.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Synchronized blocks
    @Override
    public void beginSynchronizedBlock(LocatableToken token) {
        try {
            mhBeginSynchronizedBlock.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endSynchronizedBlock(LocatableToken token, boolean included) {
        try {
            mhEndSynchronizedBlock.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Arguments
    @Override
    public void beginArgumentList(LocatableToken token) {
        try {
            mhBeginArgumentList.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endArgument() {
        try {
            mhEndArgument.invokeExact();
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endArgumentList(LocatableToken token) {
        try {
            mhEndArgumentList.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Expressions
    @Override
    public void gotExprNew(LocatableToken token) {
        try {
            mhGotExprNew.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endExprNew(LocatableToken token, boolean included) {
        try {
            mhEndExprNew.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginArrayInitList(LocatableToken token) {
        try {
            mhBeginArrayInitList.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endArrayInitList(LocatableToken token) {
        try {
            mhEndArrayInitList.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginExpression(LocatableToken token, boolean isLambdaBody) {
        try {
            mhBeginExpression.invokeExact(token, isLambdaBody);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endExpression(LocatableToken token, boolean emptyExpression) {
        try {
            mhEndExpression.invokeExact(token, emptyExpression);
            skipToToken(token, emptyExpression);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotLiteral(LocatableToken token) {
        try {
            mhGotLiteral.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotPrimitiveTypeLiteral(LocatableToken token) {
        try {
            mhGotPrimitiveTypeLiteral.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotIdentifier(LocatableToken token) {
        try {
            mhGotIdentifier.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotIdentifierEOF(LocatableToken token) {
        try {
            mhGotIdentifierEOF.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotMemberAccessEOF(LocatableToken token) {
        try {
            mhGotMemberAccessEOF.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotCompoundIdent(LocatableToken token) {
        try {
            mhGotCompoundIdent.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotCompoundComponent(LocatableToken token) {
        try {
            mhGotCompoundComponent.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void completeCompoundValue(LocatableToken token) {
        try {
            mhCompleteCompoundValue.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void completeCompoundValueEOF(LocatableToken token) {
        try {
            mhCompleteCompoundValueEOF.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void completeCompoundClass(LocatableToken token) {
        try {
            mhCompleteCompoundClass.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotMemberAccess(LocatableToken token) {
        try {
            mhGotMemberAccess.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs) {
        try {
            mhGotMemberCall.invokeExact(token, typeArgs);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotMethodCall(LocatableToken token) {
        try {
            mhGotMethodCall.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotConstructorCall(LocatableToken token) {
        try {
            mhGotConstructorCall.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotDotEOF(LocatableToken token) {
        try {
            mhGotDotEOF.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotStatementExpression() {
        try {
            mhGotStatementExpression.invokeExact();
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotClassLiteral(LocatableToken token) {
        try {
            mhGotClassLiteral.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotBinaryOperator(LocatableToken token) {
        try {
            mhGotBinaryOperator.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotUnaryOperator(LocatableToken token) {
        try {
            mhGotUnaryOperator.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotQuestionOperator(LocatableToken token) {
        try {
            mhGotQuestionOperator.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotQuestionColon(LocatableToken token) {
        try {
            mhGotQuestionColon.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotInstanceOfOperator(LocatableToken token) {
        try {
            mhGotInstanceOfOperator.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotInstanceOfVar(LocatableToken token) {
        try {
            mhGotInstanceOfVar.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotArrayElementAccess() {
        try {
            mhGotArrayElementAccess.invokeExact();
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotPostOperator(LocatableToken token) {
        try {
            mhGotPostOperator.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Anonymous class bodies
    @Override
    public void beginAnonClassBody(LocatableToken token, boolean isEnumMember) {
        try {
            mhBeginAnonClassBody.invokeExact(token, isEnumMember);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endAnonClassBody(LocatableToken token, boolean included) {
        try {
            mhEndAnonClassBody.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Statement blocks
    @Override
    public void beginStmtblockBody(LocatableToken token) {
        try {
            mhBeginStmtblockBody.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endStmtblockBody(LocatableToken token, boolean included) {
        try {
            mhEndStmtblockBody.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Initializer blocks
    @Override
    public void beginInitBlock(LocatableToken first, LocatableToken lcurly) {
        try {
            mhBeginInitBlock.invokeExact(first, lcurly);
            skipToToken(lcurly);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endInitBlock(LocatableToken rcurly, boolean included) {
        try {
            mhEndInitBlock.invokeExact(rcurly, included);
            skipToToken(rcurly, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Type definitions
    @Override
    public void beginTypeBody(LocatableToken leftCurlyToken) {
        try {
            mhBeginTypeBody.invokeExact(leftCurlyToken);
            skipToToken(leftCurlyToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endTypeBody(LocatableToken endCurlyToken, boolean included) {
        try {
            mhEndTypeBody.invokeExact(endCurlyToken, included);
            skipToToken(endCurlyToken, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotDeclBegin(LocatableToken token) {
        try {
            mhGotDeclBegin.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endDecl(LocatableToken token) {
        try {
            mhEndDecl.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotTypeDef(LocatableToken firstToken, int tdType) {
        try {
            mhGotTypeDef.invokeExact(firstToken, tdType);
            skipToToken(firstToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotTypeDefName(LocatableToken nameToken) {
        try {
            mhGotTypeDefName.invokeExact(nameToken);
            skipToToken(nameToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginTypeDefExtends(LocatableToken extendsToken) {
        try {
            mhBeginTypeDefExtends.invokeExact(extendsToken);
            skipToToken(extendsToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endTypeDefExtends() {
        try {
            mhEndTypeDefExtends.invokeExact();
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginTypeDefImplements(LocatableToken implementsToken) {
        try {
            mhBeginTypeDefImplements.invokeExact(implementsToken);
            skipToToken(implementsToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endTypeDefImplements() {
        try {
            mhEndTypeDefImplements.invokeExact();
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginTypeDefPermits(LocatableToken permitsToken) {
        try {
            mhBeginTypeDefPermits.invokeExact(permitsToken);
            skipToToken(permitsToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endTypeDefPermits() {
        try {
            mhEndTypeDefPermits.invokeExact();
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotTypeDefEnd(LocatableToken token, boolean included) {
        try {
            mhGotTypeDefEnd.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotInnerType(LocatableToken start) {
        try {
            mhGotInnerType.invokeExact(start);
            skipToToken(start);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotTopLevelDecl(LocatableToken token) {
        try {
            mhGotTopLevelDecl.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Variable declarations
    @Override
    public void beginVariableDecl(LocatableToken first) {
        try {
            mhBeginVariableDecl.invokeExact(first);
            skipToToken(first);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotVariableDecl(LocatableToken first, LocatableToken idToken, boolean inited) {
        try {
            mhGotVariableDecl.invokeExact(first, idToken, inited);
            skipToToken(idToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotSubsequentVar(LocatableToken first, LocatableToken idToken, boolean inited) {
        try {
            mhGotSubsequentVar.invokeExact(first, idToken, inited);
            skipToToken(idToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endVariable(LocatableToken token, boolean included) {
        try {
            mhEndVariable.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endVariableDecls(LocatableToken token, boolean included) {
        try {
            mhEndVariableDecls.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void beginForInitDecl(LocatableToken first) {
        try {
            mhBeginForInitDecl.invokeExact(first);
            skipToToken(first);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotForInit(LocatableToken first, LocatableToken idToken) {
        try {
            mhGotForInit.invokeExact(first, idToken);
            skipToToken(idToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotSubsequentForInit(LocatableToken first, LocatableToken idToken, boolean initFollows) {
        try {
            mhGotSubsequentForInit.invokeExact(first, idToken, initFollows);
            skipToToken(idToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endForInit(LocatableToken token, boolean included) {
        try {
            mhEndForInit.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endForInitDecls(LocatableToken token, boolean included) {
        try {
            mhEndForInitDecls.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Field declarations
    @Override
    public void beginFieldDeclarations(LocatableToken first) {
        try {
            mhBeginFieldDeclarations.invokeExact(first);
            skipToToken(first);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotField(LocatableToken first, LocatableToken idToken, boolean initExpressionFollows) {
        try {
            mhGotField.invokeExact(first, idToken, initExpressionFollows);
            skipToToken(idToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotSubsequentField(LocatableToken first, LocatableToken idToken, boolean initFollows) {
        try {
            mhGotSubsequentField.invokeExact(first, idToken, initFollows);
            skipToToken(idToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endField(LocatableToken token, boolean included) {
        try {
            mhEndField.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endFieldDeclarations(LocatableToken token, boolean included) {
        try {
            mhEndFieldDeclarations.invokeExact(token, included);
            skipToToken(token, included);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Type specifications
    @Override
    public void gotTypeSpec(List<LocatableToken> tokens) {
        try {
            mhGotTypeSpec.invokeExact(tokens);
            skipToLastToken(tokens);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotTypeCast(List<LocatableToken> tokens) {
        try {
            mhGotTypeCast.invokeExact(tokens);
            skipToLastToken(tokens);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotNewArrayDeclarator(boolean withDimension) {
        try {
            mhGotNewArrayDeclarator.invokeExact(withDimension);
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotTypeParam(LocatableToken idToken) {
        try {
            mhGotTypeParam.invokeExact(idToken);
            skipToToken(idToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotTypeParamBound(List<LocatableToken> tokens) {
        try {
            mhGotTypeParamBound.invokeExact(tokens);
            skipToLastToken(tokens);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Statements
    @Override
    public void gotThrow(LocatableToken token) {
        try {
            mhGotThrow.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotBreakContinue(LocatableToken keywordToken, LocatableToken labelToken) {
        try {
            mhGotBreakContinue.invokeExact(keywordToken, labelToken);
            skipToToken(labelToken != null ? labelToken : keywordToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotReturnStatement(boolean hasValue) {
        try {
            mhGotReturnStatement.invokeExact(hasValue);
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotYieldStatement() {
        try {
            mhGotYieldStatement.invokeExact();
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotEmptyStatement() {
        try {
            mhGotEmptyStatement.invokeExact();
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotAssert() {
        try {
            mhGotAssert.invokeExact();
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Annotations
    @Override
    public void gotAnnotation(List<LocatableToken> annName, boolean paramsFollow) {
        try {
            mhGotAnnotation.invokeExact(annName, paramsFollow);
            skipToLastToken(annName);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Lambda expressions
    @Override
    public void beginLambdaBody(boolean lambdaIsBlock, LocatableToken openCurly) {
        try {
            mhBeginLambdaBody.invokeExact(lambdaIsBlock, openCurly);
            skipToToken(openCurly);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endLambdaBody(LocatableToken closeCurly) {
        try {
            mhEndLambdaBody.invokeExact(closeCurly);
            skipToToken(closeCurly);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotLambdaFormalParam() {
        try {
            mhGotLambdaFormalParam.invokeExact();
            // No tokens to skip to
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotLambdaFormalName(LocatableToken name) {
        try {
            mhGotLambdaFormalName.invokeExact(name);
            skipToToken(name);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotLambdaFormalType(List<LocatableToken> type) {
        try {
            mhGotLambdaFormalType.invokeExact(type);
            skipToLastToken(type);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Formal parameters
    @Override
    public void beginFormalParameter(LocatableToken token) {
        try {
            mhBeginFormalParameter.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotArrayTypeIdentifier(LocatableToken token) {
        try {
            mhGotArrayTypeIdentifier.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotParentIdentifier(LocatableToken token) {
        try {
            mhGotParentIdentifier.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Record declarations
    @Override
    public void beginRecordParameters(LocatableToken parenToken) {
        try {
            mhBeginRecordParameters.invokeExact(parenToken);
            skipToToken(parenToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void gotRecordParameter(LocatableToken first, LocatableToken idToken, LocatableToken varargsToken) {
        try {
            mhGotRecordParameter.invokeExact(first, idToken, varargsToken);
            skipToToken(varargsToken != null ? varargsToken : idToken);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    @Override
    public void endRecordParameters(LocatableToken closeParen) {
        try {
            mhEndRecordParameters.invokeExact(closeParen);
            skipToToken(closeParen);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Comments
    @Override
    public void gotComment(LocatableToken token) {
        try {
            mhGotComment.invokeExact(token);
            skipToToken(token);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    // Error handling
    @Override
    public void error(String msg, int beginLine, int beginCol, int endLine, int endCol) {
        try {
            mhError.invokeExact(msg, beginLine, beginCol, endLine, endCol);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    /**
     * Advances the token stream to the position of the target token and sets it as lastToken.
     * This ensures the token stream is properly positioned before recording the last token.
     *
     * @param targetToken The token to advance to and set as lastToken
     * @param included If false, advances to one token before the target (target not included in construct)
     */
    private void skipToToken(LocatableToken targetToken, boolean included) {
        if (targetToken == null) {
            return;
        }
        
        JavaTokenFilter tokenStream = target.getTokenStream();
        LocatableToken currentToken;
        LocatableToken previousToken = null;
        
        // Keep consuming tokens until we reach or pass the target token's position
        while ((currentToken = tokenStream.nextToken()) != null) {
            // Check if we've reached the target token by comparing positions
            if (currentToken.getLine() > targetToken.getLine() ||
                (currentToken.getLine() == targetToken.getLine() &&
                 currentToken.getColumn() >= targetToken.getColumn())) {
                if (!included) {
                    tokenStream.pushBack(currentToken);
                    currentToken = previousToken;
                }

                break;
            }
            // Consume this token and continue
            previousToken = currentToken;
        }
        
        // Set lastToken: if included use target, otherwise use token before it
        if (currentToken == null) {
            target.setLastToken(currentToken);
        }
    }
    
    /**
     * Advances the token stream to the target token and sets it as lastToken (token IS included).
     */
    private void skipToToken(LocatableToken targetToken) {
        skipToToken(targetToken, true);
    }
    
    /**
     * Advances the token stream to the last token in a list and sets it as lastToken.
     *
     * @param tokens List of tokens to process
     */
    private void skipToLastToken(List<LocatableToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        skipToToken(tokens.get(tokens.size() - 1), true);
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
}
    