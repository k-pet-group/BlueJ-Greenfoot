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
package bluej.parser.ast.gen;


import java.io.IOException;
import java.io.Reader;

import antlr.TokenStream;
import antlr.TokenStreamException;
import bluej.parser.EscapedUnicodeReader;
import bluej.parser.ast.LocatableToken;


public class BlueJJavaLexer implements JavaTokenTypes, TokenStream
{
    private StringBuffer textBuffer; // text of current token
    private EscapedUnicodeReader reader;
    private int tabsize=8;
    public static final char EOF_CHAR = (char)-1;
    private int col,line;     
    private char rChar= (char)-1; 
    private boolean newline=false;
    //private LexerState state;

    public BlueJJavaLexer(Reader in) {
        reader=new EscapedUnicodeReader(in);
        col=((EscapedUnicodeReader)in).getPosition()+1;
        line=1;
    }

    private LocatableToken makeToken(int type, String txt, int beginCol, int endCol, int beginLine, int line, boolean calcCol){
        LocatableToken tok = new LocatableToken();
        if (calcCol){
            int textLength=txt.length();
            endCol=textLength+beginCol;
        }
        tok.setType(type);
        tok.setText(txt);
        tok.setColumn(beginCol);
        tok.setLine(beginLine);        
        tok.setEndLineAndCol(line, endCol);
        if (newline) {
            newline();
        }
        return tok;

    }
    
    private LocatableToken makeToken(int type, String txt, int beginCol, int endCol, int beginLine, int line){
        LocatableToken tok = new LocatableToken();
        tok.setType(type);
        tok.setText(txt);
        tok.setColumn(beginCol);
        tok.setLine(beginLine);        
        tok.setEndLineAndCol(line, endCol);
        if (newline) {
            newline();
        }
        return tok;

    }

    public int getTabSize() {
        return tabsize;
    }

    public void setTabSize(int tabsize) {
        this.tabsize = tabsize;
    }

    private int readChar(int position){
        char [] cb=new char[1];
        try{
            int val=reader.readChar(cb, position);
            if (val==-1)
                return -1;
        }catch(IOException ioe){
            //print an error
            return -1;
        }        
        return cb[0];
    }

    public LocatableToken nextToken() throws TokenStreamException {  
        resetText();
        if (rChar==(char)-1 || Character.isWhitespace(rChar)){
            return nextValidToken();
        }
        return createToken(rChar);
    }

    private LocatableToken nextValidToken(){
        if (rChar==' '){
            col++;
        }
        int rval=readChar(col-1);
        if (rval==-1){
            return processEndOfReader();                
        }
        char nextChar=(char)rval;
        if (Character.isWhitespace(nextChar)){
            if (nextChar=='\n'){
                newline();
            } else if (Character.isWhitespace(nextChar)){
                col++; 
                newline=false;
            }
            try{
                return nextToken();
            }catch(TokenStreamException e){

            }
        } 
        return createToken(nextChar);
    }

    public void resetText() {
        textBuffer=new StringBuffer();
    }

    public void consume(char c) /*throws Exception*/ {
        if (c=='\n'){
            newline();
            return;
        }
        append(c);
        //int pos=reader.getPosition();
        //if it is a unicode escape char need to bump up the column by 4
        //if (reader.isEscapedUnicodeChar()){
        //    col=col+5;
        //    reader.setEscapedUnicodeChar(false);
        //} else 
        ++col;
    }

    /*
     * Does no checking on what it is appending
     * 
     */
    public void consume(char c, boolean overwrite) /*throws Exception*/ {
        if (overwrite){
            append(c, overwrite);
            //int pos=reader.getPosition();
            if (c!='\n')
                ++col;
        }
        else consume(c);
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

    public void tab() {
        col = col+tabsize;
    }

    public void newline() {
        line++;
        col= 1;
        newline=false;
    }

    private LocatableToken createToken(char nextChar){
        int bCol=col;
        int bLine=line; 
        if (Character.isJavaIdentifierStart(nextChar))
            return createWordToken(nextChar, bCol, bLine); 
        if (Character.isDigit(nextChar))
            return createDigitToken(nextChar, bCol, bLine);
        return makeToken(getSymbolType(nextChar), textBuffer.toString(), bCol, col, bLine, line); 
    }


    private LocatableToken createDigitToken(char nextChar, int bCol, int bLine){
        //return makeToken(getNumberType(), getNumberText(nextChar), bCol, col, bLine, line);  
        return makeToken(getDigitType(nextChar, false), textBuffer.toString(), bCol, col, bLine, line, false); 
    }

    private LocatableToken createWordToken(char nextChar, int bCol, int bLine){
        populateTextBuffer(nextChar);
        return makeToken(getTokenType(), textBuffer.toString(), bCol, col, bLine, line, true);     
    }

    private LocatableToken processEndOfReader(){
        resetText();
        rChar=(char)-1;
        return makeToken(JavaTokenTypes.EOF, "EOF", col, col, line, line, false);
    }

    private void populateTextBuffer(char ch){
        char thisChar=ch;
        char [] cb=new char[1];
        int rval=0;
        boolean complete=false;
        try{
            do{  
                consume(thisChar);
                rval=reader.readChar(cb, col-1);
                //eof
                if (rval==-1){
                    rChar=(char)-1;
                    return;
                }
                thisChar=cb[0];
                if (Character.isWhitespace(thisChar))  {
                    col++;
                    complete=true;
                    rChar=(char)-1;
                    if (thisChar=='\n')
                        newline=true;
                }
                else if (!Character.isLetterOrDigit(thisChar)&& thisChar!='_'){
                    complete=true;
                    rChar=thisChar;
                }
            }while (!complete);
        }catch(IOException ioe){


        }
    }


    private boolean getTokenText(char endChar){
        char thisChar=endChar;
        char [] cb=new char[1];
        int rval=0;     
        boolean complete=false;
        char prevChar;
        try{
            while (!complete){  
                rval=reader.readChar(cb, col-1);
                //eof
                if (rval==-1){
                    return false;
                }
                prevChar= thisChar;
                thisChar=cb[0]; 
                consume(thisChar, true);
                if (thisChar=='\n'){
                    newline=true;
                    return false;
                }
                //endChar is the flag for the end of reading
                if (thisChar ==endChar && prevChar!='\\')  {
                    return true;
                }               
            }
        }catch(IOException ioe){

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
        consume(ch);
        try{
            while (!complete){ 

                rval=reader.readChar(cb, col-1);
                //eof
                if (rval==-1){
                    rChar=(char)-1;
                    return type;
                }
                ch=cb[0];
                rChar=ch;
                if (!Character.isDigit(ch)){
                    if (ch=='.'){
                        if (isDecimal){
                            rChar=ch;
                            return JavaTokenTypes.NUM_DOUBLE;
                        }
                        else {
                            isDecimal=true;
                            consume(ch);
                        }
                    }
                    else if (Character.isLetter(ch)){
                        rChar=(char)-1;
                        if (ch=='f'|| ch=='F'){
                            consume(ch);
                            type= JavaTokenTypes.NUM_FLOAT;
                            isDecimal=false;
                        } else if (ch=='d'|| ch=='D'){
                            consume(ch);
                            type= JavaTokenTypes.NUM_DOUBLE;
                        } else if (ch=='l'|| ch=='L'){
                            consume(ch);
                            type= JavaTokenTypes.NUM_LONG;
                            isDecimal=false;
                        }
                        else if (ch=='e'|| ch=='E'){
                            consume(ch);
                            type= JavaTokenTypes.NUM_DOUBLE;
                        }
                        else if (ch=='x'){
                            hexDecimalNumber=true;
                            consume(ch);
                        }
                        else if (hexDecimalNumber && (ch=='a'|| ch=='A' || ch=='b' ||ch=='B'||ch=='c'||ch=='C'||ch=='e'||ch=='E')){
                            consume(ch);
                            type= JavaTokenTypes.NUM_INT;
                        }
                        else {
                            complete=true;
                            rChar=ch;
                        }                   
                    }
                    else if (Character.isWhitespace(ch)|| (!Character.isLetterOrDigit(ch))){
                        complete=true;
                        col++;
                        rChar=ch;
                    }else {
                        complete=true;
                        rChar=ch;
                    }              

                } else {
                    consume(ch);
                    type=JavaTokenTypes.NUM_INT;
                }

            }           
        }catch(IOException ioe){

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
        try{
            do{  
                consume(ch, true);
                rval=reader.readChar(cb, col-1);
                //eof
                if (rval==-1){
                    if (type==JavaTokenTypes.ML_COMMENT)
                        return JavaTokenTypes.INVALID;
                    else return type;
                }
                ch=cb[0];
                if (ch=='\n'){
                    line++;
                    col=1;
                    if (type==JavaTokenTypes.SL_COMMENT)
                        return type;
                }

                if (checkflag){
                    if (ch=='/'){

                        complete=true;
                        consume(ch);
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
        }catch(IOException ioe){
            return JavaTokenTypes.INVALID;
        }
        return type;
    }

    private int getTokenType(){
        char ch=textBuffer.charAt(0);
        if (Character.isJavaIdentifierStart(ch))
            return getWordType();
        //need to specify what type of digit double float etc
        if (Character.isDigit(ch))
            return getNumberType();
        return getSymbolType();
    }

    private int getSymbolType(){
        int type= JavaTokenTypes.INVALID;
        if (match('"'))
            return JavaTokenTypes.STRING_LITERAL;
        if (match('\''))
            return JavaTokenTypes.CHAR_LITERAL;
        if (match('?'))
            return JavaTokenTypes.QUESTION;
        if (match(','))
            return JavaTokenTypes.COMMA;
        if (match(';'))
            return JavaTokenTypes.SEMI;
        if (match(':'))
            return JavaTokenTypes.COLON;
        if (match('^'))
            return getBXORType();
        if (match('~'))
            return JavaTokenTypes.BNOT;
        if (match('('))
            return JavaTokenTypes.LPAREN;
        if (match(')'))
            return JavaTokenTypes.RPAREN;
        if (match('['))
            return JavaTokenTypes.LBRACK;
        if (match(']'))
            return JavaTokenTypes.RBRACK;
        if (match('{'))
            return JavaTokenTypes.LCURLY;
        if (match('}'))
            return JavaTokenTypes.RCURLY;
        if (match('@'))
            return JavaTokenTypes.AT;
        if (match('&'))
            return getAndType();
        if (match('|'))
            return getOrType();
        if (match('!'))
            return getExclamationType();
        if (match('+'))
            return getPlusType();            
        if (match('-'))
            return getMinusType();
        if (match('='))
            return getEqualType();
        if (match('%'))
            return getModType();
        if (match('/'))
            return getForwardSlashType();
        if (match('.'))
            return getDotType();
        if (match('*'))
            return getStarType();
        if (match('>'))
            return getGTType();
        if (match('<'))
            return getLTType();

        return type;

    }

    private int getSymbolType(char ch){
        int type= JavaTokenTypes.INVALID;
        consume(ch);
        rChar=(char)-1;
        if (match('"', ch))
            return getStringLiteral();
        if (match('\'', ch))
            return getCharLiteral();
        if (match('?', ch))
            return JavaTokenTypes.QUESTION;
        if (match(',', ch))
            return JavaTokenTypes.COMMA;
        if (match(';', ch))
            return JavaTokenTypes.SEMI;
        if (match(':', ch))
            return JavaTokenTypes.COLON;
        if (match('^', ch))
            return getBXORType();
        if (match('~', ch))
            return JavaTokenTypes.BNOT;
        if (match('(', ch))
            return JavaTokenTypes.LPAREN;
        if (match(')', ch))
            return JavaTokenTypes.RPAREN;
        if (match('[', ch))
            return JavaTokenTypes.LBRACK;
        if (match(']', ch))
            return JavaTokenTypes.RBRACK;
        if (match('{', ch))
            return JavaTokenTypes.LCURLY;
        if (match('}', ch))
            return JavaTokenTypes.RCURLY;
        if (match('@', ch))
            return JavaTokenTypes.AT;
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

        return type;

    }

    private int getBXORType(){
        char validChars[]=new char[1];
        validChars[0]='='; 
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0], validChars)){
                return JavaTokenTypes.BXOR;
            }
            char thisChar=(char)cb[0]; 
            if (thisChar=='='){
                consume(thisChar); 
                return JavaTokenTypes.BXOR_ASSIGN;
            } else
                rChar=thisChar;           
        }catch(IOException e){
            return JavaTokenTypes.INVALID;
        }
        return JavaTokenTypes.BXOR;
    }

    private int getAndType(){
        char validChars[]=new char[2];
        validChars[0]='=';
        validChars[1]='&';        
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0], validChars)){
                return JavaTokenTypes.BAND;
            }
            char thisChar=(char)cb[0]; 
            if (thisChar=='='){
                consume(thisChar); 
                return JavaTokenTypes.BAND_ASSIGN; 
            }
            if (thisChar=='&'){
                consume(thisChar); 
                return JavaTokenTypes.LAND; 
            }
            rChar=thisChar;
        }catch(IOException e){
            return JavaTokenTypes.INVALID;
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
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0], validChars)){
                return JavaTokenTypes.BOR;
            }
            char thisChar=(char)cb[0]; 
            if (thisChar=='='){
                consume(thisChar); 
                return JavaTokenTypes.BOR_ASSIGN; 
            }
            if (thisChar=='|'){
                consume(thisChar); 
                return JavaTokenTypes.LOR; 
            }
            rChar=thisChar;
        }catch(IOException e){
            return JavaTokenTypes.INVALID;
        }
        return JavaTokenTypes.BOR;
    }

    private int getPlusType(){
        //+, +=, ++
        char validChars[]=new char[2];
        validChars[0]='+';
        validChars[1]='=';
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0], validChars)){
                return JavaTokenTypes.PLUS;
            }
            char thisChar=(char)cb[0]; 
            if (thisChar=='='){
                consume(thisChar); 
                return JavaTokenTypes.PLUS_ASSIGN; 
            }
            if (thisChar=='+'){
                consume(thisChar); 
                return JavaTokenTypes.INC; 
            }
            rChar=thisChar;
        }catch(IOException e){
            return JavaTokenTypes.INVALID;
        }
        return JavaTokenTypes.PLUS;

    }

    private int getMinusType(){
        //-, -=, --
        char validChars[]=new char[2];
        validChars[0]='=';
        validChars[1]='-'; 
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0], validChars)){
                return JavaTokenTypes.MINUS;
            }
            char thisChar=(char)cb[0]; 
            if (thisChar=='='){
                consume(thisChar); 
                return JavaTokenTypes.MINUS_ASSIGN; 
            }
            if (thisChar=='-'){
                consume(thisChar); 
                return JavaTokenTypes.DEC; 
            }
            rChar=thisChar;
        }catch(IOException e){
            return JavaTokenTypes.INVALID;
        }
        return JavaTokenTypes.MINUS;
    }

    private int getEqualType(){
        //=, ==
        char validChars[]=new char[1];
        validChars[0]='=';
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0], validChars)){
                return JavaTokenTypes.ASSIGN;
            }
            char thisChar=(char)cb[0]; 
            if (thisChar=='='){
                consume(thisChar); 
                return JavaTokenTypes.EQUAL; 
            }
            rChar=thisChar;
        }catch(IOException e){
            return JavaTokenTypes.INVALID;
        }
        return JavaTokenTypes.ASSIGN;
    }

    private int getStarType(){
        //*, *=, 
        char validChars[]=new char[1];
        validChars[0]='=';
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0])){
                return JavaTokenTypes.STAR;
            }
            char thisChar=(char)cb[0]; 
            if (thisChar=='='){
                consume(thisChar); 
                return JavaTokenTypes.STAR_ASSIGN; 
            }
            rChar=thisChar;
        }catch(IOException e){
            return JavaTokenTypes.INVALID;
        }
        return JavaTokenTypes.STAR;
    }

    private int getModType(){
        //%, %=
        char validChars[]=new char[1];
        validChars[0]='=';
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0], validChars)){
                return JavaTokenTypes.MOD;
            }
            char thisChar=(char)cb[0]; 
            if (thisChar=='='){
                consume(thisChar); 
                return JavaTokenTypes.MOD_ASSIGN; 
            }
            rChar=thisChar;
        }catch(IOException e){
            return JavaTokenTypes.INVALID;
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
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0], validChars)){
                return JavaTokenTypes.DIV;
            }
            char thisChar=(char)cb[0]; 
            if (thisChar=='='){
                consume(thisChar); 
                return JavaTokenTypes.DIV_ASSIGN; 
            }
            if (thisChar=='/')
                return getCommentType(thisChar, JavaTokenTypes.SL_COMMENT);
            if (thisChar=='*')
                return getCommentType(thisChar, JavaTokenTypes.ML_COMMENT);
            rChar=thisChar;
        }catch(IOException e){
            return JavaTokenTypes.INVALID;
        }
        return JavaTokenTypes.DIV;
    }

    private boolean isComplete(int rval, char ch){
        if (rval==-1 || Character.isWhitespace(ch)
                || Character.isLetterOrDigit(ch) || Character.isJavaIdentifierStart(ch) ){
            if(rval==-1)
                rChar=(char)-1;
            else if (ch=='\n'){
                rChar=(char)-1;
                newline=true;
            }
            else
                rChar=ch;
            return true;
        }
        return false;
    }

    private boolean isComplete(int rval, char ch, char validChars[]){
        if (!isComplete(rval, ch))
            for (int i=0; i<validChars.length; i++){
                if (validChars[i]==ch)
                    return false;
            }
        if (rval==-1)
            rChar=(char)-1;
        else if (ch=='\n'){
            rChar=(char)-1;
            newline=true;
        }
        else
            rChar=ch;
        return true;
    }

    private int getGTType(){
        char validChars[]=new char[2];
        validChars[0]='>';
        validChars[1]='='; 
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0], validChars)){
                return JavaTokenTypes.GT;
            }
            char thisChar=(char)cb[0];
            //>=
            if (thisChar=='='){
                consume(thisChar); 
                return JavaTokenTypes.GE; 
            }
            if (thisChar=='>'){
                consume(thisChar); 
                rval=reader.readChar(cb, col-1);
                thisChar=cb[0];
                //>>
                if (isComplete(rval, thisChar, validChars)){
                    return JavaTokenTypes.SR;
                }
                //>>>; >>>=; >>=
                if (thisChar=='>'){
                    consume(thisChar); 
                    rval=reader.readChar(cb, col-1);
                    thisChar=cb[0];
                    validChars[0]='=';
                    if (isComplete(rval, thisChar, validChars)){
                        return JavaTokenTypes.BSR;
                    }
                    if (thisChar=='='){
                        consume(thisChar); 
                        rChar=(char)-1;
                        return JavaTokenTypes.BSR_ASSIGN; 
                    }
                }
                if (thisChar=='='){
                    consume(thisChar); 
                    rChar=(char)-1;
                    return JavaTokenTypes.SR_ASSIGN; 
                }
            }
            rChar=thisChar;
        }catch(IOException e){
            return JavaTokenTypes.INVALID;
        }
        return JavaTokenTypes.GT;
    }

    private int getLTType(){
        char validChars[]=new char[2];
        validChars[0]='<';
        validChars[1]='='; 
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0])){
                return JavaTokenTypes.LT;
            }
            char thisChar=(char)cb[0]; 
            if (thisChar=='='){
                consume(thisChar); 
                return JavaTokenTypes.LE; 
            }
            if (thisChar=='<'){
                consume(thisChar); 
                rval=reader.readChar(cb, col-1);
                thisChar=cb[0];
                validChars[0]='=';
                if (isComplete(rval, thisChar, validChars)){
                    return JavaTokenTypes.SL;
                }
                if (thisChar=='='){
                    consume(thisChar);
                    return JavaTokenTypes.SL_ASSIGN;
                }
            }
            rChar=thisChar;
        }catch(IOException e){
            return JavaTokenTypes.INVALID;
        }
        return JavaTokenTypes.LT;

    }

    private int getExclamationType(){
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (rval==-1 || Character.isWhitespace((char)cb[0])){
                return JavaTokenTypes.LNOT;
            }
            char thisChar=(char)cb[0]; 
            if (thisChar=='='){
                consume(thisChar); 
                return JavaTokenTypes.NOT_EQUAL; 
            }
            rChar=thisChar;
        }catch(IOException e){
            return JavaTokenTypes.INVALID;
        }
        return JavaTokenTypes.LNOT;
    }

    private int getDotType(){
        char validChars[]=new char[1];
        validChars[0]='.';
        //. or .56f .12 
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            char ch=cb[0];
            rChar=ch;
            if (rval==-1 || Character.isWhitespace((char)ch)|| Character.isLetter(ch) || ch=='\n'){
                if (rval==-1)
                    rChar=(char)-1;
                if (ch=='\n'){
                    newline=true;
                    rChar=(char)-1;
                }                    
                return JavaTokenTypes.DOT;
            }
            char thisChar=(char)cb[0]; 
            if (Character.isDigit(thisChar)){
                return getDigitType(thisChar, true);
            }
            //...
            else if (ch=='.'){
                consume(ch);
                rval=reader.readChar(cb, col-1);
                if (rval==-1){
                    rChar=(char)-1;
                    return JavaTokenTypes.INVALID;
                }
                ch=cb[0];
                rChar=(char)ch;
                if (ch=='.'){
                    consume(ch);
                    rChar=(char)-1;
                    return JavaTokenTypes.TRIPLE_DOT;
                }
                else return JavaTokenTypes.INVALID;

            }
            rChar=thisChar;
        }catch(IOException e){
            return JavaTokenTypes.INVALID;
        }
        return JavaTokenTypes.DOT;
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

    private int getNumberType(){

        if (textBuffer.indexOf("f")!=-1)
            return NUM_FLOAT;
        if (textBuffer.indexOf("l")!=-1)
            return NUM_LONG;
        if (textBuffer.indexOf("d")!=-1)
            return NUM_DOUBLE;
        if (textBuffer.indexOf(".")!=-1)
            return NUM_DOUBLE;
        return NUM_INT;

    }

    private boolean match(char c){
        if (textBuffer.charAt(0)==c){
            return true;
        }
        else return false;
    }

    private boolean match(char c1, char c2){
        if (c1==c2){
            return true;
        }
        else return false;
    }

}
