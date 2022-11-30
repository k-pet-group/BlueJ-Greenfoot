/*
 This file is part of the BlueJ program. 
 Copyright (C) 2009,2010,2011,2012,2014,2016,2022  Michael Kolling and John Rosenberg 

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
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.stream.IntStream;

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
    // Only used in one frequently-called method, but stored as field to avoid recreating object each call:
    private final TreeMap<Integer, LineColPos> minusPositions = new TreeMap<>();
    private LineColPos begin;
    private LineColPos end;
    private boolean generateWhitespaceTokens = false;
    private boolean handleComments = true; // When false, doesn't recognise /*..*/ or //..\n as comments (for frames)
    private boolean handleMultilineStrings = true; // When false, treats """ as a single token rather than trying to match start/end
    
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
        keywords.put("non-sealed", JavaTokenTypes.LITERAL_non_sealed);
        keywords.put("null", JavaTokenTypes.LITERAL_null);
        keywords.put("package", JavaTokenTypes.LITERAL_package);
        keywords.put("permits", JavaTokenTypes.LITERAL_permits);
        keywords.put("private", JavaTokenTypes.LITERAL_private);
        keywords.put("protected", JavaTokenTypes.LITERAL_protected);
        keywords.put("public", JavaTokenTypes.LITERAL_public);
        keywords.put("record", JavaTokenTypes.LITERAL_record);
        keywords.put("return", JavaTokenTypes.LITERAL_return);
        keywords.put("sealed", JavaTokenTypes.LITERAL_sealed);
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
        keywords.put("yield", JavaTokenTypes.LITERAL_yield);
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
    public JavaLexer(Reader in, boolean handleComments, boolean handleMultilineStrings)
    {
        this(in, 1, 1, 0);
        this.handleComments = handleComments;
        this.handleMultilineStrings = handleMultilineStrings;
    }

    /**
     * Construct a lexer which readers from the given Reader, assuming that the
     * reader is already positioned at the given line and column within the source
     * document.
     */
    public JavaLexer(Reader in, int line, int col, int position)
    {
        reader = new EscapedUnicodeReader(in);
        LineColPos lineColPos = new LineColPos(line, col, position);
        reader.setLineColPos(lineColPos);
        end = begin = lineColPos;
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
                begin = reader.getLineColPos();
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
        LocatableToken tok = new LocatableToken(type, txt, begin, end);
        begin = end;
        return tok;
    }

    private LocatableToken createWordToken(char nextChar)
    {
        populateTextBuffer(nextChar);
        return makeToken(getWordType(), textBuffer.toString());
    }

    /**
     * Populates the textBuffer field with the next complete ident or keyword.
     */
    private void populateTextBuffer(char ch)
    {
        // Now that we have hyphenated keywords, we need to keep going when we see minus signs
        // in case they are part of a keyword.  If we get to the end of a sequence of identifier
        // parts and minus signs and it doesn't match a known hyphenated keyword, or it
        // begins with a known hyphenated keyword but has more minus signs and identifier bits after, 
        // we have to put everything from the last spare minus onwards back onto the reader.
        
        char thisChar=ch;
        boolean eof = false;
        minusPositions.clear();
        do {  
            textBuffer.append(thisChar);
            int rval = readNextChar();
            if (rval==-1){
                //eof
                eof = true;
                break;
            }
            if (rval == '-')
            {
                // Record when we see a minus for faster processing below:
                minusPositions.put(textBuffer.length(), end);
            }
            thisChar=(char)rval;
        } while (Character.isJavaIdentifierPart(thisChar) || thisChar == '-');

        // We look for the first minus where the text before that minus matches a known keyword.
        // So e.g. if we consumed "non-sealed-pipe" we'd pick out the second minus as the first
        // one that is after a known keyword ("non-sealed").
        // For "non-closed-file" it would be the first minus because there is no known keyword.
        OptionalInt keywordEnd = 
            // We look at all positions where there is a minus, but we also look at the end of 
            // the String (in case it's exactly a hyphenated keyword like "non-sealed" with no further minuses)
            IntStream.concat(minusPositions.keySet().stream().mapToInt(Integer::intValue).sorted(), IntStream.of(textBuffer.length()))
                .filter(index -> keywords.containsKey(textBuffer.substring(0, index))).findFirst();
        
        if (!minusPositions.isEmpty() && keywordEnd.orElse(-1) < textBuffer.length())
        {
            // We have found a minus but there either is not a keyword (keywordEnd will be empty)
            // or there are further minuses after the content (keywordEnd will be present,
            // but less than the full length of the string).
            int minusToPushBackFrom = keywordEnd.orElse(minusPositions.firstKey().intValue());
            end = minusPositions.get(minusToPushBackFrom);
            try
            {
                // If we found EOF then thisChar is already handled and we shouldn't push it back
                // on to the buffer:
                if (!eof)
                    textBuffer.append(thisChar);
                reader.pushBack(textBuffer.substring(minusToPushBackFrom), end);
                // Prime the rChar variable which always holds the next pending character:
                readNextChar();
            }
            catch (IOException e)
            {
                // If this happens, we have a hyphenated identifier longer than 65536 characters (the EscapedUnicodeReader buffer size).  Ignore?
            }
            textBuffer.delete(minusToPushBackFrom, textBuffer.length());
        }
    }

    /**
     * Reads in a character or string literal as a single token.
     * @param newlineAllowed Whether a newline is allowed inside the literal (it is allowed in Java's new text blocks feature)
     * @param terminator The terminator of the literal, can be ', ", or """
     * @param initialContent Any initial content that was consumed in the process of detecting this token.
     *                       For example, in the course of working out whether "a" is a simple string
     *                       literal or a multiline block, we will have consumed the 'a' character,
     *                       so we pass it in here as initialContent in order to lex it and not omit it.
     *                       It is an int list because it matches the type of readNextChar()
     * @return True if the lexing was successful, false if it was not (due to unexpected newline or EOF)
     */
    private boolean getTokenText(boolean newlineAllowed, String terminator, int... initialContent)
    {
        int initialContentIndex = 0;
        int terminatorIndex = 0;
        boolean escape = false;
        while (true)
        {  
            final int rval;
            if (initialContentIndex < initialContent.length)
            {
                rval = initialContent[initialContentIndex];
                initialContentIndex += 1;
            }
            else
            {
                rval = readNextChar();
            }
            //eof
            if (rval==-1)
            {
                return false;
            }
            char thisChar = (char)rval; 
            if (thisChar == '\n' && !newlineAllowed)
            {
                return false;
            }

            textBuffer.append(thisChar);
            if (! escape)
            {
                if (thisChar == '\\')
                {
                    escape = true;
                }
                //endChar is the flag for the end of reading
                if (thisChar == terminator.charAt(terminatorIndex))
                {
                    terminatorIndex += 1;
                    if (terminatorIndex >= terminator.length())
                    {
                        readNextChar();
                        return true;
                    }
                }
                else
                {
                    terminatorIndex = 0;
                }
            }
            else
            {
                escape = false;
            }
        }
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
        {
            readNextChar();
            if (rChar == '"')
            {
                // Either empty String literal ("" followed by non-")
                // or beginning of a text block (triple """)
                readNextChar();
                if (rChar == '"')
                {
                    // Text block:
                    textBuffer.append('"');
                    textBuffer.append('"');
                    if (handleMultilineStrings)
                        return getStringLiteral(true);
                    else
                    {
                        readNextChar();
                        return JavaTokenTypes.STRING_LITERAL_MULTILINE;
                    }
                }
                else
                {
                    // Empty string literal:
                    textBuffer.append('"');
                    return JavaTokenTypes.STRING_LITERAL;
                }
            }
            else
            {
                // Not a ", so first character of a String literal:
                return getStringLiteral(false, rChar);
            }
        }
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

    private int getStringLiteral(boolean multilineBlock, int... initialContent)
    {
        boolean success=getTokenText(multilineBlock, multilineBlock ? "\"\"\"" : "\"", initialContent);
        if (success) {
            return multilineBlock ? JavaTokenTypes.STRING_LITERAL_MULTILINE : JavaTokenTypes.STRING_LITERAL;
        }
        return JavaTokenTypes.INVALID;     
    }

    private int getCharLiteral()
    {
        boolean success=getTokenText(false, "\'");
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
        //-, -=, --, ->
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
        end = reader.getLineColPos();
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
