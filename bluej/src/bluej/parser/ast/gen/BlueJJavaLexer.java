package bluej.parser.ast.gen;


import java.io.IOException;
import java.io.Reader;
import bluej.parser.EscapedUnicodeReader;
import bluej.parser.TokenStream;
import bluej.parser.TokenStreamException;
import bluej.parser.ast.LocatableToken;
import bluej.parser.ast.Token;
import bluej.parser.ast.gen.JavaTokenTypes;

public class BlueJJavaLexer implements TokenStream, JavaTokenTypes
{

    protected Token _returnToken = null; // used to return tokens w/o using return val.
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

    protected Token makeToken() {
        LocatableToken tok = new LocatableToken();
        tok.setColumn(col);
        tok.setLine(line);
        return tok;
    }

    private Token makeToken(int type, String txt, int beginCol, int col, int beginLine, int line){
        LocatableToken tok = new LocatableToken();
        tok.setType(type);
        tok.setText(txt);
        tok.setColumn(beginCol);
        tok.setLine(beginLine);
        tok.setEndColumn(col);
        tok.setEndLine(line);
        return tok;

    }

    protected Token makeToken(int type) {
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

    public Token nextToken() throws TokenStreamException {
        Token theRetToken=null;
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


    private Token processWord(){
        Token rToken=null;
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

    private Token processOther(){
        int type=JavaTokenTypes.EOF;
        
        return null;
    }
    
    private Token processEndOfReader(){
        int type=JavaTokenTypes.EOF; 
        resetText();
        Token rToken=makeToken(type, "eof", col, col, line, line);
        return rToken;
    }

    private int getWordType(String text){
        int type=JavaTokenTypes.EOF;
        if (text.length()==1)
            type=JavaTokenTypes.LITERAL_char;
        if (text.equals("public")){
            type=JavaTokenTypes.LITERAL_public;
        }
        if (text.equals("private")){
            type=JavaTokenTypes.LITERAL_private;
        }
        if (text.equals("protected")){
            type=JavaTokenTypes.LITERAL_protected;
        }
        if (text.equals("volatile")){
            type=JavaTokenTypes.LITERAL_volatile;
        }
        if (text.equals("abstract")){
            type=JavaTokenTypes.ABSTRACT;
        }
        if (text.equals("transient")){
            type=JavaTokenTypes.LITERAL_transient;
        }
        if (text.equals("class")){
            type=JavaTokenTypes.LITERAL_class;
        }
        if (text.equals("enum")){
            type=JavaTokenTypes.LITERAL_enum;
        }
        if (text.equals("interface")){
            type=JavaTokenTypes.LITERAL_interface;
        }

        return type;
    }

}
