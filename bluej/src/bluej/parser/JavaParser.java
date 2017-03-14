/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014,2016,2017  Michael Kolling and John Rosenberg
 
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
package bluej.parser;

import static bluej.parser.JavaErrorCodes.*;
import static bluej.parser.lexer.JavaTokenTypes.*;

import java.io.Reader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import bluej.parser.lexer.JavaLexer;
import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;


/**
 * Base class for Java parsers.
 * 
 * <p>We parse the source, and when we see certain constructs we call a corresponding method
 * which subclasses can override (for instance, beginForLoop, beginForLoopBody, endForLoop).
 * In general it is arranged so that a call to beginXYZ() is always followed by a call to
 * endXYZ(). 
 * 
 * @author Davin McCall
 */
public class JavaParser
{
    protected JavaTokenFilter tokenStream;
    protected LocatableToken lastToken;

    public static TokenStream getLexer(Reader r)
    {
        return new JavaLexer(r);
    }
    
    private static TokenStream getLexer(Reader r, boolean handleComments)
    {
        return new JavaLexer(r, handleComments);
    }
    
    private static TokenStream getLexer(Reader r, int line, int col, int pos)
    {
        return new JavaLexer(r, line, col, pos);
    }
    
    public JavaParser(Reader r)
    {
        TokenStream lexer = getLexer(r);
        tokenStream = new JavaTokenFilter(lexer, this);
    }
    
    public JavaParser(Reader r, boolean handleComments)
    {
        TokenStream lexer = getLexer(r, handleComments);
        tokenStream = new JavaTokenFilter(lexer, this);
    }
    
    public JavaParser(Reader r, int line, int col, int pos)
    {
        TokenStream lexer = getLexer(r, line, col, pos);
        tokenStream = new JavaTokenFilter(lexer, this);
    }
    
    public JavaTokenFilter getTokenStream()
    {
        return tokenStream;
    }
    
    /**
     * Get the last token seen during the previous parse.
     * Many parser methods return after having read a complete structure (such as a class definition). This
     * method allows the retrieval of the last token which was actually part of the structure.
     */
    public LocatableToken getLastToken()
    {
        return lastToken;
    }

    /**
     * Signal a parse error, occurring because the next token in the token stream is
     * not valid in the current context.
     * 
     * @param msg A message/code describing the error
     */
    private void error(String msg)
    {
        errorBehind(msg, lastToken);
    }
    
    /**
     * Signal a parser error, occurring because the given token in the token stream is
     * not valid in the current context, but for which a useful error diagnosis can be
     * provided. The entire token will be highlighted as erroneous.
     * 
     * @param msg    A message/code describing the error
     * @paran token  The invalid token
     */
    private void error(String msg, LocatableToken token)
    {
        error(msg, token.getLine(), token.getColumn(), token.getEndLine(), token.getEndColumn());
    }
    
    private void errorBefore(String msg, LocatableToken token)
    {
        error(msg, token.getLine(), token.getColumn(), token.getLine(), token.getColumn());
    }
    
    private void errorBehind(String msg, LocatableToken token)
    {
        error(msg, token.getEndLine(), token.getEndColumn(), token.getEndLine(), token.getEndColumn());
    }
    
    /**
     * An error occurred during parsing. Override this method to control error behaviour.
     * @param msg A message describing the error
     * @param beginLine  The line where the erroneous token begins
     * @param beginCol   The column where the erroneous token begins
     * @param endLine    The line where the erroneous token ends
     * @param endCol     The column where the erroneous token ends
     */
    protected void error(String msg, int beginLine, int beginCol, int endLine, int endCol)
    {
        throw new ParseFailure("Parse error: (" + beginLine + ":" + beginCol + ") :" + msg);
    }

    /**
     * Parse a compilation unit (from the beginning).
     */
    public void parseCU()
    {
        int state = 0;
        while (tokenStream.LA(1).getType() != JavaTokenTypes.EOF) {
            if (tokenStream.LA(1).getType() == JavaTokenTypes.SEMI) {
                nextToken();
                continue;
            }
            state = parseCUpart(state);
        }
        finishedCU(state);
    }
    
    protected void beginPackageStatement(LocatableToken token) {  }

    /** We have the package name for this source */
    protected void gotPackage(List<LocatableToken> pkgTokens) { }

    /** We've seen the semicolon at the end of a "package" statement. */
    protected void gotPackageSemi(LocatableToken token) { }

    /** Saw a modifier (public,private etc) */
    protected void gotModifier(LocatableToken token) { }
    
    /** 
     * Modifiers were consumed. This is called after the entity to which the modifiers apply
     * has been identified (eg gotTypeDef() called)
     */
    protected void modifiersConsumed() { }
    
    /** Beginning of some arbitrary grammatical element */
    protected void beginElement(LocatableToken token) { }

    /** End of some arbitrary grammatical element.
     * 
     * @param token  The end token 
     * @param included  True if the end token is part of the element; false if it is part of the next element.
     */
    protected void endElement(LocatableToken token, boolean included) { }

    /**
     * Got the beginning (opening brace) of a method or constructor body.
     */
    protected void beginMethodBody(LocatableToken token) { }
    
    /**
     * End of a method or constructor body reached.
     */
    protected void endMethodBody(LocatableToken token, boolean included) { }
    
    protected void endMethodDecl(LocatableToken token, boolean included)
    {
        endElement(token, included);
    }
        
    /**
     * Reached a compilation unit state.
     * State 1 = package statement parsed. State 2 = one or more type definitions parsed
     */
    protected void reachedCUstate(int i) { }

    /**
     * Finished parsing a compilation unit.
     * @param state Our last state: see reachedCUState for details
     */
    protected void finishedCU(int state) { }

    /** We've seen the semicolon at the end of an "import" statement */
    protected void gotImportStmtSemi(LocatableToken token)
    {
        endElement(token, true);
    }

    protected void beginForLoop(LocatableToken token) { beginElement(token); }
    
    protected void beginForLoopBody(LocatableToken token) { }
    
    protected void endForLoopBody(LocatableToken token, boolean included) { }

    protected void endForLoop(LocatableToken token, boolean included) { }
    
    protected void beginWhileLoop(LocatableToken token) { }

    protected void beginWhileLoopBody(LocatableToken token) { }

    protected void endWhileLoopBody(LocatableToken token, boolean included) { }
    
    protected void endWhileLoop(LocatableToken token, boolean included) { }
    
    protected void beginIfStmt(LocatableToken token) { }
    
    /** Begin an "if" conditional block (the part that is executed conditionally) */
    protected void beginIfCondBlock(LocatableToken token) { }
    
    protected void endIfCondBlock(LocatableToken token, boolean included) { }

    protected void gotElseIf(LocatableToken token) {}
    
    protected void endIfStmt(LocatableToken token, boolean included) { }
    
    protected void beginSwitchStmt(LocatableToken token) { }
    
    protected void beginSwitchBlock(LocatableToken token) { }
    
    protected void endSwitchBlock(LocatableToken token) { }
    
    protected void endSwitchStmt(LocatableToken token, boolean included) { }
    
    protected void beginDoWhile(LocatableToken token) { beginElement(token); }
    
    protected void beginDoWhileBody(LocatableToken token) { }
    
    protected void endDoWhileBody(LocatableToken token, boolean included) { }
    
    protected void endDoWhile(LocatableToken token, boolean included) { }
    
    protected void beginTryCatchSmt(LocatableToken token, boolean hasResource) { }
    
    protected void beginTryBlock(LocatableToken token) { }
    
    protected void endTryBlock(LocatableToken token, boolean included) { }
    
    protected void endTryCatchStmt(LocatableToken token, boolean included) { }
    
    protected void beginSynchronizedBlock(LocatableToken token) { }
    
    protected void endSynchronizedBlock(LocatableToken token, boolean included) { }
    
    /** A list of a parameters to a method or constructor */
    protected void beginArgumentList(LocatableToken token) { }
    
    /** An individual argument has ended */
    protected void endArgument() { }
    
    /** The end of the argument list has been reached. */
    protected void endArgumentList(LocatableToken token) { }
    
    /**
     * got a "new ..." expression. Will be followed by a type spec (gotTypeSpec())
     * and possibly by array size declarations, then endExprNew()
     */
    protected void gotExprNew(LocatableToken token) { }
    
    protected void endExprNew(LocatableToken token, boolean included) { }
    
    protected void beginArrayInitList(LocatableToken token) { }
    
    protected void endArrayInitList(LocatableToken token) { }
    
    /** An anonymous class body. Preceded by a type spec (see gotTypeSpec()) except in the case of an enum member body. */
    protected void beginAnonClassBody(LocatableToken token, boolean isEnumMember) { }
    
    protected void endAnonClassBody(LocatableToken token, boolean included) { }
    
    /**
     * Beginning of a statement block. This includes anonymous statement blocks, and static
     * initializer blocks
     */
    protected void beginStmtblockBody(LocatableToken token)
    {
        beginElement(token);
    }
    
    protected void endStmtblockBody(LocatableToken token, boolean included)
    {
        endElement(token, included);
    }
    
    /**
     * Begin a (possibly static) initialisation block.
     * @param first   The first token (should be either "static" or the "{")
     * @param lcurly  The "{" token which opens the block body
     */
    protected void beginInitBlock(LocatableToken first, LocatableToken lcurly) { }
    
    /**
     * End of a (possibly static) initialisation block
     * @param rcurly    The last token (should be "}")
     * @param included  True if the last token is actually a "}"
     */
    protected void endInitBlock(LocatableToken rcurly, boolean included) { }

    /** Begin the type definition body. */
    protected void beginTypeBody(LocatableToken leftCurlyToken) { }
    
    /** End of type definition body. This should be a '}' unless an error occurred */
    protected void endTypeBody(LocatableToken endCurlyToken, boolean included) { }
    
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
    protected void gotDeclBegin(LocatableToken token) { beginElement(token); }
    
    /**
     * End a declaration (unsuccessfully).
     */
    protected void endDecl(LocatableToken token) { endElement(token, false); }
    
    /**
     * Called when the current element is recognised as a type definition.
     * @param tdType  one of TYPEDEF_CLASS, _INTERFACE, _ANNOTATION or _ENUM
     */
    protected void gotTypeDef(LocatableToken firstToken, int tdType) { }

    /** Called when we have the identifier token for a class/interface/enum definition */
    protected void gotTypeDefName(LocatableToken nameToken) { }

    /** Called when we have seen the "extends" literal token */
    protected void beginTypeDefExtends(LocatableToken extendsToken) { }
    
    /** Called after we have seen the last type in an "extends" type list */
    protected void endTypeDefExtends() { }

    /** Called when we have seen the "implements" literal token */
    protected void beginTypeDefImplements(LocatableToken implementsToken) { }

    /** Called after we have seen the last type in an "implements" type list */
    protected void endTypeDefImplements() { }

    protected void gotTypeDefEnd(LocatableToken token, boolean included)
    {
        endElement(token, included);
    }
    
    /** 
     * Got a variable declaration, which might declare multiple variables. Each
     * variable will generate gotVariable() or gotSubsequentVar().
     * @param first  The first token in the declaration
     */
    protected void beginVariableDecl(LocatableToken first) { }
    
    /**
     * Got the (first) variable in a variable declaration.
     * @param first    The first token in the declaration
     * @param idToken  The token with the variable identifier
     * @param inited   Whether the variable is initialized as part of the declaration
     */
    protected void gotVariableDecl(LocatableToken first, LocatableToken idToken, boolean inited) { }

    protected void gotSubsequentVar(LocatableToken first, LocatableToken idToken, boolean inited) { }

    protected void endVariable(LocatableToken token, boolean included) { }

    protected void endVariableDecls(LocatableToken token, boolean included) { }
    
    protected void beginForInitDecl(LocatableToken first) { }
    
    protected void gotForInit(LocatableToken first, LocatableToken idToken) { }
    
    protected void gotSubsequentForInit(LocatableToken first, LocatableToken idToken, boolean initFollows) { }
    
    protected void endForInit(LocatableToken token, boolean included) { }
    
    protected void endForInitDecls(LocatableToken token, boolean included) { }
    
    /** 
     * Got a field declaration, which might declare multiple fields. Each field will generate
     * gotField() or gotSubsequentField().
     * @param first  The first token in the declaration
     *
     */
    protected void beginFieldDeclarations(LocatableToken first) { }
    
    /**
     * Got a field (inside a type definition).
     * @param first     The first token that forms part of the field declaration
     * @param idToken   The token with the name of the field.
     * @param initExpressionFollows
     */
    protected void gotField(LocatableToken first, LocatableToken idToken, boolean initExpressionFollows) { }

    protected void gotSubsequentField(LocatableToken first, LocatableToken idToken, boolean initFollows) { }
    
    /** End a single field declaration (but not necessarily the field declaration statement) */
    protected void endField(LocatableToken token, boolean included) { }
    
    /** End a field declaration statement */
    protected void endFieldDeclarations(LocatableToken token, boolean included) { }

    /** We've seen a type specification or something that looks a lot like one. */
    protected void gotTypeSpec(List<LocatableToken> tokens) { }

    /** Seen a type cast operator. The tokens list contains the type to which is cast. */
    protected void gotTypeCast(List<LocatableToken> tokens)
    {
        gotTypeSpec(tokens);
    }
    
    /** Saw the beginning of an expression */
    protected void beginExpression(LocatableToken token) { }
    
    /** Reached the end of an expression. The given token is the first one past the end. */
    protected void endExpression(LocatableToken token, boolean emptyExpression) { }
    
    /** Saw a literal as part of an expression */
    protected void gotLiteral(LocatableToken token) { }
    
    /**
     * Saw a primitive type literal in an expression; usually occurs as "int.class"
     * or "int[].class" for example.
     * @param token  The primitive token
     */
    protected void gotPrimitiveTypeLiteral(LocatableToken token) { }
    
    /** Saw an identifier as (part of) an expression */
    protected void gotIdentifier(LocatableToken token) { }
    /**
     * Got an identifier (possibly part of a compound identifier) immediately followed by
     * end of input stream.
     */
    protected void gotIdentifierEOF(LocatableToken token) { gotIdentifier(token); }
    
    protected void gotMemberAccessEOF(LocatableToken token) { gotMemberAccess(token); }
    
    protected void gotCompoundIdent(LocatableToken token) { gotIdentifier(token); }
    protected void gotCompoundComponent(LocatableToken token) { gotMemberAccess(token); }
    protected void completeCompoundValue(LocatableToken token) { gotMemberAccess(token); }
    protected void completeCompoundValueEOF(LocatableToken token) { completeCompoundValue(token); }
    protected void completeCompoundClass(LocatableToken token) { gotMemberAccess(token); }
    
    protected void gotMemberAccess(LocatableToken token) { }
    
    /** Saw a member method call (expr.methodName()), token is the method name; arguments to follow */
    protected void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs) { }
    
    /** Saw a "naked" method call - "methodName(...)" */
    protected void gotMethodCall(LocatableToken token) { }
    
    /** Saw a call to the constructor as this(...) or super(...) */
    protected void gotConstructorCall(LocatableToken token) { }
    
    /** Saw a dot operator followed by end-of-file */
    protected void gotDotEOF(LocatableToken token)
    {
        gotBinaryOperator(token);
    }
    
    protected void gotStatementExpression() { }
    
    protected void gotClassLiteral(LocatableToken token) { }
    
    /** Saw a binary operator as part of an expression */
    protected void gotBinaryOperator(LocatableToken token) { }
    
    protected void gotUnaryOperator(LocatableToken token) { }
    
    /** Saw a "?" operator. This will be followed by the left-hand-side expression
     * (demarked by beginExpression() and endExpression(), then gotQuestionColon) followed by a continuation
     * of the current expression (for the right-hand-side).
     */
    protected void gotQuestionOperator(LocatableToken token) { }

    protected void gotQuestionColon(LocatableToken token) { }
    
    /**
     * Saw the "instanceof" operator. The type spec will follow.
     */
    protected void gotInstanceOfOperator(LocatableToken token) { }
    
    protected void gotArrayElementAccess() { }
    
    protected void gotImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken) { }
    
    protected void gotWildcardImport(List<LocatableToken> tokens, boolean isStatic, LocatableToken importToken, LocatableToken semiColonToken) { }
    
    /**
     * We've seen a constructor declaration. The token supplied is the constructor name.
     * The hiddenToken is the comment before the constructor.
     */
    protected void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken) {}

    /**
     * We've seen a method declaration; the token parameter is the method name;
     * the hiddenToken parameter is the comment before the method
     */
    protected void gotMethodDeclaration(LocatableToken token, LocatableToken hiddenToken) {}

    /** 
     * We saw a method (or constructor) parameter. The given token specifies the parameter name. 
     * The last type parsed by parseTypeSpec(boolean) is the parameter type, after any additonal
     * array declarators (see gotArrayDeclarator()) are applied.
     * 
     * @param token   The token giving the parameter name
     * @param ellipsisToken  The token, if any, with the ellipsis indicating a varargs parameter. May be null.
     */
    protected void gotMethodParameter(LocatableToken token, LocatableToken ellipsisToken) { }
    
    /**
     * Called when, after a parameter/field/variable name, array declarators "[]" are seen.
     * Will be called once for each set of "[]", immediately before gotField() or equivalent
     * is called.
     */
    protected void gotArrayDeclarator() { }

    /**
     * Called for the array components when we get "new xyz[]".
     */
    protected void gotNewArrayDeclarator(boolean withDimension) { }
    
    protected void gotAllMethodParameters() { }
    
    /**
     * Saw a type parameter for a class or method. If for a method, will be bracketed by
     * calls to {@code gotMethodTypeParamsBegin} and {@code endMethodTypeParams}
     * @param idToken  The token with the type parameter identifier
     */
    protected void gotTypeParam(LocatableToken idToken) { }
    
    protected void gotTypeParamBound(List<LocatableToken> tokens) { }

    protected void gotMethodTypeParamsBegin() { }
    
    protected void endMethodTypeParams() { }

    /**
     * Called by the lexer when it sees a comment.
     */
    public void gotComment(LocatableToken token) { }
    
    /**
     * Check whether a particular token is a type declaration initiator, i.e "class", "interface"
     * or "enum"
     */
    public boolean isTypeDeclarator(LocatableToken token)
    {
        return token.getType() == JavaTokenTypes.LITERAL_class
        || token.getType() == JavaTokenTypes.LITERAL_enum
        || token.getType() == JavaTokenTypes.LITERAL_interface;
    }

    /**
     * Check whether a token is a primitive type - "int" "float" etc
     */
    public static boolean isPrimitiveType(LocatableToken token)
    {
        return token.getType() == JavaTokenTypes.LITERAL_void
        || token.getType() == JavaTokenTypes.LITERAL_boolean
        || token.getType() == JavaTokenTypes.LITERAL_byte
        || token.getType() == JavaTokenTypes.LITERAL_char
        || token.getType() == JavaTokenTypes.LITERAL_short
        || token.getType() == JavaTokenTypes.LITERAL_int
        || token.getType() == JavaTokenTypes.LITERAL_long
        || token.getType() == JavaTokenTypes.LITERAL_float
        || token.getType() == JavaTokenTypes.LITERAL_double;
    }

    public static final int TYPEDEF_CLASS = 0;
    public static final int TYPEDEF_INTERFACE = 1;
    public static final int TYPEDEF_ENUM = 2;
    public static final int TYPEDEF_ANNOTATION = 3;
    /** looks like a type definition, but has an error */
    public static final int TYPEDEF_ERROR = 4;
    /** doesn't parse as a type definition at all */
    public static final int TYPEDEF_EPIC_FAIL = 5;

    /**
     * Get the next token from the token stream.
     */
    protected final LocatableToken nextToken()
    {
        lastToken = tokenStream.nextToken();
        return lastToken;
    }

    public final int parseCUpart(int state)
    {
        LocatableToken token = nextToken();
        if (token.getType() == JavaTokenTypes.LITERAL_package) {
            if (state != 0) {
                error("Only one 'package' statement is allowed", token);
            }
            token = parsePackageStmt(token);
            reachedCUstate(1); state = 1;
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_import) {
            parseImportStatement(token);
            reachedCUstate(1); state = 1;
        }
        else if (isModifier(token) || isTypeDeclarator(token)) {
            // optional: class/interface/enum
            gotTopLevelDecl(token);
            gotDeclBegin(token);
            tokenStream.pushBack(token);
            parseModifiers();
            parseTypeDef(token);
            reachedCUstate(2); state = 2;
        }
        else if (token.getType() == JavaTokenTypes.EOF) {
            return state;
        }
        else {
            // TODO give different diagnostic depending on state
            error("Expected: Type definition (class, interface or enum)", token);
        }
        return state;
    }

    protected void gotTopLevelDecl(LocatableToken token)
    {
    }

    /**
     * Parse a "package xyz;"-type statement. The "package"-literal token must have already
     * been read from the token stream.
     */
    public final LocatableToken parsePackageStmt(LocatableToken token)
    {
        beginPackageStatement(token);
        token = nextToken();
        if (token.getType() != JavaTokenTypes.IDENT) {
            error("Expected identifier after 'package'");
            return null;
        }
        List<LocatableToken> pkgTokens = parseDottedIdent(token);
        gotPackage(pkgTokens);
        LocatableToken lastPkgToken = lastToken;
        token = nextToken();
        if (token.getType() != JavaTokenTypes.SEMI) {
            tokenStream.pushBack(token);
            error(BJ003, lastPkgToken.getEndLine(), lastPkgToken.getEndColumn(),
                    lastPkgToken.getEndLine(), lastPkgToken.getEndColumn());
            return null;
        }
        else {
            gotPackageSemi(token);
            return token;
        }
    }
    
    /**
     * Parse an import statement.
     */
    public void parseImportStatement()
    {
        LocatableToken token = nextToken();
        if (token.getType() == JavaTokenTypes.LITERAL_import) {
            parseImportStatement(token);
        }
        else {
            error("Import statements must start with \"import\".");
        }
    }
    
    public void parseImportStatement(final LocatableToken importToken)
    {
        LocatableToken token = importToken;
        beginElement(token);
        boolean isStatic = false;
        token = tokenStream.nextToken();
        if (token.getType() == JavaTokenTypes.LITERAL_static) {
            isStatic = true;
            token = tokenStream.nextToken();
        }
        if (token.getType() != JavaTokenTypes.IDENT) {
            tokenStream.pushBack(token);
            error("Expecting identifier (package containing element to be imported)");
            endElement(token, false);
            return;
        }
        
        List<LocatableToken> tokens = parseDottedIdent(token);
        LocatableToken lastIdentToken = lastToken;
        if (tokenStream.LA(1).getType() == JavaTokenTypes.DOT) {
            LocatableToken lastToken = nextToken(); // DOT
            token = nextToken();
            if (token.getType() == JavaTokenTypes.SEMI) {
                error("Trailing '.' in import statement", lastToken.getLine(), lastToken.getColumn(),
                        lastToken.getEndLine(), lastToken.getEndColumn());
            }
            else if (token.getType() == JavaTokenTypes.STAR) {
                lastToken = token;
                token = nextToken();
                if (token.getType() != JavaTokenTypes.SEMI) {
                    tokenStream.pushBack(token);
                    error("Expected ';' following import statement", lastToken.getEndLine(), lastToken.getEndColumn(),
                            lastToken.getEndLine(), lastToken.getEndColumn());
                }
                else {
                    gotWildcardImport(tokens, isStatic, importToken, token);
                    gotImportStmtSemi(token);
                }
            }
            else {
                error("Expected package/class identifier, or '*', in import statement.");
                if (tokenStream.LA(1).getType() == JavaTokenTypes.SEMI) {
                    nextToken();
                }
            }
        }
        else {
            token = nextToken();
            if (token.getType() != JavaTokenTypes.SEMI) {
                tokenStream.pushBack(token);
                error("Expected ';' following import statement", lastIdentToken.getEndLine(), lastIdentToken.getEndColumn(),
                        lastIdentToken.getEndLine(), lastIdentToken.getEndColumn());
            }
            else {
                gotImport(tokens, isStatic, importToken, token);
                gotImportStmtSemi(token);
            }
        }
    }
    
    /**
     * Parse a type definition (class, interface, enum).
     * Returns with {@code lastToken} set to the last token seen as part of the definition.
     */
    public final void parseTypeDef()
    {
        parseModifiers();
        parseTypeDef(tokenStream.LA(1));
    }
    
    /**
     * Parse a type definition (class, interface, enum).
     * Returns with {@code lastToken} set to the last token seen as part of the definition.
     * 
     * @param firstToken  the first token of the type definition, which might still be in the token
     *                    stream, or which might be a modifier already read.
     */
    public final void parseTypeDef(LocatableToken firstToken)
    {
        int tdType = parseTypeDefBegin();
        if (tdType != TYPEDEF_EPIC_FAIL) {
            gotTypeDef(firstToken, tdType);
        }
        modifiersConsumed();
        if (tdType == TYPEDEF_EPIC_FAIL) {
            endDecl(tokenStream.LA(1));
            return;
        }
        
        // Class name
        LocatableToken token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.IDENT) {
            tokenStream.pushBack(token);
            gotTypeDefEnd(token, false);
            error("Expected identifier (in type definition)");
            return;
        }
        gotTypeDefName(token);

        token = parseTypeDefPart2();

        // Body!
        if (token == null) {
            gotTypeDefEnd(tokenStream.LA(1), false);
            return;
        }

        lastToken = parseTypeBody(tdType, token);
        gotTypeDefEnd(lastToken, lastToken.getType() == JavaTokenTypes.RCURLY);
    }
    
    /**
     * Parse a type body. Returns the last seen token, which might be the '}' closing the
     * type body or might be something else (if there is a parse error).
     * 
     * @param tdType  the type of the type definition (TYPEDEF_ constant specifying class, 
     *                interface, enum, annotation)
     * @param token  the '{' token opening the type body
     */
    public final LocatableToken parseTypeBody(int tdType, LocatableToken token)
    {
        beginTypeBody(token);

        if (tdType == TYPEDEF_ENUM) {
            parseEnumConstants();
        }
        parseClassBody();

        token = nextToken();
        if (token.getType() != JavaTokenTypes.RCURLY) {
            error("Expected '}' (in class definition)");
        }

        endTypeBody(token, token.getType() == JavaTokenTypes.RCURLY);
        return token;
    }
    
    // Possibilities:
    // 1 - parses ok, body should follow
    //       - class/interface TYPEDEF_CLAS / TYPEDEF_INTERFACE
    //       - enum            TYPEDEF_ENUM
    //       - annotation      TYPEDEF_ANNOTATION
    // 2 - doesn't even look like a type definition (TYPEDEF_EPIC_FAIL)
    public final int parseTypeDefBegin()
    {
        parseModifiers();
        LocatableToken token = nextToken();
        
        boolean isAnnotation = token.getType() == JavaTokenTypes.AT;
        if (isAnnotation) {
            LocatableToken tdToken = nextToken();
            if (tdToken.getType() != JavaTokenTypes.LITERAL_interface) {
                error("Expected 'interface' after '@' in interface definition");
                tokenStream.pushBack(tdToken);
                return TYPEDEF_EPIC_FAIL;
            }
            token = tdToken;
        }
        
        if (isTypeDeclarator(token)) {
            int tdType = -1;
            if (token.getType() == JavaTokenTypes.LITERAL_class) {
                tdType = TYPEDEF_CLASS;
            }
            else if (token.getType() == JavaTokenTypes.LITERAL_interface) {
                tdType = TYPEDEF_INTERFACE;
                //check for annotation type
                if(isAnnotation) {
                    tdType = TYPEDEF_ANNOTATION;                                                 
                }
            }
            else {
                tdType = TYPEDEF_ENUM;
            }
            
            return tdType;
        }
        else {
            error("Expected type declarator: 'class', 'interface', or 'enum'");
            return TYPEDEF_EPIC_FAIL;
        }
    }
    
    /**
     * Parse the part of a type definition after the name - the type parameters,
     * extended classes/interfaces and implemented interfaces. Returns the '{' token
     * (which begins the type definition body) on success or null on failure.
     */
    public LocatableToken parseTypeDefPart2()
    {
        // template arguments
        LocatableToken token = nextToken();
        if (token.getType() == JavaTokenTypes.LT) {
            parseTypeParams();
            token = tokenStream.nextToken();
        }

        // extends...
        if (token.getType() == JavaTokenTypes.LITERAL_extends) {
            beginTypeDefExtends(token);
            do {
                parseTypeSpec(true);
                token = nextToken();
            }
            while (token.getType() == JavaTokenTypes.COMMA);
            
            if (token.getType() == JavaTokenTypes.DOT) {
                // Incomplete type spec
                error("Incomplete type specificiation", token);
                // Don't push the token back on the token stream - it really is part of the type
                return null;
            }
            endTypeDefExtends();
        }

        // implements...
        if (token.getType() == JavaTokenTypes.LITERAL_implements) {
            beginTypeDefImplements(token);
            do {
                parseTypeSpec(true);
                token = nextToken();
            }
            while (token.getType() == JavaTokenTypes.COMMA);

            if (token.getType() == JavaTokenTypes.DOT) {
                // Incomplete type spec
                error("Incomplete type specificiation", token);
                // Don't push the token back on the token stream - it really is part of the type
                return null;
            }
            endTypeDefImplements();
        }
        
        if (token.getType() == JavaTokenTypes.LCURLY) {
            return token;
        }
        else {
            tokenStream.pushBack(token);
            error("Expected '{' (in type definition)");
            return null;
        }
    }
        
    public void parseEnumConstants()
    {
        LocatableToken token = nextToken();
        while (token.getType() == JavaTokenTypes.IDENT) {
            // The identifier is the constant name - there may be constructor arguments as well
            token = nextToken();
            if (token.getType() == JavaTokenTypes.LPAREN) {
                parseArgumentList(token);
                token = nextToken();
            }
            
            // "body"
            if (token.getType() == JavaTokenTypes.LCURLY) {
                beginAnonClassBody(token, true);
                parseClassBody();
                token = nextToken();
                if (token.getType() != JavaTokenTypes.RCURLY) {
                    error("Expected '}' at end of enum constant body");
                    endAnonClassBody(token, false);
                }
                else {
                    endAnonClassBody(token, true);
                    token = nextToken();
                }
            }

            if (token.getType() == JavaTokenTypes.SEMI) {
                return;
            }

            if (token.getType() == JavaTokenTypes.RCURLY) {
                // This is valid
                tokenStream.pushBack(token);
                return;
            }

            if (token.getType() != JavaTokenTypes.COMMA) {
                error("Expecting ',' or ';' after enum constant declaration");
                tokenStream.pushBack(token);
                return;
            }
            token = nextToken();
        }
    }
        
    /**
     * Parse formal type parameters. The opening '<' should have been read already.
     */
    public void parseTypeParams()
    {
        DepthRef dr = new DepthRef();
        dr.depth = 1;

        while (true) {
            LocatableToken idToken = nextToken();
            if (idToken.getType() != JavaTokenTypes.IDENT) {
                error("Expected identifier (in type parameter list)");
                tokenStream.pushBack(idToken);
                return;
            }
            gotTypeParam(idToken);

            LocatableToken token = nextToken();
            if (token.getType() == JavaTokenTypes.LITERAL_extends) {
                do {
                    LinkedList<LocatableToken> boundTokens = new LinkedList<LocatableToken>();
                    if (parseTargType(false, boundTokens, dr)) {
                        gotTypeParamBound(boundTokens);
                    }
                    if (dr.depth <= 0) {
                        return;
                    }
                    token = nextToken();
                } while (token.getType() == JavaTokenTypes.BAND);
            }

            if (token.getType() != JavaTokenTypes.COMMA) {
                if (token.getType() != JavaTokenTypes.GT) {
                    error("Expecting '>' at end of type parameter list");
                    tokenStream.pushBack(token);
                }
                break;
            }
        }
    }

    /**
     * Check whether a token represents a modifier (or an "at" symbol,
     * denoting an annotation).
     */
    public static boolean isModifier(LocatableToken token)
    {
        int tokType = token.getType();
        return (tokType == JavaTokenTypes.LITERAL_public
                || tokType == JavaTokenTypes.LITERAL_private
                || tokType == JavaTokenTypes.LITERAL_protected
                || tokType == JavaTokenTypes.ABSTRACT
                || tokType == JavaTokenTypes.FINAL
                || tokType == JavaTokenTypes.LITERAL_static
                || tokType == JavaTokenTypes.LITERAL_volatile
                || tokType == JavaTokenTypes.LITERAL_native
                || tokType == JavaTokenTypes.STRICTFP
                || tokType == JavaTokenTypes.LITERAL_transient
                || tokType == JavaTokenTypes.LITERAL_synchronized
                || tokType == JavaTokenTypes.AT
                || tokType == JavaTokenTypes.LITERAL_default);
    }

    /**
     * Parse a modifier list (and return all modifier tokens in a list)
     */
    public List<LocatableToken> parseModifiers()
    {
        List<LocatableToken> rval = new LinkedList<LocatableToken>();
        
        LocatableToken token = tokenStream.nextToken();
        while (isModifier(token)) {
            if (token.getType() == JavaTokenTypes.AT) {
                if( tokenStream.LA(1).getType() == JavaTokenTypes.IDENT) {
                    lastToken = token;
                    parseAnnotation();
                }
                else {
                    tokenStream.pushBack(token);
                    return rval;
                }
            }
            else {
                gotModifier(token);
            }
            lastToken = token;
            rval.add(token);
            token = nextToken();
        }                       
        tokenStream.pushBack(token);
        
        return rval;
    }

    /**
     * Having seen '{', parse the rest of a class body.
     */
    public void parseClassBody()
    {
        LocatableToken token = tokenStream.nextToken();
        while (token.getType() != JavaTokenTypes.RCURLY) {
            if (token.getType() == JavaTokenTypes.EOF) {
                error("Unexpected end-of-file in type body; missing '}'", token);
                return;
            }
            parseClassElement(token);
            token = nextToken();
        }
        tokenStream.pushBack(token);
    }
    
    public final void parseClassElement(LocatableToken token)
    {
        if (token.getType() == JavaTokenTypes.SEMI) {
            // A spurious semicolon.
            return;
        }
        
        gotDeclBegin(token);
        tokenStream.pushBack(token);
        LocatableToken hiddenToken = token.getHiddenBefore();
        
        // field declaration, method declaration, inner class
        List<LocatableToken> modifiers = parseModifiers();
        LocatableToken firstMod = null;
        if (! modifiers.isEmpty()) {
            firstMod = modifiers.get(0);
        }
        
        token = nextToken();
        if (token.getType() == JavaTokenTypes.LITERAL_class
                || token.getType() == JavaTokenTypes.LITERAL_interface
                || token.getType() == JavaTokenTypes.LITERAL_enum
                || token.getType() == JavaTokenTypes.AT) {
            gotInnerType(token);
            tokenStream.pushBack(token);
            parseTypeDef(firstMod != null ? firstMod : token);
        }
        else {
            // Not an inner type: should be a method/constructor or field,
            // or (possibly static) a initialisation block
            if (token.getType() == JavaTokenTypes.LCURLY) {
                // initialisation block
                LocatableToken firstToken = firstMod == null ? token : firstMod;
                beginInitBlock(firstToken, token);
                modifiersConsumed();
                parseStmtBlock();
                token = nextToken();
                if (token.getType() != JavaTokenTypes.RCURLY) {
                    error("Expecting '}' (at end of initialisation block)");
                    tokenStream.pushBack(token);
                    endInitBlock(token, false);
                    endElement(token, false);
                }
                else {
                    endInitBlock(token, true);
                    endElement(token, true);
                }
            }
            else if (token.getType() == JavaTokenTypes.LT
                    || token.getType() == JavaTokenTypes.IDENT
                    || isPrimitiveType(token)) {
                // method/constructor, field
                LocatableToken first = firstMod != null ? firstMod : token;
                if (token.getType() == JavaTokenTypes.LT) {
                    // generic method
                    gotMethodTypeParamsBegin();
                    parseTypeParams();
                    endMethodTypeParams();
                }
                else {
                    tokenStream.pushBack(token);
                }
                // Might be a constructor:
                boolean isConstructor = tokenStream.LA(1).getType() == JavaTokenTypes.IDENT
                        && tokenStream.LA(2).getType() == JavaTokenTypes.LPAREN;
                if (!isConstructor && !parseTypeSpec(true)) {
                    endDecl(tokenStream.LA(1));
                    return;
                }
                LocatableToken idToken = tokenStream.nextToken(); // identifier
                if (idToken.getType() != JavaTokenTypes.IDENT) {
                    modifiersConsumed();
                    tokenStream.pushBack(idToken);
                    errorBefore("Expected identifier (method or field name).", idToken);
                    endDecl(idToken);
                    return;
                }

                token = nextToken();
                int ttype = token.getType();
                if (ttype == JavaTokenTypes.LBRACK || ttype == JavaTokenTypes.SEMI
                        || ttype == JavaTokenTypes.ASSIGN || ttype == JavaTokenTypes.COMMA) {
                    // This must be a field declaration
                    beginFieldDeclarations(first);
                    if (ttype == JavaTokenTypes.LBRACK) {
                        tokenStream.pushBack(token);
                        parseArrayDeclarators();
                        token = nextToken();
                        ttype = token.getType();
                    }
                    gotField(first, idToken, ttype == JavaTokenTypes.ASSIGN);
                    if (ttype == JavaTokenTypes.SEMI) {
                        endField(token, true);
                        endFieldDeclarations(token, true);
                    }
                    else if (ttype == JavaTokenTypes.ASSIGN) {
                        parseExpression();
                        parseSubsequentDeclarations(DECL_TYPE_FIELD, true);
                    }
                    else if (ttype == JavaTokenTypes.COMMA) {
                        tokenStream.pushBack(token);
                        parseSubsequentDeclarations(DECL_TYPE_FIELD, true);
                    }
                    else {
                        error("Expected ',', '=' or ';' after field declaration");
                        tokenStream.pushBack(token);
                        endField(token, false);
                        endFieldDeclarations(token, false);
                    }
                    modifiersConsumed();
                }
                else if (ttype == JavaTokenTypes.LPAREN) {
                    // method declaration
                    if (isConstructor) {
                        gotConstructorDecl(idToken, hiddenToken);
                    }
                    else {
                        gotMethodDeclaration(idToken, hiddenToken);
                    }
                    modifiersConsumed();
                    parseMethodParamsBody();
                }
                else {
                    modifiersConsumed();
                    tokenStream.pushBack(token);
                    error("Expected ';' or '=' or '(' (in field or method declaration).");
                    endDecl(token);
                }
            }
            else {
                error("Unexpected token \"" + token.getText() + "\" in type declaration body");
                endDecl(tokenStream.LA(1));
            }
        }
        
    }

    protected void gotInnerType(LocatableToken start)
    {
    }

    protected void parseArrayDeclarators()
    {
        if (tokenStream.LA(1).getType() != JavaTokenTypes.LBRACK) {
            return;
        }

        LocatableToken token = nextToken();
        while (token.getType() == JavaTokenTypes.LBRACK) {
            token = nextToken();
            if (token.getType() != JavaTokenTypes.RBRACK) {
                errorBefore("Expecting ']' (to match '[')", token);
                if (tokenStream.LA(1).getType() == JavaTokenTypes.RBRACK) {
                    // Try and recover
                    token = nextToken(); // ']'
                }
                else {
                    tokenStream.pushBack(token);
                    return;
                }
            }
            gotArrayDeclarator();
            token = nextToken();
        }
        tokenStream.pushBack(token);
    }
        
    /**
     * We've got the return type, name, and opening parenthesis of a method/constructor
     * declaration. Parse the rest.
     */
    public void parseMethodParamsBody()
    {
        parseParameterList();
        gotAllMethodParameters();
        LocatableToken token = nextToken();
        if (token.getType() != JavaTokenTypes.RPAREN) {
            error("Expected ')' at end of parameter list (in method declaration)");
            tokenStream.pushBack(token);
            endMethodDecl(token, false);
            return;
        }
        token = nextToken();
        if (token.getType() == JavaTokenTypes.LITERAL_throws) {
            beginThrows(token);
            do {
                parseTypeSpec(true);
                token = nextToken();
            } while (token.getType() == JavaTokenTypes.COMMA);
            endThrows();
        }
        if (token.getType() == JavaTokenTypes.LCURLY) {
            // method body
            beginMethodBody(token);
            parseStmtBlock();
            token = nextToken();
            if (token.getType() != JavaTokenTypes.RCURLY) {
                error("Expected '}' at end of method body");
                tokenStream.pushBack(token);
                endMethodBody(token, false);
                endMethodDecl(token, false);
            }
            else {
                endMethodBody(token, true);
                endMethodDecl(token, true);
            }
            return;
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_default) {
            parseExpression();
            token = nextToken();
        }
        
        if (token.getType() != JavaTokenTypes.SEMI) {
            tokenStream.pushBack(token);
            error(BJ000);
            endMethodDecl(token, false);
        }
        else {
            endMethodDecl(token, true);
        }
    }

    protected void beginThrows(LocatableToken token) { }
    protected void endThrows() { }

    /**
     * Parse a statement block - such as a method body. The opening curly brace should already be consumed.
     * On return the closing curly (if present) remains in the token stream.
     */
    public void parseStmtBlock()
    {
        while(true) {
            LocatableToken token = nextToken();
            if (token.getType() == JavaTokenTypes.EOF
                    || token.getType() == JavaTokenTypes.RCURLY) {
                tokenStream.pushBack(token);
                return;
            }
            beginElement(token);
            LocatableToken ntoken = parseStatement(token, false);
            if (ntoken != null) {
                endElement(ntoken, true);
            }
            else {
                ntoken = tokenStream.LA(1);
                endElement(tokenStream.LA(1), false);
                if (ntoken == token) {
                    nextToken();
                    error("Invalid beginning of statement.", token);
                    continue;
                    // TODO we can just skip the token and keep processing, but we should be
                    // context aware. For instance if token is "catch" and we are in a try block,
                    // should bail out altogether now so that processing can continue upstream.
                }
            }
        }
    }

    public void parseStatement()
    {
        parseStatement(nextToken(), false);
    }

    private static int [] statementTokenIndexes = new int[JavaTokenTypes.INVALID + 1];
    
    static {
        statementTokenIndexes[JavaTokenTypes.SEMI] = 1;
        statementTokenIndexes[JavaTokenTypes.LITERAL_return] = 2;
        statementTokenIndexes[JavaTokenTypes.LITERAL_for] = 3;
        statementTokenIndexes[JavaTokenTypes.LITERAL_while] = 4;
        statementTokenIndexes[JavaTokenTypes.LITERAL_if] = 5;
        statementTokenIndexes[JavaTokenTypes.LITERAL_do] = 6;
        statementTokenIndexes[JavaTokenTypes.LITERAL_assert] = 7;
        statementTokenIndexes[JavaTokenTypes.LITERAL_switch] = 8;
        statementTokenIndexes[JavaTokenTypes.LITERAL_case] = 9;
        statementTokenIndexes[JavaTokenTypes.LITERAL_default] = 10;
        statementTokenIndexes[JavaTokenTypes.LITERAL_continue] = 11;
        statementTokenIndexes[JavaTokenTypes.LITERAL_break] = 12;
        statementTokenIndexes[JavaTokenTypes.LITERAL_throw] = 13;
        statementTokenIndexes[JavaTokenTypes.LITERAL_try] = 14;
        statementTokenIndexes[JavaTokenTypes.IDENT] = 15;
        statementTokenIndexes[JavaTokenTypes.LITERAL_synchronized] = 16;
        
        // Modifiers
        statementTokenIndexes[JavaTokenTypes.LITERAL_public] = 17;
        statementTokenIndexes[JavaTokenTypes.LITERAL_private] = 18;
        statementTokenIndexes[JavaTokenTypes.LITERAL_protected] = 19;
        statementTokenIndexes[JavaTokenTypes.ABSTRACT] = 20;
        statementTokenIndexes[JavaTokenTypes.FINAL] = 21;
        statementTokenIndexes[JavaTokenTypes.LITERAL_static] = 22;
        statementTokenIndexes[JavaTokenTypes.LITERAL_volatile] = 23;
        statementTokenIndexes[JavaTokenTypes.LITERAL_native] = 24;
        statementTokenIndexes[JavaTokenTypes.STRICTFP] = 25;
        statementTokenIndexes[JavaTokenTypes.LITERAL_transient] = 26;
        // statementTokenIndexes[JavaTokenTypes.LITERAL_synchronized] = 27;
        statementTokenIndexes[JavaTokenTypes.AT] = 27;
        
        // type declarators
        statementTokenIndexes[JavaTokenTypes.LITERAL_class] = 28;
        statementTokenIndexes[JavaTokenTypes.LITERAL_enum] = 29;
        statementTokenIndexes[JavaTokenTypes.LITERAL_interface] = 30;
        
        // primitive types
        statementTokenIndexes[JavaTokenTypes.LITERAL_void] = 31;
        statementTokenIndexes[JavaTokenTypes.LITERAL_boolean] = 32;
        statementTokenIndexes[JavaTokenTypes.LITERAL_byte] = 33;
        statementTokenIndexes[JavaTokenTypes.LITERAL_char] = 34;
        statementTokenIndexes[JavaTokenTypes.LITERAL_short] = 35;
        statementTokenIndexes[JavaTokenTypes.LITERAL_int] = 36;
        statementTokenIndexes[JavaTokenTypes.LITERAL_long] = 37;
        statementTokenIndexes[JavaTokenTypes.LITERAL_float] = 38;
        statementTokenIndexes[JavaTokenTypes.LITERAL_double] = 39;
        
        statementTokenIndexes[JavaTokenTypes.LCURLY] = 40;
    }
    
    /**
     * Parse a statement. Return the last token that is part of the statement (i.e the ';' or '}'
     * terminator), or null if an error was encountered.
     * 
     * @param token  The first token of the statement
     * @param allowComma  if true, allows multiple statements separated by commas. Each
     *                    statement will be parsed.
     */
    public LocatableToken parseStatement(LocatableToken token, boolean allowComma)
    {
        while (true) {
            switch (statementTokenIndexes[token.getType()]) {
            case 1: // SEMI
                gotEmptyStatement();
                return token; // empty statement
            case 2: // LITERAL_return
                token = nextToken();
                gotReturnStatement(token.getType() != JavaTokenTypes.SEMI);
                if (token.getType() != JavaTokenTypes.SEMI) {
                    tokenStream.pushBack(token);
                    parseExpression();
                    token = nextToken();
                }
                if (token.getType() != JavaTokenTypes.SEMI) {
                    tokenStream.pushBack(token);
                    error(BJ003);
                    return null;
                }
                return token;
            case 3: // LITERAL_for
                return parseForStatement(token);
            case 4: // LITERAL_while
                return parseWhileStatement(token);
            case 5: // LITERAL_if    
                return parseIfStatement(token);
            case 6: // LITERAL_do
                return parseDoWhileStatement(token);
            case 7: // LITERAL_assert
                return parseAssertStatement(token);
            case 8: // LITERAL_switch
                return parseSwitchStatement(token);
            case 9: // LITERAL_case
                gotSwitchCase();
                parseExpression();
                token = nextToken();
                if (token.getType() != JavaTokenTypes.COLON) {
                    error("Expecting ':' at end of case expression");
                    tokenStream.pushBack(token);
                    return null;
                }
                return token;
            case 10: // LITERAL_default
                gotSwitchDefault();
                token = nextToken();
                if (token.getType() != JavaTokenTypes.COLON) {
                    error("Expecting ':' at end of case expression");
                    tokenStream.pushBack(token);
                    return null;
                }
                return token;
            case 11: // LITERAL_continue
            case 12: // LITERAL_break
                // There might be a label afterwards
                LocatableToken keywordToken = token;
                token = nextToken();
                if (token.getType() == JavaTokenTypes.IDENT) {
                    token = nextToken();
                    gotBreakContinue(keywordToken, token);
                }
                else
                {
                    gotBreakContinue(keywordToken, null);
                }
                if (token.getType() != JavaTokenTypes.SEMI) {
                    tokenStream.pushBack(token);
                    error(BJ003);
                    return null;
                }
                return token;
            case 13: // LITERAL_throw
                gotThrow(token);
                parseExpression();
                token = nextToken();
                if (token.getType() != JavaTokenTypes.SEMI) {
                    tokenStream.pushBack(token);
                    error(BJ003);
                    return null;
                }
                return token;
            case 14: // LITERAL_try
                return parseTryCatchStmt(token);
            case 15: // IDENT
                // A label?
                LocatableToken ctoken = nextToken();
                if (ctoken.getType() == JavaTokenTypes.COLON) {
                    return ctoken;
                }
                tokenStream.pushBack(ctoken);
                tokenStream.pushBack(token);

                // A declaration of a variable?
                List<LocatableToken> tlist = new LinkedList<LocatableToken>();
                boolean isTypeSpec = parseTypeSpec(true, true, tlist);
                token = tokenStream.LA(1);
                pushBackAll(tlist);
                if (isTypeSpec && token.getType() == JavaTokenTypes.IDENT) {
                    token = tlist.get(0);
                    gotDeclBegin(token);
                    return parseVariableDeclarations(token, true);
                }
                else {
                    gotStatementExpression();
                    parseExpression();                                              
                    token = tokenStream.nextToken();
                    if (token.getType() == JavaTokenTypes.COMMA && allowComma) {
                        token = tokenStream.nextToken();
                        continue;
                    }
                    if (token.getType() != JavaTokenTypes.SEMI) {
                        tokenStream.pushBack(token);
                        error("Expected ';' at end of previous statement");
                        return null;
                    }
                    return token;
                }
            case 16: // LITERAL_synchronized
                // Synchronized block
                beginSynchronizedBlock(token);
                token = nextToken();
                if (token.getType() == JavaTokenTypes.LPAREN) {
                    parseExpression();
                    token = nextToken();
                    if (token.getType() != JavaTokenTypes.RPAREN) {
                        errorBefore("Expecting ')' at end of expression", token);
                        tokenStream.pushBack(token);
                        endSynchronizedBlock(token, false);
                        return null;
                    }
                    token = tokenStream.nextToken();
                }
                if (token.getType() == JavaTokenTypes.LCURLY) {
                    beginStmtblockBody(token);
                    parseStmtBlock();
                    token = nextToken();
                    if (token.getType() != JavaTokenTypes.RCURLY) {
                        error("Expecting '}' at end of synchronized block");
                        tokenStream.pushBack(token);
                        endStmtblockBody(token, false);
                        endSynchronizedBlock(token, false);
                        return null;
                    }
                    endStmtblockBody(token, true);
                    endSynchronizedBlock(token, true);
                    return token;
                }
                else {
                    error("Expecting statement block after 'synchronized'");
                    tokenStream.pushBack(token);
                    endSynchronizedBlock(token, false);
                    return null;
                }
            case 17: // LITERAL_public
            case 18: // LITERAL_private
            case 19: // LITERAL_protected
            case 20: // ABSTRACT
            case 21: // FINAL
            case 22: // LITERAL_static
            case 23: // LITERAL_volatile
            case 24: // LITERAL_native
            case 25: // STRICTFP
            case 26: // LITERAL_transient
            case 27: // AT
                tokenStream.pushBack(token);
                gotDeclBegin(token);
                parseModifiers();
                if (isTypeDeclarator(tokenStream.LA(1)) || tokenStream.LA(1).getType() == JavaTokenTypes.AT) {
                    gotInnerType(tokenStream.LA(1));
                    parseTypeDef(token);
                }
                else {
                    parseVariableDeclarations(token, true);
                }
                return null;
            case 28: // LITERAL_class
            case 29: // LITERAL_enum
            case 30: // LITERAL_interface
                tokenStream.pushBack(token);
                gotDeclBegin(token);
                parseTypeDef(token);
                return null;
            case 31: // LITERAL_void
            case 32: // LITERAL_boolean
            case 33: // LITERAL_byte
            case 34: // LITERAL_char
            case 35: // LITERAL_short
            case 36: // LITERAL_int
            case 37: // LITERAL_long
            case 38: // LITERAL_float
            case 39: // LITERAL_double
                // primitive
                tokenStream.pushBack(token);
                tlist = new LinkedList<LocatableToken>();
                parseTypeSpec(false, true, tlist);

                if (tokenStream.LA(1).getType() == JavaTokenTypes.DOT) {
                    // int.class, or int[].class are possible
                    pushBackAll(tlist);
                    parseExpression();
                    token = nextToken();
                    if (token.getType() != JavaTokenTypes.SEMI) {
                        error("Expected ';' after expression-statement");
                        return null;
                    }
                    return token;
                }
                else {
                    pushBackAll(tlist);
                    gotDeclBegin(token);
                    return parseVariableDeclarations(token, true);
                }
            case 40: // LCURLY
                beginStmtblockBody(token);
                parseStmtBlock();
                token = nextToken();
                if (token.getType() != JavaTokenTypes.RCURLY) {
                    error("Expecting '}' at end of statement block");
                    if (token.getType() != JavaTokenTypes.RPAREN) {
                        tokenStream.pushBack(token);
                    }
                    endStmtblockBody(token, false);
                    return null;
                }
                endStmtblockBody(token, true);
                return token;
            }

            // Expression, or not valid.
            if (! isExpressionTokenType(token.getType())) {
                error("Not a valid statement beginning.", token);
                return null;
            }

            tokenStream.pushBack(token);
            gotStatementExpression();
            parseExpression();
            token = tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.SEMI) {
                tokenStream.pushBack(token);
                error("Expected ';' at end of previous statement");
                return null;
            }
            return token;
        }
    }

    protected void gotSwitchCase() { }

    protected void gotSwitchDefault() { }

    protected void gotThrow(LocatableToken token) { }

    protected void gotBreakContinue(LocatableToken keywordToken, LocatableToken labelToken) { }

    protected void gotReturnStatement(boolean hasValue) { }

    protected void gotEmptyStatement() { }

    /**
     * Parse a try/catch/finally. The first token is 'try'.
     * @param token  The first token (must be 'try').
     * @return  the last token that is part of the try/catch/finally, or null
     */
    public LocatableToken parseTryCatchStmt(LocatableToken token)
    {
        beginTryCatchSmt(token, tokenStream.LA(1).getType() == JavaTokenTypes.LPAREN);
        token = nextToken();
        if (token.getType() == JavaTokenTypes.LPAREN) {
            // Java 7 try-with-resource
            do {
                token = tokenStream.LA(1);
                // Specification allows either a variable declaration (with initializer) or
                // expression.
                if (token.getType() == JavaTokenTypes.IDENT) {
                    List<LocatableToken> tlist = new LinkedList<LocatableToken>();
                    boolean isTypeSpec = parseTypeSpec(true, true, tlist);
                    token = tokenStream.LA(1);
                    pushBackAll(tlist);
                    if (isTypeSpec && token.getType() == JavaTokenTypes.IDENT) {
                        gotDeclBegin(tlist.get(0));
                        parseVariableDeclarations(tlist.get(0), false);
                    }
                    else {
                        parseExpression();                                              
                    }
                }
                else if (isModifier(token)) {
                    tokenStream.nextToken(); // remove the modifier from the token stream
                    parseVariableDeclarations();
                }
                else {
                    parseExpression();
                }
                token = tokenStream.nextToken();
            } while (token.getType() == JavaTokenTypes.SEMI);
            if (token.getType() != JavaTokenTypes.RPAREN) {
                errorBefore("Missing closing ')' after resources in 'try' statement", token);
            }
            token = nextToken();
        }
        if (token.getType() != JavaTokenTypes.LCURLY) {
            error ("Expecting '{' after 'try'");
            tokenStream.pushBack(token);
            endTryCatchStmt(token, false);
            return null;
        }
        beginTryBlock(token);
        parseStmtBlock();
        token = nextToken();
        if (token.getType() == JavaTokenTypes.RCURLY) {
            endTryBlock(token, true);
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_catch
                || token.getType() == JavaTokenTypes.LITERAL_finally) {
            // Invalid, but we can recover
            tokenStream.pushBack(token);
            error("Missing '}' at end of 'try' block");
            endTryBlock(token, false);
        }
        else {
            tokenStream.pushBack(token);
            error("Missing '}' at end of 'try' block");
            endTryBlock(token, false);
            endTryCatchStmt(token, false);
            return null;
        }

        int laType = tokenStream.LA(1).getType();
        while (laType == JavaTokenTypes.LITERAL_catch
                || laType == JavaTokenTypes.LITERAL_finally) {
            token = nextToken();
            gotCatchFinally(token);
            if (laType == JavaTokenTypes.LITERAL_catch) {
                token = nextToken();
                if (token.getType() != JavaTokenTypes.LPAREN) {
                    error("Expecting '(' after 'catch'");
                    tokenStream.pushBack(token);
                    endTryCatchStmt(token, false);
                    return null;
                }
                
                while (true) {
                    if (tokenStream.LA(1).getType() == JavaTokenTypes.FINAL) {
                        // Java 7 "final re-throw"
                        token = nextToken();
                    }
                    
                    parseTypeSpec(true);
                    token = nextToken();
                    if (token.getType() != JavaTokenTypes.BOR) {
                        // Java 7 multi-catch
                        break;
                    }
                    gotMultiCatch(token);
                }
                
                if (token.getType() != JavaTokenTypes.IDENT) {
                    error("Expecting identifier after type (in 'catch' expression)");
                    tokenStream.pushBack(token);
                    endTryCatchStmt(token, false);
                    return null;
                }
                gotCatchVarName(token);
                token = nextToken();
                
                if (token.getType() != JavaTokenTypes.RPAREN) {
                    error("Expecting ')' after identifier (in 'catch' expression)");
                    tokenStream.pushBack(token);
                    endTryCatchStmt(token, false);
                    return null;
                }
            }
            token = nextToken();
            if (token.getType() != JavaTokenTypes.LCURLY) {
                error("Expecting '{' after 'catch'/'finally'");
                tokenStream.pushBack(token);
                endTryCatchStmt(token, false);
                return null;
            }
            token = parseStatement(token, false); // parse as a statement block
            laType = tokenStream.LA(1).getType();
        }
        if (token != null) {
            endTryCatchStmt(token, true);
        }
        else {
            endTryCatchStmt(tokenStream.LA(1), false);
        }
        return token;
    }

    protected void gotCatchFinally(LocatableToken token) { }

    protected void gotMultiCatch(LocatableToken token) { }

    protected void gotCatchVarName(LocatableToken token) { }

    /**
     * Parse an "assert" statement. Returns the concluding semi-colon token, or null on error.
     * {@code lastToken} will be set to the last token which is part of the statement.
     * 
     * @param token   The token corresponding to the "assert" keyword.
     */
    public LocatableToken parseAssertStatement(LocatableToken token)
    {
        gotAssert();
        parseExpression();
        token = tokenStream.nextToken();
        if (token.getType() == JavaTokenTypes.COLON) {
            // Should be followed by a string
            lastToken = token;
            parseExpression();
            token = tokenStream.nextToken();
        }
        if (token.getType() != JavaTokenTypes.SEMI) {
            error("Expected ';' at end of assertion statement");
            tokenStream.pushBack(token);
            return null;
        }
        lastToken = token;
        return token;
    }

    protected void gotAssert() { }

    /** Parse a "switch(...) {  }" statement. */
    public LocatableToken parseSwitchStatement(LocatableToken token)
    {
        beginSwitchStmt(token);
        token = nextToken();
        if (token.getType() != JavaTokenTypes.LPAREN) {
            error("Expected '(' after 'switch'");
            tokenStream.pushBack(token);
            endSwitchStmt(token, false);
            return null;
        }
        parseExpression();
        token = nextToken();
        if (token.getType() != JavaTokenTypes.RPAREN) {
            error("Expected ')' at end of expression (in 'switch(...)')");
            tokenStream.pushBack(token);
            endSwitchStmt(token, false);
            return null;
        }
        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.LCURLY) {
            error("Expected '{' after 'switch(...)'");
            tokenStream.pushBack(token);
            endSwitchStmt(token, false);
            return null;
        }
        beginSwitchBlock(token);
        parseStmtBlock();
        token = nextToken();
        if (token.getType() != JavaTokenTypes.RCURLY) {
            error("Missing '}' at end of 'switch' statement block");
            tokenStream.pushBack(token);
            endSwitchBlock(token);
            endSwitchStmt(token, false);
            return null;
        }
        endSwitchBlock(token);
        endSwitchStmt(token, true);
        return token;
    }
    
    public LocatableToken parseDoWhileStatement(LocatableToken token)
    {
        beginDoWhile(token);
        token = nextToken(); // '{' or a statement
        LocatableToken ntoken = parseStatement(token, false);
        if (ntoken != null || token != tokenStream.LA(1)) {
            beginDoWhileBody(token);
            if (ntoken == null) {
                endDoWhileBody(tokenStream.LA(1), false);
            }
            else {
                endDoWhileBody(ntoken, true);
            }
        }

        token = nextToken();
        if (token.getType() != JavaTokenTypes.LITERAL_while) {
            error("Expecting 'while' after statement block (in 'do ... while')");
            tokenStream.pushBack(token);
            endDoWhile(token, false);
            return null;
        }
        token = nextToken();
        if (token.getType() != JavaTokenTypes.LPAREN) {
            error("Expecting '(' after 'while'");
            tokenStream.pushBack(token);
            endDoWhile(token, false);
            return null;
        }
        parseExpression();
        token = nextToken();
        if (token.getType() != JavaTokenTypes.RPAREN) {
            error("Expecting ')' after conditional expression (in 'while' statement)");
            tokenStream.pushBack(token);
            endDoWhile(token, false);
            return null;
        }
        token = nextToken(); // should be ';'
        endDoWhile(token, true);
        return token;
    }
        
    public LocatableToken parseWhileStatement(LocatableToken token)
    {
        beginWhileLoop(token);
        token = nextToken();
        if (token.getType() != JavaTokenTypes.LPAREN) {
            error("Expecting '(' after 'while'");
            tokenStream.pushBack(token);
            endWhileLoop(token, false);
            return null;
        }
        parseExpression();
        token = nextToken();
        if (token.getType() != JavaTokenTypes.RPAREN) {
            error("Expecting ')' after conditional expression (in 'while' statement)");
            tokenStream.pushBack(token);
            endWhileLoop(token, false);
            return null;
        }
        token = nextToken();
        beginWhileLoopBody(token);
        token = parseStatement(token, false);
        if (token != null) {
            endWhileLoopBody(token, true);
            endWhileLoop(token, true);
        }
        else {
            token = tokenStream.LA(1);
            endWhileLoopBody(token, false);
            endWhileLoop(token, false);
            token = null;
        }
        return token;
    }

    /**
     * Parse a "for(...)" loop (old or new style).
     * @param forToken  The "for" token, which has already been extracted from the token stream.
     * @return The last token that is part of the loop (or null).
     */
    public LocatableToken parseForStatement(LocatableToken forToken)
    {
        // TODO: if we get an unexpected token in the part between '(' and ')' check
        // if it is ')'. If so we might still expect a loop body to follow.
        beginForLoop(forToken);
        LocatableToken token = nextToken();
        if (token.getType() != JavaTokenTypes.LPAREN) {
            error("Expecting '(' after 'for'");
            tokenStream.pushBack(token);
            endForLoop(token, false);
            return null;
        }
        if (tokenStream.LA(1).getType() != JavaTokenTypes.SEMI) {
            // Could be an old or new style for-loop.
            List<LocatableToken> tlist = new LinkedList<LocatableToken>();

            LocatableToken first = tokenStream.LA(1);
            boolean isTypeSpec = false;
            if (isModifier(tokenStream.LA(1))) {
                parseModifiers();
                isTypeSpec = true;
                parseTypeSpec(false, true, tlist);
            }
            else {
                isTypeSpec = parseTypeSpec(true, true, tlist);
            }
            
            if (isTypeSpec && tokenStream.LA(1).getType() == JavaTokenTypes.IDENT) {
                // for (type var ...
                beginForInitDecl(first);
                gotTypeSpec(tlist);
                LocatableToken idToken = nextToken(); // identifier
                gotForInit(first, idToken);
                // Array declarators can follow name
                parseArrayDeclarators();

                token = nextToken();
                if (token.getType() == JavaTokenTypes.COLON) {
                    determinedForLoop(true, false);
                    // This is a "new" for loop (Java 5)
                    endForInit(idToken, true);
                    endForInitDecls(idToken, true);
                    modifiersConsumed();
                    parseExpression();
                    token = nextToken();
                    if (token.getType() != JavaTokenTypes.RPAREN) {
                        error("Expecting ')' (in for statement)");
                        tokenStream.pushBack(token);
                        endForLoop(token, false);
                        return null;
                    }
                    token = nextToken();
                    beginForLoopBody(token);
                    token = parseStatement(token, false); // loop body
                    endForLoopBody(token);
                    endForLoop(token);
                    return token;
                }
                else {
                    determinedForLoop(false, token.getType() == JavaTokenTypes.ASSIGN);
                    // Old style loop with initialiser
                    if (token.getType() == JavaTokenTypes.ASSIGN) {
                        parseExpression();
                    }
                    else {
                        tokenStream.pushBack(token);
                    }
                    if (parseSubsequentDeclarations(DECL_TYPE_FORINIT, true) == null) {
                        endForLoop(tokenStream.LA(1), false);
                        modifiersConsumed();
                        return null;
                    }
                    modifiersConsumed();
                }
            }
            else {
                // Not a type spec, so, we might have a general statement
                pushBackAll(tlist);
                token = nextToken();
                parseStatement(token, true);
            }
        }
        else {
            token = nextToken(); // SEMI
        }

        // We're expecting a regular (old-style) statement at this point
        boolean semiFollows = tokenStream.LA(1).getType() == JavaTokenTypes.SEMI;
        gotForTest(!semiFollows);
        if (!semiFollows) {
            // test expression
            parseExpression();
        }
        token = nextToken();
        if (token.getType() != JavaTokenTypes.SEMI) {
            tokenStream.pushBack(token);
            if (token.getType() == JavaTokenTypes.COMMA) {
                error(BJ003, token);  // common mistake: use ',' instead of ';'
            }
            else {
                error(BJ003);
            }
            endForLoop(token, false);
            return null;
        }
        boolean bracketFollows = tokenStream.LA(1).getType() == JavaTokenTypes.RPAREN;
        gotForIncrement(!bracketFollows);
        if (!bracketFollows) {
            // loop increment expression
            parseExpression();
            while (tokenStream.LA(1).getType() == JavaTokenTypes.COMMA) {
                nextToken();
                parseExpression();
            }
        }
        token = nextToken(); // ')'?
        if (token.getType() != JavaTokenTypes.RPAREN) {
            error("Expecting ')' (or ',') after 'for(...'");
            tokenStream.pushBack(token);
            endForLoop(token, false);
            return null;
        }
        token = nextToken();
        if (token.getType() == JavaTokenTypes.RCURLY
                || token.getType() == JavaTokenTypes.EOF) {
            error("Expecting statement after 'for(...)'");
            tokenStream.pushBack(token);
            endForLoop(token, false);
            return null;
        }
        beginForLoopBody(token);
        token = parseStatement(token, false);
        endForLoopBody(token);
        endForLoop(token);
        return token;
    }

    protected void gotForTest(boolean isPresent) { }
    protected void gotForIncrement(boolean isPresent) { }

    protected void determinedForLoop(boolean forEachLoop, boolean initExpressionFollows) { }

    private void endForLoop(LocatableToken token)
    {
        if (token == null) {
            endForLoop(tokenStream.LA(1), false);
        }
        else {
            endForLoop(token, true);
        }
    }
    
    private void endForLoopBody(LocatableToken token)
    {
        if (token == null) {
            endForLoopBody(tokenStream.LA(1), false);
        }
        else {
            endForLoopBody(token, true);
        }
    }
        
    /**
     * Parse an "if" statement.
     * @param token  The token corresponding to the "if" literal.
     */
    public LocatableToken parseIfStatement(LocatableToken token)
    {
        beginIfStmt(token);
        
        mainLoop:
        while(true) {
            token = nextToken(); // "("
            if (token.getType() != LPAREN) {
                tokenStream.pushBack(token);
                if (token.getType() == LCURLY) {
                    error(BJ002, token);
                }
                else {
                    errorBefore(BJ001, token);
                }
                endIfStmt(token, false);
                return null;
            }
            parseExpression();
            token = nextToken();
            if (token.getType() != JavaTokenTypes.RPAREN) {
                error("Expecting ')' after conditional expression (in 'if' statement)");
                tokenStream.pushBack(token);
                if (token.getType() != JavaTokenTypes.LCURLY) {
                    endIfStmt(token, false);
                    return null;
                }
            }
            token = nextToken();
            beginIfCondBlock(token);
            token = parseStatement(token, false);
            endIfCondBlock(token);
            if (tokenStream.LA(1).getType() == JavaTokenTypes.LITERAL_else) {
                tokenStream.nextToken(); // "else"
                if (tokenStream.LA(1).getType() == JavaTokenTypes.LITERAL_if) {
                    gotElseIf(token);
                    nextToken(); // "if"
                    continue mainLoop;
                }
                token = nextToken();
                beginIfCondBlock(token);
                token = parseStatement(token, false);
                endIfCondBlock(token);
            }
            endIfStmt(token);
            return token;
        }
    }
    
    private void endIfCondBlock(LocatableToken token)
    {
        if (token != null) {
            endIfCondBlock(token, true);
        }
        else {
            endIfCondBlock(tokenStream.LA(1), false);
        }
    }
    
    private void endIfStmt(LocatableToken token)
    {
        if (token != null) {
            endIfStmt(token, true);
        }
        else {
            endIfStmt(tokenStream.LA(1), false);
        }
    }
       
    public LocatableToken parseVariableDeclarations()
    {
        LocatableToken first = tokenStream.LA(1);
        gotDeclBegin(first);
        return parseVariableDeclarations(first, true);
    }
    
    /**
     * Parse a variable declaration, possibly with an initialiser, usually followed by ';'
     * 
     * @param first   The first token of the declaration (should still be
     *                in the token stream, unless it is a modifier)
     * @param mustEndWithSemi  If false, specifies that the declaration need not be terminated by a semi-colon,
     *                and that furthermore, the character terminating the declaration should be left in the token
     *                stream.
     */
    public LocatableToken parseVariableDeclarations(LocatableToken first, boolean mustEndWithSemi)
    {
        beginVariableDecl(first);
        parseModifiers();
        boolean r = parseVariableDeclaration(first);
        // parseVariableDeclaration calls modifiersConsumed(); i.e. we act as if
        // the modifiers are consumed by the type rather than the variables.
        // This is necessary because an initializer expression might contain an anonymous
        // class containing modifiers.
        if (r) {
            return parseSubsequentDeclarations(DECL_TYPE_VAR, mustEndWithSemi);
        }
        else {
            endVariableDecls(tokenStream.LA(1), false);
            return null;
        }
    }

    /* Types for parseSubsequentDeclarations and friends */
    
    /** for loop initializer */
    protected static final int DECL_TYPE_FORINIT = 0;
    /** variable */
    protected static final int DECL_TYPE_VAR = 1;
    /** field */
    protected static final int DECL_TYPE_FIELD = 2;
    
    /**
     * After seeing a type and identifier declaration, this will parse any
     * the subsequent declarations, and check for a terminating semicolon.
     * 
     * @return  the last token that is part of the declarations, or null on error (or mustEndWithSemi == false).
     */
    protected LocatableToken parseSubsequentDeclarations(int type, boolean mustEndWithSemi)
    {
        LocatableToken prevToken = lastToken;
        LocatableToken token = nextToken();
        while (token.getType() == JavaTokenTypes.COMMA) {
            endDeclaration(type, token, false);
            LocatableToken first = token;
            token = nextToken();
            if (token.getType() != JavaTokenTypes.IDENT) {
                endDeclarationStmt(type, token, false);
                error("Expecting variable identifier (or change ',' to ';')");
                return null;
            }
            parseArrayDeclarators();
            LocatableToken idtoken = token;
            prevToken = lastToken;
            token = nextToken();
            gotSubsequentDecl(type, first, idtoken, token.getType() == JavaTokenTypes.ASSIGN);
            if (token.getType() == JavaTokenTypes.ASSIGN) {
                parseExpression();
                prevToken = lastToken;
                token = nextToken();
            }
        }

        if (! mustEndWithSemi) {
            tokenStream.pushBack(token);
            endDeclaration(type, token, false);
            endDeclarationStmt(type, token, false);
            return null;
        }
        
        if (token.getType() != JavaTokenTypes.SEMI) {
            tokenStream.pushBack(token);
            errorBehind(BJ003, prevToken);
            endDeclaration(type, token, false);
            endDeclarationStmt(type, token, false);
            return null;
        }
        else {
            endDeclaration(type, token, true);
            endDeclarationStmt(type, token, true);
            return token;
        }
    }

    private void endDeclaration(int type, LocatableToken token, boolean included)
    {
        if (type == DECL_TYPE_FIELD) {
            endField(token, included);
        }
        else if (type == DECL_TYPE_VAR) {
            endVariable(token, included);
        }
        else {
            endForInit(token, included);
        }
    }
    
    private void endDeclarationStmt(int type, LocatableToken token, boolean included)
    {
        if (type == DECL_TYPE_FIELD) {
            endFieldDeclarations(token, included);
        }
        else if (type == DECL_TYPE_VAR) {
            endVariableDecls(token, included);
        }
        else {
            endForInitDecls(token, included);
        }
    }
    
    private void gotSubsequentDecl(int type, LocatableToken firstToken,
            LocatableToken nameToken, boolean inited)
    {
        if (type == DECL_TYPE_FIELD) {
            gotSubsequentField(firstToken, nameToken, inited);
        }
        else if (type == DECL_TYPE_VAR) {
            gotSubsequentVar(firstToken, nameToken, inited);
        }
        else {
            gotSubsequentForInit(firstToken, nameToken, inited);
        }
    }
    
    /**
     * Parse a variable (or field or parameter) declaration, possibly including an initialiser
     * (but not including modifiers)
     */
    private boolean parseVariableDeclaration(LocatableToken first)
    {
        List<LocatableToken> typeSpecTokens = new LinkedList<LocatableToken>();
        if (!parseTypeSpec(false, true, typeSpecTokens)) {
            return false;
        }
        gotTypeSpec(typeSpecTokens);
        
        LocatableToken token = nextToken();
        if (token.getType() != JavaTokenTypes.IDENT) {
            error("Expecting identifier (in variable/field declaration)");
            tokenStream.pushBack(token);
            return false;
        }
        
        // Array declarators can follow name
        parseArrayDeclarators();

        LocatableToken idToken = token;
        token = nextToken();
        gotVariableDecl(first, idToken, token.getType() == JavaTokenTypes.ASSIGN);
        modifiersConsumed();

        if (token.getType() == JavaTokenTypes.ASSIGN) {
            parseExpression();
        }
        else {
            tokenStream.pushBack(token);
        }
        return true;
    }
        
    /**
     * Parse a type specification. This includes class name(s) (Xyz.Abc), type arguments
     * to generic types, and array declarators.
     * 
     * <p>The final set of array declarators will not be parsed if they contain a dimension value.
     * Eg for "Abc[10][][]" this method will leave "[10][][]" unprocessed and still in the token stream.
     * 
     *  @param processArray   if false, no '[]' sequences will be parsed, only the element type.
     *  @return  true iff a type specification was successfully parsed
     */
    public boolean parseTypeSpec(boolean processArray)
    {
        List<LocatableToken> tokens = new LinkedList<LocatableToken>();
        boolean rval = parseTypeSpec(false, processArray, tokens);
        if (rval) {
            gotTypeSpec(tokens);
        }
        return rval;
    }
        
    /**
     * Parse a type specification. This could be a primitive type (including void),
     * or a class type (qualified or not, possibly with type parameters). This can
     * do a speculative parse if the following tokens might either be a type specification
     * or a statement-expression.
     * 
     * @param speculative  Whether this is a speculative parse, i.e. we might not actually
     *                     have a type specification. If this is set some parse errors will
     *                     simply return false.
     * @param processArray  Whether to parse '[]' array declarators. If false only the
     *                     element type will be parsed.
     * @param ttokens   A list which will be filled with tokens. If the return is true, the tokens
     *                  make up a possible type specification; otherwise the tokens should be
     *                  pushed back on the token stream.
     * 
     * @return true if we saw what might be a type specification (even if it
     *                         contains errors), or false if it does not appear to be
     *                     a type specification.
     */
    public boolean parseTypeSpec(boolean speculative, boolean processArray, List<LocatableToken> ttokens)
    {
        int ttype = parseBaseType(speculative, ttokens);
        if (ttype == TYPE_ERROR) {
            return false;
        }
        else if (ttype == TYPE_PRIMITIVE) {
            speculative = false;
        }
        else {
            LocatableToken token = nextToken();
            if (token.getType() == JavaTokenTypes.LT) {
                ttokens.add(token);

                // Type parameters? (or is it a "less than" comparison?)
                DepthRef dr = new DepthRef();
                dr.depth = 1;
                if (!parseTargs(speculative, ttokens, dr)) {
                    return false;
                }
            }
            else {
                tokenStream.pushBack(token);
            }
        }

        // check for inner type
        LocatableToken token = nextToken();
        if (token.getType() == JavaTokenTypes.DOT) {
            if (tokenStream.LA(1).getType() == JavaTokenTypes.IDENT) {
                ttokens.add(token);
                return parseTypeSpec(speculative, true, ttokens);
            }
            else {
                tokenStream.pushBack(token);
                return true;
            }
        }
        else if (processArray)
        {
            // check for array declarators
            while (token.getType() == JavaTokenTypes.LBRACK
                    && tokenStream.LA(1).getType() == JavaTokenTypes.RBRACK) {
                ttokens.add(token);
                token = nextToken(); // RBRACK
                ttokens.add(token);
                token = nextToken();
            }
        }

        tokenStream.pushBack(token);
        return true;
    }

    private static final int TYPE_PRIMITIVE = 0;
    private static final int TYPE_OTHER = 1;
    private static final int TYPE_ERROR = 2;

    /**
     * Parse a type "base" - a primitive type or a class type without type parameters.
     * The type parameters may follow.
     * 
     * @param speculative
     * @param ttokens
     * @return
     * @throws TokenStreamException
     */
    private int parseBaseType(boolean speculative, List<LocatableToken> ttokens)
    {
        LocatableToken token = nextToken();
        if (isPrimitiveType(token)) {
            // Ok, we have a base type
            ttokens.add(token);
            return TYPE_PRIMITIVE;
        }
        else {
            if (token.getType() != JavaTokenTypes.IDENT) {
                if (! speculative) {
                    error("Expected type identifier");
                }
                tokenStream.pushBack(token);
                return TYPE_ERROR;
            }

            ttokens.addAll(parseDottedIdent(token));
        }
        return TYPE_OTHER;
    }

    private boolean parseTargs(boolean speculative, List<LocatableToken> ttokens, DepthRef dr)
    {
        // We already have opening '<' and depth reflects this.

        int beginDepth = dr.depth;
        LocatableToken token;
        boolean needBaseType = true;

        while (dr.depth >= beginDepth) {

            if (tokenStream.LA(1).getType() == JavaTokenTypes.QUESTION) {
                // Wildcard
                token = nextToken();
                ttokens.add(token);
                token = nextToken();
                if (token.getType() == JavaTokenTypes.LITERAL_extends
                        || token.getType() == JavaTokenTypes.LITERAL_super) {
                    ttokens.add(token);
                    needBaseType = true;
                }
                else {
                    tokenStream.pushBack(token);
                    needBaseType = false;
                }
            }

            if (needBaseType) {
                boolean r = parseTargType(speculative, ttokens, dr);
                if (!r) {
                    return false;
                }
                if (dr.depth < beginDepth) {
                    break;
                }
            }

            token = nextToken();
            // Type parameters being closed
            if (token.getType() == JavaTokenTypes.GT
                    || token.getType() == JavaTokenTypes.SR
                    || token.getType() == JavaTokenTypes.BSR) {
                ttokens.add(token);
                if (token.getType() == JavaTokenTypes.GT) {
                    dr.depth--;
                }
                else if (token.getType() == JavaTokenTypes.SR) {
                    dr.depth -= 2;
                }
                else if (token.getType() == JavaTokenTypes.BSR) {
                    dr.depth -= 3;
                }
            }
            else if (token.getType() == JavaTokenTypes.COMMA) {
                needBaseType = true;
                ttokens.add(token);
            }
            else {
                if (! speculative) {
                    error("Expected '>' to close type parameter list");
                }
                tokenStream.pushBack(token);
                return false;
            }
        }
        return true;
    }

    /**
     * Parse a type argument, type part. The "? super" or "? extends" have already been dealt
     * with. The type part may itself have type arguments, and might be followed by a comma
     * or a closing '>' sequence.
     * 
     * @param speculative  Should be true if this is a speculative type parse
     * @param ttokens  A list of tokens. All tokens processed will be added to this list.
     * @param dr  Depth reference.
     */
    private boolean parseTargType(boolean speculative, List<LocatableToken> ttokens, DepthRef dr)
    {
        LocatableToken token;
        int beginDepth = dr.depth;
        
        if (tokenStream.LA(1).getType() == JavaTokenTypes.GT) {
            // Java 7 Diamond operator
            ttokens.add(tokenStream.nextToken());
            dr.depth--;
            return true;
        }
        
        int ttype = parseBaseType(speculative, ttokens);
        if (ttype == TYPE_ERROR) {
            return false;
        }

        if (ttype == TYPE_OTHER) {
            // May be type parameters
            if (tokenStream.LA(1).getType() == JavaTokenTypes.LT) {
                dr.depth++;
                ttokens.add(nextToken());
                if (!parseTargs(speculative, ttokens, dr)) {
                    return false;
                }
                if (dr.depth < beginDepth) {
                    return true;
                }
            }

            token = nextToken();
            if (token.getType() == JavaTokenTypes.DOT && tokenStream.LA(1).getType() == JavaTokenTypes.IDENT) {
                ttokens.add(token);
                if (!parseTargType(speculative, ttokens, dr)) {
                    return false;
                }
                return true;
            }
        }
        else {
            token = nextToken();
        }

        // Array declarators?
        while (token.getType() == JavaTokenTypes.LBRACK
                && tokenStream.LA(1).getType() == JavaTokenTypes.RBRACK) {
            ttokens.add(token);
            token = nextToken(); // RBRACK
            ttokens.add(token);
            token = nextToken();
        }

        tokenStream.pushBack(token);

        return true;
    }
        
    /**
     * Parse a dotted identifier. This could be a variable, method or type name.
     * @param first The first token in the dotted identifier (should be an IDENT)
     * @return A list of tokens making up the dotted identifier
     */
    public List<LocatableToken> parseDottedIdent(LocatableToken first)
    {
        List<LocatableToken> rval = new LinkedList<LocatableToken>();
        rval.add(first);
        LocatableToken token = nextToken();
        while (token.getType() == JavaTokenTypes.DOT) {
            LocatableToken ntoken = nextToken();
            if (ntoken.getType() != JavaTokenTypes.IDENT) {
                // This could be for example "xyz.class"
                tokenStream.pushBack(ntoken);
                break;
            }
            rval.add(token);
            rval.add(ntoken);
            token = nextToken();
        }
        tokenStream.pushBack(token);
        return rval;
    }
        
    /**
     * Check whether a token is an operator. Note that the LPAREN token can be an operator
     * (method call) or value (parenthesized expression).
     * 
     * "new" is not classified as an operator here (an operator operates on a value).
     */
    public static boolean isOperator(LocatableToken token)
    {
        int ttype = token.getType();
        return ttype == JavaTokenTypes.PLUS
        || ttype == JavaTokenTypes.MINUS
        || ttype == JavaTokenTypes.STAR
        || ttype == JavaTokenTypes.DIV
        || ttype == JavaTokenTypes.LBRACK
        || ttype == JavaTokenTypes.LPAREN
        || ttype == JavaTokenTypes.PLUS_ASSIGN
        || ttype == JavaTokenTypes.STAR_ASSIGN
        || ttype == JavaTokenTypes.MINUS_ASSIGN
        || ttype == JavaTokenTypes.DIV_ASSIGN
        || ttype == JavaTokenTypes.DOT
        || ttype == JavaTokenTypes.EQUAL
        || ttype == JavaTokenTypes.NOT_EQUAL
        || ttype == JavaTokenTypes.LT
        || ttype == JavaTokenTypes.LE
        || ttype == JavaTokenTypes.GT
        || ttype == JavaTokenTypes.GE
        || ttype == JavaTokenTypes.ASSIGN
        || ttype == JavaTokenTypes.BNOT
        || ttype == JavaTokenTypes.LNOT
        || ttype == JavaTokenTypes.INC
        || ttype == JavaTokenTypes.DEC
        || ttype == JavaTokenTypes.BOR
        || ttype == JavaTokenTypes.BOR_ASSIGN
        || ttype == JavaTokenTypes.BAND
        || ttype == JavaTokenTypes.BAND_ASSIGN
        || ttype == JavaTokenTypes.BXOR
        || ttype == JavaTokenTypes.BXOR_ASSIGN
        || ttype == JavaTokenTypes.LOR
        || ttype == JavaTokenTypes.LAND
        || ttype == JavaTokenTypes.SL
        || ttype == JavaTokenTypes.SL_ASSIGN
        || ttype == JavaTokenTypes.SR
        || ttype == JavaTokenTypes.SR_ASSIGN
        || ttype == JavaTokenTypes.BSR
        || ttype == JavaTokenTypes.BSR_ASSIGN
        || ttype == JavaTokenTypes.MOD
        || ttype == JavaTokenTypes.MOD_ASSIGN
        || ttype == JavaTokenTypes.LITERAL_instanceof;
    }
        
    /**
     * Check whether an operator is a binary operator.
     * 
     * "instanceof" is not considered to be a binary operator (operates on only one value).
     */
    public boolean isBinaryOperator(LocatableToken token)
    {
        int ttype = token.getType();
        return ttype == JavaTokenTypes.PLUS
        || ttype == JavaTokenTypes.MINUS
        || ttype == JavaTokenTypes.STAR
        || ttype == JavaTokenTypes.DIV
        || ttype == JavaTokenTypes.MOD
        || ttype == JavaTokenTypes.BOR
        || ttype == JavaTokenTypes.BXOR
        || ttype == JavaTokenTypes.BAND
        || ttype == JavaTokenTypes.SL
        || ttype == JavaTokenTypes.SR
        || ttype == JavaTokenTypes.BSR
        || ttype == JavaTokenTypes.BSR_ASSIGN
        || ttype == JavaTokenTypes.SR_ASSIGN
        || ttype == JavaTokenTypes.SL_ASSIGN
        || ttype == JavaTokenTypes.BAND_ASSIGN
        || ttype == JavaTokenTypes.BXOR_ASSIGN
        || ttype == JavaTokenTypes.BOR_ASSIGN
        || ttype == JavaTokenTypes.MOD_ASSIGN
        || ttype == JavaTokenTypes.DIV_ASSIGN
        || ttype == JavaTokenTypes.STAR_ASSIGN
        || ttype == JavaTokenTypes.MINUS_ASSIGN
        || ttype == JavaTokenTypes.PLUS_ASSIGN
        || ttype == JavaTokenTypes.ASSIGN
        || ttype == JavaTokenTypes.DOT
        || ttype == JavaTokenTypes.EQUAL
        || ttype == JavaTokenTypes.NOT_EQUAL
        || ttype == JavaTokenTypes.LT
        || ttype == JavaTokenTypes.LE
        || ttype == JavaTokenTypes.GT
        || ttype == JavaTokenTypes.GE
        || ttype == JavaTokenTypes.LAND
        || ttype == JavaTokenTypes.LOR;
    }
        
    public boolean isUnaryOperator(LocatableToken token)
    {
        int ttype = token.getType();
        return ttype == JavaTokenTypes.PLUS
        || ttype == JavaTokenTypes.MINUS
        || ttype == JavaTokenTypes.LNOT
        || ttype == JavaTokenTypes.BNOT
        || ttype == JavaTokenTypes.INC
        || ttype == JavaTokenTypes.DEC;
    }

    /**
     * Parse an annotation (having already seen '@', and having an identifier next in the token stream)
     */
    public void parseAnnotation()
    {
        LocatableToken token = nextToken(); // IDENT
        List<LocatableToken> annName = parseDottedIdent(token);
        boolean paramsFollow = tokenStream.LA(1).getType() == JavaTokenTypes.LPAREN;
        gotAnnotation(annName, paramsFollow);
        if (paramsFollow) {
            // arguments
            token = tokenStream.nextToken(); // LPAREN
            parseArgumentList(token);
        }
    }

    protected void gotAnnotation(List<LocatableToken> annName, boolean paramsFollow)
    {

    }

    private static int [] expressionTokenIndexes = new int[JavaTokenTypes.INVALID+1];
    
    static {
        expressionTokenIndexes[JavaTokenTypes.LITERAL_new] = 1;
        expressionTokenIndexes[JavaTokenTypes.LCURLY] = 2;
        expressionTokenIndexes[JavaTokenTypes.IDENT] = 3;
        expressionTokenIndexes[JavaTokenTypes.LITERAL_this] = 4;
        expressionTokenIndexes[JavaTokenTypes.LITERAL_super] = 5;
        
        expressionTokenIndexes[JavaTokenTypes.STRING_LITERAL] = 6;
        expressionTokenIndexes[JavaTokenTypes.CHAR_LITERAL] = 7;
        expressionTokenIndexes[JavaTokenTypes.NUM_INT] = 8;
        expressionTokenIndexes[JavaTokenTypes.NUM_LONG] = 9;
        expressionTokenIndexes[JavaTokenTypes.NUM_DOUBLE] = 10;
        expressionTokenIndexes[JavaTokenTypes.NUM_FLOAT] = 11;
        expressionTokenIndexes[JavaTokenTypes.LITERAL_null] = 12;
        expressionTokenIndexes[JavaTokenTypes.LITERAL_true] = 13;
        expressionTokenIndexes[JavaTokenTypes.LITERAL_false] = 14;
        
        expressionTokenIndexes[JavaTokenTypes.LPAREN] = 15;
        
        expressionTokenIndexes[JavaTokenTypes.LITERAL_void] = 16;
        expressionTokenIndexes[JavaTokenTypes.LITERAL_boolean] = 17;
        expressionTokenIndexes[JavaTokenTypes.LITERAL_byte] = 18;
        expressionTokenIndexes[JavaTokenTypes.LITERAL_char] = 19;
        expressionTokenIndexes[JavaTokenTypes.LITERAL_short] = 20;
        expressionTokenIndexes[JavaTokenTypes.LITERAL_int] = 21;
        expressionTokenIndexes[JavaTokenTypes.LITERAL_long] = 22;
        expressionTokenIndexes[JavaTokenTypes.LITERAL_float] = 23;
        expressionTokenIndexes[JavaTokenTypes.LITERAL_double] = 24;
        
        expressionTokenIndexes[JavaTokenTypes.PLUS] = 25;
        expressionTokenIndexes[JavaTokenTypes.MINUS] = 26;
        expressionTokenIndexes[JavaTokenTypes.LNOT] = 27;
        expressionTokenIndexes[JavaTokenTypes.BNOT] = 28;
        expressionTokenIndexes[JavaTokenTypes.INC] = 29;
        expressionTokenIndexes[JavaTokenTypes.DEC] = 30;
    }
    
    private static int [] expressionOpIndexes = new int[JavaTokenTypes.INVALID+1];
    
    static {
        expressionOpIndexes[JavaTokenTypes.RPAREN] = 1;
        expressionOpIndexes[JavaTokenTypes.SEMI] = 2;
        expressionOpIndexes[JavaTokenTypes.RBRACK] = 3;
        expressionOpIndexes[JavaTokenTypes.COMMA] = 4;
        expressionOpIndexes[JavaTokenTypes.COLON] = 5;
        expressionOpIndexes[JavaTokenTypes.EOF] = 6;
        expressionOpIndexes[JavaTokenTypes.RCURLY] = 7;
        
        expressionOpIndexes[JavaTokenTypes.LBRACK] = 8;
        expressionOpIndexes[JavaTokenTypes.LITERAL_instanceof] = 9;
        expressionOpIndexes[JavaTokenTypes.DOT] = 10;
        
        // Binary operators (not DOT)
        expressionOpIndexes[JavaTokenTypes.PLUS] = 11;
        expressionOpIndexes[JavaTokenTypes.MINUS] = 11;
        expressionOpIndexes[JavaTokenTypes.STAR] = 11;
        expressionOpIndexes[JavaTokenTypes.DIV] = 11;
        expressionOpIndexes[JavaTokenTypes.MOD] = 11;
        expressionOpIndexes[JavaTokenTypes.BOR] = 11;
        expressionOpIndexes[JavaTokenTypes.BXOR] = 11;
        expressionOpIndexes[JavaTokenTypes.BAND] = 11;
        expressionOpIndexes[JavaTokenTypes.SL] = 11;
        expressionOpIndexes[JavaTokenTypes.SR] = 11;
        expressionOpIndexes[JavaTokenTypes.BSR] = 11;
        expressionOpIndexes[JavaTokenTypes.BSR_ASSIGN] = 11;
        expressionOpIndexes[JavaTokenTypes.SR_ASSIGN] = 11;
        expressionOpIndexes[JavaTokenTypes.SL_ASSIGN] = 11;
        expressionOpIndexes[JavaTokenTypes.BAND_ASSIGN] = 11;
        expressionOpIndexes[JavaTokenTypes.BXOR_ASSIGN] = 11;
        expressionOpIndexes[JavaTokenTypes.BOR_ASSIGN] = 11;
        expressionOpIndexes[JavaTokenTypes.MOD_ASSIGN] = 11;
        expressionOpIndexes[JavaTokenTypes.DIV_ASSIGN] = 11;
        expressionOpIndexes[JavaTokenTypes.STAR_ASSIGN] = 11;
        expressionOpIndexes[JavaTokenTypes.MINUS_ASSIGN] = 11;
        expressionOpIndexes[JavaTokenTypes.PLUS_ASSIGN] = 11;
        expressionOpIndexes[JavaTokenTypes.ASSIGN] = 11;
        expressionOpIndexes[JavaTokenTypes.EQUAL] = 11;
        expressionOpIndexes[JavaTokenTypes.NOT_EQUAL] = 11;
        expressionOpIndexes[JavaTokenTypes.LT] = 11;
        expressionOpIndexes[JavaTokenTypes.LE] = 11;
        expressionOpIndexes[JavaTokenTypes.GT] = 11;
        expressionOpIndexes[JavaTokenTypes.GE] = 11;
        expressionOpIndexes[JavaTokenTypes.LAND] = 11;
        expressionOpIndexes[JavaTokenTypes.LOR] = 11;
        expressionOpIndexes[JavaTokenTypes.METHOD_REFERENCE] = 11;
    }
    
    /**
     * Check whether the given token type can lead an
     * expression.
     */
    private boolean isExpressionTokenType(int ttype)
    {
        return expressionTokenIndexes[ttype] != 0;
    }
    
    private void parseLambdaBody()
    {
        boolean blockFollows = tokenStream.LA(1).getType() == JavaTokenTypes.LCURLY;
        beginLambda(blockFollows, blockFollows ? tokenStream.LA(1) : null);
        if (blockFollows) {
            beginStmtblockBody(nextToken()); // consume the curly
            parseStmtBlock();
            LocatableToken token = nextToken();
            if (token.getType() != JavaTokenTypes.RCURLY) {
                error("Expecting '}' at end of lambda block");
                tokenStream.pushBack(token);
                endStmtblockBody(token, false);
            }
            else {
                endStmtblockBody(token, true);
                endLambda(token);
            }
        }
        else {
            parseExpression();
            endLambda(null);
        }
    }

    /**
     * A lambda expression has been found.  If lambdaIsBlock, a statement block body
     * follows, otherwise an expression follows.
     */
    protected void beginLambda(boolean lambdaIsBlock, LocatableToken openCurly) { }

    protected void endLambda(LocatableToken closeCurly) { }

    /**
     * Parse an expression
     */
    public void parseExpression()
    {
        LocatableToken token = nextToken();
        beginExpression(token);

        exprLoop:
        while (true) {
            int index = expressionTokenIndexes[token.getType()];
            switch (index) {
            case 1: // LITERAL_new
                // new XYZ(...)
                if (tokenStream.LA(1).getType() == JavaTokenTypes.EOF) {
                    gotIdentifierEOF(token);
                    endExpression(tokenStream.LA(1), true);
                    return;
                }
                parseNewExpression(token);
                break;
            case 2: // LCURLY
                // an initialiser list for an array
                do {
                    if (tokenStream.LA(1).getType() == JavaTokenTypes.RCURLY) {
                        token = nextToken(); // RCURLY
                        break;
                    }
                    parseExpression();
                    token = nextToken();
                } while (token.getType() == JavaTokenTypes.COMMA);
                if (token.getType() != JavaTokenTypes.RCURLY) {
                    errorBefore("Expected '}' at end of initialiser list expression", token);
                    tokenStream.pushBack(token);
                }
                break;
            case 3: // IDENT
                if (tokenStream.LA(1).getType() == JavaTokenTypes.LPAREN) {
                    // Method call
                    gotMethodCall(token);
                    parseArgumentList(nextToken());
                }
                else if (tokenStream.LA(1).getType() == JavaTokenTypes.LAMBDA) {
                    nextToken(); // consume LAMBDA symbol
                    parseLambdaBody();
                }
                else if (tokenStream.LA(1).getType() == JavaTokenTypes.DOT &&
                        tokenStream.LA(2).getType() == JavaTokenTypes.IDENT &&
                        tokenStream.LA(3).getType() != JavaTokenTypes.LPAREN) {
                    gotCompoundIdent(token);
                    nextToken(); // dot
                    token = tokenStream.nextToken();
                    while (tokenStream.LA(1).getType() == JavaTokenTypes.DOT &&
                            tokenStream.LA(2).getType() == JavaTokenTypes.IDENT &&
                            tokenStream.LA(3).getType() != JavaTokenTypes.LPAREN &&
                            tokenStream.LA(3).getType() != JavaTokenTypes.EOF)
                    {
                        gotCompoundComponent(token);
                        nextToken(); // dot
                        token = tokenStream.nextToken();
                    }
                    
                    // We either don't have a dot, or we do have a dot but not an
                    // identifier after it.
                    if (tokenStream.LA(1).getType() == JavaTokenTypes.DOT) {
                        LocatableToken dotToken = nextToken();
                        LocatableToken ntoken = nextToken();
                        if (ntoken.getType() == JavaTokenTypes.LITERAL_class) {
                            completeCompoundClass(token);
                            gotClassLiteral(ntoken);
                        }
                        else if (ntoken.getType() == JavaTokenTypes.LITERAL_this) {
                            completeCompoundClass(token);
                            // TODO gotThisAccessor
                        }
                        else if (ntoken.getType() == JavaTokenTypes.LITERAL_super) {
                            completeCompoundClass(token);
                            // TODO gotSuperAccessor
                        }
                        else {
                            completeCompoundValue(token);
                            // Treat dot as an operator (below)
                            tokenStream.pushBack(ntoken);
                            tokenStream.pushBack(dotToken);
                        }
                    }
                    else {
                        // No dot follows; last member
                        if (tokenStream.LA(1).getType() == JavaTokenTypes.EOF) {
                            completeCompoundValueEOF(token);
                        }
                        else {
                            if (tokenStream.LA(1).getType() == JavaTokenTypes.LBRACK
                                    && tokenStream.LA(2).getType() == JavaTokenTypes.RBRACK) {
                                completeCompoundClass(token);
                                parseArrayDeclarators();
                                if (tokenStream.LA(1).getType() == JavaTokenTypes.DOT &&
                                        tokenStream.LA(2).getType() == JavaTokenTypes.LITERAL_class) {
                                    token = nextToken();
                                    token = nextToken();
                                    gotClassLiteral(token);
                                }
                                else {
                                    error("Expecting \".class\"");
                                }
                            }
                            completeCompoundValue(token);
                        }
                    }
                }
                else if (tokenStream.LA(1).getType() == JavaTokenTypes.DOT) {
                    gotParentIdentifier(token);
                    if (tokenStream.LA(2).getType() == JavaTokenTypes.LITERAL_class) {
                        token = nextToken(); // dot
                        token = nextToken(); // class
                        gotClassLiteral(token);
                    }
                }
                else if (tokenStream.LA(1).getType() == JavaTokenTypes.LBRACK
                        && tokenStream.LA(2).getType() == JavaTokenTypes.RBRACK) {
                    gotArrayTypeIdentifier(token);
                    parseArrayDeclarators();
                    if (tokenStream.LA(1).getType() == JavaTokenTypes.DOT &&
                            tokenStream.LA(2).getType() == JavaTokenTypes.LITERAL_class) {
                        token = nextToken();
                        token = nextToken();
                        gotClassLiteral(token);
                    }
                    else {
                        error("Expecting \".class\"");
                    }
                }
                else if (tokenStream.LA(1).getType() == JavaTokenTypes.EOF) {
                    gotIdentifierEOF(token);
                }
                else {
                    gotIdentifier(token);
                }
                break;
            case 4: // LITERAL_this
            case 5: // LITERAL_super
                if (tokenStream.LA(1).getType() == JavaTokenTypes.LPAREN) {
                    // call to constructor or superclass constructor
                    gotConstructorCall(token);
                    parseArgumentList(nextToken());
                }
                else {
                    gotLiteral(token);
                }
                break;
            case 6: // STRING_LITERAL
            case 7: // CHAR_LITERAL
            case 8: // NUM_INT
            case 9: // NUM_LONG
            case 10: // NUM_DOUBLE
            case 11: // NUM_FLOAT
            case 12: // LITERAL_null
            case 13: // LITERAL_true
            case 14: // LITERAL_false
                // Literals need no further processing
                gotLiteral(token);
                break;
            case 15: // LPAREN
                // Either a parenthesised expression, or a type cast
                // We handle cast to primitive specially - it can be followed by +, ++, -, --
                // and yet be a cast.
                boolean isPrimitive = isPrimitiveType(tokenStream.LA(1));

                List<LocatableToken> tlist = new LinkedList<LocatableToken>();
                boolean isTypeSpec = parseTypeSpec(true, true, tlist);
                
                // We have a cast if
                // -it's a type spec
                // -it's followed by ')'
                // -it's not followed by an operator OR
                //  the type is primitive and the following operator is a unary operator
                //  OR following the ')' is '('
                // -it's not followed by an expression terminator - ; : , ) } ] EOF

                int tt2 = tokenStream.LA(2).getType();
                boolean isCast = isTypeSpec && tokenStream.LA(1).getType() == JavaTokenTypes.RPAREN && (tt2 != JavaTokenTypes.LAMBDA);
                if (tt2 != JavaTokenTypes.LPAREN) {
                    isCast &= !isOperator(tokenStream.LA(2)) || (isPrimitive
                            && isUnaryOperator(tokenStream.LA(2)));
                    isCast &= tt2 != JavaTokenTypes.SEMI && tt2 != JavaTokenTypes.RPAREN
                            && tt2 != JavaTokenTypes.RCURLY && tt2 != JavaTokenTypes.EOF;
                    isCast &= tt2 != JavaTokenTypes.COMMA && tt2 != JavaTokenTypes.COLON
                            && tt2 != JavaTokenTypes.RBRACK;
                    isCast &= tt2 != JavaTokenTypes.QUESTION;
                }
                
                if (isCast) {
                    // This surely must be type cast
                    gotTypeCast(tlist);
                    token = nextToken(); // RPAREN
                    token = nextToken();
                    continue exprLoop;
                }
                else {
                    // This may be either an expression OR a Lambda function.
                    //check if it is a Lambda function.

                    boolean isLambda = false;
                    if (isTypeSpec) {
                        if (tokenStream.LA(1).getType() == JavaTokenTypes.RPAREN && tt2 == JavaTokenTypes.LAMBDA) {
                            isLambda = true;
                        }
                        if (! isLambda && tokenStream.LA(1).getType() == JavaTokenTypes.IDENT) {
                            // A lambda parameter with name and type
                            isLambda = true;
                        }
                        if (! isLambda && tokenStream.LA(1).getType() == JavaTokenTypes.TRIPLE_DOT) {
                            // A lambda parameter with name and type
                            isLambda = true;
                        }
                    }
                    pushBackAll(tlist);
                    int tt1 = tokenStream.LA(1).getType();
                    tt2 = tokenStream.LA(2).getType();
                    if (! isLambda) {
                        if (tt1 == JavaTokenTypes.RPAREN && tt2 == JavaTokenTypes.LAMBDA) {
                            isLambda = true;
                        }
                        else if (isModifier(tokenStream.LA(1))) {
                            isLambda = true;
                        }
                        else if (tt1 == JavaTokenTypes.IDENT && tt2 == JavaTokenTypes.COMMA) {
                            isLambda = true;
                        }
                    }
                    
                    if (isLambda) {
                        // now we need to consume the tokens.
                        parseLambdaParameterList();
                        token = nextToken();
                        if (token.getType() == JavaTokenTypes.RPAREN) token = nextToken();
                        //Now we are expecting the lambda symbol.
                        if (token.getType() != JavaTokenTypes.LAMBDA){
                            error("Lambda identifier misplaced or not found");
                            endExpression(token, false);
                            return;
                        }
                        parseLambdaBody();
                        break;
                    } else {
                        //it is an expression.
                        parseExpression();
                        token = nextToken();
                        if (token.getType() != JavaTokenTypes.RPAREN) {
                            tokenStream.pushBack(token);
                            error("Unmatched '(' in expression; expecting ')'");
                            endExpression(token, false);
                            return;
                        }
                    }
                }
                break;
            case 16: // LITERAL_void
            case 17: // LITERAL_boolean
            case 18: // LITERAL_byte
            case 19: // LITERAL_char
            case 20: // LITERAL_short
            case 21: // LITERAL_int
            case 22: // LITERAL_long
            case 23: // LITERAL_float
            case 24: // LITERAL_double
                // Not really part of an expression, but may be followed by
                // .class or [].class  (eg int.class, int[][].class)
                gotPrimitiveTypeLiteral(token);
                parseArrayDeclarators();
                if (tokenStream.LA(1).getType() == JavaTokenTypes.DOT &&
                        tokenStream.LA(2).getType() == JavaTokenTypes.LITERAL_class) {
                    token = nextToken();
                    token = nextToken();
                    gotClassLiteral(token);
                }
                else {
                    error("Expecting \".class\"");
                }
                break;
            case 25: // PLUS
            case 26: // MINUS
            case 27: // LNOT
            case 28: // BNOT
            case 29: // INC
            case 30: // DEC
                // Unary operator
                gotUnaryOperator(token);
                token = nextToken();
                continue exprLoop;
            default:
                tokenStream.pushBack(token);
                error("Invalid expression token: " + token.getText());
                endExpression(token, true);
                return;
            }

            // Now we get an operator, or end of expression
            opLoop:
            while (true) {
                token = tokenStream.nextToken();
                switch (expressionOpIndexes[token.getType()]) {
                case 1: // RPAREN
                case 2: // SEMI
                case 3: // RBRACK
                case 4: // COMMA
                case 5: // COLON
                case 6: // EOF
                case 7: // RCURLY
                    // These are all legitimate expression endings
                    tokenStream.pushBack(token);
                    endExpression(token, false);
                    return;
                case 8: // LBRACK
                    // Array subscript?
                    if (tokenStream.LA(1).getType() == JavaTokenTypes.RBRACK) {
                        // No subscript means that this is a type - must be followed by
                        // ".class" normally. Eg Object[].class
                        token = nextToken(); // RBRACK
                        continue;
                    }
                    parseExpression();
                    token = nextToken();
                    if (token.getType() != JavaTokenTypes.RBRACK) {
                        error("Expected ']' after array subscript expression");
                        tokenStream.pushBack(token);
                    }
                    gotArrayElementAccess();
                    break;
                case 9: // LITERAL_instanceof
                    gotInstanceOfOperator(token);
                    parseTypeSpec(true);
                    break;
                case 10: // DOT
                    // Handle dot operator specially, as there are some special cases
                    LocatableToken opToken = token;
                    token = nextToken();
                    if (token.getType() == JavaTokenTypes.EOF) {
                        // Not valid, but may be useful for subclasses
                        gotDotEOF(opToken);
                        break opLoop;
                    }
                    LocatableToken la1 = tokenStream.LA(1);
                    if (la1.getType() == JavaTokenTypes.EOF
                            && la1.getColumn() == token.getEndColumn()
                            && la1.getLine() == token.getEndLine()) {
                        // Something that might look like a keyword, but might in fact
                        // be partially complete identifier.
                        String tokText = token.getText();
                        if (tokText != null && tokText.length() > 0) {
                            if (Character.isJavaIdentifierStart(tokText.charAt(0))) {
                                gotMemberAccessEOF(token);
                                // break opLoop;
                                continue;
                            }
                        }
                    }
                    
                    if (token.getType() == JavaTokenTypes.LITERAL_class) {
                        // Class literal: continue and look for another operator
                        continue;
                    }
                    else if (token.getType() == JavaTokenTypes.IDENT) {
                        if (tokenStream.LA(1).getType() == JavaTokenTypes.LPAREN) {
                            // Method call
                            gotMemberCall(token, Collections.<LocatableToken>emptyList());
                            parseArgumentList(nextToken());
                        }
                        else {
                            gotMemberAccess(token);
                        }
                        continue;
                    }
                    else if (token.getType() == JavaTokenTypes.LT) {
                        // generic method call
                        DepthRef dr = new DepthRef();
                        List<LocatableToken> ttokens = new LinkedList<LocatableToken>();
                        ttokens.add(token);
                        dr.depth = 1;
                        if (!parseTargs(false, ttokens, dr)) {
                            continue;  // we're a bit lost now really...
                        }
                        token = nextToken();
                        if (token.getType() != JavaTokenTypes.IDENT) {
                            error("Expecting method name (in call to generic method)");
                            continue;
                        }
                        gotMemberCall(token, ttokens);
                        token = nextToken();
                        if (token.getType() != JavaTokenTypes.LPAREN) {
                            error("Expecting '(' after method name");
                            continue;
                        }
                        parseArgumentList(token);
                        continue;
                    }
                    gotBinaryOperator(opToken);
                    break opLoop;
                case 11: // binary operator
                    if (token.getType() == JavaTokenTypes.METHOD_REFERENCE &&
                            tokenStream.LA(1).getType() == JavaTokenTypes.LITERAL_new) {
                        nextToken(); // consume LITERAL_new
                        continue;
                    }
                    else {
                        // Binary operators - need another operand
                        gotBinaryOperator(token);
                        token = nextToken();
                    }
                    break opLoop;
                    
                default:
                    if (token.getType() == JavaTokenTypes.INC
                            || token.getType() == JavaTokenTypes.DEC) {
                        // post operators (unary)
                        gotPostOperator(token);
                        continue;
                    }
                    else if (token.getType() == JavaTokenTypes.QUESTION) {
                        gotQuestionOperator(token);
                        parseExpression();
                        token = nextToken();
                        if (token.getType() != JavaTokenTypes.COLON) {
                            error("Expecting ':' (in ?: operator)");
                            tokenStream.pushBack(token);
                            endExpression(token, true);
                            return;
                        }
                        gotQuestionColon(token);
                        token = nextToken();
                        break opLoop;
                    }
                    else {
                        tokenStream.pushBack(token);
                        endExpression(token, false);
                        return;
                    }
                }
            }
        }
    }

    protected void gotPostOperator(LocatableToken token) { }

    protected void gotArrayTypeIdentifier(LocatableToken token)
    {
        gotIdentifier(token);        
    }

    protected void gotParentIdentifier(LocatableToken token)
    {
        gotIdentifier(token);        
    }

    public LocatableToken parseArrayInitializerList(LocatableToken token)
    {
        // an initialiser list for an array
        do {
            if (tokenStream.LA(1).getType() == JavaTokenTypes.RCURLY) {
                token = nextToken(); // RCURLY
                break;
            }
            parseExpression();
            token = nextToken();
        } while (token.getType() == JavaTokenTypes.COMMA);
        if (token.getType() != JavaTokenTypes.RCURLY) {
            errorBefore("Expected '}' at end of initialiser list expression", token);
            tokenStream.pushBack(token);
        }
        return token;
    }
    
    public void parseNewExpression(LocatableToken token)
    {
        // new XYZ(...)
        gotExprNew(token);
        token = nextToken();
        if (token.getType() != JavaTokenTypes.IDENT && !isPrimitiveType(token)) {
            tokenStream.pushBack(token);
            error("Expected type identifier after \"new\" (in expression)");
            endExprNew(token, false);
            return;
        }
        tokenStream.pushBack(token);
        parseTypeSpec(false);
        token = nextToken();

        if (token.getType() == JavaTokenTypes.LBRACK) {
            while (true) {
                // array dimensions
                boolean withDimension = false;
                if (tokenStream.LA(1).getType() != JavaTokenTypes.RBRACK) {
                    withDimension = true;
                    parseExpression();
                }
                token = nextToken();
                if (token.getType() != JavaTokenTypes.RBRACK) {
                    tokenStream.pushBack(token);
                    errorBefore("Expecting ']' after array dimension (in new ... expression)", token);
                    endExprNew(token, false);
                    return;
                }
                else {
                    gotNewArrayDeclarator(withDimension);
                }
                if (tokenStream.LA(1).getType() != JavaTokenTypes.LBRACK) {
                    break;
                }
                token = nextToken();
            }
            
            if (tokenStream.LA(1).getType() == JavaTokenTypes.LCURLY) {
                // Array initialiser list
                token = nextToken();
                beginArrayInitList(token);
                token = parseArrayInitializerList(token);
                endArrayInitList(token);
                endExprNew(token, token.getType() == JavaTokenTypes.RCURLY);
                return;
            }

            endExprNew(token, true);
            return;
        }

        if (token.getType() != JavaTokenTypes.LPAREN) {
            tokenStream.pushBack(token);
            error("Expected '(' or '[' after type name (in 'new ...' expression)");
            endExprNew(token, false);
            return;
        }
        parseArgumentList(token);

        if (tokenStream.LA(1).getType() == JavaTokenTypes.LCURLY) {
            // a class body (anonymous inner class)
            token = nextToken(); // LCURLY
            beginAnonClassBody(token, false);
            parseClassBody();
            token = nextToken();
            if (token.getType() != JavaTokenTypes.RCURLY) {
                error("Expected '}' at end of inner class body");
                tokenStream.pushBack(token);
                tokenStream.pushBack(token);
                endAnonClassBody(token, false);
                endExprNew(token, false);
                return;
            }
            endAnonClassBody(token, true);
        }
        endExprNew(token, true);
    }
    
    /**
     * Parse a comma-separated, possibly empty list of arguments to a method/constructor.
     * The closing ')' will be consumed by this method. 
     * @param token   the '(' token
     */
    public void parseArgumentList(LocatableToken token)
    {
        beginArgumentList(token);
        token = nextToken();
        if (token.getType() != JavaTokenTypes.RPAREN) {
            tokenStream.pushBack(token);
            do  {
                parseExpression();
                token = nextToken();
                endArgument();
            } while (token.getType() == JavaTokenTypes.COMMA);
            if (token.getType() != JavaTokenTypes.RPAREN) {
                errorBefore("Expecting ',' or ')' (in argument list)", token);
                tokenStream.pushBack(token);
            }
        }
        endArgumentList(token);
        return;
    }
    
    /**
     * Parse a list of formal parameters in Lambda (possibly empty)
     */
    public void parseLambdaParameterList()
    {
        LocatableToken token = nextToken();
        
        while (token.getType() != JavaTokenTypes.RPAREN
                && token.getType() != JavaTokenTypes.RCURLY) {
            tokenStream.pushBack(token);
            gotLambdaFormalParam();
            //parse modifiers if any
            List<LocatableToken> rval = parseModifiers();
            
            int tt1 = tokenStream.LA(1).getType();
            int tt2 = tokenStream.LA(2).getType();
            if (tt1 == JavaTokenTypes.IDENT && (tt2 == JavaTokenTypes.COMMA || tt2 == JavaTokenTypes.RPAREN)) {
                token = nextToken(); // identifier
                gotLambdaFormalName(token);
                token = nextToken();
            }
            else {
                if (! parseTypeSpec(false, true, rval)) {
                    modifiersConsumed();
                    error("Formal lambda parameter specified incorrectly");
                    return;
                }
                gotLambdaFormalType(rval);
                token = nextToken();
                if (token.getType() == JavaTokenTypes.TRIPLE_DOT) {
                    token = nextToken();
                }
                if (token.getType() != JavaTokenTypes.IDENT) {
                    modifiersConsumed();
                    error("Formal lambda parameter lacks a name");
                    return;
                }
                gotLambdaFormalName(token);
                parseArrayDeclarators();
                token = nextToken();
            }
            
            modifiersConsumed();
 
            if (token.getType() != JavaTokenTypes.COMMA) {
                break;
            }
            token = nextToken();
        }
        tokenStream.pushBack(token);
    }

    protected void gotLambdaFormalParam() { }
    protected void gotLambdaFormalName(LocatableToken name) { }
    protected void gotLambdaFormalType(List<LocatableToken> type) { }

    /**
     * Parse a list of formal parameters (possibly empty)
     */
    public void parseParameterList()
    {
        LocatableToken token = nextToken();
        while (token.getType() != JavaTokenTypes.RPAREN
                && token.getType() != JavaTokenTypes.RCURLY) {
            tokenStream.pushBack(token);

            beginFormalParameter(token);
            parseModifiers();
            parseTypeSpec(true);
            LocatableToken idToken = nextToken(); // identifier
            LocatableToken varargsToken = null;
            if (idToken.getType() == JavaTokenTypes.TRIPLE_DOT) {
                // var args
                varargsToken = idToken;
                idToken = nextToken();
            }
            if (idToken.getType() != JavaTokenTypes.IDENT) {
                error("Expected parameter identifier (in method parameter)");
                // TODO skip to next ',', ')' or '}' if there is one soon (LA(3)?)
                tokenStream.pushBack(idToken);
                return;
            }
            parseArrayDeclarators();
            gotMethodParameter(idToken, varargsToken);
            modifiersConsumed();
            token = nextToken();
            if (token.getType() != JavaTokenTypes.COMMA) {
                break;
            }
            token = nextToken();
        }
        tokenStream.pushBack(token);
    }

    protected void beginFormalParameter(LocatableToken token) { }

    private void pushBackAll(List<LocatableToken> tokens)
    {
        ListIterator<LocatableToken> i = tokens.listIterator(tokens.size());
        while (i.hasPrevious()) {
            tokenStream.pushBack(i.previous());
        }
    }

    private class DepthRef
    {
        int depth;
    }
}
