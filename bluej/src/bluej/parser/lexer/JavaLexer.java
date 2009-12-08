/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 

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

import bluej.parser.EscapedUnicodeReader;
import bluej.parser.TokenStream;



public class JavaLexer implements TokenStream
{
    private StringBuffer textBuffer; // text of current token
    private EscapedUnicodeReader reader;
    private int rChar; 
    private int beginColumn, beginLine;
    private int endColumn, endLine;

    public JavaLexer(Reader in) {
        reader = new EscapedUnicodeReader(in);
        endColumn = beginColumn = reader.getColumn();
        endLine = beginLine = reader.getLine();
        try {
            rChar = reader.read();
        }
        catch (IOException ioe) {
            rChar = -1;
        }
    }

    private LocatableToken makeToken(int type, String txt){           
        LocatableToken tok = new LocatableToken(type, txt);
        tok.setColumn(getBeginColumn());
        tok.setLine(getBeginLine());        
        tok.setEndLineAndCol(endLine, endColumn);
        beginColumn = endColumn;
        beginLine = endLine;
        return tok;

    }

    private int getBeginColumn() {
        return beginColumn;
    }

    private int getBeginLine() {
        return beginLine;
    }

    public LocatableToken nextToken() {  
        resetText();
        while (Character.isWhitespace((char)rChar)) {
            beginLine = reader.getLine();
            beginColumn = reader.getColumn();
            char buf[] = new char[1];
            readNextChar(buf);
        }

        return createToken();
    }

    public void resetText() {
        textBuffer=new StringBuffer();
    }

    public void consume(char c) /*throws Exception*/ {
        if (c=='\n'){
            //newline();
            return;
        }
        append(c);
    }

    /*
     * Does no checking on what it is appending
     * 
     */
    public void consume(char c, boolean overwrite) /*throws Exception*/ {
        append(c, overwrite);
    }

    /*
     * Increases the column count and writes char always
     * 
     */
    public void consume(boolean incColumn, char c, boolean overwrite) /*throws Exception*/ {
        consume(false, incColumn, c);
    }

    /*
     * Increases the column count
     * 
     */
    public void consume(boolean isFirstChar, boolean incColumn, char c) /*throws Exception*/ {
        append(c);
    }
    
    public void consume(boolean incColumn, char c) /*throws Exception*/ {
        consume(false, incColumn, c);           
    }

    private void append(char c){
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

    private LocatableToken createWordToken(char nextChar){
        populateTextBuffer(nextChar);
        return makeToken(getWordType(), textBuffer.toString());
    }

    private void populateTextBuffer(char ch){
        char thisChar=ch;
        char [] cb=new char[1];
        int rval=0;
        boolean complete=false;
        while (!complete){  
            consume(thisChar);
            rval=readNextChar(cb);
            //eof
            if (rval==-1){
                return;
            }
            thisChar=cb[0];
            if (!Character.isLetterOrDigit(thisChar)&& thisChar!='_'){
                complete=true;
            }
        }
    }

    private boolean getTokenText(char endChar){
        char thisChar=endChar;
        char [] cb=new char[1];
        int rval=0;     
        boolean complete = false;
        boolean escape = false;
        while (!complete){  
            rval=readNextChar(cb);
            //eof
            if (rval==-1){
                return false;
            }
            thisChar=cb[0]; 
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
                    readNextChar(cb);
                    return true;
                }
            }
            else {
                escape = false;
            }
        }
        return complete;
    }

    private int getDigitType(char ch, boolean dot){
        int type=JavaTokenTypes.NUM_INT;
        char [] cb=new char[1];
        int rval=0;     
        boolean complete=false;
        boolean isDecimal=dot;
        boolean hexDecimalNumber=false;
        consume(true, true, ch);

        while (!complete){ 
            rval=readNextChar(cb);
            //eof
            if (rval==-1){
                return type;
            }
            ch=cb[0];
            if (!Character.isDigit(ch)){
                if (ch=='.'){
                    if (isDecimal){
                        return JavaTokenTypes.NUM_DOUBLE;
                    }
                    else {
                        isDecimal=true;
                        consume(true, ch);
                    }
                }
                else if (Character.isLetter(ch)){
                    if (ch=='f'|| ch=='F'){
                        consume(true, ch);
                        type= JavaTokenTypes.NUM_FLOAT;
                        isDecimal=false;
                    } else if (ch=='d'|| ch=='D'){
                        consume(true, ch);
                        type= JavaTokenTypes.NUM_DOUBLE;
                    } else if (ch=='l'|| ch=='L'){
                        consume(true, ch);
                        type= JavaTokenTypes.NUM_LONG;
                        isDecimal=false;
                    }
                    else if (ch=='e'|| ch=='E'){
                        consume(true, ch);
                        type= JavaTokenTypes.NUM_DOUBLE;
                    }
                    else if (ch=='x'){
                        hexDecimalNumber=true;
                        consume(true, ch);
                    }
                    else if (hexDecimalNumber && (ch=='a'|| ch=='A' || ch=='b' ||ch=='B'||ch=='c'||ch=='C'||ch=='e'||ch=='E')){
                        consume(true, ch);
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
                consume(true, ch);
                type=JavaTokenTypes.NUM_INT;
            }

        }           

        if (isDecimal)
            return JavaTokenTypes.NUM_DOUBLE;
        return type;
    }

    private int  getCommentType(char ch, int type){
        char [] cb=new char[1];
        int rval=0;     
        boolean complete=false;
        boolean checkflag=false;

        do{  
            consume(ch, true);
            rval=readNextChar(cb);
            //eof
            if (rval==-1){
                if (type==JavaTokenTypes.ML_COMMENT)
                    return JavaTokenTypes.INVALID;
                else return type;
            }
            ch=cb[0];
            if (ch=='\n'){
                if (type==JavaTokenTypes.SL_COMMENT)
                    return type;
            }

            if (checkflag){
                if (ch=='/'){
                    complete=true;
                    consume(ch);
                    readNextChar(cb);
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

    private int getSymbolType(char ch){
        int type= JavaTokenTypes.INVALID;
        char buf[] = new char[1];
        consume(true, true, ch); 
        if (match('"', ch))
            return getStringLiteral();
        if (match('\'', ch))
            return getCharLiteral();
        if (match('?', ch)) {
            readNextChar(buf);
            return JavaTokenTypes.QUESTION;
        }
        if (match(',', ch)) {
            readNextChar(buf);
            return JavaTokenTypes.COMMA;
        }
        if (match(';', ch)) {
            readNextChar(buf);
            return JavaTokenTypes.SEMI;
        }
        if (match(':', ch)) {
            readNextChar(buf);
            return JavaTokenTypes.COLON;
        }
        if (match('^', ch))
            return getBXORType();
        if (match('~', ch)) {
            readNextChar(buf);
            return JavaTokenTypes.BNOT;
        }
        if (match('(', ch)) {
            readNextChar(buf);
            return JavaTokenTypes.LPAREN;
        }
        if (match(')', ch)) {
            readNextChar(buf);
            return JavaTokenTypes.RPAREN;
        }
        if (match('[', ch)) {
            readNextChar(buf);
            return JavaTokenTypes.LBRACK;
        }
        if (match(']', ch)) {
            readNextChar(buf);
            return JavaTokenTypes.RBRACK;
        }
        if (match('{', ch)) {
            readNextChar(buf);
            return JavaTokenTypes.LCURLY;
        }
        if (match('}', ch)) {
            readNextChar(buf);
            return JavaTokenTypes.RCURLY;
        }
        if (match('@', ch)) {
            readNextChar(buf);
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

        readNextChar(buf);
        return type;
    }

    private int getBXORType(){
        char validChars[]=new char[1];
        validChars[0]='='; 
        char [] cb=new char[1];
        int rval=readNextChar(cb);
        if (isComplete(rval, cb[0], validChars)){
            return JavaTokenTypes.BXOR;
        }
        char thisChar=(char)cb[0]; 
        if (thisChar=='='){
            consume(true, thisChar); 
            readNextChar(cb);
            return JavaTokenTypes.BXOR_ASSIGN;
        }           

        return JavaTokenTypes.BXOR;
    }

    private int getAndType(){
        char validChars[]=new char[2];
        validChars[0]='=';
        validChars[1]='&';        
        char [] cb=new char[1];
        int rval=readNextChar(cb);
        if (isComplete(rval, cb[0], validChars)){
            return JavaTokenTypes.BAND;
        }
        char thisChar=(char)cb[0]; 
        if (thisChar=='='){
            consume(true, thisChar); 
            readNextChar(cb);
            return JavaTokenTypes.BAND_ASSIGN; 
        }
        if (thisChar=='&'){
            consume(true, thisChar); 
            readNextChar(cb);
            return JavaTokenTypes.LAND; 
        }
        return JavaTokenTypes.BAND;
    }

    private int getStringLiteral(){
        boolean success=getLiteralText('"');
        if (success)
            return JavaTokenTypes.STRING_LITERAL;
        return JavaTokenTypes.INVALID;     
    }

    private boolean getLiteralText(char endChar){
        return getTokenText(endChar);           
    }

    private int getCharLiteral(){
        boolean success=getLiteralText('\'');
        if (success)
            return JavaTokenTypes.CHAR_LITERAL;
        return JavaTokenTypes.INVALID;      
    }



    private int getOrType(){
        //|, |=, ||
        char validChars[]=new char[2];
        validChars[0]='|';
        validChars[1]='=';
        char [] cb=new char[1];
        int rval=readNextChar(cb);
        if (isComplete(rval, cb[0], validChars)){
            return JavaTokenTypes.BOR;
        }
        char thisChar=(char)cb[0]; 
        if (thisChar=='='){
            consume(thisChar); 
            readNextChar(cb);
            return JavaTokenTypes.BOR_ASSIGN; 
        }
        if (thisChar=='|'){
            consume(thisChar); 
            readNextChar(cb);
            return JavaTokenTypes.LOR; 
        }

        return JavaTokenTypes.BOR;
    }

    private int getPlusType(){
        //+, +=, ++
        char validChars[]=new char[2];
        validChars[0]='+';
        validChars[1]='=';
        char [] cb=new char[1];
        int rval=readNextChar(cb);
        if (isComplete(rval, cb[0], validChars)){
            return JavaTokenTypes.PLUS;
        }
        char thisChar=(char)cb[0]; 
        if (thisChar=='='){
            consume(true, thisChar); 
            readNextChar(cb);
            return JavaTokenTypes.PLUS_ASSIGN; 
        }
        if (thisChar=='+'){
            consume(true, thisChar); 
            readNextChar(cb);
            return JavaTokenTypes.INC; 
        }

        return JavaTokenTypes.PLUS;
    }

    private int getMinusType(){
        //-, -=, --
        char validChars[]=new char[2];
        validChars[0]='=';
        validChars[1]='-'; 
        char [] cb=new char[1];
        int rval=readNextChar(cb);
        if (isComplete(rval, cb[0], validChars)){
            return JavaTokenTypes.MINUS;
        }
        char thisChar=(char)cb[0]; 
        if (thisChar=='='){
            consume(true, thisChar);
            readNextChar(cb);
            return JavaTokenTypes.MINUS_ASSIGN; 
        }
        if (thisChar=='-'){
            consume(true, thisChar); 
            readNextChar(cb);
            return JavaTokenTypes.DEC; 
        }

        return JavaTokenTypes.MINUS;
    }

    private int getEqualType(){
        //=, ==
        char validChars[]=new char[1];
        validChars[0]='=';
        char [] cb=new char[1];
        int rval=readNextChar(cb);
        if (isComplete(rval, cb[0], validChars)){
            return JavaTokenTypes.ASSIGN;
        }
        char thisChar=(char)cb[0]; 
        if (thisChar=='='){
            consume(true, thisChar); 
            readNextChar(cb);
            return JavaTokenTypes.EQUAL; 
        }

        return JavaTokenTypes.ASSIGN;
    }

    private int getStarType(){
        //*, *=, 
        char validChars[]=new char[1];
        validChars[0]='=';
        char [] cb=new char[1];
        int rval=readNextChar(cb);
        if (isComplete(rval, cb[0])){
            return JavaTokenTypes.STAR;
        }
        char thisChar=(char)cb[0]; 
        if (thisChar=='='){
            consume(true, thisChar); 
            readNextChar(cb);
            return JavaTokenTypes.STAR_ASSIGN; 
        }

        return JavaTokenTypes.STAR;
    }

    private int getModType(){
        //%, %=
        char validChars[]=new char[1];
        validChars[0]='=';
        char [] cb=new char[1];
        int rval=readNextChar(cb);
        if (isComplete(rval, cb[0], validChars)){
            return JavaTokenTypes.MOD;
        }
        char thisChar=(char)cb[0]; 
        if (thisChar=='='){
            consume(true, thisChar); 
            readNextChar(cb);
            return JavaTokenTypes.MOD_ASSIGN; 
        }

        return JavaTokenTypes.MOD;
    }

    private int getForwardSlashType(){
        // /,  /*, /= 
        char validChars[]=new char[3];
        validChars[0]='/';
        validChars[1]='*'; 
        validChars[2]='='; 
        char [] cb=new char[1];
        int rval=readNextChar(cb);
        if (isComplete(rval, cb[0], validChars)){
            return JavaTokenTypes.DIV;
        }
        char thisChar=(char)cb[0]; 
        if (thisChar=='='){
            consume(true, thisChar); 
            readNextChar(cb);
            return JavaTokenTypes.DIV_ASSIGN; 
        }
        if (thisChar=='/')
            return getCommentType(thisChar, JavaTokenTypes.SL_COMMENT);
        if (thisChar=='*')
            return getCommentType(thisChar, JavaTokenTypes.ML_COMMENT);

        return JavaTokenTypes.DIV;
    }

    private boolean isComplete(int rval, char ch) {
        if (rval==-1 || Character.isWhitespace(ch)
                || Character.isLetterOrDigit(ch) || Character.isJavaIdentifierStart(ch) ){
            return true;
        }
        return false;
    }

    private boolean isComplete(int rval, char ch, char validChars[]){
        if (!isComplete(rval, ch)) {
            for (int i=0; i<validChars.length; i++){
                if (validChars[i]==ch)
                    return false;
            }
        }
        return true;
    }

    private int getGTType(){
        char validChars[]=new char[2];
        validChars[0]='>';
        validChars[1]='='; 
        char [] cb=new char[1];
        int rval=readNextChar(cb);
        if (isComplete(rval, cb[0], validChars)){
            return JavaTokenTypes.GT;
        }
        char thisChar=(char)cb[0];
        //>=
        if (thisChar=='='){
            consume(true, thisChar); 
            readNextChar(cb);
            return JavaTokenTypes.GE; 
        }
        if (thisChar=='>'){
            consume(thisChar); 
            rval=readNextChar(cb);
            thisChar=cb[0];
            //>>
            if (isComplete(rval, thisChar, validChars)){
                return JavaTokenTypes.SR;
            }
            //>>>; >>>=; >>=
            if (thisChar=='>'){
                consume(thisChar); 
                rval=readNextChar(cb);
                thisChar=cb[0];
                validChars[0]='=';
                if (isComplete(rval, thisChar, validChars)){
                    return JavaTokenTypes.BSR;
                }
                if (thisChar=='='){
                    consume(true, thisChar); 
                    readNextChar(cb);
                    return JavaTokenTypes.BSR_ASSIGN; 
                }
            }
            if (thisChar=='='){
                consume(true, thisChar); 
                readNextChar(cb);
                return JavaTokenTypes.SR_ASSIGN; 
            }
        }

        return JavaTokenTypes.GT;
    }

    private int getLTType(){
        char validChars[]=new char[2];
        validChars[0]='<';
        validChars[1]='='; 
        char [] cb=new char[1];
        int rval=readNextChar(cb);
        if (isComplete(rval, cb[0])){
            return JavaTokenTypes.LT;
        }
        char thisChar=(char)cb[0]; 
        if (thisChar=='='){
            consume(true, thisChar); 
            readNextChar(cb);
            return JavaTokenTypes.LE; 
        }
        if (thisChar=='<'){
            consume(thisChar); 
            rval=readNextChar(cb);
            thisChar=cb[0];
            validChars[0]='=';
            if (isComplete(rval, thisChar, validChars)){
                return JavaTokenTypes.SL;
            }
            if (thisChar=='='){
                consume(true, thisChar); 
                readNextChar(cb);
                return JavaTokenTypes.SL_ASSIGN;
            }
        }

        return JavaTokenTypes.LT;

    }

    private int getExclamationType(){
        char [] cb=new char[1];
        int rval=readNextChar(cb);
        if (rval==-1 || Character.isWhitespace((char)cb[0])){
            return JavaTokenTypes.LNOT;
        }
        char thisChar=(char)cb[0]; 
        if (thisChar=='='){
            consume(true, thisChar); 
            readNextChar(cb);
            return JavaTokenTypes.NOT_EQUAL; 
        }

        return JavaTokenTypes.LNOT;
    }

    private int getDotType(){
        char validChars[]=new char[1];
        validChars[0]='.';
        //. or .56f .12 
        char [] cb=new char[1];
        int rval=readNextChar(cb);
        char ch=cb[0];
        if (rval==-1 || Character.isWhitespace((char)ch)|| Character.isLetter(ch) || ch=='\n'){
            return JavaTokenTypes.DOT;
        }
        char thisChar=(char)cb[0]; 
        if (Character.isDigit(thisChar)){
            return getDigitType(thisChar, true);
        }
        //...
        else if (ch=='.'){
            consume(true, thisChar); 
            rval= readNextChar(cb);
            if (rval==-1){
                return JavaTokenTypes.INVALID;
            }
            ch=cb[0];
            if (ch=='.'){
                consume(true, thisChar); 
                readNextChar(cb);
                return JavaTokenTypes.TRIPLE_DOT;
            }
            else return JavaTokenTypes.INVALID;

        }
        return JavaTokenTypes.DOT;
    }

    private int readNextChar(char cb[]){
        int rval=-1;
        endColumn = reader.getColumn();
        endLine = reader.getLine();
        try{
            rval= reader.read(cb, 0, 1);
            rChar = cb[0];
            if (rval == -1) {
                rChar = -1;
            }
        }catch(IOException e){

        }

        return rval;

    }

    private int getWordType(){
        String text=textBuffer.toString();
        if (text.equals("assert")) {
            return JavaTokenTypes.LITERAL_assert;
        }
        if (text.equals("public")){
            return JavaTokenTypes.LITERAL_public;
        }
        if (text.equals("private")){
            return JavaTokenTypes.LITERAL_private;
        }
        if (text.equals("protected")){
            return JavaTokenTypes.LITERAL_protected;
        }
        if (text.equals("volatile")){
            return JavaTokenTypes.LITERAL_volatile;
        }
        if (text.equals("synchronized")){
            return JavaTokenTypes.LITERAL_synchronized;
        }
        if (text.equals("abstract")){
            return JavaTokenTypes.ABSTRACT;
        }
        if (text.equals("transient")){
            return JavaTokenTypes.LITERAL_transient;
        }
        if (text.equals("class")){
            return JavaTokenTypes.LITERAL_class;
        }
        if (text.equals("enum")){
            return JavaTokenTypes.LITERAL_enum;
        }
        if (text.equals("interface")){
            return JavaTokenTypes.LITERAL_interface;
        }
        if (text.equals("switch")){
            return JavaTokenTypes.LITERAL_switch;
        }
        if (text.equals("case")){
            return JavaTokenTypes.LITERAL_case;
        }
        if (text.equals("break")){
            return JavaTokenTypes.LITERAL_break;
        }
        if (text.equals("continue")){
            return JavaTokenTypes.LITERAL_continue;
        }
        if (text.equals("while")){
            return JavaTokenTypes.LITERAL_while;
        }
        if (text.equals("do")){
            return JavaTokenTypes.LITERAL_do;
        }
        if (text.equals("for")){
            return JavaTokenTypes.LITERAL_for;
        }
        if (text.equals("if")){
            return JavaTokenTypes.LITERAL_if;
        }
        if (text.equals("else")){
            return JavaTokenTypes.LITERAL_else;
        }
        if (text.equals("void")){
            return JavaTokenTypes.LITERAL_void;
        }
        if (text.equals("byte")){
            return JavaTokenTypes.LITERAL_byte;
        }
        if (text.equals("char")){
            return JavaTokenTypes.LITERAL_char;
        }
        if (text.equals("short")){
            return JavaTokenTypes.LITERAL_short;
        }
        if (text.equals("int")){
            return JavaTokenTypes.LITERAL_int;
        }
        if (text.equals("float")){
            return JavaTokenTypes.LITERAL_float;
        }
        if (text.equals("long")){
            return JavaTokenTypes.LITERAL_long;
        }
        if (text.equals("double")){
            return JavaTokenTypes.LITERAL_double;
        }
        if (text.equals("native")){
            return  JavaTokenTypes.LITERAL_native;
        }
        if (text.equals("synchronized")){
            return JavaTokenTypes.LITERAL_synchronized;
        }
        if (text.equals("volatile")){
            return JavaTokenTypes.LITERAL_volatile;
        }
        if (text.equals("default")){
            return JavaTokenTypes.LITERAL_default;
        }
        if (text.equals("implements")){
            return JavaTokenTypes.LITERAL_implements;
        }
        if (text.equals("extends")){
            return JavaTokenTypes.LITERAL_extends;
        }
        if (text.equals("this")){
            return JavaTokenTypes.LITERAL_this;
        }
        if (text.equals("throws")){
            return JavaTokenTypes.LITERAL_throws;
        }
        if (text.equals("throw")){
            return JavaTokenTypes.LITERAL_throw;
        }
        if (text.equals("try")){
            return JavaTokenTypes.LITERAL_try;
        }
        if (text.equals("finally")){
            return JavaTokenTypes.LITERAL_finally;
        }
        if (text.equals("catch")){
            return JavaTokenTypes.LITERAL_catch;
        } 
        if (text.equals("instanceof")){
            return JavaTokenTypes.LITERAL_instanceof;
        }
        if (text.equals("true")){
            return JavaTokenTypes.LITERAL_true;
        }
        if (text.equals("false")){
            return JavaTokenTypes.LITERAL_false;
        }
        if (text.equals("null")){
            return JavaTokenTypes.LITERAL_null;
        }
        if (text.equals("new")){
            return JavaTokenTypes.LITERAL_new;
        }
        if (text.equals("void")){
            return JavaTokenTypes.LITERAL_void;
        }
        if (text.equals("static")){
            return JavaTokenTypes.LITERAL_static;
        }
        if (text.equals("boolean")){
            return JavaTokenTypes.LITERAL_boolean;
        }
        if (text.equals("byte")){
            return JavaTokenTypes.LITERAL_byte;
        }
        if (text.equals("char")){
            return JavaTokenTypes.LITERAL_char;
        }
        if (text.equals("short")){
            return JavaTokenTypes.LITERAL_short;
        }
        if (text.equals("int")){
            return JavaTokenTypes.LITERAL_int;
        }
        if (text.equals("float")){
            return JavaTokenTypes.LITERAL_float;
        }
        if (text.equals("long")){
            return JavaTokenTypes.LITERAL_long;
        }
        if (text.equals("double")){
            return JavaTokenTypes.LITERAL_double;
        }
        if (text.equals("package")){
            return JavaTokenTypes.LITERAL_package;
        }
        if (text.equals("super")){
            return JavaTokenTypes.LITERAL_super;
        }
        if (text.equals("import")){
            return JavaTokenTypes.LITERAL_import;
        }
        if (text.equals("package")){
            return JavaTokenTypes.LITERAL_package;
        }
        if (text.equals("strictfp")){
            return JavaTokenTypes.STRICTFP;
        }
        if (text.equals("goto")){
            return JavaTokenTypes.GOTO;
        }
        if (text.equals("return")){
            return JavaTokenTypes.LITERAL_return;
        }
        if (text.equals("final")){
            return JavaTokenTypes.FINAL;
        }
        return JavaTokenTypes.IDENT;

    }

    private boolean match(char c1, char c2){
        if (c1==c2){
            return true;
        }
        else return false;
    }

}
