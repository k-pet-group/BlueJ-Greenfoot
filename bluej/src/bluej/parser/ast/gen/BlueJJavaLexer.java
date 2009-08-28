package bluej.parser.ast.gen;


import java.io.IOException;
import java.io.Reader;
import antlr.TokenStreamException;
import bluej.parser.EscapedUnicodeReader;
import bluej.parser.ast.LocatableToken;

public class BlueJJavaLexer implements JavaTokenTypes
{
    private static final int LETTER_CHAR=0;
    private static final int DIGIT_CHAR=1;
    private static final int OTHER_CHAR=2;
    private StringBuffer textBuffer; // text of current token
    private EscapedUnicodeReader reader;
    private int tabsize=8;
    private static final char INVALID_TYPE=0;
    public static final char EOF_CHAR = (char)-1;
    private int col,line;     
    private char rChar= (char)-1; 
    private boolean newline=false;
    private boolean tab=false;
    //private LexerState state;



    public BlueJJavaLexer(Reader in) {
        reader=new EscapedUnicodeReader(in);
        col=((EscapedUnicodeReader)in).getPosition()+1;
        line=1;
    }

    private LocatableToken makeToken(int type, String txt, int beginCol, int endCol, int beginLine, int line){
        LocatableToken tok = new LocatableToken();
        int textLength=txt.length();
        if (newline) {
            beginCol=1;
            beginLine=line;
            newline=false;
        }
        endCol=textLength+beginCol;
        tok.setType(type);
        tok.setText(txt);
        tok.setColumn(beginCol);
        tok.setLine(beginLine);        
        tok.setEndLineAndCol(line, endCol);
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
        int rval=1;
        char nextChar=rChar;
        if (rChar==(char)-1){
            rval=readChar(col-1);
            if (rval==-1){
                return processEndOfReader();                
            }
            nextChar=(char)rval;
            if (nextChar=='\n'|| nextChar=='\t' || Character.isWhitespace(nextChar)){
                if (nextChar=='\n'){
                    newline();
                } else if (Character.isWhitespace(nextChar)){
                    col++;
                    //if there is a/n and then spaces, the newline has been taken into consideration 
                    newline=false;
                }
                try{
                    return nextToken();
                }catch(TokenStreamException e){

                }
            }       
        }
        return createToken(nextChar);
    }


    public void resetText() {
        textBuffer=new StringBuffer();
    }

    public void consume(char c) /*throws Exception*/ {
        if (c == '\t') {
            tab();
            tab=true;
            return ;
        }
        if (c=='\n'){
            newline();
            return;
        }
        append(c);
        int pos=reader.getPosition();
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
            int pos=reader.getPosition();
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
        newline=true;
    }

    private LocatableToken createToken(char nextChar){
        int bCol=col;
        int bLine=line;     
        populateTextBuffer(nextChar);
        int type=getTokenType();
        return makeToken(type, textBuffer.toString(), bCol, col, bLine, line);
    }

    //    private void adjustColLineNums(){
    //        //have to decrease by 1 because already bumped it up in populateTextBuffer
    //        if (tab){
    //            col=col-1+tabsize;
    //            tab=false;
    //        }
    //        if (newline){
    //            line=line+1;
    //            newline=false;
    //        }
    //    }

    private LocatableToken processEndOfReader(){
        resetText();
        return makeToken(JavaTokenTypes.EOF, "EOF", col, col, line, line);
    }

    private int getCharType(char c){
        //set the char type
        if (Character.isDigit(c))
            return DIGIT_CHAR;
        if (Character.isLetter(c))
            return LETTER_CHAR;
        return OTHER_CHAR;
    }

    private void populateTextBuffer(char ch){
        char thisChar=ch;
        char prevChar;
        char [] cb=new char[1];
        int rval=0;
        int charTypePrev=OTHER_CHAR;
        int charTypeNext=OTHER_CHAR;       
        resetText();
        boolean complete=false;
        try{
            do{  
                charTypePrev=getCharType(thisChar);
                prevChar=thisChar;
                consume(thisChar);
                rval=reader.readChar(cb, col-1);
                //eof
                if (rval==-1){
                    return;
                }
                thisChar=cb[0];
                charTypeNext=getCharType(thisChar);
                //Not using Character.isWhitespace(thisChar) because it would return true
                //for /n etc and those need to be considered separately
                if (thisChar ==' ')  {
                    col++;
                    if (!isComment()){                        
                        complete=true;
                        rChar=(char)-1;
                    }
                    else
                        consume(thisChar, true);
                }
                else if (isComplete(prevChar, thisChar, charTypePrev, charTypeNext)){
                    complete=true;
                    rChar=thisChar;
                }
            }while (!complete);
        }catch(IOException ioe){
    
    
        }
    }

    private boolean isComment(){
        if((textBuffer.indexOf("/*")!=-1) && ((textBuffer.indexOf("*/")==-1)|| (textBuffer.indexOf("*/")<1)))
            return true;
        if ((textBuffer.indexOf("//")!=-1) && (textBuffer.indexOf("\n")==-1))
            return true;
        return false;
    }

    private int getTokenType(){
        char ch=textBuffer.charAt(0);
        if (Character.isJavaIdentifierStart(ch))
            return getWordType();
        //need to specify what type of digit double float etc
        if (Character.isDigit(ch))
            return getNumberType();
        return getType();
    }

    private int getType(){
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


    private int getAndType(){
        //&, &=, &&
        if (textBuffer.length()==1)
            return JavaTokenTypes.BAND;   
        if (textBuffer.charAt(1)=='=')
            return JavaTokenTypes.BAND_ASSIGN;      
        if (textBuffer.charAt(1)=='&')
            return JavaTokenTypes.LAND;
        return INVALID_TYPE;
    }


    private int getOrType(){
        //|, |=, ||
        if (textBuffer.length()==1)
            return JavaTokenTypes.BOR;   
        if (textBuffer.charAt(1)=='=')
            return JavaTokenTypes.BOR_ASSIGN;      
        if (textBuffer.charAt(1)=='|')
            return JavaTokenTypes.LOR;
        return INVALID_TYPE;
    }

    private int getPlusType(){
        //+, +=, ++
        if (textBuffer.length()==1)
            return JavaTokenTypes.PLUS;   
        if (textBuffer.charAt(1)=='=')
            return JavaTokenTypes.PLUS_ASSIGN;      
        if (textBuffer.charAt(1)=='+')
            return JavaTokenTypes.INC;
        return INVALID_TYPE;
    }

    private int getMinusType(){
        //-, -=, --
        if (textBuffer.length()==1)
            return JavaTokenTypes.MINUS;   
        if (textBuffer.charAt(1)=='=')
            return JavaTokenTypes.MINUS_ASSIGN;      
        if (textBuffer.charAt(1)=='-')
            return JavaTokenTypes.DEC;
        return INVALID_TYPE;
    }

    private int getEqualType(){
        //=, ==
        if (textBuffer.length()==1)
            return JavaTokenTypes.ASSIGN;   
        if (textBuffer.charAt(1)=='=')
            return JavaTokenTypes.EQUAL;      
        return INVALID_TYPE;
    }

    private int getStarType(){
        //*, *=, */ 
        if (textBuffer.length()==1)
            return JavaTokenTypes.STAR;   
        if (textBuffer.charAt(1)=='=')
            return JavaTokenTypes.STAR_ASSIGN;     
        if (textBuffer.charAt(1)=='/')
            return JavaTokenTypes.ML_COMMENT;   
        return INVALID_TYPE;
    }

    private int getModType(){
        //%, %=
        if (textBuffer.length()==1)
            return JavaTokenTypes.MOD;   
        if (textBuffer.charAt(1)=='=')
            return JavaTokenTypes.MOD_ASSIGN;       
        return INVALID_TYPE;
    }

    private int getForwardSlashType(){
        // /, //, /*, /= /n /t
        if (textBuffer.length()==1)
            return JavaTokenTypes.DIV;   
        if (textBuffer.charAt(1)=='/')
            return JavaTokenTypes.SL_COMMENT;   
        if (textBuffer.charAt(1)=='*')
            return JavaTokenTypes.ML_COMMENT;   
        if (textBuffer.charAt(1)=='=')
            return JavaTokenTypes.DIV_ASSIGN;   
        if (textBuffer.charAt(1)=='n'){
            newline=true;
            return JavaTokenTypes.CHAR_LITERAL;
        }
        if (textBuffer.charAt(1)=='t'){
            tab=true;
            return JavaTokenTypes.CHAR_LITERAL;
        }
        return INVALID_TYPE;
    }

    private int getGTType(){
        if (textBuffer.length()==1)
            return JavaTokenTypes.GT;   
        //>=
        if (textBuffer.charAt(1)=='=')
            return JavaTokenTypes.GE;
        //>>
        if (textBuffer.charAt(1)=='>' && (textBuffer.length()==2))
            return JavaTokenTypes.SR; 
        //>>>; >>=; 
        if (textBuffer.charAt(1)=='>' && (textBuffer.length()==3)){
            if ((textBuffer.charAt(2)=='>'))
                return BSR;
            if (textBuffer.charAt(2)=='=')
                return SR_ASSIGN;
        } 
        //>>>=
        if (textBuffer.charAt(1)=='>' && textBuffer.charAt(2)=='>' && textBuffer.charAt(3)=='='&& (textBuffer.length()==4))
            return BSR_ASSIGN;
        return INVALID_TYPE;
    }

    private int getLTType(){
        if (textBuffer.length()==1)
            return JavaTokenTypes.LT;   
        //<=
        if (textBuffer.charAt(1)=='=')
            return JavaTokenTypes.LE;
        //<<
        if (textBuffer.charAt(1)=='<' && (textBuffer.length()==2))
            return JavaTokenTypes.SL; 
        //<<=; 
        if (textBuffer.charAt(1)=='<' && (textBuffer.length()==3) && (textBuffer.charAt(2)=='=')){ 
            return SL_ASSIGN;
        } 
        return INVALID_TYPE;
    }

    private int getExclamationType(){
        if (textBuffer.length()==1)
            return JavaTokenTypes.LNOT;
        if(textBuffer.charAt(1)=='=')
            return JavaTokenTypes.NOT_EQUAL;
        return INVALID_TYPE;
    }

    private int getDotType(){
        //. or .56f .12 
        if (textBuffer.length()==1)
            return JavaTokenTypes.DOT;  
        return getNumberType();
    }

    private int getBXORType(){
        if (textBuffer.length()==1)
            return JavaTokenTypes.BXOR;
        if(textBuffer.charAt(1)=='=')
            return JavaTokenTypes.BXOR_ASSIGN;
        return INVALID_TYPE;
    }


    private int getWordType(){
        String text=textBuffer.toString();
        int type=JavaTokenTypes.EOF;
        if (text.length()==1)
            return JavaTokenTypes.LITERAL_char;
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

    /*
     * Certain symbols are always stand-alone and this is just a flag to indicate 
     * that the reader should not be read further
     * private char[] singleChars={'}', '{', '[', ']', '(',')'}; 
     */
    private boolean isSingleChar(char ch){
        if (ch=='{'
            || ch=='}' 
                || ch=='['
                    || ch==']'
                        || ch=='('|| ch==')')
            return true;
        else return false;
    }

    /* 
     * There are some instance where a change in type does indicate a new token
     * and some instances where it doesn't e.g compare 1+value; and an_identifier99
     * This method checks for these cases
     */
    private boolean isComplete(char prevChar, char thisChar, int prevType, int thisType){
        if (isComment())
            return false;
        if (textBuffer.length()==0)
            return false;
        //e.g test_name  before '_'
        if ((prevType==LETTER_CHAR && thisChar=='_') )
            return false;
        //e.g test_name  after '_'
        if ((thisType==LETTER_CHAR && prevChar=='_') )
            return false;
        //e.g identifier99
        if ((prevType==LETTER_CHAR && thisType==DIGIT_CHAR) )
            return false;
        //numbers
        if ((prevType==DIGIT_CHAR && thisType==DIGIT_CHAR) )
            return false;
        if (prevType==DIGIT_CHAR && (thisChar=='f'||thisChar=='d'||thisChar=='l'))
            return false;
        //numbers with decimal places 2.03 before '.'
        if (prevType==DIGIT_CHAR && thisChar=='.')
            return false;
        //numbers with decimal places 2.03 after '.'
        if (prevChar=='.' && thisType==DIGIT_CHAR)
            return false;
        //string_literal
        if (prevChar=='\"'&& thisType==LETTER_CHAR)
            return false;
        //string_literal
        if (thisChar=='\"'&& prevType==LETTER_CHAR)
            return false;
        //char_literal
        if (prevChar=='\''&& thisType==LETTER_CHAR)
            return false;
        //char_literal
        if (thisChar=='\''&& prevType==LETTER_CHAR)
            return false;
        if ((thisType!=prevType) || 
                //e.g () 
                (thisType==OTHER_CHAR && isSingleChar(thisChar))     
        )
            return true;
        return false;
    }


}
