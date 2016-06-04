/*
 This file is part of the BlueJ program. 
 Copyright (C) 2009,2010,2011,2012,2014,2016  Michael Kolling and John Rosenberg 

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
 * A Java lexer. Breaks up a source stream into tokens.
 * 
 * @author Marion Zalk
 */
public final class JavaLexer implements TokenStream
{
    private StringBuffer textBuffer = new StringBuffer(); // text of current token
    private EscapedUnicodeReader reader;
    private int rChar; 
    private int beginColumn, beginLine, beginPosition;
    private int endColumn, endLine, endPosition;
    private boolean generateWhitespaceTokens = false;
    private boolean handleComments = true; // When false, doesn't recognise /*..*/ or //..\n as comments (for frames)
    
    private static Map<String,Integer> keywords = new HashMap<String,Integer>();
    
    static {
        keywords.put("abstract", JavaTokenTypes.ABSTRACT);
        keywords.put("assert", JavaTokenTypes.LITERAL_assert);
        keywords.put("boolean", JavaTokenTypes.LITERAL_boolean);
        keywords.put("break", JavaTokenTypes.LITERAL_break);
        keywords.put("byte", JavaTokenTypes.LITERAL_byte);
        keywords.put("case", JavaTokenTypes.LITERAL_case);
        keywords.put("catch", JavaTokenTypes.LITERAL_catch);
        keywords.put("char", JavaTokenTypes.LITERAL_char);
        keywords.put("class", JavaTokenTypes.LITERAL_class);
        keywords.put("continue", JavaTokenTypes.LITERAL_continue);
        keywords.put("default", JavaTokenTypes.LITERAL_default);
        keywords.put("do", JavaTokenTypes.LITERAL_do);
        keywords.put("double", JavaTokenTypes.LITERAL_double);
        keywords.put("else", JavaTokenTypes.LITERAL_else);
        keywords.put("enum", JavaTokenTypes.LITERAL_enum);
        keywords.put("extends", JavaTokenTypes.LITERAL_extends);
        keywords.put("false", JavaTokenTypes.LITERAL_false);
        keywords.put("final", JavaTokenTypes.FINAL);
        keywords.put("finally", JavaTokenTypes.LITERAL_finally);
        keywords.put("float", JavaTokenTypes.LITERAL_float);
        keywords.put("for", JavaTokenTypes.LITERAL_for);
        keywords.put("goto", JavaTokenTypes.GOTO);
        keywords.put("if", JavaTokenTypes.LITERAL_if);
        keywords.put("implements", JavaTokenTypes.LITERAL_implements);
        keywords.put("import", JavaTokenTypes.LITERAL_import);
        keywords.put("instanceof", JavaTokenTypes.LITERAL_instanceof);
        keywords.put("int", JavaTokenTypes.LITERAL_int);
        keywords.put("interface", JavaTokenTypes.LITERAL_interface);
        keywords.put("long", JavaTokenTypes.LITERAL_long);
        keywords.put("native", JavaTokenTypes.LITERAL_native);
        keywords.put("new", JavaTokenTypes.LITERAL_new);
        keywords.put("null", JavaTokenTypes.LITERAL_null);
        keywords.put("package", JavaTokenTypes.LITERAL_package);
        keywords.put("private", JavaTokenTypes.LITERAL_private);
        keywords.put("protected", JavaTokenTypes.LITERAL_protected);
        keywords.put("public", JavaTokenTypes.LITERAL_public);
        keywords.put("return", JavaTokenTypes.LITERAL_return);
        keywords.put("short", JavaTokenTypes.LITERAL_short);
        keywords.put("static", JavaTokenTypes.LITERAL_static);
        keywords.put("strictfp", JavaTokenTypes.STRICTFP);
        keywords.put("super", JavaTokenTypes.LITERAL_super);
        keywords.put("switch", JavaTokenTypes.LITERAL_switch);
        keywords.put("synchronized", JavaTokenTypes.LITERAL_synchronized);
        keywords.put("this", JavaTokenTypes.LITERAL_this);
        keywords.put("throw", JavaTokenTypes.LITERAL_throw);
        keywords.put("throws", JavaTokenTypes.LITERAL_throws);
        keywords.put("transient", JavaTokenTypes.LITERAL_transient);
        keywords.put("true", JavaTokenTypes.LITERAL_true);
        keywords.put("try", JavaTokenTypes.LITERAL_try);
        keywords.put("volatile", JavaTokenTypes.LITERAL_volatile);
        keywords.put("while", JavaTokenTypes.LITERAL_while);
        keywords.put("void", JavaTokenTypes.LITERAL_void);
    }

    /**
     * Construct a lexer which readers from the given Reader.
     */
    public JavaLexer(Reader in)
    {
        this(in, 1, 1, 0);
    }
    
    /**
     * Construct a lexer which readers from the given Reader.
     */
    public JavaLexer(Reader in, boolean handleComments)
    {
        this(in, 1, 1, 0);
        this.handleComments = handleComments;
    }

    /**
     * Construct a lexer which readers from the given Reader, assuming that the
     * reader is already positioned at the given line and column within the source
     * document.
     */
    public JavaLexer(Reader in, int line, int col, int position)
    {
        reader = new EscapedUnicodeReader(in);
        reader.setLineColPos(line, col, position);
        endColumn = beginColumn = col;
        endLine = beginLine = line;
        endPosition = beginPosition = position;
        try {
            rChar = reader.read();
        }
        catch (IOException ioe) {
            rChar = -1;
        }
    }
    
    /**
     * Retrieve the next token.
     */
    public LocatableToken nextToken()
    {  
        textBuffer.setLength(0);
        
        if (generateWhitespaceTokens && Character.isWhitespace((char)rChar))
        {
            StringBuilder whitespaceBuffer = new StringBuilder();
            while (Character.isWhitespace((char)rChar))
            {
                whitespaceBuffer.append((char)rChar);                
                readNextChar();
            }
            return makeToken(JavaTokenTypes.WHITESPACE, whitespaceBuffer.toString());
        }
        else
        {        
            while (Character.isWhitespace((char)rChar)) {
                beginLine = reader.getLine();
                beginColumn = reader.getColumn();
                beginPosition = reader.getPosition();
                readNextChar();
            }
        }

        if (rChar == -1) {
            // EOF
            return makeToken(JavaTokenTypes.EOF, null); 
        }
        
        char nextChar = (char) rChar;
        if (Character.isJavaIdentifierStart(nextChar)) {
            return createWordToken(nextChar); 
        }
        if (Character.isDigit(nextChar)) {
            return makeToken(readDigitToken(nextChar, false), textBuffer.toString());
        }
        return makeToken(getSymbolType(nextChar), textBuffer.toString());
    }
    
    /**
     * Make a token of the given type, with the given text. The token
     * begins where the previous token ended, and ends at the current
     * position (as found in endLine and endColumn).
     */
    private LocatableToken makeToken(int type, String txt)
    {           
        LocatableToken tok = new LocatableToken(type, txt);
        tok.setPosition(beginLine, beginColumn, endLine, endColumn, beginPosition, endPosition - beginPosition);
        beginColumn = endColumn;
        beginLine = endLine;
        beginPosition = endPosition;
        return tok;
    }

    private LocatableToken createWordToken(char nextChar)
    {
        populateTextBuffer(nextChar);
        return makeToken(getWordType(), textBuffer.toString());
    }

    private void populateTextBuffer(char ch)
    {
        char thisChar=ch;
        do {  
            textBuffer.append(thisChar);
            int rval = readNextChar();
            if (rval==-1){
                //eof
                return;
            }
            thisChar=(char)rval;
        } while (Character.isJavaIdentifierPart(thisChar));
    }

    private boolean getTokenText(char endChar)
    {
        char thisChar=endChar;
        int rval=0;     
        boolean complete = false;
        boolean escape = false;
        while (!complete){  
            rval=readNextChar();
            //eof
            if (rval==-1){
                return false;
            }
            thisChar = (char)rval; 
            if (thisChar=='\n'){
                return false;
            }

            textBuffer.append(thisChar);
            if (! escape) {
                if (thisChar == '\\') {
                    escape = true;
                }
                //endChar is the flag for the end of reading
                if (thisChar == endChar)  {
                    readNextChar();
                    return true;
                }
            }
            else {
                escape = false;
            }
        }
        return complete;
    }

    private boolean isHexDigit(char ch)
    {
        if (Character.isDigit(ch)) {
            return true;
        }
        
        if (ch >= 'a' && ch <= 'f') {
            return true;
        }
        
        if (ch >= 'A' && ch <= 'F') {
            return true;
        }
        
        return false;
    }
    
    /**
     * Read a numerical literal token.
     * 
     * @param ch   The first character of the token (must be a decimal digit)
     * @param dot  Whether there was a leading dot
     */
    private int readDigitToken(char ch, boolean dot)
    {
        int rval = ch;
        textBuffer.append(ch);
        int type = dot ? JavaTokenTypes.NUM_DOUBLE : JavaTokenTypes.NUM_INT;

        boolean fpValid = true; // whether a subsequent dot would be valid.
                // (will be set false for a non-decimal literal).
        
        if (ch == '0' && ! dot) {
            rval = readNextChar();
            if (rval == 'x' || rval == 'X') {
                // hexadecimal
                textBuffer.append((char) rval);
                rval = readNextChar();
                if (!isHexDigit((char)rval)) {
                    return JavaTokenTypes.INVALID;
                }
                
                do {
                    textBuffer.append((char) rval);
                    rval = readNextChar();
                } while (isHexDigit((char) rval) || rval == '_');
                if (rval == 'p' || rval == 'P') {
                    // super-funky semi-hexadecimal floating point literal
                    textBuffer.append((char) rval);
                    return superFunkyHFPL();
                }
                fpValid = false;
            }
            else if (rval == 'b' || rval == 'B') {
                // Java 7 binary literal
                textBuffer.append((char) rval);
                rval = readNextChar();
                if (rval != '0' && rval != '1') {
                    return JavaTokenTypes.INVALID;
                }
                
                do {
                    textBuffer.append((char) rval);
                    rval = readNextChar();
                } while (rval == '0' || rval == '1' || rval == '_');
                fpValid = false;
            }
            else if (Character.isDigit((char) rval)) {
                do {
                    // octal integer literal, or floating-point literal with leading 0
                    textBuffer.append((char) rval);
                    rval = readNextChar();
                } while (Character.isDigit((char) rval) || rval == '_');
            }
            ch = (char) rval;
        }
        else {
            rval = readNextChar();
            while (Character.isDigit((char) rval) || rval == '_') {
                textBuffer.append((char) rval);
                rval = readNextChar();
            }
        }
        
        if (rval == '.' && fpValid) {
            // A decimal.
            textBuffer.append((char) rval);
            rval = readNextChar();
            while (Character.isDigit((char) rval) || rval == '_') {
                textBuffer.append((char) rval);
                rval = readNextChar();
            }
            if (rval == 'e' || rval == 'E') {
                // exponent
                textBuffer.append((char) rval);
                rval = readNextChar();
                while (Character.isDigit((char) rval) || rval == '_') {
                    textBuffer.append((char) rval);
                    rval = readNextChar();
                }
            }
            
            // Check for type suffixes
            if (rval == 'f' || rval == 'F') {
                textBuffer.append((char) rval);
                rval = readNextChar();
                return JavaTokenTypes.NUM_FLOAT;
            }
            if (rval == 'd' || rval == 'D') {
                textBuffer.append((char) rval);
                rval = readNextChar();
            }
            return JavaTokenTypes.NUM_DOUBLE;
        }
        
        if ((rval == 'e' || rval == 'E') && fpValid) {
            // exponent
            textBuffer.append((char) rval);
            rval = readNextChar();
            while (Character.isDigit((char) rval) || rval == '_') {
                textBuffer.append((char) rval);
                rval = readNextChar();
            }
            type = JavaTokenTypes.NUM_DOUBLE;
        }
        else if (rval == 'l' || rval == 'L') {
            textBuffer.append((char) rval);
            rval = readNextChar();
            return JavaTokenTypes.NUM_LONG;
        }
        
        if (fpValid) {
            if (rval == 'f' || rval == 'F') {
                textBuffer.append((char) rval);
                rval = readNextChar();
                return JavaTokenTypes.NUM_FLOAT;
            }
            if (rval == 'd' || rval == 'D') {
                textBuffer.append((char) rval);
                rval = readNextChar();
                return JavaTokenTypes.NUM_DOUBLE;
            }
        }
        
        return type;
    }

    private int superFunkyHFPL()
    {
        // A super-funky semi-hexadecimal floating point literal looks like this:
        //   0xABCp12f
        // The 'ABC' is in hex, the 'p' can also be 'P', the '12' is a *decimal*
        // representation of the *power 2* exponent (may be negative); the 'f' (or 'F')
        // marks as a float rather than the default double ('d' or 'D').
        // So the above represents 0xABC * 2^123, or 0xABC << 12, as a float.
        
        // Up to this point, we've seen the 'p'.
        int rval = readNextChar();
        if (rval == -1) {
            return JavaTokenTypes.INVALID;
        }
        if (! Character.isDigit((char) rval) && rval != '-') {
            return JavaTokenTypes.INVALID;
        }
        
        textBuffer.append((char) rval);
        rval = readNextChar();
        while (Character.isDigit((char) rval)) {
            textBuffer.append((char) rval);
            rval = readNextChar();
        }
        
        if (rval == 'f' || rval == 'F') {
            textBuffer.append((char) rval);
            readNextChar();
            return JavaTokenTypes.NUM_FLOAT;
        }
        
        if (rval == 'd' || rval == 'D') {
            textBuffer.append((char) rval);
            readNextChar();
        }
        
        return JavaTokenTypes.NUM_DOUBLE;
    }
    
    private int getMLCommentType(char ch)
    {
        do{
            textBuffer.append(ch);
            int rval = readNextChar();
            if (rval == -1) {
                //eof
                return JavaTokenTypes.INVALID;
            }

            ch=(char)rval;
            while (ch=='*') {
                textBuffer.append((char)rval);
                rval = readNextChar();
                if (rval == -1) {
                    return JavaTokenTypes.INVALID;
                }
                if (rval == '/') {
                    textBuffer.append((char)rval);
                    readNextChar();
                    return JavaTokenTypes.ML_COMMENT;
                }
                ch=(char)rval; 
            }             
        } while (true);
    }
    
    private int getSLCommentType(char ch)
    {
        int rval=ch;     

        do{  
            textBuffer.append((char)rval);
            rval=readNextChar();
            //eof
            if (rval==-1 || rval == '\n') {
                return JavaTokenTypes.SL_COMMENT;
            }
        } while (true);
    }

    private int getSymbolType(char ch)
    {
        int type= JavaTokenTypes.INVALID;
        textBuffer.append(ch); 
        if ('"' == ch)
            return getStringLiteral();
        if ('\'' == ch)
            return getCharLiteral();
        if ('?' == ch) {
            readNextChar();
            return JavaTokenTypes.QUESTION;
        }
        if (',' == ch) {
            readNextChar();
            return JavaTokenTypes.COMMA;
        }
        if (';' == ch) {
            readNextChar();
            return JavaTokenTypes.SEMI;
        }
        if (':' == ch) {
            int rval = readNextChar();
            if (rval == ':') {
                textBuffer.append((char)rval);
                readNextChar();
                return JavaTokenTypes.METHOD_REFERENCE;
            }
            return JavaTokenTypes.COLON;
        }
        if ('^' == ch)
            return getBXORType();
        if ('~' == ch) {
            readNextChar();
            return JavaTokenTypes.BNOT;
        }
        if ('(' == ch) {
            readNextChar();
            return JavaTokenTypes.LPAREN;
        }
        if (')' == ch) {
            readNextChar();
            return JavaTokenTypes.RPAREN;
        }
        if ('[' == ch) {
            readNextChar();
            return JavaTokenTypes.LBRACK;
        }
        if (']' == ch) {
            readNextChar();
            return JavaTokenTypes.RBRACK;
        }
        if ('{' == ch) {
            readNextChar();
            return JavaTokenTypes.LCURLY;
        }
        if ('}' == ch) {
            readNextChar();
            return JavaTokenTypes.RCURLY;
        }
        if ('@' == ch) {
            readNextChar();
            return JavaTokenTypes.AT;
        }
        if ('&' == ch)
            return getAndType();
        if ('|' == ch)
            return getOrType();
        if ('!' == ch)
            return getExclamationType();
        if ('+' == ch)
            return getPlusType();            
        if ('-' == ch)
            return getMinusType();
        if ('=' == ch)
            return getEqualType();
        if ('%' == ch)
            return getModType();
        if ('/' == ch)
            return getForwardSlashType();
        if ('.' == ch)
            return getDotToken();
        if ('*' == ch)
            return getStarType();
        if ('>' == ch)
            return getGTType();
        if ('<' == ch)
            return getLTType();

        readNextChar();
        return type;
    }

    private int getBXORType()
    {
        char validChars[]=new char[1];
        validChars[0]='='; 
        int rval=readNextChar();
        if (rval != '=') {
            return JavaTokenTypes.BXOR;
        }
        char thisChar=(char)rval; 
        textBuffer.append(thisChar); 
        readNextChar();
        return JavaTokenTypes.BXOR_ASSIGN;
    }

    private int getAndType()
    {
        char validChars[]=new char[2];
        validChars[0]='=';
        validChars[1]='&';        
        int rval=readNextChar();
        char thisChar = (char)rval; 
        if (thisChar=='='){
            textBuffer.append(thisChar); 
            readNextChar();
            return JavaTokenTypes.BAND_ASSIGN; 
        }
        if (thisChar=='&'){
            textBuffer.append(thisChar); 
            readNextChar();
            return JavaTokenTypes.LAND; 
        }
        return JavaTokenTypes.BAND;
    }

    private int getStringLiteral()
    {
        boolean success=getTokenText('"');
        if (success) {
            return JavaTokenTypes.STRING_LITERAL;
        }
        return JavaTokenTypes.INVALID;     
    }

    private int getCharLiteral()
    {
        boolean success=getTokenText('\'');
        if (success) {
            return JavaTokenTypes.CHAR_LITERAL;
        }
        return JavaTokenTypes.INVALID;      
    }

    private int getOrType()
    {
        //|, |=, ||
        int rval=readNextChar();
        char thisChar=(char)rval; 
        if (thisChar=='=') {
            textBuffer.append(thisChar); 
            readNextChar();
            return JavaTokenTypes.BOR_ASSIGN; 
        }
        if (thisChar=='|') {
            textBuffer.append(thisChar); 
            readNextChar();
            return JavaTokenTypes.LOR; 
        }

        return JavaTokenTypes.BOR;
    }

    private int getPlusType()
    {
        //+, +=, ++
        int rval=readNextChar();
        char thisChar=(char)rval; 
        if (thisChar=='='){
            textBuffer.append(thisChar); 
            readNextChar();
            return JavaTokenTypes.PLUS_ASSIGN; 
        }
        if (thisChar=='+'){
            textBuffer.append(thisChar); 
            readNextChar();
            return JavaTokenTypes.INC; 
        }

        return JavaTokenTypes.PLUS;
    }

    private int getMinusType()
    {
        //-, -=, --
        int rval=readNextChar();
        char thisChar=(char)rval; 

        if (thisChar=='='){
            textBuffer.append(thisChar);
            readNextChar();
            return JavaTokenTypes.MINUS_ASSIGN; 
        }
        if (thisChar=='-'){
            textBuffer.append(thisChar); 
            readNextChar();
            return JavaTokenTypes.DEC; 
        }
        if (thisChar == '>'){
            textBuffer.append(thisChar);
            readNextChar();
            return JavaTokenTypes.LAMBDA;
        }

        return JavaTokenTypes.MINUS;
    }

    private int getEqualType()
    {
        //=, ==
        int rval = readNextChar();
        char thisChar=(char)rval; 
        if (thisChar=='='){
            textBuffer.append(thisChar); 
            readNextChar();
            return JavaTokenTypes.EQUAL; 
        }

        return JavaTokenTypes.ASSIGN;
    }

    private int getStarType()
    {
        //*, *=, 
        int rval = readNextChar();
        char thisChar=(char)rval; 
        if (thisChar == '=') {
            textBuffer.append(thisChar); 
            readNextChar();
            return JavaTokenTypes.STAR_ASSIGN; 
        }

        return JavaTokenTypes.STAR;
    }

    private int getModType()
    {
        //%, %=
        int rval=readNextChar();
        char thisChar=(char)rval; 
        if (thisChar=='='){
            textBuffer.append(thisChar); 
            readNextChar();
            return JavaTokenTypes.MOD_ASSIGN; 
        }

        return JavaTokenTypes.MOD;
    }

    private int getForwardSlashType()
    {
        // /,  /*, /= 
        int rval=readNextChar();
        char thisChar=(char)rval; 
        if (thisChar=='=') {
            textBuffer.append(thisChar); 
            readNextChar();
            return JavaTokenTypes.DIV_ASSIGN; 
        }
        if (thisChar=='/' && handleComments) {
            return getSLCommentType(thisChar);
        }
        if (thisChar=='*' && handleComments) {
            return getMLCommentType(thisChar);
        }

        return JavaTokenTypes.DIV;
    }

    private int getGTType()
    {
        int rval=readNextChar();
        char thisChar=(char)rval;
        //>=
        if (thisChar=='='){
            textBuffer.append(thisChar); 
            readNextChar();
            return JavaTokenTypes.GE; 
        }
        if (thisChar=='>'){
            //>>
            //>>>; >>>=; >>=
            textBuffer.append(thisChar); 
            rval=readNextChar();
            thisChar = (char)rval;
            if (thisChar=='>') {
                textBuffer.append(thisChar); 
                rval=readNextChar();
                thisChar = (char)rval;
                if (thisChar=='='){
                    textBuffer.append(thisChar); 
                    readNextChar();
                    return JavaTokenTypes.BSR_ASSIGN; 
                }
                return JavaTokenTypes.BSR;
            }
            if (thisChar=='='){
                textBuffer.append(thisChar); 
                readNextChar();
                return JavaTokenTypes.SR_ASSIGN; 
            }
            return JavaTokenTypes.SR;
        }

        return JavaTokenTypes.GT;
    }

    private int getLTType()
    {
        int rval=readNextChar();
        char thisChar = (char)rval; 
        if (thisChar=='='){
            textBuffer.append(thisChar); 
            readNextChar();
            return JavaTokenTypes.LE; 
        }
        if (thisChar=='<'){
            textBuffer.append(thisChar); 
            rval=readNextChar();
            thisChar = (char)rval;
            if (thisChar=='='){
                textBuffer.append(thisChar); 
                readNextChar();
                return JavaTokenTypes.SL_ASSIGN;
            }
            return JavaTokenTypes.SL;
        }

        return JavaTokenTypes.LT;
    }

    private int getExclamationType()
    {
        int rval=readNextChar();
        char thisChar = (char)rval; 
        if (thisChar=='='){
            textBuffer.append(thisChar); 
            readNextChar();
            return JavaTokenTypes.NOT_EQUAL; 
        }

        return JavaTokenTypes.LNOT;
    }

    private int getDotToken()
    {
        //. or .56f .12 
        int rval = readNextChar();
        char ch = (char)rval;
        if (Character.isDigit(ch)){
            return readDigitToken(ch, true);
        }
        //...
        else if (ch=='.'){
            textBuffer.append(ch); 
            rval= readNextChar();
            if (rval==-1){
                return JavaTokenTypes.INVALID;
            }
            ch = (char)rval;
            if (ch=='.'){
                textBuffer.append(ch); 
                readNextChar();
                return JavaTokenTypes.TRIPLE_DOT;
            }
            else return JavaTokenTypes.INVALID;

        }
        return JavaTokenTypes.DOT;
    }

    private int readNextChar()
    {
        endColumn = reader.getColumn();
        endLine = reader.getLine();
        endPosition = reader.getPosition();
        try{
            rChar = reader.read();
        } catch(IOException e) {
            rChar = -1;
        }

        return rChar;
    }

    private int getWordType()
    {
        String text=textBuffer.toString();
        Integer i = keywords.get(text);
        if (i == null) {
            return JavaTokenTypes.IDENT;
        }
        return i;
    }

    public void setGenerateWhitespaceTokens(boolean generateWhitespaceTokens)
    {
        this.generateWhitespaceTokens = generateWhitespaceTokens;
    }
}
