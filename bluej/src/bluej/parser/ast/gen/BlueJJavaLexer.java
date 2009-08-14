package bluej.parser.ast.gen;


import java.io.IOException;
import java.io.Reader;
import bluej.parser.EscapedUnicodeReader;
import bluej.parser.TokenStream;
import bluej.parser.TokenStreamException;
import bluej.parser.ast.LocatableToken;
import bluej.parser.ast.Token;
import bluej.parser.ast.gen.JavaTokenTypes;

public class BlueJJavaLexer implements JavaTokenTypes
{

    protected LocatableToken _returnToken = null; // used to return tokens w/o using return val.
    private StringBuffer textBuffer; // text of current token
    int col;
    int line;    
    private Reader reader;
    private int tabsize=8;
    public static final char NO_CHAR = 0;
    public static final char EOF_CHAR = (char)-1;
    private char [] cb=new char[1]; //just a holder used in different places

    public BlueJJavaLexer(Reader in) {
        reader=in;
        col=((EscapedUnicodeReader)in).getPosition()+1;
        // line=((EscapedUnicodeReader)in).getLine();
    }

    protected LocatableToken makeToken() {
        LocatableToken tok = new LocatableToken();
        tok.setColumn(col);
        tok.setLine(line);
        return tok;
    }

    private LocatableToken makeToken(int type, String txt, int beginCol, int col, int beginLine, int line){
        LocatableToken tok = new LocatableToken();
        tok.setType(type);
        tok.setText(txt);
        tok.setColumn(beginCol);
        tok.setLine(beginLine);
        tok.setEndLineAndCol(line, col);
        return tok;

    }

    protected LocatableToken makeToken(int type) {
        LocatableToken tok = new LocatableToken();
        tok.setType(type);
        tok.setColumn(col);
        tok.setLine(line);
        return tok;
    }

    public int getTabsize() {
        return tabsize;
    }

    public void setTabsize(int tabsize) {
        this.tabsize = tabsize;
    }

    public char LA (int index){
        cb=new char[col+1];
        int val=0;
        try{
            val=reader.read(cb, col, index);
        }catch(IOException e){
             System.out.println("Unable to read  the source stream");
             return NO_CHAR;
        }
        if (val==-1){
            return EOF_CHAR;
        }
        return (char)cb[col];
    }

    public LocatableToken nextToken() throws TokenStreamException {
        LocatableToken theRetToken=null;
        char nextChar=LA(1);
        if (!Character.isDefined(nextChar)){
            theRetToken=processEndOfReader();
        }
        else if (Character.isLetter(nextChar)){
            theRetToken= processWord();
        }      
        else{
            theRetToken= processOther();
        }
        return theRetToken;

    }

    public void resetText() {
        textBuffer=new StringBuffer();
    }



    public void consume(char c) /*throws Exception*/ {
        append(c);
        if (c == '\t') {
            tab();
        }
        else {
            ++col;
        }
    }

    private void append(char c){
        textBuffer.append(c);
    }

    public void tab() {
        col = col+tabsize;
    }



    public void newline() {
        line++;
        col= 1;
    }


    private LocatableToken processWord(){
        LocatableToken rToken=null;
        int bCol=col;
        int bLine=line;
        int endCol=col;
        int endLine=line;
        resetText();
        char c=cb[col];
        int i;
        boolean complete=false;
        do{  
            try{
                char cbuffer[]=new char[col+2];
                consume(c);
                i=reader.read(cbuffer, col, 1);
                c=cbuffer[col];
                if (i ==-1){
                    complete=true; 
                }
                else if (c ==' '){
                    col++;
                    complete=true;
                }else
                {
                    endCol++;
                    endLine++;
                }
            }catch(IOException e){
                complete=true;
                //print out an error
            }
        }while (!complete);


        int type=getWordType(textBuffer.toString());
        rToken=makeToken(type, textBuffer.toString(), bCol, col, bLine, line);
        return rToken;
    }

    private LocatableToken processOther(){
        int type=JavaTokenTypes.EOF;
        
        return null;
    }
    
    private LocatableToken processEndOfReader(){
        int type=JavaTokenTypes.EOF; 
        resetText();
        LocatableToken rToken=makeToken(type, "eof", col, col, line, line);
        return rToken;
    }

    private int getWordType(String text){
        int type=JavaTokenTypes.EOF;
        if (text.length()==1)
            type=JavaTokenTypes.LITERAL_char;
        else if (text.equals("public")){
            type=JavaTokenTypes.LITERAL_public;
        }
        else if (text.equals("private")){
            type=JavaTokenTypes.LITERAL_private;
        }
        else  if (text.equals("protected")){
            type=JavaTokenTypes.LITERAL_protected;
        }
        else if (text.equals("volatile")){
            type=JavaTokenTypes.LITERAL_volatile;
        }
        else if (text.equals("abstract")){
            type=JavaTokenTypes.ABSTRACT;
        }
        else  if (text.equals("transient")){
            type=JavaTokenTypes.LITERAL_transient;
        }
        else if (text.equals("class")){
            type=JavaTokenTypes.LITERAL_class;
        }
        else if (text.equals("enum")){
            type=JavaTokenTypes.LITERAL_enum;
        }
        else if (text.equals("interface")){
            type=JavaTokenTypes.LITERAL_interface;
        }
        else if (text.equals("switch")){
            type=JavaTokenTypes.LITERAL_switch;
        }
        else if (text.equals("while")){
            type=JavaTokenTypes.LITERAL_while;
        }
        else if (text.equals("do")){
            type=JavaTokenTypes.LITERAL_do;
        }
        else if (text.equals("for")){
            type=JavaTokenTypes.LITERAL_for;
        }
        else if (text.equals("void")){
            type=JavaTokenTypes.LITERAL_void;
        }
        else if (text.equals("byte")){
            type=JavaTokenTypes.LITERAL_byte;
        }
        else if (text.equals("char")){
            type=JavaTokenTypes.LITERAL_char;
        }
        else if (text.equals("short")){
            type=JavaTokenTypes.LITERAL_short;
        }
        else if (text.equals("int")){
            type=JavaTokenTypes.LITERAL_int;
        }
        else if (text.equals("float")){
            type=JavaTokenTypes.LITERAL_float;
        }
        else if (text.equals("long")){
            type=JavaTokenTypes.LITERAL_long;
        }
        else if (text.equals("double")){
            type=JavaTokenTypes.LITERAL_double;
        }
        else if (text.equals("native")){
            type=JavaTokenTypes.LITERAL_native;
        }
        else if (text.equals("synchronized")){
            type=JavaTokenTypes.LITERAL_synchronized;
        }
        else if (text.equals("volatile")){
            type=JavaTokenTypes.LITERAL_volatile;
        }
        else if (text.equals("default")){
            type=JavaTokenTypes.LITERAL_default;
        }
        else if (text.equals("implements")){
            type=JavaTokenTypes.LITERAL_implements;
        }
        else if (text.equals("this")){
            type=JavaTokenTypes.LITERAL_this;
        }
        else if (text.equals("throws")){
            type=JavaTokenTypes.LITERAL_throws;
        }
        else if (text.equals("try")){
            type=JavaTokenTypes.LITERAL_try;
        }
        else if (text.equals("finally")){
            type=JavaTokenTypes.LITERAL_finally;
        }
        else if (text.equals("catch")){
            type=JavaTokenTypes.LITERAL_catch;
        } 
        else if (text.equals("instanceof")){
            type=JavaTokenTypes.LITERAL_instanceof;
        }
        else if (text.equals("true")){
            type=JavaTokenTypes.LITERAL_true;
        }
        else if (text.equals("false")){
            type=JavaTokenTypes.LITERAL_false;
        }
        else if (text.equals("null")){
            type=JavaTokenTypes.LITERAL_null;
        }
        else if (text.equals("new")){
            type=JavaTokenTypes.LITERAL_new;
        }
        else if (text.equals("void")){
            type=JavaTokenTypes.LITERAL_void;
        }
        else if (text.equals("boolean")){
            type=JavaTokenTypes.LITERAL_boolean;
        }
        else if (text.equals("byte")){
            type=JavaTokenTypes.LITERAL_byte;
        }
        else if (text.equals("char")){
            type=JavaTokenTypes.LITERAL_char;
        }
        else if (text.equals("short")){
            type=JavaTokenTypes.LITERAL_short;
        }
        else if (text.equals("int")){
            type=JavaTokenTypes.LITERAL_int;
        }
        else if (text.equals("float")){
            type=JavaTokenTypes.LITERAL_float;
        }
        else if (text.equals("long")){
            type=JavaTokenTypes.LITERAL_long;
        }
        else if (text.equals("double")){
            type=JavaTokenTypes.LITERAL_double;
        }
        else type=JavaTokenTypes.IDENT;

        return type;
    }

}
