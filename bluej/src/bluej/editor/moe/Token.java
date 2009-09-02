package bluej.editor.moe;

/**
 * This is a replacement for the Token class from jedit.
 * 
 * @author Davin McCall
 */
public class Token
{
    public byte id;     // Token type, one of the constants declared below
    public int length;  // Length of text represented by this token
    public Token next;  // Next token in the chain
    
    public static final byte NULL = 0;
    public static final byte COMMENT1 = 1;  // normal comment
    public static final byte COMMENT2 = 2;  // javadoc comment
    public static final byte COMMENT3 = 3;  // standout comment
    public static final byte KEYWORD1 = 4;
    public static final byte KEYWORD2 = 5;
    public static final byte KEYWORD3 = 6;
    public static final byte PRIMITIVE = 7;
    public static final byte LITERAL1 = 8;
    public static final byte LITERAL2 = 9;
    public static final byte LABEL = 10;
    public static final byte OPERATOR = 11;
    public static final byte INVALID = 12;
    
    /* The number of token ids (above) */
    public static final byte ID_COUNT = 13;
    
    public static final byte END = 100;
    
    public Token(int length, byte id)
    {
        this.id = id;
        this.length = length;
    }
}
