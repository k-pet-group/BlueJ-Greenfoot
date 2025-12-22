/*
 This file is part of the BlueJ program.
 Copyright (C) 2025  Michael Kolling and John Rosenberg

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

import bluej.extensions2.SourceType;
import bluej.parser.lexer.*;
import bluej.parser.psi.SourceInput;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

public class SourceParser extends JavaParserCallbacksBase {
    protected JavaTokenFilter tokenStream;
    protected LocatableToken lastToken;
    protected final SourceType sourceType;
    protected boolean handleComments = true;
    protected boolean handleMultilineStrings = true;
    protected TokenStream lexer;
    
    /** Source input for file-based parsing (null for Reader-based parsing) */
    private final SourceInput sourceInput;

    private ParserBehavior parser;
    private LineColPos position = new LineColPos(1, 1, 0);

    LineColPos getOffset() {
        return this.tokenStream.getOffset();
    }

    /*
    public static TokenStream getLexer(Reader r)
    {
        return new JavaLexer(r);
    }

    public static TokenStream getLexer(Reader r, boolean handleComments, boolean handleMultilineStrings)
    {
        return new JavaLexer(r, handleComments, handleMultilineStrings);
    }

    private static TokenStream getLexer(Reader r, int line, int col, int pos)
    {
        return new JavaLexer(r, line, col, pos);
    }

    public static TokenStream getLexer(Reader r, SourceType sourceType)
    {
        Keywords kws = sourceType == SourceType.Kotlin ? new KotlinKeywords() : new JavaKeywords();
        var lexer = new JavaLexer(r, kws);

        if (sourceType == SourceType.Kotlin) {
            lexer.setGenerateWhitespaceTokens(true);
        }

        return lexer;
    }

    public static TokenStream getLexer(Reader r, SourceType sourceType, boolean handleComments, boolean handleMultilineStrings)
    {
        Keywords kws = sourceType == SourceType.Kotlin ? new KotlinKeywords() : new JavaKeywords();
        var lexer = new JavaLexer(r, kws, handleComments, handleMultilineStrings);

        if (sourceType == SourceType.Kotlin) {
            lexer.setGenerateWhitespaceTokens(true);
        }

        return lexer;
    }

    private static TokenStream getLexer(Reader r, SourceType sourceType, int line, int col, int pos)
    {
        Keywords kws = sourceType == SourceType.Kotlin ? new KotlinKeywords() : new JavaKeywords();
        var lexer = new JavaLexer(r, kws, line, col, pos);

        if (sourceType == SourceType.Kotlin) {
//            lexer.setGenerateWhitespaceTokens(true);
        }

        return lexer;
    }

     */
    
    public SourceParser setStartPosition(LineColPos position) {
        this.lexer = null;
        this.tokenStream = null;
        this.position = position;

        return this;
    }

    protected TokenStream getLexer() {
        if (lexer != null) { return lexer; }

        try {
            Keywords kws = sourceType == SourceType.Kotlin ? new KotlinKeywords() : new JavaKeywords();
            Reader reader =  getSourceInput().createReader();

            var lexer = new JavaLexer(
                reader,
                kws,
                this.position.line(),
                this.position.column(),
                this.position.position()
            );

            // TODO: handle that later

//            lexer.setHandleComments(false);

//            lexer.handleComments = handleComments,
//            handleMultilineStrings,

            if (sourceType == SourceType.Kotlin) {
//                lexer.setGenerateWhitespaceTokens(true);
            }

            return lexer;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isHandleComments() {
        return handleComments;
    }

    public SourceParser setHandleComments(boolean handleComments) {
        this.handleComments = handleComments;
        return this;
    }

    public boolean isHandleMultilineStrings() {
        return handleMultilineStrings;
    }

    public SourceParser setHandleMultilineStrings(boolean handleMultilineStrings) {
        this.handleMultilineStrings = handleMultilineStrings;
        return this;
    }

    /**
     * Creates parser from SourceInput (recommended for file-based parsing).
     *
     * @param input Source input encapsulating file and metadata
     */
    public SourceParser(SourceInput input) {
        if (input == null) {
            throw new NullPointerException("input cannot be null");
        }
        
        this.sourceInput = input;
        this.sourceType = input.sourceType();
    }

    public SourceParser(SourceInput input, int line, int col, int pos) {
        this(input);
    }

    protected ParserBehavior getParserImplementation() {
        if (parser != null) { return parser; }

        return parser = sourceType == SourceType.Kotlin
                ? new KotlinPsiParser(this)
                : new JavaParser(this);
    }

    public JavaTokenFilter getTokenStream() {
        if (tokenStream != null) { return tokenStream; }

        TokenStream lexer = getLexer();

        tokenStream = new JavaTokenFilter(lexer, this);

        return tokenStream;
    }

    public LocatableToken getLastToken() {
        return getTokenStream().getMostRecent();
    }

    public LocatableToken setLastToken(LocatableToken lastToken) {
//        this.lastToken = lastToken;
//        return lastToken;
        return getLastToken();
    }

    public void parseCU() {
        getParserImplementation().parseCU();
    }

    public int parseCUpart(int state) {
        return getParserImplementation().parseCUpart(state);
    }

    public int parseTypeDefBegin() {
        return getParserImplementation().parseTypeDefBegin();
    }

    public LocatableToken parseTypeDefPart2(boolean b) {
        return getParserImplementation().parseTypeDefPart2(b);
    }

    public LocatableToken parseTypeBody(int type, LocatableToken last) {
        return getParserImplementation().parseTypeBody(type, last);
    }

    public void parseClassElement(LocatableToken nextToken) {
        getParserImplementation().parseClassElement(nextToken);
    }

    public LocatableToken parseStatement(LocatableToken last, boolean b) {
        return getParserImplementation().parseStatement(last, b);
    }

    public LocatableToken parseStatement() {
        return getParserImplementation().parseStatement(getTokenStream().nextToken(), false);
    }

    public final boolean parseTypeSpec(boolean processArray) {
        return getParserImplementation().parseTypeSpec(processArray);
    }

    public final LocatableToken parsePackageStmt(LocatableToken token) {
        return getParserImplementation().parsePackageStmt(token);
    }

    public boolean parseTypeSpec(boolean b, boolean b1, List<LocatableToken> ll) {
        return getParserImplementation().parseTypeSpec(b, b1, ll);
    }

    public void parseImportStatement() {
        getParserImplementation().parseImportStatement();
    }

    public void parseImportStatement(LocatableToken token) {
        getParserImplementation().parseImportStatement(token);
    }

    public void parseClassBody() {
        getParserImplementation().parseClassBody();
    }

    public void parseExpression() {
        getParserImplementation().parseExpression();
    }

    public LocatableToken parseVariableDeclarations() {
        return getParserImplementation().parseVariableDeclarations();
    }

    public void parseTypeDef() {
        getParserImplementation().parseTypeDef();
    }

    public void parseTypeDef(LocatableToken token) {
        getParserImplementation().parseTypeDef(token);
    }

    public void parseMethodParamsBody() {
        getParserImplementation().parseMethodParamsBody();
    }
    
    /**
     * Gets the source input for PSI access.
     *
     * @return Source input, or null if parser created from Reader
     */
    public SourceInput getSourceInput() {
        return sourceInput;
    }

    public boolean isModifier(LocatableToken lt) {
        return getParserImplementation().isModifier(lt);
    }

    public boolean isPrimitiveType(LocatableToken lt) {
        return getParserImplementation().isPrimitiveType(lt);
    }
}
