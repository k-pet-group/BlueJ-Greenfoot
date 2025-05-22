/*
 This file is part of the BlueJ program. 
 Copyright (C) 2009,2010,2011,2012,2014,2016,2022,2024  Michael Kolling and John Rosenberg

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
package bluej.parser.lexer;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import bluej.parser.EscapedUnicodeReader;
import bluej.parser.TokenStream;

/**
 * A lexer for Kotlin source code.
 * 
 */
public class KotlinLexer implements TokenStream
{
    private static final Map<String, Integer> keywords;

    static {
        keywords = new HashMap<>();

        // Kotlin keywords
        keywords.put("package", KotlinTokenTypes.LITERAL_package);
        keywords.put("import", KotlinTokenTypes.LITERAL_import);
        keywords.put("class", KotlinTokenTypes.LITERAL_class);
        keywords.put("interface", KotlinTokenTypes.LITERAL_interface);
        keywords.put("fun", KotlinTokenTypes.LITERAL_fun);
        keywords.put("val", KotlinTokenTypes.LITERAL_val);
        keywords.put("var", KotlinTokenTypes.LITERAL_var);
        keywords.put("constructor", KotlinTokenTypes.LITERAL_constructor);
        keywords.put("by", KotlinTokenTypes.LITERAL_by);
        keywords.put("companion", KotlinTokenTypes.LITERAL_companion);
        keywords.put("init", KotlinTokenTypes.LITERAL_init);
        keywords.put("object", KotlinTokenTypes.LITERAL_object);
        keywords.put("typealias", KotlinTokenTypes.LITERAL_typealias);
        keywords.put("data", KotlinTokenTypes.LITERAL_data);

        // Control flow keywords
        keywords.put("if", KotlinTokenTypes.LITERAL_if);
        keywords.put("else", KotlinTokenTypes.LITERAL_else);
        keywords.put("while", KotlinTokenTypes.LITERAL_while);
        keywords.put("do", KotlinTokenTypes.LITERAL_do);
        keywords.put("for", KotlinTokenTypes.LITERAL_for);
        keywords.put("when", KotlinTokenTypes.LITERAL_when);
        keywords.put("break", KotlinTokenTypes.LITERAL_break);
        keywords.put("continue", KotlinTokenTypes.LITERAL_continue);
        keywords.put("return", KotlinTokenTypes.LITERAL_return);
        keywords.put("throw", KotlinTokenTypes.LITERAL_throw);
        keywords.put("try", KotlinTokenTypes.LITERAL_try);
        keywords.put("catch", KotlinTokenTypes.LITERAL_catch);
        keywords.put("finally", KotlinTokenTypes.LITERAL_finally);

        // Literals
        keywords.put("true", KotlinTokenTypes.LITERAL_true);
        keywords.put("false", KotlinTokenTypes.LITERAL_false);
        keywords.put("null", KotlinTokenTypes.LITERAL_null);
        keywords.put("this", KotlinTokenTypes.LITERAL_this);
        keywords.put("super", KotlinTokenTypes.LITERAL_super);

        // Modifiers
        keywords.put("public", KotlinTokenTypes.LITERAL_public);
        keywords.put("private", KotlinTokenTypes.LITERAL_private);
        keywords.put("protected", KotlinTokenTypes.LITERAL_protected);
        keywords.put("internal", KotlinTokenTypes.LITERAL_internal);
        keywords.put("final", KotlinTokenTypes.FINAL);
        keywords.put("abstract", KotlinTokenTypes.ABSTRACT);
    }

    private EscapedUnicodeReader reader;
    private int line;
    private int column;
    private int position;
    private boolean generateWhitespaceTokens;
    private boolean handleComments;
    private boolean handleMultilineStrings;
    private StringBuilder textBuffer = new StringBuilder();

    public KotlinLexer(Reader in)
    {
        this(in, true, true);
    }

    public KotlinLexer(Reader in, boolean handleComments, boolean handleMultilineStrings)
    {
        reader = new EscapedUnicodeReader(in);
        line = 1;
        column = 1;
        position = 0;
        this.handleComments = handleComments;
        this.handleMultilineStrings = handleMultilineStrings;
    }

    public KotlinLexer(Reader in, int line, int col, int position)
    {
        reader = new EscapedUnicodeReader(in);
        this.line = line;
        this.column = col;
        this.position = position;
        this.handleComments = true;
        this.handleMultilineStrings = true;
    }

    @Override
    public LocatableToken nextToken()
    {
        try {
            int c = reader.read();

            while (c != -1) {
                if (Character.isWhitespace(c)) {
                    if (c == '\n') {
                        line++;
                        column = 1;
                    }
                    else {
                        column++;
                    }
                    position++;
                    c = reader.read();
                    continue;
                }

                if (c == '/') {
                    int nextChar = reader.read();
                    if (nextChar == '/') {
                        // Skip single line comment
                        while ((c = reader.read()) != -1 && c != '\n') {
                            position++;
                            column++;
                        }
                        if (c == '\n') {
                            line++;
                            column = 1;
                            position++;
                        }
                        continue;
                    }
                    else if (nextChar == '*') {
                        // Skip multi-line comment
                        boolean endFound = false;
                        while (!endFound && (c = reader.read()) != -1) {
                            if (c == '*') {
                                if ((c = reader.read()) == '/') {
                                    endFound = true;
                                }
                                else {
                                    try {
                                        reader.pushBack(String.valueOf((char)c), new LineColPos(line, column, position));
                                    } catch (IOException e) {
                                        // Ignore
                                    }
                                }
                            }

                            if (c == '\n') {
                                line++;
                                column = 1;
                            }
                            else {
                                column++;
                            }
                            position++;
                        }
                        continue;
                    }
                    else {
                        try {
                            reader.pushBack(String.valueOf((char)nextChar), new LineColPos(line, column, position));
                        } catch (IOException e) {
                            // Ignore
                        }
                        return makeToken(KotlinTokenTypes.DIV, "/");
                    }
                }

                if (Character.isJavaIdentifierStart(c)) {
                    return createWordToken((char)c);
                }

                if (c == '"') {
                    return getStringLiteral();
                }

                if (Character.isDigit(c)) {
                    return getNumberToken((char)c);
                }

                // Handle simple symbols
                switch (c) {
                    case '(':
                        return makeToken(KotlinTokenTypes.LPAREN, "(");
                    case ')':
                        return makeToken(KotlinTokenTypes.RPAREN, ")");
                    case '{':
                        return makeToken(KotlinTokenTypes.LCURLY, "{");
                    case '}':
                        return makeToken(KotlinTokenTypes.RCURLY, "}");
                    case '[':
                        return makeToken(KotlinTokenTypes.LBRACK, "[");
                    case ']':
                        return makeToken(KotlinTokenTypes.RBRACK, "]");
                    case ';':
                        return makeToken(KotlinTokenTypes.SEMI, ";");
                    case ',':
                        return makeToken(KotlinTokenTypes.COMMA, ",");
                    case '.':
                        return makeToken(KotlinTokenTypes.DOT, ".");
                    case ':':
                        return makeToken(KotlinTokenTypes.COLON, ":");
                    case '=':
                        return makeToken(KotlinTokenTypes.ASSIGN, "=");
                    case '+':
                        return makeToken(KotlinTokenTypes.PLUS, "+");
                    case '-':
                        return makeToken(KotlinTokenTypes.MINUS, "-");
                    case '*':
                        return makeToken(KotlinTokenTypes.STAR, "*");
                    case '%':
                        return makeToken(KotlinTokenTypes.MOD, "%");
                    case '!':
                        return makeToken(KotlinTokenTypes.LNOT, "!");
                    case '~':
                        return makeToken(KotlinTokenTypes.BNOT, "~");
                    case '?':
                        return makeToken(KotlinTokenTypes.QUESTION, "?");
                    case '<':
                        return makeToken(KotlinTokenTypes.LT, "<");
                    case '>':
                        return makeToken(KotlinTokenTypes.GT, ">");
                    case '&':
                        return makeToken(KotlinTokenTypes.BAND, "&");
                    case '|':
                        return makeToken(KotlinTokenTypes.BOR, "|");
                    case '^':
                        return makeToken(KotlinTokenTypes.BXOR, "^");
                    case '@':
                        return makeToken(KotlinTokenTypes.AT, "@");
                    default:
                        // Unknown character
                        column++;
                        position++;
                        c = reader.read();
                        continue;
                }
            }

            // End of file
            return makeToken(KotlinTokenTypes.EOF, "");
        }
        catch (IOException e) {
            // Handle IO exception
            return makeToken(KotlinTokenTypes.EOF, "");
        }
    }

    private LocatableToken makeToken(int type, String txt)
    {
        LineColPos begin = new LineColPos(line, column - txt.length(), position - txt.length());
        LineColPos end = new LineColPos(line, column, position);
        return new LocatableToken(type, txt, begin, end);
    }

    private LocatableToken createWordToken(char firstChar)
    {
        textBuffer.setLength(0);
        textBuffer.append(firstChar);

        int startLine = line;
        int startCol = column;
        int startPos = position;

        column++;
        position++;

        try {
            int c;
            while ((c = reader.read()) != -1 && Character.isJavaIdentifierPart(c)) {
                textBuffer.append((char)c);
                column++;
                position++;
            }

            if (c != -1) {
                try {
                    reader.pushBack(String.valueOf((char)c), new LineColPos(line, column, position));
                } catch (IOException e) {
                    // Ignore
                }
            }

            String word = textBuffer.toString();
            Integer keywordType = keywords.get(word);

            if (keywordType != null) {
                return new LocatableToken(keywordType, word, 
                        new LineColPos(startLine, startCol, startPos),
                        new LineColPos(line, column, position));
            }
            else {
                return new LocatableToken(KotlinTokenTypes.IDENT, word, 
                        new LineColPos(startLine, startCol, startPos),
                        new LineColPos(line, column, position));
            }
        }
        catch (IOException e) {
            // Handle IO exception
            return new LocatableToken(KotlinTokenTypes.IDENT, textBuffer.toString(), 
                    new LineColPos(startLine, startCol, startPos),
                    new LineColPos(line, column, position));
        }
    }

    private LocatableToken getStringLiteral()
    {
        textBuffer.setLength(0);

        int startLine = line;
        int startCol = column;
        int startPos = position;

        textBuffer.append('"');
        column++;
        position++;

        try {
            boolean endFound = false;
            boolean escapeNext = false;

            while (!endFound) {
                int c = reader.read();

                if (c == -1 || (!escapeNext && c == '\n')) {
                    // Unterminated string
                    return new LocatableToken(KotlinTokenTypes.INVALID, textBuffer.toString(), 
                            new LineColPos(startLine, startCol, startPos),
                            new LineColPos(line, column, position));
                }

                textBuffer.append((char)c);

                if (c == '\n') {
                    line++;
                    column = 1;
                }
                else {
                    column++;
                }
                position++;

                if (escapeNext) {
                    escapeNext = false;
                    continue;
                }

                if (c == '\\') {
                    escapeNext = true;
                    continue;
                }

                if (c == '"') {
                    // End of string
                    endFound = true;
                }
            }

            return new LocatableToken(KotlinTokenTypes.STRING_LITERAL, textBuffer.toString(), 
                    new LineColPos(startLine, startCol, startPos),
                    new LineColPos(line, column, position));
        }
        catch (IOException e) {
            // Handle IO exception
            return new LocatableToken(KotlinTokenTypes.INVALID, textBuffer.toString(), 
                    new LineColPos(startLine, startCol, startPos),
                    new LineColPos(line, column, position));
        }
    }

    private LocatableToken getNumberToken(char firstChar)
    {
        textBuffer.setLength(0);

        int startLine = line;
        int startCol = column;
        int startPos = position;

        textBuffer.append(firstChar);
        column++;
        position++;

        try {
            boolean isFloat = false;

            while (true) {
                int c = reader.read();

                if (c == -1) {
                    break;
                }

                if (Character.isDigit(c)) {
                    textBuffer.append((char)c);
                    column++;
                    position++;
                    continue;
                }

                if (c == '.' && !isFloat) {
                    isFloat = true;
                    textBuffer.append((char)c);
                    column++;
                    position++;
                    continue;
                }

                try {
                    reader.pushBack(String.valueOf((char)c), new LineColPos(line, column, position));
                } catch (IOException e) {
                    // Ignore
                }
                break;
            }

            String number = textBuffer.toString();

            if (isFloat) {
                return new LocatableToken(KotlinTokenTypes.NUM_FLOAT, number, 
                        new LineColPos(startLine, startCol, startPos),
                        new LineColPos(line, column, position));
            }
            else {
                return new LocatableToken(KotlinTokenTypes.NUM_INT, number, 
                        new LineColPos(startLine, startCol, startPos),
                        new LineColPos(line, column, position));
            }
        }
        catch (IOException e) {
            // Handle IO exception
            return new LocatableToken(KotlinTokenTypes.INVALID, textBuffer.toString(), 
                    new LineColPos(startLine, startCol, startPos),
                    new LineColPos(line, column, position));
        }
    }

    public static TokenStream getLexer(Reader r)
    {
        return new KotlinLexer(r);
    }

    public static TokenStream getLexer(Reader r, boolean handleComments, boolean handleMultilineStrings)
    {
        return new KotlinLexer(r, handleComments, handleMultilineStrings);
    }

    private static TokenStream getLexer(Reader r, int line, int col, int pos)
    {
        return new KotlinLexer(r, line, col, pos);
    }
}
