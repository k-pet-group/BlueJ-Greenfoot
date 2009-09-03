package bluej.parser.ast.gen;


import java.io.IOException;
import java.io.Reader;
import antlr.TokenStreamException;
import bluej.parser.EscapedUnicodeReader;
import bluej.parser.ast.LocatableToken;

public class BlueJJavaLexer implements JavaTokenTypes
{
    private StringBuffer textBuffer; // text of current token
    private EscapedUnicodeReader reader;
    private int tabsize=8;
    private static final char INVALID_TYPE=0;
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

    private LocatableToken makeToken(int type, String txt, int beginCol, int endCol, int beginLine, int line){
        LocatableToken tok = new LocatableToken();
        int textLength=txt.length();
        endCol=textLength+beginCol;
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
        return makeToken(getDigitValue(nextChar, false), textBuffer.toString(), bCol, col, bLine, line); 
    }

    private LocatableToken createWordToken(char nextChar, int bCol, int bLine){
        populateTextBuffer(nextChar);
        return makeToken(getTokenType(), textBuffer.toString(), bCol, col, bLine, line);     
    }

    private LocatableToken processEndOfReader(){
        resetText();
        return makeToken(JavaTokenTypes.EOF, "EOF", col, col, line, line);
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
        char thisChar;
        char [] cb=new char[1];
        int rval=0;     
        boolean complete=false;
        try{
            while (!complete){  
                rval=reader.readChar(cb, col-1);
                //eof
                if (rval==-1){
                    return false;
                }
                thisChar=cb[0];
                consume(thisChar, true);
                //endChar is the flag for the end of reading
                if (thisChar ==endChar)  {
                    return true;
                }               
            }
        }catch(IOException ioe){

        }
        return complete;
    }

    private int getDigitValue(char ch, boolean dot){
        char [] cb=new char[1];
        int rval=0;     
        boolean complete=false;
        boolean isDecimal=dot;
        consume(ch);
        try{
            while (!complete){  
                rval=reader.readChar(cb, col-1);
                //eof
                if (rval==-1){
                    return JavaTokenTypes.NUM_INT;
                }
                ch=cb[0];
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
                        if (ch=='f'){
                            consume(ch);
                            return JavaTokenTypes.NUM_FLOAT;
                        } if (ch=='d'){
                            consume(ch);
                            return JavaTokenTypes.NUM_DOUBLE;
                        } if (ch=='l'){
                            consume(ch);
                            return JavaTokenTypes.NUM_LONG;
                        }
                        else {
                            complete=true;
                            rChar=ch;
                        }                   
                    }
                    else if (Character.isWhitespace(ch)){
                        complete=true;
                        col++;
                        rChar=ch;
                    }else {
                        complete=true;
                        rChar=ch;
                    }              

                } else 
                    consume(ch);
            }           
        }catch(IOException ioe){

        }
        if (isDecimal)
            return JavaTokenTypes.NUM_DOUBLE;
        return JavaTokenTypes.NUM_INT;
    }
    
    private void  getCommentText(char ch, int type){
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
                    return;
                }
                ch=cb[0];
                if (type==JavaTokenTypes.SL_COMMENT && ch=='\n'){
                    return;
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

        }
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
        int type=INVALID_TYPE;
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
        int type=INVALID_TYPE;
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
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0])){
                return JavaTokenTypes.BXOR;
            }
            char thisChar=(char)cb[0]; 
            if (thisChar=='='){
                consume(thisChar); 
                return JavaTokenTypes.BXOR_ASSIGN;
            } else
                rChar=thisChar;           
        }catch(IOException e){
            return INVALID_TYPE;
        }
        return JavaTokenTypes.BXOR;
    }

    private int getAndType(){
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0])){
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
            return INVALID_TYPE;
        }
        return JavaTokenTypes.BAND;
    }

    private int getStringLiteral(){
        boolean success=getLiteralText('"');
        if (success)
            return JavaTokenTypes.STRING_LITERAL;
        return INVALID_TYPE;        
    }

    private boolean getLiteralText(char endChar){
        return getTokenText(endChar);           
    }

    private int getCharLiteral(){
        boolean success=getLiteralText('\'');
        if (success)
            return JavaTokenTypes.CHAR_LITERAL;
        return INVALID_TYPE;        
    }



    private int getOrType(){
        //|, |=, ||
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0])){
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
            return INVALID_TYPE;
        }
        return JavaTokenTypes.BOR;
    }

    private int getPlusType(){
        //+, +=, ++
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0])){
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
            return INVALID_TYPE;
        }
        return JavaTokenTypes.PLUS;

    }

    private int getMinusType(){
        //-, -=, --
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0])){
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
            return INVALID_TYPE;
        }
        return JavaTokenTypes.MINUS;
    }

    private int getEqualType(){
        //=, ==
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0])){
                return JavaTokenTypes.ASSIGN;
            }
            char thisChar=(char)cb[0]; 
            if (thisChar=='='){
                consume(thisChar); 
                return JavaTokenTypes.EQUAL; 
            }
            rChar=thisChar;
        }catch(IOException e){
            return INVALID_TYPE;
        }
        return JavaTokenTypes.ASSIGN;
    }

    private int getStarType(){
        //*, *=, 
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
            return INVALID_TYPE;
        }
        return JavaTokenTypes.STAR;
    }

    private int getModType(){
        //%, %=
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0])){
                return JavaTokenTypes.MOD;
            }
            char thisChar=(char)cb[0]; 
            if (thisChar=='='){
                consume(thisChar); 
                return JavaTokenTypes.MOD_ASSIGN; 
            }
            rChar=thisChar;
        }catch(IOException e){
            return INVALID_TYPE;
        }
        return JavaTokenTypes.MOD;
    }

    private int getForwardSlashType(){
        // /, //, /*, /= 
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0])){
                return JavaTokenTypes.DIV;
            }
            char thisChar=(char)cb[0]; 
            if (thisChar=='='){
                consume(thisChar); 
                return JavaTokenTypes.DIV_ASSIGN; 
            }
            if (thisChar=='/'){
                getCommentText(thisChar, JavaTokenTypes.SL_COMMENT);
                return JavaTokenTypes.SL_COMMENT; 
            }
            if (thisChar=='*'){
                getCommentText(thisChar, JavaTokenTypes.ML_COMMENT);
                return JavaTokenTypes.ML_COMMENT; 
            }
            rChar=thisChar;
        }catch(IOException e){
            return INVALID_TYPE;
        }
        return JavaTokenTypes.DIV;
    }

    private boolean isComplete(int rval, char ch){
        if (rval==-1 || Character.isWhitespace(ch)
                || Character.isLetterOrDigit(ch) ){
            rChar=ch;
            return true;
        }
        return false;
    }

    private int getGTType(){
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (isComplete(rval, cb[0])){
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
                if (isComplete(rval, thisChar)){
                    rChar=thisChar;
                    return JavaTokenTypes.SR;
                }
                //>>>; >>>=; >>=
                if (thisChar=='>'){
                    consume(thisChar); 
                    rval=reader.readChar(cb, col-1);
                    thisChar=cb[0];
                    if (isComplete(rval, thisChar)){
                        rChar=thisChar;
                        return JavaTokenTypes.BSR;
                    }
                    if (thisChar=='='){
                        consume(thisChar); 
                        return JavaTokenTypes.BSR_ASSIGN; 
                    }
                }
                if (thisChar=='='){
                    consume(thisChar); 
                    return JavaTokenTypes.SR_ASSIGN; 
                }
            }
            rChar=thisChar;
        }catch(IOException e){
            return INVALID_TYPE;
        }
        return JavaTokenTypes.GT;
    }

    private int getLTType(){
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
                if (isComplete(rval, thisChar)){
                    rChar=thisChar;
                    return JavaTokenTypes.SL;
                }
                if (thisChar=='='){
                    consume(thisChar);
                    return JavaTokenTypes.SL_ASSIGN;
                }
            }
            rChar=thisChar;
        }catch(IOException e){
            return INVALID_TYPE;
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
            return INVALID_TYPE;
        }
        return JavaTokenTypes.LNOT;
    }

    private int getDotType(){
        //. or .56f .12 
        char [] cb=new char[1];
        try{
            int rval=reader.readChar(cb, col-1);
            if (rval==-1 || Character.isWhitespace((char)cb[0])){
                return JavaTokenTypes.DOT;
            }
            char thisChar=(char)cb[0]; 
            if (Character.isDigit(thisChar)){
                return getDigitValue(thisChar, true);
            }
            rChar=thisChar;
        }catch(IOException e){
            return INVALID_TYPE;
        }
        return JavaTokenTypes.DOT;
    }

    private int getWordType(){
        String text=textBuffer.toString();
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
        if (text.equals("strictfp")){
            return JavaTokenTypes.STRICTFP;
        }
        if (text.equals("goto")){
            return JavaTokenTypes.GOTO;
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
