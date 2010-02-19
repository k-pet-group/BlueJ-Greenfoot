/*
 This file is part of the BlueJ program. 
 Copyright (C) 2009-2010  Michael Kolling and John Rosenberg 

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
public class JavaLexer implements TokenStream
{
    private StringBuffer textBuffer; // text of current token
    private EscapedUnicodeReader reader;
    private int rChar; 
    private int beginColumn, beginLine;
    private int endColumn, endLine;
    private char [] buf = new char[1];
    
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
        this(in, 1, 1);
    }

    /**
     * Construct a lexer which readers from the given Reader, assuming that the
     * reader is already positioned at the given line and column within the source
     * document.
     */
    public JavaLexer(Reader in, int line, int col)
    {
        reader = new EscapedUnicodeReader(in);
        reader.setLineCol(line, col);
        endColumn = beginColumn = col;
        endLine = beginLine = line;
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
        resetText();
        while (Character.isWhitespace((char)rChar)) {
            beginLine = reader.getLine();
            beginColumn = reader.getColumn();
            readNextChar();
        }

        return createToken();
    }
    
    private LocatableToken makeToken(int type, String txt)
    {           
        LocatableToken tok = new LocatableToken(type, txt);
        tok.setColumn(getBeginColumn());
        tok.setLine(getBeginLine());        
        tok.setEndLineAndCol(endLine, endColumn);
        beginColumn = endColumn;
        beginLine = endLine;
        return tok;
    }

    private int getBeginColumn()
    {
        return beginColumn;
    }

    private int getBeginLine()
    {
        return beginLine;
    }

    private void resetText()
    {
        textBuffer=new StringBuffer();
    }

    private void consume(char c)
    {
        if (c=='\n'){
            //newline();
            return;
        }
        append(c);
    }

    /*
     * Does no checking on what it is appending
     */
    public void consume(char c, boolean overwrite) /*throws Exception*/ {
        append(c, overwrite);
    }

    private void append(char c)
    {
        if (!Character.isWhitespace(c)){
            textBuffer.append(c);
        }
    }

    private void append(char c, boolean overwrite){
        if (overwrite)
            textBuffer.append(c);
        else append(c);
    }

    private LocatableToken createToken(){
        if (rChar == -1) {
            // EOF
            return makeToken(JavaTokenTypes.EOF, null); 
        }
        
        char nextChar = (char) rChar;
        if (Character.isJavaIdentifierStart(nextChar))
            return createWordToken(nextChar); 
        if (Character.isDigit(nextChar))
            return createDigitToken(nextChar);
        return makeToken(getSymbolType(nextChar), textBuffer.toString());
    }


    private LocatableToken createDigitToken(char nextChar){
        return makeToken(getDigitType(nextChar, false), textBuffer.toString());
    }

    private LocatableToken createWordToken(char nextChar)
    {
        populateTextBuffer(nextChar);
        return makeToken(getWordType(), textBuffer.toString());
    }

    private void populateTextBuffer(char ch)
    {
        char thisChar=ch;
        int rval=0;
        boolean complete=false;
        while (!complete){  
            consume(thisChar);
            rval = readNextChar();
            if (rval==-1){
                //eof
                return;
            }
            thisChar=(char)rval;
            if (!Character.isLetterOrDigit(thisChar) && thisChar != '_') {
                complete=true;
            }
        }
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

    private int getDigitType(char ch, boolean dot)
    {
        int type=JavaTokenTypes.NUM_INT;
        int rval=0;     
        boolean complete=false;
        boolean isDecimal=dot;
        boolean hexDecimalNumber=false;
        append(ch);

        while (!complete){ 
            rval=readNextChar();
            //eof
            if (rval==-1){
                return type;
            }
            ch=(char)rval;
            if (!Character.isDigit(ch)){
                if (ch=='.'){
                    if (isDecimal){
                        return JavaTokenTypes.NUM_DOUBLE;
                    }
                    else {
                        isDecimal=true;
                        append(ch);
                    }
                }
                else if (Character.isLetter(ch)){
                    if (ch=='f'|| ch=='F'){
                        append(ch);
                        type= JavaTokenTypes.NUM_FLOAT;
                        isDecimal=false;
                    } else if (ch=='d'|| ch=='D'){
                        append(ch);
                        type= JavaTokenTypes.NUM_DOUBLE;
                    } else if (ch=='l'|| ch=='L'){
                        append(ch);
                        type= JavaTokenTypes.NUM_LONG;
                        isDecimal=false;
                    }
                    else if (ch=='e'|| ch=='E'){
                        append(ch);
                        type= JavaTokenTypes.NUM_DOUBLE;
                    }
                    else if (ch=='x'){
                        hexDecimalNumber=true;
                        append(ch);
                    }
                    else if (hexDecimalNumber && (ch=='a'|| ch=='A' || ch=='b' ||ch=='B'||ch=='c'||ch=='C'||ch=='e'||ch=='E')){
                        append(ch);
                        type= JavaTokenTypes.NUM_INT;
                    }
                    else {
                        complete=true;
                    }                   
                }
                else if (Character.isWhitespace(ch)|| (!Character.isLetterOrDigit(ch))){
                    complete=true;
                } else {
                    complete=true;
                }              

            } else {
                append(ch);
                type=JavaTokenTypes.NUM_INT;
            }

        }           

        if (isDecimal)
            return JavaTokenTypes.NUM_DOUBLE;
        return type;
    }

    private int  getCommentType(char ch, int type)
    {
        int rval=0;     
        boolean complete=false;
        boolean checkflag=false;

        do{  
            consume(ch, true);
            rval=readNextChar();
            //eof
            if (rval==-1){
                if (type==JavaTokenTypes.ML_COMMENT)
                    return JavaTokenTypes.INVALID;
                else return type;
            }
            ch=(char)rval;
            if (ch=='\n'){
                if (type==JavaTokenTypes.SL_COMMENT)
                    return type;
            }

            if (checkflag){
                if (ch=='/'){
                    complete=true;
                    consume(ch);
                    readNextChar();
                }//it was a false alarm and we have not reached the end of the comment
                //reset flag and buffer
                else 
                    checkflag=false;
            }      
            //endChar is the flag for the end of reading
            if (ch=='*' && type==JavaTokenTypes.ML_COMMENT){
                checkflag=true;
            }

        }while (!complete);

        return type;
    }

    private int getSymbolType(char ch)
    {
        int type= JavaTokenTypes.INVALID;
        append(ch); 
        if (match('"', ch))
            return getStringLiteral();
        if (match('\'', ch))
            return getCharLiteral();
        if (match('?', ch)) {
            readNextChar();
            return JavaTokenTypes.QUESTION;
        }
        if (match(',', ch)) {
            readNextChar();
            return JavaTokenTypes.COMMA;
        }
        if (match(';', ch)) {
            readNextChar();
            return JavaTokenTypes.SEMI;
        }
        if (match(':', ch)) {
            readNextChar();
            return JavaTokenTypes.COLON;
        }
        if (match('^', ch))
            return getBXORType();
        if (match('~', ch)) {
            readNextChar();
            return JavaTokenTypes.BNOT;
        }
        if (match('(', ch)) {
            readNextChar();
            return JavaTokenTypes.LPAREN;
        }
        if (match(')', ch)) {
            readNextChar();
            return JavaTokenTypes.RPAREN;
        }
        if (match('[', ch)) {
            readNextChar();
            return JavaTokenTypes.LBRACK;
        }
        if (match(']', ch)) {
            readNextChar();
            return JavaTokenTypes.RBRACK;
        }
        if (match('{', ch)) {
            readNextChar();
            return JavaTokenTypes.LCURLY;
        }
        if (match('}', ch)) {
            readNextChar();
            return JavaTokenTypes.RCURLY;
        }
        if (match('@', ch)) {
            readNextChar();
            return JavaTokenTypes.AT;
        }
        if (match('&', ch))
            return getAndType();
        if (match('|', ch))
            return getOrType();
        if (match('!', ch))
            return getExclamationType();
        if (match('+', ch))
            return getPlusType();            
        if (match('-', ch))
            return getMinusType();
        if (match('=', ch))
            return getEqualType();
        if (match('%', ch))
            return getModType();
        if (match('/', ch))
            return getForwardSlashType();
        if (match('.', ch))
            return getDotType();
        if (match('*', ch))
            return getStarType();
        if (match('>', ch))
            return getGTType();
        if (match('<', ch))
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
        append(thisChar); 
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
            append(thisChar); 
            readNextChar();
            return JavaTokenTypes.BAND_ASSIGN; 
        }
        if (thisChar=='&'){
            append(thisChar); 
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
        if (thisChar=='='){
            consume(thisChar); 
            readNextChar();
            return JavaTokenTypes.BOR_ASSIGN; 
        }
        if (thisChar=='|'){
            consume(thisChar); 
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
            append(thisChar); 
            readNextChar();
            return JavaTokenTypes.PLUS_ASSIGN; 
        }
        if (thisChar=='+'){
            append(thisChar); 
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
            append(thisChar);
            readNextChar();
            return JavaTokenTypes.MINUS_ASSIGN; 
        }
        if (thisChar=='-'){
            append(thisChar); 
            readNextChar();
            return JavaTokenTypes.DEC; 
        }

        return JavaTokenTypes.MINUS;
    }

    private int getEqualType()
    {
        //=, ==
        int rval=readNextChar();
        char thisChar=(char)rval; 
        if (thisChar=='='){
            append(thisChar); 
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
            append(thisChar); 
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
            append(thisChar); 
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
            append(thisChar); 
            readNextChar();
            return JavaTokenTypes.DIV_ASSIGN; 
        }
        if (thisChar=='/')
            return getCommentType(thisChar, JavaTokenTypes.SL_COMMENT);
        if (thisChar=='*')
            return getCommentType(thisChar, JavaTokenTypes.ML_COMMENT);

        return JavaTokenTypes.DIV;
    }

    private int getGTType()
    {
        int rval=readNextChar();
        char thisChar=(char)rval;
        //>=
        if (thisChar=='='){
            append(thisChar); 
            readNextChar();
            return JavaTokenTypes.GE; 
        }
        if (thisChar=='>'){
            //>>
            //>>>; >>>=; >>=
            consume(thisChar); 
            rval=readNextChar();
            thisChar = (char)rval;
            if (thisChar=='>') {
                consume(thisChar); 
                rval=readNextChar();
                thisChar = (char)rval;
                if (thisChar=='='){
                    append(thisChar); 
                    readNextChar();
                    return JavaTokenTypes.BSR_ASSIGN; 
                }
                return JavaTokenTypes.BSR;
            }
            if (thisChar=='='){
                append(thisChar); 
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
            append(thisChar); 
            readNextChar();
            return JavaTokenTypes.LE; 
        }
        if (thisChar=='<'){
            consume(thisChar); 
            rval=readNextChar();
            thisChar = (char)rval;
            if (thisChar=='='){
                append(thisChar); 
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
            append(thisChar); 
            readNextChar();
            return JavaTokenTypes.NOT_EQUAL; 
        }

        return JavaTokenTypes.LNOT;
    }

    private int getDotType()
    {
        //. or .56f .12 
        int rval = readNextChar();
        char ch = (char)rval;
        if (Character.isDigit(ch)){
            return getDigitType(ch, true);
        }
        //...
        else if (ch=='.'){
            append(ch); 
            rval= readNextChar();
            if (rval==-1){
                return JavaTokenTypes.INVALID;
            }
            ch = (char)rval;
            if (ch=='.'){
                append(ch); 
                readNextChar();
                return JavaTokenTypes.TRIPLE_DOT;
            }
            else return JavaTokenTypes.INVALID;

        }
        return JavaTokenTypes.DOT;
    }

    private int readNextChar()
    {
        int rval=-1;
        endColumn = reader.getColumn();
        endLine = reader.getLine();
        try{
            rval = reader.read(buf, 0, 1);
            rChar = buf[0];
            if (rval == -1) {
                rChar = -1;
            }
            return rChar;
        } catch(IOException e) {
            rChar = -1;
        }

        return -1;
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

    private boolean match(char c1, char c2)
    {
        if (c1==c2){
            return true;
        }
        else return false;
    }

}
