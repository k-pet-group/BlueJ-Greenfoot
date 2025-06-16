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

import java.io.Reader;
import java.util.List;

public class SourceParser extends JavaParserCallbacks {
    protected JavaTokenFilter tokenStream;
    protected LocatableToken lastToken;
    protected final SourceType sourceType;

    ParserBehavior parser;

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
        return new JavaLexer(r, kws);
    }

    public static TokenStream getLexer(Reader r, SourceType sourceType, boolean handleComments, boolean handleMultilineStrings)
    {
        Keywords kws = sourceType == SourceType.Kotlin ? new KotlinKeywords() : new JavaKeywords();
        return new JavaLexer(r, kws, handleComments, handleMultilineStrings);
    }

    private static TokenStream getLexer(Reader r, SourceType sourceType, int line, int col, int pos)
    {
        Keywords kws = sourceType == SourceType.Kotlin ? new KotlinKeywords() : new JavaKeywords();
        return new JavaLexer(r, kws, line, col, pos);
    }

    public SourceParser(Reader r) {
        TokenStream lexer = getLexer(r);
        tokenStream = new JavaTokenFilter(lexer, this);
        parser = new JavaParser(this);
        this.sourceType = SourceType.Java;

    }

    public SourceParser(Reader r, SourceType sourceType) {
        TokenStream lexer = getLexer(r, sourceType);
        tokenStream = new JavaTokenFilter(lexer, this);
        parser = sourceType == SourceType.Kotlin ? new KotlinParser(this) : new JavaParser(this);
        this.sourceType = sourceType;
    }

    public SourceParser(Reader r, SourceType sourceType, boolean handleComments)
    {
        TokenStream lexer = getLexer(r, sourceType, handleComments, true);
        tokenStream = new JavaTokenFilter(lexer, this);
        parser = sourceType == SourceType.Kotlin ? new KotlinParser(this) : new JavaParser(this);
        this.sourceType = sourceType;
    }

    public SourceParser(Reader r, SourceType sourceType, int line, int col, int pos) {
        TokenStream lexer = getLexer(r, sourceType, line, col, pos);
        tokenStream = new JavaTokenFilter(lexer, this);
        parser = sourceType == SourceType.Kotlin ? new KotlinParser(this) : new JavaParser(this);
        this.sourceType = sourceType;
    }

    public JavaTokenFilter getTokenStream() {
        return tokenStream;
    }

    public LocatableToken getLastToken() {
        return lastToken;
    }

    public LocatableToken setLastToken(LocatableToken lastToken) {
        this.lastToken = lastToken;
        return lastToken;
    }

    public void parseCU() {
        parser.parseCU();
    }

    public void parseCUpart(int state) {
        parser.parseCUpart(state);
    }

    public int parseTypeDefBegin() {
        return parser.parseTypeDefBegin();
    }

    public LocatableToken parseTypeDefPart2(boolean b) {
        return parser.parseTypeDefPart2(b);
    }

    public LocatableToken parseTypeBody(int type, LocatableToken last) {
        return parser.parseTypeBody(type, last);
    }

    public void parseClassElement(LocatableToken nextToken) {
        parser.parseClassElement(nextToken);
    }

    public LocatableToken parseStatement(LocatableToken last, boolean b) {
        return parser.parseStatement(last, b);
    }

    public LocatableToken parseStatement() {
        return parser.parseStatement(getTokenStream().nextToken(), false);
    }

    public final boolean parseTypeSpec(boolean processArray) {
        return parser.parseTypeSpec(processArray);
    }


    public boolean parseTypeSpec(boolean b, boolean b1, List<LocatableToken> ll) {
        return parser.parseTypeSpec(b, b1, ll);
    }

    public void parseImportStatement() {
        parser.parseImportStatement();
    }

    public void parseClassBody() {
        parser.parseClassBody();
    }

    public void parseExpression() {
        parser.parseExpression();
    }

    public LocatableToken parseVariableDeclarations() {
        return parser.parseVariableDeclarations();
    }

    public void parseTypeDef() {
        parser.parseTypeDef();
    }

    public void parseMethodParamsBody() {
        parser.parseMethodParamsBody();
    }
}
